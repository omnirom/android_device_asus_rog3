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
 
package org.omnirom.rogservice.gamehw;

import android.os.Bundle;
import java.util.HashMap;
import java.util.List;

public abstract class AsusGameModeInternal
{
    public abstract boolean blockForGameFirewall(int i);
    public abstract boolean denyActivity(String str, int i);
    public abstract boolean denyCustomActivity(String str, int i);
    public abstract boolean denyOverlayWindow(String str, int i);
    public abstract boolean denySound(String str, int i, int i2);
    public abstract boolean denyVibration(String str, int i, int i2);
    public abstract boolean getAlarmGameDndLock();
    public abstract boolean getCallGameDndLock();
    public abstract boolean getGameDndLock();
    public abstract int getGameDndMode();
    public abstract boolean getScreenRecorderDndLock();
    public abstract boolean isActivityFeatureEnabled();
    public abstract boolean isActivityWhiteList(String str);
    public abstract boolean isCallActivity(String str);
    public abstract boolean isCallNotification(String str, String str2, String str3, String str4, String str5);
    public abstract boolean isCustomActivityFeatureEnabled();
    public abstract boolean isInGameMode();
    public abstract boolean isSoundFeatureEnabled();
    public abstract boolean isToastFeatureEnabled();
    public abstract boolean isVibrationFeatureEnabled();
    public abstract void notifyScreenRecorder(Bundle bundle);
    public abstract void recordOverlayWindow(String str, long j, boolean z);
    public abstract void setActivityWhiteList(String str, String str2);
    public abstract void setCallGameDndLock(String str, boolean z);
    public abstract void setGameDndLock(String str, boolean z);
    public abstract void setRestrictInterfaces(List<String> list);
    public abstract void setScreenRecorderDndLock(String str, boolean z);
    public abstract void setTopPackage(String str, boolean z);
    public abstract void setUidForeground(int i, boolean z);
    public abstract void setUidRejectInterface(HashMap<Integer, List<String>> hashMap);
    public abstract boolean showCustomView(String str, int i);
    public abstract void startCustomActivity(Bundle bundle);
}
