package net.sourceforge.opencamera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Stitches Photo Sphere shots into an equirectangular 360 image.
 *  Each shot is placed using the phone's orientation when it was taken, and
 *  overlapping shots are blended with weights that fall off towards the
 *  edges of each photo. The result gets Google's GPano XMP metadata so
 *  viewers show it as a sphere.
 */
class SphereStitcher {
    private static final String TAG = "SphereStitcher";

    static boolean stitchAndSave(Context context, List<PhotoSphereActivity.Shot> shots, float tanHalfH, float tanHalfV) throws IOException {
        long maxMemory = Runtime.getRuntime().maxMemory();
        // the accumulation buffers take 16 bytes per output pixel
        final int width = maxMemory >= 400L * 1024 * 1024 ? 4096 : 2048;
        final int height = width / 2;
        final float[] accR = new float[width * height];
        final float[] accG = new float[width * height];
        final float[] accB = new float[width * height];
        final float[] accW = new float[width * height];

        // centre the panorama on the first shot
        float[] r0 = shots.get(0).rotation;
        final double yaw0 = Math.atan2(-r0[2], -r0[5]);

        // direction of each output pixel, precomputed per row and column
        final float[] cosPitch = new float[height];
        final float[] sinPitch = new float[height];
        for(int j=0;j<height;j++) {
            double pitch = Math.PI / 2.0 - (j + 0.5) * Math.PI / height;
            cosPitch[j] = (float)Math.cos(pitch);
            sinPitch[j] = (float)Math.sin(pitch);
        }
        final float[] sinYaw = new float[width];
        final float[] cosYaw = new float[width];
        for(int i=0;i<width;i++) {
            double yaw = yaw0 + ((i + 0.5) / width - 0.5) * 2.0 * Math.PI;
            sinYaw[i] = (float)Math.sin(yaw);
            cosYaw[i] = (float)Math.cos(yaw);
        }

        final float maxAngle = (float)Math.atan(Math.sqrt(tanHalfH * tanHalfH + tanHalfV * tanHalfV));
        final int threads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));

        for(PhotoSphereActivity.Shot shot : shots) {
            Bitmap bitmap = BitmapFactory.decodeFile(shot.file.getAbsolutePath());
            if( bitmap == null )
                continue;
            final int bw = bitmap.getWidth();
            final int bh = bitmap.getHeight();
            final int[] pixels = new int[bw * bh];
            bitmap.getPixels(pixels, 0, bw, 0, 0, bw, bh);
            bitmap.recycle();
            final float[] r = shot.rotation;

            // only the rows this photo can reach
            float lookZ = -r[8];
            double lookPitch = Math.asin(Math.max(-1.0f, Math.min(1.0f, lookZ)));
            double top = lookPitch + maxAngle;
            double bottom = lookPitch - maxAngle;
            final int rowStart = Math.max(0, (int)Math.floor((Math.PI / 2.0 - Math.min(Math.PI / 2.0, top)) / Math.PI * height));
            final int rowEnd = Math.min(height, (int)Math.ceil((Math.PI / 2.0 - Math.max(-Math.PI / 2.0, bottom)) / Math.PI * height));
            final float cosMax = (float)Math.cos(maxAngle);
            final float lx = -r[2], ly = -r[5], lz = -r[8];

            Thread[] workers = new Thread[threads];
            for(int t=0;t<threads;t++) {
                final int part = t;
                workers[t] = new Thread(() -> {
                    for(int j=rowStart+part;j<rowEnd;j+=threads) {
                        float cp = cosPitch[j];
                        float sp = sinPitch[j];
                        int rowOffset = j * width;
                        for(int i=0;i<width;i++) {
                            float wx = cp * sinYaw[i];
                            float wy = cp * cosYaw[i];
                            float wz = sp;
                            if( wx*lx + wy*ly + wz*lz < cosMax )
                                continue;
                            // device = R^T * world
                            float dx = r[0]*wx + r[3]*wy + r[6]*wz;
                            float dy = r[1]*wx + r[4]*wy + r[7]*wz;
                            float dz = r[2]*wx + r[5]*wy + r[8]*wz;
                            if( dz >= -1.0e-4f )
                                continue;
                            float u = dx / -dz / tanHalfH;
                            float v = dy / -dz / tanHalfV;
                            if( u <= -1.0f || u >= 1.0f || v <= -1.0f || v >= 1.0f )
                                continue;
                            float px = (u + 1.0f) * 0.5f * (bw - 1);
                            float py = (1.0f - v) * 0.5f * (bh - 1);
                            int x0 = (int)px;
                            int y0 = (int)py;
                            int x1 = Math.min(x0 + 1, bw - 1);
                            int y1 = Math.min(y0 + 1, bh - 1);
                            float fx = px - x0;
                            float fy = py - y0;
                            int c00 = pixels[y0 * bw + x0];
                            int c10 = pixels[y0 * bw + x1];
                            int c01 = pixels[y1 * bw + x0];
                            int c11 = pixels[y1 * bw + x1];
                            float w00 = (1 - fx) * (1 - fy), w10 = fx * (1 - fy), w01 = (1 - fx) * fy, w11 = fx * fy;
                            float red = ((c00 >> 16) & 0xff) * w00 + ((c10 >> 16) & 0xff) * w10 + ((c01 >> 16) & 0xff) * w01 + ((c11 >> 16) & 0xff) * w11;
                            float green = ((c00 >> 8) & 0xff) * w00 + ((c10 >> 8) & 0xff) * w10 + ((c01 >> 8) & 0xff) * w01 + ((c11 >> 8) & 0xff) * w11;
                            float blue = (c00 & 0xff) * w00 + (c10 & 0xff) * w10 + (c01 & 0xff) * w01 + (c11 & 0xff) * w11;
                            // feather: full weight in the middle, fading to zero at the edges
                            float weight = (1.0f - u*u) * (1.0f - v*v);
                            weight *= weight;
                            int index = rowOffset + i;
                            accR[index] += red * weight;
                            accG[index] += green * weight;
                            accB[index] += blue * weight;
                            accW[index] += weight;
                        }
                    }
                });
                workers[t].start();
            }
            for(Thread worker : workers) {
                try {
                    worker.join();
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] row = new int[width];
        // gaps (e.g. near the poles if those shots were skipped) are filled from the nearest row above or below
        int[] lastColor = new int[width];
        boolean[] haveLast = new boolean[width];
        final AtomicBoolean anyPixel = new AtomicBoolean(false);
        for(int j=0;j<height;j++) {
            for(int i=0;i<width;i++) {
                int index = j * width + i;
                float w = accW[index];
                if( w > 1.0e-6f ) {
                    int red = Math.min(255, Math.round(accR[index] / w));
                    int green = Math.min(255, Math.round(accG[index] / w));
                    int blue = Math.min(255, Math.round(accB[index] / w));
                    row[i] = 0xff000000 | (red << 16) | (green << 8) | blue;
                    lastColor[i] = row[i];
                    haveLast[i] = true;
                    anyPixel.set(true);
                }
                else {
                    row[i] = haveLast[i] ? lastColor[i] : 0;
                }
            }
            result.setPixels(row, 0, width, 0, j, width, 1);
        }
        if( !anyPixel.get() )
            return false;
        // fill the top rows (above the highest shot) from below
        for(int i=0;i<width;i++) {
            int firstRow = -1;
            for(int j=0;j<height;j++) {
                if( accW[j * width + i] > 1.0e-6f ) {
                    firstRow = j;
                    break;
                }
            }
            if( firstRow > 0 ) {
                int color = result.getPixel(i, firstRow);
                for(int j=0;j<firstRow;j++)
                    result.setPixel(i, j, color);
            }
        }

        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 92, jpeg);
        result.recycle();
        byte[] withXmp = insertXmp(jpeg.toByteArray(), xmp(width, height));
        return save(context, withXmp);
    }

    private static String xmp(int width, int height) {
        return "<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>" +
                "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
                "<rdf:Description rdf:about=\"\" xmlns:GPano=\"http://ns.google.com/photos/1.0/panorama/\"" +
                " GPano:UsePanoramaViewer=\"True\"" +
                " GPano:ProjectionType=\"equirectangular\"" +
                " GPano:CaptureSoftware=\"Lenz\"" +
                " GPano:CroppedAreaImageWidthPixels=\"" + width + "\"" +
                " GPano:CroppedAreaImageHeightPixels=\"" + height + "\"" +
                " GPano:FullPanoWidthPixels=\"" + width + "\"" +
                " GPano:FullPanoHeightPixels=\"" + height + "\"" +
                " GPano:CroppedAreaLeftPixels=\"0\"" +
                " GPano:CroppedAreaTopPixels=\"0\"" +
                " GPano:PoseHeadingDegrees=\"0\"/>" +
                "</rdf:RDF></x:xmpmeta><?xpacket end=\"w\"?>";
    }

    /** Inserts an XMP APP1 segment after the JPEG's SOI (and JFIF APP0, if present).
     */
    static byte[] insertXmp(byte[] jpeg, String xmp) {
        byte[] header = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);
        byte[] body = xmp.getBytes(StandardCharsets.UTF_8);
        int segmentLength = 2 + header.length + body.length;
        int pos = 2; // after SOI
        if( jpeg.length > 4 && (jpeg[2] & 0xff) == 0xff && (jpeg[3] & 0xff) == 0xe0 ) {
            int app0Length = ((jpeg[4] & 0xff) << 8) | (jpeg[5] & 0xff);
            pos = 4 + app0Length;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(jpeg.length + segmentLength + 2);
        out.write(jpeg, 0, pos);
        out.write(0xff);
        out.write(0xe1);
        out.write((segmentLength >> 8) & 0xff);
        out.write(segmentLength & 0xff);
        out.write(header, 0, header.length);
        out.write(body, 0, body.length);
        out.write(jpeg, pos, jpeg.length - pos);
        return out.toByteArray();
    }

    private static boolean save(Context context, byte[] data) throws IOException {
        String name = "PANO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".PHOTOSPHERE.jpg";
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Lenz");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if( uri == null ) {
            Log.e(TAG, "failed to create MediaStore entry");
            return false;
        }
        try( OutputStream out = resolver.openOutputStream(uri) ) {
            if( out == null )
                return false;
            out.write(data);
        }
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
        }
        return true;
    }
}
