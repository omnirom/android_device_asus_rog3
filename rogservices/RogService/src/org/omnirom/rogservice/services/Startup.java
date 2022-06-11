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
package org.omnirom.rogservice.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;

public class Startup extends BroadcastReceiver {
    private static final String TAG = "OmniRogService";
    private RogService mRogService;

    private static String SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED = "enable_tgpa_aura_effect";
    private static String SETTINGS_SYSTEM_HEADSET_SYNCABLE = "headset_syncable";
    private static String SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME = "notification_expiration_time";
    private static String SETTINGS_SYSTEM_STORAGE_SYNCABLE = "storage_syncable";

    // Secure
    private static String SYSTEM_SCALING_CONTROL = "sys_scaling_ctrl";
    private static String GAME_GENIE_SCALING_CONTROL = "gg_scaling_ctrl";


    @Override
    public void onReceive(final Context context, final Intent bootintent) {
        context.startService(new Intent(context, OmniRogService.class));
        context.startService(new Intent(context, OmniAuraLightService.class));
        AddDefaultValue(context);
    }

    private void AddDefaultValue(Context context) {
        String initialValue = "0";
        String value = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED);
        String value2 = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_HEADSET_SYNCABLE);
        String value3 = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME);
        String value4 = Settings.System.getString(context.getContentResolver(), SETTINGS_SYSTEM_STORAGE_SYNCABLE);
        String value5 = Settings.Secure.getString(context.getContentResolver(), SYSTEM_SCALING_CONTROL);
        String value6 = Settings.Secure.getString(context.getContentResolver(), GAME_GENIE_SCALING_CONTROL);

        if (TextUtils.isEmpty(value)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED, initialValue);
        } else if (TextUtils.isEmpty(value2)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_HEADSET_SYNCABLE, initialValue);
        } else if (TextUtils.isEmpty(value3)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME, initialValue);
        } else if (TextUtils.isEmpty(value4)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_SYSTEM_STORAGE_SYNCABLE, initialValue);
        } else if (TextUtils.isEmpty(value5)) {
            Settings.Secure.putString(context.getContentResolver(), SYSTEM_SCALING_CONTROL, initialValue);
        } else if (TextUtils.isEmpty(value6)) {
            Settings.Secure.putString(context.getContentResolver(), GAME_GENIE_SCALING_CONTROL, initialValue);
        }
    }
}
