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

package android.os;

import android.nfc.Tag;
import java.util.List;
import android.os.AuraLightEffect;

interface IAuraLightService
{
    byte[] getBumperContents();
    byte[] getBumperId();
    void getCustomNotifications(inout List<String> list);
    long[] getDockLedOnStatistic();
    long[] getDockStatistic();
    boolean getEnabled();
    int getFrame();
    int getLightScenario();
    boolean getNotificationEffect(String str, inout int[] iArr);
    boolean getScenarioBlendedEffect(int i, inout int[] iArr);
    boolean getScenarioEffect(int i, inout int[] iArr);
    boolean isSupportBlendedEffect();
    boolean notifyNfcTagDiscovered(in Tag tag);
    void resetStatistic();
    void setAuraLightEffect(int i, in List<AuraLightEffect> list);
    void setCustomEffectHw(in List<AuraLightEffect> list);
    void setEnabled(boolean z);
    void setFrame(int i);
    void setNotificationEffect(String str, boolean z, int i, int i2, int i3);
    void setScenarioBlendedEffect(int i, boolean z, in int[] iArr, int i2, int i3);
    void setScenarioEffect(int i, boolean z, int i2, int i3, int i4);
    void setScenarioStatus(int i, boolean z);
    void updateNotificationLight(inout String[] strArr);
}
