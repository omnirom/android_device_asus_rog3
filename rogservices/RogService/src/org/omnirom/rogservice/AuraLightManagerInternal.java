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

package org.omnirom.rogservice;

import android.service.notification.StatusBarNotification;
import java.util.List;

public interface AuraLightManagerInternal {
    void notifyBatteryStatsReset();
    void notifyLidSwitchChanged(long j, boolean z);
    void setCustomEffect(int i, int i2, List<AuraLightEffect> list);
    void setFocusedApp(String str, String str2);
    void updateNotificationLighting(List<StatusBarNotification> list);
}
