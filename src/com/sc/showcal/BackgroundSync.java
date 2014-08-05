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
import android.util.Log;

public class BackgroundSync extends IntentService {

	public BackgroundSync() {
		super("BackgroundSync");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		SharedPreferences prefs = getSharedPreferences(StartScreen.PREFS_NAME,
				0);

		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		int numberOfShows = getSharedPreferences(StartScreen.PREFS_NAME, 0)
				.getInt("number_of_shows", 0);

		if (mWifi.isConnected() && numberOfShows > 0
				&& !prefs.getBoolean("is_running", false)) {

			try {

				Log.i("com.sc.showcal", "Started background sync");
				ObjectInputStream ois = new ObjectInputStream(
						openFileInput("shows.dat"));

				ArrayList<Card> shows = new ArrayList<Card>();
				for (int i = 0; i < numberOfShows; i++)
					shows.add((Card) ois.readObject());

				for (Card c : shows) {

					ArrayList<Episode> updatedEpisodes = new ArrayList<Episode>();

					// creating a http client
					HttpClient httpclient = new DefaultHttpClient();
					HttpResponse response = httpclient.execute(new HttpGet(
							StartScreen.GET_EPISODES + c.getTVRageID()));
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
								// remember the last episode, airdate and title
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
								} // if we are at the end of a future episode
									// add // it // to // the arraylist else
								if (tag.equals("episode")) {
									if (StartScreen.dateFormat.parse(airdate)
											.compareTo(currentDate) > 0) {
										Episode e = new Episode(title, airdate,
												seasonNumber,
												Integer.parseInt(episodeNumber));
										for(Episode ep:c.episodes)
											if(ep.airDate != null && ep.airDate.equals(e.airDate))
												e.calenderID = ep.calenderID;
										updatedEpisodes.add(e);
									}
								}
							}
							eventType = parser.next();
						}

						// save the new arraylist to the card
						c.setEpisodes(updatedEpisodes);
						c.lastUpdated = System.currentTimeMillis();
						c.wasUpdated = true;

						ObjectOutputStream oos = new ObjectOutputStream(
								openFileOutput("shows.dat", 0));
						for (Card crd : shows)
							oos.writeObject(crd);
						oos.flush();
						oos.close();

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
