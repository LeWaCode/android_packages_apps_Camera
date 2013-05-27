/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2010 The CyanogenMod Project
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

package com.android.camera;

import com.android.camera.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class AdvancedSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String KEY_VOL_UP_SHUTTER = "vol_up_shutter_enabled";
    private static final String KEY_VOL_DOWN_SHUTTER = "vol_down_shutter_enabled";
    private static final String KEY_SEARCH_SHUTTER = "search_shutter_enabled";
    private static final String KEY_VOL_ZOOM = "vol_zoom_enabled";
    private static final String KEY_LONG_FOCUS = "long_focus_enabled";
    private static final String KEY_PRE_FOCUS = "pre_focus_enabled";
    private static final String KEY_STORE_EXTSD = "store_on_external_sd";
    private SharedPreferences mSharedPref;
    private static final String KEY_CAMERA_SHUTTER_MUTE = "shutter_mute";
    private static final String KEY_CAMERA_VOL_ZOOM = "key_camera_vol_zoom";

    CheckBoxPreference volUpShutter = null;
    CheckBoxPreference volDownShutter = null;
    CheckBoxPreference searchShutter = null;
    CheckBoxPreference volZoom = null;
    CheckBoxPreference longFocus = null;
    CheckBoxPreference preFocus = null;
    CheckBoxPreference storeExtSd = null;
    CheckBoxPreference shutterMute = null;
    private boolean mIsZoomSupported;

    private String getFreeSpaceString(String dir) {
        Resources r = getResources();
        StatFs stat = new StatFs(dir);
        float remaining = ((float) stat.getAvailableBlocks()
                * (float) stat.getBlockSize())/1048576; // MB
        String freeSpace;
        NumberFormat formatter = new DecimalFormat("#0.00 ");
        if (remaining >= 1024) {
            remaining /= 1024; // GB
            freeSpace = formatter.format(remaining) + r.getString(R.string.pref_camera_giga_bytes);
        } else {
            freeSpace = formatter.format(remaining) + r.getString(R.string.pref_camera_mega_bytes);
        }
        return freeSpace;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.camera_advanced_settings);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mSharedPref = getSharedPreferences("com.android.camera_preferences", 0);        
        
        volUpShutter = (CheckBoxPreference) preferenceScreen.findPreference(KEY_VOL_UP_SHUTTER);
        volDownShutter = (CheckBoxPreference) preferenceScreen.findPreference(KEY_VOL_DOWN_SHUTTER);
        searchShutter = (CheckBoxPreference) preferenceScreen.findPreference(KEY_SEARCH_SHUTTER);
        volZoom = (CheckBoxPreference) preferenceScreen.findPreference(KEY_VOL_ZOOM);
        longFocus = (CheckBoxPreference) preferenceScreen.findPreference(KEY_LONG_FOCUS);
        preFocus = (CheckBoxPreference) preferenceScreen.findPreference(KEY_PRE_FOCUS);
        storeExtSd = (CheckBoxPreference) preferenceScreen.findPreference(KEY_STORE_EXTSD);

        mIsZoomSupported = mSharedPref.getBoolean("zoom_supported", false);
        if (!mIsZoomSupported) {
            volZoom.setChecked(false);
            volZoom.setEnabled(false);
        }
        if ("GT-S5830".equalsIgnoreCase(Build.MODEL) ) {
            getPreferenceScreen().removePreference(findPreference(KEY_VOL_ZOOM));
            getPreferenceScreen().removePreference(findPreference(KEY_CAMERA_VOL_ZOOM));
        }
        shutterMute = (CheckBoxPreference) preferenceScreen.findPreference(KEY_CAMERA_SHUTTER_MUTE);  
        if (shutterMute != null) {
            shutterMute.setOnPreferenceChangeListener(this);
            boolean bShutterMute = mSharedPref.getBoolean("shutter_mute", false);
            shutterMute.setChecked(bShutterMute);
        }
        if("u880".equalsIgnoreCase(Build.MODEL) || "u8800x".equalsIgnoreCase(Build.MODEL) || "u8800".equalsIgnoreCase(Build.MODEL)){
            getPreferenceScreen().removePreference(shutterMute);
        }
        // Hide the 'Use external SD' preference if the device doesn't have an internal storage
        if (!ImageManager.hasSwitchableStorage()) {
            if (storeExtSd != null) {
                preferenceScreen.removePreference(storeExtSd);
                storeExtSd = null;
            }
        } else {
            if (storeExtSd != null) {
                Resources r = getResources();
                int summaryResId = R.string.pref_camera_storage_external_summary_off;
                storeExtSd.setSummaryOff(r.getString(summaryResId,
                        getFreeSpaceString(ImageManager.getInternalDir())));
                summaryResId = R.string.pref_camera_storage_external_summary_on;
                storeExtSd.setSummaryOn(r.getString(summaryResId,
                        getFreeSpaceString(ImageManager.getRemovableDir())));
                // Setting the default state of storeExtSd in case the preference not present
                boolean useRemovableStorage = mSharedPref.getBoolean("store_on_external_sd",
                        !ImageManager.isStorageSwitchedToInternal());
                storeExtSd.setChecked(useRemovableStorage);
            }
        }

        checkBoxes();

        setListeners(true);
    }

    public void setListeners(boolean enable) {
        if (enable) {
            volUpShutter.setOnPreferenceChangeListener(this);
            volDownShutter.setOnPreferenceChangeListener(this);
            if(null != searchShutter){//by george,2011-12-12
                searchShutter.setOnPreferenceChangeListener(this);
            }
            volZoom.setOnPreferenceChangeListener(this);
            longFocus.setOnPreferenceChangeListener(this);
            preFocus.setOnPreferenceChangeListener(this);
            if (storeExtSd != null)
                storeExtSd.setOnPreferenceChangeListener(this);
        } else {
            volUpShutter.setOnPreferenceChangeListener(null);
            volDownShutter.setOnPreferenceChangeListener(null);            
            if(null != searchShutter){//by george,2011-12-12
                searchShutter.setOnPreferenceChangeListener(null);
            }
            volZoom.setOnPreferenceChangeListener(null);
            longFocus.setOnPreferenceChangeListener(null);
            preFocus.setOnPreferenceChangeListener(null);
            if (storeExtSd != null)
                storeExtSd.setOnPreferenceChangeListener(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBoxes();
        setListeners(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setListeners(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setListeners(false);
    }

    public void checkBoxes() {
        if (mIsZoomSupported) {
            if (volUpShutter.isChecked() || volDownShutter.isChecked()) {
                volZoom.setEnabled(false);
                volZoom.setChecked(false);
            } else {
                volZoom.setEnabled(true);
            }
            if (volZoom.isChecked()) {
                volUpShutter.setEnabled(false);
                volDownShutter.setEnabled(false);
                /* no need to update checked, both must be unchecked anyway */
            } else {
                volUpShutter.setEnabled(true);
                volDownShutter.setEnabled(true);
            }
        }

        if (longFocus.isChecked()) {
            preFocus.setEnabled(false);
            preFocus.setChecked(false);
        } else {
            preFocus.setEnabled(true);
        }

        if (preFocus.isChecked()) {
            longFocus.setEnabled(false);
            longFocus.setChecked(false);
        } else {
            longFocus.setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        CheckBoxPreference checkBox = (CheckBoxPreference)preference;
        boolean checked = (Boolean)value;

        if (checkBox == volUpShutter || checkBox == volDownShutter) {
            if (mIsZoomSupported) {
                boolean up = checkBox == volUpShutter;

                if (checked) {
                    volZoom.setEnabled(false);
                } else {
                    boolean upChecked = up ? checked : volUpShutter.isChecked();
                    boolean downChecked = !up ? checked : volDownShutter.isChecked();
                    if (!upChecked && !downChecked) {
                        volZoom.setEnabled(true);
                    }
                }
            }
        } else if (checkBox == volZoom) {
            volUpShutter.setEnabled(!checked);
            volDownShutter.setEnabled(!checked);
        } else if (checkBox == longFocus) {
            preFocus.setEnabled(!checked);
        } else if (checkBox == preFocus) {
            longFocus.setEnabled(!checked);
        } else if (checkBox == storeExtSd) {
            ImageManager.updateStorageDirectory(this);
        } else if (checkBox == shutterMute) {
            mSharedPref.edit().putBoolean("shutter_mute", checked).commit();
        }
        return true;
    }
}
