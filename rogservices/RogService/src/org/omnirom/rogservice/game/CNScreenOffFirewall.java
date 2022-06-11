package org.omnirom.rogservice.game;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import com.android.internal.net.IOemNetd;
import com.android.internal.util.ArrayUtils;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Slog;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CNScreenOffFirewall {
    private static final String CN_SCREEN_OFF_FIREWALL_DELAY = "cn_screen_off_firewall_delay";
    private static final String CN_SCREEN_OFF_FIREWALL_TURN_ON = "cn_screen_off_firewall_turn_on";
    private static final String CN_SCREEN_OFF_SETTING_FIREWALL_ENABLE = "cn_screen_off_setting_firewall_enable";
    private static final int DEFAULT_CN_SCREEN_OFF_DELAY = 60;
    private static final int DEFAULT_CN_SCREEN_OFF_SETTING_ENABLE = 1;
    private static final String TAG = "CNScreenOffFirewall";
    private final Context mContext;
    private final CNScreenOffHandler mHandler;
    private final IOemNetd mNetMgr;
    private boolean mScreenOff;
    private int mScreenOffDelay;
    private long mScreenOffTime;
    private Set<Integer> mCNScreenOffNetdUids;
    private ScreenStateMonitor mScreenStateMonitor = new ScreenStateMonitor();
    private boolean mSettingScreenOffEnable;
    private final SettingsObserver mSettingsObserver;
    private boolean mTurnOnFirewall;

    public CNScreenOffFirewall(Context context, Looper looper, IOemNetd netMgr) {
        mContext = context;
        mHandler = new CNScreenOffHandler(looper);
        mNetMgr = netMgr;
        mSettingsObserver = new SettingsObserver(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(mScreenStateMonitor, filter);
        mCNScreenOffNetdUids = new HashSet();
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nCN Screen Off Firewall:");
        sb.append("\n mScreenOff=" + mScreenOff);
        sb.append("\n mScreenOffDelay=" + mScreenOffDelay);
        if (mScreenOff) {
            sb.append("\n now already screen off=" + ((System.currentTimeMillis() - mScreenOffTime) / 1000.0d));
        }
        sb.append("\n mSettingScreenOffEnable=" + mSettingScreenOffEnable);
        sb.append("\n mTurnOnFirewall=" + mTurnOnFirewall);
        if (mNetMgr != null) {
            try {
                sb.append("\n mScreenOffAppOpsUids=" + Arrays.toString(getCNScreenOffApps()));
            } catch (Exception e) {
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
            }
            checkScreenOffState();
        }
    }

    private void checkScreenOffState() {
        int i = 60;
        int screenOffDelay = Settings.System.getInt(mContext.getContentResolver(), CN_SCREEN_OFF_FIREWALL_DELAY, 60);
        if (screenOffDelay >= 0) {
            i = screenOffDelay;
        }
        mScreenOffDelay = i;
        mSettingScreenOffEnable = Settings.System.getInt(mContext.getContentResolver(), CN_SCREEN_OFF_SETTING_FIREWALL_ENABLE, 1) == 1;
        long now = System.currentTimeMillis();
        boolean expiration = now - mScreenOffTime >= ((long) (mScreenOffDelay * 1000));
        boolean turnOnFirewall = mScreenOff && mSettingScreenOffEnable && expiration;
        if (mTurnOnFirewall != turnOnFirewall) {
            mHandler.obtainMessage(1, turnOnFirewall ? 1 : 0, 0).sendToTarget();
        }
        if (mScreenOff && !expiration) {
            long delay = (mScreenOffDelay * 1000) - (now - mScreenOffTime);
            mHandler.removeMessages(2);
            CNScreenOffHandler cNScreenOffHandler = mHandler;
            cNScreenOffHandler.sendMessageDelayed(cNScreenOffHandler.obtainMessage(2), delay);
        }
        mTurnOnFirewall = turnOnFirewall;
    }

    private void turnOnCNScreenOffFirewallInner(boolean turnOn) {
        int i = 1;
        try {
            if (turnOn) {
                GameNetworkHelper.enableTLChain(GameNetworkHelper.CHAIN_CN_SCREEN_OFF, true);
            } else {
                GameNetworkHelper.enableTLChain(GameNetworkHelper.CHAIN_CN_SCREEN_OFF, false);
            }
            mTurnOnFirewall = turnOn;
            ContentResolver contentResolver = mContext.getContentResolver();
            if (!turnOn) {
                i = 0;
            }
            Settings.System.putInt(contentResolver, CN_SCREEN_OFF_FIREWALL_TURN_ON, i);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to turn on screen off firewall.", e);
        }
    }

    private class SettingsObserver extends ContentObserver {
        private final Uri mScreenOffDelayUri;
        private final Uri mScreenOffSettingFirewallEnableUri;

        public SettingsObserver(Context context) {
            super(new Handler());
            Uri uriFor = Settings.System.getUriFor(CNScreenOffFirewall.CN_SCREEN_OFF_SETTING_FIREWALL_ENABLE);
            mScreenOffSettingFirewallEnableUri = uriFor;
            Uri uriFor2 = Settings.System.getUriFor(CNScreenOffFirewall.CN_SCREEN_OFF_FIREWALL_DELAY);
            mScreenOffDelayUri = uriFor2;
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(uriFor, false, this, -1);
            resolver.registerContentObserver(uriFor2, false, this, -1);
            checkScreenOffState();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }
            if (mScreenOffSettingFirewallEnableUri.equals(uri) || mScreenOffDelayUri.equals(uri)) {
                checkScreenOffState();
            }
        }
    }

    public class CNScreenOffHandler extends Handler {
        static final int MSG_CHECK_CN_SCREEN_OFF_STATE = 2;
        static final int MSG_TURN_ON_CN_SCREEN_OFF_FIREWALL = 1;

        CNScreenOffHandler(Looper looper) {
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
                    turnOnCNScreenOffFirewallInner(turnOn);
                    return;
                case 2:
                    checkScreenOffState();
                    return;
                default:
                    return;
            }
        }
    }

    public int[] getCNScreenOffApps() {
        mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        int[] uids = new int[0];
        synchronized (mCNScreenOffNetdUids) {
            for (Integer num : mCNScreenOffNetdUids) {
                int uid = num.intValue();
                uids = ArrayUtils.appendInt(uids, uid);
            }
        }
        return uids;
    }

}
