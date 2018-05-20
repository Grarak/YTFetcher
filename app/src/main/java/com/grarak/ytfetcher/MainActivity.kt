package com.grarak.ytfetcher

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.grarak.ytfetcher.fragments.*
import com.grarak.ytfetcher.service.MusicPlayerListener
import com.grarak.ytfetcher.utils.MusicManager
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.Utils
import com.grarak.ytfetcher.utils.server.Status
import com.grarak.ytfetcher.utils.server.user.User
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult
import com.grarak.ytfetcher.views.musicplayer.MusicPlayerParentView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import java.util.*

class MainActivity : BaseActivity(), MusicPlayerListener {

    companion object {
        val USER_INTENT = MainActivity::class.java.name + ".INTENT.USER"
    }

    private val items = ArrayList<FragmentItem>()

    private var viewPager: ViewPager? = null
    var bottomNavigationView: BottomNavigationView? = null
        private set
    private var slidingUpPanelLayout: SlidingUpPanelLayout? = null
    private var currentPage: Int = 0

    private var musicPlayerView: MusicPlayerParentView? = null
    var musicManager: MusicManager? = null
        private set

    var availablePlaylists = ArrayList<String>()
        get() = ArrayList(field)
        set(value) {
            field.clear()
            field.addAll(value)
        }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        onPageChanged(it.itemId)
        true
    }

    private val simpleOnPageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            onPageChanged(position)
        }
    }

    private val foregroundFragment: Fragment?
        get() = supportFragmentManager.findFragmentByTag("foreground_fragment")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = intent.getSerializableExtra(USER_INTENT) as User

        viewPager = findViewById(R.id.viewpager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        slidingUpPanelLayout = findViewById(R.id.sliding_up_view)
        musicPlayerView = findViewById(R.id.musicplayerparent_view)

        items.add(FragmentItem(HomeFragment::class.java, R.drawable.ic_home, R.string.home))
        items.add(FragmentItem(PlaylistsFragment::class.java, R.drawable.ic_list, R.string.playlists))
        items.add(FragmentItem(SearchFragment::class.java, R.drawable.ic_search, R.string.search))
        items.add(FragmentItem(UsersFragment::class.java, R.drawable.ic_user, R.string.users))
        items.add(FragmentItem(SettingsFragment::class.java, R.drawable.ic_settings, R.string.settings))

        val adapter = ViewPagerAdapter(this, items, user)
        viewPager!!.adapter = adapter
        viewPager!!.offscreenPageLimit = items.size
        currentPage = Settings.getPage(this)
        viewPager!!.currentItem = currentPage

        val menu = bottomNavigationView!!.menu
        for (i in items.indices) {
            val item = items[i]
            menu.add(0, i, 0, item.title).setIcon(item.icon)
        }

        musicManager = MusicManager(this, user, this)
        musicPlayerView!!.setMusicManager(musicManager!!)

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("panel_visible")) {
                slidingUpPanelLayout!!.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
                musicPlayerView!!.setCollapsed(false)
            }
            availablePlaylists.addAll(savedInstanceState
                    .getStringArrayList("availablePlaylists"))
        }

        slidingUpPanelLayout!!.addPanelSlideListener(object : SlidingUpPanelLayout.SimplePanelSlideListener() {
            override fun onPanelStateChanged(panel: View?,
                                             previousState: SlidingUpPanelLayout.PanelState?,
                                             newState: SlidingUpPanelLayout.PanelState?) {
                musicPlayerView!!.setCollapsed(newState == SlidingUpPanelLayout.PanelState.COLLAPSED)
            }
        })

        slidingUpPanelLayout!!.post { slidingUpPanelLayout!!.getChildAt(1).setOnClickListener(null) }

        val foregroundFragment = foregroundFragment
        if (foregroundFragment != null) {
            showForegroundFragment(foregroundFragment)
        }

        viewPager!!.post { onPageChanged(currentPage) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }
    }

    private fun onPageChanged(position: Int) {
        val previousFragment = getViewPagerFragment(items[currentPage])
        val fragment = getViewPagerFragment(items[position])

        viewPager!!.removeOnPageChangeListener(simpleOnPageChangeListener)
        bottomNavigationView!!.setOnNavigationItemSelectedListener(null)

        viewPager!!.currentItem = position
        bottomNavigationView!!.selectedItemId = position

        viewPager!!.addOnPageChangeListener(simpleOnPageChangeListener)
        bottomNavigationView!!.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        if (previousFragment is BaseFragment) {
            previousFragment.onViewPagerPause()
        }
        if (fragment is BaseFragment) {
            fragment.onViewPagerResume()
        }

        Settings.setPage(this, position)
        currentPage = position
    }

    private fun getViewPagerFragment(item: FragmentItem): Fragment? {
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment.javaClass == item.fragmentClass) {
                return fragment
            }
        }
        return null
    }

    fun showForegroundFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.right_in, R.anim.right_out)
                .replace(R.id.foreground_content, fragment, "foreground_fragment")
                .commit()
    }

    fun removeForegroundFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.right_in, R.anim.right_out)
                .remove(fragment)
                .commit()

        for (f in supportFragmentManager.fragments) {
            if (f is BaseFragment) {
                f.onRemoveForeground()
            }
        }
    }

    override fun onBackPressed() {
        if (slidingUpPanelLayout!!.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingUpPanelLayout!!.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            return
        }

        val foregroundFragment = foregroundFragment
        if (foregroundFragment != null) {
            removeForegroundFragment(foregroundFragment)
            return
        }

        val fragment = getViewPagerFragment(items[currentPage])
        if (fragment is BaseFragment && fragment.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("panel_visible",
                slidingUpPanelLayout!!.panelState == SlidingUpPanelLayout.PanelState.EXPANDED)
        synchronized(availablePlaylists) {
            outState.putStringArrayList("availablePlaylists", availablePlaylists)
        }
    }

    override fun onResume() {
        super.onResume()
        musicManager!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        musicManager!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        musicPlayerView!!.onNoMusic()
    }

    override fun onConnect() {
        slidingUpPanelLayout!!.isTouchEnabled = true
        when {
            musicManager!!.isPlaying -> {
                musicPlayerView!!.onPlay(
                        musicManager!!.tracks, musicManager!!.trackPosition)
                musicPlayerView!!.onAudioSessionIdChanged(musicManager!!.audioSessionId)
            }

            musicManager!!.isPreparing -> musicPlayerView!!.onFetch(
                    musicManager!!.tracks, musicManager!!.trackPosition)

            musicManager!!.trackPosition >= 0 -> musicPlayerView!!.onPause(
                    musicManager!!.tracks, musicManager!!.trackPosition)

            else -> {
                musicPlayerView!!.onNoMusic()
                slidingUpPanelLayout!!.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                slidingUpPanelLayout!!.isTouchEnabled = false
            }
        }
    }

    override fun onPreparing(results: List<YoutubeSearchResult>, position: Int) {
        musicPlayerView!!.onFetch(results, position)
        slidingUpPanelLayout!!.isTouchEnabled = true
    }

    override fun onFailure(code: Int, results: List<YoutubeSearchResult>, position: Int) {
        if (code == Status.YoutubeFetchFailure) {
            Utils.toast(R.string.region_lock, this)
        } else {
            Utils.toast(R.string.server_offline, this)
        }

        musicPlayerView!!.onFailure(code, results, position)
        slidingUpPanelLayout!!.isTouchEnabled = true
    }

    override fun onPlay(results: List<YoutubeSearchResult>, position: Int) {
        musicPlayerView!!.onPlay(results, position)
        slidingUpPanelLayout!!.isTouchEnabled = true
    }

    override fun onPause(results: List<YoutubeSearchResult>, position: Int) {
        musicPlayerView!!.onPause(results, position)
        slidingUpPanelLayout!!.isTouchEnabled = true
    }

    override fun onAudioSessionIdChanged(id: Int) {
        musicPlayerView!!.onAudioSessionIdChanged(id)
    }

    override fun onDisconnect() {
        musicPlayerView!!.onNoMusic()
        slidingUpPanelLayout!!.isTouchEnabled = false
        slidingUpPanelLayout!!.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED

        musicManager!!.restart()
    }

    class ViewPagerAdapter constructor(activity: AppCompatActivity,
                                       private val fragmentItems: List<FragmentItem>,
                                       private val user: User) : FragmentPagerAdapter(activity.supportFragmentManager) {

        private val activity: Activity

        init {
            this.activity = activity
        }

        override fun getItem(position: Int): Fragment {
            val bundle = Bundle()
            bundle.putSerializable(USER_INTENT, user)
            return Fragment.instantiate(activity,
                    fragmentItems[position].fragmentClass.name,
                    bundle)
        }

        override fun getCount(): Int {
            return fragmentItems.size
        }
    }

    class FragmentItem constructor(val fragmentClass: Class<out Fragment>,
                                   @param:DrawableRes @field:DrawableRes
                                   val icon: Int, @param:StringRes @field:StringRes
                                   val title: Int)
}
