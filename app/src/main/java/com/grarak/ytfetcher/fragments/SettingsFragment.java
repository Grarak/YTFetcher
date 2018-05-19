package com.grarak.ytfetcher.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;

import com.grarak.ytfetcher.LoginActivity;
import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.utils.Prefs;
import com.grarak.ytfetcher.utils.Settings;
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem;
import com.grarak.ytfetcher.views.recyclerview.settings.ButtonItem;
import com.grarak.ytfetcher.views.recyclerview.settings.SwitchItem;

import java.util.List;

public class SettingsFragment extends RecyclerViewFragment<TitleFragment> {

    private boolean signoutDialog;

    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (signoutDialog) {
            signoutDialog();
        }
    }

    @Override
    protected void init(Bundle savedInstanceState) {
    }

    @Override
    protected void initItems(List<RecyclerViewItem> recyclerViewItems) {
        ButtonItem downloads = new ButtonItem(
                v -> showForegroundFragment(DownloadsFragment.newInstance(getUser())));
        downloads.setText(getString(R.string.downloads));
        recyclerViewItems.add(downloads);

        ButtonItem history = new ButtonItem(
                v -> showForegroundFragment(HistoryFragment.newInstance(getUser())));
        history.setText(getString(R.string.history));
        recyclerViewItems.add(history);

        ButtonItem equalizer = new ButtonItem(
                v -> showForegroundFragment(new EqualizerFragment()));
        equalizer.setText(getString(R.string.equalizer));
        recyclerViewItems.add(equalizer);

        SwitchItem darkTheme = new SwitchItem(checked -> {
            Settings.setDarkTheme(checked, getActivity());
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
        darkTheme.setText(getString(R.string.dark_theme));
        darkTheme.setChecked(Settings.isDarkTheme(getActivity()));
        recyclerViewItems.add(darkTheme);

        ButtonItem signout = new ButtonItem(v -> signoutDialog());
        signout.setText(getString(R.string.signout));
        signout.setTextColor(Color.WHITE);
        signout.setBackgroundColor(Color.RED);
        recyclerViewItems.add(signout);

        ButtonItem licenses = new ButtonItem(
                v -> showForegroundFragment(new LicenseFragment()));
        licenses.setText(getString(R.string.licenses));
        recyclerViewItems.add(licenses);
    }

    private void signoutDialog() {
        signoutDialog = true;
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.sure_question)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    getMusicManager().destroy();
                    Prefs.clear(getActivity());
                    getActivity().finish();
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                })
                .setNegativeButton(R.string.no, null)
                .setOnDismissListener(dialog -> signoutDialog = false).show();
    }
}
