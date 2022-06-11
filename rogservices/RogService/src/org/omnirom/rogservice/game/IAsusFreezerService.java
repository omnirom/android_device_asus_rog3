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

import android.media.AudioPlaybackConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import java.util.Map;

public interface IAsusFreezerService extends IInterface {
    public static final String DESCRIPTOR = "org.omnirom.rogservice.game.IAsusFreezerService";

    List<String> getAlarmAlignPrefixList() throws RemoteException;

    Map getAlarmVarMap() throws RemoteException;

    int getAppBehavior(String str, int i, int i2) throws RemoteException;

    List<String> getBlackOrWhiteList(int i, int i2) throws RemoteException;

    boolean getDownloadBehavior(String str, int i) throws RemoteException;

    Map getNetVarMap() throws RemoteException;

    Map getOptiFlexVarMap() throws RemoteException;

    AsusFreezerPolicy getPolicy(String str, int i, int i2) throws RemoteException;

    AsusFreezerPolicy[] getPolicyArray(String[] strArr, int[] iArr, int[] iArr2) throws RemoteException;

    void prepareDownloadCheck(long j) throws RemoteException;

    void updateAudioPlaybackEvent(AudioPlaybackConfiguration audioPlaybackConfiguration) throws RemoteException;

    public static class Default implements IAsusFreezerService {
        @Override
        public AsusFreezerPolicy[] getPolicyArray(String[] packages, int[] uids, int[] events) throws RemoteException {
            return null;
        }

        @Override
        public AsusFreezerPolicy getPolicy(String pkg, int uid, int event) throws RemoteException {
            return null;
        }

        @Override
        public void updateAudioPlaybackEvent(AudioPlaybackConfiguration apc) throws RemoteException {
        }

        @Override
        public Map getNetVarMap() throws RemoteException {
            return null;
        }

        @Override
        public List<String> getBlackOrWhiteList(int feature, int isBlack) throws RemoteException {
            return null;
        }

        @Override
        public int getAppBehavior(String pkg, int uid, int binaryNeedEvents) throws RemoteException {
            return 0;
        }

        @Override
        public void prepareDownloadCheck(long timeout) throws RemoteException {
        }

        @Override
        public boolean getDownloadBehavior(String pkg, int uid) throws RemoteException {
            return false;
        }

        @Override
        public Map getAlarmVarMap() throws RemoteException {
            return null;
        }

        @Override
        public List<String> getAlarmAlignPrefixList() throws RemoteException {
            return null;
        }

