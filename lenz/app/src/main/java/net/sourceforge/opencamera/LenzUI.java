package net.sourceforge.opencamera;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import net.sourceforge.opencamera.preview.Preview;
import net.sourceforge.opencamera.ui.ShutterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lenz's camera UI. The camera engine underneath still creates its old
 *  controls (they hold state it relies on), but they're kept hidden and
 *  everything the user sees and touches is here.
 */
public class LenzUI {
    /** Engine controls that must never be shown. */
    private static final int[] LEGACY_VIEWS = {
            R.id.take_photo, R.id.switch_camera, R.id.switch_multi_camera, R.id.pause_video,
            R.id.cancel_panorama, R.id.switch_video, R.id.take_photo_when_video_recording,
            R.id.zoom_seekbar, R.id.focus_seekbar, R.id.focus_bracketing_target_seekbar,
            R.id.gallery, R.id.settings, R.id.popup, R.id.exposure, R.id.exposure_lock,
            R.id.white_balance_lock, R.id.cycle_raw, R.id.store_location, R.id.text_stamp,
            R.id.stamp, R.id.focus_peaking, R.id.auto_level, R.id.cycle_flash, R.id.face_detection,
            R.id.audio_control, R.id.cycle_lock_orientation, R.id.preview_shots, R.id.kraken_icon,
            R.id.trash, R.id.share, R.id.sliders_container, R.id.popup_container,
    };

    private static final String[] PHOTO_FLASH_CYCLE = {"flash_auto", "flash_on", "flash_off"};
    private static final String[] VIDEO_FLASH_CYCLE = {"flash_off", "flash_torch"};
    private static final String[] TIMER_CYCLE = {"0", "3", "10"};
    private static final float[] ZOOM_STOPS = {0.5f, 1.0f, 2.0f, 5.0f};

    private final MainActivity main_activity;
    private final View root;
    private final ImageButton flashButton;
    private final ImageButton timerButton;
    private final TextView timerLabel;
    private final ImageButton gridButton;
    private final ImageButton settingsButton;
    private final ImageView thumbnail;
    private final ShutterButton shutter;
    private final ImageButton flipButton;
    private final LinearLayout zoomChips;
    private final TextView recChip;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OrientationEventListener orientationListener;
    private int iconRotation = 0;
    private final List<Float> zoomStops = new ArrayList<>();
    private int statusBarTop = 0;

    private final Runnable recTick = new Runnable() {
        @Override
        public void run() {
            updateRecChip();
            if( main_activity.getPreview().isVideoRecording() )
                handler.postDelayed(this, 500);
        }
    };

