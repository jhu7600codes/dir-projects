package net.sourceforge.opencamera;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lenz's performance settings. On low end phones "ultra performance" mode
 *  makes multi-frame photos (Photo and Night) much quicker by taking fewer
 *  frames and aligning them at a lower resolution.
 */
public class LenzPerformance {
    private static final String TAG = "LenzPerformance";

    public static final String PerformancePreferenceKey = "lenz_performance";
    public static final String PERFORMANCE_AUTO = "auto";
    public static final String PERFORMANCE_ULTRA = "ultra";
    public static final String PERFORMANCE_OFF = "off";

    private static volatile boolean ultra = false;
    private static Boolean lowEndDevice;

    /** Codenames of older Redmi phones (Redmi 9 / 9A / 9C and earlier); their model names are
     *  codes like M2006C3MG, so the codename is the reliable way to spot them. */
    private static final Set<String> OLD_REDMI_DEVICES = new HashSet<>(Arrays.asList(
            "angelica", "angelican", "angelicain", "cattail", // Redmi 9C / 9C NFC / 9 Activ
            "dandelion", "dandelion_c3l2", "garden", // Redmi 9A / 9i / 9AT
            "lancelot", "galahad", // Redmi 9 / 9 Prime
            "olive", "olivelite", "olivewood", "olives", // Redmi 8 / 8A
            "onc", "onclite", "pine", // Redmi 7 / 7A
            "cereus", "cactus", "sakura", "daisy", "sakura_india", // Redmi 6 / 6A / 6 Pro
            "rosy", "riva", "rolex", "ugglite", "ugg", "tiare", "vince", "ysl", // Redmi 5 / 5A / Note 5A / Go / Note 5 / S2
            "santoni", "ido", "land", "prada", "mido", "nikel", // Redmi 4 / 3S / Note 4
            "lavender", "ginkgo", "willow", "whyred", "tulip", "begonia", "merlin" // Redmi Note 5-9
    ));

    /** Reads the setting and works out whether ultra performance mode is on.
     *  Call at startup and after the setting changes.
     */
    public static void update(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String mode = prefs.getString(PerformancePreferenceKey, PERFORMANCE_AUTO);
        switch( mode ) {
            case PERFORMANCE_ULTRA:
                ultra = true;
                break;
            case PERFORMANCE_OFF:
                ultra = false;
                break;
            default:
                ultra = isLowEndDevice(context);
                break;
        }
        if( MyDebug.LOG )
            Log.d(TAG, "mode " + mode + " ultra " + ultra);
    }

    public static boolean isUltra() {
        return ultra;
    }

    /** How many threads to split image processing over: all the cores (Open Camera used a fixed 4).
     */
    public static int processingThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(2, Math.min(8, cores));
    }

    /** Number of frames for a noise reduction burst; in ultra mode we use far fewer.
     */
    public static int nrFrames(int requested) {
        if( !ultra )
            return requested;
        if( requested >= 15 )
            return 6; // night, low light
        if( requested >= 8 )
            return 4; // dark scene
        if( requested >= 4 )
            return 3; // normal scene
        return 2; // bright scene
    }

    /** How much to shrink the images used for aligning NR frames. */
    public static int alignScale(int normal) {
        return ultra ? Math.max(2, normal * 2) : normal;
    }

    /** How many NR frames to decode at once. Fewer on phones short of memory.
     */
    public static int nrDecodeBatch(Context context) {
        return ultra && totalMemoryGB(context) < 4.0 ? 2 : 4;
    }

    public static boolean isLowEndDevice(Context context) {
        if( lowEndDevice == null )
            lowEndDevice = detectLowEndDevice(context);
        return lowEndDevice;
    }

    private static double totalMemoryGB(Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if( am == null )
            return 8.0;
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        return info.totalMem / (1024.0 * 1024.0 * 1024.0);
    }

    private static boolean detectLowEndDevice(Context context) {
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(Locale.US);
        String device = Build.DEVICE == null ? "" : Build.DEVICE.toLowerCase(Locale.US);

        // Google Pixel 5 / 5a and older (Pixel, Pixel 2, 3, 3a, 4, 4a, 5, 5a, any XL / 5G)
        if( manufacturer.equals("google") && model.matches("(?i)Pixel( [2-5]a?)?( XL| 5G| \\(5G\\))?") )
            return true;

        // Samsung Galaxy S7 and older (S3 GT-I93xx, S4 GT-I95xx, S5 SM-G90x, S6 SM-G92x, S7 SM-G93x)
        if( manufacturer.equals("samsung") && model.matches("(?i)(GT-I9[3-5]\\d+.*|SM-G9[0-3]\\d.*)") )
            return true;

        // Redmi 9 / 9A / 9C and older, and Redmi Note 9 and older
        if( OLD_REDMI_DEVICES.contains(device) )
            return true;
        Matcher redmi = Pattern.compile("(?i)redmi (note )?(\\d+)").matcher(model);
        if( redmi.find() ) {
            try {
                if( Integer.parseInt(redmi.group(2)) <= 9 )
                    return true;
            }
            catch(NumberFormatException e) {
                // not a number we understand, carry on
            }
        }

        // anything else: judge by the hardware
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if( am != null && am.isLowRamDevice() )
            return true;
        if( totalMemoryGB(context) < 3.5 )
            return true;
        if( Runtime.getRuntime().availableProcessors() <= 4 )
            return true;
        int maxMHz = maxCpuMHz();
        return maxMHz > 0 && maxMHz < 2000;
    }

    /** Fastest core's top clock in MHz, or -1 if unknown.
     */
    private static int maxCpuMHz() {
        int best = -1;
        for(int i=0;i<16;i++) {
            File file = new File("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
            if( !file.exists() )
                continue;
            try( BufferedReader reader = new BufferedReader(new FileReader(file)) ) {
                String line = reader.readLine();
                if( line != null )
                    best = Math.max(best, Integer.parseInt(line.trim()) / 1000);
            }
            catch(Exception e) {
                // not readable on some phones
            }
        }
        return best;
    }
}
