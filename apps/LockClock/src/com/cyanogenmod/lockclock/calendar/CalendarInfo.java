/*
 * Copyright (C) 2013 The CyanogenMod Project (DvTonder)
 * Portions Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.lockclock.calendar;

import com.cyanogenmod.lockclock.misc.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CalendarInfo {
    private final List<EventInfo> mEventsList;
    private long mFollowingEventStart;

    public CalendarInfo() {
        mEventsList = new ArrayList<>(Constants.MAX_CALENDAR_ITEMS);
        mFollowingEventStart = 0;
    }

    public List<EventInfo> getEvents() {
        return mEventsList;
    }

    public boolean hasEvents() {
        return !mEventsList.isEmpty();
    }

    public void clearEvents() {
        mEventsList.clear();
        mFollowingEventStart = 0;
    }

    public void addEvent(EventInfo event) {
        mEventsList.add(event);
        Collections.sort(mEventsList);
    }

    public void setFollowingEventStart(long start) {
        mFollowingEventStart = start;
    }

    public long getFollowingEventStart() {
        return mFollowingEventStart;
    }

    //===============================================================================================
    // Calendar event information class
    //===============================================================================================
    /**
     * EventInfo represents an event in the widget
     */
    public record EventInfo(
            long id,
            String title,
            String description,
            long start,
            long end,
            boolean allDay) implements Comparable<EventInfo> {

        @Override
        public int compareTo(EventInfo other) {
            int startComparison = Long.compare(start, other.start);
            if (startComparison != 0) {
                return startComparison;
            }
            return Boolean.compare(other.allDay, allDay);
        }
    }
}
