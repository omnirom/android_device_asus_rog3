# Copyright (C) 2016 The CyanogenMod Project
# Copyright (C) 2019 The OmniRom Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# This file is the build configuration for a full Android
# build for grouper hardware. This cleanly combines a set of
# device-specific aspects (drivers) with a device-agnostic
# product configuration (apps).
#

# Overlays
DEVICE_PACKAGE_OVERLAYS += \
    $(LOCAL_PATH)/overlay

PRODUCT_PACKAGES += \
    FrameworksResDeviceOverlay \
    FrameworksResVendorOverlay \
    SystemUIDeviceOverlay

# Api
PRODUCT_SHIPPING_API_LEVEL := 29

# audio
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/audio/audio_policy_configuration_ZS661KS.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio/audio_policy_configuration.xml \
    $(LOCAL_PATH)/audio/audio_policy_configuration_ZS661KS.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_policy_configuration.xml \
    $(LOCAL_PATH)/audio/audio_policy_volumes_ZS661KS.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_policy_volumes_ZS661KS.xml

# Biometrics
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint@2.3-service.rog3

# Display
PRODUCT_PACKAGES += \
    android.hardware.graphics.mapper@3.0-impl-qti-display \
    android.hardware.graphics.mapper@4.0-impl-qti-display \
    android.hardware.memtrack@1.0-impl \
    android.hardware.memtrack@1.0-service \
    android.hardware.renderscript@1.0-impl \
    gralloc.kona \
    libqdutils \
    libqservice \
    libsdmcore \
    libsdmutils \
    libvulkan \
    memtrack.kona \
    vendor.qti.hardware.display.allocator-service \
    vendor.qti.hardware.display.composer-service \
    vendor.qti.hardware.display.mapper@1.0.vendor \
    vendor.qti.hardware.display.mapper@1.1.vendor \
    vendor.qti.hardware.display.mapper@2.0.vendor \
    vendor.qti.hardware.display.mapper@3.0.vendor \
    vendor.qti.hardware.display.mapper@4.0.vendor

# Input
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/idc/goodix_ts.idc:system/usr/idc/goodix_ts.idc \
    $(LOCAL_PATH)/idc/goodix_ts_station.idc:system/usr/idc/goodix_ts_station.idc \
    $(LOCAL_PATH)/idc/eGalaxTouch_EXC3200.idc:system/usr/idc/eGalaxTouch_EXC3200.idc \
    $(LOCAL_PATH)/keychars/goodix_ts.kcm:system/usr/keychars/goodix_ts.kcm \
    $(LOCAL_PATH)/keylayout/goodix_ts.kl:system/usr/keylayout/goodix_ts.kl \
    $(LOCAL_PATH)/keylayout/i-rocks_Bluetooth_Keyboard.kl:system/usr/keylayout/i-rocks_Bluetooth_Keyboard.kl

# Lights
PRODUCT_PACKAGES += \
    android.hardware.light@2.0-service.obiwan

# Prebuilt
PRODUCT_COPY_FILES += \
    $(call find-copy-subdir-files,*,device/asus/rog3/prebuilt/system,system) \
    $(call find-copy-subdir-files,*,device/asus/rog3/prebuilt/root,recovery/root) \
    $(call find-copy-subdir-files,*,device/asus/rog3/prebuilt/system_root,root) \
    $(call find-copy-subdir-files,*,device/asus/rog3/prebuilt/vendor,vendor)

PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xxhdpi

# Ramdisk
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/fstab.qcom:$(TARGET_COPY_OUT_RAMDISK)/fstab.qcom

# Sensors
PRODUCT_PACKAGES += \
    android.hardware.sensors@2.1-service.multihal \
    libsensorndkbridge

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/sensors/hals.conf:$(TARGET_COPY_OUT_VENDOR)/etc/sensors/hals.conf

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)

# Vibrator
PRODUCT_PACKAGES += \
    android.hardware.vibrator.service.rog3

# Inherit from asus sm8250-common
$(call inherit-product, device/asus/sm8250-common/common.mk)

# Inherit from vendor blobs
$(call inherit-product, vendor/asus/rog3/rog3-vendor.mk)
$(call inherit-product, vendor/images/asus/rog3/app/rog3-app.mk)
