package com.simpleware.jonathan.musicloaderexample;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by JDavis on 7/26/2016.
 */
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    Context mContext;
    List<Music> mMusic;
    BitmapDrawable mPlaceholder;
    LruCache<Long, Bitmap> mBitmapCache;

    public MusicAdapter(Context context, List<Music> music) {
        mMusic = new ArrayList<>();
        if(music != null) {
            mMusic.addAll(music);
        }
        mContext = context;
        mPlaceholder = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.ic_music_note_black_48dp);
        // Get the maximum size of byte we are allowed to allocate on the VM head and convert it to bytes.
        int maxSize = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Divide the maximum size by eight to get a adequate size the LRU cache should reach before it starts to evict bitmaps.
        int cacheSize = maxSize / 8;
        mBitmapCache = new LruCache<Long, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(Long key, Bitmap value) {
                // returns the size of bitmaps in kilobytes.
                return value.getByteCount() / 1024;
            }
        };
    }

    /**
     * Adds a {@link Music} item to the Adapter.
     * @param item
     */
    public void addItem(Music item) {
        // Append the music item to the end of the list.
        mMusic.add(item);
        // Notify the Recyclerview that an item has been inserted at our tail.
        notifyItemInserted(mMusic.size() - 1);
    }

    /**
     * Adds a {@link List} of {@link Music} to the adapters.
     * This method replaces the current music items inside of the adapter with the specified music items.
     * @param music
     */
    public void addItems(List<Music> music) {
        // Clear the old items. I only do this so that I don't have to do duplicating checks on the music items.
        mMusic.clear();
        // Add the new music list.
        mMusic.addAll(music);
        notifyItemRangeInserted(0, music.size());
    }

    /**
     * Clears the {@link Music} items inside of this adapter.
     */
    public void clearItem() {
        mMusic.clear();
    }

    @Override
    public MusicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.list_item, parent, false);
        MusicViewHolder musicViewHolder = new MusicViewHolder(v);
        return musicViewHolder;
    }

    @Override
    public void onBindViewHolder(MusicViewHolder holder, int position) {
        Music music = mMusic.get(position);
        // Check the Bitmap cache for the album art first..
        final Bitmap bitmap = mBitmapCache.get(music.getAlbumId());
        // If the bitmap is not null, then use the cached images.
        if(bitmap != null){
            holder.icon.setImageBitmap(bitmap);
        }
        else {
            // No album art could be found in the cache try reloading it.
            // In a real work example you should check that this value is not some junk value indicating that their is no album artwork.
            loadAlbumArt(holder.icon, music.getAlbumId());
        }
        holder.artist.setText(music.getArtist());
        holder.title.setText(music.getTitle());
    }

    /**
     * Helper method for asynchronously loading album art.
     * @param icon
     * @param albumId
     */
    public void loadAlbumArt(ImageView icon, long albumId) {
        // Check the current album art task if any and cancel it, if it is loading album art that doesn't match the specified album id.
        if(cancelLoadTask(icon, albumId)) {
             // There was either no task running or it was loading a different image so create a new one to load the proper image.
            LoadAlbumArt loadAlbumArt = new LoadAlbumArt(icon, mContext);
            // Store the task inside of the async drawable.
            AsyncDrawable drawable = new AsyncDrawable(mContext.getResources(), mPlaceholder.getBitmap(),loadAlbumArt);
            icon.setImageDrawable(drawable);
            loadAlbumArt.execute(albumId);
        }
    }

    /**
     * Helper method cancelling {@link LoadAlbumArt}.
     *
     * @param icon
     * @param albumId
     * @return
     */
    public boolean cancelLoadTask(ImageView icon, long albumId) {
        LoadAlbumArt loadAlbumArt = (LoadAlbumArt) getLoadTask(icon);
        // If the task is null return true because we want to try and load the album art.
        if(loadAlbumArt == null) {
            return true;
        }
        if(loadAlbumArt != null) {
            // If the album id differs cancel this task because it cannot be recycled for this imageview.
            if(loadAlbumArt.albumId != albumId) {
                loadAlbumArt.cancel(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method for extracting an {@link LoadAlbumArt}.
     * @param icon
     * @return
     */
    public AsyncTask getLoadTask(ImageView icon) {
        LoadAlbumArt task = null;
        Drawable drawable = icon.getDrawable();
        if(drawable instanceof AsyncDrawable) {
            task = ((AsyncDrawable) drawable).getLoadArtworkTask();
        }
        return task;
    }

    private class LoadAlbumArt extends AsyncTask<Long, Void, Bitmap> {

        // URI that points to the AlbumArt database.
        private final Uri albumArtURI = Uri.parse("content://media/external/audio/albumart");
        public WeakReference<ImageView> mIcon;
        // Holds a publicly accessible albumId to be checked against.
        public long albumId;
        private Context mContext;
        int width, height;

        public LoadAlbumArt(ImageView icon, Context context) {
            // Store a weak reference to the imageView.
            mIcon = new WeakReference<ImageView>(icon);
            // Store the width and height of the imageview.
            // This is necessary for properly scalling the bitmap.
            width = icon.getWidth();
            height = icon.getHeight();
            mContext = context;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(isCancelled() || bitmap == null){
                return;
            }
            // Check to make sure that the imageview has not been garbage collected as well as the
            // LoadArtworkTask is the same as this one.
            if(mIcon != null && mIcon.get() != null) {
                ImageView icon = mIcon.get();
                Drawable drawable = icon.getDrawable();
                if(drawable instanceof AsyncDrawable) {
                    LoadAlbumArt task = ((AsyncDrawable) drawable).getLoadArtworkTask();
                    // Make sure that this is the same task as the one current stored inside of the ImageView's drawable.
                    if(task != null && task == this) {
                        icon.setImageBitmap(bitmap);
                    }
                }
            }
            mBitmapCache.put(albumId, bitmap);
            super.onPostExecute(bitmap);
        }

        @Override
        protected Bitmap doInBackground(Long... params) {
            // AsyncTask are not guaranteed to start immediately and could be cancelled somewhere in between calling doInBackground.
            if(isCancelled()){
                return null;
            }
            albumId = params[0];
            // Append the albumId to the end of the albumArtURI to create a new Uri that should point directly to the album art if it exist.
            Uri albumArt = ContentUris.withAppendedId(albumArtURI, albumId);
            Bitmap bmp = null;
            try {
                // Decode the bitmap.
                bmp = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), albumArt);
                // Create a scalled down version of the bitmap to be more memory efficient.
                 // THe smaller the bitmap the more items we can store inside of the LRU cache.
                bmp = Bitmap.createScaledBitmap(bmp, width, height, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bmp;
        }
    }
    /**
     * Custom drawable that holds a LoadArtworkTask
     */
    private static class AsyncDrawable extends BitmapDrawable {
        WeakReference<LoadAlbumArt> loadArtworkTaskWeakReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, LoadAlbumArt task) {
            super(resources, bitmap);
            // Store the LoadArtwork task inside of a weak reference so it can still be garbage collected.
            loadArtworkTaskWeakReference = new WeakReference<LoadAlbumArt>(task);
        }

        public LoadAlbumArt getLoadArtworkTask() {
            return loadArtworkTaskWeakReference.get();
        }
    }

    @Override
    public int getItemCount() {
        return mMusic.size();
    }

    /**
     * Custom ViewHolder that represents the List Item.
     */
    public static class MusicViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;
        TextView artist;

        public MusicViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.title);
            artist = (TextView)itemView.findViewById(R.id.subtitle);
        }
    }
}
