package com.grarak.ytfetcher.views.recyclerview;

import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.widget.TextView;

import com.grarak.ytfetcher.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectorItem extends RecyclerViewItem {

    public interface SelectorListener {
        void onItemSelected(SelectorItem item, List<String> items, int position);
    }

    private RecyclerView.ViewHolder viewHolder;

    private final SelectorListener listener;
    private CharSequence title;
    private List<String> items = new ArrayList<>();
    private int position;

    public SelectorItem(SelectorListener listener) {
        this.listener = listener;
    }

    @Override
    protected int getLayoutXml() {
        return R.layout.item_selector;
    }

    @Override
    protected void bindViewHolder(RecyclerView.ViewHolder viewHolder) {
        this.viewHolder = viewHolder;

        TextView itemView = viewHolder.itemView.findViewById(R.id.selected_item);
        viewHolder.itemView.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), itemView);
            Menu menu = popupMenu.getMenu();
            for (int i = 0; i < items.size(); i++) {
                menu.add(0, i, 0, items.get(i));
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                position = item.getItemId();
                itemView.setText(items.get(position));
                listener.onItemSelected(this, Collections.unmodifiableList(items), position);

                return true;
            });
            popupMenu.show();
        });

        setup();
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        setup();
    }

    public void setItems(List<String> items) {
        this.items.clear();
        this.items.addAll(items);
        setup();
    }

    public void setPosition(int position) {
        this.position = position;
        setup();
        listener.onItemSelected(this, Collections.unmodifiableList(items), position);
    }

    private void setup() {
        if (viewHolder != null) {
            TextView title = viewHolder.itemView.findViewById(R.id.title);
            TextView item = viewHolder.itemView.findViewById(R.id.selected_item);

            title.setText(this.title);
            item.setText(items.get(position));
        }
    }
}
