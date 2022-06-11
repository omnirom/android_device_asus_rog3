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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import org.omnirom.rogservice.gamehw.GameDndPolicy;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class GameDndHelper {
    static final int SYSTEM_UI_POST_INCALL_NOTIFICATION = 3;
    static final int SYSTEM_UI_SPECIAL_OVERLAY = 2;
    static final String TAG = "GameModeHelper";
    static final long WHITELIST_RETENTION_TIME = 2000;
    static Intent sSrvIntent;
    private Context mContext;
    private ComponentName mDefaultPhoneApp;
    private GameDndPolicy mDefaultPolicy;
    private GameDndPolicy mDndPolicy;
    private GameDndHandler mHandler;
    private boolean mIsAlarmGameDndLock;
    private boolean mIsCallGameDndLock;
    private boolean mIsCustomActivityDndLock;
    private boolean mIsGameDndLock;
    private boolean mIsInGameMode;
    private boolean mIsScreenRecorderDndLock;
    private int mGameMode = GameDndPolicy.GAME_DND_NONE;
    private ArrayList<Callback> mCallbacks = new ArrayList<>();
    private Set<String> mTopPackages = new HashSet();
    private Set<String> mOverlayWindowPackages = new HashSet();
    private Map<String, Long> mActivityWhiteList = new HashMap();
    private static boolean ENABLE_ASUS_GAME_MODE = false;

    static {
        Intent intent = new Intent();
        sSrvIntent = intent;
        intent.setComponent(new ComponentName("com.android.systemui", "com.asus.systemui.mininotification.MinimalistNotificationService"));
    }

    public GameDndHelper(Context context, Looper looper, boolean useDefaultPolicy) {
        initGameDndHelper(context, looper, useDefaultPolicy);
    }

    private void initGameDndHelper(Context context, Looper looper, boolean useDefaultPolicy) {
        mContext = context;
        mHandler = new GameDndHandler(looper);
        mDefaultPolicy = new GameDndPolicy();
        mHandler.obtainMessage(413, useDefaultPolicy ? 1 : 0, 0).sendToTarget();
        mDndPolicy = new GameDndPolicy();
        Settings.Global.putInt(mContext.getContentResolver(), "in_game_mode", mIsInGameMode ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), "game_dnd_lock", mIsGameDndLock ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), "call_game_dnd_lock", mIsCallGameDndLock ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), "custom_activity_dnd_lock", mIsCustomActivityDndLock ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), "screen_recorder_dnd_lock", mIsScreenRecorderDndLock ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), "alarm_game_dnd_lock", mIsAlarmGameDndLock ? 1 : 0);
        if (mIsGameDndLock) {
            mGameMode |= GameDndPolicy.GAME_DND_NOTIFICATION;
        } else {
            mGameMode &= ~GameDndPolicy.GAME_DND_NOTIFICATION;
        }
        if (mIsCallGameDndLock) {
            mGameMode |= GameDndPolicy.GAME_DND_CALL;
        } else {
            mGameMode &= ~GameDndPolicy.GAME_DND_CALL;
        }
        if (mIsCustomActivityDndLock) {
            mGameMode |= GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY;
        } else {
            mGameMode &= ~GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY;
        }
    }

    public void setGameDndLock(String pkg, boolean lock) {
        if (ENABLE_ASUS_GAME_MODE) {
            mIsGameDndLock = lock;
            if (lock) {
                mGameMode |= GameDndPolicy.GAME_DND_NOTIFICATION;
            } else {
                mGameMode &= ~GameDndPolicy.GAME_DND_NOTIFICATION;
            }
            dispatchOnGameDndChanged(pkg, lock);
            return;
        }
        Slog.d(TAG, "not enable asus_game_mode, you can't set");
    }

    public boolean getGameDndLock() {
        return mIsGameDndLock;
    }

    public void setCallGameDndLock(String pkg, boolean lock) {
        if (ENABLE_ASUS_GAME_MODE) {
            mIsCallGameDndLock = lock;
            if (lock) {
                mGameMode |= GameDndPolicy.GAME_DND_CALL;
            } else {
                mGameMode &= ~GameDndPolicy.GAME_DND_CALL;
            }
            dispatchOnCallGameDndChanged(pkg, lock);
            return;
        }
        Slog.d(TAG, "not enable asus_game_mode, you can't set");
    }

    public boolean getCallGameDndLock() {
        return mIsCallGameDndLock;
    }

    public void setScreenRecorderDndLock(String pkg, boolean lock) {
        if (ENABLE_ASUS_GAME_MODE) {
            mIsScreenRecorderDndLock = lock;
            dispatchOnScreenRecorderDndChanged(pkg, lock);
            return;
        }
        Slog.d(TAG, "not enable asus_game_mode, you can't set");
    }

    public boolean getScreenRecorderDndLock() {
        return mIsScreenRecorderDndLock;
    }

    public void setCustomActivityDndLock(String pkg, boolean lock) {
        if (ENABLE_ASUS_GAME_MODE) {
            mIsCustomActivityDndLock = lock;
            if (lock) {
                mGameMode |= GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY;
            } else {
                mGameMode &= ~GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY;
            }
            dispatchOnCustomActivityDndChanged(pkg, lock);
            return;
        }
        Slog.d(TAG, "not enable asus_game_mode, you can't set");
    }

    public boolean getCustomActivityDndLock() {
        return mIsCustomActivityDndLock;
    }

    public void setAlarmGameDndLock(String callingPkg, boolean lock) {
        if (ENABLE_ASUS_GAME_MODE) {
            mIsAlarmGameDndLock = lock;
            dispatchOnAlarmGameDndChanged(callingPkg, lock);
            return;
        }
        Slog.d(TAG, "not enable asus_game_mode, you can't set");
    }

    public boolean getAlarmGameDndLock() {
        return mIsAlarmGameDndLock;
    }

    public void setInGameMode(String callingPkg, boolean inGame) {
        if (ENABLE_ASUS_GAME_MODE) {
            mIsInGameMode = inGame;
            dispatchOnInGameModeChanged(callingPkg, inGame);
            return;
        }
        Slog.d(TAG, "not enable asus_game_mode, you can't set");
    }

    public boolean isInGameMode() {
        return mIsInGameMode;
    }

    public boolean isCallNotification(String pkg, String ntag, String category, String channelId, String groupKey) {
        return isDefaultPhoneApp(pkg) || isCategoryCall(category) || isCallNotificationList(pkg, ntag, category, channelId, groupKey);
    }

    public void setActivityWhiteList(String pkg, String reason) {
        if (mHandler != null) {
            long now = System.currentTimeMillis();
            ActvityWhiteListRecord record = new ActvityWhiteListRecord(pkg, now, reason);
            Message msg = mHandler.obtainMessage(418, record);
            mHandler.sendMessage(msg);
        }
    }

    public boolean isActivityWhiteList(String pkg) {
        boolean z;
        long now = System.currentTimeMillis();
        synchronized (mActivityWhiteList) {
            z = false;
            if (mActivityWhiteList.get(pkg) != null && now - mActivityWhiteList.get(pkg).longValue() < WHITELIST_RETENTION_TIME) {
                z = true;
            }
        }
        return z;
    }

    public int getGameDndMode() {
        return mGameMode;
    }

    public boolean isToastFeatureEnabled() {
        return mDndPolicy.toast_enabled;
    }

    public boolean denyOverlayWindow(String pkg, int gameMode) {
        if (gameMode == GameDndPolicy.GAME_DND_NONE || gameMode == GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY) {
            return false;
        }
        Iterator it = mDndPolicy.toastWindowDnds.iterator();
        while (it.hasNext()) {
            GameDndPolicy.ToastWindowDnd toastWindowDnd = (GameDndPolicy.ToastWindowDnd) it.next();
            if (toastWindowDnd.pkg.equals(pkg)) {
                int blackListMode = GameDndPolicy.GAME_DND_ALL - (GameDndPolicy.GAME_DND_ALL & toastWindowDnd.gameMode);
                int focuseGameMode = GameDndPolicy.GAME_DND_ALL & gameMode;
                return (focuseGameMode & blackListMode) != GameDndPolicy.GAME_DND_NONE;
            }
        }
        return true;
    }

    public boolean isActivityFeatureEnabled() {
        return mDndPolicy.activity_enabled;
    }

    public boolean denyActivity(String component, int gameMode) {
        ComponentName cp = ComponentName.unflattenFromString(component);
        String pkg = cp != null ? cp.getPackageName() : "none";
        boolean isTop = isTopPackage(pkg);
        Iterator it = mDndPolicy.activityDnds.iterator();
        while (it.hasNext()) {
            GameDndPolicy.ActivityDnd activityDnd = (GameDndPolicy.ActivityDnd) it.next();
            if (activityDnd.component.equals(component) && (activityDnd.gameMode & gameMode) != 0) {
                if (isTop) {
                    GameLog.gameDndTopPackage(pkg, "deny_activity");
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public boolean isCustomActivityFeatureEnabled() {
        return mDndPolicy.custom_activity_enabled;
    }

    public boolean denyCustomActivity(String component, int gameMode) {
        ComponentName cp = ComponentName.unflattenFromString(component);
        if (cp != null) {
            cp.getPackageName();
        }
        if ((GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY & gameMode) == 0) {
            return false;
        }
        Iterator it = mDndPolicy.customActivityDnds.iterator();
        while (it.hasNext()) {
            GameDndPolicy.ActivityDndCustomView customActivityDnd = (GameDndPolicy.ActivityDndCustomView) it.next();
            if (customActivityDnd.component.equals(component)) {
                return true;
            }
        }
        return false;
    }

    public boolean showCustomView(String component, int gameMode) {
        ComponentName cp = ComponentName.unflattenFromString(component);
        if (cp != null) {
            cp.getPackageName();
        }
        if ((GameDndPolicy.GAME_DND_CUSTOM_ACTIVITY & gameMode) == 0) {
            return false;
        }
        Iterator it = mDndPolicy.customActivityDnds.iterator();
        while (it.hasNext()) {
            GameDndPolicy.ActivityDndCustomView customActivityDnd = (GameDndPolicy.ActivityDndCustomView) it.next();
            if (customActivityDnd.component.equals(component) && customActivityDnd.show) {
                return true;
            }
        }
        return false;
    }

    public boolean isSoundFeatureEnabled() {
        return mDndPolicy.sound_enabled;
    }

    public boolean denySound(String pkg, int usage, int gameMode) {
        Iterator it = mDndPolicy.soundDnds.iterator();
        while (it.hasNext()) {
            GameDndPolicy.SoundDnd soundDnd = (GameDndPolicy.SoundDnd) it.next();
            if (soundDnd.pkg.equals(pkg) && (soundDnd.usage == usage || usage == 21399)) {
                if ((soundDnd.gameMode & gameMode) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isVibrationFeatureEnabled() {
        return mDndPolicy.vibration_enabled;
    }

    public boolean denyVibration(String pkg, int usage, int gameMode) {
        Iterator it = mDndPolicy.vibrationDnds.iterator();
        while (it.hasNext()) {
            GameDndPolicy.VibrationDnd vibrationDnd = (GameDndPolicy.VibrationDnd) it.next();
            if (vibrationDnd.pkg.equals(pkg) && (vibrationDnd.usage == usage || usage == 21399)) {
                if ((vibrationDnd.gameMode & gameMode) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setTopPackage(String pkg, boolean add) {
        if (mHandler != null) {
            TopPackageRecord record = new TopPackageRecord(pkg, add);
            Message msg = mHandler.obtainMessage(417, record);
            mHandler.sendMessage(msg);
        }
    }

    public void startCustomActivity(Bundle bundle) {
        GameDndHandler gameDndHandler = mHandler;
        if (gameDndHandler != null && bundle != null) {
            Message msg = gameDndHandler.obtainMessage(414);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    public boolean isCallActivity(String component) {
        if (component == null) {
            return false;
        }
        Iterator it = mDndPolicy.activityDnds.iterator();
        while (it.hasNext()) {
            GameDndPolicy.ActivityDnd activityDnd = (GameDndPolicy.ActivityDnd) it.next();
            if (activityDnd.component.equals(component) && (activityDnd.gameMode & GameDndPolicy.GAME_DND_CALL) != 0) {
                return true;
            }
        }
        return false;
    }

    public void notifyScreenRecorder(Bundle bundle) {
        GameDndHandler gameDndHandler = mHandler;
        if (gameDndHandler != null && bundle != null) {
            Message msg = gameDndHandler.obtainMessage(415);
            msg.setData(bundle);
            if (!mHandler.hasMessages(415)) {
                mHandler.sendMessageAtFrontOfQueue(msg);
            }
        }
    }

    public void recordOverlayWindow(String pkg, long time, boolean add) {
        if (mHandler != null) {
            OverlayWindowRecord record = new OverlayWindowRecord(pkg, time, add);
            Message msg = mHandler.obtainMessage(416, record);
            mHandler.sendMessage(msg);
        }
    }

    public Set<String> getTopPackages() {
        Set<String> copyTopPackages;
        synchronized (mTopPackages) {
            copyTopPackages = new HashSet<>(mTopPackages);
        }
        return copyTopPackages;
    }

    public Set<String> getOverlayWindowPackages() {
        Set<String> copyOverlayWindowPackages;
        synchronized (mOverlayWindowPackages) {
            copyOverlayWindowPackages = new HashSet<>(mOverlayWindowPackages);
        }
        return copyOverlayWindowPackages;
    }

    public void setGameDndPolicy(String callingPkg, GameDndPolicy policy) {
        GameDndPolicy gameDndPolicy;
        if (policy == null || (gameDndPolicy = mDndPolicy) == null) {
            return;
        }
        synchronized (gameDndPolicy) {
            setPolicyLocked(callingPkg, policy);
        }
    }

    public GameDndPolicy getGameDndPolicy(String callingPkg) {
        GameDndPolicy copy;
        synchronized (mDndPolicy) {
            copy = mDndPolicy.copy();
        }
        return copy;
    }

    public void readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        GameDndPolicy defaultPolicy;
        GameDndPolicy policy = GameDndPolicy.readXml(parser);
        synchronized (mDefaultPolicy) {
            defaultPolicy = mDefaultPolicy.copy();
        }
        synchronized (mDndPolicy) {
            if (defaultPolicy != null) {
                try {
                    if (defaultPolicy.version > policy.version) {
                        setPolicyLocked("readDefaultXmlByVersion", defaultPolicy);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (policy != null) {
                setPolicyLocked("readXml", policy);
            }
        }
    }

    public void writeXml(XmlSerializer out, Integer version) throws IOException {
        mDndPolicy.writeXml(out, version);
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    private boolean setPolicyLocked(String reason, GameDndPolicy policy) {
        Binder.clearCallingIdentity();
        if (policy == null) {
            return false;
        }
        boolean policyChanged = !Objects.equals(mDndPolicy, policy);
        if (policyChanged) {
            dispatchOnGameDndPolicyChanged(reason, policy);
        }
        mDndPolicy = policy;
        return true;
    }

    private boolean isDefaultPhoneApp(String pkg) {
        ComponentName componentName;
        Context context;
        if (mDefaultPhoneApp == null && (context = mContext) != null) {
            TelecomManager telecomm = (TelecomManager) context.getSystemService("telecom");
            mDefaultPhoneApp = telecomm != null ? telecomm.getDefaultPhoneApp() : null;
        }
        return (pkg == null || (componentName = mDefaultPhoneApp) == null || !pkg.equals(componentName.getPackageName())) ? false : true;
    }

    private boolean isCategoryCall(String category) {
        return "call".equals(category);
    }

    private boolean isCallNotificationList(String pkg, String ntag, String category, String channelId, String groupKey) {
        String str = pkg;
        String str2 = ntag;
        if (str != null && channelId != null) {
            boolean isNtagNull = str2 == null || "NULL".equals(str2);
            boolean isGroupKeyNull = groupKey == null || "NULL".equals(groupKey);
            Iterator it = mDndPolicy.notificationListDnds.iterator();
            while (it.hasNext()) {
                GameDndPolicy.NotificationListDnd notificationListDnd = (GameDndPolicy.NotificationListDnd) it.next();
                if (notificationListDnd.pkg.equals(str) && (notificationListDnd.gameMode & GameDndPolicy.GAME_DND_CALL) != 0 && (((!isNtagNull && "NOT_NULL".equals(notificationListDnd.ntag)) || (isNtagNull && "NULL".equals(notificationListDnd.ntag))) && (((category != null && category.equals(notificationListDnd.category)) || (category == null && "NULL".equals(notificationListDnd.category))) && ((!isGroupKeyNull && "NOT_NULL".equals(notificationListDnd.groupKey)) || (isGroupKeyNull && "NULL".equals(notificationListDnd.groupKey)))))) {
                    String ntag_channelId_groupKey = str2 + "_" + channelId + "_" + groupKey;
                    String ntag_channelId_groupKey_tolowercase = ntag_channelId_groupKey.toLowerCase();
                    String keywords = notificationListDnd.keywords;
                    if (keywords != null) {
                        String[] tokens = keywords.split(",");
                        for (String token : tokens) {
                            String keyword = token.trim().toLowerCase();
                            if (ntag_channelId_groupKey_tolowercase.contains(keyword)) {
                                return true;
                            }
                        }
                        continue;
                    } else {
                        continue;
                    }
                }
                str = pkg;
                str2 = ntag;
            }
            return false;
        }
        return false;
    }

    private void dispatchOnGameDndChanged(String from, boolean lock) {
        Iterator<Callback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            Callback callback = it.next();
            callback.onGameDndChanged(from, lock);
        }
    }

    private void dispatchOnCallGameDndChanged(String from, boolean lock) {
        Iterator<Callback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            Callback callback = it.next();
            callback.onCallGameDndChanged(from, lock);
        }
    }

    private void dispatchOnScreenRecorderDndChanged(String from, boolean lock) {
        Iterator<Callback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            Callback callback = it.next();
            callback.onScreenRecorderDndChanged(from, lock);
        }
    }

    private void dispatchOnCustomActivityDndChanged(String from, boolean lock) {
        Iterator<Callback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            Callback callback = it.next();
            callback.onCustomActivityDndChanged(from, lock);
        }
    }

    private void dispatchOnGameDndPolicyChanged(String reason, GameDndPolicy policy) {
        Iterator<Callback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            Callback callback = it.next();
            callback.onGameDndPolicyChanged(reason, policy);
        }
    }

    private void dispatchOnAlarmGameDndChanged(String from, boolean lock) {
        Iterator<Callback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            Callback callback = it.next();
            callback.onAlarmGameDndChanged(from, lock);
        }
    }

    private void dispatchOnInGameModeChanged(String from, boolean inGame) {
        Iterator<Callback> it = mCallbacks.iterator();
        while (it.hasNext()) {
            Callback callback = it.next();
            callback.onInGameModeChanged(from, inGame);
        }
    }

    private boolean isTopPackage(String pkg) {
        if (pkg == null) {
            return false;
        }
        try {
            Set<String> copyTopPackages = new HashSet<>(mTopPackages);
            return copyTopPackages.contains(pkg);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get isTopPackage", e);
            return false;
        }
    }

    private boolean isOverlayWindowPackage(String pkg) {
        if (pkg == null) {
            return false;
        }
        try {
            Set<String> copyOverlayWindowPackages = new HashSet<>(mOverlayWindowPackages);
            return copyOverlayWindowPackages.contains(pkg);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get isOverlayWindowPackage", e);
            return false;
        }
    }

    private void readDefaultDndPolicy(boolean useDefaultPolicy) {
        AtomicFile policyFile = new AtomicFile(new File("/system/etc/gamednd/game_dnd_policy.xml"));
        GameDndPolicy defaultPolicy = null;
        if (policyFile.getBaseFile() != null && policyFile.getBaseFile().exists() && policyFile.getBaseFile().canRead()) {
            InputStream infile = null;
            try {
                try {
                    infile = policyFile.openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(infile, StandardCharsets.UTF_8.name());
                    int outerDepth = parser.getDepth();
                    while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                        if ("game-dnd-policy".equals(parser.getName())) {
                            defaultPolicy = GameDndPolicy.readXml(parser);
                        }
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Unable to read default game dnd policy", e);
                }
            } finally {
                IoUtils.closeQuietly(infile);
            }
        }
        synchronized (mDefaultPolicy) {
            if (defaultPolicy != null) {
                mDefaultPolicy = defaultPolicy;
            }
        }
        synchronized (mDndPolicy) {
            if (defaultPolicy != null && useDefaultPolicy) {
                setPolicyLocked("useDefaultXml", defaultPolicy);
            }
        }
    }

    private void notifyScreenRecorderInner(Message msg) {
        Bundle bundle = msg.getData();
        Intent rIntent = new Intent();
        rIntent.setAction("asus.intent.action.NOTIFY_CALL_TO_SCREEN_RECORDER");
        rIntent.putExtras(bundle);
        rIntent.addFlags(268435456);
        mContext.sendBroadcastAsUser(rIntent, UserHandle.ALL, "com.asus.permission.MANAGE_SCREEN_RECORDER_DND");
        Slog.i(TAG, "notify to screen recorder sendbroadcast");
    }

    private void notifyCustomActivityInner(Message msg) {
        final Bundle bundle = msg.getData();
        ServiceConnection srvCAConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Messenger messenger = new Messenger(service);
                Message msg2 = Message.obtain(null, 3, bundle);
                try {
                    try {
                        messenger.send(msg2);
                        if (mContext == null) {
                            return;
                        }
                    } catch (Exception e) {
                        Slog.w(GameDndHelper.TAG, "Send to System UI failed(notifyCustomActivity), err: " + e.getMessage());
                        if (mContext == null) {
                            return;
                        }
                    }
                    mContext.unbindService(this);
                } catch (Throwable th) {
                    if (mContext != null) {
                        mContext.unbindService(this);
                    }
                    throw th;
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        try {
            mContext.bindServiceAsUser(sSrvIntent, srvCAConnection, 1, mHandler, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.w(TAG, "Bind System UI failed(notifyCustomActivity), err: " + e.getMessage());
        }
    }

    private void setTopPackageInner(String pkg, boolean add) {
        synchronized (mTopPackages) {
            if (mTopPackages.contains(pkg) == add) {
                StringBuilder sb = new StringBuilder();
                sb.append("package ");
                sb.append(pkg);
                sb.append(": already ");
                sb.append(add ? "add" : "remove");
                Slog.w(TAG, sb.toString());
                return;
            }
            if (add) {
                mTopPackages.add(pkg);
            } else {
                mTopPackages.remove(pkg);
            }
        }
    }

    private void setActivityWhiteListInner(String pkg, long time, String reason) {
        synchronized (mActivityWhiteList) {
            if (mActivityWhiteList.entrySet() != null && !mActivityWhiteList.isEmpty()) {
                Iterator<Map.Entry<String, Long>> iterator = mActivityWhiteList.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Long> entry = iterator.next();
                    if (time - entry.getValue().longValue() > WHITELIST_RETENTION_TIME) {
                        iterator.remove();
                    }
                }
            }
            mActivityWhiteList.put(pkg, Long.valueOf(time));
        }
        GameLog.gameDndActivityWhiteList(pkg, time, reason);
    }

    private final class OverlayWindowRecord {
        public final boolean add = false;
        public final String pkg = mContext.getPackageName();
        public final long time = System.currentTimeMillis();

        OverlayWindowRecord(String pkg, long time, boolean add) {
            pkg = pkg;
            time = time;
            add = add;
        }
    }

    private final class TopPackageRecord {
        public final boolean add = false;
        public final String pkg = mContext.getPackageName();

        TopPackageRecord(String pkg, boolean add) {
            pkg = pkg;
            add = add;
        }
    }

    private final class ActvityWhiteListRecord {
        public final String pkg = mContext.getPackageName();
        public final String reason = "";
        public final long time = System.currentTimeMillis();

        ActvityWhiteListRecord(String pkg, long time, String reason) {
            pkg = pkg;
            time = time;
            reason = reason;
        }
    }

    private class GameDndHandler extends Handler {
        static final int MSG_NOTIFY_CALL_TO_SCREEN_RECORDER = 415;
        static final int MSG_READ_DEFAULT_DND_POLICY = 413;
        static final int MSG_RECORD_OVERLAY_WINDOW = 416;
        static final int MSG_SET_ACTIVITY_WHITE_LIST = 418;
        static final int MSG_SET_TOP_PACKAGE = 417;
        static final int MSG_START_CUSTOM_ACTIVITY = 414;

        GameDndHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_READ_DEFAULT_DND_POLICY /* 413 */:
                    boolean z = true;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    boolean useDefaultPolicy = z;
                    readDefaultDndPolicy(useDefaultPolicy);
                    return;
                case MSG_START_CUSTOM_ACTIVITY:
                    notifyCustomActivityInner(msg);
                    return;
                case MSG_NOTIFY_CALL_TO_SCREEN_RECORDER:
                    notifyScreenRecorderInner(msg);
                    return;
                case MSG_RECORD_OVERLAY_WINDOW:
                    return;
                case MSG_SET_TOP_PACKAGE:
                    TopPackageRecord record = (TopPackageRecord) msg.obj;
                    String pkg = record.pkg;
                    boolean add = record.add;
                    setTopPackageInner(pkg, add);
                    return;
                case MSG_SET_ACTIVITY_WHITE_LIST:
                    ActvityWhiteListRecord record2 = (ActvityWhiteListRecord) msg.obj;
                    String pkg2 = record2.pkg;
                    long time = record2.time;
                    String reason = record2.reason;
                    setActivityWhiteListInner(pkg2, time, reason);
                    return;
                default:
                    return;
            }
        }
    }

    public static class Callback {
        void onGameDndChanged(String from, boolean lock) {
        }

        void onCallGameDndChanged(String from, boolean lock) {
        }

        void onScreenRecorderDndChanged(String from, boolean lock) {
        }

        void onGameDndPolicyChanged(String reason, GameDndPolicy policy) {
        }

        void onCustomActivityDndChanged(String from, boolean lock) {
        }

        void onAlarmGameDndChanged(String from, boolean lock) {
        }

        void onInGameModeChanged(String form, boolean inGame) {
        }
    }
}
