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

package org.omnirom.rogservice;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

public class OmniRogService extends Service {
    private static final String TAG = "OmniRogService";
    private static final boolean DEBUG = true;

    private RogService mRogService;

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
        mRogService = new RogService(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        mRogService.onStart();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        mRogService.disable();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mRogService.mRogProxy;
    }
}