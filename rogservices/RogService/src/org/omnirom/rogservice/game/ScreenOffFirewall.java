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
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.LocalServices;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScreenOffFirewall {
    private static final String ACTION_POWER_SAVER_MODE_CHANGED = "com.asus.powersaver.action.power_saver_mode";
    private static final String ASUS_FREEZER_XML_PARSING_DONE = "asus_freezer_xml_parsing_done";
    private static final String ACTION_SCHEDULE_CHECK_SCREEN_OFF = ScreenOffFirewall.class.getSimpleName() + ".CHECK";
    private static final String ASUS_FREEZE_ENABLE = "asus_freezer_enabled";
    private static final int DEFAULT_CHECK_DYNAMIC_UID_DURATION = 600;
    private static final int DEFAULT_DOWNLOAD_CHECK_DURATION = 60;
    private static final int DEFAULT_DOWNLOAD_TIMEOUT = 5;
    private static final int DEFAULT_SCREEN_OFF_DELAY = 60;
    private static final int DEFAULT_SCREEN_OFF_ENABLE = 1;
    private static final int DEFAULT_SCREEN_OFF_INTERMISSION_PHASE_OFF = 600;
    private static final int DEFAULT_SCREEN_OFF_INTERMISSION_PHASE_ON = 7200;
    private static final String EXTRA_POWER_SAVER_MODE = "com.asus.powersaver.key.power_saver_mode";
    private static final int INTERMISSION_STATE_OFF = 0;
    private static final int INTERMISSION_STATE_PHASE_OFF = 2;
    private static final int INTERMISSION_STATE_PHASE_ON = 1;
    private static final int MODE_EXTREME_DURABLE = 11;
    private static final int MODE_UNSPECIFIED = -1;
    private static final int REQUEST_CODE_CHECK_SCREEN_OFF_STATE = 1;
    private static final String SCREEN_OFF_FIREWALL_DEBUG = "screen_off_firewall_debug";
    private static final String SCREEN_OFF_FIREWALL_DELAY = "screen_off_firewall_delay";
    private static final String SCREEN_OFF_FIREWALL_DURATION_CHECK_UID = "screen_off_firewall_duration_check_uid";
    private static final String SCREEN_OFF_FIREWALL_ENABLE = "screen_off_firewall_enabled";
    private static final String SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_OFF = "screen_off_firewall_intermission_duration_phase_off";
    private static final String SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_ON = "screen_off_firewall_intermission_duration_phase_on";
    private static final String SCREEN_OFF_FIREWALL_INTERMISSION_ENABLE = "screen_off_firewall_intermission_enable";
    private static final String SCREEN_OFF_FIREWALL_TURN_ON = "screen_off_firewall_turn_on";
    private static final String TAG = "ScreenOffFirewall";
    private ActivityManagerInternal mActivityManagerInternal;
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private IAsusFreezerService mAsusFreezerService;
    private final Context mContext;
    private IDeviceIdleController mDeviceIdleController;
    private boolean mEnableFirewall;
    private final ScreenOffHandler mHandler;
    private boolean mIsDeviceIdleControllerReady;
    private boolean mIsUltraSavingMode;
    private final PackageManager mPackageManager;
    private boolean mScreenOff;
    private int mScreenOffCheckDynamicDuration;
    private boolean mScreenOffDebug;
    private int mScreenOffDelay;
    private boolean mScreenOffIntermissionEnable;
    private long mScreenOffTime;
    private boolean mSettingAsusFreeze;
    private boolean mSettingEnableFirewall;
    private boolean mSettingScreenOffEnable;
    private SettingsObserver mSettingsObserver;
    private ScreenStateMonitor mScreenStateMonitor = new ScreenStateMonitor();
    private PackageMonitor mPackageMonitor = new PackageMonitor();
    private AlarmReceiver mAlarmReceiver = new AlarmReceiver();
    private PowerSaverReceiver mPowerSaverReceiver = new PowerSaverReceiver();
    private long mAsusFreezerXmlParse = 0;
    private int mScreenOffIntermissionDurationTurnOn = DEFAULT_SCREEN_OFF_INTERMISSION_PHASE_ON;
    private int mScreenOffIntermissionDurationTurnOff = 600;
    private Set<Integer> mScreenOffAppOpsUids = new HashSet();
    private Set<Integer> mScreenOffNetdUids = new HashSet();
    private Set<Integer> mScreenOffCDNWhiteUids = new HashSet();
    private Set<Integer> mScreenOffCDNBlackUids = new HashSet();
    private Set<Integer> mScreenOffSystemWhiteUids = new HashSet();
    private Set<Integer> mScreenOffDynamicWhiteUids = new HashSet();
    private Set<Integer> mDownloadUids = new HashSet();
    private Map<Integer, List<String>> mUidPackages = new HashMap();
    private List<String> mCdnWhitePackages = new ArrayList();
    private List<String> mCdnBlackPackages = new ArrayList();

    public ScreenOffFirewall(Context context, Looper looper) {
        String[] packageNames;
        mScreenOffTime = 0L;
        mContext = context;
        mHandler = new ScreenOffHandler(looper);
        PackageManager packageManager = context.getPackageManager();
        mPackageManager = packageManager;
        mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        mSettingsObserver = new SettingsObserver(context);
        mAppOps = (AppOpsManager) context.getSystemService("appops");
        List<ApplicationInfo> installedPkgs = packageManager.getInstalledApplications(0);
        for (ApplicationInfo appInfo : installedPkgs) {
            if (appInfo != null) {
                int uid = appInfo.uid;
                String packageName = appInfo.packageName;
                int mode = mAppOps.unsafeCheckOpNoThrow(mAppOps.OPSTR_MANAGE_MEDIA, uid, packageName);
                synchronized (mUidPackages) {
                    if (!mScreenOffSystemWhiteUids.contains(Integer.valueOf(uid)) && (appInfo.isSystemApp() || isSystem(uid, packageName))) {
                        mScreenOffSystemWhiteUids.add(Integer.valueOf(uid));
                    }
                    if (mode == 0) {
                        mScreenOffAppOpsUids.add(Integer.valueOf(uid));
                    } else {
                        mScreenOffAppOpsUids.remove(Integer.valueOf(uid));
                    }
                    if (mUidPackages.get(Integer.valueOf(uid)) == null && (packageNames = mPackageManager.getPackagesForUid(uid)) != null && packageNames.length > 0) {
                        List<String> packages = Arrays.asList(packageNames);
                        mUidPackages.put(Integer.valueOf(uid), packages);
                    }
                }
            }
        }
        mHandler.removeMessages(2);
        mHandler.obtainMessage(2).sendToTarget();
        mAppOps.startWatchingMode(mAppOps.OPSTR_MANAGE_MEDIA, (String) null, new AppOpsManager.OnOpChangedInternalListener() {
            public void onOpChanged(int op, String packageName2) {
                try {
                    int userId = ActivityManager.getCurrentUser();
                    int uid2 = mPackageManager.getPackageUidAsUser(packageName2, userId);
                    int mode2 = mAppOps.unsafeCheckOpNoThrow(mAppOps.OPSTR_MANAGE_MEDIA, uid2, packageName2);
                    synchronized (mUidPackages) {
                        if (mode2 == 0) {
                            mScreenOffAppOpsUids.add(Integer.valueOf(uid2));
                        } else {
                            mScreenOffAppOpsUids.remove(Integer.valueOf(uid2));
                        }
                        if (!mScreenOffSystemWhiteUids.contains(Integer.valueOf(uid2)) && isSystem(uid2, packageName2)) {
                            mScreenOffSystemWhiteUids.add(Integer.valueOf(uid2));
                        }
                    }
                    mHandler.removeMessages(2);
                    mHandler.obtainMessage(2).sendToTarget();
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        });
        PowerManager pm = (PowerManager) context.getSystemService("power");
        if (pm != null) {
            boolean z = !pm.isInteractive();
            mScreenOff = z;
            if (z) {
                mScreenOffTime = System.currentTimeMillis();
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(mScreenStateMonitor, filter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.PACKAGE_ADDED");
        filter2.addAction("android.intent.action.PACKAGE_REPLACED");
        filter2.addDataScheme("package");
        context.registerReceiver(mPackageMonitor, filter2);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction(ACTION_SCHEDULE_CHECK_SCREEN_OFF);
        context.registerReceiver(mAlarmReceiver, filter3);
        IntentFilter filter4 = new IntentFilter();
        filter4.addAction(ACTION_POWER_SAVER_MODE_CHANGED);
        context.registerReceiver(mPowerSaverReceiver, filter4);
    }

    public void onSystemServicesReady() {
        mHandler.postDelayed(new Runnable() {
            @Override            public void run() {
                mIsDeviceIdleControllerReady = true;
            }
        }, 15000L);
    }

    public String dump() {
        List<Integer> appOpslist;
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nScreen Off Firewall:");
        sb.append("\n mScreenOff=" + mScreenOff);
        sb.append("\n mScreenOffDelay=" + mScreenOffDelay);
        if (mScreenOff) {
            sb.append("\n now already screen off=" + ((System.currentTimeMillis() - mScreenOffTime) / 1000.0d));
        }
        sb.append("\n mSettingAsusFreeze=" + mSettingAsusFreeze);
        sb.append("\n mIsUltraSavingMode=" + mIsUltraSavingMode);
        sb.append("\n mSettingScreenOffEnable=" + mSettingScreenOffEnable);
        sb.append("\n mSettingEnableFirewall=" + mSettingEnableFirewall);
        sb.append("\n mScreenOffIntermissionEnable=" + mScreenOffIntermissionEnable);
        if (mScreenOffIntermissionEnable) {
            sb.append("\n mScreenOffIntermissionDurationTurnOn=" + mScreenOffIntermissionDurationTurnOn);
            sb.append("\n mScreenOffIntermissionDurationTurnOff=" + mScreenOffIntermissionDurationTurnOff);
            sb.append("\n mScreenOffIntermissionState=" + getIntermissionState());
        }
        sb.append("\n mAsusFreezerXmlParse=" + mAsusFreezerXmlParse);
        sb.append("\n mScreenOffCheckDynamicDuration=" + mScreenOffCheckDynamicDuration);
        sb.append("\n mEnableFirewall=" + mEnableFirewall);
        synchronized (mUidPackages) {
            List<Integer> appOpslist2 = new ArrayList<>(mScreenOffAppOpsUids);
            Collections.sort(appOpslist2);
            sb.append("\n mScreenOffAppOpsUids=" + appOpslist2);
            List<Integer> netdlist = new ArrayList<>(mScreenOffNetdUids);
            Collections.sort(netdlist);
            sb.append("\n mScreenOffNetdUids=" + netdlist);
            List<Integer> cdnWhitelist = new ArrayList<>(mScreenOffCDNWhiteUids);
            Collections.sort(cdnWhitelist);
            sb.append("\n mScreenOffCDNWhiteUids=" + cdnWhitelist);
            List<Integer> cdnBlacklist = new ArrayList<>(mScreenOffCDNBlackUids);
            Collections.sort(cdnBlacklist);
            sb.append("\n mScreenOffCDNBlackUids=" + cdnBlacklist);
            List<Integer> systemlist = new ArrayList<>(mScreenOffSystemWhiteUids);
            Collections.sort(systemlist);
            sb.append("\n mScreenOffSystemWhiteUids=" + systemlist);
            List<Integer> whitelist = new ArrayList<>(mScreenOffDynamicWhiteUids);
            Collections.sort(whitelist);
            sb.append("\n DEFAULT_DOWNLOAD_TIMEOUT=5");
            sb.append("\n DEFAULT_DOWNLOAD_CHECK_DURATION=60");
            sb.append("\n mDownloadUids=" + mDownloadUids);
            sb.append("\n mScreenOffDynamicWhiteUids=" + whitelist);
            if (mScreenOffDebug) {
                sb.append("\n mUidInfo=");
                List<Integer> uidInfo = new ArrayList<>();
                for (Integer num : mUidPackages.keySet()) {
                    uidInfo.add(Integer.valueOf(num.intValue()));
                }
                Collections.sort(uidInfo);
                for (Integer num2 : uidInfo) {
                    int uid = num2.intValue();
                    List<String> packages = mUidPackages.get(Integer.valueOf(uid));
                    if (packages != null) {
                        int i = 0;
                        sb.append("\n uid=" + uid);
                        for (String pkg : packages) {
                            if (i != 0) {
                                appOpslist = appOpslist2;
                            } else {
                                appOpslist = appOpslist2;
                                sb.append(" (");
                            }
                            sb.append(pkg);
                            if (i == packages.size() - 1) {
                                sb.append(")");
                            }
                            int i2 = i + 1;
                            int i3 = packages.size() - 1;
                            if (i != i3) {
                                sb.append(", ");
                            }
                            appOpslist2 = appOpslist;
                            i = i2;
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    private class ScreenStateMonitor extends BroadcastReceiver {
        private ScreenStateMonitor() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mScreenOff = "android.intent.action.SCREEN_OFF".equals(action);
            if (mScreenOff) {
                mScreenOffTime = System.currentTimeMillis();
                mHandler.removeMessages(4);
                int prepareCheck = mScreenOffDelay >= 5 ? mScreenOffDelay - 5 : 0;
                mHandler.sendMessageDelayed(mHandler.obtainMessage(4), prepareCheck * 1000);
            } else {
                mHandler.removeMessages(4);
                mHandler.removeMessages(5);
            }
            checkScreenOffState();
        }
    }

    private class PackageMonitor extends BroadcastReceiver {
        private PackageMonitor() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REPLACED".equals(action)) && intent.getData() != null) {
                String packageName = intent.getData().getEncodedSchemeSpecificPart();
                int uid = intent.getIntExtra("android.intent.extra.UID", -10000);
                if (uid != -10000) {
                    int mode = mAppOps.unsafeCheckOpNoThrow(mAppOps.OPSTR_MANAGE_MEDIA, uid, packageName);
                    synchronized (mUidPackages) {
                        if (mode == 0) {
                            mScreenOffAppOpsUids.add(Integer.valueOf(uid));
                        } else {
                            mScreenOffAppOpsUids.remove(Integer.valueOf(uid));
                        }
                        if (mCdnWhitePackages.contains(packageName)) {
                            mScreenOffCDNWhiteUids.add(Integer.valueOf(uid));
                        }
                        if (mCdnBlackPackages.contains(packageName)) {
                            mScreenOffCDNBlackUids.add(Integer.valueOf(uid));
                        }
                        String[] packageNames = mPackageManager.getPackagesForUid(uid);
                        if (packageNames != null && packageNames.length > 0) {
                            List<String> packages = Arrays.asList(packageNames);
                            mUidPackages.put(Integer.valueOf(uid), packages);
                        }
                    }
                    mHandler.removeMessages(2);
                    mHandler.obtainMessage(2).sendToTarget();
                }
            }
        }
    }

    private class AlarmReceiver extends BroadcastReceiver {
        private AlarmReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && ScreenOffFirewall.ACTION_SCHEDULE_CHECK_SCREEN_OFF.equals(action)) {
                checkScreenOffState();
            }
        }
    }

    private class PowerSaverReceiver extends BroadcastReceiver {
        private PowerSaverReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int powerMode = intent.getIntExtra(ScreenOffFirewall.EXTRA_POWER_SAVER_MODE, -1);
            boolean ultraSavingMode = powerMode == 11;
            if (ultraSavingMode != mIsUltraSavingMode) {
                mIsUltraSavingMode = ultraSavingMode;
                checkScreenOffState();
            }
        }
    }

    private String getIntermissionState() {
        int inState = checkIntermissionState();
        switch (inState) {
            case 0:
                return "INTERMISSION_STATE_OFF";
            case 1:
                return "INTERMISSION_STATE_PHASE_ON";
            case 2:
                return "INTERMISSION_STATE_PHASE_OFF";
            default:
                return "NONE";
        }
    }

    private void scheduleCheckScreenOffState(long checkTime) {
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 1, new Intent(ACTION_SCHEDULE_CHECK_SCREEN_OFF).addFlags(268435456), 201326592);
        mAlarmManager.cancel(pi);
        mAlarmManager.setExactAndAllowWhileIdle(2, checkTime, pi);
    }

    private int checkIntermissionState() {
        if (mScreenOff && mSettingEnableFirewall && mScreenOffIntermissionEnable) {
            long now = System.currentTimeMillis();
            long j = mScreenOffTime;
            int i = mScreenOffDelay;
            if (now - j <= i * 1000) {
                return 0;
            }
            int i2 = mScreenOffIntermissionDurationTurnOn;
            int totalOneCycle = (i2 * 1000) + (mScreenOffIntermissionDurationTurnOff * 1000);
            if (((now - j) - (i * 1000)) % totalOneCycle <= i2 * 1000) {
                return 1;
            }
            return 2;
        }
        return 0;
    }

    private void checkDebugMode() {
        boolean z = false;
        if (Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_DEBUG, 0) == 1) {
            z = true;
        }
        mScreenOffDebug = z;
    }

    private void checkScreenOffDurationUid() {
        mScreenOffCheckDynamicDuration = Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_DURATION_CHECK_UID, 600);
        mHandler.removeMessages(3);
        mHandler.obtainMessage(3).sendToTarget();
    }

    private void checkScreenOffState() {
        Settings.System.putInt(mContext.getContentResolver(), ASUS_FREEZE_ENABLE, 1);
        mSettingAsusFreeze = Settings.System.getInt(mContext.getContentResolver(), ASUS_FREEZE_ENABLE, 1) == 1;
        boolean z = Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_ENABLE, 1) == 1;
        mSettingScreenOffEnable = z;
        mSettingEnableFirewall = mSettingAsusFreeze && mIsUltraSavingMode && z;
        mScreenOffDelay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_DELAY, 60);
        mScreenOffIntermissionEnable = Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_ENABLE, 0) == 1;
        mScreenOffIntermissionDurationTurnOn = Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_ON, DEFAULT_SCREEN_OFF_INTERMISSION_PHASE_ON);
        mScreenOffIntermissionDurationTurnOff = Settings.System.getInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_OFF, 600);
        int inState = checkIntermissionState();
        long now = System.currentTimeMillis();
        boolean expiration = now - mScreenOffTime >= ((long) (mScreenOffDelay * 1000));
        boolean z2 = mScreenOffIntermissionEnable;
        boolean enableFirewall = (!z2 && mScreenOff && mSettingEnableFirewall && expiration) || (z2 && inState == 1);
        if (!z2) {
            if (!expiration && mSettingEnableFirewall && mScreenOff) {
                long checkTime = SystemClock.elapsedRealtime() + Math.abs((mScreenOffDelay * 1000) - (System.currentTimeMillis() - mScreenOffTime));
                scheduleCheckScreenOffState(checkTime);
            } else if (mEnableFirewall != enableFirewall) {
                if (enableFirewall) {
                    mHandler.removeMessages(3);
                    mHandler.obtainMessage(3).sendToTarget();
                } else {
                    mHandler.removeMessages(3);
                }
                mHandler.obtainMessage(1, enableFirewall ? 1 : 0, 0).sendToTarget();
            }
        } else if (mSettingEnableFirewall && mScreenOff && inState != 1 && inState != 2) {
            long checkTime2 = SystemClock.elapsedRealtime() + Math.abs((mScreenOffDelay * 1000) - (System.currentTimeMillis() - mScreenOffTime));
            scheduleCheckScreenOffState(checkTime2);
        } else {
            if (mEnableFirewall != enableFirewall) {
                if (enableFirewall) {
                    mHandler.removeMessages(3);
                    mHandler.obtainMessage(3).sendToTarget();
                } else {
                    mHandler.removeMessages(3);
                }
                mHandler.obtainMessage(1, enableFirewall ? 1 : 0, 0).sendToTarget();
            }
            int totalOneCycle = (mScreenOffIntermissionDurationTurnOn * 1000) + (mScreenOffIntermissionDurationTurnOff * 1000);
            if (inState == 1 || inState == 2) {
                long checkTime3 = SystemClock.elapsedRealtime() + Math.abs((inState == 1 ? mScreenOffIntermissionDurationTurnOn * 1000 : totalOneCycle) - (((now - mScreenOffTime) - (mScreenOffDelay * 1000)) % totalOneCycle));
                scheduleCheckScreenOffState(checkTime3);
            }
        }
        mEnableFirewall = enableFirewall;
    }

    private void checkScreenOffUids() {
        synchronized (mUidPackages) {
            Set<Integer> adds = new HashSet<>(mScreenOffAppOpsUids);
            Set<Integer> dels = new HashSet<>(mScreenOffNetdUids);
            Set<Integer> whites = new HashSet<>(mScreenOffSystemWhiteUids);
            whites.addAll(mScreenOffDynamicWhiteUids);
            whites.addAll(mScreenOffCDNWhiteUids);
            whites.addAll(mDownloadUids);
            Set<Integer> blacks = new HashSet<>(mScreenOffCDNBlackUids);
            adds.removeAll(mScreenOffNetdUids);
            adds.removeAll(whites);
            dels.removeAll(mScreenOffAppOpsUids);
            dels.removeAll(blacks);
            if (adds.size() != 0 || dels.size() != 0) {
                GameLog.screenOffUidChange(adds.toString(), dels.toString());
            }
            whites.retainAll(mScreenOffAppOpsUids);
            dels.addAll(whites);
            adds.addAll(blacks);
            for (Integer num : adds) {
                int addUid = num.intValue();
                setScreenOffUidLocked(addUid, true);
            }
            for (Integer num2 : dels) {
                int delUid = num2.intValue();
                setScreenOffUidLocked(delUid, false);
            }
        }
    }

    private void prepareDownloadCheck() {
        if (mAsusFreezerService == null) {
            mAsusFreezerService = IAsusFreezerService.Stub.getDefaultImpl();
        }
        IAsusFreezerService iAsusFreezerService = mAsusFreezerService;
        if (iAsusFreezerService != null) {
            try {
                iAsusFreezerService.prepareDownloadCheck(5L);
            } catch (RemoteException e) {
            }
            GameLog.screenOffDynamicUidChange("download", "prepare");
            mHandler.removeMessages(5);
            ScreenOffHandler screenOffHandler = mHandler;
            screenOffHandler.sendMessageDelayed(screenOffHandler.obtainMessage(5), 5000L);
        }
    }

    private void getDownloadBehavior() {
        if (mAsusFreezerService == null) {
            mAsusFreezerService = IAsusFreezerService.Stub.getDefaultImpl();
        }
        if (mAsusFreezerService != null) {
            List<ApplicationInfo> installedPkgs = mPackageManager.getInstalledApplications(0);
            synchronized (mUidPackages) {
                mDownloadUids.clear();
                StringBuilder sbDownload = new StringBuilder();
                for (ApplicationInfo appInfo : installedPkgs) {
                    if (appInfo != null) {
                        int uid = appInfo.uid;
                        String packageName = appInfo.packageName;
                        try {
                            if (!mScreenOffCDNWhiteUids.contains(Integer.valueOf(uid)) && !mScreenOffSystemWhiteUids.contains(Integer.valueOf(uid)) && mScreenOffAppOpsUids.contains(Integer.valueOf(uid))) {
                                boolean isDownload = mAsusFreezerService.getDownloadBehavior(packageName, uid);
                                if (isDownload) {
                                    mDownloadUids.add(Integer.valueOf(uid));
                                    sbDownload.append("uid:" + uid + "; ");
                                }
                            }
                        } catch (RemoteException e) {
                        }
                    }
                }
                if (mDownloadUids.size() > 0) {
                    GameLog.screenOffDynamicUidChange("download", sbDownload.toString());
                    mHandler.removeMessages(4);
                    ScreenOffHandler screenOffHandler = mHandler;
                    screenOffHandler.sendMessageDelayed(screenOffHandler.obtainMessage(4), 60000L);
                }
                mHandler.removeMessages(2);
                mHandler.obtainMessage(2).sendToTarget();
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        private final Uri mAsusFreezeEnableUri;
        private final Uri mAsusFreezerXmlUri;
        private final Uri mScreenOffDebugUri;
        private final Uri mScreenOffDelayUri;
        private final Uri mScreenOffDurationCheckUidUri;
        private final Uri mScreenOffFirewallEnableUri;
        private final Uri mScreenOffIntermissionDurationTurnOffUri;
        private final Uri mScreenOffIntermissionDurationTurnOnUri;
        private final Uri mScreenOffIntermissionEnableUri;

        public SettingsObserver(Context context) {
            super(new Handler());
            Uri uriFor = Settings.System.getUriFor(ScreenOffFirewall.ASUS_FREEZE_ENABLE);
            mAsusFreezeEnableUri = uriFor;
            Uri uriFor2 = Settings.System.getUriFor(ScreenOffFirewall.SCREEN_OFF_FIREWALL_ENABLE);
            mScreenOffFirewallEnableUri = uriFor2;
            Uri uriFor3 = Settings.System.getUriFor(ScreenOffFirewall.SCREEN_OFF_FIREWALL_DELAY);
            mScreenOffDelayUri = uriFor3;
            Uri uriFor4 = Settings.System.getUriFor(ScreenOffFirewall.SCREEN_OFF_FIREWALL_DEBUG);
            mScreenOffDebugUri = uriFor4;
            Uri uriFor5 = Settings.System.getUriFor(ScreenOffFirewall.SCREEN_OFF_FIREWALL_INTERMISSION_ENABLE);
            mScreenOffIntermissionEnableUri = uriFor5;
            Uri uriFor6 = Settings.System.getUriFor(ScreenOffFirewall.SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_ON);
            mScreenOffIntermissionDurationTurnOnUri = uriFor6;
            Uri uriFor7 = Settings.System.getUriFor(ScreenOffFirewall.SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_OFF);
            mScreenOffIntermissionDurationTurnOffUri = uriFor7;
            Uri uriFor8 = Settings.System.getUriFor("asus_freezer_xml_parsing_done");
            mAsusFreezerXmlUri = uriFor8;
            Uri uriFor9 = Settings.System.getUriFor(ScreenOffFirewall.SCREEN_OFF_FIREWALL_DURATION_CHECK_UID);
            mScreenOffDurationCheckUidUri = uriFor9;
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(uriFor, false, this, -1);
            resolver.registerContentObserver(uriFor2, false, this, -1);
            resolver.registerContentObserver(uriFor3, false, this, -1);
            resolver.registerContentObserver(uriFor4, false, this, -1);
            resolver.registerContentObserver(uriFor5, false, this, -1);
            resolver.registerContentObserver(uriFor6, false, this, -1);
            resolver.registerContentObserver(uriFor7, false, this, -1);
            resolver.registerContentObserver(uriFor8, false, this, -1);
            resolver.registerContentObserver(uriFor9, false, this, -1);
            updateCdnConfig();
            checkScreenOffState();
            checkDebugMode();
            checkScreenOffDurationUid();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }
            if (mAsusFreezeEnableUri.equals(uri) || mScreenOffFirewallEnableUri.equals(uri) || mScreenOffDelayUri.equals(uri) || mScreenOffIntermissionEnableUri.equals(uri) || mScreenOffIntermissionDurationTurnOnUri.equals(uri) || mScreenOffIntermissionDurationTurnOffUri.equals(uri)) {
                checkScreenOffState();
                StringBuilder config = new StringBuilder();
                config.append("mSettingAsusFreeze=" + mSettingAsusFreeze + ",mSettingScreenOffEnable=" + mSettingScreenOffEnable + ",mScreenOffDelay=" + mScreenOffDelay + ",mScreenOffIntermissionEnable=" + mScreenOffIntermissionEnable + ",mScreenOffIntermissionDurationTurnOn=" + mScreenOffIntermissionDurationTurnOn + ",mScreenOffIntermissionDurationTurnOff=" + mScreenOffIntermissionDurationTurnOff);
                GameLog.screenOffDynamicUidChange("config_change", config.toString());
            } else if (mScreenOffDebugUri.equals(uri)) {
                checkDebugMode();
            } else if (mAsusFreezerXmlUri.equals(uri)) {
                updateCdnConfig();
            } else if (mScreenOffDurationCheckUidUri.equals(uri)) {
                checkScreenOffDurationUid();
            }
        }
    }

    private void updateCdnConfig() {
        long asusFreezerXmlParse = Settings.System.getLong(mContext.getContentResolver(), "asus_freezer_xml_parsing_done", -1L);
        if (asusFreezerXmlParse == mAsusFreezerXmlParse) {
            return;
        }
        mAsusFreezerXmlParse = asusFreezerXmlParse;
        GameLog.screenOffCdnUpdate(asusFreezerXmlParse);
        if (mAsusFreezerService == null) {
            mAsusFreezerService = IAsusFreezerService.Stub.getDefaultImpl();
        }
        IAsusFreezerService iAsusFreezerService = mAsusFreezerService;
        if (iAsusFreezerService != null) {
            try {
                Map<String, String> netVarMap = iAsusFreezerService.getNetVarMap();
                if (netVarMap != null) {
                    for (Map.Entry<String, String> entry : netVarMap.entrySet()) {
                        if (entry != null) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if ("screen_off_firewall_enable".equals(key)) {
                                try {
                                    int enable = Integer.parseInt(value);
                                    Settings.System.putInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_ENABLE, enable);
                                } catch (NumberFormatException e) {
                                }
                            }
                            if ("screen_off_wait_time".equals(key)) {
                                try {
                                    int delay = Integer.parseInt(value);
                                    Settings.System.putInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_DELAY, delay);
                                } catch (NumberFormatException e2) {
                                }
                            } else if ("app_block_time".equals(key)) {
                                try {
                                    int duration = Integer.parseInt(value);
                                    Settings.System.putInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_ON, duration);
                                } catch (NumberFormatException e3) {
                                }
                            } else if ("app_idle_time".equals(key)) {
                                try {
                                    int duration2 = Integer.parseInt(value);
                                    Settings.System.putInt(mContext.getContentResolver(), SCREEN_OFF_FIREWALL_INTERMISSION_DURATION_PHASE_OFF, duration2);
                                } catch (NumberFormatException e4) {
                                }
                            }
                        }
                    }
                }
                List<String> netWhiteList = mAsusFreezerService.getBlackOrWhiteList(0, 0);
                if (netWhiteList != null) {
                    mCdnWhitePackages = netWhiteList;
                }
                List<String> netBlackList = mAsusFreezerService.getBlackOrWhiteList(0, 1);
                if (netBlackList != null) {
                    mCdnBlackPackages = netBlackList;
                }
                updateAllCdnUids();
            } catch (Exception e5) {
                Slog.w(TAG, "Failed to update cdn config.", e5);
            }
        }
    }

    private void updateAllCdnUids() {
        List<ApplicationInfo> installedPkgs = mPackageManager.getInstalledApplications(0);
        synchronized (mUidPackages) {
            mScreenOffCDNWhiteUids.clear();
            mScreenOffCDNBlackUids.clear();
            for (ApplicationInfo appInfo : installedPkgs) {
                if (appInfo != null) {
                    int uid = appInfo.uid;
                    String packageName = appInfo.packageName;
                    if (mCdnWhitePackages.contains(packageName)) {
                        mScreenOffCDNWhiteUids.add(Integer.valueOf(uid));
                    }
                    mScreenOffCDNBlackUids.clear();
                    if (mCdnBlackPackages.contains(packageName)) {
                        mScreenOffCDNBlackUids.add(Integer.valueOf(uid));
                    }
                }
            }
            mHandler.removeMessages(2);
            mHandler.obtainMessage(2).sendToTarget();
        }
    }

    private void checkDynamicUids() {
        int rawProcessState;
        synchronized (mUidPackages) {
            Set<Integer> uids = new HashSet<>(mScreenOffAppOpsUids);
            if (mAsusFreezerService == null) {
                mAsusFreezerService = IAsusFreezerService.Stub.getDefaultImpl();
            }
            if (mDeviceIdleController == null) {
                mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            }
            String[] whitelistedApps = new String[0];
            IDeviceIdleController iDeviceIdleController = mDeviceIdleController;
            if (iDeviceIdleController != null) {
                try {
                    whitelistedApps = iDeviceIdleController.getFullPowerWhitelist();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Unable to reach IDeviceIdleController", e);
                }
            }
            if (mActivityManagerInternal == null) {
                mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            }
            StringBuilder sbProcessState = new StringBuilder();
            StringBuilder sbAppBehavior = new StringBuilder();
            StringBuilder sbBattery = new StringBuilder();
            mScreenOffDynamicWhiteUids.clear();
            for (Integer num : uids) {
                int uid = num.intValue();
                ActivityManagerInternal activityManagerInternal = mActivityManagerInternal;
                if (activityManagerInternal != null && (rawProcessState = activityManagerInternal.getUidProcessState(uid)) <= 5) {
                    mScreenOffDynamicWhiteUids.add(Integer.valueOf(uid));
                    sbProcessState.append("uid:" + uid + ",state:" + rawProcessState + "; ");
                }
                List<String> packages = mUidPackages.get(Integer.valueOf(uid));
                if (packages != null) {
                    for (String pkg : packages) {
                        IAsusFreezerService iAsusFreezerService = mAsusFreezerService;
                        if (iAsusFreezerService != null) {
                            try {
                                int binaryEventCheck = iAsusFreezerService.getAppBehavior(pkg, uid, 27);
                                if (binaryEventCheck != 0) {
                                    mScreenOffDynamicWhiteUids.add(Integer.valueOf(uid));
                                    sbAppBehavior.append("uid:" + uid + ",pkg:" + pkg + ",event:" + binaryEventCheck + "; ");
                                }
                                if (Arrays.asList(whitelistedApps).contains(pkg)) {
                                    mScreenOffDynamicWhiteUids.add(Integer.valueOf(uid));
                                    sbBattery.append("uid:" + uid + "; ");
                                }
                            } catch (RemoteException e2) {
                                Slog.w(TAG, "Failed to getAppBehavior.", e2);
                            }
                        }
                    }
                }
            }
            if (sbProcessState.length() > 0) {
                GameLog.screenOffDynamicUidChange("process_state", sbProcessState.toString());
            }
            if (sbAppBehavior.length() > 0) {
                GameLog.screenOffDynamicUidChange("appbehavior", sbAppBehavior.toString());
            }
            if (sbBattery.length() > 0) {
                GameLog.screenOffDynamicUidChange("battery", sbBattery.toString());
            }
            mHandler.removeMessages(2);
            mHandler.obtainMessage(2).sendToTarget();
        }
    }

    private void turnOnScreenOffFirewallInner(boolean turnOn) {
        int i = 1;
        try {
            if (turnOn) {
                GameNetworkHelper.enableTLChain(GameNetworkHelper.CHAIN_SCREEN_OFF, true);
            } else {
                GameNetworkHelper.enableTLChain(GameNetworkHelper.CHAIN_SCREEN_OFF, false);
            }
            ContentResolver contentResolver = mContext.getContentResolver();
            if (!turnOn) {
                i = 0;
            }
            Settings.System.putInt(contentResolver, SCREEN_OFF_FIREWALL_TURN_ON, i);
            GameLog.screenOffModeChange(turnOn);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to turn on screen off firewall.", e);
        }
    }

    private void setScreenOffUidLocked(int uid, boolean add) {
        if (mScreenOffNetdUids.contains(Integer.valueOf(uid)) == add) {
            return;
        }
        if (add) {
            mScreenOffNetdUids.add(Integer.valueOf(uid));
        } else {
            mScreenOffNetdUids.remove(Integer.valueOf(uid));
        }
        try {
            GameNetworkHelper.setScreenOffApps(uid, add);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to set screen off uid.", e);
        }
    }

    private boolean isSystem(int uid, String packageName) {
        try {
            int userId = UserHandle.getUserId(uid);
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, userId);
            if ((appInfo == null || (appInfo.flags & 1) <= 0) && uid > 1000) {
                if (!packageName.contains("com.google.android")) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private class ScreenOffHandler extends Handler {
        static final int MSG_CHECK_DYNAMIC_UID = 3;
        static final int MSG_CHECK_SCREEN_OFF_FIREWALL_UID = 2;
        static final int MSG_GET_DOWNLOAD_BEHAVIOR = 5;
        static final int MSG_PREPARE_DOWNLOAD_CHECK = 4;
        static final int MSG_TURN_ON_SCREEN_OFF_FIREWALL = 1;

        ScreenOffHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    boolean z = true;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    boolean turnOn = z;
                    turnOnScreenOffFirewallInner(turnOn);
                    return;
                case 2:
                    if (!mSettingScreenOffEnable) {
                        return;
                    }
                    checkScreenOffUids();
                    return;
                case 3:
                    if (!mSettingScreenOffEnable || !mIsDeviceIdleControllerReady) {
                        return;
                    }
                    checkDynamicUids();
                    long now = System.currentTimeMillis();
                    if (mScreenOff && mSettingEnableFirewall && now - mScreenOffTime >= mScreenOffDelay * 1000) {
                        mHandler.removeMessages(3);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(3), mScreenOffCheckDynamicDuration * 1000);
                        return;
                    }
                    return;
                case 4:
                    if (!mSettingScreenOffEnable) {
                        return;
                    }
                    prepareDownloadCheck();
                    return;
                case 5:
                    if (!mSettingScreenOffEnable) {
                        return;
                    }
                    getDownloadBehavior();
                    return;
                default:
                    return;
            }
        }
    }
}
