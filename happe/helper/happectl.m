// happectl: tiny setuid-root helper the app calls to control the xray daemon
// and the wifi http proxy. installed as /usr/libexec/happe/happectl,
// root:wheel 4755.
//
//   happectl start       copy the app's config, test it, launchctl load -w
//   happectl stop        launchctl unload -w
//   happectl status      "running <pid>" / "stopped"
//   happectl proxy-on    set wifi http/https proxy to 127.0.0.1:8080
//   happectl proxy-off   restore the previous wifi proxy settings
//   happectl log         last lines of the xray log

#import <Foundation/Foundation.h>
#include <spawn.h>
#include <fcntl.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/file.h>
#include <sys/sysctl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#define LABEL       "com.jhu.happe"
#define PLIST       "/Library/LaunchDaemons/com.jhu.happe.plist"
// the daemon plist ships here and is only copied into LaunchDaemons while
// connected: no Disabled key / -w override games, which ios 10's launchctl
// silently ignored. a reboot while connected starts xray again, like the
// wifi proxy setting that also survives the reboot.
#define PLIST_SRC   "/usr/share/happe/com.jhu.happe.plist"
// written when launchd refused to run the job; later starts go direct
#define LAUNCHD_BROKEN "/etc/happe/launchd-broken"
// present while the user wants to be connected; "boot" reconnects from it
#define AUTOSTART   "/etc/happe/autostart"
#define LOCK_PATH   "/etc/happe/.lock"
#define XRAY        "/usr/libexec/happe/xray"
#define USER_CONFIG "/var/mobile/Library/Happe/config.json"
#define ROOT_DIR    "/etc/happe"
#define ROOT_CONFIG "/etc/happe/config.json"
#define PROXY_BACKUP "/etc/happe/proxies-backup.plist"
#define LOG_PATH    "/var/log/happe.log"
#define CACERT      "/usr/share/happe/cacert.pem"
#define HTTP_PORT   8080
#define SOCKS_PORT  1080

// --- private apis (present on ios, headers are mac-only) ---

typedef const struct __SCPreferences *SCPreferencesRef;
extern SCPreferencesRef SCPreferencesCreate(CFAllocatorRef, CFStringRef, CFStringRef);
extern CFPropertyListRef SCPreferencesGetValue(SCPreferencesRef, CFStringRef);
extern CFDictionaryRef SCPreferencesPathGetValue(SCPreferencesRef, CFStringRef);
extern Boolean SCPreferencesPathSetValue(SCPreferencesRef, CFStringRef, CFDictionaryRef);
extern Boolean SCPreferencesLock(SCPreferencesRef, Boolean);
extern Boolean SCPreferencesUnlock(SCPreferencesRef);
extern Boolean SCPreferencesCommitChanges(SCPreferencesRef);
extern Boolean SCPreferencesApplyChanges(SCPreferencesRef);
extern void SCPreferencesSynchronize(SCPreferencesRef);
extern int SCError(void);
extern const char *SCErrorString(int);

#define MEMORYSTATUS_CMD_SET_PRIORITY_PROPERTIES   2
#define MEMORYSTATUS_CMD_SET_JETSAM_HIGH_WATER_MARK 5
typedef struct {
    int32_t priority;
    uint64_t user_data;
} vp_memorystatus_priority_properties_t;
extern int memorystatus_control(uint32_t command, int32_t pid, uint32_t flags, void *buffer, size_t buffersize);

// --- small utils ---

static char *const kCleanEnv[] = { "PATH=/usr/bin:/bin:/usr/sbin:/sbin", "SSL_CERT_FILE=" CACERT, NULL };

