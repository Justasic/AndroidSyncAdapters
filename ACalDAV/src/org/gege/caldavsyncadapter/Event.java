/**
 * Copyright (c) 2012-2013, Gerald Garcia, Timo Berger
 *
 * This file is part of Andoid Caldav Sync Adapter Free.
 *
 * Andoid Caldav Sync Adapter Free is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or at your option any later version.
 *
 * Andoid Caldav Sync Adapter Free is distributed in the hope that 
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Andoid Caldav Sync Adapter Free.  
 * If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.gege.caldavsyncadapter;

import android.content.ContentValues;
import android.provider.CalendarContract.Events;
import android.util.Log;

import java.util.ArrayList;

/**
 * abstract class for Calendar and Android events
 */
abstract public class Event {

    /**
     * stores the ETAG of an event
     */
    public final static String ETAG = Events.SYNC_DATA1;

    private static final String TAG = "Event";

    /**
     * internal Tag used to identify deleted events
     */
    public static String INTERNALTAG = Events.SYNC_DATA2;

    /**
     * store the whole VEVENT in here
     * missing TAGs they might be missing for google update
     * <p/>
     * CREATED:20130906T102857Z
     * DTSTAMP:20130906T102857Z
     * LAST-MODIFIED:20130906T102857Z
     * SEQUENCE:0
     */
    public static String RAWDATA = Events.SYNC_DATA3;

    /**
     * stores the UID of an Event
     * example: UID:e6be67c6-eff0-44f8-a1a0-6c2cb1029944-caldavsyncadapter
     */
    public static String UID = Events.SYNC_DATA4;

    /**
     * the event transformed into ContentValues
     */
    public ContentValues ContentValues = new ContentValues();

    /**
     * returns a list of all items that are comparable with this sync adapter
     *
     * @return a list of all items that are comparable with this sync adapter
     */
    public static java.util.ArrayList<String> getComparableItems() {
        ArrayList<String> lcResult = new ArrayList<String>();
        lcResult.add(Events.DTSTART);
        lcResult.add(Events.DTEND);
        lcResult.add(Events.EVENT_TIMEZONE);
        lcResult.add(Events.EVENT_END_TIMEZONE);
        lcResult.add(Events.ALL_DAY);
        lcResult.add(Events.DURATION);
        lcResult.add(Events.TITLE);
        lcResult.add(Events.CALENDAR_ID);
        lcResult.add(Events._SYNC_ID);
        lcResult.add(ETAG);
        lcResult.add(Events.DESCRIPTION);
        lcResult.add(Events.EVENT_LOCATION);
        lcResult.add(Events.ACCESS_LEVEL);
        lcResult.add(Events.STATUS);
        lcResult.add(Events.RDATE);
        lcResult.add(Events.RRULE);
        lcResult.add(Events.EXRULE);
        lcResult.add(Events.EXDATE);
        lcResult.add(UID);

        return lcResult;
    }

    abstract public String getETag();

    abstract public void setETag(String ETag);

    /**
     * returns the AndroidCalendarId for this event.
     *
     * @return the AndroidCalendarId for this event
     */
    public long getAndroidCalendarId() {
        long lcResult = -1;
        if (this.ContentValues.containsKey(Events.CALENDAR_ID)) {
            lcResult = this.ContentValues.getAsLong(Events.CALENDAR_ID);
        }
        return lcResult;
    }

    /**
     * sets the AndroidCalendarId for this event
     *
     * @param ID the AndroidCalendarId for this event
     */
    public void setAndroidCalendarId(long ID) {
        if (this.ContentValues.containsKey(Events.CALENDAR_ID)) {
            this.ContentValues.remove(Events.CALENDAR_ID);
        }

        this.ContentValues.put(Events.CALENDAR_ID, ID);
    }

    /**
     * returns the UID for this event. you can also check, whether the UID was stored from server.
     * the V1.7 release and before didn't save them.
     * example: UID:e6be67c6-eff0-44f8-a1a0-6c2cb1029944-caldavsyncadapter
     *
     * @return the UID for this event
     */
    public String getUID() {
        String lcResult = "";
        if (this.ContentValues.containsKey(UID)) {
            lcResult = this.ContentValues.getAsString(UID);
        }

        return lcResult;
    }

    /**
     * compares the given ContentValues with the current ones for differences
     *
     * @param calendarEventValues the contentValues of the calendar event
     * @return if the events are different
     */
    public boolean checkEventValuesChanged(ContentValues calendarEventValues) {
        boolean lcResult = false;
        Object lcValueAndroid = null;
        Object lcValueCalendar = null;
        java.util.ArrayList<String> lcCompareItems = Event.getComparableItems();

        for (String key : lcCompareItems) {

            if (this.ContentValues.containsKey(key)) {
                lcValueAndroid = this.ContentValues.get(key);
            } else {
                lcValueAndroid = null;
            }

            if (calendarEventValues.containsKey(key)) {
                lcValueCalendar = calendarEventValues.get(key);
            } else {
                lcValueCalendar = null;
            }

			/*
                         * TODO: Sync is designed to "Server always wins", should be a general option for this adapter
			 */
            if (lcValueAndroid != null) {
                if (lcValueCalendar != null) {
                    if (!lcValueAndroid.toString().equals(lcValueCalendar.toString())) {
                        Log.d(TAG, "difference in " + key.toString() + ":" + lcValueAndroid.toString()
                                + " <> " + lcValueCalendar.toString());
                        this.ContentValues.put(key, lcValueCalendar.toString());
                        lcResult = true;
                    }
                } else {
                    Log.d(TAG, "difference in " + key.toString() + ":" + lcValueAndroid.toString()
                            + " <> null");
                    this.ContentValues.putNull(key);
                    lcResult = true;
                }
            } else {
                if (lcValueCalendar != null) {
                    Log.d(TAG, "difference in " + key.toString() + ":null <> " + lcValueCalendar
                            .toString());
                    this.ContentValues.put(key, lcValueCalendar.toString());
                    lcResult = true;
                } else {
                    // both null -> this is ok
                }
            }
        }

        return lcResult;
    }
}
