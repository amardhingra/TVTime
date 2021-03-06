package com.sc.showcal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
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

		int numberOfShows = getSharedPreferences(Strings.PREFS_NAME, 0).getInt(
				Strings.NUMBER_OF_SHOWS, 0);

		String calendarId = prefs.getString(Strings.CALENDAR_ID, "-1");
		if (calendarId.equals("-1"))
			calendarId = prefs.getString(Strings.BACKGROUND_CAL_ID, "-1");
		
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		
		if (numberOfShows > 0 && !prefs.getBoolean(Strings.IS_RUNNING, false) && ni != null && ni.isConnected()) {
			
			ArrayList<Card> cards = new ArrayList<Card>();

			try {
				ObjectInputStream ois = new ObjectInputStream(
						openFileInput("shows.dat"));
				for (int i = 0; i < numberOfShows; i++)
					cards.add((Card) ois.readObject());
			} catch (IOException e) {
			} catch (ClassNotFoundException e) {
			}

			for (Card c : cards) {

				boolean downloaded = false;
				while (!downloaded) {
					try {

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
							parser.setInput(new InputStreamReader(
									new ByteArrayInputStream(buildString(
											response).getBytes())));

							// data we want to update
							String airdate = "";
							String episodeNumber = "";
							String title = "";
							int seasonNumber = 1;

							// current date
							Date currentDate = 
									new Date(
									System.currentTimeMillis());

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
										Date epDate = new Date(c.offset
												+ StartScreen.dateFormat.parse(
														airdate).getTime());

										if (epDate.compareTo(currentDate) > 0
												|| epDate
														.compareTo(currentDate) == 0) {
											Episode e = new Episode(
													title,
													airdate,
													seasonNumber,
													Integer.parseInt(episodeNumber));
											updatedEpisodes.add(e);
										}
									}
								}
								eventType = parser.next();
							}

							if (c.addedToCalendar
									&& updatedEpisodes.size() > 0
									&& !calendarId.equals("-1")
									&& !oldEpisodes.toString().equals(
											updatedEpisodes.toString())) {
																
								if (oldEpisodes != null)
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
											getApplicationContext(),
											calendarId, c.title, e.title,
											epDate, c.offset, c.runtime,
											"Season " + e.seasonNumber
													+ ": Episode "
													+ e.episodeNumber);
									e.setCalenderID(calId);
								}

								c.setEpisodes(updatedEpisodes);

							}
							downloaded = true;
						}

					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (XmlPullParserException e) {
						e.printStackTrace();
					}
				}
			}
			
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(openFileOutput(
						"shows.dat", 0));
				for (Card c : cards)
					oos.writeObject(c);
				oos.flush();
				oos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (prefs.getBoolean(Strings.UPDATED, false) && numberOfShows > 0) {

			try {

				ObjectInputStream ois = new ObjectInputStream(
						openFileInput("shows.dat"));

				ArrayList<Card> cards = new ArrayList<Card>();
				for (int i = 0; i < numberOfShows; i++)
					cards.add((Card) ois.readObject());

				for (Card c : cards) {
					ArrayList<Episode> episodes = c.getEpisodes();
					if (c.addedToCalendar && episodes != null
							&& !calendarId.equals("-1")) {

						for (Episode e : episodes) {

							if (e.calenderID == null) {
								Date epDate = StartScreen.dateFormat
										.parse(e.airDate);
								String calId = CalendarEditor.addEvent(
										getApplicationContext(), calendarId,
										c.title, e.title, epDate, c.offset,
										c.runtime, "Season " + e.seasonNumber
												+ ": Episode "
												+ e.episodeNumber);
								e.setCalenderID(calId);
							}

							c.setEpisodes(episodes);
						}
					}
				}

				ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(
						"shows.dat", 0));

				for (Card c : cards)
					oos.writeObject(c);
				oos.flush();
				oos.close();

				prefs.edit().putBoolean(Strings.UPDATED, false).apply();
			} catch (IOException e) {
			} catch (ClassNotFoundException e) {
			} catch (ParseException e) {
			}
		}
	}

	public String buildString(HttpResponse response)
			throws IllegalStateException, IOException {
		// create a buffered reader and build a string from the input
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				response.getEntity().getContent()));

		// read lines appending them to a string builder
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}

		return sb.toString();
	}
}
