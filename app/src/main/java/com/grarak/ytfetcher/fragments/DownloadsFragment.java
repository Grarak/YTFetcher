package com.grarak.ytfetcher.fragments;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.grarak.ytfetcher.MainActivity;
import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.utils.Utils;
import com.grarak.ytfetcher.utils.server.user.User;
import com.grarak.ytfetcher.utils.server.youtube.Youtube;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeServer;
import com.grarak.ytfetcher.views.recyclerview.DownloadItem;
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadsFragment extends RecyclerViewFragment<TitleFragment> {

    public static DownloadsFragment newInstance(User user) {
        Bundle args = new Bundle();
        args.putSerializable(MainActivity.USER_INTENT, user);
        DownloadsFragment fragment = new DownloadsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private YoutubeServer server;
    private List<String> ids = new ArrayList<>();

    private View deleteView;
    private boolean selectionMode;
    private Set<YoutubeSearchResult> selected = new HashSet<>();
    private boolean deleteDialog;

    @Override
    protected int getLayoutXml() {
        return R.layout.fragment_downloads;
    }

    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        deleteView = getRootView().findViewById(R.id.delete_btn);
        deleteView.setOnClickListener(v -> deleteDialog());

        File[] files = Utils.getDownloadFolder(getActivity()).listFiles();
        if (files == null || files.length == 0) {
            removeForegroundFragment(this);
            Utils.toast(R.string.no_downloads, getActivity());
            return;
        }

        if (server != null) return;

        showProgress();
        server = new YoutubeServer(getActivity());

        ids.clear();
        for (File file : files) {
            if (file.getName().endsWith(".downloading")) {
                continue;
            }

            int index = file.getName().indexOf('.');
            if (index < 0) {
                continue;
            }

            ids.add(file.getName().substring(0, index));
        }

        if (ids.size() == 0) {
            removeForegroundFragment(this);
            Utils.toast(R.string.no_downloads, getActivity());
            return;
        }

        AtomicInteger fetchedCount = new AtomicInteger();
        for (String id : ids) {
            Youtube youtube = new Youtube();
            youtube.apikey = getUser().apikey;
            youtube.id = id;

            server.getInfo(youtube, new YoutubeServer.YoutubeResultCallback() {
                @Override
                public void onSuccess(YoutubeSearchResult result) {
                    result.save(getActivity());
                    if (fetchedCount.incrementAndGet() == ids.size()) {
                        addItems();
                        dismissProgress();
                    }
                }

                @Override
                public void onFailure(int code) {
                    if (fetchedCount.incrementAndGet() == ids.size()) {
                        addItems();
                        dismissProgress();
                    }
                }
            });
        }
    }

    @Override
    protected void initItems(List<RecyclerViewItem> recyclerViewItems) {
    }

    @Override
    public void onResume() {
        super.onResume();

        if (deleteDialog) {
            deleteDialog();
        }
    }

    private void enableSelectionMode() {
        selectionMode = true;
        deleteView.setVisibility(View.VISIBLE);
    }

    private void disableSelectionMode() {
        selectionMode = false;
        deleteView.setVisibility(View.INVISIBLE);
    }

    private void deleteDialog() {
        deleteDialog = true;

        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.sure_question)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    for (YoutubeSearchResult result : selected) {
                        deleteResult(result);
                    }

                    server = null;
                    selected.clear();
                    disableSelectionMode();
                    clearItems();
                    init(null);
                })
                .setNegativeButton(R.string.no, null)
                .setOnDismissListener(dialog -> deleteDialog = false).show();
    }

    private void addItems() {
        for (String id : ids) {
            YoutubeSearchResult result = YoutubeSearchResult.restore(id, getActivity());
            if (result == null) {
                continue;
            }

            addItem(new DownloadItem(result, new DownloadItem.DownloadListener() {
                @Override
                public void onClick(DownloadItem item) {
                    if (selectionMode) {
                        item.toogleSelection();
                        if (selected.contains(result)) {
                            selected.remove(result);
                        } else {
                            selected.add(result);
                        }
                        if (selected.size() == 0) {
                            disableSelectionMode();
                        }
                    } else {
                        getMusicManager().play(result);
                    }
                }

                @Override
                public void onLongClick(DownloadItem item) {
                    if (!selectionMode) {
                        enableSelectionMode();
                        onClick(item);
                    }
                }

                @Override
                public void onDelete(DownloadItem item) {
                    if (deleteResult(result)) {
                        removeItem(item);
                    }
                }
            }));
        }
    }
}
