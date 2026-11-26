package com.cyanogenmod.lockclock.weather;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.cyanogenmod.lockclock.R;
import com.cyanogenmod.lockclock.weather.WeatherInfo.DayForecast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenWeatherMapProvider implements WeatherProvider {
    private static final String TAG = "OpenWeatherMapProvider";

    private static final int FORECAST_DAYS = 5;
    private static final String SELECTION_LOCATION = "lat=%f&lon=%f";
    private static final String SELECTION_ID = "id=%s";
    private static final String APP_ID = "e2b075d68c39dc43e16995653fcd6fd0";

    private static final String URL_LOCATION =
            "http://api.openweathermap.org/data/2.5/find?q=%s&mode=json&lang=%s&appid="
            + APP_ID;
    private static final String URL_WEATHER =
            "http://api.openweathermap.org/data/2.5/weather?%s&mode=json&units=%s&lang=%s&appid="
            + APP_ID;
    private static final String URL_FORECAST =
            "http://api.openweathermap.org/data/2.5/forecast/daily?"
            + "%s&mode=json&units=%s&lang=%s&cnt=" + FORECAST_DAYS + "&appid=" + APP_ID;

    private final Context mContext;

    public OpenWeatherMapProvider(Context context) {
        mContext = context;
    }

    @Override
    public int getNameResourceId() {
        return R.string.weather_source_openweathermap;
    }

    @Override
    public List<LocationResult> getLocations(String input) {
        String url = String.format(Locale.US, URL_LOCATION, Uri.encode(input), getLanguageCode());
        String response = HttpRetriever.retrieve(url);
        if (response == null) {
            return null;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "URL = " + url + " returning a response of " + response);
        }

        try {
            var jsonResults = new JSONObject(response).getJSONArray("list");
            var results = new ArrayList<LocationResult>();
            int count = jsonResults.length();

            for (int i = 0; i < count; i++) {
                var result = jsonResults.getJSONObject(i);
                var location = new LocationResult();

                location.id = result.getString("id");
                location.city = result.getString("name");
                location.countryId = result.getJSONObject("sys").getString("country");
                results.add(location);
            }

            return results;
        } catch (JSONException e) {
            Log.w(TAG, "Received malformed location data (input=" + input + ")", e);
        }

        return null;
    }

    @Override
    public WeatherInfo getWeatherInfo(String id, String localizedCityName, boolean metric) {
        String selection = String.format(Locale.US, SELECTION_ID, id);
        return handleWeatherRequest(selection, localizedCityName, metric);
    }

    @Override
    public WeatherInfo getWeatherInfo(Location location, boolean metric) {
        String selection = String.format(Locale.US, SELECTION_LOCATION,
                location.getLatitude(), location.getLongitude());
        return handleWeatherRequest(selection, null, metric);
    }

    private WeatherInfo handleWeatherRequest(String selection,
            String localizedCityName, boolean metric) {
        String units = metric ? "metric" : "imperial";
        String locale = getLanguageCode();
        String conditionUrl = String.format(Locale.US, URL_WEATHER, selection, units, locale);
        String conditionResponse = HttpRetriever.retrieve(conditionUrl);
        if (conditionResponse == null) {
            return null;
        }

        String forecastUrl = String.format(Locale.US, URL_FORECAST, selection, units, locale);
        String forecastResponse = HttpRetriever.retrieve(forecastUrl);
        if (forecastResponse == null) {
            return null;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "URL = " + conditionUrl + " returning a response of " + conditionResponse);
        }

        try {
            var conditions = new JSONObject(conditionResponse);
            var weather = conditions.getJSONArray("weather").getJSONObject(0);
            var conditionData = conditions.getJSONObject("main");
            var windData = conditions.getJSONObject("wind");
            var forecasts = parseForecasts(new JSONObject(forecastResponse).getJSONArray("list"), metric);
            int speedUnitResId = metric ? R.string.weather_kph : R.string.weather_mph;
            if (localizedCityName == null) {
                localizedCityName = conditions.getString("name");
            }

            var w = new WeatherInfo(mContext, conditions.getString("id"), localizedCityName,
                    /* condition */ weather.getString("main"),
                    /* conditionCode */ mapConditionIconToCode(
                            weather.getString("icon"), weather.getInt("id")),
                    /* temperature */ sanitizeTemperature(conditionData.getDouble("temp"), metric),
                    /* tempUnit */ metric ? "C" : "F",
                    /* humidity */ (float) conditionData.getDouble("humidity"),
                    /* wind */ (float) windData.getDouble("speed"),
                    /* windDir */ windData.getInt("deg"),
                    /* speedUnit */ mContext.getString(speedUnitResId),
                    forecasts,
                    System.currentTimeMillis());

            Log.d(TAG, "Weather updated: " + w);
            return w;
        } catch (JSONException e) {
            Log.w(TAG, "Received malformed weather data (selection = " + selection
                    + ", lang = " + locale + ")", e);
        }

        return null;
    }

    private ArrayList<DayForecast> parseForecasts(JSONArray forecasts, boolean metric) throws JSONException {
        var result = new ArrayList<DayForecast>();
        int count = forecasts.length();

        if (count == 0) {
            throw new JSONException("Empty forecasts array");
        }
        for (int i = 0; i < count; i++) {
            var forecast = forecasts.getJSONObject(i);
            var temperature = forecast.getJSONObject("temp");
            var data = forecast.getJSONArray("weather").getJSONObject(0);
            var item = new DayForecast(
                    /* low */ sanitizeTemperature(temperature.getDouble("min"), metric),
                    /* high */ sanitizeTemperature(temperature.getDouble("max"), metric),
                    /* condition */ data.getString("main"),
                    /* conditionCode */ mapConditionIconToCode(
                            data.getString("icon"), data.getInt("id")));
            result.add(item);
        }

        return result;
    }

    // OpenWeatherMap sometimes returns temperatures in Kelvin even if we ask it
    // for deg C or deg F. Detect this and convert accordingly.
    private static float sanitizeTemperature(double value, boolean metric) {
        // threshold chosen to work for both C and F. 170 deg F is hotter
        // than the hottest place on earth.
        if (value > 170) {
            // K -> deg C
            value -= 273.15;
            if (!metric) {
                // deg C -> deg F
                value = (value * 1.8) + 32;
            }
        }
        return (float) value;
    }

    private static final HashMap<String, Integer> ICON_MAPPING = new HashMap<>();
    static {
        ICON_MAPPING.put("01d", 32);
        ICON_MAPPING.put("01n", 31);
        ICON_MAPPING.put("02d", 30);
        ICON_MAPPING.put("02n", 29);
        ICON_MAPPING.put("03d", 26);
        ICON_MAPPING.put("03n", 26);
        ICON_MAPPING.put("04d", 28);
        ICON_MAPPING.put("04n", 27);
        ICON_MAPPING.put("09d", 12);
        ICON_MAPPING.put("09n", 11);
        ICON_MAPPING.put("10d", 40);
        ICON_MAPPING.put("10n", 45);
        ICON_MAPPING.put("11d", 4);
        ICON_MAPPING.put("11n", 4);
        ICON_MAPPING.put("13d", 16);
        ICON_MAPPING.put("13n", 16);
        ICON_MAPPING.put("50d", 21);
        ICON_MAPPING.put("50n", 20);
    }

    private int mapConditionIconToCode(String icon, int conditionId) {
        // First, use condition ID for specific cases
        int code = switch (conditionId) {
            // Thunderstorms
            case 202, // thunderstorm with heavy rain
                    232, // thunderstorm with heavy drizzle
                    211 -> 4; // thunderstorm
            case 212 -> 3; // heavy thunderstorm
            case 221, // ragged thunderstorm
                    231, // thunderstorm with drizzle
                    201 -> 38; // thunderstorm with rain
            case 230, // thunderstorm with light drizzle
                    200, // thunderstorm with light rain
                    210 -> 37; // light thunderstorm

            // Drizzle
            case 300, // light intensity drizzle
                    301, // drizzle
                    302, // heavy intensity drizzle
                    310, // light intensity drizzle rain
                    311, // drizzle rain
                    312, // heavy intensity drizzle rain
                    313, // shower rain and drizzle
                    314, // heavy shower rain and drizzle
                    321 -> 9; // shower drizzle

            // Rain
            case 500, // light rain
                    501, // moderate rain
                    520, // light intensity shower rain
                    521, // shower rain
                    531 -> 11; // ragged shower rain
            case 502, // heavy intensity rain
                    503, // very heavy rain
                    504, // extreme rain
                    522 -> 12; // heavy intensity shower rain
            case 511 -> 10; // freezing rain

            // Snow
            case 600, 620 -> 14; // light snow
            case 601, 621 -> 16; // snow
            case 602, 622 -> 41; // heavy snow
            case 611, 612 -> 18; // sleet
            case 615, 616 -> 5;  // rain and snow

            // Atmosphere
            case 741 -> 20; // fog
            case 711, // smoke
                    762 -> 22; // volcanic ash
            case 701, // mist
                    721 -> 21; // haze
            case 731, // sand/dust whirls
                    751, // sand
                    761 -> 19; // dust
            case 771 -> 23; // squalls
            case 781 -> 0; // tornado

            // Extreme
            case 900 -> 0;  // tornado
            case 901 -> 1;  // tropical storm
            case 902 -> 2;  // hurricane
            case 903 -> 25; // cold
            case 904 -> 36; // hot
            case 905 -> 24; // windy
            case 906 -> 17; // hail

            default -> -1;
        };
        if (code != -1) {
            return code;
        }

        // Not yet handled - Use generic icon mapping
        Integer condition = ICON_MAPPING.get(icon);
        return condition != null ? condition : -1;
    }

    private static final HashMap<String, String> LANGUAGE_CODE_MAPPING = new HashMap<>();
    static {
        LANGUAGE_CODE_MAPPING.put("bg-", "bg");
        LANGUAGE_CODE_MAPPING.put("de-", "de");
        LANGUAGE_CODE_MAPPING.put("es-", "sp");
        LANGUAGE_CODE_MAPPING.put("fi-", "fi");
        LANGUAGE_CODE_MAPPING.put("fr-", "fr");
        LANGUAGE_CODE_MAPPING.put("it-", "it");
        LANGUAGE_CODE_MAPPING.put("nl-", "nl");
        LANGUAGE_CODE_MAPPING.put("pl-", "pl");
        LANGUAGE_CODE_MAPPING.put("pt-", "pt");
        LANGUAGE_CODE_MAPPING.put("ro-", "ro");
        LANGUAGE_CODE_MAPPING.put("ru-", "ru");
        LANGUAGE_CODE_MAPPING.put("se-", "se");
        LANGUAGE_CODE_MAPPING.put("tr-", "tr");
        LANGUAGE_CODE_MAPPING.put("uk-", "ua");
        LANGUAGE_CODE_MAPPING.put("zh-CN", "zh_cn");
        LANGUAGE_CODE_MAPPING.put("zh-TW", "zh_tw");
    }

    private String getLanguageCode() {
        var locale = mContext.getResources().getConfiguration().locale;
        String selector = locale.getLanguage() + "-" + locale.getCountry();

        for (Map.Entry<String, String> entry : LANGUAGE_CODE_MAPPING.entrySet()) {
            if (selector.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "en";
    }
}
