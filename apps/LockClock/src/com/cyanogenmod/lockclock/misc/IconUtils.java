/*
 * Copyright (C) 2013 The CyanogenMod Project (DvTonder)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.lockclock.misc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.cyanogenmod.lockclock.R;

public final class IconUtils {
    private static final String TAG = "IconUtils";
    private static final boolean D = Constants.DEBUG;

    private IconUtils() {
    }

    public static int getWeatherIconResource(Context context, String iconSet, int conditionCode) {
        if (iconSet.startsWith("ext:") || iconSet.equals(Constants.MONOCHROME)) {
            return 0;
        }

        var res = context.getResources();
        int resId = res.getIdentifier("weather_" + iconSet + "_" + conditionCode,
                "drawable", context.getPackageName());

        if (resId != 0) {
            return resId;
        }

        // Use the default color set unknown icon
        return R.drawable.weather_color_na;
    }

    public static Bitmap getWeatherIconBitmap(Context context, String iconSet,
            int color, int conditionCode) {
        return getWeatherIconBitmap(context, iconSet, color, conditionCode, 0);
    }

    public static Bitmap getWeatherIconBitmap(Context context, String iconSet,
            int color, int conditionCode, int density) {
        boolean isMonoSet = Constants.MONOCHROME.equals(iconSet);
        Resources res = null;
        int resId = 0;

        if (iconSet.startsWith("ext:")) {
            String packageName = iconSet.substring(4);
            try {
                res = context.getPackageManager().getResourcesForApplication(packageName);
                resId = res.getIdentifier("weather_" + conditionCode, "drawable", packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // fall back to colored icons
                iconSet = Constants.COLOR_STD;
            }
        }
        if (resId == 0) {
            String identifier = isMonoSet
                    ? "weather_" + conditionCode
                    : "weather_" + iconSet + "_" + conditionCode;
            res = context.getResources();
            resId = res.getIdentifier(identifier, "drawable", context.getPackageName());
        }

        if (resId == 0) {
            resId = isMonoSet ? R.drawable.weather_na : R.drawable.weather_color_na;
        }

        return getOverlaidBitmap(res, resId, isMonoSet ? color : 0, density);
    }

    public static Bitmap getOverlaidBitmap(Resources res, int resId, int color) {
        return getOverlaidBitmap(res, resId, color, 0);
    }

    public static Bitmap getOverlaidBitmap(Resources res, int resId, int color, int density) {
        var src = getBitmapFromResource(res, resId, density);
        if (color == 0 || src == null) {
            return src;
        }

        var dest = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                Bitmap.Config.ARGB_8888);
        var c = new Canvas(dest);
        var paint = new Paint();

        // Overlay the selected color and set the imageview
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        c.drawBitmap(src, 0, 0, paint);
        return dest;
    }

    public static Bitmap getBitmapFromResource(Resources res, int resId, int density) {
        if (density == 0) {
            if (D) Log.d(TAG, "Decoding resource id = " + resId + " for default density");
            return BitmapFactory.decodeResource(res, resId);
        }

        if (D) Log.d(TAG, "Decoding resource id = " + resId + " for density = " + density);
        var d = res.getDrawableForDensity(resId, density);
        if (d instanceof BitmapDrawable bd) {
            return bd.getBitmap();
        }

        var result = Bitmap.createBitmap(d.getIntrinsicWidth(),
                d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        var canvas = new Canvas(result);
        d.setBounds(0, 0, result.getWidth(), result.getHeight());
        d.draw(canvas);
        canvas.setBitmap(null);

        return result;
    }

    public static int getNextHigherDensity(Context context) {
        var res = context.getResources();
        int density = res.getDisplayMetrics().densityDpi;

        return switch (density) {
            case DisplayMetrics.DENSITY_LOW -> DisplayMetrics.DENSITY_MEDIUM;
            case DisplayMetrics.DENSITY_MEDIUM -> DisplayMetrics.DENSITY_HIGH;
            case DisplayMetrics.DENSITY_HIGH -> DisplayMetrics.DENSITY_XHIGH;
            case DisplayMetrics.DENSITY_XHIGH -> DisplayMetrics.DENSITY_XXHIGH;
            case DisplayMetrics.DENSITY_XXHIGH -> DisplayMetrics.DENSITY_XXXHIGH;
            // fallback: use current density
            default -> density;
        };
    }
}
