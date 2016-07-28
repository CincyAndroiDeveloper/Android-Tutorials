package com.simpleware.jonathan.musicloaderexample;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JDavis on 7/26/2016.
 */
public class MusicLoader extends AsyncTaskLoader<List<Music>> {

    List<Music> mCache;
    MusicObserver mMusicObserver;

    public MusicLoader(Context context) {
        super(context);
    }

    @Override
    public List<Music> loadInBackground() {
        final ContentResolver contentResolver = getContext().getContentResolver();
        String [] projections = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " =1";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cr = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projections, selection, null, sortOrder);
        List<Music> items = new ArrayList<>();
        if(cr != null && cr.moveToFirst()) {
            // Cache the column indexes so we don't have to look them up for every iteration of the do-while loop.
            int idIndex = cr.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleIndex = cr.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistIndex = cr.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumId =  cr.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            do {
                if(isLoadInBackgroundCanceled()){
                    return items;
                }
                // Music object to hold the music data.
                Music item = new Music();
                // Retrieve the respective music data from the cursor using the column index.
                item.setId(cr.getLong(idIndex));
                item.setTitle(cr.getString(titleIndex));
                item.setArtist(cr.getString(artistIndex));
                item.setAlbumId(cr.getLong(albumId));
                // Once we've loaded the Music object, store it inside of the arraylist.
                items.add(item);
            }
            while(cr.moveToNext());
            cr.close();

        }
        return items;
    }

    @Override
    public void deliverResult(List<Music> data) {
        if(isReset()){

            // CLose cursors or databse handles.
            return;
        }
        // Keep a reference to the loaded music data.
        mCache = data;

        // If we are started pass the loaded music to our super implementation that handles sending it to the registered activity/fragment.
        if(isStarted()){
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStopLoading() {
       cancelLoad();
    }

    @Override
    protected void onStartLoading() {
        if(mCache != null) {
            deliverResult(mCache);
        }

        if(mMusicObserver == null) {
            mMusicObserver = new MusicObserver(this, new Handler());
            getContext().getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mMusicObserver);
        }

        if(takeContentChanged() || mCache == null) {
            forceLoad();
        }
    }

    @Override
    protected void onReset() {
        // Close any cursors, web-sockets or database objects
        if(mMusicObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mMusicObserver);
            mMusicObserver = null;
        }
    }

    /**
     * Simple observer that notifies the loader when it has detected a change.
     */
    private static class MusicObserver extends ContentObserver {

        private android.support.v4.content.Loader mLoader;

        public MusicObserver(android.support.v4.content.Loader loader, Handler handler) {
            super(handler);
            mLoader = loader;
        }

        @Override
        public void onChange(boolean selfChange) {
            // A change has been detectec notify the Loader.
            mLoader.onContentChanged();
        }
    }
}
