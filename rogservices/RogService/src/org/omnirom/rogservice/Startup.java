/*
* Copyright (C) 2022 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.omnirom.rogservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import org.omnirom.rogservice.game.OmniGameModeService;

public class Startup extends BroadcastReceiver {
    private static final String TAG = "OmniRogService";
    private RogService mRogService;

    private static String GAME_MODE_NETWORK_WHITELIST = "game_mode_network_whitelist";
    private static String SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED = "enable_tgpa_aura_effect";
    private static String SETTINGS_SYSTEM_HEADSET_SYNCABLE = "headset_syncable";
    private static String SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME = "notification_expiration_time";
    private static String SETTINGS_SYSTEM_STORAGE_SYNCABLE = "storage_syncable";
    private static String SCREEN_OFF_FIREWALL_DEBUG = "screen_off_firewall_debug";
    private static String SCREEN_OFF_FIREWALL_DELAY = "screen_off_firewall_delay";
    private static String SCREEN_OFF_FIREWALL_DURATION_CHECK_UID = "screen_off_firewall_duration_check_uid";
    private static String SCREEN_OFF_FIREWALL_ENABLE = "screen_off_firewall_enabled";
    private static String SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_OFF = "screen_off_firewall_intermission_duration_phase_off";
    private static String SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_ON = "screen_off_firewall_intermission_duration_phase_on";
    private static String SCREEN_OFF_FIREWALL_INTERMISSION_ENABLE = "screen_off_firewall_intermission_enable";
    private static String SCREEN_OFF_FIREWALL_TURN_ON = "screen_off_firewall_turn_on";
    private static String CN_SCREEN_OFF_FIREWALL_DELAY = "cn_screen_off_firewall_delay";
    private static String CN_SCREEN_OFF_SETTING_FIREWALL_ENABLE = "cn_screen_off_setting_firewall_enable";
    // Secure
    private static String SYSTEM_SCALING_CONTROL = "sys_scaling_ctrl";
    private static String GAME_GENIE_SCALING_CONTROL = "gg_scaling_ctrl";
    private static String SECURE_AUDIO_OUTPUT_DEVICE = "audio_output_device";
    private static String SECURE_IMMERSIVE_MODE = "enable_immersive_mode_tutorial";
    private static String SETTINGS_SECURE_DISPLAY_MODE_WITH_DOCK = "display_mode_with_dock";


    @Override
    public void onReceive(final Context context, final Intent bootintent) {
        context.startService(new Intent(context, OmniRogService.class));
        context.startService(new Intent(context, OmniAuraLightService.class));
        context.startService(new Intent(context, OmniGameModeService.class));
        AddDefaultValue(context);
    }

    private void AddDefaultValue(Context context) {
        String initialValue = "0";
        String value = Settings.System.getString(context.getContentResolver(), GAME_MODE_NETWORK_WHITELIST);
        String value2 = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED);
        String value3 = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_HEADSET_SYNCABLE);
        String value4 = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME);
        String value5 = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_STORAGE_SYNCABLE);
        String value6 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_DEBUG);
        String value7 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_DELAY);
        String value8 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_DURATION_CHECK_UID);
        String value9 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_ENABLE);
        String value10 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_OFF);
        String value11 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_ON);
        String value12 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_ENABLE);
        String value13 = Settings.System.getString(context.getContentResolver(), SCREEN_OFF_FIREWALL_TURN_ON);
        String value14 = Settings.System.getString(context.getContentResolver(), CN_SCREEN_OFF_FIREWALL_DELAY);
        String value15 = Settings.System.getString(context.getContentResolver(), CN_SCREEN_OFF_SETTING_FIREWALL_ENABLE);

        String value16 = Settings.Secure.getString(context.getContentResolver(), SYSTEM_SCALING_CONTROL);
        String value17 = Settings.Secure.getString(context.getContentResolver(), GAME_GENIE_SCALING_CONTROL);
        String value18 = Settings.Secure.getString(context.getContentResolver(), SECURE_AUDIO_OUTPUT_DEVICE);
        String value19 = Settings.Secure.getString(context.getContentResolver(), SECURE_IMMERSIVE_MODE);
        String value20 = Settings.Secure.getString(context.getContentResolver(), SETTINGS_SECURE_DISPLAY_MODE_WITH_DOCK);

        if (TextUtils.isEmpty(value)) {
            Settings.System.putString(context.getContentResolver(), GAME_MODE_NETWORK_WHITELIST, initialValue);
        } else if (TextUtils.isEmpty(value2)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED, initialValue);
        } else if (TextUtils.isEmpty(value3)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_HEADSET_SYNCABLE, initialValue);
        } else if (TextUtils.isEmpty(value4)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME, initialValue);
        } else if (TextUtils.isEmpty(value5)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_STORAGE_SYNCABLE, initialValue);
        } else if (TextUtils.isEmpty(value6)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_DEBUG, initialValue);
        } else if (TextUtils.isEmpty(value7)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_DELAY, initialValue);
        } else if (TextUtils.isEmpty(value8)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_DURATION_CHECK_UID, initialValue);
        } else if (TextUtils.isEmpty(value9)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_ENABLE, initialValue);
        } else if (TextUtils.isEmpty(value10)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_OFF, initialValue);
        } else if (TextUtils.isEmpty(value11)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_ON, initialValue);
        } else if (TextUtils.isEmpty(value12)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_ENABLE, initialValue);
        } else if (TextUtils.isEmpty(value13)) {
            Settings.System.putString(context.getContentResolver(), SCREEN_OFF_FIREWALL_TURN_ON, initialValue);
        } else if (TextUtils.isEmpty(value14)) {
            Settings.System.putString(context.getContentResolver(), CN_SCREEN_OFF_FIREWALL_DELAY, initialValue);
        } else if (TextUtils.isEmpty(value15)) {
            Settings.System.putString(context.getContentResolver(), CN_SCREEN_OFF_SETTING_FIREWALL_ENABLE, initialValue);
        } else if (TextUtils.isEmpty(value16)) {
            Settings.Secure.putString(context.getContentResolver(), SYSTEM_SCALING_CONTROL, initialValue);
        } else if (TextUtils.isEmpty(value17)) {
            Settings.Secure.putString(context.getContentResolver(), GAME_GENIE_SCALING_CONTROL, initialValue);
        } else if (TextUtils.isEmpty(value18)) {
            Settings.Secure.putString(context.getContentResolver(), SECURE_AUDIO_OUTPUT_DEVICE, initialValue);
        } else if (TextUtils.isEmpty(value18)) {
            Settings.Secure.putString(context.getContentResolver(), SECURE_IMMERSIVE_MODE, initialValue);
        } else if (TextUtils.isEmpty(value20)) {
            Settings.Secure.putString(context.getContentResolver(), SETTINGS_SECURE_DISPLAY_MODE_WITH_DOCK, initialValue);
        }
    }
}
