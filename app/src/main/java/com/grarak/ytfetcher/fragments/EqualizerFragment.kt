package com.grarak.ytfetcher.fragments

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.fragments.titles.TitleFragment
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import com.grarak.ytfetcher.views.recyclerview.SeekBarItem
import com.grarak.ytfetcher.views.recyclerview.SelectorItem
import java.util.*

class EqualizerFragment : RecyclerViewFragment<TitleFragment>() {

    override val layoutXml: Int = R.layout.fragment_equalizer

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun init(savedInstanceState: Bundle?) {}

    override fun initItems(items: ArrayList<RecyclerViewItem>) {
        val manager = equalizerManager

        val bandRange = ArrayList<String>()
        for (i in manager!!.bandLevelLower / 100..manager.bandLevelUpper / 100) {
            bandRange.add(getString(R.string.db, i))
        }

        val bands = ArrayList<SeekBarItem>()

        val bandPresets = SelectorItem(object : SelectorItem.SelectorListener {
            override fun onItemSelected(item: SelectorItem, items: List<String>, position: Int) {
                manager.bandPreset = position
                for (i in bands.indices) {
                    val seekBarItem = bands[i]
                    seekBarItem.current = (manager.getBandLevel(i) - manager.bandLevelLower) / 100
                }
            }
        })

        for (i in 0 until manager.numberOfBands) {

            val band = SeekBarItem(object : SeekBarItem.SeekBarListener {
                override fun onSeekStop(item: SeekBarItem, items: List<String>, position: Int) {
                    manager.setBandLevel(i,
                            (position * 100 + manager.bandLevelLower).toShort().toInt())
                    bandPresets.position = 0
                }
            })

            band.title = getString(R.string.hz,
                    manager.getBandLevelCenterFreq(i) / 1000)
            band.items = bandRange
            band.current = (manager.getBandLevel(i) - manager.bandLevelLower) / 100

            items.add(band)
            bands.add(band)
        }

        val bandPresetList = ArrayList<String>()
        for (i in 0 until manager.numberOfBandPresets) {
            bandPresetList.add(manager.getBandPresetName(i))
        }

        bandPresets.title = getString(R.string.profile)
        bandPresets.items = bandPresetList
        bandPresets.position = manager.bandPreset
        items.add(bandPresets)

        val bassStrengthRange = (manager.bassBoostUpper - manager.bassBoostLower).toFloat()
        val bassStrength = (manager.bassBoostStrength - manager.bassBoostLower).toFloat()
        val bassBoostRangeList = ArrayList<String>()
        for (i in 0..100) {
            bassBoostRangeList.add(String.format(Locale.getDefault(), "%d%%", i))
        }
        val bassBoostSeekBar = SeekBarItem(object : SeekBarItem.SeekBarListener {
            override fun onSeekStop(item: SeekBarItem, items: List<String>, position: Int) {
                manager.bassBoostStrength = Math.round(
                        position / 100f * bassStrengthRange + manager.bassBoostLower)
            }

        })
        bassBoostSeekBar.title = getString(R.string.bass_boost)
        bassBoostSeekBar.items = bassBoostRangeList
        bassBoostSeekBar.current = Math.round(bassStrength / bassStrengthRange * 100)
        items.add(bassBoostSeekBar)

        val virtualizerStrengthRange = (manager.virtualizerUpper - manager.virtualizerLower).toFloat()
        val virtualizerStrength = (manager.virtualizerStrength - manager.virtualizerLower).toFloat()
        val virtualizerRangeList = ArrayList<String>()
        for (i in 0..100) {
            virtualizerRangeList.add(String.format(Locale.getDefault(), "%d%%", i))
        }
        val virtualizerSeekBar = SeekBarItem(object : SeekBarItem.SeekBarListener {
            override fun onSeekStop(item: SeekBarItem, items: List<String>, position: Int) {
                manager.virtualizerStrength = Math.round(
                        position / 100f * virtualizerStrengthRange + manager.virtualizerLower)
            }
        })
        virtualizerSeekBar.title = getString(R.string.surround_sound)
        virtualizerSeekBar.items = virtualizerRangeList
        virtualizerSeekBar.current = Math.round(virtualizerStrength / virtualizerStrengthRange * 100)
        items.add(virtualizerSeekBar)

        val hallPresetList = ArrayList<String>()
        for (i in 0 until manager.numberOfReverbPresets) {
            hallPresetList.add(manager.getReverbPresetName(i))
        }
        val hallProfile = SelectorItem(object : SelectorItem.SelectorListener {
            override fun onItemSelected(item: SelectorItem, items: List<String>, position: Int) {
                manager.reverbPreset = position
            }
        })
        hallProfile.title = getString(R.string.hall)
        hallProfile.items = hallPresetList
        hallProfile.position = manager.reverbPreset
        items.add(hallProfile)
    }
}
