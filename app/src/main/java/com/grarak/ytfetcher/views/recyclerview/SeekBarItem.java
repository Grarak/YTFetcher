package com.grarak.ytfetcher.views.recyclerview;

import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.RecyclerView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.grarak.ytfetcher.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeekBarItem extends RecyclerViewItem {

    public interface SeekBarListener {
        void onSeekStop(SeekBarItem item, List<String> items, int position);
    }

    private final SeekBarListener listener;

    private RecyclerView.ViewHolder viewHolder;

    private CharSequence title;
    private List<String> items = new ArrayList<>();
    private int current;
    private boolean enabled = true;

    public SeekBarItem(SeekBarListener seekBarListener) {
        listener = seekBarListener;
    }

    @Override
    protected int getLayoutXml() {
        return R.layout.item_seekbar;
    }

    @Override
    protected void bindViewHolder(RecyclerView.ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
        setup();
    }

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            TextView progressText = viewHolder.itemView.findViewById(R.id.progress);
            progressText.setText(items.get(progress));
            if (fromUser) {
                current = progress;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            listener.onSeekStop(SeekBarItem.this,
                    Collections.unmodifiableList(items), current);
        }
    };

    public void setTitle(CharSequence title) {
        this.title = title;
        setup();
    }

    public void setItems(List<String> items) {
        this.items.clear();
        this.items.addAll(items);
        setup();
    }

    public void setCurrent(int current) {
        this.current = current;
        setup();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setup();
    }

    private void setup() {
        if (viewHolder != null) {
            TextView titleText = viewHolder.itemView.findViewById(R.id.title);
            TextView progressText = viewHolder.itemView.findViewById(R.id.progress);
            AppCompatSeekBar seekBar = viewHolder.itemView.findViewById(R.id.seekbar);

            titleText.setText(title);
            progressText.setText(items.get(current));

            seekBar.setOnSeekBarChangeListener(null);
            seekBar.setMax(items.size() - 1);
            seekBar.setProgress(current);
            seekBar.setEnabled(enabled);
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        }
    }
}