// runs argv[0] with a clean environment, returns exit status (or -1) and
// optionally the combined stdout+stderr.
static int run(const char *const argv[], NSString **output) {
    int fds[2];
    if (pipe(fds) != 0) return -1;
    posix_spawn_file_actions_t fa;
    posix_spawn_file_actions_init(&fa);
    posix_spawn_file_actions_addopen(&fa, 0, "/dev/null", O_RDONLY, 0);
    posix_spawn_file_actions_adddup2(&fa, fds[1], 1);
    posix_spawn_file_actions_adddup2(&fa, fds[1], 2);
    posix_spawn_file_actions_addclose(&fa, fds[0]);
    posix_spawn_file_actions_addclose(&fa, fds[1]);
    pid_t pid;
    int rc = posix_spawn(&pid, argv[0], &fa, NULL, (char *const *)argv, kCleanEnv);
    posix_spawn_file_actions_destroy(&fa);
    close(fds[1]);
    NSMutableData *buf = [NSMutableData data];
    if (rc == 0) {
        char tmp[4096];
        ssize_t n;
        while ((n = read(fds[0], tmp, sizeof tmp)) > 0) [buf appendBytes:tmp length:(NSUInteger)n];
    }
    close(fds[0]);
    if (rc != 0) {
        if (output) *output = [NSString stringWithFormat:@"spawn %s failed: %s", argv[0], strerror(rc)];
        return -1;
    }
    int st = 0;
    while (waitpid(pid, &st, 0) < 0 && errno == EINTR) {}
    if (output) *output = [[NSString alloc] initWithData:buf encoding:NSUTF8StringEncoding] ?: @"";
    return WIFEXITED(st) ? WEXITSTATUS(st) : -1;
}

static const char *launchctlPath(void) {
    const char *candidates[] = { "/bin/launchctl", "/usr/bin/launchctl", "/sbin/launchctl" };
    for (size_t i = 0; i < sizeof candidates / sizeof *candidates; i++)
        if (access(candidates[i], X_OK) == 0) return candidates[i];
    return NULL;
}

// first process named xray; daemonOnly = only ones reparented to launchd
// (ours), otherwise also e.g. one started by hand in a terminal
static pid_t findXrayProc(BOOL daemonOnly) {
    int mib[4] = { CTL_KERN, KERN_PROC, KERN_PROC_ALL, 0 };
    size_t len = 0;
    if (sysctl(mib, 3, NULL, &len, NULL, 0) != 0) return 0;
    len += len / 4;
    struct kinfo_proc *procs = malloc(len);
    if (!procs) return 0;
    pid_t found = 0;
    if (sysctl(mib, 3, procs, &len, NULL, 0) == 0) {
        size_t n = len / sizeof(struct kinfo_proc);
        for (size_t i = 0; i < n; i++) {
            if (strncmp(procs[i].kp_proc.p_comm, "xray", MAXCOMLEN) == 0 && (!daemonOnly || procs[i].kp_eproc.e_ppid == 1)) {
                found = procs[i].kp_proc.p_pid;
                break;
            }
        }
    }
    free(procs);
    return found;
}

static pid_t findXray(void) {
    return findXrayProc(YES);
}

// unix time the process started, 0 if unknown
static long processStart(pid_t pid) {
    int mib[4] = { CTL_KERN, KERN_PROC, KERN_PROC_PID, pid };
    struct kinfo_proc kp;
    size_t len = sizeof kp;
    if (sysctl(mib, 4, &kp, &len, NULL, 0) != 0 || len == 0) return 0;
    return (long)kp.kp_proc.p_starttime.tv_sec;
}

static BOOL portOpen(int port) {
    int s = socket(AF_INET, SOCK_STREAM, 0);
    if (s < 0) return NO;
    struct sockaddr_in a = { 0 };
    a.sin_len = sizeof a;
    a.sin_family = AF_INET;
    a.sin_port = htons(port);
    a.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    BOOL ok = connect(s, (struct sockaddr *)&a, sizeof a) == 0;
    close(s);
    return ok;
}