        @Override
        public Map getOptiFlexVarMap() throws RemoteException {
            return null;
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends Binder implements IAsusFreezerService {
        static final int TRANSACTION_getAlarmAlignPrefixList = 10;
        static final int TRANSACTION_getAlarmVarMap = 9;
        static final int TRANSACTION_getAppBehavior = 6;
        static final int TRANSACTION_getBlackOrWhiteList = 5;
        static final int TRANSACTION_getDownloadBehavior = 8;
        static final int TRANSACTION_getNetVarMap = 4;
        static final int TRANSACTION_getOptiFlexVarMap = 11;
        static final int TRANSACTION_getPolicy = 2;
        static final int TRANSACTION_getPolicyArray = 1;
        static final int TRANSACTION_prepareDownloadCheck = 7;
        static final int TRANSACTION_updateAudioPlaybackEvent = 3;

        public Stub() {
            attachInterface(this, IAsusFreezerService.DESCRIPTOR);
        }

        public static IAsusFreezerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IAsusFreezerService.DESCRIPTOR);
            if (iin != null && (iin instanceof IAsusFreezerService)) {
                return (IAsusFreezerService) iin;
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
                    return "getPolicyArray";
                case 2:
                    return "getPolicy";
                case 3:
                    return "updateAudioPlaybackEvent";
                case 4:
                    return "getNetVarMap";
                case 5:
                    return "getBlackOrWhiteList";
                case 6:
                    return "getAppBehavior";
                case 7:
                    return "prepareDownloadCheck";
                case 8:
                    return "getDownloadBehavior";
                case 9:
                    return "getAlarmVarMap";
                case 10:
                    return "getAlarmAlignPrefixList";
                case 11:
                    return "getOptiFlexVarMap";
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
            AudioPlaybackConfiguration _arg0;
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(IAsusFreezerService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            String[] _arg02 = data.createStringArray();
                            int[] _arg1 = data.createIntArray();
                            int[] _arg2 = data.createIntArray();
                            AsusFreezerPolicy[] _result = getPolicyArray(_arg02, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeTypedArray(_result, 1);
                            return true;
                        case 2:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            String _arg03 = data.readString();
                            int _arg12 = data.readInt();
                            int _arg22 = data.readInt();
                            AsusFreezerPolicy _result2 = getPolicy(_arg03, _arg12, _arg22);
                            reply.writeNoException();
                            if (_result2 != null) {
                                reply.writeInt(1);
                                _result2.writeToParcel(reply, 1);
                            } else {
                                reply.writeInt(0);
                            }
                            return true;
                        case 3:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            if (data.readInt() != 0) {
                                _arg0 = AudioPlaybackConfiguration.CREATOR.createFromParcel(data);
                            } else {
                                _arg0 = null;
                            }
                            updateAudioPlaybackEvent(_arg0);
                            reply.writeNoException();
                            return true;
                        case 4:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            Map _result3 = getNetVarMap();
                            reply.writeNoException();
                            reply.writeMap(_result3);
                            return true;
                        case 5:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            int _arg04 = data.readInt();
                            int _arg13 = data.readInt();
                            List<String> _result4 = getBlackOrWhiteList(_arg04, _arg13);
                            reply.writeNoException();
                            reply.writeStringList(_result4);
                            return true;
                        case 6:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            String _arg05 = data.readString();
                            int _arg14 = data.readInt();
                            int _arg23 = data.readInt();
                            int _result5 = getAppBehavior(_arg05, _arg14, _arg23);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            return true;
                        case 7:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            long _arg06 = data.readLong();
                            prepareDownloadCheck(_arg06);
                            reply.writeNoException();
                            return true;
                        case 8:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            String _arg07 = data.readString();
                            int _arg15 = data.readInt();
                            boolean downloadBehavior = getDownloadBehavior(_arg07, _arg15);
                            reply.writeNoException();
                            reply.writeInt(downloadBehavior ? 1 : 0);
                            return true;
                        case 9:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            Map _result6 = getAlarmVarMap();
                            reply.writeNoException();
                            reply.writeMap(_result6);
                            return true;
                        case 10:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            List<String> _result7 = getAlarmAlignPrefixList();
                            reply.writeNoException();
                            reply.writeStringList(_result7);
                            return true;
                        case 11:
                            data.enforceInterface(IAsusFreezerService.DESCRIPTOR);
                            Map _result8 = getOptiFlexVarMap();
                            reply.writeNoException();
                            reply.writeMap(_result8);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        private static class Proxy implements IAsusFreezerService {
            public static IAsusFreezerService sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IAsusFreezerService.DESCRIPTOR;
            }

            @Override
            public AsusFreezerPolicy[] getPolicyArray(String[] packages, int[] uids, int[] events) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    _data.writeStringArray(packages);
                    _data.writeIntArray(uids);
                    _data.writeIntArray(events);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getPolicyArray(packages, uids, events);
                    }
                    _reply.readException();
                    AsusFreezerPolicy[] _result = (AsusFreezerPolicy[]) _reply.createTypedArray(AsusFreezerPolicy.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public AsusFreezerPolicy getPolicy(String pkg, int uid, int event) throws RemoteException {
                AsusFreezerPolicy _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    _data.writeInt(event);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getPolicy(pkg, uid, event);
                    }
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = AsusFreezerPolicy.CREATOR.createFromParcel(_reply);
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
            public void updateAudioPlaybackEvent(AudioPlaybackConfiguration apc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    if (apc != null) {
                        _data.writeInt(1);
                        apc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().updateAudioPlaybackEvent(apc);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Map getNetVarMap() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getNetVarMap();
                    }
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    Map _result = _reply.readHashMap(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getBlackOrWhiteList(int feature, int isBlack) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    _data.writeInt(feature);
                    _data.writeInt(isBlack);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getBlackOrWhiteList(feature, isBlack);
                    }
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getAppBehavior(String pkg, int uid, int binaryNeedEvents) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    _data.writeInt(binaryNeedEvents);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAppBehavior(pkg, uid, binaryNeedEvents);
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
            public void prepareDownloadCheck(long timeout) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    _data.writeLong(timeout);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().prepareDownloadCheck(timeout);
                    } else {
                        _reply.readException();
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getDownloadBehavior(String pkg, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    boolean z = false;
                    boolean _status = this.mRemote.transact(8, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getDownloadBehavior(pkg, uid);
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
            public Map getAlarmVarMap() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAlarmVarMap();
                    }
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    Map _result = _reply.readHashMap(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<String> getAlarmAlignPrefixList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAlarmAlignPrefixList();
                    }
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Map getOptiFlexVarMap() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAsusFreezerService.DESCRIPTOR);
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getOptiFlexVarMap();
                    }
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    Map _result = _reply.readHashMap(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(IAsusFreezerService impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IAsusFreezerService getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}

