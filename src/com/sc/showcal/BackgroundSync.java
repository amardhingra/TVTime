package com.sc.showcal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class BackgroundSync extends IntentService {

	public BackgroundSync() {
		super("BackgroundSync");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		SharedPreferences prefs = getSharedPreferences(Strings.PREFS_NAME, 0);

		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		int numberOfShows = getSharedPreferences(Strings.PREFS_NAME, 0).getInt(
				Strings.NUMBER_OF_SHOWS, 0);

		String calendarId = prefs.getString(Strings.CALENDAR_ID, "-1");
		if (calendarId.equals("-1"))
			calendarId = prefs.getString(Strings.BACKGROUND_CAL_ID, "-1");

		System.out.println("Trying background sync");

		if ((mWifi.isConnected() && numberOfShows > 0 && !prefs.getBoolean(
				Strings.IS_RUNNING, false))
				|| prefs.getBoolean(Strings.UPDATED, false)) {

			System.out.println("Starting wifi sync");

			try {

				ObjectInputStream ois = new ObjectInputStream(
						openFileInput("shows.dat"));

				ArrayList<Card> cards = new ArrayList<Card>();
				for (int i = 0; i < numberOfShows; i++)
					cards.add((Card) ois.readObject());

				for (Card c : cards) {

					System.out.println("Downloading data for " + c.title);

					ArrayList<Episode> updatedEpisodes = new ArrayList<Episode>();
					ArrayList<Episode> oldEpisodes = c.getEpisodes();

					// creating a http client
					HttpClient httpclient = new DefaultHttpClient();
					HttpResponse response = httpclient.execute(new HttpGet(
							Strings.GET_EPISODES + c.getTVRageID()));
					StatusLine statusLine = response.getStatusLine();
					if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

						// creating an xml pull parser to parse the data
						XmlPullParserFactory factory = XmlPullParserFactory
								.newInstance();
						factory.setNamespaceAware(true);
						XmlPullParser parser = factory.newPullParser();
						parser.setInput(new InputStreamReader(response
								.getEntity().getContent()));

						// data we want to update
						String airdate = "";
						String episodeNumber = "";
						String title = "";
						int seasonNumber = 1;

						// current date
						Date currentDate = new Date(System.currentTimeMillis());

						int eventType = parser.getEventType();

						while (eventType != XmlPullParser.END_DOCUMENT) {

							if (eventType == XmlPullParser.START_TAG) {
								String tag = parser.getName();
								// remember the last episode, airdate and
								// title
								if (tag.equals("seasonnum")) {
									parser.next();
									episodeNumber = parser.getText();
								} else if (tag.equals("airdate")) {
									parser.next();
									airdate = parser.getText();
								} else if (tag.equals("title")) {
									parser.next();
									title = parser.getText();
								}
								// break if we reach a special episode
								else if (tag.equals("Special")) {
									break;
								}

							} else if (eventType == XmlPullParser.END_TAG) {
								String tag = parser.getName();

								// increment the season number
								if (tag.equals("Season")) {
									seasonNumber++;
								}
								// if we are at the end of a future
								// episode add it to the arraylist
								else if (tag.equals("episode")) {
									if (StartScreen.dateFormat.parse(airdate)
											.compareTo(currentDate) > 0) {
										Episode e = new Episode(title, airdate,
												seasonNumber,
												Integer.parseInt(episodeNumber));
										updatedEpisodes.add(e);
									}
								}
							}
							eventType = parser.next();
						}

						if (c.addedToCalendar && updatedEpisodes.size() > 0
								&& !calendarId.equals("-1")) {

							System.out.println("Changine calendar events for "
									+ c.title);

							for (Episode e : oldEpisodes) {
								if (e.calenderID != null) {
									CalendarEditor.deleteEvent(
											getApplicationContext(),
											e.getCalenderID());
								}
							}

							for (Episode e : updatedEpisodes) {
								Date epDate = StartScreen.dateFormat
										.parse(e.airDate);
								String calId = CalendarEditor.addEvent(
										getApplicationContext(), calendarId,
										c.title, e.title, epDate, "Season "
												+ e.seasonNumber + ": Episode "
												+ e.episodeNumber);
								e.setCalenderID(calId);
							}

							c.setEpisodes(updatedEpisodes);

						}

						// save the new arraylist to the card

						ObjectOutputStream oos = new ObjectOutputStream(
								openFileOutput("shows.dat", 0));

						for (Card crd : cards)
							oos.writeObject(crd);
						oos.flush();
						oos.close();

						prefs.edit().putBoolean(Strings.UPDATED, false).apply();
					}
				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}

		}
	}
}