// copies the app-written config into a root-only location. refuses symlinks,
// non-regular files and files not owned by the calling user.
static BOOL installConfig(uid_t caller, NSString **err) {
    int in = open(USER_CONFIG, O_RDONLY | O_NOFOLLOW);
    if (in < 0) {
        *err = [NSString stringWithFormat:@"cannot open %s: %s", USER_CONFIG, strerror(errno)];
        return NO;
    }
    struct stat st;
    if (fstat(in, &st) != 0 || !S_ISREG(st.st_mode) || (st.st_uid != caller && caller != 0) || st.st_size > 256 * 1024) {
        close(in);
        *err = @"config file is not a regular file owned by the caller (or too big)";
        return NO;
    }
    NSMutableData *data = [NSMutableData dataWithLength:(NSUInteger)st.st_size];
    ssize_t got = read(in, data.mutableBytes, data.length);
    close(in);
    if (got != (ssize_t)data.length || ![NSJSONSerialization JSONObjectWithData:data options:0 error:nil]) {
        *err = @"config is not valid json";
        return NO;
    }
    mkdir(ROOT_DIR, 0700);
    chown(ROOT_DIR, 0, 0);
    chmod(ROOT_DIR, 0700);
    NSString *tmp = @ROOT_CONFIG ".tmp";
    int out = open(tmp.fileSystemRepresentation, O_WRONLY | O_CREAT | O_TRUNC | O_NOFOLLOW, 0600);
    if (out < 0 || write(out, data.bytes, data.length) != (ssize_t)data.length) {
        if (out >= 0) close(out);
        *err = [NSString stringWithFormat:@"cannot write %s: %s", ROOT_CONFIG, strerror(errno)];
        return NO;
    }
    fchown(out, 0, 0);
    fchmod(out, 0600);
    close(out);
    if (rename(tmp.fileSystemRepresentation, ROOT_CONFIG) != 0) {
        *err = [NSString stringWithFormat:@"rename failed: %s", strerror(errno)];
        return NO;
    }
    return YES;
}

static void tuneJetsam(pid_t pid) {
    // iphone 5s has 1gb of ram; keep jetsam from picking the daemon first and
    // give it a roomy high water mark. best effort: ignored if it fails.
    vp_memorystatus_priority_properties_t props = { 10, 0 };
    int a = memorystatus_control(MEMORYSTATUS_CMD_SET_PRIORITY_PROPERTIES, pid, 0, &props, sizeof props);
    int b = memorystatus_control(MEMORYSTATUS_CMD_SET_JETSAM_HIGH_WATER_MARK, pid, 256, NULL, 0);
    if (a != 0 || b != 0) fprintf(stderr, "note: jetsam tuning partly failed (%d/%d), not fatal\n", a, b);
}

// --- commands ---

// socks port in the installed config (1080 unless it was moved)
static int configSocksPort(void) {
    NSData *d = [NSData dataWithContentsOfFile:@ROOT_CONFIG];
    NSDictionary *cfg = d ? [NSJSONSerialization JSONObjectWithData:d options:0 error:nil] : nil;
    for (NSDictionary *in in cfg[@"inbounds"])
        if ([in[@"tag"] isEqual:@"socks-in"]) return [in[@"port"] intValue];
    return SOCKS_PORT;
}

// some tweaks already listen on 1080; move our socks inbound to a free port
static int moveSocksIfBusy(void) {
    int port = SOCKS_PORT;
    if (!portOpen(port)) return port;
    for (port = 10808; port < 10830 && portOpen(port); port++) {}
    NSData *d = [NSData dataWithContentsOfFile:@ROOT_CONFIG];
    NSMutableDictionary *cfg = d ? [NSJSONSerialization JSONObjectWithData:d options:NSJSONReadingMutableContainers error:nil] : nil;
    for (NSMutableDictionary *in in cfg[@"inbounds"])
        if ([in[@"tag"] isEqual:@"socks-in"]) in[@"port"] = @(port);
    NSData *out = cfg ? [NSJSONSerialization dataWithJSONObject:cfg options:NSJSONWritingPrettyPrinted error:nil] : nil;
    if (!out || ![out writeToFile:@ROOT_CONFIG atomically:YES]) return 0;
    chmod(ROOT_CONFIG, 0600);
    return port;
}