    LenzUI(MainActivity main_activity) {
        this.main_activity = main_activity;
        root = main_activity.findViewById(R.id.lenz_ui);
        flashButton = main_activity.findViewById(R.id.lenz_flash);
        timerButton = main_activity.findViewById(R.id.lenz_timer);
        timerLabel = main_activity.findViewById(R.id.lenz_timer_label);
        gridButton = main_activity.findViewById(R.id.lenz_grid);
        settingsButton = main_activity.findViewById(R.id.lenz_settings);
        thumbnail = main_activity.findViewById(R.id.lenz_thumbnail);
        shutter = main_activity.findViewById(R.id.lenz_shutter);
        flipButton = main_activity.findViewById(R.id.lenz_flip);
        zoomChips = main_activity.findViewById(R.id.lenz_zoom_chips);
        recChip = main_activity.findViewById(R.id.lenz_rec);

        shutter.setOnClickListener(v -> {
            if( !main_activity.getPreview().isVideo() )
                shutter.pulse();
            main_activity.clickedTakePhoto(v);
        });
        flipButton.setOnClickListener(v -> {
            v.animate().rotationBy(180).setDuration(300).start();
            main_activity.clickedSwitchCamera(v);
        });
        thumbnail.setOnClickListener(main_activity::clickedGallery);
        flashButton.setOnClickListener(v -> cycleFlash());
        timerButton.setOnClickListener(v -> cycleTimer());
        gridButton.setOnClickListener(v -> toggleGrid());
        settingsButton.setOnClickListener(v -> main_activity.openSettings());

        // keep the top bar below the status bar, and the preview below the top bar
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars() | androidx.core.view.WindowInsetsCompat.Type.displayCutout()).top;
            if( top != statusBarTop ) {
                statusBarTop = top;
                applyTopInset();
            }
            return insets;
        });
        applyTopInset();

        // keep the engine's own controls hidden whatever it does with them
        View content = main_activity.findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnGlobalLayoutListener(this::hideLegacyViews);
        hideLegacyViews();

        // the UI stays portrait like Gcam; icons turn to follow the phone
        orientationListener = new OrientationEventListener(main_activity) {
            @Override
            public void onOrientationChanged(int orientation) {
                if( orientation == ORIENTATION_UNKNOWN )
                    return;
                int snapped = ((orientation + 45) / 90 * 90) % 360;
                int rotation = (360 - snapped) % 360;
                if( rotation != iconRotation ) {
                    iconRotation = rotation;
                    rotateIcons();
                }
            }
        };
    }

    private int dp(float value) {
        return (int)(value * main_activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void applyTopInset() {
        View topBar = main_activity.findViewById(R.id.lenz_top_bar);
        topBar.setPadding(topBar.getPaddingLeft(), statusBarTop + dp(6), topBar.getPaddingRight(), dp(6));
        View rec = recChip;
        android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams)rec.getLayoutParams();
        params.topMargin = getTopBarBottom() + dp(12);
        rec.setLayoutParams(params);
        if( main_activity.getMainUI() != null && main_activity.getPreview() != null )
            main_activity.getMainUI().layoutUI();
    }

    /** Where the top bar ends; the preview starts here.
     */
    public int getTopBarBottom() {
        // padding above and below plus the 48dp icons
        return statusBarTop + dp(6 + 48 + 6);
    }

    private void hideLegacyViews() {
        for(int id : LEGACY_VIEWS) {
            View view = main_activity.findViewById(id);
            if( view != null && view.getVisibility() != View.GONE )
                view.setVisibility(View.GONE);
        }
    }

    void onResume() {
        if( orientationListener.canDetectOrientation() )
            orientationListener.enable();
        refresh();
    }

    void onPause() {
        orientationListener.disable();
        handler.removeCallbacks(recTick);
    }

    private void rotateIcons() {
        // the view's rotation is clockwise, the device turning anticlockwise needs the icons turned back
        float target = iconRotation > 180 ? iconRotation - 360 : iconRotation;
        View[] views = {flashButton, timerButton, timerLabel, gridButton, settingsButton, thumbnail, flipButton};
        for(View view : views)
            view.animate().rotation(target).setDuration(250).start();
        for(int i=0;i<zoomChips.getChildCount();i++)
            zoomChips.getChildAt(i).animate().rotation(target).setDuration(250).start();
    }

    /** Updates everything that depends on the camera or mode; call after the camera opens or the mode changes.
     */
    public void refresh() {
        Preview preview = main_activity.getPreview();
        if( preview.isVideoRecording() )
            shutter.setState(ShutterButton.State.RECORDING);
        else if( preview.isVideo() )
            shutter.setState(ShutterButton.State.VIDEO);
        else
            shutter.setState(ShutterButton.State.PHOTO);
        boolean recording = preview.isVideoRecording();
        int controls = recording ? View.INVISIBLE : View.VISIBLE;
        flipButton.setVisibility(preview.getCameraControllerManager().getNumberOfCameras() > 1 ? controls : View.INVISIBLE);
        thumbnail.setVisibility(controls);
        settingsButton.setVisibility(controls);
        timerButton.setVisibility(controls);
        timerLabel.setVisibility(controls);
        updateTopBar();
        buildZoomChips();
        if( recording ) {
            recChip.setVisibility(View.VISIBLE);
            handler.removeCallbacks(recTick);
            handler.post(recTick);
        }
        else {
            recChip.setVisibility(View.GONE);
            handler.removeCallbacks(recTick);
        }
    }

    private void updateRecChip() {
        Preview preview = main_activity.getPreview();
        if( !preview.isVideoRecording() ) {
            recChip.setVisibility(View.GONE);
            return;
        }
        long seconds = preview.getVideoTime(false) / 1000;
        String time = seconds >= 3600 ?
                String.format(Locale.US, "%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60) :
                String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
        recChip.setText("●  " + time);
    }

    // flash

    private void cycleFlash() {
        Preview preview = main_activity.getPreview();
        List<String> supported = preview.getSupportedFlashValues();
        if( supported == null || supported.isEmpty() )
            return;
        String[] cycle = preview.isVideo() ? VIDEO_FLASH_CYCLE : PHOTO_FLASH_CYCLE;
        String current = preview.getCurrentFlashValue();
        int index = 0;
        for(int i=0;i<cycle.length;i++) {
            if( cycle[i].equals(current) ) {
                index = i;
                break;
            }
        }
        for(int step=1;step<=cycle.length;step++) {
            String next = cycle[(index + step) % cycle.length];
            if( supported.contains(next) ) {
                preview.updateFlash(next);
                break;
            }
        }
        updateTopBar();
    }

    private void updateTopBar() {
        Preview preview = main_activity.getPreview();
        List<String> supported = preview.getSupportedFlashValues();
        boolean hasFlash = supported != null && supported.size() > 1;
        flashButton.setVisibility(hasFlash ? View.VISIBLE : View.INVISIBLE);
        String flash = preview.getCurrentFlashValue();
        int icon = R.drawable.ic_lenz_flash_off;
        if( flash != null ) {
            switch( flash ) {
                case "flash_auto":
                case "flash_frontscreen_auto":
                    icon = R.drawable.ic_lenz_flash_auto;
                    break;
                case "flash_on":
                case "flash_red_eye":
                case "flash_frontscreen_on":
                    icon = R.drawable.ic_lenz_flash_on;
                    break;
                case "flash_torch":
                case "flash_frontscreen_torch":
                    icon = R.drawable.ic_lenz_flash_torch;
                    break;
            }
        }
        flashButton.setImageResource(icon);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main_activity);
        String timer = prefs.getString(PreferenceKeys.TimerPreferenceKey, "0");
        boolean timerOn = !"0".equals(timer);
        timerButton.setAlpha(timerOn ? 1.0f : 0.6f);
        timerLabel.setText(timerOn ? timer + "s" : "");

        boolean gridOn = !"preference_grid_none".equals(prefs.getString(PreferenceKeys.ShowGridPreferenceKey, "preference_grid_none"));
        gridButton.setAlpha(gridOn ? 1.0f : 0.6f);
    }

    // timer and grid

    private void cycleTimer() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main_activity);
        String timer = prefs.getString(PreferenceKeys.TimerPreferenceKey, "0");
        int index = 0;
        for(int i=0;i<TIMER_CYCLE.length;i++) {
            if( TIMER_CYCLE[i].equals(timer) )
                index = i;
        }
        prefs.edit().putString(PreferenceKeys.TimerPreferenceKey, TIMER_CYCLE[(index + 1) % TIMER_CYCLE.length]).apply();
        updateTopBar();
    }

    private void toggleGrid() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main_activity);
        boolean gridOn = !"preference_grid_none".equals(prefs.getString(PreferenceKeys.ShowGridPreferenceKey, "preference_grid_none"));
        prefs.edit().putString(PreferenceKeys.ShowGridPreferenceKey, gridOn ? "preference_grid_none" : "preference_grid_3x3").apply();
        main_activity.getApplicationInterface().getDrawPreview().updateSettings();
        updateTopBar();
    }

    // zoom

    private void buildZoomChips() {
        zoomChips.removeAllViews();
        zoomStops.clear();
        Preview preview = main_activity.getPreview();
        if( preview.getCameraController() == null || !preview.supportsZoom() ) {
            zoomChips.setVisibility(View.GONE);
            return;
        }
        float min = preview.getZoomRatio(0);
        float max = preview.getZoomRatio(preview.getMaxZoom());
        for(float stop : ZOOM_STOPS) {
            if( stop >= min - 0.01f && stop <= max + 0.01f )
                zoomStops.add(stop);
        }
        if( zoomStops.size() < 2 ) {
            zoomChips.setVisibility(View.GONE);
            return;
        }
        zoomChips.setVisibility(View.VISIBLE);
        float density = main_activity.getResources().getDisplayMetrics().density;
        for(final float stop : zoomStops) {
            TextView chip = new TextView(main_activity);
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(12);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            int size = (int)(32 * density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins((int)(2 * density), 0, (int)(2 * density), 0);
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> zoomToRatio(stop));
            chip.setRotation(iconRotation > 180 ? iconRotation - 360 : iconRotation);
            zoomChips.addView(chip);
        }
        updateZoom();
    }

    private void zoomToRatio(float ratio) {
        Preview preview = main_activity.getPreview();
        int best = 0;
        float bestDiff = Float.MAX_VALUE;
        for(int i=0;i<=preview.getMaxZoom();i++) {
            float diff = Math.abs(preview.getZoomRatio(i) - ratio);
            if( diff < bestDiff ) {
                bestDiff = diff;
                best = i;
            }
        }
        preview.zoomTo(best, true, true);
        handler.postDelayed(this::updateZoom, 50);
    }

    /** Highlights the zoom chip for the current zoom; the active chip shows the exact zoom (e.g. 1.4x).
     */
    public void updateZoom() {
        Preview preview = main_activity.getPreview();
        if( zoomStops.isEmpty() || preview.getCameraController() == null )
            return;
        float zoom = preview.getZoomRatio();
        int active = 0;
        for(int i=0;i<zoomStops.size();i++) {
            if( zoom >= zoomStops.get(i) - 0.05f )
                active = i;
        }
        int accent = ContextCompat.getColor(main_activity, R.color.lenz_accent);
        for(int i=0;i<zoomChips.getChildCount();i++) {
            TextView chip = (TextView)zoomChips.getChildAt(i);
            boolean isActive = i == active;
            float value = isActive ? zoom : zoomStops.get(i);
            String text = (Math.abs(value - Math.round(value)) < 0.05f) ?
                    String.format(Locale.US, "%d", Math.round(value)) :
                    String.format(Locale.US, "%.1f", value);
            if( value < 1.0f )
                text = String.format(Locale.US, ".%d", Math.round(value * 10));
            chip.setText(isActive ? text + "x" : text);
            chip.setTextColor(isActive ? accent : 0xffffffff);
            chip.setBackground(isActive ? ContextCompat.getDrawable(main_activity, R.drawable.lenz_chip_active) : null);
        }
    }

    // gallery thumbnail

    void setThumbnail(Bitmap bitmap) {
        if( bitmap == null ) {
            thumbnail.setImageDrawable(null);
            return;
        }
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(main_activity.getResources(), bitmap);
        drawable.setCircular(true);
        thumbnail.setImageDrawable(drawable);
    }

    View getRoot() {
        return root;
    }
}
