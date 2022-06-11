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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.net.IOemNetd;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.omnirom.rogservice.gamehw.GameNetworkPolicy;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class GameNetworkHelper {
    public static final String CHAIN_CN_SCREEN_OFF = "cn_screen_off";
    public static final String CHAIN_GAME = "game";
    public static final String CHAIN_LIMIT = "limit";
    public static final String CHAIN_RESTRICT = "restrict";
    public static final String CHAIN_SCREEN_OFF = "screen_off";
    private static final String TAG = "GameNetworkHelper";
    private CNScreenOffFirewall mCNScreenOffFirewall;
    private static IOemNetd mOemNetdService;
    private final Context mContext;
    private FirewallMonitor mFirewallMonitor;
    private final GameDndHelper mGameDndHelper;
    private boolean mGameFirewallMode;
    private final GameNetworkHandler mHandler;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private PackageManager mPackageManager;
    private GameNetworkPolicy mPolicy;
    private List<String> mRestrictInterfaces;
    private ScreenOffFirewall mScreenOffFirewall;
    private HashMap<Integer, List<String>> mUidRejectInterface;
    private final ArrayList<GameNetworkCallback> mCallbacks = new ArrayList<>();
    private Set<Integer> mForegroundUids = new HashSet();
    private Map<Integer, String> mLastNotifyRestrictUid = new HashMap();

    public GameNetworkHelper(Context context, Looper looper, GameDndHelper gameDndHelper) {
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                mHandler.obtainMessage(2271, 0).sendToTarget();
            }
        };
        mNetworkCallback = networkCallback;
        mContext = context;
        GameNetworkHandler gameNetworkHandler = new GameNetworkHandler(looper);
        mHandler = gameNetworkHandler;
        mFirewallMonitor = new FirewallMonitor(context, gameNetworkHandler);
        if (isCNSKU()) {
            mCNScreenOffFirewall = new CNScreenOffFirewall(context, looper, mOemNetdService);
        } else {
            mScreenOffFirewall = new ScreenOffFirewall(context, looper);
        }
        mGameDndHelper = gameDndHelper;
        mPolicy = new GameNetworkPolicy();
        mPackageManager = context.getPackageManager();
        ((ConnectivityManager) context.getSystemService(ConnectivityManager.class)).registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
    }

    public void onSystemServicesReady() {
        ScreenOffFirewall screenOffFirewall = mScreenOffFirewall;
        if (screenOffFirewall != null) {
            screenOffFirewall.onSystemServicesReady();
        }
    }

    public GameNetworkPolicy getNetworkPolicy() {
        GameNetworkPolicy copy;
        synchronized (mPolicy) {
            copy = mPolicy.copy();
        }
        return copy;
    }

    public void initFirewall() {
        synchronized (mPolicy) {
            GameNetworkPolicy newPolicy = mPolicy.copy();
            Iterator it = newPolicy.reject_uid.iterator();
            while (it.hasNext()) {
                int uid = ((Integer) it.next()).intValue();
                setBlackListInner(uid, true);
            }
        }
    }

    public void setGameFirewallMode(boolean gameFirewallMode) {
        if (gameFirewallMode == mGameFirewallMode) {
            Slog.w(TAG, "setGameFirewallMode: already " + gameFirewallMode);
            return;
        }
        mGameFirewallMode = gameFirewallMode;
        mHandler.obtainMessage(2266, gameFirewallMode ? 1 : 0, 0).sendToTarget();
    }

    public boolean getGameFirewallMode() {
        return mGameFirewallMode;
    }

    public void setBlackList(int uid, boolean add) {
        mHandler.obtainMessage(2267, uid, add ? 1 : 0, 0).sendToTarget();
    }

    public int[] getBlackList() {
        int[] uids = new int[0];
        GameNetworkPolicy newPolicy = getNetworkPolicy();
        Iterator it = newPolicy.reject_uid.iterator();
        while (it.hasNext()) {
            int uid = ((Integer) it.next()).intValue();
            uids = ArrayUtils.appendInt(uids, uid);
        }
        return uids;
    }

    public void setUidForeground(int uid, boolean foreground) {
        mHandler.obtainMessage(2268, uid, foreground ? 1 : 0, 0).sendToTarget();
    }

    public boolean blockForGameFirewall(int uid) {
        GameNetworkPolicy newPolicy = getNetworkPolicy();
        return mGameFirewallMode && newPolicy.reject_uid.contains(Integer.valueOf(uid)) && !mForegroundUids.contains(Integer.valueOf(uid));
    }

    public void setUidRejectInterface(HashMap<Integer, List<String>> uidRejectInterface) {
        Message msg = mHandler.obtainMessage();
        msg.what = 2269;
        msg.obj = uidRejectInterface;
        mHandler.sendMessage(msg);
    }

    public void setRestrictInterfaces(List<String> restrictInterfaces) {
        Message msg = mHandler.obtainMessage();
        msg.what = 2270;
        msg.obj = restrictInterfaces;
        mHandler.sendMessage(msg);
    }

    public void readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        GameNetworkPolicy policy = GameNetworkPolicy.readXml(parser);
        if (policy != null) {
            synchronized (mPolicy) {
                setPolicyLocked(policy);
            }
        }
    }

    public void writeXml(XmlSerializer out, Integer version) throws IOException {
        mPolicy.writeXml(out, version);
    }

    public void addCallback(GameNetworkCallback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(GameNetworkCallback callback) {
        mCallbacks.remove(callback);
    }

    public String dumpScreenOffFirewall() {
        ScreenOffFirewall screenOffFirewall;
        CNScreenOffFirewall cNScreenOffFirewall;
        if (isCNSKU() && (cNScreenOffFirewall = mCNScreenOffFirewall) != null) {
            return cNScreenOffFirewall.dump();
        }
        if (!isCNSKU() && (screenOffFirewall = mScreenOffFirewall) != null) {
            return screenOffFirewall.dump();
        }
        return "";
    }

    private void setBlackListPolicyInner(int uid, boolean add) {
        synchronized (mPolicy) {
            GameNetworkPolicy gameNetworkPolicy = mPolicy;
            if (gameNetworkPolicy == null) {
                return;
            }
            GameNetworkPolicy newPolicy = gameNetworkPolicy.copy();
            Set<Integer> uids = new HashSet<>(newPolicy.reject_uid);
            if (uids.contains(Integer.valueOf(uid)) == add) {
                StringBuilder sb = new StringBuilder();
                sb.append("uid ");
                sb.append(uid);
                sb.append(": already ");
                sb.append(add ? "add" : "remove");
                Slog.w(TAG, sb.toString());
                return;
            }
            if (add) {
                uids.add(Integer.valueOf(uid));
            } else {
                uids.remove(Integer.valueOf(uid));
            }
            newPolicy.reject_uid = new ArrayList(uids);
            setPolicyLocked(newPolicy);
            setBlackListInner(uid, add);
            GameLog.gameNetworkBlackUid(uid, add);
        }
    }

    private void setBlackListInner(int uid, boolean add) {
        try {
            setGameApps(uid, add);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set GameFirewallWhiteList", e);
        }
    }

    private void setUidForegroundInner(int uid, boolean foreground) {
        HashMap<Integer, List<String>> hashMap;
        try {
            if (mPolicy == null) {
                return;
            }
            Set<Integer> uids = new HashSet<>(mPolicy.reject_uid);
            if (uids.contains(Integer.valueOf(uid))) {
                setGameApps(uid, !foreground);
                synchronized (mForegroundUids) {
                    if ((!mForegroundUids.contains(Integer.valueOf(uid))) == foreground) {
                        if (foreground) {
                            mForegroundUids.add(Integer.valueOf(uid));
                        } else {
                            mForegroundUids.remove(Integer.valueOf(uid));
                        }
                        GameLog.gameNetworkUidForground(uid, foreground);
                    }
                }
            }
            if (foreground && (hashMap = mUidRejectInterface) != null && mPackageManager != null && hashMap.get(Integer.valueOf(uid)) != null && ActivityManager.checkUidPermission("android.permission.INTERNET", uid) == 0) {
                String[] packageNames = mPackageManager.getPackagesForUid(uid);
                List<String> rejectInterfaces = mUidRejectInterface.get(Integer.valueOf(uid));
                if (rejectInterfaces != null && rejectInterfaces.size() > 0 && packageNames != null && packageNames.length > 0) {
                    for (String packageName : packageNames) {
                        if (packageNames.length != 1 && !mGameDndHelper.getTopPackages().contains(packageName)) {
                        }
                        mFirewallMonitor.requestNotifyRestrictUidForeground(uid, packageName);
                        mLastNotifyRestrictUid.put(Integer.valueOf(uid), packageName);
                        return;
                    }
                }
            } else if (!foreground) {
                mLastNotifyRestrictUid.remove(Integer.valueOf(uid));
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set setUidForegroundInner", e);
        }
    }

    private void setUidRejectInterfaceInner(HashMap<Integer, List<String>> uidRejectInterface) {
        try {
            mUidRejectInterface = uidRejectInterface;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set setUidRejectInterfaceInner", e);
        }
    }

    private void setRestrictInterfacesInner(List<String> restrictInterfaces) {
        try {
            mRestrictInterfaces = restrictInterfaces;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set setRestrictInterfacesInner", e);
        }
    }

    private void checkNetworkAvaiableInner() {
        if (mLastNotifyRestrictUid.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, String> entry : mLastNotifyRestrictUid.entrySet()) {
            int uid = entry.getKey().intValue();
            String packageName = entry.getValue();
            mFirewallMonitor.requestNotifyRestrictUidForeground(uid, packageName);
        }
    }

    private boolean setPolicyLocked(GameNetworkPolicy policy) {
        long identity = Binder.clearCallingIdentity();
        if (policy != null) {
            try {
                boolean policyChanged = !Objects.equals(mPolicy, policy);
                if (policyChanged) {
                    dispatchOnPolicyChanged();
                }
                mPolicy = policy;
                return true;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return false;
    }

    private void dispatchOnPolicyChanged() {
        Iterator<GameNetworkCallback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            GameNetworkCallback callback = it.next();
            callback.onPolicyChanged();
        }
    }

    private void setGameFirewallModeInner(boolean gameFirewallMode) { 
        try {
            if (gameFirewallMode) {
                enableTLChain(CHAIN_GAME, true);
            } else {
                enableTLChain(CHAIN_GAME, false);
            }
            GameLog.gameNetworkModeChange(gameFirewallMode);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set GameFirewallMode", e);
        }
    }

    private class GameNetworkHandler extends Handler {
        static final int MSG_CHECK_NETWORK_AVAILABLE = 2271;
        static final int MSG_SET_FIREWALL_BLACKLIST = 2267;
        static final int MSG_SET_RESTRICT_INTERFACE = 2270;
        static final int MSG_SET_UID_FOREGROUND = 2268;
        static final int MSG_SET_UID_REJECT_INTERFACE = 2269;
        static final int MSG_TURN_GAME_FIREWALL_MODE = 2266;

        GameNetworkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            boolean foreground = false;
            switch (msg.what) {
                case MSG_TURN_GAME_FIREWALL_MODE:
                    if (msg.arg1 == 1) {
                        foreground = true;
                    }
                    boolean gameFirewallMode = foreground;
                    setGameFirewallModeInner(gameFirewallMode);
                    return;
                case MSG_SET_FIREWALL_BLACKLIST:
                    int uid = msg.arg1;
                    if (msg.arg2 == 1) {
                        foreground = true;
                    }
                    setBlackListPolicyInner(uid, foreground);
                    return;
                case MSG_SET_UID_FOREGROUND:
                    int uid2 = msg.arg1;
                    if (msg.arg2 == 1) {
                        foreground = true;
                    }
                    setUidForegroundInner(uid2, foreground);
                    return;
                case MSG_SET_UID_REJECT_INTERFACE:
                    HashMap<Integer, List<String>> uidRejectInterface = (HashMap) msg.obj;
                    setUidRejectInterfaceInner(uidRejectInterface);
                    return;
                case MSG_SET_RESTRICT_INTERFACE:
                    List<String> restrictInterfaces = (List) msg.obj;
                    setRestrictInterfacesInner(restrictInterfaces);
                    return;
                case MSG_CHECK_NETWORK_AVAILABLE:
                    checkNetworkAvaiableInner();
                    return;
                default:
                    return;
            }
        }
    }

    public static class GameNetworkCallback {
        void onPolicyChanged() {
        }
    }

    private static boolean isCNSKU() {
        String sku = SystemProperties.get("ro.vendor.build.asus.sku", "");
        return sku.toLowerCase().startsWith("cn");
    }

    public static void setGameApps(int uid, boolean reject) {
        try {
            if (reject) {
                mOemNetdService.trafficLimitAddGameApp(uid);
            } else {
                mOemNetdService.trafficLimitRemoveGameApp(uid);
            }
        } catch (Exception e) {
        }
    }

    public static void enableTLChain(String chain, boolean enable) {
        int tlChain = 0;
        try {
            if (CHAIN_RESTRICT.equals(chain)) {
                tlChain = 0;
            } else if (CHAIN_GAME.equals(chain)) {
                tlChain = 1;
            } else if (CHAIN_LIMIT.equals(chain)) {
                tlChain = 2;
            } else if (CHAIN_SCREEN_OFF.equals(chain)) {
                tlChain = 3;
            } else if (CHAIN_CN_SCREEN_OFF.equals(chain)) {
                tlChain = 4;
            }
            mOemNetdService.trafficLimitEnableChain(tlChain, enable);
        } catch (Exception e) {
        }
    }

    public static void setScreenOffApps(int uid, boolean reject) {
        try {
            if (reject) {
                mOemNetdService.trafficLimitAddScreenOffApp(uid);
            } else {
                mOemNetdService.trafficLimitRemoveScreenOffApp(uid);
            }
        } catch (Exception e) {
        }
    }
}