static int cmdStatus(void) {
    pid_t pid = findXray();
    if (pid > 0 && portOpen(HTTP_PORT)) {
        printf("running %d socks %d since %ld\n", pid, configSocksPort(), processStart(pid));
        return 0;
    }
    if (pid > 0) {
        printf("starting %d (port %d not open yet)\n", pid, HTTP_PORT);
        return 0;
    }
    if (access(AUTOSTART, F_OK) == 0) {
        // connected before a reboot/respring, xray not back yet
        printf("stopped (reconnect pending)\n");
        return 4;
    }
    printf("stopped\n");
    return 3;
}

static BOOL copyFile(const char *from, const char *to, mode_t mode) {
    NSData *d = [NSData dataWithContentsOfFile:@(from)];
    if (!d) return NO;
    NSString *tmp = [@(to) stringByAppendingString:@".tmp"];
    if (![d writeToFile:tmp atomically:NO]) return NO;
    chown(tmp.fileSystemRepresentation, 0, 0);
    chmod(tmp.fileSystemRepresentation, mode);
    return rename(tmp.fileSystemRepresentation, to) == 0;
}

static NSString *tailOf(NSString *path, NSUInteger lines) {
    NSString *s = [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:nil];
    NSArray *all = [[s stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]]
        componentsSeparatedByString:@"\n"];
    NSUInteger from = all.count > lines ? all.count - lines : 0;
    return [[all subarrayWithRange:NSMakeRange(from, all.count - from)] componentsJoinedByString:@"\n"];
}

static void waitForXray(BOOL running, int tenths) {
    for (int i = 0; i < tenths; i++) {
        BOOL up = findXray() > 0 && (!running || portOpen(HTTP_PORT));
        if (up == running) return;
        usleep(100 * 1000);
    }
}

// forget the job by label (works even if its plist file is already gone,
// e.g. after a package upgrade) and by path
static void removeJob(const char *lc) {
    if (!lc) return;
    const char *rm[] = { lc, "remove", LABEL, NULL };
    run(rm, NULL);
    const char *unload[] = { lc, "unload", PLIST, NULL };
    run(unload, NULL);
}

// kills every xray, including ones started by hand, so the ports are free
static void killXray(void) {
    for (int round = 0; round < 3; round++) {
        pid_t pid;
        int n = 0;
        while ((pid = findXrayProc(NO)) > 0 && n++ < 10) {
            kill(pid, round == 0 ? SIGTERM : SIGKILL);
            for (int i = 0; i < 30 && kill(pid, 0) == 0; i++) usleep(100 * 1000);
        }
        if (findXrayProc(NO) <= 0) return;
    }
}

static int cmdStop(void) {
    unlink(AUTOSTART);
    removeJob(launchctlPath());
    unlink(PLIST);
    killXray();
    printf("stopped\n");
    return 0;
}

// fallback when launchd won't run the job: start xray ourselves, detached
// (own session, reparented to launchd), same env and log as the plist
static pid_t spawnDirect(void) {
    pid_t child = fork();
    if (child < 0) return -1;
    if (child == 0) {
        setsid();
        if (fork() != 0) _exit(0);
        int fd = open(LOG_PATH, O_WRONLY | O_CREAT | O_APPEND, 0644);
        int nul = open("/dev/null", O_RDONLY);
        if (nul >= 0) dup2(nul, 0);
        if (fd >= 0) {
            dup2(fd, 1);
            dup2(fd, 2);
        }
        for (int i = 3; i < 256; i++) close(i);
        chdir("/");
        char *const argv[] = { XRAY, "run", "-c", ROOT_CONFIG, NULL };
        char *const env[] = { "PATH=/usr/bin:/bin:/usr/sbin:/sbin", "SSL_CERT_FILE=" CACERT,
                              "XRAY_LOCATION_ASSET=/usr/share/happe", "GOMEMLIMIT=96MiB", "GOGC=50", NULL };
        execve(XRAY, argv, env);
        _exit(127);
    }
    int st;
    waitpid(child, &st, 0);
    waitForXray(YES, 50);
    return findXray();
}

