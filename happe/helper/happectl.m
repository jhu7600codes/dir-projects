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
#include <sys/sysctl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#define LABEL       "com.jhu.happe"
#define PLIST       "/Library/LaunchDaemons/com.jhu.happe.plist"
#define XRAY        "/usr/libexec/happe/xray"
#define USER_CONFIG "/var/mobile/Library/Happe/config.json"
#define ROOT_DIR    "/etc/happe"
#define ROOT_CONFIG "/etc/happe/config.json"
#define PROXY_BACKUP "/etc/happe/proxies-backup.plist"
#define LOG_PATH    "/var/log/happe.log"
#define CACERT      "/usr/share/happe/cacert.pem"
#define HTTP_PORT   8080

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

static pid_t findXray(void) {
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
            if (strncmp(procs[i].kp_proc.p_comm, "xray", MAXCOMLEN) == 0 && procs[i].kp_eproc.e_ppid == 1) {
                found = procs[i].kp_proc.p_pid;
                break;
            }
        }
    }
    free(procs);
    return found;
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

static int cmdStatus(void) {
    pid_t pid = findXray();
    if (pid > 0 && portOpen(HTTP_PORT)) {
        printf("running %d\n", pid);
        return 0;
    }
    if (pid > 0) {
        printf("starting %d (port %d not open yet)\n", pid, HTTP_PORT);
        return 0;
    }
    printf("stopped\n");
    return 3;
}

static int cmdStop(void) {
    const char *lc = launchctlPath();
    if (!lc) {
        printf("error: launchctl not found\n");
        return 1;
    }
    NSString *out = nil;
    const char *argv[] = { lc, "unload", "-w", PLIST, NULL };
    run(argv, &out);
    for (int i = 0; i < 30 && findXray() > 0; i++) usleep(100 * 1000);
    pid_t left = findXray();
    if (left > 0) kill(left, SIGKILL);
    printf("stopped\n");
    return 0;
}

static int cmdStart(uid_t caller) {
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
    if (!installConfig(caller, &err)) {
        printf("error: %s\n", err.UTF8String);
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
    const char *unload[] = { lc, "unload", PLIST, NULL };
    run(unload, NULL);
    for (int i = 0; i < 30 && findXray() > 0; i++) usleep(100 * 1000);

    const char *load[] = { lc, "load", "-w", PLIST, NULL };
    rc = run(load, &out);
    for (int i = 0; i < 50 && !(findXray() > 0 && portOpen(HTTP_PORT)); i++) usleep(100 * 1000);

    if (findXray() <= 0) {
        // some old launchctl builds ignore -w overrides; force it
        const char *force[] = { lc, "load", "-F", PLIST, NULL };
        run(force, NULL);
        for (int i = 0; i < 50 && !(findXray() > 0 && portOpen(HTTP_PORT)); i++) usleep(100 * 1000);
    }

    pid_t pid = findXray();
    if (pid <= 0) {
        printf("error: xray did not start (launchctl exit %d: %s). see %s\n", rc,
               [out stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]].UTF8String, LOG_PATH);
        return 1;
    }
    tuneJetsam(pid);
    printf("running %d\n", pid);
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
            next[@"ExceptionsList"] = @[ @"localhost", @"127.0.0.1", @"*.local", @"169.254/16" ];
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

int main(int argc, char **argv) {
    @autoreleasepool {
        setvbuf(stdout, NULL, _IONBF, 0);
        uid_t caller = getuid();
        if (argc != 2) {
            fprintf(stderr, "usage: happectl start|stop|status|proxy-on|proxy-off|log\n");
            return 64;
        }
        const char *cmd = argv[1];
        if (strcmp(cmd, "status") == 0) return cmdStatus();
        if (strcmp(cmd, "log") == 0) return cmdLog();

        if (setgid(0) != 0 || setuid(0) != 0) {
            printf("error: happectl is not setuid root (run: chown root:wheel %s && chmod 4755 %s)\n", argv[0], argv[0]);
            return 2;
        }
        if (strcmp(cmd, "start") == 0) return cmdStart(caller);
        if (strcmp(cmd, "stop") == 0) return cmdStop();
        if (strcmp(cmd, "proxy-on") == 0) return setProxy(YES);
        if (strcmp(cmd, "proxy-off") == 0) return setProxy(NO);
        fprintf(stderr, "unknown command %s\n", cmd);
        return 64;
    }
}
