package app.lineagifier;

// runs inside shizuku's process (shell uid 2000, or root if shizuku was started with root)
interface IShellService {
    // shizuku calls this transaction code when it wants the service gone
    void destroy() = 16777114;

    // runs `sh -c cmd`; returns [exitCode, stdout, stderr]
    String[] exec(String cmd) = 1;

    // streams an apk into `pm install -r -S size`; returns [exitCode, stdout, stderr].
    // the shell uid can't read our private files, so we hand it a descriptor instead of a path.
    String[] installApk(in ParcelFileDescriptor apk, long size) = 2;
}
