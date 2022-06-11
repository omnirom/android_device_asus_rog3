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

public class GameDndPolicy implements Parcelable {
    public static final String AOW_TAG = "allow-overlay-window";
    public static final String CA_TAG = "custom-activity";
    public static final String DA_TAG = "deny-activity";
    public static final boolean DEFAULT_ACTIVITY_ENABLED = true;
    public static final boolean DEFAULT_CUSTOM_ACTIVITY_ENABLED = true;
    public static final boolean DEFAULT_NOTIFICATION_LIST_ENABLED = true;
    public static final boolean DEFAULT_SOUND_ENABLED = true;
    public static final boolean DEFAULT_TOAST_ENABLED = true;
    public static final boolean DEFAULT_VIBRATION_ENABLED = true;
    public static final String DNL_TAG = "deny-notification-list";
    public static final String DS_TAG = "deny-sound";
    public static final String DV_TAG = "deny-vibration";
    public static final String GAME_DND_ATT_ACTIVITY_ENABLED = "activity_enabled";
    public static final String GAME_DND_ATT_CATEGORY = "category";
    public static final String GAME_DND_ATT_COMPONENT = "component";
    public static final String GAME_DND_ATT_CUSTOM_ACTIVITY_ENABLED = "custom_activity_enabled";
    public static final String GAME_DND_ATT_GAME_MODE = "game-mode";
    public static final String GAME_DND_ATT_GROUPKEY = "groupkey";
    public static final String GAME_DND_ATT_KEYWORDS = "keywords";
    public static final String GAME_DND_ATT_NOTIFICATION_LIST_ENABLED = "notification_list_enabled";
    public static final String GAME_DND_ATT_NTAG = "ntag";
    public static final String GAME_DND_ATT_PKG = "package";
    public static final String GAME_DND_ATT_SHOW = "show";
    public static final String GAME_DND_ATT_SOUND_ENABLED = "sound_enabled";
    public static final String GAME_DND_ATT_TOAST_ENABLED = "toast_enabled";
    public static final String GAME_DND_ATT_USAGE = "usage";
    public static final String GAME_DND_ATT_VERSION = "version";
    public static final String GAME_DND_ATT_VIBRATION_ENABLED = "vibration_enabled";
    public static final String GDP_TAG = "game-dnd-policy";
    public static final int USAGE_DND_ALL = 21399;
    public static final int XML_VERSION = 14;
    public ArrayList<ActivityDnd> activityDnds;
    public boolean activity_enabled;
    public ArrayList<ActivityDndCustomView> customActivityDnds;
    public boolean custom_activity_enabled;
    public ArrayList<NotificationListDnd> notificationListDnds;
    public boolean notification_list_enabled;
    public ArrayList<SoundDnd> soundDnds;
    public boolean sound_enabled;
    public ArrayList<ToastWindowDnd> toastWindowDnds;
    public boolean toast_enabled;
    public int version;
    public ArrayList<VibrationDnd> vibrationDnds;
    public boolean vibration_enabled;
    public static int GAME_DND_NONE = 0;
    public static int GAME_DND_NOTIFICATION = 1;
    public static int GAME_DND_CALL = 2;
    public static int GAME_DND_CUSTOM_ACTIVITY = 4;
    public static int GAME_DND_ALL = 1 | 2;
    public static String GMN = "GAME_DND_NONE";
    public static String GDN = "GAME_DND_NOTIFICATION";
    public static String GDC = "GAME_DND_CALL";
    public static String GDCA = "GAME_DND_CUSTOM_ACTIVITY";
    public static final Parcelable.Creator<GameDndPolicy> CREATOR = new Parcelable.Creator<GameDndPolicy>() {
        @Override
        public GameDndPolicy createFromParcel(Parcel parcel) {
            return new GameDndPolicy(parcel);
        }

        @Override
        public GameDndPolicy[] newArray(int size) {
            return new GameDndPolicy[size];
        }
    };

    public GameDndPolicy() {
        toastWindowDnds = new ArrayList<>();
        activityDnds = new ArrayList<>();
        this.soundDnds = new ArrayList<>();
        vibrationDnds = new ArrayList<>();
        customActivityDnds = new ArrayList<>();
        notificationListDnds = new ArrayList<>();
        version = 14;
        toast_enabled = true;
        activity_enabled = true;
        sound_enabled = true;
        vibration_enabled = true;
        custom_activity_enabled = true;
        notification_list_enabled = true;
    }

