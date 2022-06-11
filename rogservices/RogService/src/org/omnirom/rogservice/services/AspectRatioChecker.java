/*
 * Copyright (c) 2022 The OmniRom Project
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

package org.omnirom.rogservice.services;

import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Slog;
import android.util.Xml;
import android.view.WindowManager;
import com.android.internal.telephony.HbpcdLookup;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;

public class AspectRatioChecker {
    private static final String ANDROID_APP_PREFIX = "com.android";
    private static final String ANDROID_SYSTEM = "android";
    private static final String ASUS_APP_PREFIX = "com.asus";
    private static final String COLUMN_NAME_IS_GAME = "is_game";
    private static final String COLUMN_NAME_PACKAGE_NAME = "packagename";
    public static final String DATA_SCALING_FOLDER = "/data/system/appscaling/";
    public static final boolean ENABLE_NOTCH_UI = SystemProperties.getBoolean("persist.sys.notchui.enable", false);
    public static final String FILE_ASPECT_RATIO_CONFIG = "app_aspect_ratio_config.xml";
    public static final String FILE_NOTCH_CONFIG = "app_notch_config.xml";
    private static final String GAME_APP_PROVIDER_URI = "content://com.asus.focusapplistener.game.GameAppProvider";
    private static final String GOOGLE_APP_PREFIX = "com.google";
    private static final String SYSTEM_SCALING_CONTROL = "sys_scaling_ctrl";
    private static final String GAME_GENIE_SCALING_CONTROL = "gg_scaling_ctrl";
    private static final float MAX_ASPECT_FULL_SCREEN = Float.MAX_VALUE;
    private static final float MAX_ASPECT_SIXTEEN_NINE = 1.78f;
    private static final String SYS_SCALING_FOLDER = "/system/etc/appscaling/";
    private static final String TAG = "AspectRatioChecker";
    private static AspectRatioChecker sInstance;
    private final ArraySet<String> mSystemApps = new ArraySet<>();
    private final HashMap<String, Float> mAppAspectConfigs = new HashMap<>();
    private final HashMap<String, NotchConfig> mAppNotchConfigs = new HashMap<>();
    private final HashMap<String, NotchConfig> mWinNotchConfigs = new HashMap<>();
    private long mAspectConfigLastModified = -1;
    private long mNotchConfigLastModified = -1;
    private boolean mSystemReady = false;
    private boolean mSysScalingCtrlEnabled = true;
    private boolean mGameGenieScalingCtrlEnabled = false;

    private static class NotchConfig {
        boolean canfillNotchInLand;
        boolean fillNotch;
        boolean locked;
        float minAspect;
        boolean mustRestart;

        private NotchConfig() {
            fillNotch = false;
            canfillNotchInLand = true;
            locked = false;
            mustRestart = false;
            minAspect = 0.0f;
        }
    }

    private AspectRatioChecker() {
        refresh();
    }

    public static AspectRatioChecker getInstance() {
        if (sInstance == null) {
            sInstance = new AspectRatioChecker();
        }
        return sInstance;
    }

    private File getLastModifiedFile(String fileName) {
        File preloadedFile = new File(SYS_SCALING_FOLDER + fileName);
        long preloadedLastModified = 0;
        if (preloadedFile.exists() && preloadedFile.canRead()) {
            preloadedLastModified = preloadedFile.lastModified();
        }
        File downloadedFile = new File(DATA_SCALING_FOLDER + fileName);
        long downloadedLastModified = 0;
        if (downloadedFile.exists() && downloadedFile.canRead()) {
            downloadedLastModified = downloadedFile.lastModified();
        }
        if (downloadedLastModified > preloadedLastModified) {
            return downloadedFile;
        }
        return preloadedFile;
    }

    public List<String> getRecordedApps() {
        ArrayList arrayList;
        synchronized (mAppAspectConfigs) {
            arrayList = new ArrayList(mAppAspectConfigs.keySet());
        }
        return arrayList;
    }

    public float getMinAspect(WindowManager.LayoutParams attrs) {
        String title = attrs.getTitle().toString();
        synchronized (mWinNotchConfigs) {
            NotchConfig config = mWinNotchConfigs.get(title);
            if (config == null) {
                return 0.0f;
            }
            return config.minAspect;
        }
    }

    public boolean isGameApp(String pkgName) {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            return false;
        }
        Context context = activityThread.getSystemContext();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            try {
                try {
                    cursor = cr.query(Uri.parse(GAME_APP_PROVIDER_URI), new String[]{COLUMN_NAME_IS_GAME}, "packagename=?", new String[]{pkgName}, null);
                } catch (Exception e) {
                }
            } catch (Exception e2) {
                Slog.w(TAG, "Get the category failed, err: " + e2.getMessage());
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (cursor != null && cursor.moveToFirst()) {
                return "1".equals(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_IS_GAME)));
            }
            if (cursor != null) {
                cursor.close();
            }
            return false;
        } finally {
            if (0 != 0) {
                try {
                    cursor.close();
                } catch (Exception e3) {
                }
            }
        }
    }

    public static boolean isVipApp(String pkgName) {
        if (pkgName != null) {
            if (pkgName.equals("android") || pkgName.startsWith(ANDROID_APP_PREFIX) || pkgName.startsWith(GOOGLE_APP_PREFIX) || pkgName.startsWith(ASUS_APP_PREFIX)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isDefaultFillNotchRegion(String pkgName) {
        synchronized (mAppNotchConfigs) {
            NotchConfig notchData = mAppNotchConfigs.get(pkgName);
            if (notchData != null && notchData.fillNotch) {
                return true;
            }
            if (pkgName != null && pkgName.startsWith(ASUS_APP_PREFIX)) {
                return true;
            }
            return false;
        }
    }

    public boolean mustFillNotchRegion(String pkgName) {
        synchronized (mAppNotchConfigs) {
            NotchConfig notchData = mAppNotchConfigs.get(pkgName);
            if (notchData != null) {
                if (notchData.fillNotch && notchData.locked) {
                    return true;
                }
                if (!notchData.fillNotch && notchData.locked) {
                    return false;
                }
            }
            return pkgName != null && pkgName.startsWith(ASUS_APP_PREFIX);
        }
    }

    public boolean mustFillNotchRegion(WindowManager.LayoutParams attrs) {
        if (mSysScalingCtrlEnabled) {
            return false;
        }
        int sysUiFl = attrs.systemUiVisibility | attrs.subtreeSystemUiVisibility;
        int fl = attrs.flags;
        boolean isFullscreen = ((fl & 1024) == 0 && (sysUiFl & 4) == 0) ? false : true;
        if (!isFullscreen) {
            return false;
        }
        String title = attrs.getTitle().toString();
        synchronized (mWinNotchConfigs) {
            NotchConfig config = mWinNotchConfigs.get(title);
            if (config == null) {
                return false;
            }
            return config.fillNotch;
        }
    }

    public boolean mustNotFillNotchRegion(String pkgName) {
        synchronized (mAppNotchConfigs) {
            NotchConfig notchData = mAppNotchConfigs.get(pkgName);
            if (notchData != null && !notchData.fillNotch && notchData.locked) {
                return true;
            }
            return false;
        }
    }

    public boolean mustNotFillNotchRegion(WindowManager.LayoutParams attrs) {
        String title = attrs.getTitle().toString();
        synchronized (mWinNotchConfigs) {
            NotchConfig notchData = mWinNotchConfigs.get(title);
            if (notchData != null && !notchData.fillNotch && notchData.locked) {
                return true;
            }
            return false;
        }
    }

    public boolean mustNotFillNotchRegionInLandscape(String pkgName) {
        synchronized (mAppNotchConfigs) {
            NotchConfig notchData = mAppNotchConfigs.get(pkgName);
            boolean z = true;
            if (notchData == null) {
                return pkgName != null && pkgName.startsWith(ASUS_APP_PREFIX);
            }
            if (notchData.canfillNotchInLand) {
                z = false;
            }
            return z;
        }
    }

    public boolean mustRestart(String packageName, List<FeatureInfo> reqFeatures) {
        if (!ENABLE_NOTCH_UI) {
            return false;
        }
        for (FeatureInfo feature : reqFeatures) {
            if (PackageManager.FEATURE_VR_MODE.equals(feature.name)) {
                return true;
            }
        }
        return isGameApp(packageName);
    }

    private void parseAspectConfig(File file) {
        XmlPullParser parser;
        parser = Xml.newPullParser();
        int type = 0;
        HashMap<String, Float> appAspectConfigs = new HashMap<>();
        try {
            FileReader reader = new FileReader(file);
            try {
                try {
                    parser.setInput(reader);
                    while (true) {
                        type = parser.next();
                        if (type == 2 || type == 1) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Parse " + file.getName() + " failed, err: " + e.getMessage());
                }
                if (type == 2 && parser.getName().equals("config")) {
                    while (true) {
                        XmlUtils.nextElement(parser);
                        if (parser.getEventType() == 1) {
                            break;
                        }
                        String name = parser.getName();
                        if ("sixteen-nine".equals(name)) {
                            String pkgName = parser.getAttributeValue(null, "package");
                            if (pkgName == null) {
                                Slog.w(TAG, "<sixteen-nine> without package in " + file + " at " + parser.getPositionDescription());
                            } else {
                                appAspectConfigs.put(pkgName, Float.valueOf((float) MAX_ASPECT_SIXTEEN_NINE));
                            }
                            XmlUtils.skipCurrentTag(parser);
                        } else if ("fullscreen".equals(name)) {
                            String pkgName2 = parser.getAttributeValue(null, "package");
                            if (pkgName2 == null) {
                                Slog.w(TAG, "<fullscreen> without package in " + file + " at " + parser.getPositionDescription());
                            } else {
                                appAspectConfigs.put(pkgName2, Float.valueOf(Float.MAX_VALUE));
                            }
                            XmlUtils.skipCurrentTag(parser);
                        } else if (HbpcdLookup.PATH_ARBITRARY_MCC_SID_MATCH.equals(name)) {
                            String pkgName3 = parser.getAttributeValue(null, "package");
                            if (pkgName3 == null) {
                                Slog.w(TAG, "<arbitrary> without package in " + file + " at " + parser.getPositionDescription());
                            } else {
                                String maxAspectStr = parser.getAttributeValue(null, "maxAspect");
                                try {
                                    appAspectConfigs.put(pkgName3, Float.valueOf(Float.parseFloat(maxAspectStr)));
                                } catch (Exception e2) {
                                    Slog.w(TAG, "Parse maxAspect " + maxAspectStr + " failed, err: " + e2.getMessage());
                                }
                            }
                        }
                    }
                    synchronized (mAppAspectConfigs) {
                        mAppAspectConfigs.clear();
                        mAppAspectConfigs.putAll(appAspectConfigs);
                    }
                }
            } finally {
                IoUtils.closeQuietly(reader);
            }
        } catch (Exception e3) {
            Slog.w(TAG, "Couldn't find or open file " + file);
        }
    }

    private void parseNotchConfig(File file) {
        XmlPullParser parser;
        parser = Xml.newPullParser();
        int type = 0;
        HashMap<String, NotchConfig> apps = new HashMap<>();
        HashMap<String, NotchConfig> wins = new HashMap<>();
        mSysScalingCtrlEnabled = true;
        mGameGenieScalingCtrlEnabled = true;
        try {
            FileReader reader = new FileReader(file);
            try {
                try {
                    parser.setInput(reader);
                    while (true) {
                        type = parser.next();
                        if (type == 2 || type == 1) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Parse " + file.getName() + " failed, err: " + e.getMessage());
                }
                if (type == 2 && parser.getName().equals("config")) {
                    while (true) {
                        XmlUtils.nextElement(parser);
                        if (parser.getEventType() == 1) {
                            break;
                        }
                        String name = parser.getName();
                        if ("package".equals(name)) {
                            String pkgName = parser.getAttributeValue(null, "name");
                            if (pkgName == null) {
                                Slog.w(TAG, "<package> without package in " + file + " at " + parser.getPositionDescription());
                            } else {
                                NotchConfig data = apps.get(pkgName);
                                if (data == null) {
                                    data = new NotchConfig();
                                    apps.put(pkgName, data);
                                }
                                data.fillNotch = Boolean.parseBoolean(parser.getAttributeValue(null, "fillNotch"));
                                String canfillNotchInLandStr = parser.getAttributeValue(null, "canfillNotchInLand");
                                if (canfillNotchInLandStr != null) {
                                    data.canfillNotchInLand = Boolean.parseBoolean(canfillNotchInLandStr);
                                }
                                data.locked = Boolean.parseBoolean(parser.getAttributeValue(null, "locked"));
                                data.mustRestart = Boolean.parseBoolean(parser.getAttributeValue(null, "restart"));
                            }
                            XmlUtils.skipCurrentTag(parser);
                        } else if (Context.WINDOW_SERVICE.equals(name)) {
                            String winName = parser.getAttributeValue(null, "name");
                            if (winName == null) {
                                Slog.w(TAG, "<window> without title in " + file + " at " + parser.getPositionDescription());
                            } else {
                                NotchConfig data2 = wins.get(winName);
                                if (data2 == null) {
                                    data2 = new NotchConfig();
                                    wins.put(winName, data2);
                                }
                                data2.fillNotch = Boolean.parseBoolean(parser.getAttributeValue(null, "fillNotch"));
                                data2.locked = Boolean.parseBoolean(parser.getAttributeValue(null, "locked"));
                                String minAspectStr = parser.getAttributeValue(null, "minAspect");
                                if (minAspectStr != null) {
                                    try {
                                        data2.minAspect = Float.parseFloat(minAspectStr);
                                    } catch (Exception e2) {
                                        Slog.w(TAG, "Parse minAspect of " + winName + " failed, err:" + e2.getMessage());
                                    }
                                }
                            }
                            XmlUtils.skipCurrentTag(parser);
                        } else if ("switch".equals(name)) {
                            boolean enable = Boolean.parseBoolean(parser.getAttributeValue(null, "enable"));
                            String switchName = parser.getAttributeValue(null, "name");
                            if ("system".equals(switchName)) {
                                mSysScalingCtrlEnabled = enable;
                            } else if ("gamegenie".equals(switchName)) {
                                mGameGenieScalingCtrlEnabled = enable;
                            }
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                    synchronized (mAppNotchConfigs) {
                        mAppNotchConfigs.clear();
                        mAppNotchConfigs.putAll(apps);
                    }
                    synchronized (mWinNotchConfigs) {
                        mWinNotchConfigs.clear();
                        mWinNotchConfigs.putAll(wins);
                    }
                    refreshSettings();
                }
            } finally {
                IoUtils.closeQuietly(reader);
            }
        } catch (Exception e3) {
            Slog.w(TAG, "Couldn't find or open file " + file);
        }
    }

    public void refresh() {
        File aspectConfigFile = getLastModifiedFile(FILE_ASPECT_RATIO_CONFIG);
        if (aspectConfigFile.exists() && aspectConfigFile.canRead()) {
            long lastModified = aspectConfigFile.lastModified();
            if (mAspectConfigLastModified != lastModified) {
                parseAspectConfig(aspectConfigFile);
                mAspectConfigLastModified = lastModified;
            }
        }
        File appNotchConfigsFile = getLastModifiedFile(FILE_NOTCH_CONFIG);
        if (appNotchConfigsFile.exists() && appNotchConfigsFile.canRead()) {
            long lastModified2 = appNotchConfigsFile.lastModified();
            if (mNotchConfigLastModified != lastModified2) {
                parseNotchConfig(appNotchConfigsFile);
                mNotchConfigLastModified = lastModified2;
            }
        }
    }

    private void refreshSettings() {
        ActivityThread activityThread;
        if (!mSystemReady || (activityThread = ActivityThread.currentActivityThread()) == null) {
            return;
        }
        Context context = activityThread.getSystemContext();
        ContentResolver cr = context.getContentResolver();
        int i = 1;
        try {
            Settings.Secure.putInt(cr, SYSTEM_SCALING_CONTROL, mSysScalingCtrlEnabled ? 1 : 0);
        } catch (Exception e) {
            Slog.w(TAG, "Write settings SYSTEM_SCALING_CONTROL failed, err: " + e.getMessage());
        }
        try {
            if (!mGameGenieScalingCtrlEnabled) {
                i = 0;
            }
            Settings.Secure.putInt(cr, GAME_GENIE_SCALING_CONTROL, i);
        } catch (Exception e2) {
            Slog.w(TAG, "Write settings GAME_GENIE_SCALING_CONTROL failed, err: " + e2.getMessage());
        }
    }

    public void systemReady() {
        mSystemReady = true;
        refreshSettings();
    }
}
