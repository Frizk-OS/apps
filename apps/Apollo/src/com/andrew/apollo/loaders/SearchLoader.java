/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SearchLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * The {@link Cursor} used to run the query.
     */
    private Cursor mCursor;

    /**
     * Constructor of <code>SongLoader</code>
     * 
     * @param context The {@link Context} to use
     * @param query The search query
     */
    public SearchLoader(final Context context, final String query) {
        super(context);
        // Create the Cursor
        mCursor = makeSearchCursor(context, query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        try (var cursor = mCursor) {
            mCursor = null;
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    var id = -1L;

                    // Copy the song name
                    final var songName = cursor.getString(cursor
                            .getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));

                    // Check for a song Id
                    if (!TextUtils.isEmpty(songName)) {
                        id = cursor.getLong(cursor
                                .getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    }

                    // Copy the album name
                    final var album = cursor.getString(cursor
                            .getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));

                    // Check for a album Id
                    if (id < 0 && !TextUtils.isEmpty(album)) {
                        id = cursor.getLong(cursor
                                .getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
                    }

                    // Copy the artist name
                    final var artist = cursor.getString(cursor
                            .getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));

                    // Check for a artist Id
                    if (id < 0 && !TextUtils.isEmpty(artist)) {
                        id = cursor.getLong(cursor
                                .getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
                    }

                    // Create a new song
                    final var song = new Song(id, songName, artist, album, -1);

                    // Add everything up
                    mSongList.add(song);
                } while (cursor.moveToNext());
            }
        }
        return mSongList;
    }

    /**
     * * @param context The {@link Context} to use.
     * 
     * @param query The user's query.
     * @return The {@link Cursor} used to perform the search.
     */
    public static final Cursor makeSearchCursor(final Context context, final String query) {
        return context.getContentResolver().query(
                Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(query)),
                new String[] {
                        BaseColumns._ID, MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Media.TITLE, "data1", "data2" //$NON-NLS-2$ 
                }, null, null, null);
    }

}
