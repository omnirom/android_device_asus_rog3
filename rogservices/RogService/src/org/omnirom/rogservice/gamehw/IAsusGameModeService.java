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

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IAsusGameModeService extends IInterface {
    public static final String DESCRIPTOR = "org.omnirom.rogservice.gamehw.IAsusGameModeService";

    boolean blockForGameFirewall(int i) throws RemoteException;

    boolean denySound(String str, int i, int i2) throws RemoteException;

    boolean getAlarmGameDndLock() throws RemoteException;

    boolean getCallGameDndLock() throws RemoteException;

    boolean getCustomActivityDndLock() throws RemoteException;

    boolean getGameDndLock() throws RemoteException;

    int getGameDndMode() throws RemoteException;

    GameDndPolicy getGameDndPolicy(String str) throws RemoteException;

    int[] getGameFirewallBlackList(String str) throws RemoteException;

    boolean getScreenRecorderDndLock() throws RemoteException;

    boolean isInGameMode() throws RemoteException;

    boolean isSoundFeatureEnabled() throws RemoteException;

    void setAlarmGameDndLock(String str, boolean z) throws RemoteException;

    void setCallGameDndLock(String str, boolean z) throws RemoteException;

    void setCustomActivityDndLock(String str, boolean z) throws RemoteException;

    void setGameDndLock(String str, boolean z) throws RemoteException;

    void setGameDndPolicy(String str, GameDndPolicy gameDndPolicy) throws RemoteException;

    void setGameFirewallBlackList(String str, int i, boolean z) throws RemoteException;

    void setInGameMode(String str, boolean z) throws RemoteException;

    void setScreenRecorderDndLock(String str, boolean z) throws RemoteException;

    public static class Default implements IAsusGameModeService {
        @Override
        public void setGameFirewallBlackList(String pkg, int uid, boolean add) throws RemoteException {
        }

        @Override
        public int[] getGameFirewallBlackList(String pkg) throws RemoteException {
            return null;
        }

        @Override
        public void setGameDndLock(String pkg, boolean lock) throws RemoteException {
        }

        @Override
        public boolean getGameDndLock() throws RemoteException {
            return false;
        }

        @Override
        public void setScreenRecorderDndLock(String pkg, boolean lock) throws RemoteException {
        }

        @Override
        public boolean getScreenRecorderDndLock() throws RemoteException {
            return false;
        }

        @Override
        public void setCallGameDndLock(String pkg, boolean lock) throws RemoteException {
        }

        @Override
        public boolean getCallGameDndLock() throws RemoteException {
            return false;
        }

        @Override
        public int getGameDndMode() throws RemoteException {
            return 0;
        }

        @Override
        public boolean isSoundFeatureEnabled() throws RemoteException {
            return false;
        }

        @Override
        public boolean denySound(String pkg, int usage, int gameMode) throws RemoteException {
            return false;
        }

        @Override
        public void setGameDndPolicy(String pkg, GameDndPolicy policy) throws RemoteException {
        }

        @Override
        public GameDndPolicy getGameDndPolicy(String pkg) throws RemoteException {
            return null;
        }

        @Override
        public void setCustomActivityDndLock(String pkg, boolean lock) throws RemoteException {
        }

        @Override
        public boolean getCustomActivityDndLock() throws RemoteException {
            return false;
        }

        @Override
        public void setAlarmGameDndLock(String pkg, boolean lock) throws RemoteException {
        }

        @Override
        public boolean getAlarmGameDndLock() throws RemoteException {
            return false;
        }

        @Override
        public void setInGameMode(String pkg, boolean inGame) throws RemoteException {
        }

        @Override
        public boolean isInGameMode() throws RemoteException {
            return false;
        }

        @Override
        public boolean blockForGameFirewall(int uid) throws RemoteException {
            return false;
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IAsusGameModeService {
        static final int TRANSACTION_blockForGameFirewall = 20;
        static final int TRANSACTION_denySound = 11;
        static final int TRANSACTION_getAlarmGameDndLock = 17;
        static final int TRANSACTION_getCallGameDndLock = 8;
        static final int TRANSACTION_getCustomActivityDndLock = 15;
        static final int TRANSACTION_getGameDndLock = 4;
        static final int TRANSACTION_getGameDndMode = 9;
        static final int TRANSACTION_getGameDndPolicy = 13;
        static final int TRANSACTION_getGameFirewallBlackList = 2;
        static final int TRANSACTION_getScreenRecorderDndLock = 6;
        static final int TRANSACTION_isInGameMode = 19;
        static final int TRANSACTION_isSoundFeatureEnabled = 10;
        static final int TRANSACTION_setAlarmGameDndLock = 16;
        static final int TRANSACTION_setCallGameDndLock = 7;
        static final int TRANSACTION_setCustomActivityDndLock = 14;
        static final int TRANSACTION_setGameDndLock = 3;
        static final int TRANSACTION_setGameDndPolicy = 12;
        static final int TRANSACTION_setGameFirewallBlackList = 1;
        static final int TRANSACTION_setInGameMode = 18;
        static final int TRANSACTION_setScreenRecorderDndLock = 5;

        public Stub() {
            attachInterface(this, IAsusGameModeService.DESCRIPTOR);
        }

        public static IAsusGameModeService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IAsusGameModeService.DESCRIPTOR);
            if (iin != null && (iin instanceof IAsusGameModeService)) {
                return (IAsusGameModeService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "setGameFirewallBlackList";
                case 2:
                    return "getGameFirewallBlackList";
                case 3:
                    return "setGameDndLock";
                case 4:
                    return "getGameDndLock";
                case 5:
                    return "setScreenRecorderDndLock";
                case 6:
                    return "getScreenRecorderDndLock";
                case 7:
                    return "setCallGameDndLock";
                case 8:
                    return "getCallGameDndLock";
                case 9:
                    return "getGameDndMode";
                case 10:
                    return "isSoundFeatureEnabled";
                case 11:
                    return "denySound";
                case 12:
                    return "setGameDndPolicy";
                case 13:
                    return "getGameDndPolicy";
                case 14:
                    return "setCustomActivityDndLock";
                case 15:
                    return "getCustomActivityDndLock";
                case 16:
                    return "setAlarmGameDndLock";
                case 17:
                    return "getAlarmGameDndLock";
                case 18:
                    return "setInGameMode";
                case 19:
                    return "isInGameMode";
                case 20:
                    return "blockForGameFirewall";
                default:
                    return null;
            }
        }

        @Override
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            GameDndPolicy _arg1;
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(IAsusGameModeService.DESCRIPTOR);
                    return true;
                default:
                    boolean _arg12 = false;
                    switch (code) {
                        case 1:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg0 = data.readString();
                            int _arg13 = data.readInt();
                            if (data.readInt() != 0) {
                                _arg12 = true;
                            }
                            setGameFirewallBlackList(_arg0, _arg13, _arg12);
                            reply.writeNoException();
                            return true;
                        case 2:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg02 = data.readString();
                            int[] _result = getGameFirewallBlackList(_arg02);
                            reply.writeNoException();
                            reply.writeIntArray(_result);
                            return true;
                        case 3:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg03 = data.readString();
                            if (data.readInt() != 0) {
                                _arg12 = true;
                            }
                            setGameDndLock(_arg03, _arg12);
                            reply.writeNoException();
                            return true;
                        case 4:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            boolean gameDndLock = getGameDndLock();
                            reply.writeNoException();
                            reply.writeInt(gameDndLock ? 1 : 0);
                            return true;
                        case 5:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg04 = data.readString();
                            if (data.readInt() != 0) {
                                _arg12 = true;
                            }
                            setScreenRecorderDndLock(_arg04, _arg12);
                            reply.writeNoException();
                            return true;
                        case 6:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            boolean screenRecorderDndLock = getScreenRecorderDndLock();
                            reply.writeNoException();
                            reply.writeInt(screenRecorderDndLock ? 1 : 0);
                            return true;
                        case 7:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg05 = data.readString();
                            if (data.readInt() != 0) {
                                _arg12 = true;
                            }
                            setCallGameDndLock(_arg05, _arg12);
                            reply.writeNoException();
                            return true;
                        case 8:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            boolean callGameDndLock = getCallGameDndLock();
                            reply.writeNoException();
                            reply.writeInt(callGameDndLock ? 1 : 0);
                            return true;
                        case 9:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            int _result2 = getGameDndMode();
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            return true;
                        case 10:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            boolean isSoundFeatureEnabled = isSoundFeatureEnabled();
                            reply.writeNoException();
                            reply.writeInt(isSoundFeatureEnabled ? 1 : 0);
                            return true;
                        case 11:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg06 = data.readString();
                            int _arg14 = data.readInt();
                            int _arg2 = data.readInt();
                            boolean denySound = denySound(_arg06, _arg14, _arg2);
                            reply.writeNoException();
                            reply.writeInt(denySound ? 1 : 0);
                            return true;
                        case 12:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg07 = data.readString();
                            if (data.readInt() != 0) {
                                _arg1 = GameDndPolicy.CREATOR.createFromParcel(data);
                            } else {
                                _arg1 = null;
                            }
                            setGameDndPolicy(_arg07, _arg1);
                            reply.writeNoException();
                            return true;
                        case 13:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg08 = data.readString();
                            GameDndPolicy _result3 = getGameDndPolicy(_arg08);
                            reply.writeNoException();
                            if (_result3 != null) {
                                reply.writeInt(1);
                                _result3.writeToParcel(reply, 1);
                            } else {
                                reply.writeInt(0);
                            }
                            return true;
                        case 14:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg09 = data.readString();
                            if (data.readInt() != 0) {
                                _arg12 = true;
                            }
                            setCustomActivityDndLock(_arg09, _arg12);
                            reply.writeNoException();
                            return true;
                        case 15:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            boolean customActivityDndLock = getCustomActivityDndLock();
                            reply.writeNoException();
                            reply.writeInt(customActivityDndLock ? 1 : 0);
                            return true;
                        case 16:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg010 = data.readString();
                            if (data.readInt() != 0) {
                                _arg12 = true;
                            }
                            setAlarmGameDndLock(_arg010, _arg12);
                            reply.writeNoException();
                            return true;
                        case 17:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            boolean alarmGameDndLock = getAlarmGameDndLock();
                            reply.writeNoException();
                            reply.writeInt(alarmGameDndLock ? 1 : 0);
                            return true;
                        case 18:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            String _arg011 = data.readString();
                            if (data.readInt() != 0) {
                                _arg12 = true;
                            }
                            setInGameMode(_arg011, _arg12);
                            reply.writeNoException();
                            return true;
                        case 19:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            boolean isInGameMode = isInGameMode();
                            reply.writeNoException();
                            reply.writeInt(isInGameMode ? 1 : 0);
                            return true;
                        case 20:
                            data.enforceInterface(IAsusGameModeService.DESCRIPTOR);
                            int _arg012 = data.readInt();
                            boolean blockForGameFirewall = blockForGameFirewall(_arg012);
                            reply.writeNoException();
                            reply.writeInt(blockForGameFirewall ? 1 : 0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        private static class Proxy implements IAsusGameModeService {
            public static IAsusGameModeService sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IAsusGameModeService.DESCRIPTOR;
            }

            @Override
            public void setGameFirewallBlackList(String pkg, int uid, boolean add) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    _data.writeInt(add ? 1 : 0);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setGameFirewallBlackList(pkg, uid, add);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getGameFirewallBlackList(String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getGameFirewallBlackList(pkg);
                    }
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setGameDndLock(String pkg, boolean lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(lock ? 1 : 0);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setGameDndLock(pkg, lock);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getGameDndLock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getGameDndLock();
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setScreenRecorderDndLock(String pkg, boolean lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(lock ? 1 : 0);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setScreenRecorderDndLock(pkg, lock);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getScreenRecorderDndLock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getScreenRecorderDndLock();
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setCallGameDndLock(String pkg, boolean lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(lock ? 1 : 0);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setCallGameDndLock(pkg, lock);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getCallGameDndLock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(8, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getCallGameDndLock();
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getGameDndMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getGameDndMode();
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSoundFeatureEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isSoundFeatureEnabled();
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean denySound(String pkg, int usage, int gameMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(usage);
                    _data.writeInt(gameMode);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().denySound(pkg, usage, gameMode);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setGameDndPolicy(String pkg, GameDndPolicy policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    if (policy != null) {
                        _data.writeInt(1);
                        policy.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(12, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setGameDndPolicy(pkg, policy);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public GameDndPolicy getGameDndPolicy(String pkg) throws RemoteException {
                GameDndPolicy _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    boolean _status = this.mRemote.transact(13, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getGameDndPolicy(pkg);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = GameDndPolicy.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setCustomActivityDndLock(String pkg, boolean lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(lock ? 1 : 0);
                    boolean _status = this.mRemote.transact(14, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setCustomActivityDndLock(pkg, lock);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getCustomActivityDndLock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(15, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getCustomActivityDndLock();
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setAlarmGameDndLock(String pkg, boolean lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(lock ? 1 : 0);
                    boolean _status = this.mRemote.transact(16, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setAlarmGameDndLock(pkg, lock);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getAlarmGameDndLock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(17, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAlarmGameDndLock();
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setInGameMode(String pkg, boolean inGame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(inGame ? 1 : 0);
                    boolean _status = this.mRemote.transact(18, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setInGameMode(pkg, inGame);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isInGameMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(19, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isInGameMode();
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean blockForGameFirewall(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusGameModeService.DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(20, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().blockForGameFirewall(uid);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _status2 = z;
                    return _status2;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IAsusGameModeService impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IAsusGameModeService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}