static int cmdStart(uid_t caller, BOOL useInstalledConfig) {
    const char *lc = launchctlPath();
    if (!lc) {
        printf("error: launchctl not found (install the launchctl package from cydia)\n");
        return 1;
    }
    if (access(XRAY, X_OK) != 0) {
        printf("error: %s missing or not executable\n", XRAY);
        return 1;
    }
    NSString *err = nil;
    if (useInstalledConfig ? access(ROOT_CONFIG, R_OK) != 0 : !installConfig(caller, &err)) {
        printf("error: %s\n", err ? err.UTF8String : "no saved config to reconnect with");
        return 1;
    }

    NSString *out = nil;
    const char *test[] = { XRAY, "run", "-test", "-c", ROOT_CONFIG, NULL };
    int rc = run(test, &out);
    if (rc != 0) {
        printf("error: xray rejected the config (exit %d):\n%s\n", rc, out.UTF8String);
        return 1;
    }

    // reload so a new config takes effect
    removeJob(lc);
    killXray();

    if (portOpen(HTTP_PORT)) {
        printf("error: port %d is already used by another app or tweak. reboot, then connect again.\n", HTTP_PORT);
        return 1;
    }
    int socks = moveSocksIfBusy();
    if (!socks) {
        printf("error: port %d is used by another tweak and the config couldn't be moved to another port\n", SOCKS_PORT);
        return 1;
    }
    // fresh log per connect so errors are easy to find
    truncate(LOG_PATH, 0);

    pid_t pid = 0;
    BOOL direct = access(LAUNCHD_BROKEN, F_OK) == 0;
    NSString *launchdWhy = nil;
    if (!direct) {
        if (!copyFile(PLIST_SRC, PLIST, 0644)) {
            printf("error: cannot install %s from %s\n", PLIST, PLIST_SRC);
            return 1;
        }
        const char *load[] = { lc, "load", PLIST, NULL };
        rc = run(load, &out);
        waitForXray(YES, 50);
        if (findXray() <= 0) {
            // loaded but not launched - kick it by label
            const char *kick[] = { lc, "start", LABEL, NULL };
            run(kick, NULL);
            waitForXray(YES, 50);
        }
        pid = findXray();
        if (pid <= 0) {
            NSString *list = nil;
            const char *ls[] = { lc, "list", LABEL, NULL };
            run(ls, &list);
            NSString *status = @"?";
            NSRange r = [list rangeOfString:@"\"LastExitStatus\" = "];
            if (r.location != NSNotFound) {
                NSString *rest = [list substringFromIndex:NSMaxRange(r)];
                status = [rest componentsSeparatedByString:@";"].firstObject;
            }
            launchdWhy = [NSString stringWithFormat:@"launchd: load exit %d, LastExitStatus %@", rc, status];
            [[NSString stringWithFormat:@"%@\n\n%@\n\nlog:\n%@\n", launchdWhy, list, tailOf(@LOG_PATH, 15)]
                writeToFile:@LAUNCHD_BROKEN atomically:YES encoding:NSUTF8StringEncoding error:nil];
            removeJob(lc);
            unlink(PLIST);
            killXray();
            direct = YES;
        }
    }
    if (direct) {
        // launchd won't run the job on this phone: run xray ourselves, detached
        truncate(LOG_PATH, 0);
        pid = spawnDirect();
        if (pid <= 0 || !portOpen(HTTP_PORT)) {
            if (pid > 0) killXray();
            printf("error: xray did not start.%s%s\n%s\n", launchdWhy ? "\n" : "", launchdWhy ? launchdWhy.UTF8String : "",
                   tailOf(@LOG_PATH, 12).UTF8String);
            return 1;
        }
    }
    tuneJetsam(pid);
    close(open(AUTOSTART, O_WRONLY | O_CREAT, 0600));
    printf("running %d socks %d since %ld%s\n", pid, socks, processStart(pid), direct ? " (direct)" : "");
    return 0;
}

