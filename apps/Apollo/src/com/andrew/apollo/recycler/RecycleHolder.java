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

package com.andrew.apollo.recycler;

import android.view.View;
import android.widget.AbsListView.RecyclerListener;

import com.andrew.apollo.ui.MusicHolder;

/**
 * A {@link RecyclerListener} for {@link MusicHolder}'s views.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecycleHolder implements RecyclerListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMovedToScrapHeap(final View view) {
        var holder = (MusicHolder) view.getTag();
        if (holder == null) {
            holder = new MusicHolder(view);
            view.setTag(holder);
        }

        // Release mBackground's reference
        final var background = holder.mBackground.get();
        if (background != null) {
            background.setImageDrawable(null);
            background.setImageBitmap(null);
        }

        // Release mImage's reference
        final var image = holder.mImage.get();
        if (image != null) {
            image.setImageDrawable(null);
            image.setImageBitmap(null);
        }

        // Release mLineOne's reference
        final var lineOne = holder.mLineOne.get();
        if (lineOne != null) {
            lineOne.setText(null);
        }

        // Release mLineTwo's reference
        final var lineTwo = holder.mLineTwo.get();
        if (lineTwo != null) {
            lineTwo.setText(null);
        }

        // Release mLineThree's reference
        final var lineThree = holder.mLineThree.get();
        if (lineThree != null) {
            lineThree.setText(null);
        }
    }

}
