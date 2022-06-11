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

import android.os.Parcel;
import android.os.Parcelable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class GameNetworkPolicy implements Parcelable {
    public static final Parcelable.Creator<GameNetworkPolicy> CREATOR = new Parcelable.Creator<GameNetworkPolicy>() {
        @Override
        public GameNetworkPolicy createFromParcel(Parcel parcel) {
            return new GameNetworkPolicy(parcel);
        }

        @Override
        public GameNetworkPolicy[] newArray(int size) {
            return new GameNetworkPolicy[size];
        }
    };
    public static final boolean DEFAULT_GNP_ENABLED = true;
    public static final String FIREWALL_TAG = "firewall";
    public static final String GNP_ATT_ENABLED = "enabled";
    public static final String GNP_ATT_REJECT_UID = "reject_uid";
    public static final String GNP_ATT_VERSION = "version";
    public static final String GNP_TAG = "game-network-policy";
    public static final int INVALID_UID = -1;
    static final int XML_VERSION = 1;
    public boolean enabled;
    public ArrayList<Integer> reject_uid;
    public int version;

    public GameNetworkPolicy() {
        this.enabled = true;
        this.reject_uid = new ArrayList<>();
    }

    public GameNetworkPolicy(Parcel source) {
        boolean z = true;
        this.enabled = true;
        this.reject_uid = new ArrayList<>();
        this.version = source.readInt();
        this.enabled = source.readInt() != 1 ? false : z;
        this.reject_uid = (ArrayList) source.readSerializable();
    }

    public static GameNetworkPolicy readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        int uid;
        if (parser.getEventType() != 2 || !GNP_TAG.equals(parser.getName())) {
            return null;
        }
        GameNetworkPolicy gnp = new GameNetworkPolicy();
        gnp.version = GameModeUtils.safeInt(parser, "version", 1);
        gnp.enabled = GameModeUtils.safeBoolean(parser, "enabled", true);
        while (true) {
            int type = parser.next();
            if (type != 1) {
                String tag = parser.getName();
                if (type == 3 && GNP_TAG.equals(tag)) {
                    return gnp;
                }
                if (type == 2 && FIREWALL_TAG.equals(tag) && (uid = GameModeUtils.safeInt(parser, GNP_ATT_REJECT_UID, -1)) != -1) {
                    gnp.reject_uid.add(Integer.valueOf(uid));
                }
            } else {
                throw new IllegalStateException("Failed to reach END_DOCUMENT");
            }
        }
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.version), Boolean.valueOf(this.enabled), this.reject_uid);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(GameNetworkPolicy.class.getSimpleName()).append('[');
        sb.append("version=");
        sb.append(this.version);
        sb.append(",enabled=");
        sb.append(this.enabled);
        ArrayList<Integer> arrayList = this.reject_uid;
        if (arrayList != null && arrayList.size() > 0) {
            sb.append(",reject_uid=");
            int i = 0;
            Iterator<Integer> it = this.reject_uid.iterator();
            while (it.hasNext()) {
                int agnu = it.next().intValue();
                sb.append(agnu);
                int i2 = i + 1;
                if (i != this.reject_uid.size() - 1) {
                    sb.append("; ");
                }
                i = i2;
            }
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeInt(this.version);
        dest.writeInt(this.enabled ? 1 : 0);
        dest.writeSerializable(this.reject_uid);
    }

    public void writeXml(XmlSerializer out, Integer version) throws IOException {
        out.startTag(null, GNP_TAG);
        out.attribute(null, "version", Integer.toString(version == null ? 1 : version.intValue()));
        out.attribute(null, "enabled", Boolean.toString(this.enabled));
        Iterator<Integer> it = this.reject_uid.iterator();
        while (it.hasNext()) {
            int uid = it.next().intValue();
            out.startTag(null, FIREWALL_TAG);
            out.attribute(null, GNP_ATT_REJECT_UID, Integer.toString(uid));
            out.endTag(null, FIREWALL_TAG);
        }
        out.endTag(null, GNP_TAG);
    }

    public GameNetworkPolicy copy() {
        Parcel parcel = Parcel.obtain();
        try {
            writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return new GameNetworkPolicy(parcel);
        } finally {
            parcel.recycle();
        }
    }
}
