package com.grarak.ytfetcher.utils;

import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;

import com.grarak.ytfetcher.R;

public class EqualizerManager {

    private final Context context;

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private PresetReverb presetReverb;
    private Virtualizer virtualizer;

    private int audioSessionId;

    public EqualizerManager(Context context) {
        this.context = context;

        equalizer = new Equalizer(0, 0);
        bassBoost = new BassBoost(0, 0);
        presetReverb = new PresetReverb(0, 0);
        virtualizer = new Virtualizer(0, 0);
    }

    public void setAudioSessionId(int id) {
        if (audioSessionId == id) return;
        audioSessionId = id;

        release();

        equalizer = new Equalizer(0, id);
        bassBoost = new BassBoost(0, id);
        presetReverb = new PresetReverb(0, id);
        virtualizer = new Virtualizer(0, id);

        equalizer.setEnabled(true);
        bassBoost.setEnabled(true);
        presetReverb.setEnabled(true);
        virtualizer.setEnabled(true);

        apply();
    }

    public int getNumberOfBands() {
        return equalizer.getNumberOfBands();
    }

    public int getBandLevelLower() {
        return equalizer.getBandLevelRange()[0];
    }

    public int getBandLevelUpper() {
        return equalizer.getBandLevelRange()[1];
    }

    public int getBandLevel(int band) {
        return Prefs.getInt("band_level_" + band, equalizer.getBandLevel((short) band), context);
    }

    public int getBandLevelCenterFreq(int band) {
        return equalizer.getCenterFreq((short) band);
    }

    public void setBandLevel(int band, int level) {
        Prefs.saveBoolean("use_band_level_preset", false, context);
        Prefs.saveInt("band_level_" + band, level, context);
        if (audioSessionId != 0) {
            apply();
        }
    }

    public int getNumberOfBandPresets() {
        return equalizer.getNumberOfPresets() + 1;
    }

    public String getBandPresetName(int preset) {
        if (preset == 0) return "Custom";
        return equalizer.getPresetName((short) (preset - 1));
    }

    public int getBandPreset() {
        return Prefs.getInt("band_level_preset", equalizer.getCurrentPreset(), context) + 1;
    }

    public void setBandPreset(int preset) {
        Prefs.saveBoolean("use_band_level_preset", true, context);
        Prefs.saveInt("band_level_preset", preset - 1, context);
        if (audioSessionId != 0) {
            apply();
        }
    }

    public int getBassBoostLower() {
        return 0;
    }

    public int getBassBoostUpper() {
        return 1000;
    }

    public void setBassBoostStrength(int strength) {
        Prefs.saveInt("bassboost_strength", strength, context);
        if (audioSessionId != 0) {
            apply();
        }
    }

    public int getBassBoostStrength() {
        return Prefs.getInt("bassboost_strength", bassBoost.getRoundedStrength(), context);
    }

    public int getNumberOfReverbPresets() {
        return context.getResources().getStringArray(R.array.reverb_presets).length;
    }

    public String getReverbPresetName(int preset) {
        return context.getResources().getStringArray(R.array.reverb_presets)[preset];
    }

    public int getReverbPreset() {
        return Prefs.getInt("reverb_preset", presetReverb.getPreset(), context);
    }

    public void setReverbPreset(int preset) {
        Prefs.saveInt("reverb_preset", preset, context);
        if (audioSessionId != 0) {
            apply();
        }
    }

    public int getVirtualizerLower() {
        return 0;
    }

    public int getVirtualizerUpper() {
        return 1000;
    }

    public void setVirtualizerStrength(int strength) {
        Prefs.saveInt("virtualizer_strength", strength, context);
        if (audioSessionId != 0) {
            apply();
        }
    }

    public int getVirtualizerStrength() {
        return Prefs.getInt("virtualizer_strength",
                virtualizer.getRoundedStrength(), context);
    }

    private void apply() {
        boolean useBandPreset = Prefs.getBoolean("use_band_level_preset", false, context);
        if (useBandPreset) {
            int bandLevelPreset = Prefs.getInt("band_level_preset", Integer.MIN_VALUE, context);
            if (bandLevelPreset != Integer.MIN_VALUE && bandLevelPreset >= 0) {
                equalizer.usePreset((short) bandLevelPreset);
            }
            for (int i = 0; i < getNumberOfBands(); i++) {
                Prefs.remove("band_level_" + i, context);
            }
        } else {
            for (int i = 0; i < getNumberOfBands(); i++) {
                int level = Prefs.getInt("band_level_" + i, Integer.MIN_VALUE, context);
                if (level != Integer.MIN_VALUE) {
                    equalizer.setBandLevel((short) i, (short) level);
                }
            }
        }

        int bassBoostStrength = Prefs.getInt("bassboost_strength", Integer.MIN_VALUE, context);
        if (bassBoostStrength != Integer.MIN_VALUE) {
            bassBoost.setStrength((short) bassBoostStrength);
        }

        int reverbPreset = Prefs.getInt("reverb_preset", Integer.MIN_VALUE, context);
        if (reverbPreset != Integer.MIN_VALUE) {
            presetReverb.setPreset((short) reverbPreset);
        }

        int virtualizerStrength = Prefs.getInt("virtualizer_strength", Integer.MIN_VALUE, context);
        if (virtualizerStrength != Integer.MIN_VALUE) {
            virtualizer.setStrength((short) virtualizerStrength);
        }
    }

    public void release() {
        equalizer.release();
        bassBoost.release();
        presetReverb.release();
        virtualizer.release();
    }
}
