package com.grarak.ytfetcher;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;

import com.grarak.ytfetcher.fragments.BaseFragment;
import com.grarak.ytfetcher.fragments.HomeFragment;
import com.grarak.ytfetcher.fragments.PlaylistsFragment;
import com.grarak.ytfetcher.fragments.SearchFragment;
import com.grarak.ytfetcher.fragments.SettingsFragment;
import com.grarak.ytfetcher.fragments.UsersFragment;
import com.grarak.ytfetcher.service.MusicPlayerListener;
import com.grarak.ytfetcher.utils.MusicManager;
import com.grarak.ytfetcher.utils.Settings;
import com.grarak.ytfetcher.utils.Utils;
import com.grarak.ytfetcher.utils.server.Status;
import com.grarak.ytfetcher.utils.server.user.User;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult;
import com.grarak.ytfetcher.views.musicplayer.MusicPlayerParentView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements MusicPlayerListener {

    public static final String USER_INTENT = MainActivity.class.getName() + ".INTENT.USER";

    private final List<FragmentItem> items = new ArrayList<>();

    private ViewPager viewPager;
    private BottomNavigationView bottomNavigationView;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private int currentPage;

    private MusicPlayerParentView musicPlayerView;
    private MusicManager musicManager;

    private final ArrayList<String> availablePlaylists = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        User user = (User) getIntent().getSerializableExtra(USER_INTENT);

        viewPager = findViewById(R.id.viewpager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        slidingUpPanelLayout = findViewById(R.id.sliding_up_view);
        musicPlayerView = findViewById(R.id.musicplayerparent_view);

        items.add(new FragmentItem(HomeFragment.class, R.drawable.ic_home, R.string.home));
        items.add(new FragmentItem(PlaylistsFragment.class, R.drawable.ic_list, R.string.playlists));
        items.add(new FragmentItem(SearchFragment.class, R.drawable.ic_search, R.string.search));
        items.add(new FragmentItem(UsersFragment.class, R.drawable.ic_user, R.string.users));
        items.add(new FragmentItem(SettingsFragment.class, R.drawable.ic_settings, R.string.settings));

        ViewPagerAdapter adapter = new ViewPagerAdapter(this, items, user);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(items.size());
        currentPage = Settings.getPage(this);
        viewPager.setCurrentItem(currentPage);

        Menu menu = bottomNavigationView.getMenu();
        for (int i = 0; i < items.size(); i++) {
            FragmentItem item = items.get(i);
            menu.add(0, i, 0, item.title).setIcon(item.icon);
        }

        musicManager = new MusicManager(this, user, this);
        musicPlayerView.setMusicManager(musicManager);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("panel_visible")) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                musicPlayerView.setCollapsed(false);
            }
            availablePlaylists.addAll(savedInstanceState
                    .getStringArrayList("availablePlaylists"));
        }

        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelStateChanged(View panel,
                                            SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {
                musicPlayerView.setCollapsed(newState == SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        slidingUpPanelLayout.post(() -> slidingUpPanelLayout.getChildAt(1).setOnClickListener(null));

        Fragment foregroundFragment = getSupportFragmentManager()
                .findFragmentByTag("foreground_fragment");
        if (foregroundFragment != null) {
            showForegroundFragment(foregroundFragment);
        }

        viewPager.post(() -> onPageChanged(currentPage));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener =
            item -> {
                onPageChanged(item.getItemId());
                return true;
            };

    private ViewPager.SimpleOnPageChangeListener simpleOnPageChangeListener =
            new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    onPageChanged(position);
                }
            };

    private void onPageChanged(int position) {
        Fragment previousFragment = getViewPagerFragment(items.get(currentPage));
        Fragment fragment = getViewPagerFragment(items.get(position));

        viewPager.removeOnPageChangeListener(simpleOnPageChangeListener);
        bottomNavigationView.setOnNavigationItemSelectedListener(null);

        viewPager.setCurrentItem(position);
        bottomNavigationView.setSelectedItemId(position);

        viewPager.addOnPageChangeListener(simpleOnPageChangeListener);
        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        if (previousFragment instanceof BaseFragment) {
            ((BaseFragment) previousFragment).onViewPagerPause();
        }
        if (fragment instanceof BaseFragment) {
            ((BaseFragment) fragment).onViewPagerResume();
        }

        Settings.setPage(this, position);
        currentPage = position;
    }

    private Fragment getViewPagerFragment(FragmentItem item) {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment.getClass() == item.fragmentClass) {
                return fragment;
            }
        }
        return null;
    }

    public void showForegroundFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.right_in, R.anim.right_out)
                .replace(R.id.foreground_content, fragment, "foreground_fragment")
                .commit();
    }

    public void removeForegroundFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.right_in, R.anim.right_out)
                .remove(fragment)
                .commit();

        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof BaseFragment) {
                ((BaseFragment) f).onRemoveForeground();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (slidingUpPanelLayout.getPanelState() ==
                SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }

        Fragment foregroundFragment = getSupportFragmentManager()
                .findFragmentByTag("foreground_fragment");
        if (foregroundFragment != null) {
            removeForegroundFragment(foregroundFragment);
            return;
        }

        Fragment fragment = getViewPagerFragment(items.get(currentPage));
        if (fragment instanceof BaseFragment && ((BaseFragment) fragment).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("panel_visible",
                slidingUpPanelLayout.getPanelState() ==
                        SlidingUpPanelLayout.PanelState.EXPANDED);
        synchronized (availablePlaylists) {
            outState.putStringArrayList("availablePlaylists", availablePlaylists);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        musicManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        musicManager.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        musicPlayerView.onNoMusic();
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public void setAvailablePlaylists(List<String> playlists) {
        synchronized (availablePlaylists) {
            availablePlaylists.clear();
            availablePlaylists.addAll(playlists);
        }
    }

    public List<String> getAvailablePlaylists() {
        synchronized (availablePlaylists) {
            return new ArrayList<>(availablePlaylists);
        }
    }

    @Override
    public void onConnect() {
        slidingUpPanelLayout.setTouchEnabled(true);
        if (musicManager.isPlaying()) {
            musicPlayerView.onPlay(
                    musicManager.getTracks(), musicManager.getTrackPosition());
            musicPlayerView.onAudioSessionIdChanged(musicManager.getAudioSessionId());
        } else if (musicManager.isPreparing()) {
            musicPlayerView.onFetch(
                    musicManager.getTracks(), musicManager.getTrackPosition());
        } else if (musicManager.getTrackPosition() >= 0) {
            musicPlayerView.onPause(
                    musicManager.getTracks(), musicManager.getTrackPosition());
        } else {
            musicPlayerView.onNoMusic();
            slidingUpPanelLayout.setPanelState(
                    SlidingUpPanelLayout.PanelState.COLLAPSED);
            slidingUpPanelLayout.setTouchEnabled(false);
        }
    }

    @Override
    public void onPreparing(List<YoutubeSearchResult> results, int position) {
        musicPlayerView.onFetch(results, position);
        slidingUpPanelLayout.setTouchEnabled(true);
    }

    @Override
    public void onFailure(int code, List<YoutubeSearchResult> results, int position) {
        if (code == Status.YoutubeFetchFailure) {
            Utils.toast(R.string.region_lock, this);
        } else {
            Utils.toast(R.string.server_offline, this);
        }

        musicPlayerView.onFailure(code, results, position);
        slidingUpPanelLayout.setTouchEnabled(true);
    }

    @Override
    public void onPlay(List<YoutubeSearchResult> results, int position) {
        musicPlayerView.onPlay(results, position);
        slidingUpPanelLayout.setTouchEnabled(true);
    }

    @Override
    public void onPause(List<YoutubeSearchResult> results, int position) {
        musicPlayerView.onPause(results, position);
        slidingUpPanelLayout.setTouchEnabled(true);
    }

    @Override
    public void onAudioSessionIdChanged(int id) {
        musicPlayerView.onAudioSessionIdChanged(id);
    }

    @Override
    public void onDisconnect() {
        musicPlayerView.onNoMusic();
        slidingUpPanelLayout.setTouchEnabled(false);
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        musicManager.restart();
    }

    private static class ViewPagerAdapter extends FragmentStatePagerAdapter {

        private final Activity activity;
        private final List<FragmentItem> fragmentItems;
        private final User user;

        private ViewPagerAdapter(AppCompatActivity activity,
                                 List<FragmentItem> fragmentItems,
                                 User user) {
            super(activity.getSupportFragmentManager());
            this.activity = activity;
            this.fragmentItems = fragmentItems;
            this.user = user;
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(USER_INTENT, user);
            return Fragment.instantiate(activity,
                    fragmentItems.get(position).fragmentClass.getName(),
                    bundle);
        }

        @Override
        public int getCount() {
            return fragmentItems.size();
        }
    }

    private static class FragmentItem {

        private Class<? extends Fragment> fragmentClass;
        @DrawableRes
        private int icon;
        @StringRes
        private int title;

        private FragmentItem(Class<? extends Fragment> fragmentClass,
                             @DrawableRes int icon, @StringRes int title) {
            this.fragmentClass = fragmentClass;
            this.icon = icon;
            this.title = title;
        }
    }
}
