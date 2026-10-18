package net.sourceforge.opencamera;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraExtensionCharacteristics;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;


import net.sourceforge.opencamera.preview.ApplicationInterface;
import net.sourceforge.opencamera.preview.Preview;
import net.sourceforge.opencamera.ui.ModeBarView;

import java.util.ArrayList;
import java.util.List;

/** Glue between Lenz's mode bar and Open Camera's settings: works out which
 *  modes the current camera supports, which one is active, and switches mode
 *  by setting the same preferences the popup menu uses.
 */
public class ModeController implements ModeBarView.Listener {
    private static final String TAG = "ModeController";

    private static final float TIME_LAPSE_RATE = 10.0f;
    /** Open Camera only keeps the NR mode for the session, so we remember Night mode ourselves. */
    private static final String NightModePreferenceKey = "lenz_night_mode";

    private final MainActivity main_activity;
    private final ModeBarView modeBar;
    private boolean restoredNightMode = false;

    ModeController(MainActivity main_activity) {
        this.main_activity = main_activity;
        this.modeBar = main_activity.findViewById(R.id.mode_bar);
        this.modeBar.setListener(this);
    }

    public ModeBarView getModeBar() {
        return modeBar;
    }

    private boolean supportsExtension(int extension) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && main_activity.supportsCameraExtension(extension);
    }

    private boolean supportsNight() {
        return main_activity.supportsNoiseReduction() || supportsExtension(CameraExtensionCharacteristics.EXTENSION_NIGHT);
    }

    private float slowMotionRate() {
        List<Float> rates = main_activity.getApplicationInterface().getSupportedVideoCaptureRates();
        // rates are in increasing order, so the first is the slowest
        return rates.isEmpty() ? 1.0f : rates.get(0);
    }

    private float timeLapseRate() {
        List<Float> rates = main_activity.getApplicationInterface().getSupportedVideoCaptureRates();
        float best = 1.0f;
        for(float rate : rates) {
            if( Math.abs(rate - TIME_LAPSE_RATE) < 1.0e-5f )
                return rate;
            if( rate > best && rate < TIME_LAPSE_RATE )
                best = rate;
        }
        return best;
    }

    public List<LenzMode> availableModes() {
        List<LenzMode> modes = new ArrayList<>();
        if( PhotoSphereActivity.isSupported(main_activity) )
            modes.add(LenzMode.PHOTO_SPHERE);
        if( main_activity.supportsPanorama() )
            modes.add(LenzMode.PANORAMA);
        if( supportsNight() )
            modes.add(LenzMode.NIGHT);
        if( supportsExtension(CameraExtensionCharacteristics.EXTENSION_BOKEH) )
            modes.add(LenzMode.PORTRAIT);
        modes.add(LenzMode.PHOTO);
        modes.add(LenzMode.VIDEO);
        if( slowMotionRate() < 1.0f - 1.0e-5f )
            modes.add(LenzMode.SLOW_MOTION);
        if( timeLapseRate() > 1.0f + 1.0e-5f )
            modes.add(LenzMode.TIME_LAPSE);
        return modes;
    }

    /** Works out the current mode from Open Camera's settings, so the bar stays
     *  right if the user changes mode another way (e.g. the popup menu).
     */
    public LenzMode currentMode() {
        MyApplicationInterface app = main_activity.getApplicationInterface();
        if( main_activity.getPreview().isVideo() ) {
            float rate = app.getVideoCaptureRateFactor();
            if( rate < 1.0f - 1.0e-5f )
                return LenzMode.SLOW_MOTION;
            if( rate > 1.0f + 1.0e-5f )
                return LenzMode.TIME_LAPSE;
            return LenzMode.VIDEO;
        }
        switch( app.getPhotoMode() ) {
            case Panorama:
                return LenzMode.PANORAMA;
            case X_Bokeh:
                return LenzMode.PORTRAIT;
            case X_Night:
                return LenzMode.NIGHT;
            case NoiseReduction:
                return app.getNRModePref() == ApplicationInterface.NRModePref.NRMODE_LOW_LIGHT ? LenzMode.NIGHT : LenzMode.PHOTO;
            default:
                return LenzMode.PHOTO;
        }
    }

    /** Updates the bar for the current camera; call after the camera is (re)opened.
     */
    public void refresh() {
        if( main_activity.getPreview().getCameraController() == null )
            return;
        if( !restoredNightMode ) {
            restoredNightMode = true;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
            if( sharedPreferences.getBoolean(NightModePreferenceKey, false) )
                main_activity.getApplicationInterface().setNRMode("preference_nr_mode_low_light");
        }
        modeBar.setModes(availableModes(), currentMode());
    }

    /** Called for a swipe on the camera preview.
     */
    public void onPreviewSwipe(float dx, float dy) {
        if( modeBar.getVisibility() != android.view.View.VISIBLE )
            return;
        float min = 60 * main_activity.getResources().getDisplayMetrics().density;
        boolean portrait = main_activity.getSystemOrientation() == MainActivity.SystemOrientation.PORTRAIT;
        float along = portrait ? dx : dy;
        float across = portrait ? dy : dx;
        if( Math.abs(along) < min || Math.abs(along) < 1.5f * Math.abs(across) )
            return;
        // swiping towards the start of the bar reveals the next mode, like Gcam
        modeBar.step(along < 0 ? 1 : -1);
    }

    @Override
    public void onModeSelected(LenzMode mode) {
        if( MyDebug.LOG )
            Log.d(TAG, "onModeSelected: " + mode);
        Preview preview = main_activity.getPreview();
        if( preview.isVideoRecording() || preview.isTakingPhotoOrOnTimer() ) {
            refresh();
            return;
        }
        if( mode == LenzMode.PHOTO_SPHERE ) {
            main_activity.startActivity(new Intent(main_activity, PhotoSphereActivity.class));
            // keep the bar on the mode we came from
            refresh();
            return;
        }

        MyApplicationInterface app = main_activity.getApplicationInterface();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        float captureRate = 1.0f;
        if( mode == LenzMode.SLOW_MOTION )
            captureRate = slowMotionRate();
        else if( mode == LenzMode.TIME_LAPSE )
            captureRate = timeLapseRate();
        editor.putFloat(PreferenceKeys.getVideoCaptureRatePreferenceKey(preview.getCameraId(), app.getCameraIdSPhysicalPref()), captureRate);

        if( !mode.isVideo ) {
            String photoMode;
            switch( mode ) {
                case PANORAMA:
                    photoMode = "preference_photo_mode_panorama";
                    break;
                case PORTRAIT:
                    photoMode = "preference_photo_mode_x_bokeh";
                    break;
                case NIGHT:
                    photoMode = main_activity.supportsNoiseReduction() ? "preference_photo_mode_noise_reduction" : "preference_photo_mode_x_night";
                    break;
                default:
                    photoMode = main_activity.supportsNoiseReduction() ? "preference_photo_mode_noise_reduction" : "preference_photo_mode_std";
                    break;
            }
            editor.putString(PreferenceKeys.PhotoModePreferenceKey, photoMode);
            app.setNRMode(mode == LenzMode.NIGHT ? "preference_nr_mode_low_light" : "preference_nr_mode_normal");
            editor.putBoolean(NightModePreferenceKey, mode == LenzMode.NIGHT);
        }
        editor.apply();

        app.getDrawPreview().updateSettings(); // the photo mode is cached
        String toast = main_activity.getResources().getString(mode.labelRes);
        if( mode.isVideo != preview.isVideo() ) {
            // also reopens the camera with the new settings
            main_activity.clickedSwitchVideo(null);
            if( mode != LenzMode.VIDEO )
                main_activity.updateForSettings(true, toast, false, true);
        }
        else {
            main_activity.updateForSettings(true, toast, false, true);
        }
    }
}
