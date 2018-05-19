package com.grarak.ytfetcher.fragments;

import android.animation.ValueAnimator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.utils.Utils;
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewAdapter;
import com.grarak.ytfetcher.views.recyclerview.RecyclerViewItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class RecyclerViewFragment<TF extends BaseFragment>
        extends BaseFragment {

    private List<RecyclerViewItem> items = new ArrayList<>();
    private RecyclerViewAdapter adapter;

    private BottomNavigationView bottomNavigationView;

    private View rootView;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private TextView messageView;
    private View progressView;
    private int firstVisibleItem;

    private int progressCount;

    private TF titleFragment;
    private View titleContent;

    private ItemsLoader<TF> itemsLoader;

    @LayoutRes
    protected int getLayoutXml() {
        return R.layout.fragment_recyclerview;
    }

    protected RecyclerViewAdapter createAdapter() {
        return new RecyclerViewAdapter(items);
    }

    protected abstract LinearLayoutManager createLayoutManager();

    private OnScrollListener onScrollListener = new OnScrollListener();

    private class OnScrollListener extends RecyclerView.OnScrollListener {
        private int scrollDistance;
        private int bottomTranslation;

        private int getScrollDistance() {
            return recyclerView.computeVerticalScrollOffset();
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            scrollDistance = getScrollDistance();
            bottomTranslation += dy;
            setTranslations();

            firstVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
        }

        private void setTranslations() {
            if (titleContent != null) {
                titleContent.setTranslationY(-scrollDistance / 2);
            }

            if (bottomNavigationView == null
                    || recyclerView.getPaddingBottom() == 0) {
                return;
            }
            if (bottomTranslation > bottomNavigationView.getHeight()) {
                bottomTranslation = bottomNavigationView.getHeight();
            } else if (bottomTranslation < 0) {
                bottomTranslation = 0;
            }
            bottomNavigationView.setTranslationY(bottomTranslation);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (bottomNavigationView == null || newState != 0 || bottomTranslation == 0
                    || recyclerView.getPaddingBottom() == 0) {
                return;
            }

            animateBottomNavigation(
                    layoutManager.findLastVisibleItemPosition() == itemsSize() - 1
                            || bottomTranslation < bottomNavigationView.getHeight() * 0.5f);
        }

        private void animateBottomNavigation(boolean show) {
            ValueAnimator animator = ValueAnimator.ofInt(bottomTranslation,
                    show ? 0 : bottomNavigationView.getHeight());
            animator.addUpdateListener(animation -> {
                bottomTranslation = (int) animation.getAnimatedValue();
                if (bottomNavigationView != null) {
                    bottomNavigationView.setTranslationY(bottomTranslation);
                }
            });
            animator.start();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        bottomNavigationView = getBottomNavigationView();

        rootView = inflater.inflate(getLayoutXml(), container, false);

        recyclerView = rootView.findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(layoutManager = createLayoutManager());
        recyclerView.setAdapter(adapter == null ? adapter = createAdapter() : adapter);

        messageView = rootView.findViewById(R.id.message);
        progressView = rootView.findViewById(R.id.progress);
        if (messageView != null) {
            messageView.setText(getEmptyViewsMessage());
        }

        titleContent = rootView.findViewById(R.id.content_title);
        titleFragment = getTitleFragment();
        if (titleFragment == null && getTitleFragmentClass() != null) {
            titleFragment = (TF) Fragment.instantiate(getActivity(), getTitleFragmentClass().getName());
        }
        if (titleFragment != null) {
            setUpTitleFragment(titleFragment);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.content_title, titleFragment, "title_fragment").commit();
        }

        init(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("progress_visible")) {
                showProgress();
                synchronized (this) {
                    progressCount--;
                }
            } else {
                dismissProgress();
            }
            post();
        } else if (itemsLoader == null) {
            showProgress();
            itemsLoader = new ItemsLoader<>(this);
            itemsLoader.execute();
        }
        return rootView;
    }

    private static class ItemsLoader<TF extends BaseFragment> extends AsyncTask<Void, Void, List<RecyclerViewItem>> {

        private WeakReference<RecyclerViewFragment<TF>> fragmentRef;

        private ItemsLoader(RecyclerViewFragment<TF> fragment) {
            fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        protected List<RecyclerViewItem> doInBackground(Void... voids) {
            RecyclerViewFragment<TF> fragment = fragmentRef.get();
            if (fragment != null) {
                List<RecyclerViewItem> items = new ArrayList<>();
                fragment.initItems(items);
                return items;
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecyclerViewItem> recyclerViewItems) {
            super.onPostExecute(recyclerViewItems);

            RecyclerViewFragment<TF> fragment = fragmentRef.get();
            if (fragment != null && recyclerViewItems != null) {
                fragment.items.addAll(recyclerViewItems);
                fragment.adapter.notifyDataSetChanged();
                fragment.post();
                fragment.dismissProgress();
            }
        }
    }

    private void post() {
        if (messageView != null) {
            messageView.setVisibility(
                    progressView.getVisibility() == View.INVISIBLE &&
                            itemsSize() == 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void onViewFinished() {
        int leftPadding = recyclerView.getPaddingLeft();
        int rightPadding = recyclerView.getPaddingRight();
        if (Utils.isLandscape(getActivity())) {
            leftPadding = getResources().getDimensionPixelSize(
                    R.dimen.recyclerview_padding);
            rightPadding = leftPadding;
        }

        int titleHeight = 0;
        if (titleContent != null) {
            titleHeight = titleContent.getHeight();
        }
        recyclerView.setPadding(
                leftPadding,
                recyclerView.getPaddingTop() + titleHeight,
                rightPadding,
                recyclerView.getPaddingBottom()
        );

        recyclerView.scrollToPosition(firstVisibleItem);
        if (!recyclerView.getClipToPadding()) {
            recyclerView.addOnScrollListener(onScrollListener);
        }

        if (titleContent != null) {
            recyclerView.setOnTouchListener((v, event) -> {
                if (progressView == null || progressView.getVisibility() == View.INVISIBLE) {
                    titleContent.dispatchTouchEvent(event);
                }
                return false;
            });
        }
    }

    protected View getRootView() {
        return rootView;
    }

    protected TF getTitleFragment() {
        return (TF) getChildFragmentManager().findFragmentByTag("title_fragment");
    }

    protected Class<TF> getTitleFragmentClass() {
        return null;
    }

    protected void setUpTitleFragment(TF fragment) {
    }

    protected abstract void init(Bundle savedInstanceState);

    protected abstract void initItems(List<RecyclerViewItem> items);

    protected void addItem(RecyclerViewItem item) {
        items.add(item);
        if (adapter != null) {
            adapter.notifyItemInserted(items.size() - 1);
        }
        if (messageView != null) {
            messageView.setVisibility(View.INVISIBLE);
        }
    }

    protected void removeItem(RecyclerViewItem item) {
        removeItem(items.indexOf(item));
    }

    protected void removeItem(int index) {
        if (index < 0) return;
        items.remove(index);
        if (adapter != null) {
            adapter.notifyItemRemoved(index);
        }
        if (messageView != null && itemsSize() == 0) {
            messageView.setVisibility(View.VISIBLE);
        }
    }

    protected void clearItems() {
        items.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (messageView != null) {
            messageView.setVisibility(View.VISIBLE);
        }
    }

    public void showProgress() {
        if (progressView != null) {
            synchronized (this) {
                progressCount++;
                recyclerView.setVisibility(View.INVISIBLE);
                if (messageView != null) {
                    messageView.setVisibility(View.INVISIBLE);
                }
                progressView.setVisibility(View.VISIBLE);
                if (titleContent != null) {
                    titleContent.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    public void dismissProgress() {
        if (progressView != null) {
            synchronized (this) {
                progressCount--;
                if (progressCount <= 0) {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (messageView != null) {
                        messageView.setVisibility(
                                progressView.getVisibility() == View.INVISIBLE &&
                                        itemsSize() == 0 ? View.VISIBLE : View.INVISIBLE);
                    }
                    progressView.setVisibility(View.INVISIBLE);
                    if (titleContent != null) {
                        titleContent.setVisibility(View.VISIBLE);
                    }
                    progressCount = 0;
                }
            }
        }
    }

    protected List<RecyclerViewItem> getItems() {
        return items;
    }

    public int itemsSize() {
        return items.size();
    }

    protected RecyclerView getRecyclerView() {
        return recyclerView;
    }

    protected RecyclerViewAdapter getRecyclerViewAdapter() {
        return adapter;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("progress_visible", progressView != null
                && progressView.getVisibility() == View.VISIBLE);
    }

    @Override
    public void onViewPagerResume() {
        super.onViewPagerResume();

        if (titleFragment != null) {
            titleFragment.onViewPagerResume();
        }
    }

    @Override
    public void onViewPagerPause() {
        super.onViewPagerPause();

        onScrollListener.animateBottomNavigation(true);
        if (titleFragment != null) {
            titleFragment.onViewPagerPause();
        }
    }

    @Override
    public boolean onBackPressed() {
        return titleFragment != null && titleFragment.onBackPressed();
    }

    protected String getEmptyViewsMessage() {
        return null;
    }
}
