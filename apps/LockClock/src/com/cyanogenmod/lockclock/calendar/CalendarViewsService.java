/*
 * Copyright (C) 2013 The CyanogenMod Project (DvTonder)
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

import android.app.AlarmManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.cyanogenmod.lockclock.ClockWidgetProvider;
import com.cyanogenmod.lockclock.ClockWidgetService;
import com.cyanogenmod.lockclock.R;
import com.cyanogenmod.lockclock.calendar.CalendarInfo.EventInfo;
import com.cyanogenmod.lockclock.misc.Constants;
import com.cyanogenmod.lockclock.misc.Preferences;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class CalendarViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CalendarRemoteViewsFactory(this.getApplicationContext(), intent);
    }

}

class CalendarRemoteViewsFactory implements RemoteViewsFactory {
    private static final String TAG = "CalendarRemoteViewsFactory";
    private static final boolean D = Constants.DEBUG;

    private static final long UPCOMING_EVENT_HOURS_IN_MILLIS =
            Constants.CALENDAR_UPCOMING_EVENTS_FROM_HOUR * 60L * 60L * 1000L;
    private static final long DAY_IN_MILLIS = 24L * 60L * 60L * 1000L;

    private final Context mContext;
    private CalendarInfo mCalendarInfo = new CalendarInfo();

    public CalendarRemoteViewsFactory(Context applicationContext, Intent intent) {
        mContext = applicationContext;
    }

    @Override
    public int getCount() {
        return mCalendarInfo.getEvents().size();
    }

    @Override
    public long getItemId(int position) {
        return mCalendarInfo.getEvents().get(position).id();
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    private SpannableString getSpannableString(String text, boolean bold) {
        if (text == null) {
            return null;
        }
        var spanText = new SpannableString(text);
        if (bold) {
            spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
        }
        return spanText;
    }

    private long getStartOfDay() {
        var cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private boolean isUpcoming(EventInfo event) {
        long startOfDay = getStartOfDay();
        long now = System.currentTimeMillis();
        long endOfUpcoming;

        if (startOfDay + UPCOMING_EVENT_HOURS_IN_MILLIS > now) {
            endOfUpcoming = startOfDay + DAY_IN_MILLIS;
        } else {
            endOfUpcoming = startOfDay + 2 * DAY_IN_MILLIS;
        }
        return event.start() < endOfUpcoming;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= mCalendarInfo.getEvents().size()) {
            return null;
        }

        boolean highlightNext = Preferences.calendarHighlightUpcomingEvents(mContext);
        boolean nextBold = Preferences.calendarUpcomingEventsBold(mContext);
        int color;
        int detailsColor;
        var itemViews = new RemoteViews(mContext.getPackageName(), R.layout.calendar_item);
        var event = mCalendarInfo.getEvents().get(position);

        // Add the event text fields
        if (highlightNext && isUpcoming(event)) {
            color = Preferences.calendarUpcomingEventsFontColor(mContext);
            detailsColor = Preferences.calendarUpcomingEventsDetailsFontColor(mContext);
            itemViews.setTextViewText(R.id.calendar_event_title,
                    getSpannableString(event.title(), nextBold));
            itemViews.setTextViewText(R.id.calendar_event_details,
                    getSpannableString(event.description(), nextBold));
        } else {
            color = Preferences.calendarFontColor(mContext);
            detailsColor = Preferences.calendarDetailsFontColor(mContext);
            itemViews.setTextViewText(R.id.calendar_event_title, event.title());
            itemViews.setTextViewText(R.id.calendar_event_details, event.description());
        }
        itemViews.setTextColor(R.id.calendar_event_title, color);
        itemViews.setTextColor(R.id.calendar_event_details, detailsColor);
        if (D) Log.v(TAG, "Showing at position " + position + " event: " + event.title());

        var fillInIntent = new Intent();
        fillInIntent.setData(ContentUris.withAppendedId(Events.CONTENT_URI, event.id()));
        // work around stock calendar not displaying the correct date with only uri
        fillInIntent.putExtra("beginTime", event.start());
        fillInIntent.putExtra("endTime", event.end());
        fillInIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        itemViews.setOnClickFillInIntent(R.id.calendar_item, fillInIntent);

        return itemViews;
    }

    @Override
    public int getViewTypeCount() {
        // There's only one view type for the events
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
        updateCalendarInfo(mContext);
        updatePanelVisibility();
    }

    @Override
    public void onDataSetChanged() {
        if (D) Log.v(TAG, "onDataSetChanged()");
        updateCalendarInfo(mContext);
        updatePanelVisibility();
    }

    private void updateCalendarInfo(Context context) {
        // Load the settings
        Set<String> calendarList = Preferences.calendarsToDisplay(context);
        boolean remindersOnly = Preferences.showEventsWithRemindersOnly(context);
        boolean hideAllDay = !Preferences.showAllDayEvents(context);
        long lookAhead = Preferences.lookAheadTimeInMs(context);

        if (D) Log.d(TAG, "Checking for calendar events...");
        getCalendarEvents(context, lookAhead, calendarList, remindersOnly, hideAllDay);
        scheduleCalendarUpdate(context);
    }

    /**
     * Trigger the hiding of the Calendar panel if there are no events to display
     */
    private void updatePanelVisibility() {
        if (!mCalendarInfo.hasEvents()) {
            if (D) Log.v(TAG, "No events - Hide calendar panel");
            var updateIntent = new Intent(mContext, ClockWidgetProvider.class);
            updateIntent.setAction(ClockWidgetService.ACTION_HIDE_CALENDAR);
            mContext.sendBroadcast(updateIntent);
        }
    }

    /**
     * Get the next set of calendar events (up to MAX_CALENDAR_ITEMS) within a
     * certain look-ahead time. Result is stored in the CalendarInfo object
     */
    private void getCalendarEvents(Context context, long lookahead, Set<String> calendars,
            boolean remindersOnly, boolean hideAllDay) {
        long now = System.currentTimeMillis();
        long later = now + lookahead;
        var newCalendarInfo = new CalendarInfo();

        // Build the 'where' clause
        var where = new StringBuilder();
        if (remindersOnly) {
            where.append(CalendarContract.Events.HAS_ALARM).append("=1");
        }
        if (hideAllDay) {
            if (remindersOnly) {
                where.append(" AND ");
            }
            where.append(CalendarContract.Events.ALL_DAY).append("!=1");
        }
        if (calendars != null && !calendars.isEmpty()) {
            if (remindersOnly || hideAllDay) {
                where.append(" AND ");
            }
            where.append(CalendarContract.Events.CALENDAR_ID)
                    .append(" in (")
                    .append(TextUtils.join(",", calendars))
                    .append(") ");
        }

        // Projection array
        String[] projection = {
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.ALL_DAY,
        };

        // all day events are stored in UTC, that is why we need to fetch events after 'later'
        var uri = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI,
                String.format(Locale.US, "%d/%d", now - DAY_IN_MILLIS, later + DAY_IN_MILLIS));
        try (var cursor = context.getContentResolver().query(uri, projection,
                where.toString(), null, CalendarContract.Instances.BEGIN + " ASC")) {
            if (cursor != null) {
                // The indices for the projection array
                int indexEventId = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID);
                int indexTitle = cursor.getColumnIndex(CalendarContract.Events.TITLE);
                int indexBeginTime = cursor.getColumnIndex(CalendarContract.Instances.BEGIN);
                int indexEndTime = cursor.getColumnIndex(CalendarContract.Instances.END);
                int indexDescription = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION);
                int indexLocation = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION);
                int indexAllDay = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY);

                int showLocation = Preferences.calendarLocationMode(context);
                int showDescription = Preferences.calendarDescriptionMode(context);
                var time = new Time();
                int eventCount = 0;

                // Iterate through returned rows to a maximum number of calendar events
                while (cursor.moveToNext() && eventCount < Constants.MAX_CALENDAR_ITEMS) {
                    long eventId = cursor.getLong(indexEventId);
                    String title = cursor.getString(indexTitle);
                    long begin = cursor.getLong(indexBeginTime);
                    long end = cursor.getLong(indexEndTime);
                    String description = cursor.getString(indexDescription);
                    String location = cursor.getString(indexLocation);
                    boolean allDay = cursor.getInt(indexAllDay) != 0;
                    boolean multiDay = allDay && (end - begin) > DAY_IN_MILLIS;

                    if (allDay) {
                        begin = convertUtcToLocal(time, begin);
                        end = convertUtcToLocal(time, end);
                    }

                    if (end < now || begin > later) {
                        continue;
                    }

                    if (D) Log.v(TAG, "Adding event: " + title + " with id: " + eventId);

                    // Start building the event details string
                    // Starting with the date
                    var sb = new StringBuilder();
                    int format;
                    if (allDay) {
                        format = Constants.CALENDAR_FORMAT_ALLDAY;
                    } else if (DateUtils.isToday(begin)) {
                        format = Constants.CALENDAR_FORMAT_TODAY;
                    } else {
                        format = Constants.CALENDAR_FORMAT_FUTURE;
                    }
                    if ((allDay && !multiDay) || begin == end) {
                        sb.append(DateUtils.formatDateTime(context, begin, format));
                    } else {
                        sb.append(DateUtils.formatDateRange(context, begin, end, format));
                    }

                    // Add the event location if it should be shown
                    if (showLocation != Preferences.SHOW_NEVER && !TextUtils.isEmpty(location)) {
                        switch (showLocation) {
                            case Preferences.SHOW_FIRST_LINE -> {
                                int stringEnd = location.indexOf('\n');
                                sb.append(": ");
                                sb.append(stringEnd == -1
                                        ? location
                                        : location.substring(0, stringEnd));
                            }
                            case Preferences.SHOW_ALWAYS -> sb.append(": ").append(location);
                        }
                    }

                    // Add the event description if it should be shown
                    if (showDescription != Preferences.SHOW_NEVER
                            && !TextUtils.isEmpty(description)) {
                        // Show the appropriate separator
                        sb.append(showLocation == Preferences.SHOW_NEVER ? ": " : " - ");

                        switch (showDescription) {
                            case Preferences.SHOW_FIRST_LINE -> {
                                int stringEnd = description.indexOf('\n');
                                sb.append(stringEnd == -1
                                        ? description
                                        : description.substring(0, stringEnd));
                            }
                            case Preferences.SHOW_ALWAYS -> sb.append(description);
                        }
                    }

                    // Add the event details to the CalendarInfo object and move to next record
                    newCalendarInfo.addEvent(new EventInfo(eventId, title, sb.toString(), begin,
                            end, allDay));
                    eventCount++;
                }
                mCalendarInfo = newCalendarInfo;
            }
        }

        // check for first event outside of lookahead window
        long endOfLookahead = now + lookahead;
        long minUpdateTime = getMinUpdateFromNow(endOfLookahead);

        // don't bother with querying if the end result is later than the minimum update time anyway
        if (endOfLookahead < minUpdateTime) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(CalendarContract.Instances.BEGIN);
            where.append(" > ");
            where.append(endOfLookahead);

            uri = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI,
                    String.format(Locale.US, "%d/%d", endOfLookahead, minUpdateTime));
            projection = new String[] {
                    CalendarContract.Instances.BEGIN
            };
            try (var cursor = context.getContentResolver().query(uri, projection, where.toString(),
                    null, CalendarContract.Instances.BEGIN + " ASC limit 1")) {
                if (cursor != null && cursor.moveToFirst()) {
                    mCalendarInfo.setFollowingEventStart(cursor.getLong(0));
                }
            }
        }
    }

    private static long convertUtcToLocal(Time time, long utcTime) {
        time.timezone = Time.TIMEZONE_UTC;
        time.set(utcTime);
        time.timezone = Time.getCurrentTimezone();
        return time.normalize(true);
    }

    private static long getMinUpdateFromNow(long now) {
        // we update at least once a day
        return now + DAY_IN_MILLIS;
    }

    // ===============================================================================================
    // Update timer related functionality
    // ===============================================================================================
    /**
     * Calculates and returns the next time we should push widget updates.
     */
    private long calculateUpdateTime(Context context) {
        long now = System.currentTimeMillis();
        boolean highlightNext = Preferences.calendarHighlightUpcomingEvents(mContext);
        long lookAhead = Preferences.lookAheadTimeInMs(context);
        long minUpdateTime = getMinUpdateFromNow(now);

        // Check if there is a calendar event earlier
        for (EventInfo event : mCalendarInfo.getEvents()) {
            long end = event.end();
            long start = event.start();
            if (now < start) {
                minUpdateTime = Math.min(minUpdateTime, start);
            }
            if (now < end) {
                minUpdateTime = Math.min(minUpdateTime, end);
            }
        }

        if (mCalendarInfo.getFollowingEventStart() > 0) {
            // Make sure to update when the next event gets into the lookahead window
            minUpdateTime = Math.min(minUpdateTime, mCalendarInfo.getFollowingEventStart()
                    - lookAhead);
        }

        if (highlightNext) {
            // Update at midnight and at 8pm if highlighting of upcoming events is enabled
            long startOfDay = getStartOfDay();
            if (now < startOfDay + UPCOMING_EVENT_HOURS_IN_MILLIS
                    && startOfDay + UPCOMING_EVENT_HOURS_IN_MILLIS < minUpdateTime) {
                minUpdateTime = startOfDay + UPCOMING_EVENT_HOURS_IN_MILLIS;
            } else if (startOfDay + DAY_IN_MILLIS < minUpdateTime) {
                minUpdateTime = startOfDay + DAY_IN_MILLIS;
            }
        }

        // Construct a log entry in human readable form
        if (D) {
            var date1 = new Date(now);
            var date2 = new Date(minUpdateTime);
            Log.i(TAG, "cLock: It is now " + DateFormat.getTimeFormat(context).format(date1)
                    + ", next widget update on " + DateFormat.getDateFormat(context).format(date2)
                    + " at " + DateFormat.getTimeFormat(context).format(date2));
        }

        // Return the next update time
        return minUpdateTime;
    }

    /**
     * Schedule an alarm to trigger an update at the next weather refresh or at
     * the next event time boundary (start/end).
     */
    private void scheduleCalendarUpdate(Context context) {
        var pi = ClockWidgetService.getRefreshIntent(context);
        long updateTime = calculateUpdateTime(context);

        // Clear any old alarms and schedule the new alarm
        // Since the updates are now only done very infrequently, it can wake the device to ensure
        // the latest date is available when the user turns the screen on after a few hours sleep
        var am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        if (updateTime > 0) {
            am.set(AlarmManager.RTC_WAKEUP, updateTime, pi);
        }
    }

    @Override
    public void onDestroy() {
        mCalendarInfo.clearEvents();
    }
}
