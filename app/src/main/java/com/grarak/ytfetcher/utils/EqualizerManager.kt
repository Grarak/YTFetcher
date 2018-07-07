package com.grarak.ytfetcher.utils

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer

import com.grarak.ytfetcher.R

class EqualizerManager(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null
    private var virtualizer: Virtualizer? = null

    private var audioSessionId: Int = 0

    val numberOfBands: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 0

    val bandLevelLower: Int
        get() = equalizer?.bandLevelRange?.get(0)?.toInt() ?: 0

    val bandLevelUpper: Int
        get() = equalizer?.bandLevelRange?.get(1)?.toInt() ?: 0

    val numberOfBandPresets: Int
        get() = (equalizer?.numberOfPresets ?: 0) + 1

    var bandPreset: Int
        get() = Prefs.getInt("band_level_preset", equalizer?.currentPreset?.toInt()
                ?: -1, context) + 1
        set(preset) {
            Prefs.saveBoolean("use_band_level_preset", true, context)
            Prefs.saveInt("band_level_preset", preset - 1, context)
            if (audioSessionId != 0) {
                apply()
            }
        }

    val bassBoostLower: Int
        get() = 0

    val bassBoostUpper: Int
        get() = 1000

    var bassBoostStrength: Int
        get() = Prefs.getInt("bassboost_strength", bassBoost?.roundedStrength?.toInt()
                ?: 0, context)
        set(strength) {
            Prefs.saveInt("bassboost_strength", strength, context)
            if (audioSessionId != 0) {
                apply()
            }
        }

    val numberOfReverbPresets: Int
        get() = context.resources.getStringArray(R.array.reverb_presets).size

    var reverbPreset: Int
        get() = Prefs.getInt("reverb_preset", presetReverb?.preset?.toInt() ?: 0, context)
        set(preset) {
            Prefs.saveInt("reverb_preset", preset, context)
            if (audioSessionId != 0) {
                apply()
            }
        }

    val virtualizerLower: Int
        get() = 0

    val virtualizerUpper: Int
        get() = 1000

    var virtualizerStrength: Int
        get() = Prefs.getInt("virtualizer_strength",
                virtualizer?.roundedStrength?.toInt() ?: 0, context)
        set(strength) {
            Prefs.saveInt("virtualizer_strength", strength, context)
            if (audioSessionId != 0) {
                apply()
            }
        }

    init {
        try {
            equalizer = Equalizer(0, 0)
            bassBoost = BassBoost(0, 0)
            presetReverb = PresetReverb(0, 0)
            virtualizer = Virtualizer(0, 0)
        } catch (ignored: IllegalArgumentException) {
        }
    }

    fun setAudioSessionId(id: Int) {
        if (audioSessionId == id) return
        audioSessionId = id

        release()

        try {
            equalizer = Equalizer(0, id)
            bassBoost = BassBoost(0, id)
            presetReverb = PresetReverb(0, id)
            virtualizer = Virtualizer(0, id)
        } catch (ignored: IllegalArgumentException) {
        }

        equalizer?.enabled = true
        bassBoost?.enabled = true
        presetReverb?.enabled = true
        virtualizer?.enabled = true

        apply()
    }

    fun getBandLevel(band: Int): Int {
        return Prefs.getInt("band_level_$band", equalizer?.getBandLevel(band.toShort())?.toInt()
                ?: 0, context)
    }

    fun getBandLevelCenterFreq(band: Int): Int {
        return equalizer?.getCenterFreq(band.toShort()) ?: 0
    }

    fun setBandLevel(band: Int, level: Int) {
        Prefs.saveBoolean("use_band_level_preset", false, context)
        Prefs.saveInt("band_level_$band", level, context)
        if (audioSessionId != 0) {
            apply()
        }
    }

    fun getBandPresetName(preset: Int): String {
        return if (preset == 0) "Custom" else equalizer?.getPresetName((preset - 1).toShort())
                ?: "Unknown"
    }

    fun getReverbPresetName(preset: Int): String {
        return context.resources.getStringArray(R.array.reverb_presets)[preset]
    }

    private fun apply() {
        equalizer?.run {

            val useBandPreset = Prefs.getBoolean("use_band_level_preset", false, context)
            if (useBandPreset) {
                val bandLevelPreset = Prefs.getInt("band_level_preset", Integer.MIN_VALUE, context)
                if (bandLevelPreset != Integer.MIN_VALUE && bandLevelPreset >= 0) {
                    usePreset(bandLevelPreset.toShort())
                }
                for (i in 0 until numberOfBands) {
                    Prefs.remove("band_level_$i", context)
                }
            } else {
                for (i in 0 until numberOfBands) {
                    val level = Prefs.getInt("band_level_$i", Integer.MIN_VALUE, context)
                    if (level != Integer.MIN_VALUE) {
                        setBandLevel(i.toShort(), level.toShort())
                    }
                }
            }
        }

        bassBoost?.run {
            val bassBoostStrength = Prefs.getInt("bassboost_strength", Integer.MIN_VALUE, context)
            if (bassBoostStrength != Integer.MIN_VALUE) {
                setStrength(bassBoostStrength.toShort())
            }
        }

        presetReverb?.run {
            val reverbPreset = Prefs.getInt("reverb_preset", Integer.MIN_VALUE, context)
            if (reverbPreset != Integer.MIN_VALUE) {
                preset = reverbPreset.toShort()
            }
        }

        virtualizer?.run {
            val virtualizerStrength = Prefs.getInt("virtualizer_strength", Integer.MIN_VALUE, context)
            if (virtualizerStrength != Integer.MIN_VALUE) {
                setStrength(virtualizerStrength.toShort())
            }
        }
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        presetReverb?.release()
        virtualizer?.release()
    }
}
