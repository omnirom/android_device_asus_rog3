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

package org.omnirom.rogservice.services;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import libcore.io.IoUtils;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import org.omnirom.rogservice.AuraLightEffect;
import org.omnirom.rogservice.AuraLightManager;
import org.omnirom.rogservice.IAuraLightService;
import org.omnirom.rogservice.services.IAuraLightServiceInternal;

public class AuraLightService {
    private static final String ACTION_BIND_TGPA_SERVICE = "com.tencent.inlab.tcsystem.action.AIDL_TCSYSTEMSERVICE";
    private static final String ACTION_FAN_FW_UPDATED = "com.asus.gamecenter.SYSTEMFW_UPDATE_FAN";
    private static final String ACTION_POWER_SAVER_MODE_CHANGED = "com.asus.powersaver.action.power_saver_mode";
    private static final String ACTION_SCHEDULE_SYNC_FRAME;
    private static final String ACTION_STOP_NOTIFICATION;
    private static final int ASUS_ANALYTICS_UPLOAD_INTERVAL = 86400000;
    private static final String ATTR_AURA_LIGHT_SCENARIO_ACTIVE = "active";
    private static final String ATTR_AURA_LIGHT_SCENARIO_COLOR = "color";
    private static final String ATTR_AURA_LIGHT_SCENARIO_MODE = "mode";
    private static final String ATTR_AURA_LIGHT_SCENARIO_RATE = "rate";
    private static final String ATTR_AURA_LIGHT_SCENARIO_TYPE = "type";
    private static final String ATTR_AURA_LIGHT_SETTING_ENABLED = "enabled";
    private static final String ATTR_AURA_LIGHT_SETTING_VERSION = "version";
    private static final String ATTR_AURA_NOTIFICATION_SCENARIO_PACKAGE = "package";
    private static final String ATTR_BUMPER_STATE = "state";
    private static final int BLENDED_MODE_COMET_TO_LEFT = 8;
    private static final int BLENDED_MODE_COMET_TO_RIGHT = 6;
    private static final int BLENDED_MODE_FLASH_DASH_TO_LEFT = 9;
    private static final int BLENDED_MODE_FLASH_DASH_TO_RIGHT = 7;
    private static final int BLENDED_MODE_MIXED_ASYNC = 4;
    private static final int BLENDED_MODE_MIXED_SINGLE = 5;
    private static final int BLENDED_MODE_MIXED_STATIC = 2;
    private static final int BLENDED_MODE_MIXED_SYNC = 3;
    private static final int BLENDED_MODE_RAINBOW = 1;
    public static final List<AuraLightEffect> BUMPER_INSTALL_EFFECT_02;
    public static final List<AuraLightEffect> BUMPER_INSTALL_EFFECT_03;
    private static final int BUMPER_TAG_END_BLOCK = 17;
    private static final String BUMPER_URI = "bumper://android";
    private static final int BUMPER_VENDOR_ASUS_ID = 2;
    private static final String CLS_NAME_TGPA_SERVICE;
    private static final boolean DEBUG_ANALYTICS;
    private static final int DEFAULT_BLUE_COLOR;
    private static final int DEFAULT_LED_STATES;
    private static final int DEFAULT_NOTIFICATION_EXPIRATION_TIME = 1800000;
    private static final int DEFAULT_RED_COLOR;
    private static final int DEFAULT_WHITE_COLOR;
    private static final int DONGLE_TYPE_DT_DOCK = 3;
    private static final int DONGLE_TYPE_INBOX = 1;
    private static final int DONGLE_TYPE_NO_DONGLE = 0;
    private static final int DONGLE_TYPE_OTHER = 4;
    private static final int DONGLE_TYPE_STATION = 2;
    private static final String DROPBOX_TAG_CUSTOM_EFFECT_CHANGE_COUNT = "asus_light_game_cnt";
    private static final String DROPBOX_TAG_CUSTOM_EFFECT_TOTAL_TIME = "asus_light_game_time";
    private static final String DROPBOX_TAG_CUSTOM_LIGHT_EVENT = "asus_light_type_gameevent";
    private static final String DROPBOX_TAG_INBOX_CONNECT = "asus_inbox_connect";
    private static final String DROPBOX_TAG_REAL_LIGHT_ON = "asus_light_real_on";
    private static final String DROPBOX_TAG_SYSTEM_EFFECT_CHANGE_COUNT = "asus_light_switch_cnt";
    private static final String DROPBOX_TAG_SYSTEM_EFFECT_TOTAL_TIME = "asus_light_switch_time";
    private static final String DROPBOX_TAG_SYSTEM_LIGHT_EVENT = "asus_light_type_systemevent";
    private static final String EXTRA_POWER_SAVER_MODE = "com.asus.powersaver.key.power_saver_mode";
    private static final String FACTORY_ALGORITHM = "PBKDF2WithHmacSHA1";
    public static final List<AuraLightEffect> GAME_APPS_LAUNCH_EFFECT;
    private static int GAME_APPS_LAUNCH_EFFECT_DELAY = 0;
    private static double GAME_OBIWAN_RATE = 0.0d;
    private static final boolean HAS_2ND_DISPLAY;
    private static final boolean IS_ANAKIN;
    private static final boolean IS_PICASSO;
    private static final int ITERATION_COUNT = 5308;
    private static final String KEY_ALGORITHM = "AES";
    private static final int LED_STATE_TURN_ON_ALL = 0;
    private static final int LED_STATE_TURN_ON_FRONT = 2;
    private static final int LED_STATE_TURN_ON_LOGO = 1;
    private static final int MAX_CUSTOM_EFFECT_TIME = 60000;
    private static final int MODE_CUSTOM_DEEP = 6;
    private static final int MODE_EXTREME_DURABLE = 11;
    private static final int MODE_ULTRA_SAVING = 1;
    private static final int MODE_UNSPECIFIED = -1;
    private static final int MSG_APPLY_CUSTOM_EFFECT = 7;
    private static final int MSG_BIND_TCSYSTEMSERVICE = 18;
    private static final int MSG_EXEC_TCSYSTEMSERVICE_CMD = 19;
    private static final int MSG_LID_SWITCH_CHANGED = 9;
    private static final int MSG_NFC_TAG_DISCOVERED = 10;
    private static final int MSG_NOTIFY_LIGHT_CHANGED = 0;
    private static final int MSG_NOTIFY_SETTINGS_CHANGED = 1;
    private static final int MSG_SET_CUSTOM_EFFECT = 6;
    private static final int MSG_SET_FOCUSED_APP = 8;
    private static final int MSG_SET_LIGHT_AGAIN = 2;
    private static final int MSG_SET_SYS_CUSTOM_EFFECT = 14;
    private static final int MSG_STOP_NOTIFICATION_LIGHT = 3;
    private static final int MSG_SYNC_WITH_GAME_VICE_AND_INBOX = 21;
    private static final int MSG_TURN_OFF_NFC = 12;
    private static final int MSG_TURN_ON_NFC_IF_NEEDED = 11;
    private static final int MSG_UPDATE_DONGLE_TYPE = 22;
    private static final int MSG_UPDATE_NOTIFICATION_LIGHT = 15;
    private static final int MSG_UPDATE_PHONE_RELATED_SCENARIO = 20;
    private static final int MSG_UPDATE_SUSPENSION_RELATED_SCENARIO = 5;
    private static final int MSG_UPLOAD_ASUS_ANALYTICS_REGULARLY = 27;
    private static final int MSG_UPLOAD_ASUS_INBOX_ANALYTICS = 25;
    private static final int MSG_UPLOAD_ASUS_INBOX_ANALYTICS_FOR_BATTERY_RESET = 26;
    private static final int MSG_UPLOAD_ASUS_LIGHT_ANALYTICS = 23;
    private static final int MSG_UPLOAD_ASUS_LIGHT_ANALYTICS_FOR_BATTERY_RESET = 24;
    private static final int MSG_UPLOAD_CUSTOM_LIGHT_ANALYTICS = 17;
    private static final int MSG_UPLOAD_SYSTEM_LIGHT_ANALYTICS = 16;
    private static final int MSG_WRITE_SETTINGS = 4;
    private static final String MUSIC_NOTIFICATION_CHANNEL_ID = "com.asus.aurasync.musiceffect";
    private static final String MUSIC_NOTIFICATION_OWNER = "com.asus.gamecenter";
    private static final String PACKAGE_NAME_TGPA;
    private static final int PHONE_STATE_NONE = 0;
    private static final int PHONE_STATE_OFF_HOOK = 2;
    private static final int PHONE_STATE_RINGING = 1;
    private static final List<AuraLightEffect> POWER_CONNECTED_EFFECT;
    private static final String PROP_BOOTING_EFFECT = "persist.sys.aura.booteffect";
    private static final String PROP_BUMPER_ENABLED = "vendor.phone.aura.bumper_enable";
    private static final String PROP_DONGLE_TYPE = "vendor.asus.dongletype";
    private static final String PROP_FAN_STATE = "persist.sys.asus.userfan";
    private static final String PROP_GAME_VICE_STATE = "vendor.asus.donglestate_GV_PD";
    private static final String PROP_NFC_MODE = "vendor.asus.nfc.mode";
    private static final int REQUEST_CODE_SCHEDULE_SYNC_FRAME = 1;
    private static final int REQUEST_CODE_STOP_NOTIFICATION = 3;
    private static final boolean[] SCENARIOS_ACTIVE_STATE_DEFAULT;
    private static final int[] SCENARIOS_ALLOW_WHEN_BUMPER_CONNECTED;
    private static final int[] SCENARIOS_ALLOW_WHEN_BUMPER_DISCONNECTED;
    private static final int SETTINGS_ANALYTICS_UPLOAD_INTERVAL;
    private static final int SETTINGS_ANALYTICS_UPLOAD_SHORT_INTERVAL = 300000;
    private static final String SETTINGS_SYSTEM_BUMPER_CONNECTED_EFFECT = "bumper_connected_effect";
    private static final int SYNC_DELAY = 8940;
    private static final int SYNC_DELAY_FIRST_TIME = 1000;
    private static final int SYNC_DELAY_WITH_DT_HEADSET = 1440;
    private static final String TAG = "AuraLightService";
    private static final String TAG_AURA_LIGHT_BLENDED = "blended";
    private static final String TAG_AURA_LIGHT_SCENARIO = "scenario";
    private static final String TAG_AURA_LIGHT_SETTING = "aura-light-setting";
    private static final String TAG_AURA_NOTIFICATION_CUSTOM = "custom";
    private static final String TAG_AURA_NOTIFICATION_SETTING = "aura-notification-setting";
    private static final String TAG_BUMPER_SETTINGS = "bumper";
    private static final int TGPA_CMD_QUERY_ALL = 0;
    private static final int TGPA_CMD_QUERY_SPECIFIC = 1;
    private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";
    private static final int UPDATE_DONGLE_TYPE_DELAY = 10000;
    private static final int VERSION_MR2 = 2;
    private static final int WRITE_DELAY = 10000;
    private ActivityManagerInternal mActivityManagerInternal;
    private Set<Integer> mAttachedHeadsetPids;
    private AtomicFile mAuraLightFile;
    private AtomicFile mNotificationFile;
    private BatteryManagerInternal mBatteryManagerInternal;
    private BlendedLightEffect mBlendedEffect;
    private InputManager mInputManager;
    private IAuraLightServiceInternal mIAuraLightServiceInternal;
    private LocalService mLocalService;
    private boolean mBootCompleted;
    private long mBootLightStartTime;
    private byte[] mBumperContent;
    private byte[] mBumperId;
    private int mBumperState;
    private CameraMonitor mCameraMonitor;
    private Context mContext;
    private int mChargingIndicatorPolicy;
    private int mColor;
    private Animator mCpuEffectAnimator;
    private BumperInfo mCurrentBumperInfo;
    private int mCurrentLedStates;
    private boolean mCustomEffectEnabled;
    private int mCustomLedStates;
    private long[] mDockDuration;
    private long mDockLedChangeTime;
    private long[] mDockLedOnDuration;
    private int mDockState;
    private DropBoxManager mDropBoxManager;
    private LightEffect[] mEffects;
    private boolean mEnabled;
    private String mFocusedApp;
    private boolean mFocusedAppIsGame;
    private boolean mFocusedAppSupportCustomLight;
    private Handler mHandler;
    private HeadsetLightController mHeadsetController;
    private boolean mHeadsetSyncable;
    boolean mInboxConnect;
    private boolean mIpLightEnabled;
    private boolean mIsCetraRGBConnected;
    private boolean mIsCharging;
    private boolean mIsGameViceConnected;
    private boolean mIsInboxAndBumperConnected;
    private boolean mIsStorageDeviceConnected;
    private boolean mIsUltraSavingMode;
    private boolean mKeyguardShowing;
    private int mLedStatesRecord;
    private boolean mLightRealOn;
    private boolean mLightSettingsChanged;
    private int mMode;
    private Map<String, LightEffect> mNotificationEffects;
    private int mNotificationExpirationTime;
    private boolean mNotificationSettingsChanged;
    private List<Message> mPendingTcSystemServiceCommand;
    private int mPhoneState;
    private PhoneStateListener mPhoneStateListener;
    private int mRate;
    private Map<Integer, LightEffect> mRestoreCetraEffect;
    private int mScenario;
    private long mScenarioEffectStartTime;
    private boolean mScreenOn;
    private Runnable mSettingsAnalyticsUploader;
    private SettingsObserver mSettingsObserver;
    private boolean mSupportBlendedEffect;
    private List<String> mSupportCustomLightApps;
    private Runnable mSupportCustomLightAppsChecker;
    private int mSyncDelay;
    private boolean mSystemEffectEnabled;
    private boolean mSystemEffectEnabledByUser;
    private IBinder mTcSystemService;
    private ServiceConnection mTcSystemServiceConnection;
    private UsbDeviceController mUsbDeviceController;
    private boolean mXModeOn;
    private Object mLock = new Object();
    private static String stringsku = SystemProperties.get("ro.vendor.build.asus.sku", "WW");
    private static boolean ISASUSCNSKU = "CN".equals(stringsku);;
    private final boolean[] mStatus = new boolean[16];

