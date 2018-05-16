package com.grarak.ytfetcher.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewTreeObserver;

import com.grarak.ytfetcher.MainActivity;
import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.service.DownloadService;
import com.grarak.ytfetcher.utils.MusicManager;
import com.grarak.ytfetcher.utils.Utils;
import com.grarak.ytfetcher.utils.server.GenericCallback;
import com.grarak.ytfetcher.utils.server.Status;
import com.grarak.ytfetcher.utils.server.playlist.PlaylistId;
import com.grarak.ytfetcher.utils.server.playlist.PlaylistServer;
import com.grarak.ytfetcher.utils.server.user.User;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class BaseFragment extends Fragment {

    private User user;

    private PlaylistServer playlistServer;
    private YoutubeSearchResult resultToAddPlaylist;
    private final Queue<YoutubeSearchResult> resultsToQueue = new LinkedBlockingQueue<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playlistServer = new PlaylistServer(getActivity());
        if (savedInstanceState != null) {
            resultToAddPlaylist = (YoutubeSearchResult) savedInstanceState
                    .getSerializable("resultToAddPlaylist");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (resultToAddPlaylist != null) {
            showPlaylistDialog(resultToAddPlaylist);
        }

        getActivity().registerReceiver(downloadedReceiver,
                new IntentFilter(DownloadService.ACTION_DOWNLOADED));
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(downloadedReceiver);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver
                .OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                if (getActivity() == null) return;
                onViewFinished();
            }
        });
    }

    public void onViewFinished() {
    }

    protected User getUser() {
        if (user == null) {
            user = (User) getArguments().getSerializable(MainActivity.USER_INTENT);
        }
        return user;
    }

    public BottomNavigationView getBottomNavigationView() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getBottomNavigationView();
        }
        return null;
    }

    public void onViewPagerResume() {
    }

    public void onViewPagerPause() {
    }

    public boolean onBackPressed() {
        return false;
    }

    public MusicManager getMusicManager() {
        return ((MainActivity) getActivity()).getMusicManager();
    }

    public void showForegroundFragment(Fragment fragment) {
        ((MainActivity) getActivity()).showForegroundFragment(fragment);
    }

    public void removeForegroundFragment(Fragment fragment) {
        ((MainActivity) getActivity()).removeForegroundFragment(fragment);
    }

    public void setAvailablePlaylists(List<String> playlists) {
        ((MainActivity) getActivity()).setAvailablePlaylists(playlists);
    }

    public List<String> getAvailablePlaylists() {
        return ((MainActivity) getActivity()).getAvailablePlaylists();
    }

    public void showPlaylistDialog(YoutubeSearchResult result) {
        if (getAvailablePlaylists().size() == 0) {
            Utils.toast(R.string.no_playlists, getActivity());
            return;
        }

        resultToAddPlaylist = result;
        String[] playlists = getAvailablePlaylists().toArray(new String[getAvailablePlaylists().size()]);
        new AlertDialog.Builder(getActivity()).setItems(playlists, (dialog, which) -> {
            PlaylistId playlistId = new PlaylistId();
            playlistId.apikey = getUser().apikey;
            playlistId.name = playlists[which];
            playlistId.id = result.id;
            playlistServer.addToPlaylist(playlistId, new GenericCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int code) {
                    if (code == Status.PlaylistIdAlreadyExists) {
                        Utils.toast(R.string.already_in_playlist, getActivity());
                    } else {
                        Utils.toast(R.string.failed_add_playlist, getActivity());
                    }
                }
            });
        }).setOnDismissListener(dialog -> resultToAddPlaylist = null).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("resultToAddPlaylist", resultToAddPlaylist);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted(requestCode);
        } else {
            onPermissionDenied(requestCode);
        }
    }

    public void requestPermissions(int request, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> needrequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(getActivity(), permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    needrequest.add(permission);
                }
            }
            if (needrequest.size() > 0) {
                requestPermissions(needrequest.toArray(new String[needrequest.size()]), request);
                return;
            }
        }
        onPermissionGranted(request);
    }

    public void onPermissionGranted(int request) {
        if (request == 0) {
            while (resultsToQueue.size() != 0) {
                DownloadService.queueDownload(getActivity(), getUser(), resultsToQueue.poll());
            }
        }
    }

    public void onPermissionDenied(int request) {
        Utils.toast(R.string.no_permissions, getActivity());
    }

    public void queueDownload(YoutubeSearchResult result) {
        if (Integer.parseInt(result.duration.substring(0, result.duration.indexOf(':'))) > 20) {
            Utils.toast(getString(R.string.too_long, result.title), getActivity());
            return;
        }
        if (result.getDownloadPath(getActivity()).exists()) {
            return;
        }
        resultsToQueue.offer(result);
        requestPermissions(0, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private BroadcastReceiver downloadedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            YoutubeSearchResult result = (YoutubeSearchResult)
                    intent.getSerializableExtra(DownloadService.INTENT_DOWNLOAD);
            onDownloaded(result);
        }
    };

    protected void onDownloaded(YoutubeSearchResult result) {
    }

    public boolean deleteResult(YoutubeSearchResult result) {
        int currentTrack = getMusicManager().getTrackPosition();
        if (currentTrack >= 0) {
            if (getMusicManager().getTracks().get(currentTrack).equals(result)) {
                Utils.toast(R.string.delete_not_possible, getActivity());
                return false;
            }
        }
        return result.delete(getActivity());
    }

    public void onRemoveForeground() {
    }
}