static SCPreferencesRef openPrefs(void) {
    SCPreferencesRef prefs = SCPreferencesCreate(NULL, CFSTR("happectl"), NULL);
    if (!prefs) printf("error: SCPreferencesCreate failed: %s\n", SCErrorString(SCError()));
    return prefs;
}

// keys of NetworkServices entries that sit on the wifi interface (en0)
static NSArray *wifiServiceKeys(SCPreferencesRef prefs) {
    NSDictionary *services = (__bridge NSDictionary *)SCPreferencesGetValue(prefs, CFSTR("NetworkServices"));
    NSMutableArray *keys = [NSMutableArray array];
    for (NSString *key in services) {
        NSDictionary *iface = services[key][@"Interface"];
        if ([iface[@"DeviceName"] isEqual:@"en0"] || [iface[@"Hardware"] isEqual:@"AirPort"])
            [keys addObject:key];
    }
    return keys;
}

static int setProxy(BOOL on) {
    SCPreferencesRef prefs = openPrefs();
    if (!prefs) return 1;
    if (!SCPreferencesLock(prefs, true)) {
        printf("error: cannot lock network preferences: %s\n", SCErrorString(SCError()));
        CFRelease(prefs);
        return 1;
    }
    NSArray *keys = wifiServiceKeys(prefs);
    if (keys.count == 0) {
        printf("error: no wifi network service found in preferences.plist\n");
        SCPreferencesUnlock(prefs);
        CFRelease(prefs);
        return 1;
    }

    NSMutableDictionary *backup = [NSMutableDictionary dictionaryWithContentsOfFile:@PROXY_BACKUP] ?: [NSMutableDictionary dictionary];
    for (NSString *key in keys) {
        NSString *path = [NSString stringWithFormat:@"/NetworkServices/%@/Proxies", key];
        NSDictionary *cur = (__bridge NSDictionary *)SCPreferencesPathGetValue(prefs, (__bridge CFStringRef)path);
        NSMutableDictionary *next;
        if (on) {
            // only back up settings that are not ours, so repeated connects keep the original
            if (![cur[@"HTTPProxy"] isEqual:@"127.0.0.1"]) backup[key] = cur ?: @{};
            next = [cur mutableCopy] ?: [NSMutableDictionary dictionary];
            next[@"HTTPEnable"] = @1;
            next[@"HTTPProxy"] = @"127.0.0.1";
            next[@"HTTPPort"] = @HTTP_PORT;
            next[@"HTTPSEnable"] = @1;
            next[@"HTTPSProxy"] = @"127.0.0.1";
            next[@"HTTPSPort"] = @HTTP_PORT;
            next[@"ProxyAutoConfigEnable"] = @0;
            next[@"ProxyAutoDiscoveryEnable"] = @0;
            // totallynotspyware is the safari jailbreak: after a reboot the proxy is
            // still set but xray only comes back after re-jailbreaking, so tns
            // must stay reachable without it
            next[@"ExceptionsList"] = @[ @"localhost", @"127.0.0.1", @"*.local", @"169.254/16",
                                         @"totallynotspyware.app", @"*.totallynotspyware.app" ];
        } else {
            NSDictionary *old = backup[key];
            if (old) {
                next = [old mutableCopy];
                [backup removeObjectForKey:key];
            } else {
                next = [cur mutableCopy] ?: [NSMutableDictionary dictionary];
                for (NSString *k in @[ @"HTTPProxy", @"HTTPPort", @"HTTPSProxy", @"HTTPSPort" ]) [next removeObjectForKey:k];
                next[@"HTTPEnable"] = @0;
                next[@"HTTPSEnable"] = @0;
            }
        }
        if (!SCPreferencesPathSetValue(prefs, (__bridge CFStringRef)path, (__bridge CFDictionaryRef)next)) {
            printf("error: cannot set %s: %s\n", path.UTF8String, SCErrorString(SCError()));
            SCPreferencesUnlock(prefs);
            CFRelease(prefs);
            return 1;
        }
    }
    mkdir(ROOT_DIR, 0700);
    [backup writeToFile:@PROXY_BACKUP atomically:YES];

    BOOL ok = SCPreferencesCommitChanges(prefs) && SCPreferencesApplyChanges(prefs);
    int scerr = SCError();
    SCPreferencesUnlock(prefs);
    SCPreferencesSynchronize(prefs);
    CFRelease(prefs);
    if (!ok) {
        printf("error: commit/apply failed: %s\n", SCErrorString(scerr));
        return 1;
    }
    printf("proxy %s on %lu wifi service(s)\n", on ? "set" : "cleared", (unsigned long)keys.count);
    return 0;
}