    public GameDndPolicy(Parcel source) {
        toastWindowDnds = new ArrayList<>();
        activityDnds = new ArrayList<>();
        this.soundDnds = new ArrayList<>();
        vibrationDnds = new ArrayList<>();
        customActivityDnds = new ArrayList<>();
        notificationListDnds = new ArrayList<>();
        version = source.readInt();
        boolean z = false;
        toast_enabled = source.readInt() == 1;
        activity_enabled = source.readInt() == 1;
        sound_enabled = source.readInt() == 1;
        vibration_enabled = source.readInt() == 1;
        source.readTypedList(toastWindowDnds, ToastWindowDnd.CREATOR);
        source.readTypedList(activityDnds, ActivityDnd.CREATOR);
        source.readTypedList(this.soundDnds, SoundDnd.CREATOR);
        source.readTypedList(vibrationDnds, VibrationDnd.CREATOR);
        custom_activity_enabled = source.readInt() == 1;
        source.readTypedList(customActivityDnds, ActivityDndCustomView.CREATOR);
        notification_list_enabled = source.readInt() == 1 ? true : z;
        source.readTypedList(notificationListDnds, NotificationListDnd.CREATOR);
    }

    public boolean equals(Object o) {
        if (!(o instanceof GameDndPolicy)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        GameDndPolicy other = (GameDndPolicy) o;
        return other.version == version && other.toast_enabled == toast_enabled &&
                other.activity_enabled == activity_enabled && other.sound_enabled == sound_enabled &&
                other.vibration_enabled == vibration_enabled && Objects.equals(other.toastWindowDnds, toastWindowDnds) &&
                Objects.equals(other.activityDnds, activityDnds) && Objects.equals(other.soundDnds, this.soundDnds) &&
                Objects.equals(other.vibrationDnds, vibrationDnds) && other.custom_activity_enabled == custom_activity_enabled &&
                Objects.equals(other.customActivityDnds, customActivityDnds) && other.notification_list_enabled == notification_list_enabled &&
                Objects.equals(other.notificationListDnds, notificationListDnds);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(version), Boolean.valueOf(toast_enabled),
                Boolean.valueOf(activity_enabled), Boolean.valueOf(sound_enabled),
                Boolean.valueOf(vibration_enabled), toastWindowDnds, activityDnds,
                this.soundDnds, vibrationDnds, Boolean.valueOf(custom_activity_enabled),
                customActivityDnds, Boolean.valueOf(notification_list_enabled), notificationListDnds);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder().append('{');
        sb.append("version=");
        sb.append(version);
        sb.append(",toast_enabled=");
        sb.append(toast_enabled);
        sb.append(",activity_enabled=");
        sb.append(activity_enabled);
        sb.append(",sound_enabled=");
        sb.append(sound_enabled);
        sb.append(",vibration_enabled=");
        sb.append(vibration_enabled);
        sb.append(",custom_activity_enabled=");
        sb.append(custom_activity_enabled);
        sb.append(",notification_list_enabled=");
        sb.append(notification_list_enabled);
        ArrayList<ToastWindowDnd> arrayList = toastWindowDnds;
        if (arrayList != null && arrayList.size() > 0) {
            sb.append("\n");
            sb.append(",toastWindowDnds=[");
            int i = 0;
            Iterator<ToastWindowDnd> it = toastWindowDnds.iterator();
            while (it.hasNext()) {
                ToastWindowDnd twd = it.next();
                sb.append(twd.toString());
                int i2 = i + 1;
                if (i != toastWindowDnds.size() - 1) {
                    sb.append("; ");
                }
                i = i2;
            }
            sb.append(']');
        }
        ArrayList<ActivityDnd> arrayList2 = activityDnds;
        if (arrayList2 != null && arrayList2.size() > 0) {
            sb.append("\n");
            sb.append(",activityDnds=[");
            int i3 = 0;
            Iterator<ActivityDnd> it2 = activityDnds.iterator();
            while (it2.hasNext()) {
                ActivityDnd ad = it2.next();
                sb.append(ad.toString());
                int i4 = i3 + 1;
                if (i3 != activityDnds.size() - 1) {
                    sb.append("; ");
                }
                i3 = i4;
            }
            sb.append(']');
        }
        ArrayList<SoundDnd> arrayList3 = this.soundDnds;
        if (arrayList3 != null && arrayList3.size() > 0) {
            sb.append("\n");
            sb.append(",soundDnds=[");
            int i5 = 0;
            Iterator<SoundDnd> it3 = this.soundDnds.iterator();
            while (it3.hasNext()) {
                SoundDnd sd = it3.next();
                sb.append(sd.toString());
                int i6 = i5 + 1;
                if (i5 != this.soundDnds.size() - 1) {
                    sb.append("; ");
                }
                i5 = i6;
            }
            sb.append(']');
        }
        ArrayList<VibrationDnd> arrayList4 = vibrationDnds;
        if (arrayList4 != null && arrayList4.size() > 0) {
            sb.append("\n");
            sb.append(",vibrationDnds=[");
            int i7 = 0;
            Iterator<VibrationDnd> it4 = vibrationDnds.iterator();
            while (it4.hasNext()) {
                VibrationDnd vd = it4.next();
                sb.append(vd.toString());
                int i8 = i7 + 1;
                if (i7 != vibrationDnds.size() - 1) {
                    sb.append("; ");
                }
                i7 = i8;
            }
            sb.append(']');
        }
        ArrayList<ActivityDndCustomView> arrayList5 = customActivityDnds;
        if (arrayList5 != null && arrayList5.size() > 0) {
            sb.append("\n");
            sb.append(",customActivityDnds=[");
            int i9 = 0;
            Iterator<ActivityDndCustomView> it5 = customActivityDnds.iterator();
            while (it5.hasNext()) {
                ActivityDndCustomView cad = it5.next();
                sb.append(cad.toString());
                int i10 = i9 + 1;
                if (i9 != customActivityDnds.size() - 1) {
                    sb.append("; ");
                }
                i9 = i10;
            }
            sb.append(']');
        }
        ArrayList<NotificationListDnd> arrayList6 = notificationListDnds;
        if (arrayList6 != null && arrayList6.size() > 0) {
            sb.append("\n");
            sb.append(",notificationListDnds=[");
            int i11 = 0;
            Iterator<NotificationListDnd> it6 = notificationListDnds.iterator();
            while (it6.hasNext()) {
                NotificationListDnd nld = it6.next();
                sb.append(nld.toString());
                int i12 = i11 + 1;
                if (i11 != notificationListDnds.size() - 1) {
                    sb.append("; ");
                }
                i11 = i12;
            }
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeInt(version);
        dest.writeInt(toast_enabled ? 1 : 0);
        dest.writeInt(activity_enabled ? 1 : 0);
        dest.writeInt(sound_enabled ? 1 : 0);
        dest.writeInt(vibration_enabled ? 1 : 0);
        dest.writeTypedList(toastWindowDnds);
        dest.writeTypedList(activityDnds);
        dest.writeTypedList(this.soundDnds);
        dest.writeTypedList(vibrationDnds);
        dest.writeInt(custom_activity_enabled ? 1 : 0);
        dest.writeTypedList(customActivityDnds);
        dest.writeInt(notification_list_enabled ? 1 : 0);
        dest.writeTypedList(notificationListDnds);
    }

    public static GameDndPolicy readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() == 2 && GDP_TAG.equals(parser.getName())) {
            GameDndPolicy gdp = new GameDndPolicy();
            gdp.version = GameModeUtils.safeInt(parser, "version", 14);
            gdp.toast_enabled = GameModeUtils.safeBoolean(parser, GAME_DND_ATT_TOAST_ENABLED, true);
            gdp.activity_enabled = GameModeUtils.safeBoolean(parser, GAME_DND_ATT_ACTIVITY_ENABLED, true);
            gdp.sound_enabled = GameModeUtils.safeBoolean(parser, GAME_DND_ATT_SOUND_ENABLED, true);
            gdp.vibration_enabled = GameModeUtils.safeBoolean(parser, GAME_DND_ATT_VIBRATION_ENABLED, true);
            gdp.custom_activity_enabled = GameModeUtils.safeBoolean(parser, GAME_DND_ATT_CUSTOM_ACTIVITY_ENABLED, true);
            gdp.notification_list_enabled = GameModeUtils.safeBoolean(parser, GAME_DND_ATT_NOTIFICATION_LIST_ENABLED, true);
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    String tag = parser.getName();
                    if (type == 3 && GDP_TAG.equals(tag)) {
                        return gdp;
                    }
                    if (type == 2) {
                        if (AOW_TAG.equals(tag)) {
                            String pkg = parser.getAttributeValue(null, "package");
                            int gameMode = GameModeUtils.getGameMode(parser.getAttributeValue(null, GAME_DND_ATT_GAME_MODE));
                            ToastWindowDnd twd = new ToastWindowDnd(pkg, gameMode);
                            gdp.toastWindowDnds.add(twd);
                        } else if (DA_TAG.equals(tag)) {
                            String component = parser.getAttributeValue(null, "component");
                            int gameMode2 = GameModeUtils.getGameMode(parser.getAttributeValue(null, GAME_DND_ATT_GAME_MODE));
                            ActivityDnd ad = new ActivityDnd(component, gameMode2);
                            gdp.activityDnds.add(ad);
                        } else if (DS_TAG.equals(tag)) {
                            String pkg2 = parser.getAttributeValue(null, "package");
                            int usage = GameModeUtils.getUsage(parser.getAttributeValue(null, GAME_DND_ATT_USAGE));
                            int gameMode3 = GameModeUtils.getGameMode(parser.getAttributeValue(null, GAME_DND_ATT_GAME_MODE));
                            if (GameModeUtils.validUsage(usage)) {
                                SoundDnd sd = new SoundDnd(pkg2, usage, gameMode3);
                                gdp.soundDnds.add(sd);
                            }
                        } else if (DV_TAG.equals(tag)) {
                            String pkg3 = parser.getAttributeValue(null, "package");
                            int usage2 = GameModeUtils.getUsage(parser.getAttributeValue(null, GAME_DND_ATT_USAGE));
                            int gameMode4 = GameModeUtils.getGameMode(parser.getAttributeValue(null, GAME_DND_ATT_GAME_MODE));
                            if (GameModeUtils.validUsage(usage2)) {
                                VibrationDnd vd = new VibrationDnd(pkg3, usage2, gameMode4);
                                gdp.vibrationDnds.add(vd);
                            }
                        } else if (CA_TAG.equals(tag)) {
                            String component2 = parser.getAttributeValue(null, "component");
                            boolean show = GameModeUtils.safeBoolean(parser, "show", true);
                            int gameMode5 = GameModeUtils.getGameMode(parser.getAttributeValue(null, GAME_DND_ATT_GAME_MODE));
                            ActivityDndCustomView adc = new ActivityDndCustomView(component2, show, gameMode5);
                            gdp.customActivityDnds.add(adc);
                        } else if (DNL_TAG.equals(tag)) {
                            String pkg4 = parser.getAttributeValue(null, "package");
                            String ntag = parser.getAttributeValue(null, GAME_DND_ATT_NTAG);
                            String category = parser.getAttributeValue(null, "category");
                            String groupKey = parser.getAttributeValue(null, GAME_DND_ATT_GROUPKEY);
                            String keywords = parser.getAttributeValue(null, "keywords");
                            int gameMode6 = GameModeUtils.getGameMode(parser.getAttributeValue(null, GAME_DND_ATT_GAME_MODE));
                            NotificationListDnd nld = new NotificationListDnd(pkg4, ntag, category, groupKey, keywords, gameMode6);
                            gdp.notificationListDnds.add(nld);
                        }
                    }
                } else {
                    throw new IllegalStateException("Failed to reach END_DOCUMENT");
                }
            }
        } else {
            return null;
        }
    }

    public void writeXml(XmlSerializer out, Integer version) throws IOException {
        out.startTag(null, GDP_TAG);
        out.attribute(null, "version", Integer.toString(version == null ? 14 : version.intValue()));
        out.attribute(null, GAME_DND_ATT_TOAST_ENABLED, Boolean.toString(toast_enabled));
        out.attribute(null, GAME_DND_ATT_ACTIVITY_ENABLED, Boolean.toString(activity_enabled));
        out.attribute(null, GAME_DND_ATT_SOUND_ENABLED, Boolean.toString(sound_enabled));
        out.attribute(null, GAME_DND_ATT_VIBRATION_ENABLED, Boolean.toString(vibration_enabled));
        out.attribute(null, GAME_DND_ATT_CUSTOM_ACTIVITY_ENABLED, Boolean.toString(custom_activity_enabled));
        out.attribute(null, GAME_DND_ATT_NOTIFICATION_LIST_ENABLED, Boolean.toString(notification_list_enabled));
        Iterator<ToastWindowDnd> it = toastWindowDnds.iterator();
        while (it.hasNext()) {
            ToastWindowDnd toastWindowDnd = it.next();
            out.startTag(null, AOW_TAG);
            out.attribute(null, "package", toastWindowDnd.pkg);
            out.attribute(null, GAME_DND_ATT_GAME_MODE, GameModeUtils.gameModeToString(toastWindowDnd.gameMode));
            out.endTag(null, AOW_TAG);
        }
        Iterator<ActivityDnd> it2 = activityDnds.iterator();
        while (it2.hasNext()) {
            ActivityDnd activityDnd = it2.next();
            out.startTag(null, DA_TAG);
            out.attribute(null, "component", activityDnd.component);
            out.attribute(null, GAME_DND_ATT_GAME_MODE, GameModeUtils.gameModeToString(activityDnd.gameMode));
            out.endTag(null, DA_TAG);
        }
        Iterator<SoundDnd> it3 = this.soundDnds.iterator();
        while (it3.hasNext()) {
            SoundDnd soundDnd = it3.next();
            out.startTag(null, DS_TAG);
            out.attribute(null, "package", soundDnd.pkg);
            out.attribute(null, GAME_DND_ATT_USAGE, GameModeUtils.usageToString(soundDnd.usage));
            out.attribute(null, GAME_DND_ATT_GAME_MODE, GameModeUtils.gameModeToString(soundDnd.gameMode));
            out.endTag(null, DS_TAG);
        }
        Iterator<VibrationDnd> it4 = vibrationDnds.iterator();
        while (it4.hasNext()) {
            VibrationDnd vibrationDnd = it4.next();
            out.startTag(null, DV_TAG);
            out.attribute(null, "package", vibrationDnd.pkg);
            out.attribute(null, GAME_DND_ATT_USAGE, GameModeUtils.usageToString(vibrationDnd.usage));
            out.attribute(null, GAME_DND_ATT_GAME_MODE, GameModeUtils.gameModeToString(vibrationDnd.gameMode));
            out.endTag(null, DV_TAG);
        }
        Iterator<ActivityDndCustomView> it5 = customActivityDnds.iterator();
        while (it5.hasNext()) {
            ActivityDndCustomView customActivityDnd = it5.next();
            out.startTag(null, CA_TAG);
            out.attribute(null, "component", customActivityDnd.component);
            out.attribute(null, "show", Boolean.toString(customActivityDnd.show));
            out.attribute(null, GAME_DND_ATT_GAME_MODE, GameModeUtils.gameModeToString(customActivityDnd.gameMode));
            out.endTag(null, CA_TAG);
        }
        Iterator<NotificationListDnd> it6 = notificationListDnds.iterator();
        while (it6.hasNext()) {
            NotificationListDnd notificationListDnd = it6.next();
            out.startTag(null, DNL_TAG);
            out.attribute(null, "package", notificationListDnd.pkg);
            out.attribute(null, GAME_DND_ATT_NTAG, notificationListDnd.ntag);
            out.attribute(null, "category", notificationListDnd.category);
            out.attribute(null, GAME_DND_ATT_GROUPKEY, notificationListDnd.groupKey);
            out.attribute(null, "keywords", notificationListDnd.keywords);
            out.attribute(null, GAME_DND_ATT_GAME_MODE, GameModeUtils.gameModeToString(notificationListDnd.gameMode));
            out.endTag(null, DV_TAG);
        }
        out.endTag(null, GDP_TAG);
    }

    public GameDndPolicy copy() {
        Parcel parcel = Parcel.obtain();
        try {
            writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return new GameDndPolicy(parcel);
        } finally {
            parcel.recycle();
        }
    }

    public static class ToastWindowDnd implements Parcelable {
        static final Parcelable.Creator<ToastWindowDnd> CREATOR = new Parcelable.Creator<ToastWindowDnd>() { // from class: android.game.GameDndPolicy.ToastWindowDnd.1
            @Override
            public ToastWindowDnd createFromParcel(Parcel parcel) {
                return new ToastWindowDnd(parcel);
            }

            @Override
            public ToastWindowDnd[] newArray(int size) {
                return new ToastWindowDnd[size];
            }
        };
        public int gameMode;
        public String pkg;

        public ToastWindowDnd(String pkg, int gameMode) {
            this.pkg = pkg;
            this.gameMode = gameMode;
        }

        public ToastWindowDnd(Parcel parcel) {
            this.pkg = parcel.readString();
            this.gameMode = parcel.readInt();
        }

        public String toString() {
            return "pkg=" + this.pkg + ", game-mode=" + this.gameMode;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ToastWindowDnd)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            ToastWindowDnd other = (ToastWindowDnd) o;
            String str = other.pkg;
            return str != null && str.equals(this.pkg) && other.gameMode == this.gameMode;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.pkg);
            dest.writeInt(this.gameMode);
        }
    }

    public static class ActivityDnd implements Parcelable {
        static final Parcelable.Creator<ActivityDnd> CREATOR = new Parcelable.Creator<ActivityDnd>() { // from class: android.game.GameDndPolicy.ActivityDnd.1
            @Override // android.os.Parcelable.Creator
            public ActivityDnd createFromParcel(Parcel parcel) {
                return new ActivityDnd(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ActivityDnd[] newArray(int size) {
                return new ActivityDnd[size];
            }
        };
        public String component;
        public int gameMode;

        public ActivityDnd(String component, int gameMode) {
            this.component = component;
            this.gameMode = gameMode;
        }

        public ActivityDnd(Parcel parcel) {
            this.component = parcel.readString();
            this.gameMode = parcel.readInt();
        }

        public String toString() {
            return "component=" + this.component + ", game-mode=" + this.gameMode;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ActivityDnd)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            ActivityDnd other = (ActivityDnd) o;
            String str = other.component;
            return str != null && str.equals(this.component) && other.gameMode == this.gameMode;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.component);
            dest.writeInt(this.gameMode);
        }
    }

    public static class SoundDnd implements Parcelable {
        static final Parcelable.Creator<SoundDnd> CREATOR = new Parcelable.Creator<SoundDnd>() { // from class: android.game.GameDndPolicy.SoundDnd.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SoundDnd createFromParcel(Parcel parcel) {
                return new SoundDnd(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SoundDnd[] newArray(int size) {
                return new SoundDnd[size];
            }
        };
        public int gameMode;
        public String pkg;
        public int usage;

        public SoundDnd(String pkg, int usage, int gameMode) {
            this.pkg = pkg;
            this.usage = usage;
            this.gameMode = gameMode;
        }

        public SoundDnd(Parcel parcel) {
            this.pkg = parcel.readString();
            this.usage = parcel.readInt();
            this.gameMode = parcel.readInt();
        }

        public String toString() {
            return "pkg=" + this.pkg + ", usage=" + this.usage + ", game-mode=" + this.gameMode;
        }

        public boolean equals(Object o) {
            if (!(o instanceof SoundDnd)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            SoundDnd other = (SoundDnd) o;
            String str = other.pkg;
            return str != null && str.equals(this.pkg) && other.usage == this.usage && other.gameMode == this.gameMode;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.pkg);
            dest.writeInt(this.usage);
            dest.writeInt(this.gameMode);
        }
    }

    /* loaded from: classes.dex */
    public static class VibrationDnd implements Parcelable {
        static final Parcelable.Creator<VibrationDnd> CREATOR = new Parcelable.Creator<VibrationDnd>() { // from class: android.game.GameDndPolicy.VibrationDnd.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public VibrationDnd createFromParcel(Parcel parcel) {
                return new VibrationDnd(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public VibrationDnd[] newArray(int size) {
                return new VibrationDnd[size];
            }
        };
        public int gameMode;
        public String pkg;
        public int usage;

        public VibrationDnd(String pkg, int usage, int gameMode) {
            this.pkg = pkg;
            this.usage = usage;
            this.gameMode = gameMode;
        }

        public VibrationDnd(Parcel parcel) {
            this.pkg = parcel.readString();
            this.usage = parcel.readInt();
            this.gameMode = parcel.readInt();
        }

        public String toString() {
            return "pkg=" + this.pkg + ", usage=" + this.usage + ", game-mode=" + this.gameMode;
        }

        public boolean equals(Object o) {
            if (!(o instanceof VibrationDnd)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            VibrationDnd other = (VibrationDnd) o;
            String str = other.pkg;
            return str != null && str.equals(this.pkg) && other.usage == this.usage && other.gameMode == this.gameMode;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.pkg);
            dest.writeInt(this.usage);
            dest.writeInt(this.gameMode);
        }
    }

    /* loaded from: classes.dex */
    public static class ActivityDndCustomView implements Parcelable {
        static final Parcelable.Creator<ActivityDndCustomView> CREATOR = new Parcelable.Creator<ActivityDndCustomView>() { // from class: android.game.GameDndPolicy.ActivityDndCustomView.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ActivityDndCustomView createFromParcel(Parcel parcel) {
                return new ActivityDndCustomView(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ActivityDndCustomView[] newArray(int size) {
                return new ActivityDndCustomView[size];
            }
        };
        public String component;
        public int gameMode;
        public boolean show;

        public ActivityDndCustomView(String component, boolean show, int gameMode) {
            this.component = component;
            this.show = show;
            this.gameMode = gameMode;
        }

        public ActivityDndCustomView(Parcel parcel) {
            this.component = parcel.readString();
            this.show = parcel.readInt() != 1 ? false : true;
            this.gameMode = parcel.readInt();
        }

        public String toString() {
            return "component=" + this.component + ", show=" + this.show + ", game-mode=" + this.gameMode;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ActivityDndCustomView)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            ActivityDndCustomView other = (ActivityDndCustomView) o;
            String str = other.component;
            return str != null && str.equals(this.component) && other.show == this.show && other.gameMode == this.gameMode;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.component);
            dest.writeInt(this.show ? 1 : 0);
            dest.writeInt(this.gameMode);
        }
    }

    /* loaded from: classes.dex */
    public static class NotificationListDnd implements Parcelable {
        static final Parcelable.Creator<NotificationListDnd> CREATOR = new Parcelable.Creator<NotificationListDnd>() { // from class: android.game.GameDndPolicy.NotificationListDnd.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public NotificationListDnd createFromParcel(Parcel parcel) {
                return new NotificationListDnd(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public NotificationListDnd[] newArray(int size) {
                return new NotificationListDnd[size];
            }
        };
        public String category;
        public int gameMode;
        public String groupKey;
        public String keywords;
        public String ntag;
        public String pkg;

        public NotificationListDnd(String pkg, String ntag, String category, String groupKey, String keywords, int gameMode) {
            this.pkg = "none";
            this.ntag = "NULL";
            this.category = "NULL";
            this.groupKey = "NULL";
            if (pkg != null) {
                this.pkg = pkg;
            }
            if (ntag != null) {
                this.ntag = ntag;
            }
            if (category != null) {
                this.category = category;
            }
            if (groupKey != null) {
                this.groupKey = groupKey;
            }
            this.keywords = keywords;
            this.gameMode = gameMode;
        }

        public NotificationListDnd(Parcel parcel) {
            this.pkg = "none";
            this.ntag = "NULL";
            this.category = "NULL";
            this.groupKey = "NULL";
            this.pkg = parcel.readString();
            this.ntag = parcel.readString();
            this.category = parcel.readString();
            this.groupKey = parcel.readString();
            this.keywords = parcel.readString();
            this.gameMode = parcel.readInt();
        }

        public String toString() {
            return "pkg=" + this.pkg + ", ntag=" + this.ntag + ", category=" + this.category + ", groupKey=" + this.groupKey + ", keywords=" + this.keywords + ", game-mode=" + this.gameMode;
        }

        public boolean equals(Object o) {
            if (!(o instanceof NotificationListDnd)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            NotificationListDnd other = (NotificationListDnd) o;
            String str = other.pkg;
            return str != null && other.ntag != null && other.category != null && other.groupKey != null && other.keywords != null && str.equals(this.pkg) && other.ntag.equals(this.ntag) && other.category.equals(this.category) && other.groupKey.equals(this.groupKey) && other.keywords.equals(this.keywords) && other.gameMode == this.gameMode;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.pkg);
            dest.writeString(this.ntag);
            dest.writeString(this.category);
            dest.writeString(this.groupKey);
            dest.writeString(this.keywords);
            dest.writeInt(this.gameMode);
        }
    }
}
