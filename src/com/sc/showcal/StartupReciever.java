package com.sc.showcal;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent i) {

		Intent intent = new Intent(context, BackgroundSync.class);
		intent.setAction("com.sc.showcal.BACKGROUND_SYNC");

		boolean alarmUp = (PendingIntent.getService(context, 0, intent,
				PendingIntent.FLAG_NO_CREATE) != null);

		if (!alarmUp) {

			PendingIntent pintent = PendingIntent.getService(context, 0,
					intent, 0);
			AlarmManager alarm = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			alarm.setRepeating(AlarmManager.RTC, Calendar.getInstance()
					.getTimeInMillis(), 2 * 24 * 3600 * 1000, pintent);

		}

	}
}
