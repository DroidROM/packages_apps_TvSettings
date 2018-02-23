/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tv.settings.device;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.tv.settings.MainFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.device.sound.SoundFragment;
import com.android.tv.settings.system.SecurityFragment;

/**
 * The "Device Preferences" screen in TV settings.
 */
@Keep
public class DevicePrefFragment extends SettingsPreferenceFragment {
    private static final String TAG = "DeviceFragment";

    @VisibleForTesting
    static final String KEY_DEVELOPER = "developer";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_SECURITY = "security";
    private static final String KEY_USAGE = "usageAndDiag";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_SOUNDS = "sound_effects";
    @VisibleForTesting
    static final String KEY_CAST_SETTINGS = "cast";
    private static final String KEY_GOOGLE_SETTINGS = "google_settings";
    private static final String KEY_HOME_SETTINGS = "home";
    private static final String KEY_SPEECH_SETTINGS = "speech";
    private static final String KEY_SEARCH_SETTINGS = "search";

    private Preference mSoundsPref;
    private boolean mInputSettingNeeded;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (isRestricted()) {
            setPreferencesFromResource(R.xml.device_restricted, null);
        } else {
            setPreferencesFromResource(R.xml.device, null);
        }
        mSoundsPref = findPreference(KEY_SOUNDS);
        final Preference inputPref = findPreference(KEY_INPUTS);
        if (inputPref != null) {
            inputPref.setVisible(mInputSettingNeeded);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final TvInputManager manager = (TvInputManager) getContext().getSystemService(
                Context.TV_INPUT_SERVICE);
        if (manager != null) {
            for (final TvInputInfo input : manager.getTvInputList()) {
                if (input.isPassthroughInput()) {
                    mInputSettingNeeded = true;
                }
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateDeveloperOptions();
        updateSounds();
        updateGoogleSettings();
        updateCastSettings();
        hideIfIntentUnhandled(findPreference(KEY_HOME_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_CAST_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_USAGE));
        hideIfIntentUnhandled(findPreference(KEY_SPEECH_SETTINGS));
        hideIfIntentUnhandled(findPreference(KEY_SEARCH_SETTINGS));
    }

    @Override
    public int getMetricsCategory() {
        // TODO(70572789): Finalize metrics categories.
        return 0;
    }

    private void hideIfIntentUnhandled(Preference preference) {
        if (preference == null) {
            return;
        }
        preference.setVisible(
                MainFragment.systemIntentIsHandled(getContext(), preference.getIntent()) != null);
    }

    private boolean isRestricted() {
        return SecurityFragment.isRestrictedProfileInEffect(getContext());
    }

    @VisibleForTesting
    void updateDeveloperOptions() {
        final Preference developerPref = findPreference(KEY_DEVELOPER);
        if (developerPref == null) {
            return;
        }

        developerPref.setVisible(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(
                getContext()));
    }

    private void updateSounds() {
        if (mSoundsPref == null) {
            return;
        }

        mSoundsPref.setIcon(SoundFragment.getSoundEffectsEnabled(getContext().getContentResolver())
                ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
    }

    private void updateGoogleSettings() {
        final Preference googleSettingsPref = findPreference(KEY_GOOGLE_SETTINGS);
        if (googleSettingsPref != null) {
            final ResolveInfo info = MainFragment.systemIntentIsHandled(getContext(),
                    googleSettingsPref.getIntent());
            googleSettingsPref.setVisible(info != null);
            if (info != null && info.activityInfo != null) {
                googleSettingsPref.setIcon(
                        info.activityInfo.loadIcon(getContext().getPackageManager()));
                googleSettingsPref.setTitle(
                        info.activityInfo.loadLabel(getContext().getPackageManager()));
            }

            final Preference speechPref = findPreference(KEY_SPEECH_SETTINGS);
            if (speechPref != null) {
                speechPref.setVisible(info == null);
            }
            final Preference searchPref = findPreference(KEY_SEARCH_SETTINGS);
            if (searchPref != null) {
                searchPref.setVisible(info == null);
            }
        }
    }

    @VisibleForTesting
    void updateCastSettings() {
        final Preference castPref = findPreference(KEY_CAST_SETTINGS);
        if (castPref != null) {
            final ResolveInfo info = MainFragment.systemIntentIsHandled(
                        getContext(), castPref.getIntent());
            if (info != null) {
                try {
                    final Context targetContext = getContext()
                            .createPackageContext(info.resolvePackageName != null
                                    ? info.resolvePackageName : info.activityInfo.packageName, 0);
                    castPref.setIcon(targetContext.getDrawable(info.iconResourceId));
                } catch (Resources.NotFoundException | PackageManager.NameNotFoundException
                        | SecurityException e) {
                    Log.e(TAG, "Cast settings icon not found", e);
                }
                castPref.setTitle(info.activityInfo.loadLabel(getContext().getPackageManager()));
            }
        }
    }
}