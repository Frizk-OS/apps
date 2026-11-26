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
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to query {@link MediaStore.Audio.Media.EXTERNAL_CONTENT_URI} and return
 * the songs on a user's device.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * Constructor of <code>SongLoader</code>
     * 
     * @param context The {@link Context} to use
     */
    public SongLoader(final Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        try (var cursor = makeSongCursor(getContext())) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Copy the song Id
                    final var id = cursor.getLong(0);

                    // Copy the song name
                    final var songName = cursor.getString(1);

                    // Copy the artist name
                    final var artist = cursor.getString(2);

                    // Copy the album name
                    final var album = cursor.getString(3);

                    // Copy the duration
                    final var duration = cursor.getLong(4);

                    // Convert the duration into seconds
                    final var durationInSecs = (int) duration / 1000;

                    // Create a new song
                    final var song = new Song(id, songName, artist, album, durationInSecs);

                    // Add everything up
                    mSongList.add(song);
                } while (cursor.moveToNext());
            }
        }
        return mSongList;
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     * 
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the song query.
     */
    public static final Cursor makeSongCursor(final Context context) {
        final var mSelection = AudioColumns.IS_MUSIC + "=1"
                + " AND " + AudioColumns.TITLE + " != ''";
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] {
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        AudioColumns.TITLE,
                        /* 2 */
                        AudioColumns.ARTIST,
                        /* 3 */
                        AudioColumns.ALBUM,
                        /* 4 */
                        AudioColumns.DURATION
                }, mSelection, null,
                PreferenceUtils.getInstance(context).getSongSortOrder());
    }
}
