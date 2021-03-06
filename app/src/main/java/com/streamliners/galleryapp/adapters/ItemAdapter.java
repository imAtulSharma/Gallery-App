package com.streamliners.galleryapp.adapters;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.streamliners.galleryapp.GalleryActivity;
import com.streamliners.galleryapp.R;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.models.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents adapter for items data list
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    /**
     * List of the items
     */
    private final List<Item> mItemList;

    /**
     * List of the visible items
     */
    public final List<Item> visibleItemsList;
    /**
     * Context of the activity for inflating purpose
     */
    private final Context mContext;

    /**
     * Listener for the call backs
     */
    private final OnListSizeChangeListener mListener;

    /**
     * For the index of the item selected in the list
     */
    public int index = -1;

    /**
     * For the binding of the item selected from the list
     */
    public ItemCardBinding itemBinding;

    /**
     * To check whether the Drag and Drop enabled or not
     */
    public boolean isDragAndDropEnabled;

    /**
     * To initialize the object with...
     * @param context context of the activity
     * @param items list of the items
     * @param listener listener for the callbacks
     */
    public ItemAdapter(Context context, List<Item> items, OnListSizeChangeListener listener) {
        this.mContext = context;
        this.mItemList = items;
        this.mListener = listener;
        this.visibleItemsList = new ArrayList<>(items);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Making the binding for the item
        ItemCardBinding binding = ItemCardBinding.inflate(LayoutInflater.from(mContext), parent, false);

        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        // Item for the specific position
        Item item = visibleItemsList.get(position);

        // Binding the data to the views
        Glide.with(mContext).asBitmap().load(item.url).into(holder.cardBinding.imageView);
        holder.cardBinding.labelView.setText(item.label);
        holder.cardBinding.labelView.setBackgroundColor(item.color);
    }

    @Override
    public int getItemCount() {
        mListener.onListSizeChanges(visibleItemsList.size());
        return visibleItemsList.size();
    }

    /**
     * To filter the visible list
     * @param query query for the search
     */
    public void filter(String query) {
        // Clear the list
        visibleItemsList.clear();

        // Check for query given
        if (query.trim().isEmpty()) {
            // Add all the items of the main list into visible list
            visibleItemsList.addAll(mItemList);
        } else {
            // Filter according to the query
            for (Item item :
                    mItemList) {
                if (item.label.toLowerCase().contains(query.toLowerCase())) {
                    visibleItemsList.add(item);
                }
            }
        }

        // Refreshing the list
        notifyDataSetChanged();
    }

    /**
     * To sort alphabetically the visible list
     */
    public void sortAlphabetically() {
        // Sort the list
        Collections.sort(visibleItemsList, (o1, o2) -> o1.label.compareTo(o2.label));

        notifyDataSetChanged();
    }

    // Notify methods

    /**
     * To notify the adapter for the item added
     * @param item item to be added
     */
    public void add(Item item){
        mItemList.add(item);
        visibleItemsList.add(item);
        notifyItemInserted(visibleItemsList.size()-1);

        // Showing the toast
        Toast.makeText(mContext, "Item Added!", Toast.LENGTH_SHORT).show();
    }

    /**
     * To notify the adapter for the item deletion
     * @param position index of the item to be deleted
     */
    public void delete(int position){
        mItemList.remove(visibleItemsList.get(position));
        visibleItemsList.remove(position);
        notifyItemRemoved(position);

        // Showing the toast
        Toast.makeText(mContext, "Item Deleted!", Toast.LENGTH_SHORT).show();
    }

    /**
     * To notify the adapter for the item edition
     * @param position index of the item to be edit
     * @param item edited(changed) item
     */
    public void edit(int position, Item item){
        mItemList.set(mItemList.indexOf(visibleItemsList.get(position)), item);
        visibleItemsList.set(position, item);
        notifyItemChanged(position);

        // Showing the toast
        Toast.makeText(mContext, "Item Edited!", Toast.LENGTH_SHORT).show();
    }

    /**
     * To notify the adapter that the item moved
     * @param from initial index
     * @param to index where the item to be moved
     */
    public void move(int from, int to){
        Collections.swap(mItemList, mItemList.indexOf(visibleItemsList.get(from)), mItemList.indexOf(visibleItemsList.get(to)));
        Collections.swap(visibleItemsList, from, to);
        notifyItemMoved(from, to);
    }

    /**
     * Represents view holder for the recycler view
     */
    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener{
        /**
         * Binding of the item selected
         */
        private final ItemCardBinding cardBinding;

        /**
         * To give binding to the holder
         * @param itemCardBinding binding of the view
         */
        public ItemViewHolder(ItemCardBinding itemCardBinding) {
            super(itemCardBinding.getRoot());
            cardBinding = itemCardBinding;
            cardBinding.imageView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            // Checking for the Drag and Drop
            // if not valid then no context menu is showed
            // Otherwise will inflate the menu and show it
            if (isDragAndDropEnabled) {
                return;
            }

            // Setting the index and binding of the item in the list
            index = this.getAbsoluteAdapterPosition();
            itemBinding = cardBinding;

            // Inflate the menu
            MenuInflater inflater = ((GalleryActivity) mContext).getMenuInflater();
            inflater.inflate(R.menu.contextual_menu, menu);
        }
    }

    /**
     * List size changing callbacks
     */
    public interface OnListSizeChangeListener {
        /**
         * When list has no objects
         */
        void onListSizeChanges(int size);
    }
}
