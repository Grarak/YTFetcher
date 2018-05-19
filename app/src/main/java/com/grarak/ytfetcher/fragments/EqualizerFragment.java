package com.grarak.ytfetcher.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.utils.EqualizerManager;
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem;
import com.grarak.ytfetcher.views.recyclerview.SeekBarItem;
import com.grarak.ytfetcher.views.recyclerview.SelectorItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EqualizerFragment extends RecyclerViewFragment<TitleFragment> {

    @Override
    protected int getLayoutXml() {
        return R.layout.fragment_equalizer;
    }

    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @Override
    protected void init(Bundle savedInstanceState) {
    }

    @Override
    protected void initItems(List<RecyclerViewItem> items) {
        EqualizerManager manager = getEqualizerManager();

        List<String> bandRange = new ArrayList<>();
        for (int i = manager.getBandLevelLower() / 100;
             i <= manager.getBandLevelUpper() / 100; i++) {
            bandRange.add(getString(R.string.db, i));
        }

        List<SeekBarItem> bands = new ArrayList<>();

        SelectorItem bandPresets = new SelectorItem((item, items12, position) -> {
            manager.setBandPreset(position);
            for (int i = 0; i < bands.size(); i++) {
                SeekBarItem seekBarItem = bands.get(i);
                seekBarItem.setCurrent((manager.getBandLevel(i) - manager.getBandLevelLower()) / 100);
            }
        });

        for (short i = 0; i < manager.getNumberOfBands(); i++) {
            final short bandIndex = i;

            SeekBarItem band = new SeekBarItem((item, items1, position)
                    -> {
                manager.setBandLevel(bandIndex,
                        (short) (position * 100 + manager.getBandLevelLower()));
                bandPresets.setPosition(0);
            });

            band.setTitle(getString(R.string.hz,
                    manager.getBandLevelCenterFreq(bandIndex) / 1000));
            band.setItems(bandRange);
            band.setCurrent((manager.getBandLevel(bandIndex) - manager.getBandLevelLower()) / 100);

            items.add(band);
            bands.add(band);
        }

        List<String> bandPresetList = new ArrayList<>();
        for (int i = 0; i < manager.getNumberOfBandPresets(); i++) {
            bandPresetList.add(manager.getBandPresetName(i));
        }

        bandPresets.setTitle(getString(R.string.profile));
        bandPresets.setItems(bandPresetList);
        bandPresets.setPosition(manager.getBandPreset());
        items.add(bandPresets);

        float bassStrengthRange = manager.getBassBoostUpper() - manager.getBassBoostLower();
        float bassStrength = manager.getBassBoostStrength() - manager.getBassBoostLower();
        List<String> bassBoostRangeList = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            bassBoostRangeList.add(String.format(Locale.getDefault(), "%d%%", i));
        }
        SeekBarItem bassBoostSeekBar = new SeekBarItem((item, items13, position)
                -> manager.setBassBoostStrength(Math.round(
                position / 100f * bassStrengthRange + manager.getBassBoostLower())));
        bassBoostSeekBar.setTitle(getString(R.string.bass_boost));
        bassBoostSeekBar.setItems(bassBoostRangeList);
        bassBoostSeekBar.setCurrent(Math.round(bassStrength / bassStrengthRange * 100));
        items.add(bassBoostSeekBar);

        float virtualizerStrengthRange = manager.getVirtualizerUpper() - manager.getVirtualizerLower();
        float virtualizerStrength = manager.getVirtualizerStrength() - manager.getVirtualizerLower();
        List<String> virtualizerRangeList = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            virtualizerRangeList.add(String.format(Locale.getDefault(), "%d%%", i));
        }
        SeekBarItem virtualizerSeekBar = new SeekBarItem((item, items13, position)
                -> manager.setVirtualizerStrength(Math.round(
                position / 100f * virtualizerStrengthRange + manager.getVirtualizerLower())));
        virtualizerSeekBar.setTitle(getString(R.string.surround_sound));
        virtualizerSeekBar.setItems(virtualizerRangeList);
        virtualizerSeekBar.setCurrent(Math.round(virtualizerStrength / virtualizerStrengthRange * 100));
        items.add(virtualizerSeekBar);

        List<String> hallPresetList = new ArrayList<>();
        for (int i = 0; i < manager.getNumberOfReverbPresets(); i++) {
            hallPresetList.add(manager.getReverbPresetName(i));
        }
        SelectorItem hallProfile = new SelectorItem((item, items14, position)
                -> manager.setReverbPreset(position));
        hallProfile.setTitle(getString(R.string.hall));
        hallProfile.setItems(hallPresetList);
        hallProfile.setPosition(manager.getReverbPreset());
        items.add(hallProfile);
    }
}
