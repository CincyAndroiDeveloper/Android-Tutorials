package com.simpleware.jonathan.musicloaderexample;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.CursorAdapter;

import java.util.ArrayList;
import java.util.List;

public class LoaderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Music>> {

    private RecyclerView mRecyclerView;
    private MusicAdapter musicAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerVw);
        // Use a LinearLayoutManager to make the RecyclerView display the Music in a vertically scrolling list.
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicAdapter = new MusicAdapter(this, new ArrayList<Music>());
        mRecyclerView.setAdapter(musicAdapter);

        final LoaderManager supportLoaderManager = getSupportLoaderManager();
        supportLoaderManager.initLoader(1, null, this);
    }

    @Override
    public Loader<List<Music>> onCreateLoader(int id, Bundle args) {
        return new MusicLoader(this);
    }


    @Override
    public void onLoadFinished(Loader<List<Music>> loader, List<Music> data) {
        // Add the newly loaded music to adapter.
        musicAdapter.addItems(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Music>> loader) {
        // Clear the old music because a new list is going to be coming.
        musicAdapter.clearItem();
    }
}
