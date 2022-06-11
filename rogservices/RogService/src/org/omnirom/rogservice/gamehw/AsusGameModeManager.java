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

import android.content.Context;
import android.os.RemoteException;

public class AsusGameModeManager {
    private Context mContext;
    private IAsusGameModeService mService;

    public AsusGameModeManager(Context context, IAsusGameModeService service) {
        mContext = context;
        mService = service;
    }

    public void setGameFirewallBlackList(int uid, boolean add) {
        try {
            String pkg = mContext.getPackageName();
            mService.setGameFirewallBlackList(pkg, uid, add);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int[] getGameFirewallBlackList() {
        try {
            String pkg = mContext.getPackageName();
            return mService.getGameFirewallBlackList(pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setGameDndLock(boolean lock) {
        try {
            String pkg = mContext.getPackageName();
            mService.setGameDndLock(pkg, lock);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean getGameDndLock() {
        try {
            mContext.getPackageName();
            return mService.getGameDndLock();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setScreenRecorderDndLock(boolean lock) {
        try {
            String pkg = mContext.getPackageName();
            mService.setScreenRecorderDndLock(pkg, lock);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean getScreenRecorderDndLock() {
        try {
            mContext.getPackageName();
            return mService.getScreenRecorderDndLock();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setCallGameDndLock(boolean lock) {
        try {
            String pkg = mContext.getPackageName();
            mService.setCallGameDndLock(pkg, lock);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean getCallGameDndLock() {
        try {
            mContext.getPackageName();
            return mService.getCallGameDndLock();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setGameDndPolicy(GameDndPolicy policy) {
        try {
            mService.setGameDndPolicy(mContext.getOpPackageName(), policy);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public GameDndPolicy getGameDndPolicy() {
        try {
            return mService.getGameDndPolicy(mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setCustomActivityDndLock(boolean lock) {
        try {
            String pkg = mContext.getPackageName();
            mService.setCustomActivityDndLock(pkg, lock);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean getCustomActivityDndLock() {
        try {
            mContext.getPackageName();
            return mService.getCustomActivityDndLock();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setAlarmGameDndLock(boolean lock) {
        try {
            String pkg = mContext.getPackageName();
            mService.setAlarmGameDndLock(pkg, lock);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean getAlarmGameDndLock() {
        try {
            mContext.getPackageName();
            return mService.getAlarmGameDndLock();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setInGameMode(boolean inGame) {
        try {
            String pkg = mContext.getPackageName();
            mService.setInGameMode(pkg, inGame);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isInGameMode() {
        try {
            mContext.getPackageName();
            return mService.isInGameMode();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }
}