static int cmdLog(void) {
    NSString *log = [NSString stringWithContentsOfFile:@LOG_PATH encoding:NSUTF8StringEncoding error:nil];
    if (!log) {
        printf("(no log yet)\n");
        return 0;
    }
    NSArray *lines = [log componentsSeparatedByString:@"\n"];
    NSUInteger from = lines.count > 40 ? lines.count - 40 : 0;
    printf("%s\n", [[lines subarrayWithRange:NSMakeRange(from, lines.count - from)] componentsJoinedByString:@"\n"].UTF8String);
    return 0;
}

// after a (re)jailbreak: reconnect if the user was connected. run by the
// boot launchd job, /etc/rc.d/happe and the app; safe to run many times.
static int cmdBoot(void) {
    if (access(AUTOSTART, F_OK) != 0) {
        printf("not connected before, nothing to do\n");
        return 0;
    }
    if (findXray() > 0 && portOpen(HTTP_PORT)) {
        printf("already running %d\n", findXray());
        return 0;
    }
    int rc = cmdStart(0, YES);
    if (rc == 0) setProxy(YES);
    return rc;
}

int main(int argc, char **argv) {
    @autoreleasepool {
        setvbuf(stdout, NULL, _IONBF, 0);
        uid_t caller = getuid();
        if (argc != 2) {
            fprintf(stderr, "usage: happectl start|stop|status|boot|proxy-on|proxy-off|log\n");
            return 64;
        }
        const char *cmd = argv[1];
        if (strcmp(cmd, "status") == 0) return cmdStatus();
        if (strcmp(cmd, "log") == 0) return cmdLog();

        if (setgid(0) != 0 || setuid(0) != 0) {
            printf("error: happectl is not setuid root (run: chown root:wheel %s && chmod 4755 %s)\n", argv[0], argv[0]);
            return 2;
        }
        // one start/stop/boot at a time (the boot job, rc.d and the app can race)
        mkdir(ROOT_DIR, 0700);
        int lockfd = open(LOCK_PATH, O_RDWR | O_CREAT, 0600);
        if (lockfd >= 0) flock(lockfd, LOCK_EX);
        if (strcmp(cmd, "boot") == 0) return cmdBoot();
        if (strcmp(cmd, "start") == 0) return cmdStart(caller, NO);
        if (strcmp(cmd, "stop") == 0) return cmdStop();
        if (strcmp(cmd, "proxy-on") == 0) return setProxy(YES);
        if (strcmp(cmd, "proxy-off") == 0) return setProxy(NO);
        fprintf(stderr, "unknown command %s\n", cmd);
        return 64;
    }
}
