/*
 * Copyright (C) 2013 David van Tonder
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

package com.cyanogenmod.lockclock.weather;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cyanogenmod.lockclock.R;
import com.cyanogenmod.lockclock.misc.IconUtils;
import com.cyanogenmod.lockclock.misc.Preferences;
import com.cyanogenmod.lockclock.weather.WeatherInfo.DayForecast;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class ForecastBuilder {
    private static final String TAG = "ForecastBuilder";

    private ForecastBuilder() {
    }

    /**
     * This method is used to build the full current conditions and horizontal forecasts
     * panels
     *
     * @param context
     * @param w = the Weather info object that contains the forecast data
     * @return = a built view that can be displayed
     */
    public static View buildFullPanel(Context context, int resourceId, WeatherInfo w) {
        // Load some basic settings
        var inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int color = Preferences.weatherFontColor(context);
        boolean invertLowHigh = Preferences.invertLowHighTemperature(context);

        var view = inflater.inflate(resourceId, null);

        // Set the weather source
        var weatherSource = (TextView) view.findViewById(R.id.weather_source);
        weatherSource.setText(Preferences.weatherProvider(context).getNameResourceId());

        // Set the current conditions
        // Weather Image
        var weatherImage = (ImageView) view.findViewById(R.id.weather_image);
        String iconsSet = Preferences.getWeatherIconSet(context);
        weatherImage.setImageBitmap(w.getConditionBitmap(iconsSet, color,
                IconUtils.getNextHigherDensity(context)));

        // Weather Condition
        var weatherCondition = (TextView) view.findViewById(R.id.weather_condition);
        weatherCondition.setText(w.getCondition());

        // Weather Temps
        var weatherTemp = (TextView) view.findViewById(R.id.weather_temp);
        weatherTemp.setText(w.getFormattedTemperature());

        // City
        var city = (TextView) view.findViewById(R.id.weather_city);
        city.setText(w.getCity());

        // Weather Update Time
        var lastUpdate = w.getTimestamp();
        String timestamp = DateFormat.format("E", lastUpdate) + " "
                + DateFormat.getTimeFormat(context).format(lastUpdate);
        var updateTime = (TextView) view.findViewById(R.id.update_time);
        updateTime.setText(timestamp);
        updateTime.setVisibility(Preferences.showWeatherTimestamp(context) ? View.VISIBLE : View.GONE);

        // Weather Temps Panel additional items
        String low = w.getFormattedLow();
        String high = w.getFormattedHigh();
        var weatherLowHigh = (TextView) view.findViewById(R.id.weather_low_high);
        weatherLowHigh.setText(invertLowHigh ? high + " | " + low : low + " | " + high);

        // Get things ready
        var forecastView = (LinearLayout) view.findViewById(R.id.forecast_view);
        var progressIndicator = view.findViewById(R.id.progress_indicator);

        // Build the forecast panel
        if (buildSmallPanel(context, forecastView, w)) {
            // Success, hide the progress container
            progressIndicator.setVisibility(View.GONE);
        }

        return view;
    }

    /**
     * This method is used to build the small, horizontal forecasts panel
     * @param context
     * @param smallPanel = a horizontal linearlayout that will contain the forecasts
     * @param w = the Weather info object that contains the forecast data
     */
    public static boolean buildSmallPanel(Context context, LinearLayout smallPanel, WeatherInfo w) {
        if (smallPanel == null) {
            Log.d(TAG, "Invalid view passed");
            return false;
        }

        // Get things ready
        var inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int color = Preferences.weatherFontColor(context);
        boolean invertLowHigh = Preferences.invertLowHighTemperature(context);

        var forecasts = w.getForecasts();
        if (forecasts == null || forecasts.size() <= 1) {
            smallPanel.setVisibility(View.GONE);
            return false;
        }

        var myTimezone = TimeZone.getDefault();
        var calendar = new GregorianCalendar(myTimezone);

        // Iterate through the forecasts
        for (DayForecast d : forecasts) {
            // Load the views
            var forecastItem = inflater.inflate(R.layout.forecast_item, null);

            // The day of the week
            var day = (TextView) forecastItem.findViewById(R.id.forecast_day);
            day.setText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
            calendar.roll(Calendar.DAY_OF_WEEK, true);

            // Weather Image
            var image = (ImageView) forecastItem.findViewById(R.id.weather_image);
            String iconsSet = Preferences.getWeatherIconSet(context);
            int resId = d.getConditionResource(context, iconsSet);
            if (resId != 0) {
                image.setImageResource(resId);
            } else {
                image.setImageBitmap(d.getConditionBitmap(context, iconsSet, color));
            }

            // Temperatures
            String dayLow = d.getFormattedLow();
            String dayHigh = d.getFormattedHigh();
            var temps = (TextView) forecastItem.findViewById(R.id.weather_temps);
            temps.setText(invertLowHigh ? dayHigh + " " + dayLow : dayLow + " " + dayHigh);

            // Add the view
            smallPanel.addView(forecastItem,
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        return true;
    }
}
