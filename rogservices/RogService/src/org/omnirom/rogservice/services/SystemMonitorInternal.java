package org.omnirom.rogservice.services;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;

public abstract class SystemMonitorInternal {

    public interface IApiMonitor {
        void notifyApiCalling(Bundle bundle);
    }

    public interface IFocusAppMonitor {
        void notifyFocusAppChanged(Bundle bundle);
    }

    public abstract String getFocusAppClassName();

    public abstract String getFocusAppPackageName();

    public abstract void registerApiObserver(IApiMonitor iApiMonitor);

    public abstract void registerFocusAppObserver(IFocusAppMonitor iFocusAppMonitor);

    public abstract void unRegisterApiObserver(IApiMonitor iApiMonitor);

    public abstract void unRegisterFocusAppObserver(IFocusAppMonitor iFocusAppMonitor);

    public abstract void updateApiListener(ApiCallerMessage apiCallerMessage);

    public abstract void updateFocusApp(AppFocusedMessage appFocusedMessage);

    public abstract void updateMonitorKeyEvent(KeyEvent keyEvent, int i);

    public abstract void updateRecentAnimationFocusApp(AppFocusedMessage appFocusedMessage);

    public abstract void updateScreenBrightnessOverridden(int i);

    public static class AppFocusedMessage {
        private String mAppName = null;
        private final Rect mBounds = new Rect();
        private String mDisplayedTime = null;
        private Intent mIntent = null;
        private boolean mIsExternalDisplay = false;
        private String mLaunchedFromPackage = null;
        private int mPid = -1;
        private String mProcessName = null;
        private String mReason = null;
        private String mTaskDescriptionLabel = null;
        private int mTaskId = -1;
        private int mUid = -1;
        private int mUserId = 0;
        private int mWindowingMode = 0;

        public AppFocusedMessage() {
        }

        public AppFocusedMessage(Bundle bundle) {
            mPid = bundle.getInt("focusAppPid", -1);
            mTaskId = bundle.getInt("focusAppTaskId", -1);
            mUid = bundle.getInt("focusAppUid", -1);
            mUserId = bundle.getInt("userId", 0);
            mWindowingMode = bundle.getInt("windowingMode", 0);
            mIsExternalDisplay = bundle.getBoolean("isExternalDisplay");
            String appName = bundle.getString("focusApp");
            if (appName != null) {
                mAppName = appName;
            }
            String time = bundle.getString("displayedTime");
            if (time != null) {
                mDisplayedTime = time;
            }
            String launchedFromPackage = bundle.getString("launchedFromPackage");
            if (launchedFromPackage != null) {
                mLaunchedFromPackage = launchedFromPackage;
            }
            String processName = bundle.getString("focusProcess");
            if (processName != null) {
                mProcessName = processName;
            }
            String reason = bundle.getString("reason");
            if (reason != null) {
                mReason = reason;
            }
            String taskDescriptionLabel = bundle.getString("TaskDescriptionLabel");
            if (taskDescriptionLabel != null) {
                mTaskDescriptionLabel = taskDescriptionLabel;
            }
            Intent i = (Intent) bundle.getParcelable("launchIntent");
            if (i != null) {
                mIntent = new Intent(i);
            }
            Parcelable bounds = bundle.getParcelable("bounds");
            if (bounds instanceof Rect) {
                mBounds.set((Rect) bounds);
            }
        }

        public void setAppPid(int pid) {
            mPid = pid;
        }

        public void setAppTaskId(int id) {
            mTaskId = id;
        }

        public void setFocusAppUid(int uid) {
            mUid = uid;
        }

        public void setUserId(int userId) {
            mUserId = userId;
        }

        public void setWindowingMode(int windowingMode) {
            mWindowingMode = windowingMode;
        }

        public void setDisplay(boolean isExternalDisplay) {
            mIsExternalDisplay = isExternalDisplay;
        }

        public void setFocusedApp(String app) {
            if (app != null) {
                mAppName = new String(app);
            }
        }

        public void setDisplayedTime(String time) {
            if (time != null) {
                mDisplayedTime = new String(time);
            }
        }

        public void setLaunchedFromPackage(String launchedFromPackage) {
            if (launchedFromPackage != null) {
                mLaunchedFromPackage = new String(launchedFromPackage);
            }
        }

        public void setFocusedProcess(String processName) {
            if (processName != null) {
                mProcessName = new String(processName);
            }
        }

        public void setReason(String reason) {
            if (reason != null) {
                mReason = new String(reason);
            }
        }

        public void setTaskDescriptionLabel(String label) {
            if (label != null) {
                mTaskDescriptionLabel = label;
            }
        }

        public void setIntent(Intent i) {
            if (i != null) {
                mIntent = new Intent(i);
            }
        }

        public void setBounds(Rect bounds) {
            if (bounds != null) {
                mBounds.set(bounds);
            }
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt("focusAppPid", mPid);
            bundle.putInt("focusAppTaskId", mTaskId);
            bundle.putInt("focusAppUid", mUid);
            bundle.putInt("userId", mUserId);
            bundle.putInt("windowingMode", mWindowingMode);
            bundle.putBoolean("isExternalDisplay", mIsExternalDisplay);
            bundle.putString("focusApp", mAppName);
            bundle.putString("displayedTime", mDisplayedTime);
            bundle.putString("launchedFromPackage", mLaunchedFromPackage);
            bundle.putString("focusProcess", mProcessName);
            bundle.putString("reason", mReason);
            bundle.putString("TaskDescriptionLabel", mTaskDescriptionLabel);
            bundle.putParcelable("launchIntent", mIntent);
            bundle.putParcelable("bounds", mBounds);
            return bundle;
        }
    }

    public static class ApiCallerMessage {
        private String mAppPackageName = null;
        private String mCallingApi = null;
        private String mState = null;
        private int mUid = -1;

        public ApiCallerMessage() {
        }

        public ApiCallerMessage(Bundle bundle) {
            mUid = bundle.getInt("appUid", -1);
            String appPackageName = bundle.getString("appPackageName");
            if (appPackageName != null) {
                mAppPackageName = new String(appPackageName);
            }
            String callingApi = bundle.getString("callingApi");
            if (callingApi != null) {
                mCallingApi = new String(callingApi);
            }
            String state = bundle.getString("state");
            if (state != null) {
                mState = new String(state);
            }
        }

        public void setUid(int uid) {
            mUid = uid;
        }

        public void setAppPackageName(String appPackageName) {
            if (appPackageName != null) {
                mAppPackageName = new String(appPackageName);
            }
        }

        public void setCallingApi(String callingApi) {
            if (callingApi != null) {
                mCallingApi = new String(callingApi);
            }
        }

        public void setState(String state) {
            if (state != null) {
                mState = new String(state);
            }
        }

        public void setAudioUid(int uid) {
            mUid = uid;
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt("appUid", mUid);
            bundle.putString("appPackageName", mAppPackageName);
            bundle.putString("callingApi", mCallingApi);
            bundle.putString("state", mState);
            return bundle;
        }
    }
}
