package net.sourceforge.opencamera;

/** The camera modes shown in Lenz's swipeable mode bar.
 */
public enum LenzMode {
    PHOTO_SPHERE(R.string.lenz_mode_photo_sphere, false),
    PANORAMA(R.string.lenz_mode_panorama, false),
    NIGHT(R.string.lenz_mode_night, false),
    PORTRAIT(R.string.lenz_mode_portrait, false),
    PHOTO(R.string.lenz_mode_photo, false),
    VIDEO(R.string.lenz_mode_video, true),
    SLOW_MOTION(R.string.lenz_mode_slow_motion, true),
    TIME_LAPSE(R.string.lenz_mode_time_lapse, true);

    public final int labelRes;
    public final boolean isVideo;

    LenzMode(int labelRes, boolean isVideo) {
        this.labelRes = labelRes;
        this.isVideo = isVideo;
    }
}
