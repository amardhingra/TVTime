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

		System.out.println("Checking for alarm");
		
		Intent intent = new Intent(context, BackgroundSync.class);
		intent.setAction("com.sc.showcal.BACKGROUND_SYNC");

		boolean alarmUp = (PendingIntent.getService(context, 0,
				intent, PendingIntent.FLAG_NO_CREATE) != null);

		if (!alarmUp) {

			System.out.println("Started alarm");
			
			PendingIntent pintent = PendingIntent.getService(context, 0,
					intent, 0);
			AlarmManager alarm = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			alarm.setRepeating(AlarmManager.RTC, Calendar.getInstance()
					.getTimeInMillis(), 24 * 60 * 60 * 1000, pintent);

		}

	}
}
