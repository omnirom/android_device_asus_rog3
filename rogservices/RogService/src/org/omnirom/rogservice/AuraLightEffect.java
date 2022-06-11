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

import android.os.Parcel;
import android.os.Parcelable;

public final class AuraLightEffect implements Parcelable {

    private int mColor;
    private int mDuration;
    private int mRate;
    private int mType;

    public AuraLightEffect(int type, int color, int rate, int duration) {
        mType = type;
        mColor = color;
        mRate = rate;
        mDuration = duration;
    }

    public AuraLightEffect(Parcel in) {
        mType = in.readInt();
        mColor = in.readInt();
        mRate = in.readInt();
        mDuration = in.readInt();
    }

    public int getType() {
        return mType;
    }

    public int getColor() {
        return mColor;
    }

    public int getRate() {
        return mRate;
    }

    public int getDuration() {
        return mDuration;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mType);
        out.writeInt(mColor);
        out.writeInt(mRate);
        out.writeInt(mDuration);
    }

    public String toString() {
        return "AuraLightEffect { mType=" + mType + ", mColor=0x" + Integer.toHexString(mColor) + ", mRate=" + mRate + ", mDuration=" + mDuration + "}";
    }

    public static final Parcelable.Creator<AuraLightEffect> CREATOR =
            new Parcelable.Creator<AuraLightEffect>() {
        @Override
        public AuraLightEffect createFromParcel(Parcel source) {
            return new AuraLightEffect(source);
        }

        @Override
        public AuraLightEffect[] newArray(int size) {
            return new AuraLightEffect[size];
        }
    };
}
