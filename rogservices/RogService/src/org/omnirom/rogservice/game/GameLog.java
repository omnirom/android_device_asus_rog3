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

package org.omnirom.rogservice.game;

import android.content.Context;
import android.os.Build;
import com.android.internal.util.FrameworkStatsLog;
import android.service.notification.StatusBarNotification;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameLog {
    private static final boolean DEBUG = false;
    private static final SimpleDateFormat FORMAT;
    private static final String[] GAME_DND_MSGS;
    public static final int GAME_DND_SIZE;
    private static final long[] GAME_DND_TIMES;
    private static final int[] GAME_DND_TYPES;
    private static final String[] GAME_NETWORK_MSGS;
    public static final int GAME_NETWORK_SIZE;
    private static final long[] GAME_NETWORK_TIMES;
    private static final int[] GAME_NETWORK_TYPES;
    private static final String[] SCREEN_OFF_FIREWALL_MSGS;
    public static final int SCREEN_OFF_FIREWALL_SIZE;
    private static final long[] SCREEN_OFF_FIREWALL_TIMES;
    private static final int[] SCREEN_OFF_FIREWALL_TYPES;
    private static final String TAG = "AsusGameModeLog";
    private static final int TYPE_GAME_DND_ACTIVITY_WHITE_LIST = 7;
    private static final int TYPE_GAME_DND_INTERCEPTED = 3;
    private static final int TYPE_GAME_DND_MODE_CHANGE = 1;
    private static final int TYPE_GAME_DND_NOTIFICATION_INTERCEPTED = 4;
    private static final int TYPE_GAME_DND_NOTIFY = 5;
    private static final int TYPE_GAME_DND_POLICY_CHANGE = 2;
    private static final int TYPE_GAME_DND_TOP_PACKAGE = 6;
    private static final int TYPE_GAME_NETWORK_BLACK_UID = 1003;
    private static final int TYPE_GAME_NETWORK_MODE_CHANGE = 1001;
    private static final int TYPE_GAME_NETWORK_UID_FORGROUND = 1002;
    private static final int TYPE_SCREEN_OFF_CDN_UPDATE = 2003;
    private static final int TYPE_SCREEN_OFF_DYNAMIC_UID_CHANGE = 2004;
    private static final int TYPE_SCREEN_OFF_FIREWALL_CHANGE = 2001;
    private static final int TYPE_SCREEN_OFF_UID_CHANGE = 2002;
    private static Context mContext;
    private static int sGameDndNext;
    private static int sGameDndSize;
    private static int sGameNetworkNext;
    private static int sGameNetworkSize;
    private static int sScreenOffFirewallNext;
    private static int sScreenOffFirewallSize;

    static {
        boolean z = Build.IS_DEBUGGABLE;
        int i = FrameworkStatsLog.APP_PROCESS_DIED__IMPORTANCE__IMPORTANCE_BACKGROUND;
        int i2 = z ? 400 : 200;
        GAME_DND_SIZE = i2;
        int i3 = Build.IS_DEBUGGABLE ? 400 : 200;
        GAME_NETWORK_SIZE = i3;
        if (!Build.IS_DEBUGGABLE) {
            i = 200;
        }
        SCREEN_OFF_FIREWALL_SIZE = i;
        GAME_DND_TIMES = new long[i2];
        GAME_DND_TYPES = new int[i2];
        GAME_DND_MSGS = new String[i2];
        GAME_NETWORK_TIMES = new long[i3];
        GAME_NETWORK_TYPES = new int[i3];
        GAME_NETWORK_MSGS = new String[i3];
        SCREEN_OFF_FIREWALL_TIMES = new long[i];
        SCREEN_OFF_FIREWALL_TYPES = new int[i];
        SCREEN_OFF_FIREWALL_MSGS = new String[i];
        FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    }

    public GameLog(Context context) {
        mContext = context;
    }

    public static void gameDndModeChange(String pkg, boolean lock, String mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("from=");
        sb.append(pkg);
        sb.append(", ");
        sb.append(mode);
        sb.append(" is ");
        sb.append(lock ? "On" : "Off");
        gameDndAppend(1, sb.toString());
    }

    public static void gameDndPolicyChange(String reason, String config) {
        gameDndAppend(2, "what=" + reason + ", policy=" + config);
    }

    public static void gameDndIntercepted(String pkg, String reason) {
        gameDndAppend(3, "pkg=" + pkg + ", intercepted=" + reason);
    }

    public static void gameDndNotificationIntercepted(StatusBarNotification sbn, String reason) {
        if (sbn == null) {
            return;
        }
        gameDndAppend(4, sbn.getKey() + "," + reason);
    }

    public static void gameDndNotify(String pkg, String to, String reason) {
        gameDndAppend(5, "pkg=" + pkg + ", notify=" + to + ", reason=" + reason);
    }

    public static void gameDndTopPackage(String pkg, String reason) {
        gameDndAppend(6, pkg + " is top, so " + reason + " don't block it");
    }

    public static void gameDndActivityWhiteList(String pkg, long time, String reason) {
        gameDndAppend(7, "pkg=" + pkg + ", reason=" + reason + ", time=" + time);
    }

    public static void gameNetworkModeChange(boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("firewall=");
        sb.append(enable ? "On" : "Off");
        gameNetworkAppend(TYPE_GAME_NETWORK_MODE_CHANGE, sb.toString());
    }

    public static void gameNetworkUidForground(int uid, boolean foreground) {
        StringBuilder sb = new StringBuilder();
        sb.append("uid=");
        sb.append(uid);
        sb.append(", state=");
        sb.append(foreground ? "foreground" : "background");
        gameNetworkAppend(TYPE_GAME_NETWORK_UID_FORGROUND, sb.toString());
    }

    public static void gameNetworkBlackUid(int uid, boolean add) {
        StringBuilder sb = new StringBuilder();
        sb.append("uid=");
        sb.append(uid);
        sb.append(", ");
        sb.append(add ? "add" : "remove");
        gameNetworkAppend(TYPE_GAME_NETWORK_BLACK_UID, sb.toString());
    }

    public static void screenOffModeChange(boolean enable) {
        StringBuilder sb = new StringBuilder();
        sb.append("firewall=");
        sb.append(enable ? "On" : "Off");
        screenOffFirewallAppend(TYPE_SCREEN_OFF_FIREWALL_CHANGE, sb.toString());
    }

    public static void screenOffUidChange(String addList, String delList) {
        screenOffFirewallAppend(TYPE_SCREEN_OFF_UID_CHANGE, "add=" + addList + ", del=" + delList);
    }

    public static void screenOffCdnUpdate(long time) {
        screenOffFirewallAppend(TYPE_SCREEN_OFF_CDN_UPDATE, "time=" + time);
    }

    public static void screenOffDynamicUidChange(String type, String list) {
        screenOffFirewallAppend(TYPE_SCREEN_OFF_DYNAMIC_UID_CHANGE, "type=" + type + ", list=" + list);
    }

    private static String typeToString(int type) {
        switch (type) {
            case 1:
                return "game_mode_change";
            case 2:
                return "game_policy_change";
            case 3:
                return "game_intercepted";
            case 4:
                return "game_notification_intercepted";
            case 5:
                return "game_notify";
            case 6:
                return "game_top_package";
            case 7:
                return "activity_white_list";
            case 1001:
                return "network_change";
            case 1002:
                return "uid_foreground";
            case 1003:
                return "black_uid";
            case TYPE_SCREEN_OFF_FIREWALL_CHANGE:
                return "firewall_change";
            case TYPE_SCREEN_OFF_UID_CHANGE:
                return "uid_change";
            case TYPE_SCREEN_OFF_CDN_UPDATE:
                return "cdn_update";
            case TYPE_SCREEN_OFF_DYNAMIC_UID_CHANGE:
                return "dynamic_white_uid";
            default:
                return "unknown";
        }
    }

    private static void gameDndAppend(int type, String msg) {
        String[] strArr = GAME_DND_MSGS;
        synchronized (strArr) {
            GAME_DND_TIMES[sGameDndNext] = System.currentTimeMillis();
            int[] iArr = GAME_DND_TYPES;
            int i = sGameDndNext;
            iArr[i] = type;
            strArr[i] = msg;
            int i2 = GAME_DND_SIZE;
            sGameDndNext = (i + 1) % i2;
            int i3 = sGameDndSize;
            if (i3 < i2) {
                sGameDndSize = i3 + 1;
            }
        }
    }

    private static void gameNetworkAppend(int type, String msg) {
        String[] strArr = GAME_NETWORK_MSGS;
        synchronized (strArr) {
            GAME_NETWORK_TIMES[sGameNetworkNext] = System.currentTimeMillis();
            int[] iArr = GAME_NETWORK_TYPES;
            int i = sGameNetworkNext;
            iArr[i] = type;
            strArr[i] = msg;
            int i2 = GAME_NETWORK_SIZE;
            sGameNetworkNext = (i + 1) % i2;
            int i3 = sGameNetworkSize;
            if (i3 < i2) {
                sGameNetworkSize = i3 + 1;
            }
        }
    }

    private static void screenOffFirewallAppend(int type, String msg) {
        String[] strArr = SCREEN_OFF_FIREWALL_MSGS;
        synchronized (strArr) {
            SCREEN_OFF_FIREWALL_TIMES[sScreenOffFirewallNext] = System.currentTimeMillis();
            int[] iArr = SCREEN_OFF_FIREWALL_TYPES;
            int i = sScreenOffFirewallNext;
            iArr[i] = type;
            strArr[i] = msg;
            int i2 = SCREEN_OFF_FIREWALL_SIZE;
            sScreenOffFirewallNext = (i + 1) % i2;
            int i3 = sScreenOffFirewallSize;
            if (i3 < i2) {
                sScreenOffFirewallSize = i3 + 1;
            }
        }
    }

    public static void dumpGameDnd(PrintWriter pw, String prefix) {
        synchronized (GAME_DND_MSGS) {
            int i = sGameDndNext - sGameDndSize;
            int i2 = GAME_DND_SIZE;
            int start = (i + i2) % i2;
            for (int i3 = 0; i3 < sGameDndSize; i3++) {
                int j = (start + i3) % GAME_DND_SIZE;
                pw.print(prefix);
                pw.print(FORMAT.format(new Date(GAME_DND_TIMES[j])));
                pw.print(' ');
                pw.print(typeToString(GAME_DND_TYPES[j]));
                pw.print(": ");
                pw.println(GAME_DND_MSGS[j]);
            }
        }
    }

    public static void dumpGameNetwork(PrintWriter pw, String prefix) {
        synchronized (GAME_NETWORK_MSGS) {
            int i = sGameNetworkNext - sGameNetworkSize;
            int i2 = GAME_NETWORK_SIZE;
            int start = (i + i2) % i2;
            for (int i3 = 0; i3 < sGameNetworkSize; i3++) {
                int j = (start + i3) % GAME_NETWORK_SIZE;
                pw.print(prefix);
                pw.print(FORMAT.format(new Date(GAME_NETWORK_TIMES[j])));
                pw.print(' ');
                pw.print(typeToString(GAME_NETWORK_TYPES[j]));
                pw.print(": ");
                pw.println(GAME_NETWORK_MSGS[j]);
            }
        }
    }

    public static void dumpScreenOffFirewall(PrintWriter pw, String prefix) {
        synchronized (SCREEN_OFF_FIREWALL_MSGS) {
            int i = sScreenOffFirewallNext - sScreenOffFirewallSize;
            int i2 = SCREEN_OFF_FIREWALL_SIZE;
            int start = (i + i2) % i2;
            for (int i3 = 0; i3 < sScreenOffFirewallSize; i3++) {
                int j = (start + i3) % SCREEN_OFF_FIREWALL_SIZE;
                pw.print(prefix);
                pw.print(FORMAT.format(new Date(SCREEN_OFF_FIREWALL_TIMES[j])));
                pw.print(' ');
                pw.print(typeToString(SCREEN_OFF_FIREWALL_TYPES[j]));
                pw.print(": ");
                pw.println(SCREEN_OFF_FIREWALL_MSGS[j]);
            }
        }
    }
}
