package com.example.space;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TripImageAdapter extends RecyclerView.Adapter<TripImageAdapter.ImageViewHolder> {

    private List<Bitmap> images;
    private Context context;

    public TripImageAdapter(Context context) {
        this.context = context;
        this.images = new ArrayList<>();
    }

    public void addImage(Bitmap bitmap) {
        images.add(bitmap);
        notifyItemInserted(images.size() - 1);
    }

    public void clearImages() {
        int size = images.size();
        images.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.imageView.setImageBitmap(images.get(position));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_thumbnail);
        }
    }
}