package com.grarak.ytfetcher.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.View

import com.grarak.ytfetcher.LoginActivity
import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.fragments.titles.TitleFragment
import com.grarak.ytfetcher.utils.Prefs
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem
import com.grarak.ytfetcher.views.recyclerview.settings.ButtonItem
import com.grarak.ytfetcher.views.recyclerview.settings.SwitchItem

class SettingsFragment : RecyclerViewFragment<TitleFragment>() {

    private var signoutDialog: Boolean = false

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun onResume() {
        super.onResume()

        if (signoutDialog) {
            signoutDialog()
        }
    }

    override fun init(savedInstanceState: Bundle?) {}

    override fun initItems(items: ArrayList<RecyclerViewItem>) {
        val downloads = ButtonItem(View.OnClickListener {
            showForegroundFragment(DownloadsFragment.newInstance(user!!))
        })
        downloads.text = getString(R.string.downloads)
        items.add(downloads)

        val history = ButtonItem(View.OnClickListener {
            showForegroundFragment(HistoryFragment.newInstance(user!!))
        })
        history.text = getString(R.string.history)
        items.add(history)

        val equalizer = ButtonItem(View.OnClickListener {
            showForegroundFragment(EqualizerFragment())
        })
        equalizer.text = getString(R.string.equalizer)
        items.add(equalizer)

        val darkTheme = SwitchItem(object : SwitchItem.SwitchListener {
            override fun onCheckedChanged(checked: Boolean) {
                Settings.setDarkTheme(checked, activity!!)
                val intent = Intent(activity, LoginActivity::class.java)
                startActivity(intent)
                activity!!.finish()
            }
        })
        darkTheme.text = getString(R.string.dark_theme)
        darkTheme.checked = Settings.isDarkTheme(activity!!)
        items.add(darkTheme)

        val signout = ButtonItem(View.OnClickListener {
            signoutDialog()
        })
        signout.text = getString(R.string.signout)
        signout.textColor = Color.WHITE
        signout.backgroundColor = Color.RED
        items.add(signout)

        val licenses = ButtonItem(View.OnClickListener {
            showForegroundFragment(LicenseFragment())
        })
        licenses.text = getString(R.string.licenses)
        items.add(licenses)
    }

    private fun signoutDialog() {
        signoutDialog = true
        AlertDialog.Builder(activity!!)
                .setMessage(R.string.sure_question)
                .setPositiveButton(R.string.yes) { _, _ ->
                    musicManager!!.destroy()
                    Prefs.clear(activity!!)
                    activity!!.finish()
                    startActivity(Intent(activity, LoginActivity::class.java))
                }
                .setNegativeButton(R.string.no, null)
                .setOnDismissListener { signoutDialog = false }.show()
    }
}
