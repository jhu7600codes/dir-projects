package net.sourceforge.opencamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraControl;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lenz Photo Sphere: guides the user to point the camera in every direction,
 *  takes a photo at each target (with the phone's orientation from the
 *  rotation vector sensor), then stitches them into a 360 equirectangular
 *  image that Google Photos and other viewers show as a sphere.
 */
@SuppressLint("UnsafeOptInUsageError")
public class PhotoSphereActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "PhotoSphereActivity";

    /** Max angle between the camera and a target for an automatic shot. */
    private static final float CAPTURE_ANGLE_DEG = 3.0f;
    /** Max rotation speed (degrees per second) for an automatic shot, to avoid blur. */
    private static final float MAX_SPEED_DEG = 12.0f;
    /** Long side of the photos we keep for stitching. */
    private static final int SHOT_SIZE = 1600;

    static class Target {
        final float[] dir; // unit vector in world coordinates (x east, y north, z up)
        boolean done;

        Target(float[] dir) {
            this.dir = dir;
        }
    }

    static class Shot {
        final File file;
        final float[] rotation; // device to world rotation matrix, row major
        final Target target;

        Shot(File file, float[] rotation, Target target) {
            this.file = file;
            this.rotation = rotation;
            this.target = target;
        }
    }

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private final float[] rotation = new float[9];
    private boolean haveRotation = false;
    private final float[] lastLook = new float[3];
    private long lastLookTime = 0;
    private float speedDeg = 0.0f;

    private final List<Target> targets = new ArrayList<>();
    private final List<Shot> shots = new ArrayList<>();
    private boolean targetsCreated = false;
    private boolean capturing = false;
    private boolean stitching = false;

    // camera field of view for a portrait photo, as tangents of the half angles
    private float tanHalfH = 0.45f;
    private float tanHalfV = 0.6f;

    private PreviewView previewView;
    private SphereOverlayView overlay;
    private TextView progressText;
    private TextView hintText;
    private View stitchingPanel;
    private ImageCapture imageCapture;
    private Camera camera;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private File shotDir;

    /** Whether the device has the sensors needed for Photo Sphere.
     */
    public static boolean isSupported(Context context) {
        SensorManager sm = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        return sm != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE) &&
                (sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null || sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if( rotationSensor == null )
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        shotDir = new File(getCacheDir(), "sphere");
        deleteShotFiles();
        //noinspection ResultOfMethodCallIgnored
        shotDir.mkdirs();

        buildUI();
        startCamera();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makePill(String text, boolean filled) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(22), dp(12), dp(22), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        if( filled ) {
            bg.setColor(ContextCompat.getColor(this, R.color.lenz_accent));
            view.setTextColor(ContextCompat.getColor(this, R.color.lenz_on_accent));
        }
        else {
            bg.setColor(ContextCompat.getColor(this, R.color.lenz_surface));
            view.setTextColor(ContextCompat.getColor(this, R.color.lenz_on_surface));
        }
        view.setBackground(bg);
        return view;
    }

    private void buildUI() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        previewView = new PreviewView(this);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        overlay = new SphereOverlayView(this);
        root.addView(overlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        hintText = makePill(getString(R.string.sphere_hint), false);
        hintText.setTextSize(14);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        hintParams.setMargins(dp(24), dp(48), dp(24), 0);
        root.addView(hintText, hintParams);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        TextView undo = makePill(getString(R.string.sphere_undo), false);
        undo.setOnClickListener(v -> undoShot());
        progressText = makePill("", false);
        TextView done = makePill(getString(R.string.sphere_done), true);
        done.setOnClickListener(v -> finishSphere());
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemParams.setMargins(dp(8), 0, dp(8), 0);
        bar.addView(undo, itemParams);
        bar.addView(progressText, itemParams);
        bar.addView(done, itemParams);
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        barParams.setMargins(0, 0, 0, dp(56));
        root.addView(bar, barParams);

        TextView stitchingText = makePill(getString(R.string.sphere_stitching), true);
        FrameLayout panel = new FrameLayout(this);
        panel.setBackgroundColor(0xcc000000);
        panel.addView(stitchingText, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        panel.setVisibility(View.GONE);
        panel.setClickable(true);
        stitchingPanel = panel;
        root.addView(panel, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);
        updateProgress();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                ResolutionSelector selector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(new Size(2016, 1512), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();
                Preview preview = new Preview.Builder().setResolutionSelector(new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setResolutionSelector(selector)
                        .setTargetRotation(android.view.Surface.ROTATION_0)
                        .build();
                provider.unbindAll();
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
                readFieldOfView();
            }
            catch(Exception e) {
                Log.e(TAG, "failed to start camera", e);
                Toast.makeText(this, R.string.sphere_no_camera, Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void readFieldOfView() {
        Camera2CameraInfo info = Camera2CameraInfo.from(camera.getCameraInfo());
        android.util.SizeF sensorSize = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        float[] focalLengths = info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if( sensorSize != null && focalLengths != null && focalLengths.length > 0 && focalLengths[0] > 0 ) {
            float f = focalLengths[0];
            float longSide = Math.max(sensorSize.getWidth(), sensorSize.getHeight());
            // the photos are 4:3, which may crop a wider sensor
            float shortSide = Math.min(Math.min(sensorSize.getWidth(), sensorSize.getHeight()), longSide * 3.0f / 4.0f);
            longSide = Math.min(longSide, shortSide * 4.0f / 3.0f);
            // held in portrait: the short side of the sensor is horizontal
            tanHalfH = (shortSide / 2.0f) / f;
            tanHalfV = (longSide / 2.0f) / f;
        }
        if( MyDebug.LOG )
            Log.d(TAG, "fov h " + Math.toDegrees(2 * Math.atan(tanHalfH)) + " v " + Math.toDegrees(2 * Math.atan(tanHalfV)));
    }

    private void lockExposure() {
        // keep exposure and white balance the same across shots so the seams match
        try {
            Camera2CameraControl.from(camera.getCameraControl()).setCaptureRequestOptions(new CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                    .build());
        }
        catch(Exception e) {
            Log.e(TAG, "failed to lock exposure", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( rotationSensor != null )
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if( !stitching )
            deleteShotFiles();
    }

    private void deleteShotFiles() {
        File[] files = shotDir.listFiles();
        if( files != null ) {
            for(File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    static float[] dirFromAngles(double yaw, double pitch) {
        return new float[]{
                (float)(Math.cos(pitch) * Math.sin(yaw)),
                (float)(Math.cos(pitch) * Math.cos(yaw)),
                (float)Math.sin(pitch)
        };
    }

    /** Lays out the targets around the direction the camera faces when we start.
     */
    private void createTargets(float[] look) {
        double yaw0 = Math.atan2(look[0], look[1]);
        int[][] rows = {
                // pitch degrees, number of shots around
                {0, 12},
                {45, 8},
                {-45, 8},
                {90, 1},
                {-90, 1},
        };
        for(int[] row : rows) {
            double pitch = Math.toRadians(row[0]);
            for(int i=0;i<row[1];i++) {
                double yaw = yaw0 + 2.0 * Math.PI * i / row[1];
                targets.add(new Target(dirFromAngles(yaw, pitch)));
            }
        }
        targetsCreated = true;
        updateProgress();
    }

    /** Returns the camera's viewing direction in world coordinates.
     */
    private static void lookDir(float[] r, float[] out) {
        // the back camera looks along -z in device coordinates
        out[0] = -r[2];
        out[1] = -r[5];
        out[2] = -r[8];
    }

    private static float dot(float[] a, float[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(rotation, event.values);
        haveRotation = true;
        float[] look = new float[3];
        lookDir(rotation, look);
        long now = event.timestamp;
        if( lastLookTime != 0 ) {
            float dt = (now - lastLookTime) / 1.0e9f;
            if( dt > 0 ) {
                float angle = (float)Math.toDegrees(Math.acos(Math.max(-1.0f, Math.min(1.0f, dot(look, lastLook)))));
                // smooth the speed a little
                speedDeg = 0.7f * speedDeg + 0.3f * (angle / dt);
            }
        }
        System.arraycopy(look, 0, lastLook, 0, 3);
        lastLookTime = now;

        if( !targetsCreated && camera != null )
            createTargets(look);
        overlay.invalidate();
        if( !targetsCreated || capturing || stitching || imageCapture == null )
            return;

        Target nearest = nearestTarget(look);
        if( nearest != null ) {
            float angle = (float)Math.toDegrees(Math.acos(Math.min(1.0f, dot(look, nearest.dir))));
            if( angle < CAPTURE_ANGLE_DEG && speedDeg < MAX_SPEED_DEG )
                takeShot(nearest);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private Target nearestTarget(float[] look) {
        Target best = null;
        float bestDot = -2.0f;
        for(Target target : targets) {
            if( target.done )
                continue;
            float d = dot(look, target.dir);
            if( d > bestDot ) {
                bestDot = d;
                best = target;
            }
        }
        return best;
    }

    private void takeShot(final Target target) {
        capturing = true;
        final float[] shotRotation = rotation.clone();
        overlay.flash();
        imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                File file = new File(shotDir, "shot_" + System.nanoTime() + ".jpg");
                boolean ok = false;
                try {
                    ok = saveShot(image, file);
                }
                catch(Exception e) {
                    Log.e(TAG, "failed to save shot", e);
                }
                finally {
                    image.close();
                }
                final boolean saved = ok;
                handler.post(() -> {
                    capturing = false;
                    if( saved ) {
                        if( shots.isEmpty() )
                            lockExposure();
                        target.done = true;
                        shots.add(new Shot(file, shotRotation, target));
                        updateProgress();
                        if( allDone() )
                            finishSphere();
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "capture failed", exception);
                handler.post(() -> capturing = false);
            }
        });
    }

    /** Decodes the captured JPEG, scales it down, turns it upright (portrait) and saves it.
     */
    private boolean saveShot(ImageProxy image, File file) throws IOException {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        int sample = 1;
        while( Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SHOT_SIZE )
            sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if( bitmap == null )
            return false;
        int longSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        float scale = longSide > SHOT_SIZE ? SHOT_SIZE / (float)longSide : 1.0f;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        Bitmap upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if( upright != bitmap )
            bitmap.recycle();
        try( FileOutputStream out = new FileOutputStream(file) ) {
            upright.compress(Bitmap.CompressFormat.JPEG, 92, out);
        }
        upright.recycle();
        return true;
    }

    private boolean allDone() {
        for(Target target : targets) {
            if( !target.done )
                return false;
        }
        return !targets.isEmpty();
    }

    private void undoShot() {
        if( shots.isEmpty() || capturing || stitching )
            return;
        Shot shot = shots.remove(shots.size() - 1);
        shot.target.done = false;
        //noinspection ResultOfMethodCallIgnored
        shot.file.delete();
        updateProgress();
    }

    private void updateProgress() {
        int done = 0;
        for(Target target : targets) {
            if( target.done )
                done++;
        }
        progressText.setText(getString(R.string.sphere_progress, done, targets.size()));
        hintText.setVisibility(done == 0 ? View.VISIBLE : View.GONE);
        overlay.invalidate();
    }

    private void finishSphere() {
        if( stitching || capturing )
            return;
        if( shots.size() < 3 ) {
            Toast.makeText(this, R.string.sphere_need_more, Toast.LENGTH_SHORT).show();
            return;
        }
        stitching = true;
        sensorManager.unregisterListener(this);
        stitchingPanel.setVisibility(View.VISIBLE);
        final List<Shot> toStitch = new ArrayList<>(shots);
        final float th = tanHalfH;
        final float tv = tanHalfV;
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            boolean ok = false;
            try {
                ok = SphereStitcher.stitchAndSave(appContext, toStitch, th, tv);
            }
            catch(Throwable e) {
                Log.e(TAG, "stitching failed", e);
            }
            final boolean saved = ok;
            for(Shot shot : toStitch) {
                //noinspection ResultOfMethodCallIgnored
                shot.file.delete();
            }
            handler.post(() -> {
                Toast.makeText(appContext, saved ? R.string.sphere_saved : R.string.sphere_failed, Toast.LENGTH_LONG).show();
                if( !isFinishing() )
                    finish();
            });
        }).start();
    }

    /** Draws the targets, the centre reticle, and an arrow towards the next target.
     */
    private class SphereOverlayView extends View {
        private final android.graphics.Paint dotPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint donePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint ringPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint flashPaint = new android.graphics.Paint();
        private long flashTime = 0;
        private final float[] look = new float[3];

        SphereOverlayView(Context context) {
            super(context);
            int accent = ContextCompat.getColor(context, R.color.lenz_accent);
            dotPaint.setColor(accent);
            dotPaint.setStyle(android.graphics.Paint.Style.FILL);
            dotPaint.setShadowLayer(dp(4), 0, 0, 0x80000000);
            donePaint.setColor(0x66ffffff);
            ringPaint.setColor(Color.WHITE);
            ringPaint.setStyle(android.graphics.Paint.Style.STROKE);
            ringPaint.setStrokeWidth(dp(3));
            ringPaint.setShadowLayer(dp(3), 0, 0, 0x80000000);
            flashPaint.setColor(Color.WHITE);
            setLayerType(LAYER_TYPE_SOFTWARE, null); // for the shadows
        }

        void flash() {
            flashTime = System.currentTimeMillis();
            invalidate();
        }

        /** Projects a world direction to screen coordinates; returns false if it's behind the camera.
         */
        private boolean project(float[] dir, float[] out) {
            // device = R^T * world
            float dx = rotation[0]*dir[0] + rotation[3]*dir[1] + rotation[6]*dir[2];
            float dy = rotation[1]*dir[0] + rotation[4]*dir[1] + rotation[7]*dir[2];
            float dz = rotation[2]*dir[0] + rotation[5]*dir[1] + rotation[8]*dir[2];
            // the preview fills the screen height, so scale by the vertical field of view
            float k = (getHeight() / 2.0f) / tanHalfV;
            if( dz >= -1.0e-3f ) {
                out[0] = dx;
                out[1] = -dy;
                return false;
            }
            out[0] = getWidth() / 2.0f + dx / -dz * k;
            out[1] = getHeight() / 2.0f - dy / -dz * k;
            return true;
        }

        @Override
        protected void onDraw(@NonNull android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2.0f;
            float cy = getHeight() / 2.0f;
            if( haveRotation && targetsCreated ) {
                lookDir(rotation, look);
                float[] p = new float[2];
                for(Target target : targets) {
                    if( project(target.dir, p) && p[0] > -dp(20) && p[0] < getWidth() + dp(20) && p[1] > -dp(20) && p[1] < getHeight() + dp(20) )
                        canvas.drawCircle(p[0], p[1], target.done ? dp(5) : dp(11), target.done ? donePaint : dotPaint);
                }
                Target next = nearestTarget(look);
                if( next != null ) {
                    boolean inFront = project(next.dir, p);
                    boolean onScreen = inFront && p[0] >= 0 && p[0] <= getWidth() && p[1] >= 0 && p[1] <= getHeight();
                    if( !onScreen ) {
                        // arrow at the edge of the reticle pointing at the next target
                        float vx = inFront ? p[0] - cx : p[0];
                        float vy = inFront ? p[1] - cy : p[1];
                        float len = (float)Math.sqrt(vx*vx + vy*vy);
                        if( len > 1.0e-5f ) {
                            vx /= len;
                            vy /= len;
                            float r = dp(70);
                            float ax = cx + vx * r;
                            float ay = cy + vy * r;
                            android.graphics.Path path = new android.graphics.Path();
                            path.moveTo(ax + vx * dp(16), ay + vy * dp(16));
                            path.lineTo(ax - vy * dp(10), ay + vx * dp(10));
                            path.lineTo(ax + vy * dp(10), ay - vx * dp(10));
                            path.close();
                            canvas.drawPath(path, dotPaint);
                        }
                    }
                }
            }
            canvas.drawCircle(cx, cy, dp(30), ringPaint);

            long since = System.currentTimeMillis() - flashTime;
            if( since < 200 ) {
                flashPaint.setAlpha((int)(160 * (1.0f - since / 200.0f)));
                canvas.drawRect(0, 0, getWidth(), getHeight(), flashPaint);
                postInvalidateOnAnimation();
            }
        }
    }
}
