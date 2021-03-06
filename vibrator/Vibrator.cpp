/*
 * Copyright (C) 2017 The Android Open Source Project
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

#define LOG_TAG "VibratorService"

#include <log/log.h>

#include <android-base/logging.h>
#include <hardware/hardware.h>
#include <hardware/vibrator.h>
#include <cutils/properties.h>

#include "Vibrator.h"

#include <cinttypes>
#include <cmath>
#include <iostream>
#include <fstream>

static constexpr char ACTIVATE_PATH[] = "/sys/class/leds/vibrator/activate";
static constexpr char DURATION_PATH[] = "/sys/class/leds/vibrator/duration";
static constexpr char STATE_PATH[] = "/sys/class/leds/vibrator/state";
static constexpr char EFFECT_INDEX_PATH[] = "/sys/class/leds/vibrator/lp_trigger_effect";
static constexpr char SCALE_PATH[] = "/sys/class/leds/vibrator/scale";

namespace android {
namespace hardware {
namespace vibrator {
namespace V1_2 {
namespace implementation {

using Status = ::android::hardware::vibrator::V1_0::Status;
using EffectStrength = ::android::hardware::vibrator::V1_0::EffectStrength;

static constexpr uint32_t WAVEFORM_TICK_EFFECT_INDEX = 2;
static constexpr uint32_t WAVEFORM_TICK_EFFECT_MS = 15;

static constexpr uint32_t WAVEFORM_CLICK_EFFECT_INDEX = 3;
static constexpr uint32_t WAVEFORM_CLICK_EFFECT_MS = 11;

static constexpr uint32_t WAVEFORM_HEAVY_CLICK_EFFECT_INDEX = 4;
static constexpr uint32_t WAVEFORM_HEAVY_CLICK_EFFECT_MS = 15;

static constexpr uint32_t WAVEFORM_DOUBLE_CLICK_EFFECT_INDEX = 7;
static constexpr uint32_t WAVEFORM_DOUBLE_CLICK_EFFECT_MS = 130;

static constexpr float AMP_ATTENUATE_STEP_SIZE = 0.125f;

static uint8_t amplitudeToScale(uint8_t amplitude, uint8_t maximum) {
    return std::round((-20 * std::log10(amplitude / static_cast<float>(maximum))) /
                      (AMP_ATTENUATE_STEP_SIZE));
}

/*
 * Write value to path and close file.
 */
template <typename T>
static void set(const std::string& path, const T& value) {
    std::ofstream file(path);

    if (!file.is_open()) {
        LOG(ERROR) << "Unable to open: " << path << " (" <<  strerror(errno) << ")";
        return;
    }

    file << value;
}

Vibrator::Vibrator()
{}

Return<Status> Vibrator::on(uint32_t timeoutMs, uint32_t effectIndex) {
    set(STATE_PATH, 1);
    set(DURATION_PATH, timeoutMs);
    set(EFFECT_INDEX_PATH, effectIndex);
    set(ACTIVATE_PATH, 1);

    return Status::OK;
}

// Methods from ::android::hardware::vibrator::V1_1::IVibrator follow.
Return<Status> Vibrator::on(uint32_t timeoutMs) {
    return on(timeoutMs, 0);
}

Return<Status> Vibrator::off()  {
    set(ACTIVATE_PATH, 0);
    return Status::OK;
}

Return<bool> Vibrator::supportsAmplitudeControl()  {
    return true;
}

Return<Status> Vibrator::setAmplitude(uint8_t amplitude) {
    if (amplitude == 0) {
        return Status::BAD_VALUE;
    }

    int32_t scale = amplitudeToScale(amplitude, UINT8_MAX);
    set(SCALE_PATH, scale);

    return Status::OK;
}

Return<void> Vibrator::perform(V1_0::Effect effect, EffectStrength strength,
        perform_cb _hidl_cb) {
    return performEffect(static_cast<Effect>(effect), strength, _hidl_cb);
}

Return<void> Vibrator::perform_1_1(V1_1::Effect_1_1 effect, EffectStrength strength,
        perform_cb _hidl_cb) {
    return performEffect(static_cast<Effect>(effect), strength, _hidl_cb);
}

Return<void> Vibrator::perform_1_2(Effect effect, EffectStrength strength,
        perform_cb _hidl_cb) {
    return performEffect(static_cast<Effect>(effect), strength, _hidl_cb);
}

Return<void> Vibrator::performEffect(Effect effect, EffectStrength strength,
        perform_cb _hidl_cb) {
    Status status = Status::OK;
    uint32_t timeMs;
    uint32_t effectIndex;

    switch (effect) {
    case Effect::TICK:
        effectIndex = WAVEFORM_TICK_EFFECT_INDEX;
        timeMs = WAVEFORM_TICK_EFFECT_MS;
        break;
    case Effect::CLICK:
        effectIndex = WAVEFORM_CLICK_EFFECT_INDEX;
        timeMs = WAVEFORM_CLICK_EFFECT_MS;
        break;
    case Effect::HEAVY_CLICK:
        effectIndex = WAVEFORM_HEAVY_CLICK_EFFECT_INDEX;
        timeMs = WAVEFORM_HEAVY_CLICK_EFFECT_MS;
        break;
    case Effect::DOUBLE_CLICK:
        effectIndex = WAVEFORM_DOUBLE_CLICK_EFFECT_INDEX;
        timeMs = WAVEFORM_DOUBLE_CLICK_EFFECT_MS;
        break;
    default:
        _hidl_cb(Status::UNSUPPORTED_OPERATION, 0);
        return Void();
    }

    switch (strength) {
    case EffectStrength::LIGHT:
        effectIndex -= 1;
        break;
    case EffectStrength::MEDIUM:
        break;
    case EffectStrength::STRONG:
        effectIndex += 1;
        break;
    default:
        _hidl_cb(Status::UNSUPPORTED_OPERATION, 0);
        return Void();
    }

    setAmplitude(UINT8_MAX); // Always set full-scale for non-ringtone constants
    on(timeMs, effectIndex);
    _hidl_cb(status, timeMs);

    return Void();
}


} // namespace implementation
}  // namespace V1_2
}  // namespace vibrator
}  // namespace hardware
}  // namespace android