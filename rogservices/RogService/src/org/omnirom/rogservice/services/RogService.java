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

import android.app.SystemServiceRegistry;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IVibratorManagerService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.SomeArgs;
import com.asus.ims.rogproxy.IRogProxy;
import org.omnirom.rogservice.AuraLightManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class RogService {
    private static final int ACTION_A2V = 1000;
    private static final int ACTION_AIRTRIGGER = 16896;
    private static final int ACTION_AURA = 2000;
    private static final int ACTION_FPS = 4000;
    private static final int ACTION_GAME_VIBRATE = 17408;
    private static final int ACTION_PERF = 3000;
    private static final int ACTION_STOP_CAME_VIBRATE = 17409;
    private static final int ACTION_VOLUMEBOOST = 16897;
    private static final int ASUS_UNITY = 32;
    private static final String FPS_CONTROLLER_CLASS = "com.asus.hardwarestub.centralcontrol.FPSCentralControlService";
    private static final long FPS_CONTROLLER_LIFETIME = 1800000;
    private static final String FPS_CONTROLLER_PACKAGE = "com.asus.hardwarestub";
    private static final int INTERACTION = 2;
    private static final int MAX_DELAY_MS = 1000;
    private static final int MSG_A2V_AUTO = 7;
    private static final int MSG_BIND_FPS_CONTROLLER = 3;
    private static final int MSG_BOOST_PERFORMANCE = 5;
    private static final int MSG_RELEASE_PERFORMANCE = 6;
    private static final int MSG_SET_AURA_EFFECTS = 1;
    private static final int MSG_SET_FPS = 2;
    private static final int MSG_STATS = 9;
    private static final int MSG_SYSTEM_RUNNING = 0;
    private static final int MSG_UNBIND_FPS_CONTROLLER = 4;
    private static final String TAG = "RepublicOfGamersService";
    private static final String mMissionApp = "com.moonshine.rogphonear";
    private Context mContext;
    private Handler mGameHandler;
    public Handler mHandler;
    private HandlerThread mHandlerThread;
    private PackageManager mPackageManager;
    private SystemMonitorInternal mSystemMonitorInternal;
    public HandlerThread mThread;
    private static boolean DEBUG = false;
    private static boolean mInProgress = false;
    private static int mLastPowerHint = 2;

    protected final IBinder mRogProxy = new IRogProxy.Stub() {
        public void sendMessage(int action, String s1, String s2, String s3, int i1, int i2, int i3, int i4, int i5, int i6, Bundle bundle) {
            Slog.i(TAG, String.format("get action = 0x%08x", Integer.valueOf(action)));
            if (action == ACTION_GAME_VIBRATE) {
                requestGameVibrate(s1, s2, s3, i1, i2, i3, i4, Binder.getCallingUid(), i6, bundle);
            }
            if (action == ACTION_STOP_CAME_VIBRATE) {
                requestStopGameVibrate();
            }
            RogFeatureMessage msg = new RogFeatureMessage();
            msg.args = SomeArgs.obtain();
            if (s1 != null) {
                msg.args.arg1 = new String(s1);
            }
            if (s2 != null) {
                msg.args.arg2 = new String(s2);
            }
            if (s3 != null) {
                msg.args.arg3 = new String(s3);
            }
            msg.args.argi1 = i1;
            msg.args.argi2 = i2;
            msg.args.argi3 = i3;
            msg.args.argi4 = i4;
            msg.args.argi5 = i5;
            msg.args.argi6 = i6;
            if (bundle != null) {
                msg.bundle = new Bundle(bundle);
            }
            switch (action) {
                case ACTION_A2V:
                    requestVibrator(msg);
                    return;
                case ACTION_AURA:
                    sendStats(action, msg);
                    requestSetAuraEffects(msg);
                    return;
                case ACTION_PERF:
                    sendStats(action, msg);
                    requestBoostPerformance(msg);
                    return;
                case ACTION_FPS:
                    sendStats(action, msg);
                    requestSetFps(msg);
                    return;
                case ACTION_AIRTRIGGER:
                    if (s2 != null && mMissionApp.equals(s2)) {
                        sendApiCallerMessageLocked(s2, "airtrigger_tap", s3, 0);
                        return;
                    }
                    return;
                case ACTION_VOLUMEBOOST:
                    if (s2 != null) {
                        sendApiCallerMessageLocked(s2, "volume_boost", s3, 0);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };

    public static class RogFeatureMessage {
        public SomeArgs args;
        public Bundle bundle;
    }

    public RogService(Context context) {
        mContext = context;
    }

    public void onStart() {
        mContext.startService(new Intent(mContext, AuraLightManager.class));

        HandlerThread handlerThread = new HandlerThread("RogService-WorkThread", 0);
        mThread = handlerThread;
        handlerThread.start();
        mHandler = new RogServiceHandler(mContext, mThread.getLooper());
        HandlerThread handlerThread2 = new HandlerThread("HapticPlayerThread", -8);
        mHandlerThread = handlerThread2;
        handlerThread2.start();
        mGameHandler = new GameVibrateHandler(mHandlerThread.getLooper());
        mPackageManager = mContext.getPackageManager();
    }

    void disable() {
    }

    private void requestSetFps(RogFeatureMessage msg) {
        Message targetMsg = Message.obtain(mHandler, 2);
        targetMsg.sendingUid = Binder.getCallingUid();
        targetMsg.arg1 = msg.args.argi1;
        targetMsg.sendToTarget();
    }

    private void sendStats(int action, RogFeatureMessage msg) {
        Message rogMsg = mHandler.obtainMessage();
        rogMsg.what = 9;
        rogMsg.arg1 = action;
        rogMsg.obj = msg.args;
        mHandler.sendMessage(rogMsg);
    }

    private void requestSetAuraEffects(RogFeatureMessage msg) {
        if ("setPCEffect".equals(msg.args.arg1)) {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.MANAGE_AURA_LIGHT", "set pc effects");
        } else {
            mContext.enforceCallingOrSelfPermission("com.asus.permission.CUSTOMIZE_AURA_LIGHT", "set custom effects");
        }
        Message targetMsg = Message.obtain(mHandler, 1);
        targetMsg.sendingUid = Binder.getCallingUid();
        targetMsg.arg1 = !"setPCEffect".equals(msg.args.arg1) ? 1 : 0;
        targetMsg.setData(msg.bundle);
        targetMsg.sendToTarget();
    }

    private void requestBoostPerformance(RogFeatureMessage msg) {
        Message rogMsg = mHandler.obtainMessage();
        rogMsg.what = 5;
        rogMsg.obj = Integer.valueOf(msg.args.argi2);
        mHandler.sendMessage(rogMsg);
    }

    private void requestVibrator(RogFeatureMessage msg) {
        Message rogMsg = mHandler.obtainMessage();
        if ("A2V_AUTO".equals(msg.args.arg1)) {
            rogMsg.what = 7;
        }
        rogMsg.obj = msg.args;
        mHandler.sendMessage(rogMsg);
    }

    private void requestGameVibrate(String s1, String s2, String s3, int i1, int i2, int i3, int i4, int i5, int i6, Bundle bundle) {
        if (mGameHandler!= null) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = new String(s1);
            args.arg2 = new String(s2);
            args.argi1 = i1;
            args.argi2 = i2;
            args.argi3 = i3;
            args.argi4 = i4;
            args.argi5 = i5;
            mGameHandler.removeMessages(4000);
            Message message = mGameHandler.obtainMessage(4000);
            message.obj = args;
            mGameHandler.sendMessage(message);
        }
    }

    private void requestStopGameVibrate() {
        mGameHandler.removeMessages(GameVibrateHandler.MSG_STOP);
        Message message = mGameHandler.obtainMessage(GameVibrateHandler.MSG_STOP);
        mGameHandler.sendMessage(message);
    }

    private static class RogServiceHandler extends Handler {
        private Context mContext;
        private IBinder mFpsController;
        private final List<Message> mPendingFpsCommands = new ArrayList();
        private ServiceConnection mFpsControllerConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Slog.d(RogService.TAG, "Hello...FPS Ctrler");
                mFpsController = service;
                synchronized (mPendingFpsCommands) {
                    if (!mPendingFpsCommands.isEmpty()) {
                        for (Message msg : mPendingFpsCommands) {
                            if (!hasMessages(msg.what, msg.obj)) {
                                try {
                                    sendMessage(msg);
                                } catch (Exception e) {
                                    Slog.w(RogService.TAG, "Send msg failed, err: " + e.getMessage());
                                }
                            }
                        }
                        mPendingFpsCommands.clear();
                        return;
                    }
                    removeMessages(4);
                    sendEmptyMessageDelayed(4, 1800000L);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Slog.d(RogService.TAG, "Good Bye...FPS Ctrler");
                mFpsController = null;
            }
        };

        RogServiceHandler(Context context, Looper looper) {
            super(looper);
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Slog.i(RogService.TAG, "MSG_SYSTEM_RUNNING & RELEASE ROGPROXY");
                    List<String> exemptionsList = Arrays.asList("Lcom/asus/ims/rogproxy/IRogProxy");
                    if (!Process.ZYGOTE_PROCESS.setApiDenylistExemptions(exemptionsList)) {
                        Slog.e(RogService.TAG, "Failed to set API blacklist exemptions!");
                        return;
                    } else {
                        Slog.w(RogService.TAG, "update list success ");
                        return;
                    }
                case 1:
                case 6:
                default:
                    return;
                case 2:
                    removeMessages(4);
                    int fps = msg.arg1;
                    boolean success = handleSetFps(msg.sendingUid, fps);
                    if (!success) {
                        synchronized (mPendingFpsCommands) {
                            Message pendingMsg = Message.obtain(msg);
                            mPendingFpsCommands.add(pendingMsg);
                        }
                        if (!hasMessages(3)) {
                            sendEmptyMessage(3);
                            return;
                        }
                        return;
                    } else if (!hasMessages(2)) {
                        sendEmptyMessageDelayed(4, 1800000L);
                        return;
                    } else {
                        return;
                    }
                case 3:
                    int retryCount = msg.arg2;
                    if (retryCount < 5) {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName(RogService.FPS_CONTROLLER_PACKAGE, RogService.FPS_CONTROLLER_CLASS));
                        intent.setPackage(RogService.FPS_CONTROLLER_PACKAGE);
                        boolean success2 = mContext.bindServiceAsUser(intent, mFpsControllerConnection, 1, UserHandle.OWNER);
                        if (!success2) {
                            Message bindAgain = Message.obtain(msg);
                            bindAgain.arg2 = retryCount + 1;
                            sendMessageDelayed(bindAgain, 5000L);
                            return;
                        }
                        return;
                    }
                    return;
                case 4:
                    try {
                        mContext.unbindService(mFpsControllerConnection);
                        return;
                    } catch (Exception e) {
                        return;
                    }
                case 5:
                    if (!RogService.mInProgress) {
                        int delay = ((Integer) msg.obj).intValue();
                        boolean unused = RogService.mInProgress = true;
                        Slog.i(RogService.TAG, "MSG_BOOST_PERFORMANCE duration:" + delay);
                        if (delay > 1000) {
                            delay = 1000;
                        }
                        sendEmptyMessageDelayed(6, delay);
                    } else {
                        removeMessages(6);
                        int delay2 = ((Integer) msg.obj).intValue();
                        if (delay2 > 1000) {
                            delay2 = 1000;
                        }
                        sendEmptyMessageDelayed(6, delay2);
                    }
                    Slog.i(RogService.TAG, "MSG_BOOST_PERFORMANCE mLastPowerHint:" + RogService.mLastPowerHint);
                    return;
            }
        }

        private void handleSetAuraLightEffects(int callingUid, Bundle data, int mode) {
            data.getInt("effect_region", -1);
            ArrayList<Integer> types = data.getIntegerArrayList("effect_types");
            ArrayList<Integer> colors = data.getIntegerArrayList("effect_colors");
            ArrayList<Integer> rates = data.getIntegerArrayList("effect_rates");
            ArrayList<Integer> durations = data.getIntegerArrayList("effect_durations");
            if (types == null || colors == null || rates == null || durations == null) {
                Slog.w(RogService.TAG, "No aura effect data");
                return;
            }
            int effectSize = types.size();
            if (effectSize != colors.size() || effectSize != rates.size() || effectSize != durations.size()) {
                Slog.w(RogService.TAG, "Lack of Aura effect data ");
            }
        }

        private boolean handleSetFps(int uid, int fps) {
            if (mFpsController == null) {
                return false;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                try {
                    data.writeInterfaceToken("com.asus.hardwarestub.centralcontrol.IFPSCentralControl");
                    data.writeInt(uid);
                    data.writeInt(fps);
                    mFpsController.transact(5, data, reply, 0);
                    reply.readException();
                    reply.recycle();
                    data.recycle();
                    return true;
                } catch (Exception e) {
                    Slog.w(RogService.TAG, "Set fps failed, err: " + e.getMessage());
                    reply.recycle();
                    data.recycle();
                    return false;
                }
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
                throw th;
            }
        }
    }

    private void sendApiCallerMessageLocked(String pkgName, String callingApi, String state, int uid) {
        if (mSystemMonitorInternal != null) {
            SystemMonitorInternal.ApiCallerMessage msg = new SystemMonitorInternal.ApiCallerMessage();
            msg.setAppPackageName(pkgName);
            msg.setCallingApi(callingApi);
            msg.setState(state);
            msg.setUid(uid);
            mSystemMonitorInternal.updateApiListener(msg);
            return;
        }
        Slog.d(TAG, "Cannot send AppFocusedMessage to SystemMonitorService");
    }

    private class GameEffect {
        VibrationEffect mEffect;
        int mRelativeTime;

        GameEffect() {
        }
    }

    class GameVibrateHandler extends Handler {
        public static final int MSG_PARSE = 4000;
        public static final int MSG_START = 40003;
        public static final int MSG_STOP = 40002;
        public static final int MSG_VIBRATE = 40001;
        private SomeArgs mArgs;
        private Handler mHandler;
        private String mJson;
        private ArrayList<GameEffect> mEffectList = new ArrayList<>();
        int mLoop = 1;
        int mInterval = 0;
        int mFreq = -1;
        int mAmplitude = -1;
        int mCounter = 1;
        private long DEFAULT_ONESHOT_DURATION = 65;
        private int mUid = 1000;
        private IVibratorManagerService mService = IVibratorManagerService.Stub.asInterface(ServiceManager.getService("vibrator_manager"));
        private Binder mToken = new Binder();

        GameVibrateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4000:
                    mCounter = 1;
                    removeMessages(MSG_START);
                    removeMessages(40001);
                    SomeArgs args = (SomeArgs) msg.obj;
                    mLoop = args.argi1;
                    mInterval = args.argi2;
                    mAmplitude = args.argi3;
                    mFreq = args.argi4;
                    String json = (String) args.arg1;
                    mUid = args.argi5;
                    parse(json);
                    return;
                case 40001:
                    try {
                        VibrationEffect vibrationEffect = (VibrationEffect) msg.obj;
                        return;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                case MSG_STOP /* 40002 */:
                    try {
                        removeMessages(MSG_START);
                        removeMessages(40001);
                        if (mEffectList.size() > 0) {
                            mEffectList.clear();
                        }
                        mCounter = 1;
                        mLoop = 1;
                        return;
                    } catch (Exception ex2) {
                        ex2.printStackTrace();
                        return;
                    }
                case MSG_START /* 40003 */:
                    int i = mLoop;
                    if (i > 1 && mCounter >= i) {
                        mCounter = 1;
                        mLoop = 1;
                        removeMessages(40001);
                        removeMessages(MSG_START);
                        removeMessages(MSG_STOP);
                        if (mEffectList.size() > 0) {
                            mEffectList.clear();
                            return;
                        }
                        return;
                    }
                    for (int i2 = 0; i2 < mEffectList.size(); i2++) {
                        GameEffect effect = mEffectList.get(i2);
                        Message message = obtainMessage(40001);
                        message.obj = effect.mEffect;
                        sendMessageDelayed(message, effect.mRelativeTime);
                    }
                    int i3 = mLoop;
                    if (i3 > 1) {
                        Log.i(RogService.TAG, "counter = " + mCounter + ", mLoop = " + mLoop + ", mInterval = " + mInterval);
                        if (mCounter < mLoop) {
                            Message loop = obtainMessage(MSG_START);
                            sendMessageDelayed(loop, mInterval);
                        }
                        mCounter++;
                    }
                    if (mLoop == -1) {
                        Message loop2 = obtainMessage(MSG_START);
                        sendMessageDelayed(loop2, mInterval);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private void parse(String json) {
            try {
                mJson = new String(json);
                if (mEffectList.size() > 0) {
                    mEffectList.clear();
                }
                JSONObject reader = new JSONObject(mJson);
                JSONObject metaData = reader.getJSONObject("Metadata");
                metaData.getString("Version");
                metaData.getString("Created");
                metaData.getString("Description");
                JSONArray patternArray = reader.getJSONArray("Pattern");
                for (int i = 0; i < patternArray.length(); i++) {
                    JSONObject eventArray = patternArray.getJSONObject(i);
                    JSONObject event = eventArray.getJSONObject("Event");
                    if ("continuous".equals(event.getString("Type"))) {
                        createContinuous(event, event.getInt("RelativeTime"));
                    }
                    if ("transient".equals(event.getString("Type"))) {
                        createTransient(event, event.getInt("RelativeTime"));
                    }
                }
                if (mLoop == 0) {
                    mLoop = 1;
                }
                if (mInterval < 0) {
                    mInterval = 0;
                }
                if (mInterval > 1000) {
                    mInterval = 1000;
                }
                if (mLoop > 1 && mInterval == 0) {
                    mInterval = 10;
                }
                removeMessages(MSG_START);
                Message start = obtainMessage(MSG_START);
                sendMessage(start);
            } catch (Exception e) {
                Log.i(RogService.TAG, "parse exception json = " + mJson + "\n");
            }
        }

        private void createContinuous(JSONObject event, int relativeTime) {
            try {
                JSONObject param = event.getJSONObject("Parameters");
                double paramIntensity = param.getDouble("Intensity");
                int i = mAmplitude;
                if (i != -1) {
                    paramIntensity = (i * 100) / 255;
                }
                JSONArray curveArray = param.getJSONArray("Curve");
                int[] intensity = new int[curveArray.length()];
                long[] timing = new long[curveArray.length()];
                int preTime = -1;
                for (int i2 = 0; i2 < curveArray.length(); i2++) {
                    JSONObject curve = curveArray.getJSONObject(i2);
                    if (preTime == -1) {
                        timing[i2] = curve.getInt("Time");
                        preTime = curve.getInt("Time");
                    } else {
                        timing[i2] = curve.getInt("Time") - preTime;
                        preTime = curve.getInt("Time");
                    }
                    intensity[i2] = (int) (curve.getDouble("Intensity") * paramIntensity);
                }
                GameEffect effect = new GameEffect();
                effect.mEffect = VibrationEffect.createWaveform(timing, intensity, -1);
                effect.mRelativeTime = event.getInt("RelativeTime");
                mEffectList.add(effect);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void createTransient(JSONObject event, int relativeTime) {
            try {
                JSONObject param = event.getJSONObject("Parameters");
                int intensity = param.getInt("Intensity");
                int i = mAmplitude;
                if (i != -1) {
                    intensity = (i * 100) / 255;
                }
                GameEffect effect = new GameEffect();
                effect.mEffect = VibrationEffect.createOneShot(DEFAULT_ONESHOT_DURATION, intensity);
                effect.mRelativeTime = event.getInt("RelativeTime");
                mEffectList.add(effect);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
