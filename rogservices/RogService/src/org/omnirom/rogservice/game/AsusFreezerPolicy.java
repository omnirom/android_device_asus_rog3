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

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

public class AsusFreezerPolicy implements Parcelable {
    public static final Parcelable.Creator<AsusFreezerPolicy> CREATOR = new Parcelable.Creator<AsusFreezerPolicy>() {
        @Override
        public AsusFreezerPolicy createFromParcel(Parcel source) {
            return new AsusFreezerPolicy(source);
        }
        @Override
        public AsusFreezerPolicy[] newArray(int size) {
            return new AsusFreezerPolicy[size];
        }
    };
    public String[] mCanNotFreezeProcesses;
    public long mFreezeDuration;
    public long mFreezeWaitingTime;
    public int mNeedFreeze;
    public String[] mNeedFreezeProcesses;
    public String mReason;

    private AsusFreezerPolicy(Parcel in) {
        mReason = "unknown";
        mNeedFreeze = in.readInt();
        mFreezeWaitingTime = in.readLong();
        mFreezeDuration = in.readLong();
        mReason = in.readString();
        mNeedFreezeProcesses = in.createStringArray();
        mCanNotFreezeProcesses = in.createStringArray();
    }

    public String toString() {
        return "needFreeze=" + mNeedFreeze + ",freezeWaitingTime=" + mFreezeWaitingTime + ",reason=" + mReason;
    }

    public void setReason(String reason) {
        mReason = reason;
    }

    public AsusFreezerPolicy(boolean needFreeze, long freezeWaitingTime) {
        this(needFreeze ? 1 : 0, freezeWaitingTime, 0L, null, null, "unknown");
    }

    public AsusFreezerPolicy(boolean needFreeze, long freezeWaitingTime, String reason) {
        this(needFreeze ? 1 : 0, freezeWaitingTime, 0L, null, null, reason);
    }

    public AsusFreezerPolicy(int needFreeze, long freezeWaitingTime, long freezeDuration, List<String> needFreezeProcessesList, List<String> canNotFreezeProcessesList, String reason) {
        mReason = "unknown";
        mNeedFreeze = needFreeze;
        mFreezeWaitingTime = freezeWaitingTime;
        mFreezeDuration = freezeDuration;
        if (needFreezeProcessesList != null && needFreezeProcessesList.size() > 0) {
            String[] strArr = new String[needFreezeProcessesList.size()];
            mNeedFreezeProcesses = strArr;
            mNeedFreezeProcesses = (String[]) needFreezeProcessesList.toArray(strArr);
        } else {
            mNeedFreezeProcesses = null;
        }
        if (canNotFreezeProcessesList != null && canNotFreezeProcessesList.size() > 0) {
            String[] strArr2 = new String[canNotFreezeProcessesList.size()];
            mCanNotFreezeProcesses = strArr2;
            mCanNotFreezeProcesses = (String[]) canNotFreezeProcessesList.toArray(strArr2);
        } else {
            mCanNotFreezeProcesses = null;
        }
        mReason = reason;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeInt(mNeedFreeze);
        dest.writeLong(mFreezeWaitingTime);
        dest.writeLong(mFreezeDuration);
        dest.writeString(mReason);
        dest.writeStringArray(mNeedFreezeProcesses);
        dest.writeStringArray(mCanNotFreezeProcesses);
    }
}
