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

package org.omnirom.rogservice.game;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.UserHandle;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

public class FirewallMonitor {
    private static final String CMD_ARG_PACKAGE = "package";
    private static final String CMD_ARG_UID = "uid";
    private static final int CMD_NOTIFY_RESTRICT_UID_FOREGROUND = 1;
    private static final String FIREWALL_MONITOR_CLASS = "com.asus.mobilemanager.net.monitor.FirewallMonitorService";
    private static final String FIREWALL_MONITOR_DESCRIPTOR = "com.asus.mobilemanager.net.monitor.IFirewallMonitor";
    private static final long FIREWALL_MONITOR_LIFETIME = 1800000;
    private static final String FIREWALL_MONITOR_PACKAGE = "com.asus.mobilemanager";
    private static final int MSG_BIND_FIREWALL_MONITOR_SERVICE = 0;
    private static final int MSG_EXEC_FIREWALL_MONITOR_CMD = 1;
    private static final int MSG_UNBIND_FIREWALL_MONITOR_SERVICE = 2;
    private static final String TAG = "FirewallMonitor";
    private Context mContext;
    private IBinder mFirewallMonitor;
    private Handler mHandler;
    private final List<Message> mPendingCommands = new ArrayList();
    private ServiceConnection mFirewallMonitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Slog.i(FirewallMonitor.TAG, "Firewall Monitor online...");
            mFirewallMonitor = service;
            synchronized (mPendingCommands) {
                if (mPendingCommands.isEmpty()) {
                    mHandler.removeMessages(2);
                    mHandler.sendEmptyMessageDelayed(2, 1800000L);
                    return;
                }
                for (Message msg : mPendingCommands) {
                    try {
                        mHandler.sendMessage(msg);
                    } catch (Exception e) {
                        Slog.w(FirewallMonitor.TAG, "Send msg failed, err: " + e.getMessage());
                    }
                }
                mPendingCommands.clear();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Slog.i(FirewallMonitor.TAG, "Firewall Monitor offline...");
            mFirewallMonitor = null;
        }
    };

    public FirewallMonitor(Context context, Handler handler) {
        mContext = context;
        mHandler = new MonitorHandler(handler.getLooper());
    }

    protected void requestNotifyRestrictUidForeground(int uid, String packageName) {
        Bundle args = new Bundle();
        args.putInt("uid", uid);
        args.putString("package", packageName);
        Message msg = Message.obtain(mHandler, 1, 1, 0);
        msg.setData(args);
        msg.sendToTarget();
    }

    private final class MonitorHandler extends Handler {
        public MonitorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int retryCount = msg.arg2;
                    if (retryCount < 5) {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName(FirewallMonitor.FIREWALL_MONITOR_PACKAGE, FirewallMonitor.FIREWALL_MONITOR_CLASS));
                        intent.setPackage(FirewallMonitor.FIREWALL_MONITOR_PACKAGE);
                        boolean success = mContext.bindServiceAsUser(intent, mFirewallMonitorConnection, 1, UserHandle.OWNER);
                        if (!success) {
                            Message bindAgain = Message.obtain(msg);
                            bindAgain.arg2 = retryCount + 1;
                            mHandler.sendMessageDelayed(bindAgain, 5000L);
                            return;
                        }
                        return;
                    }
                    return;
                case 1:
                    boolean success2 = handleExecFirewallMonitorCmd(msg.arg1, msg.getData());
                    if (!success2) {
                        synchronized (mPendingCommands) {
                            Message pendingCmd = Message.obtain(msg);
                            mPendingCommands.add(pendingCmd);
                        }
                        if (!hasMessages(0)) {
                            sendEmptyMessage(0);
                            return;
                        }
                        return;
                    } else if (!hasMessages(1)) {
                        sendEmptyMessageDelayed(2, 1800000L);
                        return;
                    } else {
                        return;
                    }
                case 2:
                    try {
                        mContext.unbindService(mFirewallMonitorConnection);
                        return;
                    } catch (Exception e) {
                        return;
                    }
                default:
                    return;
            }
        }
    }

    private boolean handleExecFirewallMonitorCmd(int cmd, Bundle args) {
        if (mFirewallMonitor == null) {
            return false;
        }
        switch (cmd) {
            case 1:
                if (args == null) {
                    return true;
                }
                int uid = args.getInt("uid", 0);
                String packageName = args.getString("package", "");
                boolean success = notifyRestrictUidForeground(uid, packageName);
                return success;
            default:
                return true;
        }
    }

    private boolean notifyRestrictUidForeground(int uid, String packageName) {
        if (mFirewallMonitor == null) {
            return false;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(FIREWALL_MONITOR_DESCRIPTOR);
            data.writeInt(uid);
            data.writeString(packageName);
            mFirewallMonitor.transact(1, data, reply, 0);
            reply.readException();
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Notify Restrict Uid failed, err: " + e.getMessage());
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }
}
