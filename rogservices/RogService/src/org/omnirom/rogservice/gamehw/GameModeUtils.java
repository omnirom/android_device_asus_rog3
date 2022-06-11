/*
 * Copyright (C) 2022 The OmniROM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.rogservice.gamehw;

import android.app.AppGlobals;
import android.content.pm.IPackageManager;
import android.media.AudioAttributes;
import android.text.TextUtils;
import java.util.HashSet;
import org.xmlpull.v1.XmlPullParser;

public class GameModeUtils {
    static HashSet sUsageHashSet;

    static {
        int[] iArr;
        HashSet hashSet = new HashSet();
        sUsageHashSet = hashSet;
        hashSet.clear();
        for (int u : AudioAttributes.SDK_USAGES) {
            sUsageHashSet.add(Integer.valueOf(u));
        }
        sUsageHashSet.add(Integer.valueOf((int) GameDndPolicy.USAGE_DND_ALL));
    }

    public static int safeInt(XmlPullParser parser, String att, int defValue) {
        String val = parser.getAttributeValue(null, att);
        return tryParseInt(val, defValue);
    }

    public static int tryParseInt(String value, int defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    public static boolean safeBoolean(XmlPullParser parser, String att, boolean defValue) {
        String val = parser.getAttributeValue(null, att);
        return safeBoolean(val, defValue);
    }

    public static boolean safeBoolean(String val, boolean defValue) {
        return TextUtils.isEmpty(val) ? defValue : Boolean.parseBoolean(val);
    }

    public static boolean validUsage(int usage) {
        return sUsageHashSet.contains(Integer.valueOf(usage));
    }

    public static int getUsage(String usageString) {
        if ("USAGE_UNKNOWN".equals(usageString)) {
            return 0;
        }
        if ("USAGE_MEDIA".equals(usageString)) {
            return 1;
        }
        if ("USAGE_VOICE_COMMUNICATION".equals(usageString)) {
            return 2;
        }
        if ("USAGE_VOICE_COMMUNICATION_SIGNALLING".equals(usageString)) {
            return 3;
        }
        if ("USAGE_ALARM".equals(usageString)) {
            return 4;
        }
        if ("USAGE_NOTIFICATION".equals(usageString)) {
            return 5;
        }
        if ("USAGE_NOTIFICATION_RINGTONE".equals(usageString)) {
            return 6;
        }
        if ("USAGE_NOTIFICATION_COMMUNICATION_REQUEST".equals(usageString)) {
            return 7;
        }
        if ("USAGE_NOTIFICATION_COMMUNICATION_INSTANT".equals(usageString)) {
            return 8;
        }
        if ("USAGE_NOTIFICATION_COMMUNICATION_DELAYED".equals(usageString)) {
            return 9;
        }
        if ("USAGE_NOTIFICATION_EVENT".equals(usageString)) {
            return 10;
        }
        if ("USAGE_ASSISTANCE_ACCESSIBILITY".equals(usageString)) {
            return 11;
        }
        if ("USAGE_ASSISTANCE_NAVIGATION_GUIDANCE".equals(usageString)) {
            return 12;
        }
        if ("USAGE_ASSISTANCE_SONIFICATION".equals(usageString)) {
            return 13;
        }
        if ("USAGE_GAME".equals(usageString)) {
            return 14;
        }
        if (!"USAGE_DND_ALL".equals(usageString)) {
            return 0;
        }
        return GameDndPolicy.USAGE_DND_ALL;
    }

    public static String usageToString(int usage) {
        if (usage == 21399) {
            return "USAGE_DND_ALL";
        }
        return AudioAttributes.usageToString(usage);
    }

    public static int getGameMode(String gameModeString) {
        int mode = GameDndPolicy.GAME_DND_NONE;
        if (gameModeString == null) {
            return mode;
        }
        if (gameModeString.contains(GameDndPolicy.GDN)) {
            mode |= GameDndPolicy.GAME_DND_NOTIFICATION;
        }
        if (gameModeString.contains(GameDndPolicy.GDC)) {
            mode |= GameDndPolicy.GAME_DND_CALL;
        }
        if (gameModeString.contains(GameDndPolicy.GDCA)) {
            return mode | GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY;
        }
        return mode;
    }

    public static String gameModeToString(int gameMode) {
        StringBuilder sb = new StringBuilder();
        if (gameMode == GameDndPolicy.GAME_DND_NONE) {
            sb.append(GameDndPolicy.GMN);
            return sb.toString();
        }
        if ((GameDndPolicy.GAME_DND_NOTIFICATION & gameMode) != 0) {
            if (sb.length() != 0) {
                sb.append(" | ");
            }
            sb.append(GameDndPolicy.GDN);
        }
        if ((GameDndPolicy.GAME_DND_CALL & gameMode) != 0) {
            if (sb.length() != 0) {
                sb.append(" | ");
            }
            sb.append(GameDndPolicy.GDC);
        }
        if ((GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY & gameMode) != 0) {
            if (sb.length() != 0) {
                sb.append(" | ");
            }
            sb.append(GameDndPolicy.GDCA);
        }
        return sb.toString();
    }

    public static boolean checkSystemApp(String packageName) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int match = -3;
        try {
            match = pm.checkSignatures(packageName, "android");
        } catch (Exception e) {
        }
        return match == 0;
    }

    public static boolean checkSystemApp(int uid) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int match = -3;
        try {
            match = pm.checkUidSignatures(uid, 1000);
        } catch (Exception e) {
        }
        return match == 0;
    }
}
