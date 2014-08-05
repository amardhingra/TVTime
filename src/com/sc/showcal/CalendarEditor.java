package com.sc.showcal;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;

public class CalendarEditor {

	public static String addEvent(Context context, String calendarId,
			String showTitle, String episodeTitle, Date airDate, String description) {

		// constructing the calendar event to insert
		ContentValues values = new ContentValues();
		values.put("calendar_id", calendarId);
		values.put(Events.TITLE, showTitle);
		values.put(Events.DESCRIPTION, episodeTitle + ", " +  description);
		values.put(Events.EVENT_TIMEZONE, "America/New_York");
		values.put(Events.DTSTART, airDate.getTime() + 61200000);
		values.put(Events.DTEND, airDate.getTime() + 61200000 + 3600000);
		values.put(Events.EVENT_COLOR,
				context.getResources().getColor(R.color.app_color));
		values.put(Events.SELF_ATTENDEE_STATUS, Events.STATUS_CONFIRMED);

		// getting the URI and inserting the event
		Uri eventURI = Uri.parse("content://com.android.calendar/events");
		Uri calURI = context.getContentResolver().insert(eventURI, values);

		// adding a reminder to the calender event
		values.clear();
		values.put(Reminders.EVENT_ID, calURI.getLastPathSegment());
		values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
		values.put(Reminders.MINUTES, 0);
		context.getContentResolver().insert(Reminders.CONTENT_URI, values);

		return calURI.getLastPathSegment();
	}

	public static void deleteEvent(Context context, String eventID) {

		// setting up the arguments
		String[] eventArgs = new String[] { eventID };
		
		// deleting the event
		context.getContentResolver().delete(Events.CONTENT_URI, Events._ID + " =? ",
				eventArgs);
	}

}
