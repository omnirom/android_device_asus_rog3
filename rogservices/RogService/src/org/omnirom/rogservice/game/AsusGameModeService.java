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

import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import org.omnirom.rogservice.gamehw.AsusGameModeInternal;
import org.omnirom.rogservice.gamehw.GameDndPolicy;
import org.omnirom.rogservice.gamehw.GameModeUtils;
import org.omnirom.rogservice.gamehw.GameNetworkPolicy;
import org.omnirom.rogservice.gamehw.IAsusGameModeService;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AsusGameModeService {
    private static final String ATTR_VERSION = "version";
    static final boolean DBG;
    private static final int DB_VERSION = 1;
    private static final long INIT_DELAY = 5000;
    private static final long LOAD_DELAY = 3000;
    private static final String PERM_MANAGE_GAME_DND = "com.asus.permission.MANAGE_GAME_DND";
    private static final String PERM_MANAGE_GAME_FIREWALL = "asus.permission.MANAGE_GAME_FIREWALL";
    private static final String PERM_MANAGE_SCREEN_RECORDER_DND = "com.asus.permission.MANAGE_SCREEN_RECORDER_DND";
    public static final String TAG = "AsusGameModeService";
    private static final String TAG_ASUS_GAME_POLICY = "asus-game-policy";
    private static final long WRITE_DELAY;
    private GameDndHelper mGameDndHelper;
    private GameNetworkHelper mGameNetworkHelper;
    private AsusGameModeHandler mHandler;
    private PackageManager mPackageManager;
    private AtomicFile mPolicyFile;
    private SettingsObserver mSettingsObserver;
    private final String GAME_MODE_NETWORK_WHITELIST = "game_mode_network_whitelist";
    private final HandlerThread mBackgroundHandlerThread = new HandlerThread("AsusGameModeHandlerThread", 0);
    private Map<Integer, List<String>> mUidPackages = new HashMap();
    private static Context mContext;

    static {
        boolean isLoggable = Log.isLoggable(TAG, 3);
        DBG = isLoggable;
        WRITE_DELAY = isLoggable ? 1000L : 10000;
    }

    public AsusGameModeService(Context context) {
        mContext = context;
    }

    public void disable() {
    }

    public void onStart() {
        mPackageManager = mContext.getPackageManager();
        File systemDir = new File(Environment.getExternalStorageDirectory(), "Rogservices");
        AtomicFile atomicFile = new AtomicFile(new File(systemDir, "asus_game_policy.xml"));
        mPolicyFile = atomicFile;
        boolean useDefaultPolicy = !atomicFile.getBaseFile().exists();
        IBinder b = ServiceManager.getService("network_management");
        mBackgroundHandlerThread.start();
        mHandler = new AsusGameModeHandler(mBackgroundHandlerThread.getLooper());
        GameDndHelper gameDndHelper = new GameDndHelper(mContext, mBackgroundHandlerThread.getLooper(), useDefaultPolicy);
        mGameDndHelper = gameDndHelper;
        gameDndHelper.addCallback(new GameDndHelper.Callback() {
            @Override
            public void onGameDndChanged(String from, boolean lock) {
                GameLog.gameDndModeChange(from, lock, "game_notification");
                long identity = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(mContext.getContentResolver(), "game_dnd_lock", lock ? 1 : 0);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onCallGameDndChanged(String from, boolean lock) {
                GameLog.gameDndModeChange(from, lock, "game_call");
                long identity = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(mContext.getContentResolver(), "call_game_dnd_lock", lock ? 1 : 0);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onScreenRecorderDndChanged(String from, boolean lock) {
                GameLog.gameDndModeChange(from, lock, "screen_recorder_dnd");
                long identity = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(mContext.getContentResolver(), "screen_recorder_dnd_lock", lock ? 1 : 0);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onGameDndPolicyChanged(String reason, GameDndPolicy policy) {
                GameLog.gameDndPolicyChange(reason, policy.toString());
                savePolicyFile();
                long identity = Binder.clearCallingIdentity();
                boolean ignore = false;
                try {
                    if ("readXml".equals(reason)) {
                        String etag = Settings.Global.getString(mContext.getContentResolver(), "game_dnd_policy_config_etag");
                        if (etag != null) {
                            ignore = true;
                        }
                    }
                    if (!ignore) {
                        String val = Integer.toString(policy.hashCode());
                        Settings.Global.putString(mContext.getContentResolver(), "game_dnd_policy_config_etag", val);
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onCustomActivityDndChanged(String from, boolean lock) {
                GameLog.gameDndModeChange(from, lock, "game_custom_activity");
                long identity = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(mContext.getContentResolver(), "custom_activity_dnd_lock", lock ? 1 : 0);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onAlarmGameDndChanged(String from, boolean lock) {
                GameLog.gameDndModeChange(from, lock, "game_alarm");
                long identity = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(mContext.getContentResolver(), "alarm_game_dnd_lock", lock ? 1 : 0);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onInGameModeChanged(String from, boolean inGame) {
                GameLog.gameDndModeChange(from, inGame, "in_game");
                long identity = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(mContext.getContentResolver(), "in_game_mode", inGame ? 1 : 0);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        });
        GameNetworkHelper gameNetworkHelper = new GameNetworkHelper(mContext, mBackgroundHandlerThread.getLooper(), mGameDndHelper);
        mGameNetworkHelper = gameNetworkHelper;
        gameNetworkHelper.addCallback(new GameNetworkHelper.GameNetworkCallback() {
            @Override
            void onPolicyChanged() {
                savePolicyFile();
            }
        });
        loadPolicyFile(useDefaultPolicy);
        initHelper();
        new AsusGameModeInternalImpl(mHandler);
        mSettingsObserver = new SettingsObserver();
        try {
            new BinderService();
        } catch (Exception e) {
            Slog.w(TAG, "can't publish service: asus_game_mode_service, " + e.getMessage());
        }
    }

    public void onBootPhase(int phase) {
        GameNetworkHelper gameNetworkHelper;
        if ((gameNetworkHelper = mGameNetworkHelper) != null) {
            gameNetworkHelper.onSystemServicesReady();
        }
    }

    private void initHelper() {
        mHandler.removeMessages(5213);
        mHandler.sendEmptyMessageDelayed(5213, INIT_DELAY);
    }

    public void savePolicyFile() {
        if (!mHandler.hasMessages(5214)) {
            mHandler.sendEmptyMessageDelayed(5214, WRITE_DELAY);
        }
    }

    void readPolicyXml(InputStream stream) throws XmlPullParserException, NumberFormatException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(stream, StandardCharsets.UTF_8.name());
        XmlUtils.beginDocument(parser, TAG_ASUS_GAME_POLICY);
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("game-dnd-policy".equals(parser.getName())) {
                mGameDndHelper.readXml(parser);
            } else if ("game-network-policy".equals(parser.getName())) {
                mGameNetworkHelper.readXml(parser);
            }
        }
    }

    private void writePolicyXml(OutputStream stream, boolean forBackup) throws IOException {
        XmlSerializer out = new FastXmlSerializer();
        out.setOutput(stream, StandardCharsets.UTF_8.name());
        out.startDocument(null, true);
        out.startTag(null, TAG_ASUS_GAME_POLICY);
        out.attribute(null, ATTR_VERSION, Integer.toString(1));
        mGameDndHelper.writeXml(out, null);
        mGameNetworkHelper.writeXml(out, null);
        out.endTag(null, TAG_ASUS_GAME_POLICY);
        out.endDocument();
    }

    private void setGameFirewallModeInner() {
        boolean z = false;
        if (Settings.System.getInt(mContext.getContentResolver(), GAME_MODE_NETWORK_WHITELIST, 0) == 1) {
            z = true;
        }
        boolean gameFirewallMode = z;
        mGameNetworkHelper.setGameFirewallMode(gameFirewallMode);
    }

    private void loadPolicyFile(boolean useDefaultPolicy) {
        if (!mHandler.hasMessages(5215)) {
            AsusGameModeHandler asusGameModeHandler = mHandler;
            asusGameModeHandler.sendMessageDelayed(asusGameModeHandler.obtainMessage(5215, useDefaultPolicy ? 1 : 0, 0), useDefaultPolicy ? 3000L : 0L);
        }
    }

    private void handleLoadPolicyFile(boolean useDefaultPolicy) {
        if (DBG) {
            Slog.d(TAG, "loadPolicyFile");
        }
        if (useDefaultPolicy) {
            handleSavePolicyFile();
        }
        synchronized (mPolicyFile) {
            InputStream infile = null;
            try {
                try {
                    infile = mPolicyFile.openRead();
                    readPolicyXml(infile);
                } catch (NumberFormatException | XmlPullParserException e) {
                    Log.wtf(TAG, "Unable to parse asus game policy", e);
                }
            } catch (IOException e2) {
                Log.wtf(TAG, "Unable to read asus game policy", e2);
            }
            IoUtils.closeQuietly(infile);
        }
    }

    private void handleInitHelper() {
        String[] packageNames;
        mGameNetworkHelper.initFirewall();
        synchronized (mUidPackages) {
            GameNetworkPolicy networkPolicy = mGameNetworkHelper.getNetworkPolicy();
            if (networkPolicy != null && networkPolicy.reject_uid != null && mPackageManager != null) {
                Iterator it = networkPolicy.reject_uid.iterator();
                while (it.hasNext()) {
                    int uid = ((Integer) it.next()).intValue();
                    if (mUidPackages.get(Integer.valueOf(uid)) == null && (packageNames = mPackageManager.getPackagesForUid(uid)) != null && packageNames.length > 0) {
                        List<String> packages = Arrays.asList(packageNames);
                        mUidPackages.put(Integer.valueOf(uid), packages);
                    }
                }
            }
        }
    }

    private void handleSavePolicyFile() {
        if (DBG) {
            Slog.d(TAG, "handleSavePolicyFile");
        }
        synchronized (mPolicyFile) {
            try {
                FileOutputStream stream = mPolicyFile.startWrite();
                try {
                    writePolicyXml(stream, false);
                    mPolicyFile.finishWrite(stream);
                } catch (IOException e) {
                    Slog.w(TAG, "Failed to save policy file, restoring backup", e);
                    mPolicyFile.failWrite(stream);
                }
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to save policy file", e2);
                return;
            }
        }
        BackupManager.dataChanged(mContext.getPackageName());
    }

    private final class AsusGameModeInternalImpl extends AsusGameModeInternal {
        Handler mHandler;

        AsusGameModeInternalImpl(Handler h) {
            mHandler = null;
            mHandler = h;
        }

        public void setGameDndLock(String pkg, boolean lock) {
            boolean isSystemApp = GameModeUtils.checkSystemApp(pkg);
            if (isSystemApp) {
                mGameDndHelper.setGameDndLock(pkg, lock);
                return;
            }
            Slog.d(TAG, pkg + " is not system app, you can't set game dnd lock");
        }

        public boolean getGameDndLock() {
            return mGameDndHelper.getGameDndLock();
        }

        public void setScreenRecorderDndLock(String pkg, boolean lock) {
            boolean isSystemApp = GameModeUtils.checkSystemApp(pkg);
            if (isSystemApp) {
                mGameDndHelper.setScreenRecorderDndLock(pkg, lock);
                return;
            }
            Slog.d(TAG, pkg + " is not system app, you can't set screen recorder dnd lock");
        }

        public boolean getScreenRecorderDndLock() {
            return mGameDndHelper.getScreenRecorderDndLock();
        }

        public void setCallGameDndLock(String pkg, boolean lock) {
            mGameDndHelper.setCallGameDndLock(pkg, lock);
        }

        public boolean getCallGameDndLock() {
            return mGameDndHelper.getCallGameDndLock();
        }

        public int getGameDndMode() {
            return mGameDndHelper.getGameDndMode();
        }

        public boolean isToastFeatureEnabled() {
            return mGameDndHelper.isToastFeatureEnabled();
        }

        public boolean denyOverlayWindow(String pkg, int gameMode) {
            return mGameDndHelper.denyOverlayWindow(pkg, gameMode);
        }

        public boolean isActivityFeatureEnabled() {
            return mGameDndHelper.isActivityFeatureEnabled();
        }

        public boolean denyActivity(String component, int gameMode) {
            return mGameDndHelper.denyActivity(component, gameMode);
        }

        public boolean isSoundFeatureEnabled() {
            return mGameDndHelper.isSoundFeatureEnabled();
        }

        public boolean denySound(String pkg, int usage, int gameMode) {
            return mGameDndHelper.denySound(pkg, usage, gameMode);
        }

        public boolean isVibrationFeatureEnabled() {
            return mGameDndHelper.isVibrationFeatureEnabled();
        }

        public boolean denyVibration(String pkg, int usage, int gameMode) {
            return mGameDndHelper.denyVibration(pkg, usage, gameMode);
        }

        public void setTopPackage(String pkg, boolean add) {
            mGameDndHelper.setTopPackage(pkg, add);
        }

        public boolean isCustomActivityFeatureEnabled() {
            return mGameDndHelper.isCustomActivityFeatureEnabled();
        }

        public boolean denyCustomActivity(String component, int gameMode) {
            return mGameDndHelper.denyCustomActivity(component, gameMode);
        }

        public void startCustomActivity(Bundle bundle) {
            mGameDndHelper.startCustomActivity(bundle);
        }

        public void setUidForeground(int uid, boolean foreground) {
            mGameNetworkHelper.setUidForeground(uid, foreground);
        }

        public boolean isCallActivity(String component) {
            return mGameDndHelper.isCallActivity(component);
        }

        public void notifyScreenRecorder(Bundle bundle) {
            mGameDndHelper.notifyScreenRecorder(bundle);
        }

        public void recordOverlayWindow(String pkg, long time, boolean add) {
            mGameDndHelper.recordOverlayWindow(pkg, time, add);
        }

        public boolean showCustomView(String component, int gameMode) {
            return mGameDndHelper.showCustomView(component, gameMode);
        }

        public boolean getAlarmGameDndLock() {
            return mGameDndHelper.getAlarmGameDndLock();
        }

        public boolean isInGameMode() {
            return mGameDndHelper.isInGameMode();
        }

        public boolean isCallNotification(String pkg, String ntag, String category, String channelId, String groupKey) {
            return mGameDndHelper.isCallNotification(pkg, ntag, category, channelId, groupKey);
        }

        public boolean blockForGameFirewall(int uid) {
            return mGameNetworkHelper.blockForGameFirewall(uid);
        }

        public void setActivityWhiteList(String pkg, String reason) {
            mGameDndHelper.setActivityWhiteList(pkg, reason);
        }

        public boolean isActivityWhiteList(String pkg) {
            return mGameDndHelper.isActivityWhiteList(pkg);
        }

        public void setUidRejectInterface(HashMap<Integer, List<String>> uidRejectInterface) {
            mGameNetworkHelper.setUidRejectInterface(uidRejectInterface);
        }

        public void setRestrictInterfaces(List<String> restrictInterfaces) {
            mGameNetworkHelper.setRestrictInterfaces(restrictInterfaces);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri mGameFirewallModeUri;

        public SettingsObserver() {
            super(new Handler());
            Uri uriFor = Settings.System.getUriFor(GAME_MODE_NETWORK_WHITELIST);
            mGameFirewallModeUri = uriFor;
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(uriFor, false, this, -1);
            setGameFirewallModeInner();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null && mGameFirewallModeUri.equals(uri)) {
                setGameFirewallModeInner();
            }
        }
    }

    private class AsusGameModeHandler extends Handler {
        static final int MSG_INIT_HELPER = 5213;
        static final int MSG_LOAD_POLICY_FILE = 5215;
        static final int MSG_SAVE_POLICY_FILE = 5214;

        AsusGameModeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT_HELPER:
                    handleInitHelper();
                    return;
                case MSG_SAVE_POLICY_FILE:
                    handleSavePolicyFile();
                    return;
                case MSG_LOAD_POLICY_FILE :
                    boolean z = true;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    boolean useDefaultPolicy = z;
                    handleLoadPolicyFile(useDefaultPolicy);
                    return;
                default:
                    return;
            }
        }
    }

    private final class BinderService extends IAsusGameModeService.Stub {
        private BinderService() {
        }

        public void setGameFirewallBlackList(String callingPkg, int uid, boolean add) {
            String[] packageNames;
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_FIREWALL, TAG);
            synchronized (mUidPackages) {
                if (mPackageManager != null && mUidPackages.get(Integer.valueOf(uid)) == null &&
                   (packageNames = mPackageManager.getPackagesForUid(uid)) != null && packageNames.length > 0) {
                    List<String> packages = Arrays.asList(packageNames);
                    mUidPackages.put(Integer.valueOf(uid), packages);
                }
            }
            mGameNetworkHelper.setBlackList(uid, add);
        }

        public int[] getGameFirewallBlackList(String callingPkg) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_FIREWALL, TAG);
            return mGameNetworkHelper.getBlackList();
        }

        public void setGameDndLock(String callingPkg, boolean lock) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_DND, TAG);
            mGameDndHelper.setGameDndLock(callingPkg, lock);
        }

        public boolean getGameDndLock() {
            return mGameDndHelper.getGameDndLock();
        }

        public void setScreenRecorderDndLock(String callingPkg, boolean lock) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_SCREEN_RECORDER_DND, TAG);
            mGameDndHelper.setScreenRecorderDndLock(callingPkg, lock);
        }

        public boolean getScreenRecorderDndLock() {
            return mGameDndHelper.getScreenRecorderDndLock();
        }

        public void setCallGameDndLock(String callingPkg, boolean lock) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_DND, TAG);
            mGameDndHelper.setCallGameDndLock(callingPkg, lock);
        }

        public boolean getCallGameDndLock() {
            return mGameDndHelper.getCallGameDndLock();
        }

        public int getGameDndMode() {
            return mGameDndHelper.getGameDndMode();
        }

        public boolean isSoundFeatureEnabled() {
            return mGameDndHelper.isSoundFeatureEnabled();
        }

        public boolean denySound(String pkg, int usage, int gameMode) {
            return mGameDndHelper.denySound(pkg, usage, gameMode);
        }

        public void setGameDndPolicy(String callingPkg, GameDndPolicy policy) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_DND, TAG);
            long identity = Binder.clearCallingIdentity();
            try {
                mGameDndHelper.setGameDndPolicy(callingPkg, policy);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public GameDndPolicy getGameDndPolicy(String callingPkg) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_DND, TAG);
            long identity = Binder.clearCallingIdentity();
            try {
                return mGameDndHelper.getGameDndPolicy(callingPkg);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setCustomActivityDndLock(String callingPkg, boolean lock) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_DND, TAG);
            mGameDndHelper.setCustomActivityDndLock(callingPkg, lock);
        }

        public boolean getCustomActivityDndLock() {
            return mGameDndHelper.getCustomActivityDndLock();
        }

        public void setAlarmGameDndLock(String callingPkg, boolean lock) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_DND, TAG);
            mGameDndHelper.setAlarmGameDndLock(callingPkg, lock);
        }

        public boolean getAlarmGameDndLock() {
            return mGameDndHelper.getAlarmGameDndLock();
        }

        public void setInGameMode(String callingPkg, boolean inGame) {
            mContext.enforceCallingOrSelfPermission(PERM_MANAGE_GAME_DND, TAG);
            mGameDndHelper.setInGameMode(callingPkg, inGame);
        }

        public boolean isInGameMode() {
            return mGameDndHelper.isInGameMode();
        }

        public boolean blockForGameFirewall(int uid) {
            return mGameNetworkHelper.blockForGameFirewall(uid);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) {
                return;
            }
            if (mGameDndHelper != null) {
                pw.println("Game dnd state:");
                pw.println(" mIsInGameMode=" + mGameDndHelper.isInGameMode());
                pw.println(" mIsGameDndLock=" + mGameDndHelper.getGameDndLock());
                pw.println(" mIsCallGameDndLock=" + mGameDndHelper.getCallGameDndLock());
                pw.println(" mIsScreenRecorderDndLock=" + mGameDndHelper.getScreenRecorderDndLock());
                pw.println(" mIsCustomActivityDndLock=" + mGameDndHelper.getCustomActivityDndLock());
                pw.println(" mIsAlarmGameDndLock=" + mGameDndHelper.getAlarmGameDndLock());
                pw.println(" GameDndPolicy=" + mGameDndHelper.getGameDndPolicy("dump").toString());
                pw.println(" WhiteListTime=2000 ms");
                StringBuilder sb = new StringBuilder();
                sb.append(" mTopPackages=");
                for (String topPackage : mGameDndHelper.getTopPackages()) {
                    sb.append("\n   " + topPackage);
                }
                sb.append(" mOverlayWindowPackages=");
                for (String overlayWindowPackage : mGameDndHelper.getOverlayWindowPackages()) {
                    sb.append("\n   " + overlayWindowPackage);
                }
                pw.println(sb.toString());
                pw.println("\n  Game dnd history (max: " + GameLog.GAME_DND_SIZE + "):");
                GameLog.dumpGameDnd(pw, "    ");
            }
            if (mGameNetworkHelper != null) {
                pw.println("\n\nGame Network state:");
                StringBuilder sb2 = new StringBuilder();
                sb2.append(" mGameFirewallMode=");
                sb2.append(mGameNetworkHelper.getGameFirewallMode() ? "On" : "Off");
                pw.println(sb2.toString());
                pw.println(" uids (black list):");
                int[] uids = mGameNetworkHelper.getBlackList();
                StringBuilder sb3 = new StringBuilder();
                synchronized (mUidPackages) {
                    for (int uid : uids) {
                        List<String> packages = (List) mUidPackages.get(Integer.valueOf(uid));
                        int i = 0;
                        sb3.append(" uid=");
                        sb3.append(uid);
                        if (packages == null) {
                            sb3.append("\n");
                        } else {
                            for (String pkg : packages) {
                                if (i == 0) {
                                    sb3.append(" (");
                                }
                                sb3.append(pkg);
                                if (i == packages.size() - 1) {
                                    sb3.append(")");
                                }
                                int i2 = i + 1;
                                if (i != packages.size() - 1) {
                                    sb3.append(", ");
                                }
                                i = i2;
                            }
                            sb3.append("\n");
                        }
                    }
                    pw.println(sb3.toString());
                }
                pw.println("\n  Game network history (max: " + GameLog.GAME_NETWORK_SIZE + "):");
                GameLog.dumpGameNetwork(pw, "    ");
                pw.println("\n" + mGameNetworkHelper.dumpScreenOffFirewall());
                pw.println("\n  Screen Off Firewall history (max: " + GameLog.SCREEN_OFF_FIREWALL_SIZE + "):");
                GameLog.dumpScreenOffFirewall(pw, "    ");
            }
        }
    }
}