    static {
        int i;
        int i2;
        boolean z = SystemProperties.getBoolean("persist.sys.debug.analytics", false);
        DEBUG_ANALYTICS = z;
        boolean z2 = Build.DEVICE.startsWith("ASUS_I005") || Build.DEVICE.equals("ZS673KS");
        IS_ANAKIN = z2;
        boolean z3 = Build.DEVICE.startsWith("ASUS_I007") || Build.DEVICE.equals("ZS675KW");
        IS_PICASSO = z3;
        HAS_2ND_DISPLAY = "1".equals(SystemProperties.get("ro.boot.id.bc"));
        if (z) {
            i = 300000;
        } else {
            i = 3600000;
        }
        SETTINGS_ANALYTICS_UPLOAD_INTERVAL = i;
        PACKAGE_NAME_TGPA = z2 ? "com.tencent.inlab.solarcore" : "com.tencent.inlab.tcsystem";
        CLS_NAME_TGPA_SERVICE = z2 ? "com.tencent.inlab.solarcore.tcsystem.TCSystemService" : "com.tencent.inlab.tcsystem.TCSystemService";
        DEFAULT_WHITE_COLOR = z2 ? 9699328 : 16777215;
        DEFAULT_RED_COLOR = z2 ? 9699328 : 16711680;
        DEFAULT_BLUE_COLOR = z2 ? 148 : 255;
        if (z2) {
            i2 = 65553;
        } else {
            i2 = 70451;
        }
        DEFAULT_LED_STATES = i2;
        ACTION_SCHEDULE_SYNC_FRAME = AuraLightService.class.getSimpleName() + ".SYNC";
        ACTION_STOP_NOTIFICATION = AuraLightService.class.getSimpleName() + ".STOP_NOTIFICATION";
        GAME_APPS_LAUNCH_EFFECT_DELAY = 500;
        GAME_OBIWAN_RATE = 0.6896067415730337d;
        GAME_APPS_LAUNCH_EFFECT = Arrays.asList(
            new AuraLightEffect(1, Color.argb(255, 2, 2, 2), 0,(int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 8, 8, 8), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 20, 20, 20), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 47, 47, 47), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 102, 102, 102), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 99, 90, 89), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 98, 75, 76), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 98, 61, 65), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 97, 48, 54), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 93, 36, 39), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 95, 23, 31), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 92, 14, 20), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 95, 4, 15), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 92, 0, 6), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 91, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 93, 0, 0), 0, (int) (870.0d * GAME_OBIWAN_RATE)),
            new AuraLightEffect(1, Color.argb(255, 82, 0, 1), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 62, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 44, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 27, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 9, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 0, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 34, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 70, 0, 1), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 75, 0, 6), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 82, 10, 17), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 96, 31, 37), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 120, 62, 68), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 171, (int) 136, 139), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 253, 253, 253), 0, (int) (90.0d * GAME_OBIWAN_RATE)),
            new AuraLightEffect(1, Color.argb(255, 235, (int) 210, 200), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 206, 139, 135), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 165, 52, 63), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 118, 0, 2), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 111, 0, 3), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 105, 0, 7), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 86, 0, 7), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 60, 2, 4), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 33, 4, 3), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 14, 6, 2), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 8, 8, 8), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 33, 19, 17), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 85, 40, 46), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 141, 92, 98), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 211, 189, 185), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 187, 139, 111), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 212, 162, 91), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 185, 83, 50), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 162, 24, 21), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 136, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 118, 0, 2), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 113, 0, 3), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 108, 0, 5), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 103, 0, 5), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 98, 2, 5), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 94, 2, 6), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 74, 3, 4), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 55, 5, 6), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 34, 2, 2), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 17, 6, 3), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 8, 8, 8), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 52, 17, 20), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 104, 39, 47), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 141, 92, 98), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 211, 189, 185), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 185, 141, 109), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 212, 162, 91), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 185, 83, 50), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 159, 23, 20), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 136, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 116, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 110, 0, 4), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 103, 0, 6), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 96, 0, 5), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 89, 0, 5), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 75, 0, 2), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 55, 0, 1), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 34, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 23, 0, 3), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 16, 6, 7), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 21, 21, 21), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 72, 42, 43), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 138, 94, 98), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 211, 189, 185), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 187, 139, 111), 0,(int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 212, 162, 91), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 185, 83, 50), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 159, 23, 20), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 136, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 116, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 110, 0, 2), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 103, 0, 4), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 96, 0, 4), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 89, 0, 5), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 82, 0, 3), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 65, 0, 1), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 47, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 34, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 21, 0, 1), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 10, 1, 1), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 8, 8, 8), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 52, 17, 20), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 104, 39, 47), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 141, 92, 98), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 199, (int) 175, 179), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 222, (int) 188, 134), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 207, 100, 60), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 202, 37, 30), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 198, 0, 1), 0, (int) (300.0d * GAME_OBIWAN_RATE)),
            new AuraLightEffect(1, Color.argb(255, 192, 0, 2), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 178, 0, 1), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, (int) 155, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 130, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 98, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 62, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 27, 0, 0), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 229, 229, 229), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 193, 193, 193), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 141, 141, 141), 0, (int) (GAME_OBIWAN_RATE * 30.0d)),
            new AuraLightEffect(1, Color.argb(255, 87, 87, 87), 0, (int) (GAME_OBIWAN_RATE * 60.0d)),
            new AuraLightEffect(1, Color.argb(255, 35, 35, 35), 0, (int) (GAME_OBIWAN_RATE * 30.0d)));

        POWER_CONNECTED_EFFECT = Arrays.asList(
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 1586962, 0, 30),
            new AuraLightEffect(1, 3174181, 0, 30),
            new AuraLightEffect(1, 4761400, 0, 30),
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 1586962, 0, 30),
            new AuraLightEffect(1, 3174181, 0, 30),
            new AuraLightEffect(1, 4761400, 0, 30),
            new AuraLightEffect(1, 6348619, 0, 1200),
            new AuraLightEffect(1, 6348619, 0, 30),
            new AuraLightEffect(1, 6084168, 0, 30),
            new AuraLightEffect(1, 5819717, 0, 30),
            new AuraLightEffect(1, 5555522, 0, 30),
            new AuraLightEffect(1, 5356607, 0, 30),
            new AuraLightEffect(1, 5092412, 0, 30),
            new AuraLightEffect(1, 4827961, 0, 30),
            new AuraLightEffect(1, 4629046, 0, 30),
            new AuraLightEffect(1, 4364851, 0, 30),
            new AuraLightEffect(1, 4100401, 0, 30),
            new AuraLightEffect(1, 3901742, 0, 30),
            new AuraLightEffect(1, 3637291, 0, 30),
            new AuraLightEffect(1, 3373096, 0, 30),
            new AuraLightEffect(1, 3174181, 0, 30),
            new AuraLightEffect(1, 2909730, 0, 30),
            new AuraLightEffect(1, 2645535, 0, 30),
            new AuraLightEffect(1, 2381084, 0, 30),
            new AuraLightEffect(1, 2182425, 0, 30),
            new AuraLightEffect(1, 1917975, 0, 30),
            new AuraLightEffect(1, 1653780, 0, 30),
            new AuraLightEffect(1, 1454865, 0, 30),
            new AuraLightEffect(1, 1190414, 0, 30),
            new AuraLightEffect(1, 926219, 0, 30),
            new AuraLightEffect(1, 727304, 0, 30),
            new AuraLightEffect(1, 463109, 0, 30),
            new AuraLightEffect(1, 198658, 0, 30));

        BUMPER_INSTALL_EFFECT_02 = Arrays.asList(
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 65794, 0, 30),
            new AuraLightEffect(1, 263690, 0, 30),
            new AuraLightEffect(1, 593686, 0, 30),
            new AuraLightEffect(1, 1055528, 0, 30),
            new AuraLightEffect(1, 1649214, 0, 30),
            new AuraLightEffect(1, 2375002, 0, 30),
            new AuraLightEffect(1, 3232634, 0, 30),
            new AuraLightEffect(1, 4222112, 0, 30),
            new AuraLightEffect(1, 5343690, 0, 30),
            new AuraLightEffect(1, 6597370, 0, 30),
            new AuraLightEffect(1, 6597370, 0, 30),
            new AuraLightEffect(1, 6003689, 0, 30),
            new AuraLightEffect(1, 5541338, 0, 30),
            new AuraLightEffect(1, 5013451, 0, 30),
            new AuraLightEffect(1, 4551356, 0, 30),
            new AuraLightEffect(1, 4089518, 0, 30),
            new AuraLightEffect(1, 3693217, 0, 30),
            new AuraLightEffect(1, 3297172, 0, 30),
            new AuraLightEffect(1, 2901383, 0, 30),
            new AuraLightEffect(1, 2505596, 0, 30),
            new AuraLightEffect(1, 2175344, 0, 30),
            new AuraLightEffect(1, 2175344, 0, 30),
            new AuraLightEffect(1, 2505596, 0, 30),
            new AuraLightEffect(1, 2901383, 0, 30),
            new AuraLightEffect(1, 3297172, 0, 30),
            new AuraLightEffect(1, 3693217, 0, 30),
            new AuraLightEffect(1, 4089518, 0, 30),
            new AuraLightEffect(1, 4551356, 0, 30),
            new AuraLightEffect(1, 5013451, 0, 30),
            new AuraLightEffect(1, 5541338, 0, 30),
            new AuraLightEffect(1, 6003689, 0, 30),
            new AuraLightEffect(1, 6597370, 0, 30),
            new AuraLightEffect(1, 6597370, 0, 30),
            new AuraLightEffect(1, 6003689, 0, 30),
            new AuraLightEffect(1, 5541338, 0, 30),
            new AuraLightEffect(1, 5013451, 0, 30),
            new AuraLightEffect(1, 4551356, 0, 30),
            new AuraLightEffect(1, 4089518, 0, 30),
            new AuraLightEffect(1, 3693217, 0, 30),
            new AuraLightEffect(1, 3297172, 0, 30),
            new AuraLightEffect(1, 2901383, 0, 30),
            new AuraLightEffect(1, 2505596, 0, 30),
            new AuraLightEffect(1, 2175344, 0, 30),
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 1385525, 0, 30),
            new AuraLightEffect(1, 2968688, 0, 30),
            new AuraLightEffect(1, 4684210, 0, 30),
            new AuraLightEffect(1, 6597370, 0, 30),
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 1385525, 0, 30),
            new AuraLightEffect(1, 2968688, 0, 30),
            new AuraLightEffect(1, 4684210, 0, 30),
            new AuraLightEffect(1, 6597370, 0, 30),
            new AuraLightEffect(1, 6605055, 0, 1200),
            new AuraLightEffect(1, 2068446, 0, 30),
            new AuraLightEffect(1, 1935315, 0, 30),
            new AuraLightEffect(1, 1802696, 0, 30),
            new AuraLightEffect(1, 1735357, 0, 30),
            new AuraLightEffect(1, 1668019, 0, 30),
            new AuraLightEffect(1, 1535401, 0, 30),
            new AuraLightEffect(1, 1468320, 0, 30),
            new AuraLightEffect(1, 1401239, 0, 30),
            new AuraLightEffect(1, 1268622, 0, 30),
            new AuraLightEffect(1, 1201541, 0, 30),
            new AuraLightEffect(1, 1134716, 0, 30),
            new AuraLightEffect(1, 1067892, 0, 30),
            new AuraLightEffect(1, 1001068, 0, 30),
            new AuraLightEffect(1, 934245, 0, 30),
            new AuraLightEffect(1, 867421, 0, 30),
            new AuraLightEffect(1, 800598, 0, 30),
            new AuraLightEffect(1, 734031, 0, 30),
            new AuraLightEffect(1, 667465, 0, 30),
            new AuraLightEffect(1, 600899, 0, 30),
            new AuraLightEffect(1, 534333, 0, 30),
            new AuraLightEffect(1, 467767, 0, 30),
            new AuraLightEffect(1, 401458, 0, 30),
            new AuraLightEffect(1, 400428, 0, 30),
            new AuraLightEffect(1, 334120, 0, 30),
            new AuraLightEffect(1, 267811, 0, 30),
            new AuraLightEffect(1, 267295, 0, 30),
            new AuraLightEffect(1, 200987, 0, 30),
            new AuraLightEffect(1, 200471, 0, 30),
            new AuraLightEffect(1, 134163, 0, 30),
            new AuraLightEffect(1, 133648, 0, 30),
            new AuraLightEffect(1, 67597, 0, 30),
            new AuraLightEffect(1, 67339, 0, 30),
            new AuraLightEffect(1, 66824, 0, 30),
            new AuraLightEffect(1, 1030, 0, 30),
            new AuraLightEffect(1, (int) 772, 0, 30),
            new AuraLightEffect(1, (int) 515, 0, 30),
            new AuraLightEffect(1, 258, 0, 30),
            new AuraLightEffect(1, 1, 0, 30),
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 0, 0, 30));

        BUMPER_INSTALL_EFFECT_03 = Arrays.asList(
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 131329, 0, 30),
            new AuraLightEffect(1, 656388, 0, 30),
            new AuraLightEffect(1, 1444105, 0, 30),
            new AuraLightEffect(1, 2625552, 0, 30),
            new AuraLightEffect(1, 4069657, 0, 30),
            new AuraLightEffect(1, 5907492, 0, 30),
            new AuraLightEffect(1, 8007985, 0, 30),
            new AuraLightEffect(1, 10502208, 0, 30),
            new AuraLightEffect(1, 13259089, 0, 30),
            new AuraLightEffect(1, 16409700, 0, 30),
            new AuraLightEffect(1, 16409700, 0, 30),
            new AuraLightEffect(1, 15293275, 0, 30),
            new AuraLightEffect(1, 14308436, 0, 30),
            new AuraLightEffect(1, 13323340, 0, 30),
            new AuraLightEffect(1, 12338501, 0, 30),
            new AuraLightEffect(1, 11419198, 0, 30),
            new AuraLightEffect(1, 10565688, 0, 30),
            new AuraLightEffect(1, 9712178, 0, 30),
            new AuraLightEffect(1, 8858668, 0, 30),
            new AuraLightEffect(1, 8136230, 0, 30),
            new AuraLightEffect(1, 7348513, 0, 30),
            new AuraLightEffect(1, 7348513, 0, 30),
            new AuraLightEffect(1, 8136230, 0, 30),
            new AuraLightEffect(1, 8858668, 0, 30),
            new AuraLightEffect(1, 9712178, 0, 30),
            new AuraLightEffect(1, 10565688, 0, 30),
            new AuraLightEffect(1, 11419198, 0, 30),
            new AuraLightEffect(1, 12338501, 0, 30),
            new AuraLightEffect(1, 13323340, 0, 30),
            new AuraLightEffect(1, 14308436, 0, 30),
            new AuraLightEffect(1, 15293275, 0, 30),
            new AuraLightEffect(1, 16409700, 0, 30),
            new AuraLightEffect(1, 16409700, 0, 30),
            new AuraLightEffect(1, 15293275, 0, 30),
            new AuraLightEffect(1, 14308436, 0, 30),
            new AuraLightEffect(1, 13323340, 0, 30),
            new AuraLightEffect(1, 12338501, 0, 30),
            new AuraLightEffect(1, 11419198, 0, 30),
            new AuraLightEffect(1, 10565688, 0, 30),
            new AuraLightEffect(1, 9712178, 0, 30),
            new AuraLightEffect(1, 8858668, 0, 30),
            new AuraLightEffect(1, 8136230, 0, 30),
            new AuraLightEffect(1, 7348513, 0, 30),
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 3478805, 0, 30),
            new AuraLightEffect(1, 7351597, 0, 30),
            new AuraLightEffect(1, 11683655, 0, 30),
            new AuraLightEffect(1, 16409700, 0, 30),
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 3478805, 0, 30),
            new AuraLightEffect(1, 7351597, 0, 30),
            new AuraLightEffect(1, 11683655, 0, 30),
            new AuraLightEffect(1, 16409700, 0, 30),
            new AuraLightEffect(1, 16737380, 0, 1200),
            new AuraLightEffect(1, 14556959, 0, 30),
            new AuraLightEffect(1, 13835549, 0, 30),
            new AuraLightEffect(1, 13114139, 0, 30),
            new AuraLightEffect(1, 12392986, 0, 30),
            new AuraLightEffect(1, 11737369, 0, 30),
            new AuraLightEffect(1, 11081495, 0, 30),
            new AuraLightEffect(1, 10491414, 0, 30),
            new AuraLightEffect(1, 9901333, 0, 30),
            new AuraLightEffect(1, 9310995, 0, 30),
            new AuraLightEffect(1, 8720914, 0, 30),
            new AuraLightEffect(1, 8130833, 0, 30),
            new AuraLightEffect(1, 7606288, 0, 30),
            new AuraLightEffect(1, 7081743, 0, 30),
            new AuraLightEffect(1, 6622734, 0, 30),
            new AuraLightEffect(1, 6098189, 0, 30),
            new AuraLightEffect(1, 5639180, 0, 30),
            new AuraLightEffect(1, 5180171, 0, 30),
            new AuraLightEffect(1, 4786698, 0, 30),
            new AuraLightEffect(1, 4393225, 0, 30),
            new AuraLightEffect(1, 3999752, 0, 30),
            new AuraLightEffect(1, 3606279, 0, 30),
            new AuraLightEffect(1, 3278342, 0, 30),
            new AuraLightEffect(1, 2885126, 0, 30),
            new AuraLightEffect(1, 2622725, 0, 30),
            new AuraLightEffect(1, 2294788, 0, 30),
            new AuraLightEffect(1, 2032644, 0, 30),
            new AuraLightEffect(1, 1770243, 0, 30),
            new AuraLightEffect(1, 1508099, 0, 30),
            new AuraLightEffect(1, 1245698, 0, 30),
            new AuraLightEffect(1, 1049090, 0, 30),
            new AuraLightEffect(1, 852225, 0, 30),
            new AuraLightEffect(1, 721153, 0, 30),
            new AuraLightEffect(1, 524545, 0, 30),
            new AuraLightEffect(1, 393216, 0, 30),
            new AuraLightEffect(1, 262144, 0, 30),
            new AuraLightEffect(1, 196608, 0, 30),
            new AuraLightEffect(1, 131072, 0, 30),
            new AuraLightEffect(1, 65536, 0, 30),
            new AuraLightEffect(1, 0, 0, 30),
            new AuraLightEffect(1, 0, 0, 30));

        SCENARIOS_ALLOW_WHEN_BUMPER_DISCONNECTED = new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15};
        SCENARIOS_ALLOW_WHEN_BUMPER_CONNECTED = new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        SCENARIOS_ACTIVE_STATE_DEFAULT = new boolean[]{false, false, !z3, false, !z3, false, !z3, false, false, false, false, !z3, !z3, false, !z3, !z3};
    }

    private final class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String pkg;
            boolean success;
            boolean z = false;
            switch (msg.what) {
                case 0:
                    sendBroadcast(new Intent("asus.intent.action.AURA_LIGHT_CHANGED"), "com.asus.permission.MANAGE_AURA_LIGHT");
                    return;
                case 1:
                    Intent intent = new Intent("asus.intent.action.AURA_SETTING_CHANGED");
                    intent.addCategory(String.valueOf(msg.arg1));
                    sendBroadcast(intent, "com.asus.permission.MANAGE_AURA_LIGHT");
                    return;
                case 2:
                    synchronized (mLock) {
                        AuraLightService auraLightService = AuraLightService.this;
                        auraLightService.setLightLocked(auraLightService.mScenario, mColor, mMode, mRate, mBlendedEffect);
                    }
                    return;
                case 3:
                    synchronized (mLock) {
                        setScenarioStatusLocked(7, false);
                    }
                    return;
                case 4:
                    writeSettings();
                    return;
                case 5:
                    synchronized (mLock) {
                        AuraLightService auraLightService2 = AuraLightService.this;
                        auraLightService2.setScenarioStatusLocked(9, auraLightService2.mScreenOn);
                        AuraLightService auraLightService3 = AuraLightService.this;
                        auraLightService3.setScenarioStatusLocked(10, !auraLightService3.mScreenOn);
                        AuraLightService auraLightService4 = AuraLightService.this;
                        auraLightService4.setScenarioStatusLocked(6, auraLightService4.mScreenOn && mXModeOn);
                        setScenarioStatusLocked(1, false);
                        setScenarioEffectLocked(1, false, -1, 0, 0);
                        if (mCpuEffectAnimator != null) {
                            mCpuEffectAnimator.cancel();
                            mCpuEffectAnimator = null;
                        }
                    }
                    return;
                case 6:
                    if (mCustomEffectEnabled && (msg.obj instanceof List)) {
                        List<AuraLightEffect> effects = (List) msg.obj;
                        int totalTime = 0;
                        for (AuraLightEffect effect : effects) {
                            int duration = effect.getDuration();
                            if (totalTime + duration > 60000) {
                                mHandler.sendEmptyMessageDelayed(7, totalTime);
                                return;
                            }
                            Message applyMsg = mHandler.obtainMessage(7, effect);
                            applyMsg.arg1 = msg.arg1;
                            mHandler.sendMessageDelayed(applyMsg, totalTime);
                            totalTime += duration;
                        }
                        mHandler.sendEmptyMessageDelayed(7, totalTime);
                        return;
                    }
                    return;
                case 7:
                    synchronized (mLock) {
                        if (mCpuEffectAnimator != null) {
                            mCpuEffectAnimator.cancel();
                            mCpuEffectAnimator = null;
                        }
                        if (!(msg.obj instanceof AuraLightEffect)) {
                            setScenarioStatusLocked(1, false);
                            setScenarioEffectLocked(1, false, -1, 0, 0);
                            mCustomLedStates = 0;
                            return;
                        }
                        mCustomLedStates = msg.arg1;
                        AuraLightEffect effect2 = (AuraLightEffect) msg.obj;
                        int type = effect2.getType();
                        int rate = effect2.getRate();
                        setScenarioStatusLocked(1, true);
                        if (rate < -2 && (type == 2 || type == 3)) {
                            setScenarioEffectLocked(1, true, effect2.getColor(), 0, rate);
                            AuraLightService auraLightService5 = AuraLightService.this;
                            auraLightService5.mCpuEffectAnimator = auraLightService5.getCpuEffectAnimator(1, type, effect2.getColor(), rate, 0);
                            if (mCpuEffectAnimator != null) {
                                mCpuEffectAnimator.start();
                            }
                            return;
                        }
                        setScenarioEffectLocked(1, true, effect2.getColor(), type, rate);
                        return;
                    }
                case 8:
                    if ((msg.obj instanceof String) && (pkg = (String) msg.obj) != null && !pkg.equals(mFocusedApp)) {
                        handleFocusedAppChanged(pkg);
                        return;
                    }
                    return;
                case 9:
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    boolean lidOpen = z;
                    synchronized (mLock) {
                        handleLidSwitchChangedLocked(lidOpen);
                    }
                    return;
                case 10:
                    synchronized (mLock) {
                        handleNfcTagDiscoveredLocked();
                    }
                    return;
                case 11:
                    if (!AuraLightService.IS_ANAKIN && !AuraLightService.IS_PICASSO && mBumperState == 0) {
                        boolean prevDisable = mHandler.hasMessages(12);
                        mHandler.removeMessages(12);
                        NfcAdapter adapter = null;
                        try {
                            adapter = NfcAdapter.getNfcAdapter(mContext);
                        } catch (Exception e) {
                            Slog.w(AuraLightService.TAG, "Get NfcAdapter failed when turning on NFC, err: " + e.getMessage());
                        }
                        if (adapter != null) {
                            boolean isEnable = adapter.isEnabled();
                            if (!isEnable) {
                                adapter.enable();
                                prevDisable = true;
                            }
                            if (prevDisable) {
                                mHandler.sendEmptyMessageDelayed(12, 5000L);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    return;
                case 12:
                    NfcAdapter adapter2 = null;
                    try {
                        adapter2 = NfcAdapter.getNfcAdapter(mContext);
                    } catch (Exception e2) {
                        Slog.w(AuraLightService.TAG, "Get NfcAdapter failed when turning off NFC, err: " + e2.getMessage());
                    }
                    if (adapter2 != null) {
                        adapter2.disable();
                        return;
                    }
                    return;
                case 13:
                default:
                    return;
                case 14:
                    if (AuraLightService.DEBUG_ANALYTICS) {
                        Slog.d(AuraLightService.TAG, "handleMessage: MSG_SET_SYS_CUSTOM_EFFECT");
                    }
                    synchronized (mLock) {
                        if (mCpuEffectAnimator != null) {
                            mCpuEffectAnimator.cancel();
                            mCpuEffectAnimator = null;
                        }
                    }
                    if (!(msg.obj instanceof List)) {
                        if (AuraLightService.DEBUG_ANALYTICS) {
                            Slog.d(AuraLightService.TAG, "MSG_SET_SYS_CUSTOM_EFFECT: Turn off the light");
                        }
                        setScenarioEffectLocked(1, false, -1, 0, 0);
                        return;
                    }
                    final int scenario = msg.arg1;
                    if (AuraLightService.DEBUG_ANALYTICS) {
                        Slog.d(AuraLightService.TAG, "MSG_SET_SYS_CUSTOM_EFFECT: scenario=" + scenario);
                    }
                    List<AuraLightEffect> effects2 = (List) msg.obj;
                    List<Animator> animators = new ArrayList<>();
                    for (AuraLightEffect effect3 : effects2) {
                        Animator anim = getCpuEffectAnimator(scenario, effect3.getType(), effect3.getColor(), effect3.getRate(), effect3.getDuration());
                        if (anim != null) {
                            animators.add(anim);
                        }
                    }
                    synchronized (mLock) {
                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playSequentially(animators);
                        animatorSet.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationCancel(Animator animation) {
                                synchronized (mLock) {
                                    if (AuraLightService.DEBUG_ANALYTICS) {
                                        Slog.d(AuraLightService.TAG, "MSG_SET_SYS_CUSTOM_EFFECT: onAnimationCancel()");
                                    }
                                    setScenarioStatusLocked(scenario, false);
                                }
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                synchronized (mLock) {
                                    if (AuraLightService.DEBUG_ANALYTICS) {
                                        Slog.d(AuraLightService.TAG, "MSG_SET_SYS_CUSTOM_EFFECT: onAnimationEnd()");
                                    }
                                    setScenarioStatusLocked(scenario, false);
                                    setScenarioEffectLocked(1, false, -1, 0, 0);
                                }
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                                synchronized (mLock) {
                                    if (AuraLightService.DEBUG_ANALYTICS) {
                                        Slog.d(AuraLightService.TAG, "MSG_SET_SYS_CUSTOM_EFFECT: onAnimationStart()");
                                    }
                                    setScenarioStatusLocked(scenario, true);
                                }
                            }
                        });
                        animatorSet.start();
                        mCpuEffectAnimator = animatorSet;
                        mHandler.sendEmptyMessageDelayed(14, animatorSet.getTotalDuration());
                    }
                    return;
                case 15:
                    List<StatusBarNotification> sbns = (List) msg.obj;
                    synchronized (mLock) {
                        handleUpdateNotificationLightLocked(sbns);
                    }
                    return;
                case 16:
                    if ((msg.obj instanceof String) && mDropBoxManager != null) {
                        String data = (String) msg.obj;
                        String encryptData = Encryption.encrypt(data);
                        if (encryptData != null && mDropBoxManager != null) {
                            mDropBoxManager.addText(AuraLightService.DROPBOX_TAG_SYSTEM_LIGHT_EVENT, encryptData);
                            return;
                        }
                        return;
                    }
                    return;
                case 17:
                    if ((msg.obj instanceof String) && mDropBoxManager != null) {
                        String data2 = (String) msg.obj;
                        String encryptData2 = Encryption.encrypt(data2);
                        if (encryptData2 != null && mDropBoxManager != null) {
                            mDropBoxManager.addText(AuraLightService.DROPBOX_TAG_CUSTOM_LIGHT_EVENT, encryptData2);
                            return;
                        }
                        return;
                    }
                    return;
                case 18:
                    int retryCount = msg.arg1;
                    if (retryCount < 5) {
                        Intent intent2 = new Intent(AuraLightService.ACTION_BIND_TGPA_SERVICE);
                        intent2.setComponent(new ComponentName(AuraLightService.PACKAGE_NAME_TGPA, AuraLightService.CLS_NAME_TGPA_SERVICE));
                        intent2.setPackage(AuraLightService.PACKAGE_NAME_TGPA);
                        boolean success2 = mContext.bindServiceAsUser(intent2, mTcSystemServiceConnection, 1, UserHandle.OWNER);
                        if (!success2) {
                            Slog.w(AuraLightService.TAG, "BindService failed, retryCount=" + retryCount);
                            Message bindAgain = Message.obtain(mHandler, 18);
                            bindAgain.arg1 = retryCount + 1;
                            mHandler.sendMessageDelayed(bindAgain, 5000L);
                            return;
                        }
                        return;
                    }
                    return;
                case 19:
                    int cmd = msg.arg1;
                    if (cmd == 0) {
                        boolean success3 = handleCollectSupportCustomLightApps();
                        success = success3;
                    } else if (cmd != 1) {
                        success = true;
                    } else {
                        String pkgName = (String) msg.obj;
                        boolean success4 = handleIdentifySupportCustomLightApp(pkgName);
                        success = success4;
                    }
                    if (success) {
                        if (!mHandler.hasMessages(19) && mTcSystemService != null) {
                            mContext.unbindService(mTcSystemServiceConnection);
                            mTcSystemService = null;
                            return;
                        }
                        return;
                    }
                    Slog.w(AuraLightService.TAG, "Execute cmd failed. TcSystemService may not alive.");
                    synchronized (mPendingTcSystemServiceCommand) {
                        mPendingTcSystemServiceCommand.add(msg);
                    }
                    mHandler.removeMessages(18);
                    mHandler.sendEmptyMessage(18);
                    return;
                case 20:
                    int state = msg.arg1;
                    if (state == 1) {
                        mPhoneState = 1;
                    } else if (state == 2) {
                        mPhoneState = 2;
                    } else {
                        mPhoneState = 0;
                    }
                    AuraLightService auraLightService6 = AuraLightService.this;
                    auraLightService6.setScenarioStatusInternal(4, auraLightService6.mPhoneState == 1);
                    AuraLightService auraLightService7 = AuraLightService.this;
                    if (auraLightService7.mPhoneState == 2) {
                        z = true;
                    }
                    auraLightService7.setScenarioStatusInternal(5, z);
                    return;
                case 21:
                    syncFrame();
                    return;
                case 22:
                    int dongleType = SystemProperties.getInt(AuraLightService.PROP_DONGLE_TYPE, 0);
                    if (dongleType == 0 && mDockState != 0) {
                        mDockState = 0;
                        updateLedState();
                        return;
                    }
                    return;
                case 23:
                    if (msg.obj instanceof AsusAnalytics) {
                        AsusAnalytics analytics = (AsusAnalytics) msg.obj;
                        int scenario2 = analytics.scenario;
                        long timeStamp = analytics.timeStamp;
                        if (scenario2 != -1) {
                            z = true;
                        }
                        boolean realOn = z;
                        if (mDropBoxManager != null && mLightRealOn != realOn) {
                            DropBoxManager dropBoxManager = mDropBoxManager;
                            StringBuilder sb = new StringBuilder();
                            sb.append(realOn ? "+" : "-");
                            sb.append(",");
                            sb.append(timeStamp);
                            dropBoxManager.addText(AuraLightService.DROPBOX_TAG_REAL_LIGHT_ON, sb.toString());
                        }
                        mLightRealOn = realOn;
                        AuraLightService auraLightService8 = AuraLightService.this;
                        auraLightService8.sendLightBroadcast(auraLightService8.mContext, realOn);
                        return;
                    }
                    return;
                case 24:
                    if (msg.obj instanceof AsusAnalytics) {
                        AsusAnalytics analytics2 = (AsusAnalytics) msg.obj;
                        int scenario3 = analytics2.scenario;
                        long timeStamp2 = analytics2.timeStamp;
                        if (scenario3 != -1) {
                            z = true;
                        }
                        boolean realOn2 = z;
                        if (mDropBoxManager != null) {
                            DropBoxManager dropBoxManager2 = mDropBoxManager;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(realOn2 ? "+" : "-");
                            sb2.append(",");
                            sb2.append(timeStamp2);
                            dropBoxManager2.addText(AuraLightService.DROPBOX_TAG_REAL_LIGHT_ON, sb2.toString());
                            return;
                        }
                        return;
                    }
                    return;
                case 25:
                    if (msg.obj instanceof InboxAnalytics) {
                        InboxAnalytics analytics3 = (InboxAnalytics) msg.obj;
                        boolean connect = analytics3.connect;
                        String fanState = analytics3.fanState;
                        long timeStamp3 = analytics3.timeStamp;
                        if (mDropBoxManager != null && mInboxConnect != connect) {
                            DropBoxManager dropBoxManager3 = mDropBoxManager;
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append(connect ? "+" : "-");
                            sb3.append(",");
                            sb3.append(fanState);
                            sb3.append(",");
                            sb3.append(timeStamp3);
                            dropBoxManager3.addText(AuraLightService.DROPBOX_TAG_INBOX_CONNECT, sb3.toString());
                        }
                        mInboxConnect = connect;
                        AuraLightService auraLightService9 = AuraLightService.this;
                        auraLightService9.sendInboxBroadcast(auraLightService9.mContext, connect);
                        return;
                    }
                    return;
                case 26:
                    if (msg.obj instanceof InboxAnalytics) {
                        InboxAnalytics analytics4 = (InboxAnalytics) msg.obj;
                        boolean connect2 = analytics4.connect;
                        String fanState2 = analytics4.fanState;
                        long timeStamp4 = analytics4.timeStamp;
                        if (mDropBoxManager != null) {
                            DropBoxManager dropBoxManager4 = mDropBoxManager;
                            StringBuilder sb4 = new StringBuilder();
                            sb4.append(connect2 ? "+" : "-");
                            sb4.append(",");
                            sb4.append(fanState2);
                            sb4.append(",");
                            sb4.append(timeStamp4);
                            dropBoxManager4.addText(AuraLightService.DROPBOX_TAG_INBOX_CONNECT, sb4.toString());
                            return;
                        }
                        return;
                    }
                    return;
                case 27:
                    long timeStamp5 = System.currentTimeMillis();
                    if (mScenario != -1) {
                        z = true;
                    }
                    boolean realOn3 = z;
                    if (mDropBoxManager != null) {
                        DropBoxManager dropBoxManager5 = mDropBoxManager;
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append(realOn3 ? "+" : "-");
                        sb5.append(",");
                        sb5.append(timeStamp5);
                        dropBoxManager5.addText(AuraLightService.DROPBOX_TAG_REAL_LIGHT_ON, sb5.toString());
                    }
                    boolean connect3 = mInboxConnect;
                    String fanState3 = SystemProperties.get(AuraLightService.PROP_FAN_STATE, "0");
                    if (mDropBoxManager != null) {
                        DropBoxManager dropBoxManager6 = mDropBoxManager;
                        StringBuilder sb6 = new StringBuilder();
                        sb6.append(connect3 ? "+" : "-");
                        sb6.append(",");
                        sb6.append(fanState3);
                        sb6.append(",");
                        sb6.append(timeStamp5);
                        dropBoxManager6.addText(AuraLightService.DROPBOX_TAG_INBOX_CONNECT, sb6.toString());
                    }
                    Message msg2 = new Message();
                    msg2.what = 27;
                    mHandler.removeMessages(27);
                    mHandler.sendMessageDelayed(msg2, 86400000L);
                    AuraLightService auraLightService10 = AuraLightService.this;
                    auraLightService10.sendLightBroadcast(auraLightService10.mContext, realOn3);
                    AuraLightService auraLightService11 = AuraLightService.this;
                    auraLightService11.sendInboxBroadcast(auraLightService11.mContext, connect3);
                    return;
            }
        }
    }

    private void sendLightBroadcast(Context context, boolean realOn) {
        Intent lightIntent = new Intent();
        lightIntent.setAction("asus.intent.action.MSG_AURA_LIGHT_CHANGE");
        lightIntent.putExtra("light_status", realOn);
        context.sendStickyBroadcastAsUser(lightIntent, UserHandle.SYSTEM);
    }

    private void sendInboxBroadcast(Context context, boolean connect) {
        Intent InboxIntent = new Intent();
        InboxIntent.setAction("asus.intent.action.MSG_AURA_INBOX_CHANGE");
        InboxIntent.putExtra("connection_status", connect);
        context.sendStickyBroadcastAsUser(InboxIntent, UserHandle.SYSTEM);
    }

    private static class BumperInfo {
        String bumperId;
        String characterId;
        String gameId;
        String lightId;
        String themeId;
        String uid;
        String vendorId;

        private BumperInfo() {
        }
    }

    private class CameraMonitor {
        private CameraManager.AvailabilityCallback mAvailabilityCallback;
        private boolean mIsCameraUsing;
        private boolean mIsFlashLightUsing;
        private CameraManager.TorchCallback mTorchCallback;

        private CameraMonitor() {
            this.mIsCameraUsing = false;
            this.mIsFlashLightUsing = false;
            this.mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
                @Override
                public void onCameraAvailable(String cameraId) {
                    if (AuraLightService.DEBUG_ANALYTICS) {
                        Slog.d(AuraLightService.TAG, "onCameraAvailable: cameraId=" + cameraId);
                    }
                    if (!CameraMonitor.this.isBackCamera(cameraId)) {
                        if (AuraLightService.DEBUG_ANALYTICS) {
                            Slog.d(AuraLightService.TAG, "onCameraAvailable: !isBackCamera");
                            return;
                        }
                        return;
                    }
                    CameraMonitor.this.mIsCameraUsing = false;
                    CameraMonitor.this.updateIpLightState();
                }

                @Override
                public void onCameraUnavailable(String cameraId) {
                    if (AuraLightService.DEBUG_ANALYTICS) {
                        Slog.d(AuraLightService.TAG, "onCameraUnavailable: cameraId=" + cameraId);
                    }
                    if (!CameraMonitor.this.isBackCamera(cameraId)) {
                        if (AuraLightService.DEBUG_ANALYTICS) {
                            Slog.d(AuraLightService.TAG, "onCameraUnavailable: !isBackCamera");
                            return;
                        }
                        return;
                    }
                    CameraMonitor.this.mIsCameraUsing = true;
                    CameraMonitor.this.updateIpLightState();
                    CameraMonitor.this.notifyTurnOffIpLight();
                }
            };
            this.mTorchCallback = new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    if (AuraLightService.DEBUG_ANALYTICS) {
                        Slog.d(AuraLightService.TAG, "onTorchModeChanged: cameraId=" + cameraId + ", enabled=" + enabled);
                    }
                    CameraMonitor.this.mIsFlashLightUsing = enabled;
                    CameraMonitor.this.updateIpLightState();
                    if (enabled) {
                        CameraMonitor.this.notifyTurnOffIpLight();
                    }
                }
            };
        }

        public boolean canUseIpLight() {
            return !this.mIsFlashLightUsing && !this.mIsCameraUsing;
        }

        private boolean isBackCamera(String cameraId) {
            CameraManager cameraManager = (CameraManager) mContext.getSystemService("camera");
            try {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = (Integer) chars.get(CameraCharacteristics.LENS_FACING);
                return new Integer(1).equals(facing);
            } catch (Exception e) {
                Slog.w(AuraLightService.TAG, "Check the lens facing of " + cameraId + " failed, err: " + e.getMessage());
                return true;
            }
        }

        private void updateIpLightState() {
            if (mBumperState != 1) {
                return;
            }
            if (canUseIpLight()) {
                enableIpLight(true);
            } else {
                enableIpLight(false);
            }
        }

        private void notifyTurnOffIpLight() {
            if (mMode == 0 || mBumperState != 1) {
                return;
            }
            Toast.makeText(mContext, 17039721, 0).show();
        }

        public void startMonitor() {
            CameraManager cameraManager = (CameraManager) mContext.getSystemService("camera");
            cameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, mHandler);
            cameraManager.registerTorchCallback(this.mTorchCallback, mHandler);
        }

        public void stopMonitor() {
            CameraManager cameraManager = (CameraManager) mContext.getSystemService("camera");
            cameraManager.unregisterAvailabilityCallback(this.mAvailabilityCallback);
            cameraManager.unregisterTorchCallback(this.mTorchCallback);
        }
    }

    private static class LightEffect {
        boolean active;
        BlendedLightEffect blendedEffect;
        int color;
        long effectStartTime;
        long effectTotalTime;
        int mode;
        int rate;
        int stateChangeCount;

        LightEffect() {
        }

        LightEffect(boolean active, int color, int mode, int rate) {
            this.active = active;
            this.color = color;
            this.mode = mode;
            this.rate = rate;
            if (active) {
                this.effectStartTime = System.currentTimeMillis();
            }
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append("LightEffect { active=" + this.active);
            sb.append(", color=0x" + Integer.toHexString(this.color));
            sb.append(", mode=" + AuraLightManager.modeToString(this.mode));
            sb.append(", rate=" + AuraLightManager.rateToString(this.rate));
            if (ISASUSCNSKU && !AuraLightService.IS_PICASSO) {
                sb.append(", stateChangeCount=" + this.stateChangeCount);
                sb.append(", effectStartTime=" + this.effectStartTime);
                sb.append(", effectTotalTime=" + this.effectTotalTime);
            }
            if (this.blendedEffect != null) {
                str = ", blendedEffect=" + this.blendedEffect;
            } else {
                str = "";
            }
            sb.append(str);
            sb.append("}");
            return sb.toString();
        }
    }

    private static class BlendedLightEffect {
        final int[] colors;
        int mode;
        int rate;

        BlendedLightEffect() {
            this.colors = new int[6];
        }

        BlendedLightEffect(int mode, int rate, int... colors) {
            this.mode = mode;
            this.rate = rate;
            this.colors = Arrays.copyOfRange(colors, 0, 6);
        }

        public String toString() {
            return "BlendedLightEffect { mode=" + AuraLightManager.modeToString(this.mode) + ", rate=" + AuraLightManager.rateToString(this.rate) + ", " + colorsToString() + "}";
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof BlendedLightEffect)) {
                return false;
            }
            BlendedLightEffect other = (BlendedLightEffect) obj;
            return this.mode == other.mode && this.rate == other.rate && Arrays.equals(this.colors, other.colors);
        }

        private String colorsToString() {
            int[] iArr;
            StringBuilder sb = new StringBuilder();
            sb.append("colors=[");
            for (int color : this.colors) {
                sb.append(" 0x" + Integer.toHexString(color) + " ");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private static class AsusAnalytics {
        int scenario;
        long timeStamp;

        AsusAnalytics(int scenario, long timeStamp) {
            this.scenario = scenario;
            this.timeStamp = timeStamp;
        }
    }

    private static class InboxAnalytics {
        boolean connect;
        String fanState;
        long timeStamp;

        InboxAnalytics(boolean connect, String fanState, long timeStamp) {
            this.connect = connect;
            this.fanState = fanState;
            this.timeStamp = timeStamp;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        static final String SETTINGS_GLOBAL_GAME_MODE_ENABLED = "asus_gamemode";
        static final String SETTINGS_SYSTEM_CHARGING_INDICATOR_POLICY = "charging_indicator_policy";
        static final String SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED = "enable_tgpa_aura_effect";
        static final String SETTINGS_SYSTEM_HEADSET_SYNCABLE = "headset_syncable";
        static final String SETTINGS_SYSTEM_LED_STATES = "led_states";
        static final String SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME = "notification_expiration_time";
        static final String SETTINGS_SYSTEM_STORAGE_SYNCABLE = "storage_syncable";
        static final String SETTINGS_SYSTEM_SYSTEM_EFFECT_ENABLED = "enable_system_effect";
        private final Uri mChargingIndicatorPolicyUri;
        String mCustomEffectAllowApp;
        private final Uri mCustomEffectEnabledUri;
        private final Uri mGameModeEnabledUri;
        private final Uri mHeadsetSyncableUri;
        private final Uri mLedStatesUri;
        private final Uri mNotificationExpirationTimeUri;
        private final Uri mSystemEffectEnabledUri;
        int mSystemEffectOnCount = 0;
        int mSystemEffectOffCount = 0;
        long mSystemEffectStartTime = 0;
        long mSystemEffectTotalTime = 0;
        int mCustomEffectOnCount = 0;
        int mCustomEffectOffCount = 0;
        long mCustomEffectStartTime = 0;
        long mCustomEffectTotalTime = 0;
        long mLastUploadTime = System.currentTimeMillis();

        public SettingsObserver() {
            super(new Handler());
            Uri uriFor = Settings.Global.getUriFor(SETTINGS_GLOBAL_GAME_MODE_ENABLED);
            this.mGameModeEnabledUri = uriFor;
            Uri uriFor2 = Settings.System.getUriFor(SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED);
            mCustomEffectEnabledUri = uriFor2;
            Uri uriFor3 = Settings.System.getUriFor(SETTINGS_SYSTEM_SYSTEM_EFFECT_ENABLED);
            mSystemEffectEnabledUri = uriFor3;
            Uri uriFor4 = Settings.System.getUriFor(SETTINGS_SYSTEM_CHARGING_INDICATOR_POLICY);
            mChargingIndicatorPolicyUri = uriFor4;
            Uri uriFor5 = Settings.System.getUriFor(SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME);
            this.mNotificationExpirationTimeUri = uriFor5;
            Uri uriFor6 = Settings.System.getUriFor(SETTINGS_SYSTEM_LED_STATES);
            this.mLedStatesUri = uriFor6;
            Uri uriFor7 = Settings.System.getUriFor(SETTINGS_SYSTEM_HEADSET_SYNCABLE);
            mHeadsetSyncableUri = uriFor7;
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(uriFor, false, this, -1);
            resolver.registerContentObserver(uriFor2, false, this, -1);
            resolver.registerContentObserver(uriFor3, false, this, -1);
            resolver.registerContentObserver(uriFor4, false, this, -1);
            resolver.registerContentObserver(uriFor5, false, this, -1);
            resolver.registerContentObserver(uriFor6, false, this, -1);
            resolver.registerContentObserver(uriFor7, false, this, -1);
        }

        public void init() {
            ContentResolver resolver = mContext.getContentResolver();
            boolean z = false;
            mXModeOn = Settings.Global.getInt(resolver, SETTINGS_GLOBAL_GAME_MODE_ENABLED, 0) == 1;
            mCustomEffectEnabled = Settings.System.getInt(resolver, SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED, 1) == 1;
            mSystemEffectEnabled = Settings.System.getInt(resolver, SETTINGS_SYSTEM_SYSTEM_EFFECT_ENABLED, 1) == 1;
            AuraLightService auraLightService = AuraLightService.this;
            auraLightService.mSystemEffectEnabledByUser = auraLightService.mSystemEffectEnabled;
            mChargingIndicatorPolicy = Settings.System.getInt(resolver, SETTINGS_SYSTEM_CHARGING_INDICATOR_POLICY, 1);
            mNotificationExpirationTime = Settings.System.getInt(resolver, SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME, AuraLightService.DEFAULT_NOTIFICATION_EXPIRATION_TIME);
            AuraLightService auraLightService2 = AuraLightService.this;
            if (Settings.System.getInt(resolver, SETTINGS_SYSTEM_HEADSET_SYNCABLE, 0) == 1) {
                z = true;
            }
            auraLightService2.mHeadsetSyncable = z;
            transferLagencyLedStatesIfNeeded();
            mLedStatesRecord = Settings.System.getInt(resolver, SETTINGS_SYSTEM_LED_STATES, AuraLightService.DEFAULT_LED_STATES);
            updateSyncDelay();
            updateLedState();
            if (ISASUSCNSKU && !AuraLightService.IS_PICASSO) {
                if (mCustomEffectEnabled) {
                    mCustomEffectStartTime = System.currentTimeMillis();
                }
                if (mSystemEffectEnabled) {
                    mSystemEffectStartTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            AuraLightService auraLightService;
            if (uri == null) {
                return;
            }
            boolean z = false;
            if (this.mGameModeEnabledUri.equals(uri)) {
                AuraLightService auraLightService2 = AuraLightService.this;
                if (Settings.Global.getInt(auraLightService2.mContext.getContentResolver(), SETTINGS_GLOBAL_GAME_MODE_ENABLED, 0) == 1) {
                    z = true;
                }
                auraLightService2.mXModeOn = z;
                updateSuspensionRelatedScenarioStatus();
            } else if (mCustomEffectEnabledUri.equals(uri)) {
                if (Settings.System.getInt(mContext.getContentResolver(), SETTINGS_SYSTEM_CUSTOM_EFFECT_ENABLED, 1) == 1) {
                    z = true;
                }
                boolean customEffectEnabled = z;
                if (customEffectEnabled != mCustomEffectEnabled) {
                    mCustomEffectEnabled = customEffectEnabled;
                    updateSuspensionRelatedScenarioStatus();
                    if (ISASUSCNSKU && mFocusedAppSupportCustomLight && !AuraLightService.IS_PICASSO) {
                        mCustomEffectAllowApp = mFocusedApp;
                        long now = System.currentTimeMillis();
                        if (mCustomEffectEnabled) {
                            mCustomEffectOnCount++;
                            mCustomEffectStartTime = now;
                        } else {
                            mCustomEffectOffCount++;
                            mCustomEffectTotalTime += now - mCustomEffectStartTime;
                            mCustomEffectStartTime = 0L;
                        }
                        mHandler.removeCallbacks(mSettingsAnalyticsUploader);
                        mHandler.postDelayed(mSettingsAnalyticsUploader, 300000);
                    }
                }
            } else if (mSystemEffectEnabledUri.equals(uri)) {
                int state = Settings.System.getInt(mContext.getContentResolver(), SETTINGS_SYSTEM_SYSTEM_EFFECT_ENABLED, 1);
                if (AuraLightService.DEBUG_ANALYTICS) {
                    Slog.d(AuraLightService.TAG, "onChange: enable_system_effect: state=" + state);
                }
                mSystemEffectEnabled = state == 1;
                if (state != -1) {
                    AuraLightService auraLightService3 = AuraLightService.this;
                    auraLightService3.mSystemEffectEnabledByUser = auraLightService3.mSystemEffectEnabled;
                }
                if (mSystemEffectEnabled) {
                    resetActiveStateIfNeed();
                }
                if (mSystemEffectEnabled) {
                    z = mEffects[12].active;
                }
                boolean enableBootingEffect = z;
                setSystemPropertiesNoThrow(AuraLightService.PROP_BOOTING_EFFECT, enableBootingEffect ? "2" : "0");
                synchronized (mLock) {
                    updateLightLocked();
                }
                if (ISASUSCNSKU && !AuraLightService.IS_PICASSO) {
                    if (mSystemEffectEnabled) {
                        mSystemEffectOnCount++;
                        mSystemEffectStartTime = System.currentTimeMillis();
                    } else {
                        mSystemEffectOffCount++;
                        mSystemEffectTotalTime += System.currentTimeMillis() - mSystemEffectStartTime;
                        mSystemEffectStartTime = 0L;
                    }
                    mHandler.removeCallbacks(mSettingsAnalyticsUploader);
                    mHandler.postDelayed(mSettingsAnalyticsUploader, 300000);
                }
            } else if (this.mLedStatesUri.equals(uri)) {
                AuraLightService auraLightService4 = AuraLightService.this;
                auraLightService4.mLedStatesRecord = Settings.System.getInt(auraLightService4.mContext.getContentResolver(), SETTINGS_SYSTEM_LED_STATES, AuraLightService.DEFAULT_LED_STATES);
                updateLedState();
                if (mDockState != 0) {
                    mHandler.removeMessages(21);
                    mHandler.sendEmptyMessage(21);
                }
            } else if (mChargingIndicatorPolicyUri.equals(uri)) {
                AuraLightService auraLightService5 = AuraLightService.this;
                auraLightService5.mChargingIndicatorPolicy = Settings.System.getInt(auraLightService5.mContext.getContentResolver(), SETTINGS_SYSTEM_CHARGING_INDICATOR_POLICY, 1);
                if (mBatteryManagerInternal != null) {
                    float percentage = mBatteryManagerInternal.getBatteryLevel() / 100.0f;
                    int color = getChargingIndicatorColor(percentage);
                    synchronized (mLock) {
                        AuraLightService auraLightService6 = AuraLightService.this;
                        auraLightService6.setScenarioEffectLocked(8, auraLightService6.mEffects[8].active, color, percentage <= 0.05f ? 0 : 1, 0);
                    }
                }
            } else if (this.mNotificationExpirationTimeUri.equals(uri)) {
                AuraLightService auraLightService7 = AuraLightService.this;
                auraLightService7.mNotificationExpirationTime = Settings.System.getInt(auraLightService7.mContext.getContentResolver(), SETTINGS_SYSTEM_NOTIFICATION_EXPIRATION_TIME, AuraLightService.DEFAULT_NOTIFICATION_EXPIRATION_TIME);
                boolean expirationTimeChanged = false;
                if (mNotificationExpirationTime <= 0) {
                    if (mStatus[7]) {
                        expirationTimeChanged = true;
                    }
                } else {
                    expirationTimeChanged = true;
                }
                if (!expirationTimeChanged) {
                    cancelStopNotification();
                    return;
                }
                scheduleStopNotification(mNotificationExpirationTime);
            } else if (mHeadsetSyncableUri.equals(uri)) {
                AuraLightService auraLightService8 = AuraLightService.this;
                if (Settings.System.getInt(auraLightService8.mContext.getContentResolver(), SETTINGS_SYSTEM_HEADSET_SYNCABLE, 0) == 1) {
                    z = true;
                }
                auraLightService8.mHeadsetSyncable = z;
                updateSyncDelay();
                if (mHeadsetSyncable) {
                    refreshLedLight();
                }
                if (!mAttachedHeadsetPids.isEmpty() && mHeadsetSyncable) {
                    mHandler.removeMessages(21);
                    mHandler.sendEmptyMessageDelayed(21, 1000L);
                }
            }
        }

        public void uploadAnalytics() {
            long systemEffectOffTime;
            long uploadInterval;
            String str;
            Slog.i(AuraLightService.TAG, "ALS perform analytics");
            long now = System.currentTimeMillis();
            long uploadInterval2 = now - mLastUploadTime;
            mLastUploadTime = now;
            long j = mCustomEffectStartTime;
            if (j > 0) {
                mCustomEffectTotalTime += now - j;
                mCustomEffectStartTime = now;
            }
            long j2 = mSystemEffectStartTime;
            if (j2 > 0) {
                mSystemEffectTotalTime += now - j2;
                mSystemEffectStartTime = now;
            }
            String customEffectChangeCountData = "count=" + (mCustomEffectOnCount + mCustomEffectOffCount) +
                " package=" + mCustomEffectAllowApp + " open_count=" + mCustomEffectOnCount + " close_count=" + mCustomEffectOffCount;
            String customEffectTotalTimeData = "time=" + mCustomEffectTotalTime + " package=" + mCustomEffectAllowApp;
            StringBuilder changeCountData = new StringBuilder();
            changeCountData.append("count=" + mSystemEffectOnCount + " position=systemtool_open_light_switch");
            changeCountData.append("\n");
            changeCountData.append("count=" + mSystemEffectOffCount + " position=systemtool_close_light_switch");
            changeCountData.append("\n");
            changeCountData.append("count=" + (mCustomEffectOnCount + mCustomEffectOffCount) + " position=gamegenie_light_switch");
            StringBuilder effectTotalTimeData = new StringBuilder();
            StringBuilder sb = new StringBuilder();
            sb.append("time=");
            String str2 = "count=";
            sb.append(mSystemEffectTotalTime);
            sb.append(" position=systemtool_open_light_switch");
            effectTotalTimeData.append(sb.toString());
            effectTotalTimeData.append("\n");
            long systemEffectOffTime2 = Math.max(0L, uploadInterval2 - mSystemEffectTotalTime);
            effectTotalTimeData.append("time=" + systemEffectOffTime2 + " position=systemtool_close_light_switch");
            effectTotalTimeData.append("\n");
            effectTotalTimeData.append("time=" + mCustomEffectTotalTime + " position=gamegenie_light_switch");
            int i = 0;
            while (i < mEffects.length) {
                String position = null;
                switch (i) {
                    case 4:
                        position = "arua_telephone_ringing_light_switch";
                        break;
                    case 5:
                        position = "arua_telephone_offhook_light_switch";
                        break;
                    case 6:
                        position = "arua_xmode_light_switch";
                        break;
                    case 7:
                        position = "arua_info_light_switch";
                        break;
                    case 8:
                        position = "arua_charging_light_switch";
                        break;
                    case 9:
                        position = "arua_screenlighting_light_switch";
                        break;
                    case 11:
                        position = "arua_gamestart_light_switch";
                        break;
                    case 15:
                        position = "aura_accessory_light_switch";
                        break;
                }
                if (mEffects[i].effectStartTime > 0) {
                    uploadInterval = uploadInterval2;
                    systemEffectOffTime = systemEffectOffTime2;
                    mEffects[i].effectTotalTime += now - mEffects[i].effectStartTime;
                    mEffects[i].effectStartTime = now;
                } else {
                    uploadInterval = uploadInterval2;
                    systemEffectOffTime = systemEffectOffTime2;
                }
                if (position == null) {
                    str = str2;
                } else {
                    changeCountData.append("\n");
                    StringBuilder sb2 = new StringBuilder();
                    str = str2;
                    sb2.append(str);
                    sb2.append(mEffects[i].stateChangeCount);
                    sb2.append(" position=");
                    sb2.append(position);
                    changeCountData.append(sb2.toString());
                    effectTotalTimeData.append("\n");
                    effectTotalTimeData.append("time=" + mEffects[i].effectTotalTime + " position=" + position);
                }
                mEffects[i].stateChangeCount = 0;
                i++;
                str2 = str;
                uploadInterval2 = uploadInterval;
                systemEffectOffTime2 = systemEffectOffTime;
            }
            mDropBoxManager.addText(AuraLightService.DROPBOX_TAG_SYSTEM_EFFECT_CHANGE_COUNT, Encryption.encrypt(changeCountData.toString()));
            mDropBoxManager.addText(AuraLightService.DROPBOX_TAG_SYSTEM_EFFECT_TOTAL_TIME, Encryption.encrypt(effectTotalTimeData.toString()));
            if (mCustomEffectAllowApp != null) {
                mDropBoxManager.addText(AuraLightService.DROPBOX_TAG_CUSTOM_EFFECT_CHANGE_COUNT, Encryption.encrypt(customEffectChangeCountData));
                mDropBoxManager.addText(AuraLightService.DROPBOX_TAG_CUSTOM_EFFECT_TOTAL_TIME, Encryption.encrypt(customEffectTotalTimeData));
            }
            mSystemEffectOnCount = 0;
            mSystemEffectOffCount = 0;
            mCustomEffectOnCount = 0;
            mCustomEffectOffCount = 0;
            mSystemEffectTotalTime = 0L;
            mCustomEffectTotalTime = 0L;
            for (int i2 = 0; i2 < mEffects.length; i2++) {
                mEffects[i2].stateChangeCount = 0;
                mEffects[i2].effectTotalTime = 0L;
            }
            if (!mCustomEffectEnabled || !mFocusedAppSupportCustomLight) {
                mCustomEffectAllowApp = null;
            }
        }

        private void transferLagencyLedStatesIfNeeded() {
            int ledStatesCandidate;
            int ledStatesCandidate2;
            ContentResolver resolver = mContext.getContentResolver();
            int inboxLedState = Settings.System.getInt(resolver, "inbox_led_state", -1);
            int stationLedState = Settings.System.getInt(resolver, "station_led_state", -1);
            if (inboxLedState != -1 || stationLedState != -1) {
                int ledStatesCandidate3 = Settings.System.getInt(resolver, SETTINGS_SYSTEM_LED_STATES, AuraLightService.DEFAULT_LED_STATES);
                if (inboxLedState == 0) {
                    ledStatesCandidate = ledStatesCandidate3 | 32 | 16;
                } else if (inboxLedState == 1) {
                    ledStatesCandidate = (ledStatesCandidate3 & (-33)) | 16;
                } else {
                    ledStatesCandidate = (ledStatesCandidate3 | 32) & (-17);
                }
                if (stationLedState == 0) {
                    ledStatesCandidate2 = ledStatesCandidate | 512 | 256;
                } else if (stationLedState == 1) {
                    ledStatesCandidate2 = (ledStatesCandidate & (-513)) | 256;
                } else {
                    ledStatesCandidate2 = (ledStatesCandidate | 512) & (-257);
                }
                Settings.System.putInt(resolver, "inbox_led_state", -1);
                Settings.System.putInt(resolver, "station_led_state", -1);
                Settings.System.putInt(resolver, SETTINGS_SYSTEM_LED_STATES, ledStatesCandidate2);
            }
        }
    }

    public AuraLightService(Context context) {
        mContext = context;
    }

    private int getLightScenarioLocked() {
        int[] iArr;
        boolean z = DEBUG_ANALYTICS;
        if (z) {
            Slog.d(TAG, "getLightScenarioLocked: mSystemEffectEnabled=" + mSystemEffectEnabled);
        }
        if (!mEnabled || mIsUltraSavingMode) {
            if (z) {
                Slog.d(TAG, "getLightScenarioLocked: mEnabled=" + mEnabled + ", mIsUltraSavingMode=" + mIsUltraSavingMode);
            }
            return -1;
        }
        for (int scenario : AuraLightManager.SCENARIO_PRIORITIES) {
            if (this.mStatus[scenario] && mEffects[scenario].active && (scenario == 1 || mSystemEffectEnabled)) {
                return scenario;
            }
        }
        return -1;
    }

    private void handleFocusedAppChanged(String packageName) {
        if (packageName == null || packageName.equals(mFocusedApp)) {
            return;
        }
        mFocusedApp = packageName;
        mFocusedAppSupportCustomLight = mSupportCustomLightApps.contains(packageName);
        boolean prevAppIsGame = mFocusedAppIsGame;
        boolean isGameApp = AspectRatioChecker.getInstance().isGameApp(packageName);
        mFocusedAppIsGame = isGameApp;
        if (isGameApp) {
            showGameAppsLaunchEffect();
        } else if (prevAppIsGame != isGameApp) {
            synchronized (mLock) {
                updateLightLocked();
            }
        }
        if (ISASUSCNSKU && mCustomEffectEnabled && !IS_PICASSO) {
            long now = System.currentTimeMillis();
            if (mFocusedAppSupportCustomLight) {
                mSettingsObserver.mCustomEffectAllowApp = mFocusedApp;
                mSettingsObserver.mCustomEffectStartTime = now;
            } else if (mSettingsObserver.mCustomEffectAllowApp != null) {
                mSettingsObserver.mCustomEffectTotalTime += now - mSettingsObserver.mCustomEffectStartTime;
                mSettingsObserver.mCustomEffectStartTime = 0L;
                mHandler.removeCallbacks(mSettingsAnalyticsUploader);
                mHandler.post(mSettingsAnalyticsUploader);
            }
        }
    }

    private boolean handleCollectSupportCustomLightApps() {
        if (this.mTcSystemService == null) {
            Slog.w(TAG, "Query TCSystemService failed (Custom All App), err: TcSystemService has not bound.");
            return false;
        }
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> installedPkgs = pm.getInstalledApplications(0);
        List<String> tmp = new ArrayList<>();
        for (ApplicationInfo appInfo : installedPkgs) {
            if (!appInfo.isSystemApp()) {
                List<String> moduleList = getModuleList(appInfo.packageName);
                if (moduleList != null) {
                    Slog.w(TAG, "Collect all app, pkg=" + appInfo.packageName + ", moduleList=" + Arrays.toString(moduleList.toArray()));
                }
                if (moduleList != null && moduleList.contains("light")) {
                    tmp.add(appInfo.packageName);
                }
            }
        }
        synchronized (mLock) {
            mSupportCustomLightApps.clear();
            mSupportCustomLightApps.addAll(tmp);
        }
        return true;
    }

    private boolean handleIdentifySupportCustomLightApp(String packageName) {
        if (this.mTcSystemService == null) {
            Slog.w(TAG, "Query TCSystemService failed (Custom App), err: TcSystemService has not bound.");
            return false;
        }
        List<String> moduleList = getModuleList(packageName);
        if (moduleList != null) {
            Slog.w(TAG, "Custom pkg=" + packageName + ", moduleList=" + Arrays.toString(moduleList.toArray()));
        }
        if (moduleList != null && moduleList.contains("light")) {
            synchronized (mLock) {
                mSupportCustomLightApps.add(packageName);
            }
            return true;
        }
        return true;
    }

    private List<String> getModuleList(String packageName) {
        if (this.mTcSystemService == null) {
            Slog.w(TAG, "Query TCSystemService failed, err: TcSystemService has not bound");
            return null;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        List<String> result = null;
        try {
            try {
                data.writeInterfaceToken("com.tencent.inlab.tcsystem.ITCSystemService");
                data.writeString(packageName);
                this.mTcSystemService.transact(13, data, reply, 0);
                reply.readException();
                result = reply.createStringArrayList();
            } catch (Exception e) {
                Slog.w(TAG, "Query TCSystemService failed, err: " + e.getMessage());
            }
            return result;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void onAwakeStateChanged(boolean isAwake) {
        mScreenOn = isAwake;
        int screenOffDelay = 0;
        if (mHeadsetSyncable && !mAttachedHeadsetPids.isEmpty() && mAttachedHeadsetPids.contains(6467)) {
            screenOffDelay = 1000;
        }
        mHandler.sendEmptyMessageDelayed(5, mScreenOn ? 1000L : screenOffDelay);
        if (ISASUSCNSKU && mScreenOn && !IS_PICASSO) {
            long now = System.currentTimeMillis();
            if (now - mSettingsObserver.mLastUploadTime >= SETTINGS_ANALYTICS_UPLOAD_INTERVAL) {
                mHandler.removeCallbacks(mSettingsAnalyticsUploader);
                mHandler.post(mSettingsAnalyticsUploader);
            }
        }
    }

    public void onKeyguardStateChanged(boolean isShowing) {
        mKeyguardShowing = isShowing;
        if (!isShowing && mBumperState == 0) {
            mHandler.sendEmptyMessageDelayed(11, 2000L);
        }
    }

    public void onStart() {
        LightEffect[] lightEffectArr = new LightEffect[16];
        boolean[] zArr = SCENARIOS_ACTIVE_STATE_DEFAULT;
        boolean z = zArr[0];
        int i = DEFAULT_RED_COLOR;
        lightEffectArr[0] = new LightEffect(z, i, 1, 0);
        lightEffectArr[1] = new LightEffect(zArr[1], i, 1, 0);
        boolean z2 = zArr[2];
        int i2 = DEFAULT_WHITE_COLOR;
        lightEffectArr[2] = new LightEffect(z2, i2, 4, 0);
        lightEffectArr[3] = new LightEffect(zArr[3], i, 1, 0);
        lightEffectArr[4] = new LightEffect(zArr[4], 2944413, 2, -2);
        lightEffectArr[5] = new LightEffect(zArr[5], i, 1, 0);
        lightEffectArr[6] = new LightEffect(zArr[6], i2, IS_ANAKIN ? 3 : 4, -1);
        lightEffectArr[7] = new LightEffect(zArr[7], i, 1, 0);
        lightEffectArr[8] = new LightEffect(zArr[8], i, 1, 0);
        lightEffectArr[9] = new LightEffect(zArr[9], i, 3, 0);
        lightEffectArr[10] = new LightEffect(zArr[10], DEFAULT_BLUE_COLOR, 2, 0);
        lightEffectArr[11] = new LightEffect(zArr[11], i2, 4, 0);
        lightEffectArr[12] = new LightEffect(zArr[12], i, 2, -2);
        lightEffectArr[13] = new LightEffect(zArr[13], i, 1, 0);
        lightEffectArr[14] = new LightEffect(zArr[14], i, 1, 0);
        lightEffectArr[15] = new LightEffect(zArr[15], i, 1, 0);
        mEffects = lightEffectArr;
        mNotificationEffects = new HashMap();
        mRestoreCetraEffect = new HashMap();
        mEnabled = true;
        mScreenOn = true;
        mXModeOn = false;
        mCustomEffectEnabled = true;
        mSystemEffectEnabled = true;
        mSystemEffectEnabledByUser = true;
        mLightSettingsChanged = false;
        mNotificationSettingsChanged = false;
        mKeyguardShowing = false;
        mIsUltraSavingMode = false;
        mSupportBlendedEffect = false;
        mHeadsetSyncable = false;
        mIpLightEnabled = false;
        mLightRealOn = false;
        mInboxConnect = false;
        mChargingIndicatorPolicy = 1;
        mNotificationExpirationTime = DEFAULT_NOTIFICATION_EXPIRATION_TIME;
        mSyncDelay = SYNC_DELAY;
        mScenario = -1;
        mBumperState = 3;
        mPhoneState = 0;
        mIsCharging = false;
        mIsGameViceConnected = false;
        mIsInboxAndBumperConnected = false;
        mIsCetraRGBConnected = false;
        mIsStorageDeviceConnected = false;
        mAttachedHeadsetPids = new ArraySet();
        int i3 = DEFAULT_LED_STATES;
        mLedStatesRecord = i3;
        mCurrentLedStates = i3;
        mCustomLedStates = 0;
        mScenarioEffectStartTime = 0L;
        mBootCompleted = false;
        mFocusedAppIsGame = false;
        mFocusedAppSupportCustomLight = false;
        mSupportCustomLightApps = new ArrayList();
        mPendingTcSystemServiceCommand = new ArrayList();
        mTcSystemServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mTcSystemService = service;
                synchronized (mPendingTcSystemServiceCommand) {
                    if (!mPendingTcSystemServiceCommand.isEmpty()) {
                        for (Message msg : mPendingTcSystemServiceCommand) {
                            if (!mHandler.hasMessages(msg.what, msg.obj)) {
                                try {
                                    mHandler.sendMessage(msg);
                                } catch (Exception e) {
                                    Slog.w(AuraLightService.TAG, "Send msg failed, err: " + e.getMessage());
                                }
                            }
                        }
                        mPendingTcSystemServiceCommand.clear();
                        return;
                    }
                    mContext.unbindService(this);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mTcSystemService = null;
            }
        };
        mSupportCustomLightAppsChecker = new Runnable() {
            @Override
            public void run() {
                Message cmd = Message.obtain(mHandler, 19);
                cmd.arg1 = 0;
                if (mTcSystemService == null) {
                    synchronized (mPendingTcSystemServiceCommand) {
                        mPendingTcSystemServiceCommand.add(cmd);
                    }
                    mHandler.removeMessages(18);
                    mHandler.sendEmptyMessage(18);
                } else {
                    cmd.sendToTarget();
                }
                mHandler.postDelayed(this, 43200000);
            }
        };
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Message msg = Message.obtain(mHandler, 20);
                msg.arg1 = state;
                mHandler.sendMessageDelayed(msg, 500L);
            }
        };
        mCameraMonitor = new CameraMonitor();
        mDockState = 0;
        mDockLedChangeTime = SystemClock.elapsedRealtime();
        mDockDuration = new long[]{0, 0, 0, 0};
        mDockLedOnDuration = new long[]{0, 0, 0, 0};
        mSettingsAnalyticsUploader = new Runnable() {
            @Override
            public void run() {
                mSettingsObserver.uploadAnalytics();
                mHandler.postDelayed(this, AuraLightService.SETTINGS_ANALYTICS_UPLOAD_INTERVAL);
            }
        };
        showBootEffect(true);
        HandlerThread thread = new HandlerThread("AuraLight");
        thread.start();
        mHandler = new WorkerHandler(thread.getLooper());
        mUsbDeviceController = new UsbDeviceController(mContext, mHandler);
        mHeadsetController = new HeadsetLightController(mContext, mHandler, mUsbDeviceController);
        File systemDir = new File(Environment.getExternalStorageDirectory(), "Rogservices");
        mAuraLightFile = new AtomicFile(new File(systemDir, "aura_light_setting.xml"));
        mNotificationFile = new AtomicFile(new File(systemDir, "aura_notification_setting.xml"));
        readSettings();
        if (mBumperState == 1) {
            enableIpLight(true);
            enableNfcTypeV(false);
        }
        if (mNotificationEffects.get("!default") == null) {
            mNotificationEffects.put("!default", new LightEffect(true, 13791027, 3, 0));
        }
        LocalService localService = new LocalService();
        mLocalService = localService;

        new BinderService();
        onSystemServicesReady();
        onBootPhase();
    }

    void disable() {
    }

    public void onBootPhase() {
        if (!StorageManager.inCryptKeeperBounce()) {
            mBootCompleted = true;
            TelephonyManager.from(mContext).listen(mPhoneStateListener, 32);
            nativeNotifyScreenOffEffectActive(mEffects[10].active);
        }
        showBootEffect(false);
        if (ISASUSCNSKU && !IS_PICASSO) {
            mHandler.postDelayed(mSupportCustomLightAppsChecker, 60000L);
        }
        updateSystemEffectState(false);
        if (!IS_PICASSO) {
            mUsbDeviceController.onSystemReady();
            mHeadsetController.onSystemReady();
        }
        Message msg = new Message();
        msg.what = 27;
        mHandler.removeMessages(27);
        mHandler.sendMessageDelayed(msg, 86400000L);
    }

    private void onSystemServicesReady() {
        updateLidState();
        Context context = mContext;
        mDropBoxManager = (DropBoxManager) context.getSystemService(DropBoxManager.class);
        updateSupportBlendedEffect();
        SettingsObserver settingsObserver = new SettingsObserver();
        mSettingsObserver = settingsObserver;
        settingsObserver.init();
        if (ISASUSCNSKU && !IS_PICASSO) {
            mHandler.postDelayed(mSettingsAnalyticsUploader, SETTINGS_ANALYTICS_UPLOAD_INTERVAL);
        }
        context.getContentResolver();
        boolean z = IS_PICASSO;
        if (!z) {
            mHeadsetController.addStateMonitor(new HeadsetLightController.HeadsetStateMonitor() {
                @Override
                public void onStateChanged(int pid, boolean attached) {
                    if (attached) {
                        mAttachedHeadsetPids.add(Integer.valueOf(pid));
                        showPowerConnectedEffect();
                        allScenariosRateToSlow();
                        if (pid == 6501) {
                            mIsCetraRGBConnected = attached;
                            resetCetraRGBScenarios();
                        }
                    } else {
                        mAttachedHeadsetPids.remove(Integer.valueOf(pid));
                        if (pid == 6501) {
                            mIsCetraRGBConnected = attached;
                            restoreCetraRGBScenarios();
                        }
                    }
                    updateSupportBlendedEffect();
                    updateSyncDelay();
                    updateLedState();
                    if (attached && mHeadsetSyncable) {
                        mHandler.removeMessages(21);
                        mHandler.sendEmptyMessageDelayed(21, 1000L);
                    }
                }
            });
        }

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int state = intent.getIntExtra("status", -1);
                int level = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int plugged = intent.getIntExtra("plugged", 0);
                float percentage = level / scale;
                int color = getChargingIndicatorColor(percentage);
                synchronized (mLock) {
                    AuraLightService auraLightService = AuraLightService.this;
                    boolean z2 = true;
                    auraLightService.setScenarioEffectLocked(8, auraLightService.mEffects[8].active, color, percentage <= 0.05f ? 0 : 1, 0);
                    boolean prevIsCharging = mIsCharging;
                    AuraLightService auraLightService2 = AuraLightService.this;
                    if (state == 1 || plugged == 0) {
                        z2 = false;
                    }
                    auraLightService2.mIsCharging = z2;
                    if (!prevIsCharging && mIsCharging && mScenario != 15) {
                        showPowerConnectedEffect();
                    }
                    AuraLightService auraLightService3 = AuraLightService.this;
                    auraLightService3.setScenarioStatusLocked(8, auraLightService3.mIsCharging);
                }
            }
        }, new IntentFilter("android.intent.action.BATTERY_CHANGED"), null, mHandler);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int powerMode = intent.getIntExtra(AuraLightService.EXTRA_POWER_SAVER_MODE, -1);
                if (AuraLightService.DEBUG_ANALYTICS) {
                    Slog.d(AuraLightService.TAG, "onReceive: ACTION_POWER_SAVER_MODE_CHANGED, powerMode=" + powerMode);
                }
                boolean ultraSavingMode = true;
                if (powerMode != 1 && powerMode != 6 && powerMode != 11) {
                    ultraSavingMode = false;
                }
                if (ultraSavingMode != mIsUltraSavingMode) {
                    mIsUltraSavingMode = ultraSavingMode;
                    updateLightLocked();
                }
            }
        }, new IntentFilter(ACTION_POWER_SAVER_MODE_CHANGED), null, mHandler);
        if (!z) {
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    showPowerConnectedEffect();
                }
            }, new IntentFilter(ACTION_FAN_FW_UPDATED), null, mHandler);
            mCameraMonitor.startMonitor();
        }
        if (ISASUSCNSKU && !z) {
            IntentFilter packageFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            packageFilter.addDataScheme("package");
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    Uri data = intent.getData();
                    String pkgName = data.getEncodedSchemeSpecificPart();
                    Message cmd = Message.obtain(mHandler, 19);
                    cmd.arg1 = 1;
                    cmd.obj = pkgName;
                    Slog.w(AuraLightService.TAG, "onReceive: " + intent.getAction());
                    if (mTcSystemService == null) {
                        synchronized (mPendingTcSystemServiceCommand) {
                            mPendingTcSystemServiceCommand.add(cmd);
                        }
                        mHandler.removeMessages(18);
                        mHandler.sendEmptyMessage(18);
                        return;
                    }
                    cmd.sendToTarget();
                }
            }, packageFilter, null, mHandler);
        }
        if (!z) {
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    syncFrame();
                }
            }, new IntentFilter(ACTION_SCHEDULE_SYNC_FRAME), null, mHandler);
        }
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                synchronized (mLock) {
                    setScenarioStatusLocked(7, false);
                }
            }
        }, new IntentFilter(ACTION_STOP_NOTIFICATION), null, mHandler);
    }

    private void readLightSettings() {
        XmlPullParser parser;
        synchronized (mLock) {
            try {
                try {
                    FileInputStream stream = mAuraLightFile.openRead();
                    parser = Xml.newPullParser();
                    int type = parser.next();
                    try {
                        parser.setInput(stream, StandardCharsets.UTF_8.name());
                        while (true) {
                            if (type == 2 || type == 1) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Slog.w(TAG, "Failed parsing aura light settings, err: " + e.getMessage());
                        IoUtils.closeQuietly(stream);
                    }
                    if (type != 2) {
                        Slog.w(TAG, "No start tag found in aura light settings");
                        IoUtils.closeQuietly(stream);
                        return;
                    }
                    String versionString = parser.getAttributeValue(null, ATTR_AURA_LIGHT_SETTING_VERSION);
                    if (versionString != null) {
                        Integer.valueOf(versionString).intValue();
                    }
                    mEnabled = Boolean.valueOf(parser.getAttributeValue(null, "enabled")).booleanValue();
                    LightEffect[] effects = new LightEffect[16];
                    LightEffect[] lightEffectArr = mEffects;
                    System.arraycopy(lightEffectArr, 0, effects, 0, lightEffectArr.length);
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                            break;
                        } else if (type2 != 3 && type2 != 4) {
                            String tagName = parser.getName();
                            if (tagName.equals(TAG_AURA_LIGHT_SCENARIO)) {
                                readScenario(parser, effects);
                            } else if (tagName.equals(TAG_BUMPER_SETTINGS)) {
                                mBumperState = Integer.valueOf(parser.getAttributeValue(null, "state")).intValue();
                            } else {
                                Slog.w(TAG, "Unknown element under <aura-light-setting>: " + tagName);
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                    System.arraycopy(effects, 1, mEffects, 1, effects.length - 1);
                    IoUtils.closeQuietly(stream);
                } catch (FileNotFoundException e2) {
                    Slog.i(TAG, "No existing aura light settings");
                }
            } catch (Throwable th) {
                //throw th;
            }
        }
    }

    private void readScenario(XmlPullParser parser, LightEffect[] out) throws NumberFormatException, XmlPullParserException, IOException {
        int scenario = Integer.valueOf(parser.getAttributeValue(null, "type")).intValue();
        if (scenario == 0 || scenario == 1 || scenario == 2 || scenario == 3) {
            return;
        }
        out[scenario].active = Boolean.valueOf(parser.getAttributeValue(null, "active")).booleanValue();
        out[scenario].color = Integer.valueOf(parser.getAttributeValue(null, ATTR_AURA_LIGHT_SCENARIO_COLOR)).intValue();
        out[scenario].mode = Integer.valueOf(parser.getAttributeValue(null, "mode")).intValue();
        out[scenario].rate = Integer.valueOf(parser.getAttributeValue(null, ATTR_AURA_LIGHT_SCENARIO_RATE)).intValue();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_AURA_LIGHT_BLENDED)) {
                            readBlendedEffect(parser, scenario, out);
                        } else {
                            Slog.w(TAG, "Unknown element under <scenario>: " + tagName);
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readBlendedEffect(XmlPullParser parser, int scenario, LightEffect[] out) throws NumberFormatException, XmlPullParserException, IOException {
        out[scenario].blendedEffect = new BlendedLightEffect();
        out[scenario].blendedEffect.mode = Integer.valueOf(parser.getAttributeValue(null, "mode")).intValue();
        out[scenario].blendedEffect.rate = Integer.valueOf(parser.getAttributeValue(null, ATTR_AURA_LIGHT_SCENARIO_RATE)).intValue();
        for (int i = 0; i < 6; i++) {
            String colorAttr = ATTR_AURA_LIGHT_SCENARIO_COLOR + i;
            out[scenario].blendedEffect.colors[i] = Integer.valueOf(parser.getAttributeValue(null, colorAttr)).intValue();
        }
    }

    private void updateSupportBlendedEffect() {
        int i;
        mSupportBlendedEffect = !mIsCetraRGBConnected && mAttachedHeadsetPids.isEmpty() && !mIsStorageDeviceConnected && !mIsGameViceConnected && ((i = mDockState) == 6 || i == 7 || IS_ANAKIN);
    }

    private void readNotificationSettings() {
        XmlPullParser parser;
        synchronized (mLock) {
            try {
                try {
                    parser = Xml.newPullParser();
                    int type = parser.next();
                    FileInputStream stream = mNotificationFile.openRead();
                    mNotificationEffects.clear();
                    try {
                        parser.setInput(stream, StandardCharsets.UTF_8.name());
                        while (true) {
                            if (type == 2 || type == 1) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Slog.w(TAG, "Failed parsing notification light settings, err: " + e.getMessage());
                        if (type == DONGLE_TYPE_NO_DONGLE) {
                            mNotificationEffects.clear();
                        }
                    }
                    if (type != 2) {
                        Slog.w(TAG, "No start tag found in aura light settings");
                        if (type == DONGLE_TYPE_NO_DONGLE) {
                            mNotificationEffects.clear();
                        }
                        IoUtils.closeQuietly(stream);
                        return;
                    }
                    int outerDepth = parser.getDepth();
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                            break;
                        } else if (type2 != 3 && type2 != 4) {
                            String tagName = parser.getName();
                            if (tagName.equals("custom")) {
                                readCustom(parser);
                            } else {
                                Slog.w(TAG, "Unknown element under <aura-light-setting>: " + tagName);
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                    if (1 == 0) {
                        mNotificationEffects.clear();
                    }
                    IoUtils.closeQuietly(stream);
                } catch (FileNotFoundException e2) {
                    Slog.i(TAG, "No existing notification light settings");
                }
            } catch (Throwable th) {
                //throw th;
            }
        }
    }

    private void readCustom(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        String pkg = parser.getAttributeValue(null, "package");
        LightEffect effect = new LightEffect();
        effect.active = Boolean.valueOf(parser.getAttributeValue(null, "active")).booleanValue();
        effect.color = Integer.valueOf(parser.getAttributeValue(null, ATTR_AURA_LIGHT_SCENARIO_COLOR)).intValue();
        effect.mode = Integer.valueOf(parser.getAttributeValue(null, "mode")).intValue();
        effect.rate = Integer.valueOf(parser.getAttributeValue(null, ATTR_AURA_LIGHT_SCENARIO_RATE)).intValue();
        mNotificationEffects.put(pkg, effect);
    }

    private void readSettings() {
        readLightSettings();
        readNotificationSettings();
    }

    private void scheduleWriteSettings() {
        if (!mHandler.hasMessages(4)) {
            mHandler.sendEmptyMessageDelayed(4, 10000);
        }
    }

    private void scheduleSyncFrame() {
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 1, new Intent(ACTION_SCHEDULE_SYNC_FRAME).addFlags(268435456), 201326592);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService("alarm");
        if (alarmManager != null) {
            alarmManager.cancel(pi);
            alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + mSyncDelay, pi);
        }
    }

    private void sendBroadcast(Intent intent, String permission) {
        ActivityManagerInternal activityManagerInternal = this.mActivityManagerInternal;
        if (activityManagerInternal == null || !activityManagerInternal.isSystemReady()) {
            return;
        }
        try {
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL, permission);
        } catch (Exception e) {
        }
    }

    private void setLightLocked(int scenario, int color, int mode, int speed, BlendedLightEffect blendedEffect) {
        int i;
        if (!mBootCompleted) {
            return;
        }
        boolean z = true;
        if (mSupportBlendedEffect && blendedEffect != null && blendedEffect.mode != 0) {
            if (DEBUG_ANALYTICS) {
                Slog.d(TAG, "setLightLocked -> nativeSetBlendedLight");
            }
            nativeSetBlendedLight(transferToBlendedMode(blendedEffect.mode), blendedEffect.colors, blendedEffect.rate, mCurrentLedStates);
            if (mHeadsetSyncable) {
                mHeadsetController.requestSetBlendedEffect(blendedEffect.mode, blendedEffect.colors);
            }
        } else {
            if (DEBUG_ANALYTICS) {
                Slog.d(TAG, "setLightLocked -> nativeSetLight");
            }
            int ledStates = mCurrentLedStates;
            if (scenario == 1 && (i = mCustomLedStates) > 0) {
                ledStates = (ledStates & (-6)) | i;
            }
            nativeSetLight(mode, simulateLedColor(color), speed, ledStates);
            if (mHeadsetSyncable) {
                mHeadsetController.requestSetEffect(mode, color);
            }
        }
        if (scenario == -1) {
            mScenarioEffectStartTime = 0L;
        } else {
            mScenarioEffectStartTime = SystemClock.elapsedRealtime();
        }
        if (scenario == 11) {
            color &= 16777215;
        }
        if (mScenario == scenario || scenario <= 0 || color <= 0 || scenario == 0 || scenario == 1 || scenario == 12) {
            z = false;
        }
        boolean uploadSystemLightAnalytics = z;
        mScenario = scenario;
        mColor = color;
        mMode = mode;
        mRate = speed;
        mBlendedEffect = blendedEffect;
        updateDockLedDurationLocked();
        mHandler.sendEmptyMessage(0);
        mHandler.removeMessages(21);
        if (mEnabled && mScenario != -1 && (mIsGameViceConnected || mIsInboxAndBumperConnected || (mHeadsetSyncable && !mAttachedHeadsetPids.isEmpty()))) {
            mHandler.sendEmptyMessageDelayed(21, 1000L);
        }
        if (ISASUSCNSKU && !IS_PICASSO && uploadSystemLightAnalytics) {
            uploadSystemLightAnalytics(scenario);
        }
        AsusAnalytics analytics = new AsusAnalytics(scenario, System.currentTimeMillis());
        Message msg = mHandler.obtainMessage(23, analytics);
        mHandler.sendMessage(msg);
    }

    private int transferToBlendedMode(int mode) {
        switch (mode) {
            case 5:
                return 1;
            case 6:
                return 2;
            case 7:
                return 3;
            case 8:
                return 4;
            case 9:
                return 5;
            case 10:
                return 6;
            case 11:
                return 7;
            case 12:
                return 8;
            case 13:
                return 9;
            default:
                return 0;
        }
    }

    private void refreshLedLight() {
        BlendedLightEffect blendedLightEffect;
        if (mSupportBlendedEffect && (blendedLightEffect = mBlendedEffect) != null && blendedLightEffect.mode != 0) {
            nativeSetBlendedLight(transferToBlendedMode(mBlendedEffect.mode), mBlendedEffect.colors, mBlendedEffect.rate, mCurrentLedStates);
            if (mHeadsetSyncable) {
                mHeadsetController.requestSetBlendedEffect(mBlendedEffect.mode, mBlendedEffect.colors);
                return;
            }
            return;
        }
        if (DEBUG_ANALYTICS) {
            Slog.d(TAG, "refreshLedLight -> nativeSetLight");
        }
        nativeSetLight(mMode, simulateLedColor(mColor), mRate, mCurrentLedStates);
        if (mHeadsetSyncable) {
            mHeadsetController.requestSetEffect(mMode, mColor);
        }
    }

    private void setScenarioEffectLocked(int scenario, boolean active, int color, int mode, int rate) {
        boolean prevActive;
        int color2;
        if (scenario >= 0) {
            LightEffect[] lightEffectArr = mEffects;
            if (scenario >= lightEffectArr.length) {
                return;
            }
            boolean prevActive2 = lightEffectArr[scenario].active;
            if (scenario == 12) {
                prevActive = SystemProperties.getInt(PROP_BOOTING_EFFECT, 2) != 0;
            } else if (scenario == 14) {
                prevActive = Settings.System.getInt(mContext.getContentResolver(), SETTINGS_SYSTEM_BUMPER_CONNECTED_EFFECT, 1) == 1;
            } else {
                prevActive = prevActive2;
            }
            if (scenario != 1 && prevActive == active) {
                color2 = color;
                if (mEffects[scenario].color == color2 && mEffects[scenario].mode == mode && mEffects[scenario].rate == rate &&
                    (mEffects[scenario].blendedEffect == null || mEffects[scenario].blendedEffect.mode == 0)) {
                    return;
                }
            } else {
                color2 = color;
            }
            if (scenario == 12) {
                setSystemPropertiesNoThrow(PROP_BOOTING_EFFECT, active ? "2" : "0");
            } else if (scenario == 14) {
                Settings.System.putInt(mContext.getContentResolver(), SETTINGS_SYSTEM_BUMPER_CONNECTED_EFFECT, active ? 1 : 0);
            } else if (scenario == 10) {
                nativeNotifyScreenOffEffectActive(active);
            }
            if ((scenario != 0 && scenario != 1 && scenario != 13) || (scenario == 13 && mEffects[scenario].active != active)) {
                mLightSettingsChanged = true;
            }
            if (mode == 4) {
                try {
                    color2 = DEFAULT_WHITE_COLOR;
                } catch (Exception e) {
                    e = e;
                    Slog.w(TAG, "Set scenario effect failed, err: " + e.getMessage());
                    return;
                }
            }
            try {
                if (ISASUSCNSKU && active != mEffects[scenario].active && !IS_PICASSO) {
                    mEffects[scenario].stateChangeCount++;
                    if (active) {
                        try {
                            if (mEffects[scenario].effectStartTime == 0) {
                                mEffects[scenario].effectStartTime = System.currentTimeMillis();
                            }
                            if (scenario == 13) {
                                uploadSystemLightAnalytics(13);
                            }
                        } catch (Exception e2) {
                            Slog.w(TAG, "Set scenario effect failed, err: " + e2.getMessage());
                            return;
                        }
                    } else if (mEffects[scenario].effectStartTime != 0) {
                        try {
                            mEffects[scenario].effectTotalTime += System.currentTimeMillis() - mEffects[scenario].effectStartTime;
                            mEffects[scenario].effectStartTime = 0L;
                        } catch (Exception e3) {
                            Slog.w(TAG, "Set scenario effect failed, err: " + e3.getMessage());
                            return;
                        }
                    }
                }
                mEffects[scenario].active = active;
                mEffects[scenario].color = color2;
                mEffects[scenario].mode = mode;
                mEffects[scenario].rate = rate;
                if (mSupportBlendedEffect) {
                    mEffects[scenario].blendedEffect = null;
                }
                updateLightLocked();
                if (mLightSettingsChanged) {
                    scheduleWriteSettings();
                    Message msg = Message.obtain(mHandler, 1, scenario, -1);
                    msg.sendToTarget();
                }
                if (!active) {
                    updateSystemEffectState(false);
                }
            } catch (Exception e4) {
            }
        }
    }

    private void setScenarioBlendedEffectLocked(int scenario, boolean active, int[] colors, int mode, int rate) {
        if (scenario < 0 || scenario >= mEffects.length || scenario == 12 || mode == 1 || mode == 2 || mode == 3 || mode == 4) {
            return;
        }
        BlendedLightEffect newBlendedEffect = new BlendedLightEffect(mode, rate, colors);
        BlendedLightEffect blendedLightEffect = mEffects[scenario].blendedEffect;
        if (mEffects[scenario].active == active && newBlendedEffect.equals(mEffects[scenario].blendedEffect)) {
            return;
        }
        if (scenario == 10) {
            nativeNotifyScreenOffEffectActive(active);
        }
        if ((scenario != 0 && scenario != 1 && scenario != 13) || (scenario == 13 && mEffects[scenario].active != active)) {
            mLightSettingsChanged = true;
        }
        try {
            mEffects[scenario].active = active;
            mEffects[scenario].blendedEffect = newBlendedEffect;
            updateLightLocked();
            if (mLightSettingsChanged) {
                scheduleWriteSettings();
                Message msg = Message.obtain(mHandler, 1, scenario, -1);
                msg.sendToTarget();
            }
            if (!active) {
                updateSystemEffectState(false);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Set scenario effect failed, err: " + e.getMessage());
        }
    }

    private void setScenarioStatusLocked(int scenario, boolean status) {
        if (DEBUG_ANALYTICS) {
            Slog.d(TAG, "setScenarioStatusLocked: scenario=" + scenario + ", status=" + status + ", mStatus[scenario]=" + this.mStatus[scenario]);
        }
        boolean[] zArr = this.mStatus;
        if (zArr[scenario] != status) {
            try {
                zArr[scenario] = status;
                updateLightLocked();
            } catch (Exception e) {
                Slog.w(TAG, "Set scenario status failed, err: " + e.getMessage());
            }
        }
        if ((scenario == 2 || scenario == 3) && !status) {
            updateSystemEffectState(false);
        }
    }

    private void setSystemPropertiesNoThrow(String name, String value) {
        try {
            SystemProperties.set(name, value);
        } catch (Exception e) {
            Slog.w(TAG, "Wtf set " + name + " = " + value + " failed, err: " + e.getMessage());
        }
    }

    private void showBootEffect(boolean turnOn) {
        boolean z = DEBUG_ANALYTICS;
        if (z) {
            Slog.d(TAG, "showBootEffect: turnOn=" + turnOn + ", mBootCompleted=" + mBootCompleted);
        }
        if (SystemProperties.getInt(PROP_BOOTING_EFFECT, 2) == 0) {
            if (!turnOn && mBootCompleted) {
                updateSuspensionRelatedScenarioStatus();
                synchronized (mLock) {
                    updateLightLocked();
                }
            }
        } else if (turnOn) {
            if (z) {
                Slog.d(TAG, "showBootEffect -> nativeSetLight");
            }
            mColor = mEffects[12].color;
            mMode = mEffects[12].mode;
            mRate = mEffects[12].rate;
            mBootLightStartTime = System.currentTimeMillis();
            nativeSetLight(mMode, mColor, mRate, DEFAULT_LED_STATES);
        } else {
            float halfBreathingDuration = AuraLightManager.BREATHING_DURATIONS[Math.abs(-2)] / 2.0f;
            float waitingTime = halfBreathingDuration - ((System.currentTimeMillis() - mBootLightStartTime) % halfBreathingDuration);
            mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    showBootEffectAuraLightService();
                }
            }, (long)(waitingTime));
        }
    }

    public void showBootEffectAuraLightService() {
        if (mBootCompleted) {
            updateSuspensionRelatedScenarioStatus();
            synchronized (mLock) {
                updateLightLocked();
            }
            uploadSystemLightAnalytics(12);
            return;
        }
        if (DEBUG_ANALYTICS) {
            Slog.d(TAG, "showBootEffect -> nativeSetLight");
        }
        nativeSetLight(1, mColor, mRate, DEFAULT_LED_STATES);
    }

    private void showGameAppsLaunchEffect() {
        synchronized (mLock) {
            this.mStatus[11] = true;
            int scenario = getLightScenarioLocked();
            if (scenario == 11) {
                mHandler.removeMessages(6);
                mHandler.removeMessages(7);
                mHandler.removeMessages(14);
                Message msg = mHandler.obtainMessage(14, GAME_APPS_LAUNCH_EFFECT);
                msg.arg1 = scenario;
                mHandler.sendMessageDelayed(msg, GAME_APPS_LAUNCH_EFFECT_DELAY);
            } else {
                updateLightLocked();
            }
            this.mStatus[11] = false;
        }
    }

    private void showPowerConnectedEffect() {
        synchronized (mLock) {
            this.mStatus[15] = true;
            int scenario = getLightScenarioLocked();
            if (scenario == 15) {
                mHandler.removeMessages(6);
                mHandler.removeMessages(7);
                mHandler.removeMessages(14);
                Message msg = mHandler.obtainMessage(14, POWER_CONNECTED_EFFECT);
                msg.arg1 = scenario;
                msg.sendToTarget();
            } else {
                refreshLedLight();
            }
            this.mStatus[15] = false;
        }
    }

    private void showBumperInstalledEffect() {
        boolean z = DEBUG_ANALYTICS;
        if (z) {
            Slog.d(TAG, "showBumperInstalledEffect");
        }
        synchronized (mLock) {
            this.mStatus[14] = true;
            int scenario = getLightScenarioLocked();
            if (z) {
                Slog.d(TAG, "showBumperInstalledEffect: scenario=" + scenario);
            }
            if (scenario == 14) {
                mHandler.removeMessages(6);
                mHandler.removeMessages(7);
                mHandler.removeMessages(14);
                Message msg = mHandler.obtainMessage(14, "03".equals(mCurrentBumperInfo.lightId) ? BUMPER_INSTALL_EFFECT_03 : BUMPER_INSTALL_EFFECT_02);
                msg.arg1 = scenario;
                msg.sendToTarget();
            }
            this.mStatus[14] = false;
        }
    }

    private int simulateLedColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int r2 = (int) (r * SmoothStep(r / 255.0d));
        int g2 = (int) (g * SmoothStep(g / 255.0d));
        return (r2 << 16) | (g2 << 8) | ((int) (b * SmoothStep(b / 255.0d)));
    }

    private double SmoothStep(double x) {
        if (x <= 0.0d) {
            return 0.0d;
        }
        if (x >= 1.0d) {
            return 1.0d;
        }
        return ((3.0d * x) * x) - (((2.0d * x) * x) * x);
    }

    private void updateDockLedDurationLocked() {
        long now = SystemClock.elapsedRealtime();
        long duration = now - mDockLedChangeTime;
        int dockIdx = 0;
        switch (mDockState) {
            case 6:
                dockIdx = 1;
                break;
            case 7:
                dockIdx = 2;
                break;
            case 8:
                dockIdx = 3;
                break;
        }
        long[] jArr = mDockDuration;
        jArr[dockIdx] = jArr[dockIdx] + duration;
        if (mColor != 0) {
            long[] jArr2 = mDockLedOnDuration;
            jArr2[dockIdx] = jArr2[dockIdx] + duration;
        }
        mDockLedChangeTime = now;
    }

    private void updateLightLocked() {
        BlendedLightEffect blendedLightEffect;
        boolean z = DEBUG_ANALYTICS;
        if (z) {
            Slog.d(TAG, "updateLightLocked");
        }
        int scenario = getLightScenarioLocked();
        LightEffect current = scenario < 0 ? new LightEffect(true, 0, 0, 0) : mEffects[scenario];
        if (z) {
            Slog.d(TAG, "updateLightLocked: scenario=" + scenario + ", color=" + current.color + ", mode=" + current.mode + ", rate=" + current.rate);
            Slog.d(TAG, "updateLightLocked: mScenario=" + mScenario + ", mColor=" + mColor + ", mMode=" + mMode + ", mRate=" + mRate);
        }
        if (scenario == 1 || mScenario != scenario || mColor != current.color || mMode != current.mode || mRate != current.rate || ((mBlendedEffect == null && current.blendedEffect != null) || ((blendedLightEffect = mBlendedEffect) != null && !blendedLightEffect.equals(current.blendedEffect)))) {
            setLightLocked(scenario, current.color, current.mode, current.rate, current.blendedEffect);
        }
    }

    private void updateSuspensionRelatedScenarioStatus() {
        mHandler.sendEmptyMessage(5);
    }

    private void writeLightSettings() {
        synchronized (mLock) {
            if (!mLightSettingsChanged) {
                return;
            }
            FileOutputStream stream = null;
            try {
                stream = mAuraLightFile.startWrite();
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(stream, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_AURA_LIGHT_SETTING);
                fastXmlSerializer.attribute(null, "enabled", Boolean.toString(mEnabled));
                fastXmlSerializer.attribute(null, ATTR_AURA_LIGHT_SETTING_VERSION, Integer.toString(2));
                for (int i = 0; i < mEffects.length; i++) {
                    if (i != 0 && i != 1 && i != 2 && i != 3) {
                        fastXmlSerializer.startTag(null, TAG_AURA_LIGHT_SCENARIO);
                        fastXmlSerializer.attribute(null, "type", Integer.toString(i));
                        fastXmlSerializer.attribute(null, "active", Boolean.toString(mEffects[i].active));
                        fastXmlSerializer.attribute(null, ATTR_AURA_LIGHT_SCENARIO_COLOR, Integer.toString(mEffects[i].color));
                        fastXmlSerializer.attribute(null, "mode", Integer.toString(mEffects[i].mode));
                        fastXmlSerializer.attribute(null, ATTR_AURA_LIGHT_SCENARIO_RATE, Integer.toString(mEffects[i].rate));
                        if (mEffects[i].blendedEffect != null) {
                            fastXmlSerializer.startTag(null, TAG_AURA_LIGHT_BLENDED);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(mEffects[i].blendedEffect.mode));
                            fastXmlSerializer.attribute(null, ATTR_AURA_LIGHT_SCENARIO_RATE, Integer.toString(mEffects[i].blendedEffect.rate));
                            for (int j = 0; j < 6; j++) {
                                String colorAttr = ATTR_AURA_LIGHT_SCENARIO_COLOR + j;
                                fastXmlSerializer.attribute(null, colorAttr, Integer.toString(mEffects[i].blendedEffect.colors[j]));
                            }
                            fastXmlSerializer.endTag(null, TAG_AURA_LIGHT_BLENDED);
                        }
                        fastXmlSerializer.endTag(null, TAG_AURA_LIGHT_SCENARIO);
                    }
                }
                fastXmlSerializer.startTag(null, TAG_BUMPER_SETTINGS);
                fastXmlSerializer.attribute(null, "state", Integer.toString(mBumperState));
                fastXmlSerializer.endTag(null, TAG_BUMPER_SETTINGS);
                fastXmlSerializer.endTag(null, TAG_AURA_LIGHT_SETTING);
                fastXmlSerializer.endDocument();
                mAuraLightFile.finishWrite(stream);
                mLightSettingsChanged = false;
                BackupManager.dataChanged(mContext.getPackageName());
            } catch (Exception e) {
                Slog.w(TAG, "Failed to save AuraLight file, restoring backup, err: " + e.getMessage());
                mAuraLightFile.failWrite(stream);
            }
        }
    }

    private void writeNotificationSettings() {
        synchronized (mLock) {
            if (!mNotificationSettingsChanged) {
                return;
            }
            FileOutputStream stream = null;
            try {
                stream = mNotificationFile.startWrite();
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(stream, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_AURA_NOTIFICATION_SETTING);
                for (String pkg : mNotificationEffects.keySet()) {
                    LightEffect effect = mNotificationEffects.get(pkg);
                    if (effect != null) {
                        fastXmlSerializer.startTag(null, "custom");
                        fastXmlSerializer.attribute(null, "package", pkg);
                        fastXmlSerializer.attribute(null, "active", Boolean.toString(effect.active));
                        fastXmlSerializer.attribute(null, ATTR_AURA_LIGHT_SCENARIO_COLOR, Integer.toString(effect.color));
                        fastXmlSerializer.attribute(null, "mode", Integer.toString(effect.mode));
                        fastXmlSerializer.attribute(null, ATTR_AURA_LIGHT_SCENARIO_RATE, Integer.toString(effect.rate));
                        if (effect.blendedEffect != null) {
                            fastXmlSerializer.startTag(null, TAG_AURA_LIGHT_BLENDED);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(effect.blendedEffect.mode));
                            fastXmlSerializer.attribute(null, ATTR_AURA_LIGHT_SCENARIO_RATE, Integer.toString(effect.blendedEffect.rate));
                            for (int j = 0; j < 6; j++) {
                                String colorAttr = ATTR_AURA_LIGHT_SCENARIO_COLOR + j;
                                fastXmlSerializer.attribute(null, colorAttr, Integer.toString(effect.blendedEffect.colors[j]));
                            }
                            fastXmlSerializer.endTag(null, TAG_AURA_LIGHT_BLENDED);
                        }
                        fastXmlSerializer.endTag(null, "custom");
                    }
                }
                fastXmlSerializer.endTag(null, TAG_AURA_NOTIFICATION_SETTING);
                fastXmlSerializer.endDocument();
                mNotificationFile.finishWrite(stream);
                mNotificationSettingsChanged = false;
                BackupManager.dataChanged(mContext.getPackageName());
            } catch (Exception e) {
                Slog.w(TAG, "Failed to save AuraNotification file, restoring backup, err: " + e.getMessage());
                mNotificationFile.failWrite(stream);
            }
        }
    }

    private void writeSettings() {
        writeLightSettings();
        writeNotificationSettings();
    }

    private ValueAnimator getCpuEffectAnimator(final int scenario, final int mode, final int color, final int rate, int animatorDuration) {
        int duration;
        LightEffect[] lightEffectArr = mEffects;
        if (scenario >= lightEffectArr.length || !lightEffectArr[scenario].active) {
            return null;
        }
        ValueAnimator.AnimatorUpdateListener updateListener = null;
        if (mode == 2) {
            duration = AuraLightManager.BREATHING_DURATIONS[Math.abs(rate)];
            updateListener = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float alpha = (Math.abs(((Float) valueAnimator.getAnimatedValue()).floatValue() - 0.5f) * 2.95f) - 0.2375f;
                    if (alpha > 1.0f) {
                        alpha = 1.0f;
                    }
                    if (alpha < 0.0f) {
                        alpha = 0.0f;
                    }
                    int[] rgb = new int[3];
                    int i = color;
                    rgb[0] = (i >> 16) & 255;
                    rgb[1] = (i >> 8) & 255;
                    rgb[2] = i & 255;
                    for (int i2 = 0; i2 < rgb.length; i2++) {
                        rgb[i2] = (int) (rgb[i2] * alpha);
                    }
                    int i3 = rgb[0];
                    int colorShift = (i3 << 16) | (rgb[1] << 8) | rgb[2];
                    synchronized (mLock) {
                        AuraLightService auraLightService = AuraLightService.this;
                        auraLightService.setScenarioEffectLocked(scenario, auraLightService.mEffects[scenario].active, colorShift, 1, 0);
                    }
                }
            };
        } else if (mode == 3) {
            duration = AuraLightManager.STROBING_DURATIONS[Math.abs(rate)];
            updateListener = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    int colorShift = color * (value < 0.5f ? 1 : 0);
                    synchronized (mLock) {
                        AuraLightService auraLightService = AuraLightService.this;
                        auraLightService.setScenarioEffectLocked(scenario, auraLightService.mEffects[scenario].active, colorShift, 1, 0);
                    }
                }
            };
        } else if (mode != 1 && mode != 4) {
            return null;
        } else {
            duration = animatorDuration;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(animatorDuration <= 0 ? -1 : (int) Math.ceil(animatorDuration / duration));
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator2) {
                synchronized (mLock) {
                    if (mode == 4) {
                        AuraLightService auraLightService = AuraLightService.this;
                        auraLightService.setScenarioEffectLocked(scenario, auraLightService.mEffects[scenario].active, color, 4, rate);
                    } else {
                        AuraLightService auraLightService2 = AuraLightService.this;
                        auraLightService2.setScenarioEffectLocked(scenario, auraLightService2.mEffects[scenario].active, color, 1, 0);
                    }
                }
            }

            @Override
            public void onAnimationEnd(Animator animator2) {
            }

            @Override
            public void onAnimationCancel(Animator animator2) {
            }

            @Override
            public void onAnimationRepeat(Animator animator2) {
            }
        });
        if (updateListener != null) {
            animator.addUpdateListener(updateListener);
        }
        return animator;
    }

    private void updateSyncDelay() {
        if (mDockState == 8 && mHeadsetSyncable && !mAttachedHeadsetPids.isEmpty()) {
            mSyncDelay = SYNC_DELAY_WITH_DT_HEADSET;
        } else {
            mSyncDelay = SYNC_DELAY;
        }
    }

    private void updateLedState() {
        int i = mLedStatesRecord;
        mCurrentLedStates = i;
        int i2 = mDockState;
        if (i2 == 6 || i2 == 7 || i2 == 8 || mBumperState == 1) {
            mCurrentLedStates = i & (-2);
        }
        if (mIpLightEnabled) {
            mCurrentLedStates |= 4;
        } else {
            mCurrentLedStates &= -5;
        }
        if (!mIsGameViceConnected) {
            mCurrentLedStates &= -65537;
        }
        if (i2 != 6) {
            int i3 = mCurrentLedStates & (-17);
            mCurrentLedStates = i3;
            mCurrentLedStates = i3 & (-33);
        }
        if (i2 != 7) {
            int i4 = mCurrentLedStates & (-257);
            mCurrentLedStates = i4;
            mCurrentLedStates = i4 & (-513);
        }
        if (i2 != 8) {
            mCurrentLedStates &= -4097;
        }
        if (mBootCompleted) {
            refreshLedLight();
        }
    }

    private void updateLidState() {
        mInputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
        boolean lidOpen = true;
        if (mInputManager.SWITCH_STATE_ON == 1) {
            lidOpen = false;
        }
        mLocalService.notifyLidSwitchChanged(System.currentTimeMillis(), lidOpen);
    }

    private void updateSystemEffectState(boolean bumperStateChanged) {
        boolean z = DEBUG_ANALYTICS;
        if (z) {
            Slog.d(TAG, "updateSystemEffectState: bumperStateChanged=" + bumperStateChanged);
        }
        if (isAllScenarioDisabled()) {
            if (z) {
                Slog.d(TAG, "updateSystemEffectState: set enable_system_effect as -1");
            }
            Settings.System.putInt(mContext.getContentResolver(), "enable_system_effect", -1);
        } else if (bumperStateChanged) {
            if (z) {
                Slog.d(TAG, "updateSystemEffectState: set enable_system_effect as 1");
            }
            Settings.System.putInt(mContext.getContentResolver(), "enable_system_effect", mSystemEffectEnabledByUser ? 1 : 0);
        }
    }

    private boolean isAllScenarioDisabled() {
        int[] scenarios;
        if (mBumperState == 1) {
            scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_CONNECTED;
        } else {
            scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_DISCONNECTED;
        }
        for (int scenario : scenarios) {
            if (scenario == 2 || scenario == 3) {
                if (this.mStatus[scenario]) {
                    return false;
                }
            } else if (scenario == 12) {
                if (SystemProperties.getInt(PROP_BOOTING_EFFECT, 2) != 0) {
                    return false;
                }
            } else if (scenario == 14) {
                if (Settings.System.getInt(mContext.getContentResolver(), SETTINGS_SYSTEM_BUMPER_CONNECTED_EFFECT, 1) == 1) {
                    return false;
                }
            } else if (mEffects[scenario].active) {
                return false;
            }
        }
        return true;
    }

    private void resetActiveStateIfNeed() {
        int[] scenarios;
        if (!isAllScenarioDisabled()) {
            return;
        }
        if (mBumperState == 1) {
            scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_CONNECTED;
        } else {
            scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_DISCONNECTED;
        }
        synchronized (mLock) {
            for (int scenario : scenarios) {
                mEffects[scenario].active = SCENARIOS_ACTIVE_STATE_DEFAULT[scenario];
                if (mEffects[scenario].active) {
                    if (scenario == 12) {
                        setSystemPropertiesNoThrow(PROP_BOOTING_EFFECT, "2");
                    } else if (scenario == 14) {
                        Settings.System.putInt(mContext.getContentResolver(), SETTINGS_SYSTEM_BUMPER_CONNECTED_EFFECT, 1);
                    }
                }
            }
        }
        scheduleWriteSettings();
    }

    private void resetCetraRGBScenarios() {
        int[] scenarios;
        synchronized (mLock) {
            if (mBumperState == 1) {
                scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_CONNECTED;
            } else {
                scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_DISCONNECTED;
            }
            for (int scenario : scenarios) {
                LightEffect effect = mEffects[scenario];
                if (effect.mode == 3 || effect.mode >= 5) {
                    LightEffect restoreEffect = new LightEffect(effect.active, effect.color, effect.mode, effect.rate);
                    mRestoreCetraEffect.put(Integer.valueOf(scenario), restoreEffect);
                    setScenarioEffectLocked(scenario, effect.active, effect.color, 1, effect.rate);
                }
            }
        }
    }

    private void restoreCetraRGBScenarios() {
        synchronized (mLock) {
            for (Map.Entry<Integer, LightEffect> mentry : mRestoreCetraEffect.entrySet()) {
                LightEffect effect = mentry.getValue();
                setScenarioEffectLocked(mentry.getKey().intValue(), effect.active, effect.color, effect.mode, effect.rate);
            }
            mRestoreCetraEffect.clear();
        }
    }

    private void resetStorageScenarios() {
        allScenariosRateToSlow();
    }

    private void allScenariosRateToSlow() {
        int[] scenarios;
        synchronized (mLock) {
            if (mBumperState == 1) {
                scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_CONNECTED;
            } else {
                scenarios = SCENARIOS_ALLOW_WHEN_BUMPER_DISCONNECTED;
            }
            for (int scenario : scenarios) {
                LightEffect effect = mEffects[scenario];
                if (effect.rate != 0) {
                    setScenarioEffectLocked(scenario, effect.active, effect.color, effect.mode, 0);
                }
            }
        }
    }

    private void handleLidSwitchChangedLocked(boolean lidOpen) {
        int state = lidOpen ? 3 : 0;
        if (state == 3) {
            if (mBumperState == 1) {
                updateBumperStateLocked(2);
            }
            enableNfcTypeV(true);
        }
        updateBumperStateLocked(state);
        if (state == 0) {
            if (mCurrentBumperInfo != null) {
                updateBumperStateLocked(1);
            } else if (!mKeyguardShowing) {
                mHandler.sendEmptyMessage(11);
            }
        }
    }

    private void handleNfcTagDiscoveredLocked() {
        if (isJdBumper()) {
            notifyBumperDetectedLocked();
        } else if (mBumperState == 0) {
            updateBumperStateLocked(1);
        }
    }

    private void handleUpdateNotificationLightLocked(List<StatusBarNotification> sbns) {
        if (!mEffects[7].active) {
            cancelStopNotification();
            setScenarioStatusLocked(7, false);
            return;
        }
        StatusBarNotification sbnToApply = null;
        boolean applyToAll = mNotificationEffects.get("!default").active;
        long now = System.currentTimeMillis();
        for (StatusBarNotification sbn : sbns) {
            if (mNotificationExpirationTime <= 0 || now - sbn.getPostTime() < mNotificationExpirationTime) {
                if (applyToAll || mNotificationEffects.get(sbn.getPackageName()) != null) {
                    if (sbnToApply == null || sbn.getPostTime() > sbnToApply.getPostTime()) {
                        sbnToApply = sbn;
                    }
                }
            }
        }
        if (sbnToApply == null) {
            cancelStopNotification();
            setScenarioStatusLocked(7, false);
            return;
        }
        LightEffect effect = applyToAll ? mNotificationEffects.get("!default") : mNotificationEffects.get(sbnToApply.getPackageName());
        setScenarioEffectLocked(7, true, effect.color, effect.mode, effect.rate);
        setScenarioStatusLocked(7, true);
        if (mNotificationExpirationTime > 0) {
            long delay = (sbnToApply.getPostTime() + mNotificationExpirationTime) - System.currentTimeMillis();
            scheduleStopNotification(delay);
            return;
        }
        cancelStopNotification();
    }

    private void scheduleStopNotification(long delay) {
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 3, new Intent(ACTION_STOP_NOTIFICATION).addFlags(268435456), 201326592);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService("alarm");
        if (alarmManager != null) {
            alarmManager.cancel(pi);
            alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + delay, pi);
        }
    }

    private void cancelStopNotification() {
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 3, new Intent(ACTION_STOP_NOTIFICATION).addFlags(268435456), 201326592);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService("alarm");
        if (alarmManager != null) {
            alarmManager.cancel(pi);
        }
    }

    private boolean isJdBumper() {
        BumperInfo bumperInfo = mCurrentBumperInfo;
        return bumperInfo != null && "01".equals(bumperInfo.vendorId) && "02".equals(mCurrentBumperInfo.gameId);
    }

    private void notifyBumperDetectedLocked() {
        JSONObject tagJsonObj = new JSONObject();
        JSONObject imeiJsonObj = new JSONObject();
        if (!fillTagInfo(tagJsonObj) || !fillImeiInfo(imeiJsonObj)) {
            return;
        }
        Intent imeiIntent = new Intent("com.tencent.inlab.tcsystem.solarcore");
        imeiIntent.setPackage("com.tencent.inlab.tcsystem");
        imeiIntent.putExtra("notify", imeiJsonObj.toString());
        sendBroadcast(imeiIntent, "com.tencent.inlab.tcsystem.permission.SOLARCORE");
        Intent tagIntent = new Intent("com.tencent.inlab.tcsystem.solarcore");
        tagIntent.setPackage("com.tencent.inlab.tcsystem");
        tagIntent.putExtra("notify", tagJsonObj.toString());
        sendBroadcast(tagIntent, "com.tencent.inlab.tcsystem.permission.SOLARCORE");
    }

    private void updateBumperStateLocked(int state) {
        boolean z = DEBUG_ANALYTICS;
        if (z) {
            Slog.d(TAG, "updateBumperStateLocked: state=" + state + ", mBumperState=" + mBumperState);
        }
        if (state == mBumperState) {
            return;
        }
        int prevState = mBumperState;
        if (prevState == 3) {
            if (state != 2) {
                mBumperState = state;
            }
        } else if (prevState == 0) {
            mBumperState = state;
        } else if ((prevState == 1 || prevState == 2) && state != 0) {
            mBumperState = state;
        }
        if (z) {
            Slog.d(TAG, "updateBumperStateLocked: prevState=" + prevState + ", mBumperState=" + mBumperState);
        }
        int i = mBumperState;
        if (i != prevState) {
            boolean z2 = false;
            if (i == 1) {
                enableIpLight(true);
            } else if (i == 3) {
                enableIpLight(false);
                mCurrentBumperInfo = null;
                this.mBumperId = null;
            }
            int i2 = mBumperState;
            if (i2 == 1 || i2 == 2) {
                updateSystemEffectState(true);
            }
            if (mBumperState != 1) {
                mIsInboxAndBumperConnected = false;
            }
            Intent intent = new Intent("asus.rog.intent.action.BUMPER_STATE_CHANGED");
            intent.putExtra("asus.rog.extra.STATE", mBumperState);
            boolean isRogBumper = false;
            if (mCurrentBumperInfo != null) {
                if (mBumperState == 1 && fillTagInfo(intent)) {
                    enableNfcTypeV(false);
                    String vendorId = intent.getStringExtra("asus.rog.extra.VENDOR_ID");
                    try {
                        if (Integer.parseInt(vendorId) == 2) {
                            z2 = true;
                        }
                        isRogBumper = z2;
                    } catch (Exception e) {
                        if (DEBUG_ANALYTICS) {
                            Slog.w(TAG, "Failed to parseInt, err: " + e.getMessage());
                        }
                    }
                    boolean z3 = DEBUG_ANALYTICS;
                    if (z3) {
                        Slog.w(TAG, "updateBumperStateLocked: vendorId=" + vendorId + ", isRogBumper=" + isRogBumper);
                    }
                    if (isRogBumper) {
                        showBumperInstalledEffect();
                        Intent themeNotifier = new Intent("com.asus.themeapp.BUMPER_DETECTED");
                        themeNotifier.putExtra("bumper_state", 1);
                        themeNotifier.putExtra("vendor_id", vendorId);
                        themeNotifier.putExtra("theme_id", intent.getStringExtra("asus.rog.extra.THEME_ID"));
                        themeNotifier.addFlags(16777216);
                        sendBroadcast(themeNotifier, "com.asus.permission.BUMPER");
                        if (z3) {
                            Slog.d(TAG, "updateBumperStateLocked: Send broadcast to Theme app");
                        }
                    }
                }
            } else if (z) {
                Slog.d(TAG, "updateBumperStateLocked: mCurrentBumperInfo is null");
            }
            if (!isRogBumper) {
                if (DEBUG_ANALYTICS) {
                    Slog.d(TAG, "updateBumperStateLocked: Send asus.rog.intent.action.BUMPER_STATE_CHANGED broadcast");
                }
                intent.addFlags(16777216);
                sendBroadcast(intent, "com.asus.permission.BUMPER");
            }
            mLightSettingsChanged = true;
            scheduleWriteSettings();
        }
    }

    private boolean fillTagInfo(Intent intent) {
        BumperInfo bumperInfo;
        if (intent == null || (bumperInfo = mCurrentBumperInfo) == null) {
            if (DEBUG_ANALYTICS) {
                Slog.d(TAG, "fillTagInfo: intent is null or mCurrentBumperInfo is null");
                return false;
            }
            return false;
        }
        intent.putExtra("asus.rog.extra.ID", bumperInfo.bumperId);
        intent.putExtra("asus.rog.extra.VENDOR_ID", mCurrentBumperInfo.vendorId);
        intent.putExtra("asus.rog.extra.GAME_ID", mCurrentBumperInfo.gameId);
        intent.putExtra("asus.rog.extra.CHARACTER_ID", mCurrentBumperInfo.characterId);
        intent.putExtra("asus.rog.extra.UID", mCurrentBumperInfo.uid);
        intent.putExtra("asus.rog.extra.LIGHT_ID", mCurrentBumperInfo.lightId);
        intent.putExtra("asus.rog.extra.THEME_ID", mCurrentBumperInfo.themeId);
        if (DEBUG_ANALYTICS) {
            Slog.d(TAG, "fillTagInfo: bumperId=" + mCurrentBumperInfo.bumperId + ", vendorId=" + mCurrentBumperInfo.vendorId + ", gameId=" + mCurrentBumperInfo.gameId + ", characterId=" + mCurrentBumperInfo.characterId + ", uid=" + mCurrentBumperInfo.uid + ", lightId=" + mCurrentBumperInfo.lightId + ", themeId=" + mCurrentBumperInfo.themeId);
            return true;
        }
        return true;
    }

    private boolean fillTagInfo(JSONObject jsonObj) {
        if (jsonObj == null || mCurrentBumperInfo == null) {
            return false;
        }
        try {
            jsonObj.put("method", "notify");
            jsonObj.put("field", "NFCDevice");
            JSONObject params = new JSONObject();
            params.put("uid", mCurrentBumperInfo.bumperId);
            params.put("game_id", mCurrentBumperInfo.gameId);
            params.put("theme_id", mCurrentBumperInfo.themeId);
            params.put("character_id", mCurrentBumperInfo.characterId);
            params.put("vendor_id", mCurrentBumperInfo.vendorId);
            params.put("bumper_state", 1);
            jsonObj.put("params", params);
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Fill tag info failed, err: " + e.getMessage());
            return false;
        }
    }

    private boolean fillImeiInfo(JSONObject jsonObj) {
        TelephonyManager telephonyManager;
        if (jsonObj != null && (telephonyManager = (TelephonyManager) mContext.getSystemService("phone")) != null) {
            String imeiStr = telephonyManager.getImei();
            try {
                jsonObj.put("method", "notify");
                jsonObj.put("field", "Properity");
                JSONObject params = new JSONObject();
                params.put("IMEI", imeiStr);
                jsonObj.put("result", params);
                jsonObj.put("return_code", "0");
                jsonObj.put("return_msg", "");
                return true;
            } catch (Exception e) {
                Slog.w(TAG, "Fill IMEI info failed, err: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean extractTagInfo(Tag tag) {
        char c;
        char c2 = 0;
        if (tag == null) {
            if (DEBUG_ANALYTICS) {
                Slog.w(TAG, "extractTagInfo: tag is null");
            }
            return false;
        }
        BumperInfo bumperInfo = new BumperInfo();
        this.mBumperId = tag.getId();
        String bumperId = String.format("%0" + (this.mBumperId.length * 2) + "X", new BigInteger(1, this.mBumperId));
        bumperInfo.bumperId = bumperId;
        NdefMessage ndefMesg = extractMessage(tag);
        if (ndefMesg == null) {
            if (DEBUG_ANALYTICS) {
                Slog.w(TAG, "extractTagInfo: ndefMesg is null");
            }
            return false;
        }
        NdefRecord[] ndefRecords = ndefMesg.getRecords();
        if (ndefRecords == null || ndefRecords.length == 0) {
            if (DEBUG_ANALYTICS) {
                Slog.w(TAG, "extractTagInfo: ndefRecords is null or ndefRecords.length = 0");
                return false;
            }
            return false;
        }
        boolean validUri = false;
        boolean validConfig = false;
        int length = ndefRecords.length;
        int i = 0;
        while (i < length) {
            NdefRecord ndefRecord = ndefRecords[i];
            String recTypes = new String(ndefRecord.getType(), StandardCharsets.UTF_8);
            if ("U".equals(recTypes.trim().toUpperCase())) {
                String mesg = new String(ndefRecord.getPayload(), StandardCharsets.UTF_8);
                if (BUMPER_URI.equals(mesg)) {
                    validUri = true;
                }
                c = 2;
            } else if (!"T".equals(recTypes.trim().toUpperCase())) {
                c = 2;
            } else {
                String mesg2 = new String(ndefRecord.getPayload(), StandardCharsets.UTF_8);
                String[] configs = getConfigs(bumperId, mesg2);
                if (configs == null) {
                    c = 2;
                } else {
                    bumperInfo.vendorId = configs[c2];
                    bumperInfo.gameId = configs[1];
                    c = 2;
                    bumperInfo.characterId = configs[2];
                    bumperInfo.uid = configs[3];
                    bumperInfo.lightId = configs[4];
                    bumperInfo.themeId = configs[5];
                    validConfig = true;
                }
            }
            i++;
            c2 = 0;
        }
        if (validUri & validConfig) {
            mCurrentBumperInfo = bumperInfo;
            return true;
        } else if (DEBUG_ANALYTICS) {
            Slog.d(TAG, "extractTagInfo: validUri=" + validUri + ", validConfig=" + validConfig);
            return false;
        } else {
            return false;
        }
    }

    private NdefMessage extractMessage(Tag tag) {
        byte[] tagId = tag.getId();
        NfcV nfcV = NfcV.get(tag);
        if (nfcV == null) {
            return null;
        }
        loginNfcV(nfcV, tagId);
        byte[] data = getData(nfcV, tagId);
        try {
            byte[] trimData = trimNfcVData(data);
            return new NdefMessage(trimData);
        } catch (Exception e) {
            Slog.w(TAG, "Extract NdefMessage failed, err: " + e.getMessage());
            return null;
        }
    }

    private String[] getConfigs(String tagId, String message) {
        try {
            byte[] rawData = Base64.getDecoder().decode(message);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(tagId.toCharArray(), nativeGetTs(), ITERATION_COUNT, 128);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(2, secret, new IvParameterSpec(nativeGetTIv()));
            String content = new String(cipher.doFinal(rawData), StandardCharsets.UTF_8);
            if (DEBUG_ANALYTICS) {
                Slog.d(TAG, "getConfigs: content=" + content);
            }
            String[] configs = content.split(",");
            if (configs.length == 6) {
                return configs;
            }
            return null;
        } catch (Exception e) {
            Slog.w(TAG, "Get content failed, err: " + e.getMessage());
            return null;
        }
    }

    private byte[] getData(NfcV nfcV, byte[] tagId) {
        byte[] cmd = {32, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16};
        System.arraycopy(tagId, 0, cmd, 2, 8);
        return runNciCommand(nfcV, tagId, cmd);
    }

    private String getPwd(String tagId) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(tagId.toCharArray(), nativeGetPs(), ITERATION_COUNT, 30);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
            return Base64.getEncoder().encodeToString(secret.getEncoded());
        } catch (Exception e) {
            Slog.w(TAG, "Get pwd failed, err: " + e.getMessage());
            return "";
        }
    }

    private byte[] getRandomNum(NfcV nfcV, byte[] tagId) {
        byte[] cmd = {32, -78, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(tagId, 6, cmd, 2, 1);
        System.arraycopy(tagId, 0, cmd, 3, 8);
        byte[] response = runNciCommand(nfcV, tagId, cmd);
        byte[] randomNum = {response[1], response[2], response[1], response[2]};
        return randomNum;
    }

    private void loginNfcV(NfcV nfcV, byte[] tagId) {
        String tagIdStr = String.format("%0" + (tagId.length * 2) + "X", new BigInteger(1, tagId));
        byte[] randomNum = getRandomNum(nfcV, tagId);
        if (randomNum == null) {
            return;
        }
        String pwdStr = getPwd(tagIdStr);
        byte[] pwd = pwdStr.getBytes();
        byte[] xorPwd = {(byte) ((randomNum[0] & 255) ^ pwd[0]), (byte) ((randomNum[1] & 255) ^ pwd[1]), (byte) ((randomNum[2] & 255) ^ pwd[2]), (byte) ((randomNum[3] & 255) ^ pwd[3])};
        setPwd(nfcV, tagId, xorPwd);
    }

    private void setPwd(NfcV nfcV, byte[] tagId, byte[] pwd) {
        byte[] cmd = {32, -77, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, pwd[0], pwd[1], pwd[2], pwd[3]};
        System.arraycopy(tagId, 6, cmd, 2, 1);
        System.arraycopy(tagId, 0, cmd, 3, 8);
        runNciCommand(nfcV, tagId, cmd);
    }

    private byte[] trimNfcVData(byte[] data) {
        byte[] trimData = new byte[data.length - 1];
        System.arraycopy(data, 1, trimData, 0, trimData.length);
        int i = trimData.length - 1;
        while (i >= 0 && trimData[i] == 0) {
            i--;
        }
        return Arrays.copyOf(trimData, i + 1);
    }

    private byte[] runNciCommand(NfcV nfcV, byte[] tagId, byte[] cmd) {
        try {
            try {
                nfcV.connect();
                byte[] response = nfcV.transceive(cmd);
                try {
                    nfcV.close();
                } catch (Exception e) {
                }
                return response;
            } catch (Exception e2) {
                Slog.w(TAG, "Run NCI command failed, err: " + e2.getMessage());
                try {
                    nfcV.close();
                } catch (Exception e3) {
                }
                return tagId;
            }
        } catch (Throwable th) {
            try {
                nfcV.close();
            } catch (Exception e4) {
            }
            throw th;
        }
    }

    private void enableIpLight(boolean enable) {
        if (DEBUG_ANALYTICS) {
            Slog.d(TAG, "enableIpLight: enable=" + enable + ", mCameraMonitor.canUseIpLight()=" + mCameraMonitor.canUseIpLight());
        }
        boolean z = enable && mCameraMonitor.canUseIpLight();
        mIpLightEnabled = z;
        setSystemPropertiesNoThrow(PROP_BUMPER_ENABLED, z ? "1" : "0");
        updateLedState();
    }

    private void enableNfcTypeV(boolean enable) {
        setSystemPropertiesNoThrow(PROP_NFC_MODE, enable ? "0" : "1");
        NfcAdapter adapter = null;
        try {
            adapter = NfcAdapter.getNfcAdapter(mContext);
        } catch (Exception e) {
            Slog.w(TAG, "Get NfcAdapter failed when turning on NFC, err: " + e.getMessage());
        }
        if (adapter != null && adapter.isEnabled()) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                try {
                    data.writeInterfaceToken("android.nfc.INfcAdapter");
                    adapter.getService().asBinder().transact(1000, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            } catch (Exception e2) {
                Slog.w(TAG, "Notify NfcService failed, err: " + e2.getMessage());
            }
        }
    }

    private void disableMusicNotification() {
        PackageManager pm = mContext.getPackageManager();
        INotificationManager nm = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        try {
            int ownerUid = pm.getPackageUid(MUSIC_NOTIFICATION_OWNER, 0);
            NotificationChannel musicEffectChannel = nm.getNotificationChannelForPackage(MUSIC_NOTIFICATION_OWNER, ownerUid, MUSIC_NOTIFICATION_CHANNEL_ID, (String) null, true);
            if (musicEffectChannel != null && musicEffectChannel.getImportance() != 0) {
                musicEffectChannel.setImportance(0);
                musicEffectChannel.lockFields(4);
                nm.updateNotificationChannelForPackage(MUSIC_NOTIFICATION_OWNER, ownerUid, musicEffectChannel);
            }
        } catch (Exception e) {
        }
    }

    private int getChargingIndicatorColor(float percentage) {
        if (mChargingIndicatorPolicy == 0) {
            if (percentage < 1.0f) {
                return 16711680;
            }
            return 6141697;
        } else if (percentage > 0.8f) {
            return 6141697;
        } else {
            if (percentage <= 0.2f) {
                return 16711680;
            }
            return 16750848;
        }
    }

    private void uploadSystemLightAnalytics(int event) {
        int type = 1;
        if (mBumperState == 1) {
            type = 2;
        } else if (mDockState != 0) {
            type = 3;
        }
        String data = "type=" + type + " event=" + event + " timestamp=" + System.currentTimeMillis();
        Message msg = Message.obtain(mHandler, 16);
        msg.obj = data;
        msg.sendToTarget();
    }

    private void uploadCustomLightAnalytics(int type, String packageName, int color, int mode, int rate) {
        String data = "type=" + type + " package=" + packageName + " color=0x" + Integer.toHexString(color) + " mode=" + mode + " rate=" + rate + " timestamp=" + System.currentTimeMillis();
        Message msg = Message.obtain(mHandler, 17);
        msg.obj = data;
        msg.sendToTarget();
    }

    private void getCustomNotificationsInternal(List<String> pkgs) {
        if (pkgs == null) {
            return;
        }
        synchronized (mLock) {
            Set<String> pkgSet = mNotificationEffects.keySet();
            for (String pkg : pkgSet) {
                if (!"!default".equals(pkg)) {
                    pkgs.add(pkg);
                }
            }
        }
    }

    private long[] getDockLedOnStatisticInternal() {
        long[] copyOf;
        synchronized (mLock) {
            updateDockLedDurationLocked();
            long[] jArr = mDockLedOnDuration;
            copyOf = Arrays.copyOf(jArr, jArr.length);
        }
        return copyOf;
    }

    private long[] getDockStatisticInternal() {
        long[] copyOf;
        synchronized (mLock) {
            updateDockLedDurationLocked();
            long[] jArr = mDockDuration;
            copyOf = Arrays.copyOf(jArr, jArr.length);
        }
        return copyOf;
    }

    private int getFrameInternal() {
        boolean z = IS_ANAKIN;
        if (z && HAS_2ND_DISPLAY && !mAttachedHeadsetPids.isEmpty() && !mIsGameViceConnected && mDockState == 0) {
            return frameworkGetFrame();
        }
        if (z && mDockState == 6) {
            return frameworkGetFrame();
        }
        return nativeGetFrame();
    }

    private int frameworkGetFrame() {
        long now = SystemClock.elapsedRealtime();
        if (mScenarioEffectStartTime != 0) {
            double frameLength = AuraLightManager.getFrameLength(mMode, mRate);
            long cycleLength = (long) (AuraLightManager.getFrameCount(mMode) * frameLength);
            if (cycleLength > 60000) {
                cycleLength = 60000;
            }
            long timeOffset = (now - mScenarioEffectStartTime) % cycleLength;
            int frame = (int) (timeOffset / frameLength);
            if (DEBUG_ANALYTICS) {
                Slog.w(TAG, "frameworkGetFrame getframe = " + frame);
            }
            return frame;
        }
        return 0;
    }

    private int getLightScenarioInternal() {
        int lightScenarioLocked;
        synchronized (mLock) {
            lightScenarioLocked = getLightScenarioLocked();
        }
        return lightScenarioLocked;
    }

    private boolean getNotificationEffectInternal(String pkg, int[] output) {
        boolean z;
        try {
            synchronized (mLock) {
                LightEffect effect = mNotificationEffects.get(pkg);
                output[0] = effect.color;
                output[1] = effect.mode;
                output[2] = effect.rate;
                z = effect.active;
            }
            return z;
        } catch (Exception e) {
            Slog.w(TAG, "Get notification effect failed, err: " + e.getMessage());
            return false;
        }
    }

    private boolean getScenarioBlendedEffectInternal(int scenario, int[] output) {
        if (output != null) {
            try {
                if (output.length >= 8) {
                    if (scenario >= 0 && scenario < 16) {
                        synchronized (mLock) {
                            LightEffect effect = mEffects[scenario];
                            if (effect.blendedEffect == null) {
                                return false;
                            }
                            System.arraycopy(effect.blendedEffect.colors, 0, output, 0, effect.blendedEffect.colors.length);
                            output[6] = effect.blendedEffect.mode;
                            output[7] = effect.blendedEffect.rate;
                            return effect.blendedEffect.mode != 0;
                        }
                    }
                    for (int i = 0; i < 8; i++) {
                        output[i] = 0;
                    }
                    return false;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Get scenario effect failed, err: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    private boolean getScenarioEffectInternal(int scenario, int[] output) {
        boolean z;
        try {
            if (scenario < 0 || scenario >= 16) {
                output[2] = 0;
                output[1] = 0;
                output[0] = 0;
                return false;
            }
            synchronized (mLock) {
                LightEffect effect = mEffects[scenario];
                output[0] = effect.color;
                output[1] = effect.mode;
                output[2] = effect.rate;
                z = effect.active;
            }
            return z;
        } catch (Exception e) {
            Slog.w(TAG, "Get scenario effect failed, err: " + e.getMessage());
            return false;
        }
    }

    private void resetStatisticInternal() {
        synchronized (mLock) {
            mDockDuration = new long[]{0, 0, 0, 0};
            mDockLedOnDuration = new long[]{0, 0, 0, 0};
            mDockLedChangeTime = SystemClock.elapsedRealtime();
        }
    }

    private void setEnableInternal(boolean enabled) {
        synchronized (mLock) {
            boolean statusChanged = mEnabled != enabled;
            mEnabled = enabled;
            updateLightLocked();
            if (statusChanged) {
                mLightSettingsChanged = true;
                scheduleWriteSettings();
            }
        }
    }

    private void setFrameInternal(int frame) {
        nativeSetFrame(frame);
        if (mHeadsetSyncable) {
            mHeadsetController.requestSetFrame(frame);
        }
        sendBroadcast(new Intent("asus.intent.action.AURA_FRAME_CHANGED"), "com.asus.permission.MANAGE_AURA_LIGHT");
    }

    private void syncFrame() {
        int frameNum = getFrameInternal();
        setFrameInternal(frameNum);
        if (mEnabled && mScenario != -1) {
            if (mIsGameViceConnected || mIsInboxAndBumperConnected || (mHeadsetSyncable && !mAttachedHeadsetPids.isEmpty())) {
                if (mHeadsetSyncable && !mAttachedHeadsetPids.isEmpty() && !mScreenOn && mEffects[10].active && mSystemEffectEnabled) {
                    scheduleSyncFrame();
                    return;
                }
                mHandler.removeMessages(21);
                mHandler.sendEmptyMessageDelayed(21, mSyncDelay);
            }
        }
    }

    private void setCustomEffectInternal(int targetLights, List<AuraLightEffect> effects) {
        mHandler.removeMessages(6);
        mHandler.removeMessages(7);
        Message msg = mHandler.obtainMessage(6, effects);
        int i = mDockState;
        if (i == 6 || i == 7 || i == 8) {
            targetLights = 0;
        }
        if (mBumperState == 1) {
            targetLights &= -2;
        }
        msg.arg1 = targetLights & 5;
        msg.sendToTarget();
    }

    private void setNotificationEffectInternal(String pkg, boolean active, int color, int mode, int rate) {
        synchronized (mLock) {
            if (!"!default".equals(pkg) && !active) {
                mNotificationEffects.remove(pkg);
            } else {
                mNotificationEffects.put(pkg, new LightEffect(active, color, mode, rate));
            }
        }
        Message msg = Message.obtain(mHandler, 1, 7, -1);
        msg.sendToTarget();
        mNotificationSettingsChanged = true;
        scheduleWriteSettings();
    }

    private void setScenarioEffectInternal(int scenario, boolean active, int color, int mode, int rate) {
        synchronized (mLock) {
            setScenarioEffectLocked(scenario, active, color, mode, rate);
            LightEffect effect = mRestoreCetraEffect.get(Integer.valueOf(scenario));
            if (effect != null && (effect.color != color || ((effect.mode <= 5 && mode != 1) || effect.mode > 5 || effect.rate != rate))) {
                mRestoreCetraEffect.remove(Integer.valueOf(scenario));
            }
        }
        if (scenario == 13 && active) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    disableMusicNotification();
                }
            }, 100L);
        }
    }

    private void setScenarioBlendedEffectInternal(int scenario, boolean active, int[] colors, int mode, int rate) {
        synchronized (mLock) {
            setScenarioBlendedEffectLocked(scenario, active, colors, mode, rate);
        }
    }

    private void setScenarioStatusInternal(int scenario, boolean status) {
        synchronized (mLock) {
            setScenarioStatusLocked(scenario, status);
        }
    }

    public int nativeGetFrame() {
        try {
            mIAuraLightServiceInternal.nativeGetFrame();
        } catch (Exception e) {
        }
        return 1;
    }
    
    public byte[] nativeGetPs() {
        mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
        try {
            mIAuraLightServiceInternal.nativeGetPs();
        } catch (Exception e) {
        }
        return null;
    }

    public byte[] nativeGetTIv() {
        mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
        long origId = Binder.clearCallingIdentity();
        try {
            mIAuraLightServiceInternal.nativeGetTIv();
        } catch (Exception e) {
        }
        return null;
    }

    public byte[] nativeGetTs() {
        mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
        long origId = Binder.clearCallingIdentity();
        try {
            mIAuraLightServiceInternal.nativeGetTs();
        } catch (Exception e) {
        }
        return null;
    }

    public boolean nativeNotifyScreenOffEffectActive(boolean activate) {
        mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
        try {
            mIAuraLightServiceInternal.nativeNotifyScreenOffEffectActive(activate);
        } catch (Exception e) {
        }
        return true;
    }

    public boolean nativeSetBlendedLight(int i, int[] iArr, int i2, int i3) {
        mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
        try {
            mIAuraLightServiceInternal.nativeSetBlendedLight(i, iArr, i2, i3);
        } catch (Exception e) {
        }
        return true;
    }

    public boolean nativeSetFrame(int frame) {
        mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
        try {
            mIAuraLightServiceInternal.nativeSetFrame(frame);
        } catch (Exception e) {
        }
        return true;
    }

    public boolean nativeSetLight(int i, int i2, int i3, int i4) {
        mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
        try {
           mIAuraLightServiceInternal.nativeSetLight(i, i2, i3, i4);
        } catch (Exception e) {
        }
        return true;
    }

    public final class LocalService implements AuraLightManagerInternal {
        private LocalService() {
        }

        @Override
        public void updateNotificationLighting(List<StatusBarNotification> sbns) {
            Message msg = new Message();
            msg.what = 15;
            msg.obj = new ArrayList(sbns);
            mHandler.sendMessage(msg);
        }

        @Override
        public void setFocusedApp(String packageName, String resultTo) {
            mHandler.removeMessages(8);
            Message msg = new Message();
            msg.what = 8;
            msg.obj = resultTo == null ? packageName : resultTo;
            mHandler.sendMessage(msg);
        }

        @Override
        public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
            Message msg = new Message();
            msg.what = 9;
            msg.arg1 = lidOpen ? 1 : 0;
            mHandler.sendMessage(msg);
        }

        @Override
        public void setCustomEffect(int callingUid, int targetLights, List<AuraLightEffect> effects) {
            setCustomEffectInternal(targetLights, effects);
        }

        @Override
        public void notifyBatteryStatsReset() {
            AsusAnalytics analytics = new AsusAnalytics(mScenario, System.currentTimeMillis());
            Message msg = mHandler.obtainMessage(24, analytics);
            mHandler.sendMessage(msg);
            boolean inboxConnect = mInboxConnect;
            String fanState = SystemProperties.get(AuraLightService.PROP_FAN_STATE, "0");
            InboxAnalytics inboxAnalytics = new InboxAnalytics(inboxConnect, fanState, System.currentTimeMillis());
            Message inboxMsg = mHandler.obtainMessage(26, inboxAnalytics);
            mHandler.sendMessage(inboxMsg);
        }
    }

    private final class BinderService extends IAuraLightService.Stub {
        private BinderService() {
        }

        public void setEnabled(boolean enabled) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                setEnableInternal(enabled);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public boolean getEnabled() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            return mEnabled;
        }

        public int getLightScenario() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                return getLightScenarioInternal();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void setScenarioStatus(int scenario, boolean status) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                setScenarioStatusInternal(scenario, status);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void setScenarioBlendedEffect(int scenario, boolean active, int[] colors, int mode, int rate) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                setScenarioBlendedEffectInternal(scenario, active, colors, mode, rate);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void setScenarioEffect(int scenario, boolean active, int color, int mode, int rate) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                setScenarioEffectInternal(scenario, active, color, mode, rate);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public boolean getScenarioBlendedEffect(int scenario, int[] output) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                return getScenarioBlendedEffectInternal(scenario, output);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public boolean getScenarioEffect(int scenario, int[] output) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                return getScenarioEffectInternal(scenario, output);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void setFrame(int frame) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                setFrameInternal(frame);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public long[] getDockStatistic() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                return getDockStatisticInternal();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public long[] getDockLedOnStatistic() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                return getDockLedOnStatisticInternal();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void resetStatistic() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                resetStatisticInternal();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void setNotificationEffect(String pkg, boolean active, int color, int mode, int rate) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                setNotificationEffectInternal(pkg, active, color, mode, rate);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public boolean getNotificationEffect(String pkg, int[] output) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                return getNotificationEffectInternal(pkg, output);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Deprecated
        public void updateNotificationLight(String[] pkgs) {
        }

        public void getCustomNotifications(List<String> pkgs) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                getCustomNotificationsInternal(pkgs);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public int getFrame() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                return getFrameInternal();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void setAuraLightEffect(int targetLights, List<AuraLightEffect> effects) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.CUSTOMIZE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                setCustomEffectInternal(targetLights, effects);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void setCustomEffectHw(List<AuraLightEffect> effects) {
            Message msg;
            mContext.enforceCallingOrSelfPermission("com.asus.permission.CUSTOMIZE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            try {
                mHandler.removeMessages(6);
                mHandler.removeMessages(7);
                if (mFocusedAppIsGame) {
                    msg = mHandler.obtainMessage(6, effects);
                    if (ISASUSCNSKU && !AuraLightService.IS_PICASSO && effects != null && effects.size() > 0) {
                        AuraLightEffect firstEffect = effects.get(0);
                        int type = 1;
                        if (mBumperState != 1) {
                            if (mDockState != 0) {
                                type = 3;
                            }
                        } else {
                            type = 2;
                        }
                        AuraLightService auraLightService = AuraLightService.this;
                        auraLightService.uploadCustomLightAnalytics(type, auraLightService.mFocusedApp, firstEffect.getColor(), firstEffect.getType(), firstEffect.getRate());
                    }
                } else {
                    msg = mHandler.obtainMessage(14, effects);
                    msg.arg1 = 14;
                    if (ISASUSCNSKU && !AuraLightService.IS_PICASSO) {
                        uploadSystemLightAnalytics(14);
                    }
                }
                msg.sendToTarget();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public byte[] getBumperId() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.CUSTOMIZE_AURA_LIGHT", null);
            return mBumperId;
        }

        public byte[] getBumperContents() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.CUSTOMIZE_AURA_LIGHT", null);
            long origId = Binder.clearCallingIdentity();
            Binder.restoreCallingIdentity(origId);
            return null;
        }

        public boolean notifyNfcTagDiscovered(Tag tag) {
            boolean success = extractTagInfo(tag);
            if (AuraLightService.DEBUG_ANALYTICS) {
                Slog.d(AuraLightService.TAG, "notifyNfcTagDiscovered: success=" + success);
            }
            if (success) {
                Message msg = new Message();
                msg.what = 10;
                mHandler.sendMessage(msg);
            }
            return success;
        }

        public boolean isSupportBlendedEffect() {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", null);
            return mSupportBlendedEffect;
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(mContext, AuraLightService.TAG, pw)) {
                return;
            }
            pw.println("Current state:");
            pw.println("  mEnabled=" + mEnabled);
            pw.println("  mScreenOn=" + mScreenOn);
            pw.println("  mXModeOn=" + mXModeOn);
            pw.println("  mCustomEffectEnabled=" + mCustomEffectEnabled);
            pw.println("  mSystemEffectEnabled=" + mSystemEffectEnabled);
            pw.println("  mKeyguardShowing=" + mKeyguardShowing);
            pw.println("  mIsUltraSavingMode=" + mIsUltraSavingMode);
            pw.println("  mColor=0x" + Integer.toHexString(mColor));
            pw.println("  mMode=" + AuraLightManager.modeToString(mMode));
            pw.println("  mRate=" + AuraLightManager.rateToString(mRate));
            pw.println("  mScenario=" + AuraLightManager.scenarioToString(mScenario));
            pw.println("  mBumperState=" + AuraLightManager.bumperStateToString(mBumperState));
            pw.println("  mPhoneState=" + mPhoneState);
            pw.println("  mIsCharging=" + mIsCharging);
            pw.println("  mLedStatesRecord=0x" + Integer.toHexString(mLedStatesRecord));
            pw.println("  mCurrentLedStates=0x" + Integer.toHexString(mCurrentLedStates));
            pw.println("  mChargingIndicatorPolicy=" + mChargingIndicatorPolicy);
            pw.println("  mNotificationExpirationTime=" + mNotificationExpirationTime);
            pw.println("  mSyncDelay=" + mSyncDelay);
            pw.println("  mSupportBlendedEffect=" + mSupportBlendedEffect);
            pw.println("  mScenarioEffectStartTime=" + mScenarioEffectStartTime);
            pw.println("Scenario status:");
            for (int i = 0; i < mStatus.length; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(AuraLightManager.scenarioToString(i));
                sb.append(": ");
                sb.append(mStatus[i] ? "Enabled" : "Disabled");
                pw.println(sb.toString());
            }
            pw.println("Effect status:");
            for (int i2 = 0; i2 < mEffects.length; i2++) {
                pw.println("  " + AuraLightManager.scenarioToString(i2) + ": " + mEffects[i2]);
            }
            pw.println("Notification effects:");
            for (String pkg : mNotificationEffects.keySet()) {
                LightEffect effect = (LightEffect) mNotificationEffects.get(pkg);
                pw.println("  Pkg [" + pkg + "]: " + effect);
            }
            pw.println("Support custom light apps: " + mSupportCustomLightApps);
            if (!AuraLightService.IS_PICASSO) {
                mHeadsetController.dump(fd, pw, args);
            }
        }
    }

    private static class Encryption {
        private static final String AES_TRANSFORMATION = "AES/CBC/PKCS7Padding";
        private static final String IV_PARAMS = "5soq,p9tjw7qq37-";

        private Encryption() {
        }

        public static String encrypt(String message) {
            if (AuraLightService.DEBUG_ANALYTICS || message == null) {
                return message;
            }
            try {
                SecretKeySpec skeySpec = new SecretKeySpec(makeHash(Build.getSerial() + "&#$" + Build.MODEL + "-%&" + Build.DEVICE), AuraLightService.KEY_ALGORITHM);
                IvParameterSpec ivSpec = new IvParameterSpec(IV_PARAMS.getBytes());
                Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
                cipher.init(1, skeySpec, ivSpec);
                byte[] dstBuff = cipher.doFinal(message.getBytes("UTF8"));
                String encrypt_msg = android.util.Base64.encodeToString(dstBuff, 8);
                return encrypt_msg;
            } catch (Exception e) {
                Slog.d(AuraLightService.TAG, "encrypt fail , msg = " + e.getMessage());
                return message;
            }
        }

        private static byte[] makeHash(String source) {
            if (TextUtils.isEmpty(source)) {
                return null;
            }
            byte[] hash_bytes = null;
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] source_bytes = source.getBytes();
                digest.update(source_bytes, 0, source_bytes.length - 1);
                byte[] digest_bytes = digest.digest();
                hash_bytes = new byte[digest_bytes.length];
                for (int i = 0; i < digest_bytes.length; i++) {
                    hash_bytes[(digest_bytes.length - 1) - i] = (byte) (digest_bytes[i] & 255);
                }
                int i2 = hash_bytes.length;
                hash_bytes[i2 / 2] = (byte) (hash_bytes[0] & hash_bytes[hash_bytes.length - 1]);
            } catch (Exception e) {
            }
            return hash_bytes;
        }
    }
}
