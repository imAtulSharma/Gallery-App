package com.streamliners.galleryapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.models.Item;

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
     * Context of the activity for inflating purpose
     */
    private final Context mContext;

    /**
     * To initialize the object with...
     * @param context context of the activity
     * @param items list of the items
     */
    public ItemAdapter(Context context, List<Item> items) {
        this.mContext = context;
        this.mItemList = items;
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
        Item item = mItemList.get(position);

        // Binding the data to the views
        Glide.with(mContext).asBitmap().load(item.url).into(holder.cardBinding.imageView);
        holder.cardBinding.labelView.setText(item.label);
        holder.cardBinding.labelView.setBackgroundColor(item.color);
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    /**
     * Represents view holder for the recycler view
     */
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ItemCardBinding cardBinding;

        /**
         * To give binding to the holder
         * @param itemCardBinding binding of the view
         */
        public ItemViewHolder(ItemCardBinding itemCardBinding) {
            super(itemCardBinding.getRoot());

            cardBinding = itemCardBinding;
        }
    }
}
