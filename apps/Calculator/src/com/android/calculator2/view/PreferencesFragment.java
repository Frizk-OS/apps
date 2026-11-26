/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.ListView;

import com.android.calculator2.CalculatorWidget;
import com.android.calculator2.FloatingCalculator;
import com.android.calculator2.Preferences;
import com.android.calculator2.R;
import com.xlythe.engine.theme.Theme;
import com.xlythe.engine.theme.ThemeListPreference;

public class PreferencesFragment extends PreferenceFragment {
    private static final String EXTRA_LIST_POSITION = "list_position";
    private static final String EXTRA_LIST_VIEW_OFFSET = "list_view_top";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        // Update theme (as needed)
        boolean useLightTheme = Theme.isLightTheme(getContext());

        var panels = findPreference("panels");
        if (panels != null) {
            panels.setIcon(useLightTheme ? R.drawable.settings_panels_icon_grey
                    : R.drawable.settings_panels_icon_white);
            panels.setOnPreferenceClickListener(preference -> {
                var fragment = new PageOrderFragment();
                getFragmentManager().beginTransaction().replace(R.id.content_view, fragment)
                        .addToBackStack(null).commit();
                return true;
            });
        }

        var actions = findPreference("actions");
        if (actions != null) {
            actions.setIcon(useLightTheme ? R.drawable.settings_actions_icon_grey
                    : R.drawable.settings_actions_icon_white);
            actions.setOnPreferenceClickListener(preference -> {
                var fragment = new ActionsPreferencesFragment();
                getFragmentManager().beginTransaction().replace(R.id.content_view, fragment)
                        .addToBackStack(null).commit();
                return true;
            });
        }

        var units = findPreference("units");
        if (units != null) {
            units.setIcon(useLightTheme ? R.drawable.settings_units_icon_grey
                    : R.drawable.settings_units_icon_white);
            units.setOnPreferenceClickListener(preference -> {
                var fragment = new UnitsPreferencesFragment();
                getFragmentManager().beginTransaction().replace(R.id.content_view, fragment)
                        .addToBackStack(null).commit();
                return true;
            });
        }

        var theme = (ThemeListPreference) findPreference("SELECTED_THEME");
        if (theme != null) {
            theme.setIcon(useLightTheme ? R.drawable.settings_theme_icon_grey
                    : R.drawable.settings_theme_icon_white);
            theme.setOnPreferenceChangeListener((preference, newValue) -> {
                String appName = newValue.toString();

                // Update theme
                Theme.setPackageName(appName);

                // Create a new intent to relaunch the settings
                var intent = new Intent(getActivity(), Preferences.class);

                // Preserve the list offsets
                int itemPosition = getListView().getFirstVisiblePosition();
                var child = getListView().getChildAt(0);
                int itemOffset = child != null ? child.getTop() : 0;

                intent.putExtra(EXTRA_LIST_POSITION, itemPosition);
                intent.putExtra(EXTRA_LIST_VIEW_OFFSET, itemOffset);

                // Go
                startActivity(intent);
                getActivity().finish();

                // Set a smooth fade transition
                getActivity().overridePendingTransition(android.R.anim.fade_in,
                        android.R.anim.fade_out);
                return true;
            });
            getPreferenceScreen().removePreference(theme);
        }

        var about = findPreference("ABOUT");
        if (about != null) {
            about.setIcon(useLightTheme ? R.drawable.settings_about_icon_grey
                    : R.drawable.settings_about_icon_white);

            String versionName = "";
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(
                        getActivity().getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            about.setTitle(about.getTitle() + " v" + versionName);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Restore the scroll position, if any
        final var args = getArguments();
        if (args != null) {
            getListView().setSelectionFromTop(args.getInt(EXTRA_LIST_POSITION, 0),
                    args.getInt(EXTRA_LIST_VIEW_OFFSET, 0));
        }
    }

    public ListView getListView() {
        return (ListView) getView().findViewById(android.R.id.list);
    }

    protected Context getContext() {
        return getActivity();
    }

    public static class ActionsPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.layout.preferences_actions);

            var floatingCalc = findPreference("FLOATING_CALCULATOR");
            if (floatingCalc != null) {
                floatingCalc.setOnPreferenceChangeListener((preference, newValue) -> {
                    var startServiceIntent = new Intent(getActivity(),
                            FloatingCalculator.class);
                    if ((Boolean) newValue) {
                        // Start Floating Calc service if not up yet
                        getActivity().startService(startServiceIntent);
                    } else {
                        // Stop Floating Calc service if up
                        getActivity().stopService(startServiceIntent);
                    }

                    return true;
                });
            }

            var widgetBg = findPreference("SHOW_WIDGET_BACKGROUND");
            if (widgetBg != null) {
                widgetBg.setOnPreferenceChangeListener((preference, newValue) -> {
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                            .putBoolean("SHOW_WIDGET_BACKGROUND", (Boolean) newValue).commit();
                    final var intent = new Intent(getActivity(), CalculatorWidget.class);
                    intent.setAction("refresh");
                    getActivity().sendBroadcast(intent);

                    return true;
                });
            }

            var vibrateOnPress = findPreference("VIBRATE_ON_PRESS");
            if (vibrateOnPress != null) {
                var vi = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                if (!vi.hasVibrator()) {
                    removePreference(vibrateOnPress);
                }
            }
        }

        protected void removePreference(Preference preference) {
            getPreferenceScreen().removePreference(preference);
        }
    }

    public static class UnitsPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.layout.preferences_units);
        }
    }
}
