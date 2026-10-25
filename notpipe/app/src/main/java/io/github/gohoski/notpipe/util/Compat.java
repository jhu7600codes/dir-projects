package io.github.gohoski.notpipe.util;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;

import java.lang.reflect.Method;

import io.github.gohoski.notpipe.NotPipe;

/**
 * Created by Claude on 25.10.2026.
 * Calls to APIs newer than Android 1.5 go through reflection here, so the classes keep loading on old Dalvik.
 */

public class Compat {
    private static final String WRITE_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";

    private Compat() {}

    /**
     * Service.startForeground() on 2.0+, Service.setForeground() (removed from the SDK) on 1.5/1.6.
     */
    public static void startForeground(Service service, int id, Notification notification) {
        try {
            if (NotPipe.SDK >= 5) {
                Method m = Service.class.getMethod("startForeground", int.class, Notification.class);
                m.invoke(service, id, notification);
            } else {
                Method m = Service.class.getMethod("setForeground", boolean.class);
                m.invoke(service, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopForeground(Service service) {
        try {
            if (NotPipe.SDK >= 5) {
                Method m = Service.class.getMethod("stopForeground", boolean.class);
                m.invoke(service, true);
            } else {
                Method m = Service.class.getMethod("setForeground", boolean.class);
                m.invoke(service, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Activity.invalidateOptionsMenu() on 3.0+, where the action bar doesn't call onPrepareOptionsMenu by itself */
    public static void invalidateOptionsMenu(Activity activity) {
        if (NotPipe.SDK < 11) return;
        try {
            Activity.class.getMethod("invalidateOptionsMenu").invoke(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Asks for the storage permission on Android 6.0+ (it's granted at install time before that).
     * @return true if the permission is already granted
     */
    public static boolean ensureStoragePermission(Activity activity) {
        if (NotPipe.SDK < 23) return true;
        try {
            Method check = activity.getClass().getMethod("checkSelfPermission", String.class);
            if ((Integer) check.invoke(activity, WRITE_STORAGE) == 0) return true;
            Method request = activity.getClass().getMethod("requestPermissions", String[].class, int.class);
            request.invoke(activity, new String[]{WRITE_STORAGE}, 1);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * Since targetSdkVersion is 24+, Android 7.0+ throws on file:// URIs passed to other apps.
     * notPipe has no FileProvider (it doesn't exist on old Android), so the check is turned off instead.
     */
    public static void allowFileUris() {
        if (NotPipe.SDK < 24) return;
        try {
            Class<?> strictMode = Class.forName("android.os.StrictMode");
            Class<?> policyClass = Class.forName("android.os.StrictMode$VmPolicy");
            Class<?> builderClass = Class.forName("android.os.StrictMode$VmPolicy$Builder");
            Object policy = builderClass.getMethod("build").invoke(builderClass.newInstance());
            strictMode.getMethod("setVmPolicy", policyClass).invoke(null, policy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
