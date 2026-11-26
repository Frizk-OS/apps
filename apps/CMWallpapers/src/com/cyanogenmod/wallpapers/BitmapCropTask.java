package com.cyanogenmod.wallpapers;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

class BitmapCropTask extends AsyncTask<Void, Void, Boolean> {

    public interface OnBitmapCroppedHandler {
        void onBitmapCropped(byte[] imageBytes);
    }

    private static final String LOGTAG = BitmapCropTask.class.getSimpleName();
    protected static final int DEFAULT_COMPRESS_QUALITY = 90;

    Uri mInUri = null;
    Context mContext;
    String mInFilePath;
    byte[] mInImageBytes;
    int mInResId = 0;
    InputStream mInStream;
    RectF mCropBounds = null;
    int mOutWidth, mOutHeight;
    int mRotation;
    String mOutputFormat = "jpg"; // for now
    boolean mSetWallpaper;
    boolean mSaveCroppedBitmap;
    Bitmap mCroppedBitmap;
    Runnable mOnEndRunnable;
    Resources mResources;
    OnBitmapCroppedHandler mOnBitmapCroppedHandler;
    boolean mNoCrop;

    public BitmapCropTask(Context c, String filePath,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mContext = c;
        mInFilePath = filePath;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    public BitmapCropTask(byte[] imageBytes,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mInImageBytes = imageBytes;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    public BitmapCropTask(Context c, Uri inUri,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mContext = c;
        mInUri = inUri;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    public BitmapCropTask(Context c, Resources res, int inResId,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mContext = c;
        mInResId = inResId;
        mResources = res;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    private void init(RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mCropBounds = cropBounds;
        mRotation = rotation;
        mOutWidth = outWidth;
        mOutHeight = outHeight;
        mSetWallpaper = setWallpaper;
        mSaveCroppedBitmap = saveCroppedBitmap;
        mOnEndRunnable = onEndRunnable;
    }

    public void setOnBitmapCropped(OnBitmapCroppedHandler handler) {
        mOnBitmapCroppedHandler = handler;
    }

    public void setNoCrop(boolean value) {
        mNoCrop = value;
    }

    public void setOnEndRunnable(Runnable onEndRunnable) {
        mOnEndRunnable = onEndRunnable;
    }

    // Helper to setup input stream
    private void regenerateInputStream() {
        if (mInUri == null && mInResId == 0 && mInFilePath == null && mInImageBytes == null) {
            Log.w(LOGTAG, "cannot read original file, no input URI, resource ID, or "
                    + "image byte array given");
        } else {
            if (mInStream != null) {
                try {
                    mInStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (mInUri != null) {
                    mInStream = new BufferedInputStream(
                            mContext.getContentResolver().openInputStream(mInUri));
                } else if (mInFilePath != null) {
                    mInStream = mContext.openFileInput(mInFilePath);
                } else if (mInImageBytes != null) {
                    mInStream = new BufferedInputStream(
                            new ByteArrayInputStream(mInImageBytes));
                } else {
                    mInStream = new BufferedInputStream(
                            mResources.openRawResource(mInResId));
                }
            } catch (FileNotFoundException e) {
                Log.w(LOGTAG, "cannot read file: " + mInUri, e);
            }
        }
    }

    public Point getImageBounds() {
        regenerateInputStream();
        if (mInStream != null) {
            var options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(mInStream, null, options);
            if (options.outWidth != 0 && options.outHeight != 0) {
                return new Point(options.outWidth, options.outHeight);
            }
        }
        return null;
    }

    public void setCropBounds(RectF cropBounds) {
        mCropBounds = cropBounds;
    }

    public Bitmap getCroppedBitmap() {
        return mCroppedBitmap;
    }

    public boolean cropBitmap() {
        var failure = false;

        regenerateInputStream();

        WallpaperManager wallpaperManager = null;
        if (mSetWallpaper) {
            wallpaperManager = WallpaperManager.getInstance(mContext.getApplicationContext());
        }
        if (mSetWallpaper && mNoCrop && mInStream != null) {
            try {
                wallpaperManager.setStream(mInStream);
            } catch (IOException e) {
                Log.w(LOGTAG, "cannot write stream to wallpaper", e);
                failure = true;
            }
            return !failure;
        }
        if (mInStream != null) {
            // Find crop bounds (scaled to original image size)
            var roundedTrueCrop = new Rect();
            var rotateMatrix = new Matrix();
            var inverseRotateMatrix = new Matrix();
            if (mRotation > 0) {
                rotateMatrix.setRotate(mRotation);
                inverseRotateMatrix.setRotate(-mRotation);

                mCropBounds.roundOut(roundedTrueCrop);
                mCropBounds = new RectF(roundedTrueCrop);

                var bounds = getImageBounds();

                var rotatedBounds = new float[] { bounds.x, bounds.y };
                rotateMatrix.mapPoints(rotatedBounds);
                rotatedBounds[0] = Math.abs(rotatedBounds[0]);
                rotatedBounds[1] = Math.abs(rotatedBounds[1]);

                mCropBounds.offset(-rotatedBounds[0] / 2, -rotatedBounds[1] / 2);
                inverseRotateMatrix.mapRect(mCropBounds);
                mCropBounds.offset(bounds.x / 2, bounds.y / 2);

                regenerateInputStream();
            }

            mCropBounds.roundOut(roundedTrueCrop);

            if (roundedTrueCrop.width() <= 0 || roundedTrueCrop.height() <= 0) {
                Log.w(LOGTAG, "crop has bad values for full size image");
                return false;
            }

            // See how much we're reducing the size of the image
            var scaleDownSampleSize = Math.min(roundedTrueCrop.width() / mOutWidth,
                    roundedTrueCrop.height() / mOutHeight);

            // Attempt to open a region decoder
            BitmapRegionDecoder decoder = null;
            try {
                decoder = BitmapRegionDecoder.newInstance(mInStream, true);
            } catch (IOException e) {
                Log.w(LOGTAG, "cannot open region decoder for file: " + mInUri, e);
            }

            Bitmap crop = null;
            if (decoder != null) {
                // Do region decoding to get crop bitmap
                var options = new BitmapFactory.Options();
                if (scaleDownSampleSize > 1) {
                    options.inSampleSize = scaleDownSampleSize;
                }
                crop = decoder.decodeRegion(roundedTrueCrop, options);
                decoder.recycle();
            }

            if (crop == null) {
                // BitmapRegionDecoder has failed, try to crop in-memory
                regenerateInputStream();
                Bitmap fullSize = null;
                if (mInStream != null) {
                    var options = new BitmapFactory.Options();
                    if (scaleDownSampleSize > 1) {
                        options.inSampleSize = scaleDownSampleSize;
                    }
                    fullSize = BitmapFactory.decodeStream(mInStream, null, options);
                }
                if (fullSize != null) {
                    mCropBounds.left /= scaleDownSampleSize;
                    mCropBounds.top /= scaleDownSampleSize;
                    mCropBounds.bottom /= scaleDownSampleSize;
                    mCropBounds.right /= scaleDownSampleSize;
                    mCropBounds.roundOut(roundedTrueCrop);

                    crop = Bitmap.createBitmap(fullSize, roundedTrueCrop.left,
                            roundedTrueCrop.top, roundedTrueCrop.width(),
                            roundedTrueCrop.height());
                }
            }

            if (crop == null) {
                Log.w(LOGTAG, "cannot decode file: " + mInUri);
                return false;
            }
            if (mOutWidth > 0 && mOutHeight > 0 || mRotation > 0) {
                var dimsAfter = new float[] { crop.getWidth(), crop.getHeight() };
                rotateMatrix.mapPoints(dimsAfter);
                dimsAfter[0] = Math.abs(dimsAfter[0]);
                dimsAfter[1] = Math.abs(dimsAfter[1]);

                if (!(mOutWidth > 0 && mOutHeight > 0)) {
                    mOutWidth = Math.round(dimsAfter[0]);
                    mOutHeight = Math.round(dimsAfter[1]);
                }

                var cropRect = new RectF(0, 0, dimsAfter[0], dimsAfter[1]);
                var returnRect = new RectF(0, 0, mOutWidth, mOutHeight);

                var m = new Matrix();
                if (mRotation == 0) {
                    m.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);
                } else {
                    var m1 = new Matrix();
                    m1.setTranslate(-crop.getWidth() / 2f, -crop.getHeight() / 2f);
                    var m2 = new Matrix();
                    m2.setRotate(mRotation);
                    var m3 = new Matrix();
                    m3.setTranslate(dimsAfter[0] / 2f, dimsAfter[1] / 2f);
                    var m4 = new Matrix();
                    m4.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);

                    var c1 = new Matrix();
                    c1.setConcat(m2, m1);
                    var c2 = new Matrix();
                    c2.setConcat(m4, m3);
                    m.setConcat(c2, c1);
                }

                var tmp = Bitmap.createBitmap((int) returnRect.width(),
                        (int) returnRect.height(), Bitmap.Config.ARGB_8888);
                if (tmp != null) {
                    var c = new Canvas(tmp);
                    var p = new Paint();
                    p.setFilterBitmap(true);
                    c.drawBitmap(crop, m, p);
                    crop = tmp;
                }
            }

            if (mSaveCroppedBitmap) {
                mCroppedBitmap = crop;
            }

            // Get output compression format
            var cf = convertExtensionToCompressFormat(getFileExtension(mOutputFormat));

            // Compress to byte array
            var tmpOut = new ByteArrayOutputStream(2048);
            if (crop.compress(cf, DEFAULT_COMPRESS_QUALITY, tmpOut)) {
                // If we need to set to the wallpaper, set it
                if (mSetWallpaper && wallpaperManager != null) {
                    var outByteArray = tmpOut.toByteArray();
                    try (var bais = new ByteArrayInputStream(outByteArray)) {
                        wallpaperManager.setStream(bais);
                        if (mOnBitmapCroppedHandler != null) {
                            mOnBitmapCroppedHandler.onBitmapCropped(outByteArray);
                        }
                    } catch (IOException e) {
                        Log.w(LOGTAG, "cannot write stream to wallpaper", e);
                        failure = true;
                    }
                }
            } else {
                Log.w(LOGTAG, "cannot compress bitmap");
                failure = true;
            }
        }
        return !failure; // True if any of the operations failed
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return cropBitmap();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mOnEndRunnable != null) {
            mOnEndRunnable.run();
        }
    }

    protected static CompressFormat convertExtensionToCompressFormat(String extension) {
        return extension.equals("png") ? CompressFormat.PNG : CompressFormat.JPEG;
    }

    protected static String getFileExtension(String requestFormat) {
        var outputFormat = (requestFormat == null ? "jpg" : requestFormat)
                .toLowerCase(Locale.US);
        return (outputFormat.equals("png") || outputFormat.equals("gif"))
                ? "png" // We don't support gif compression.
                : "jpg";
    }
}
