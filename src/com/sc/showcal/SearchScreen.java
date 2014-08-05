package com.sc.showcal;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("unused")
public class SearchScreen extends Activity {

	// Linear layout in which views scroll
	LinearLayout scroller;

	// Search box
	EditText searchBox;

	LayoutInflater inflater;

	// Arraylist for search results
	ArrayList<Card> cards;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_screen);

		/* initializing the variables */

		scroller = (LinearLayout) findViewById(R.id.scroller_linear_layout_search);

		// adding a textwatcher to the searchbox
		searchBox = (EditText) findViewById(R.id.searchBar);
		searchBox.addTextChangedListener(tw);

		// getting the layout inflater
		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// initializing the arraylist for search results
		cards = new ArrayList<Card>();

		// setting the action bar text
		getActionBar().setTitle("Search");
	}

	/*************************
	 * BUTTON PRESS METHOD
	 *************************/

	// method that gets invoked when a user adds a show from the results
	public void addShow(View v) {
		// get the position of the card in the arraylist
		int position = Integer.parseInt(((TextView) ((LinearLayout) v
				.getParent().getParent()).findViewById(R.id.search_position))
				.getText().toString());

		// get the TVRage ID of the show
		new IDServCon().execute(cards.get(position));
	}

	/******************************
	 * METHODS TO SAVE CARD DATA
	 ******************************/

	// method for saving image of added show
	public void saveImage(Card c) {
		try {
			FileOutputStream out = openFileOutput(c.getTitle() + ".jpg", 0);
			c.getBitmap().compress(CompressFormat.JPEG, 100, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// method of saving card of added show
	public void saveCard(Card newCard) {

		// reading in the shows already saved in memory and updating it
		SharedPreferences prefs = getSharedPreferences(StartScreen.PREFS_NAME,
				0);
		int numberOfShows = prefs.getInt("number_of_shows", 0);
		prefs.edit().putInt("number_of_shows", numberOfShows + 1).apply();

		cards = new ArrayList<Card>();
		try {
			if (numberOfShows > 0) {
				ObjectInputStream ois = new ObjectInputStream(
						openFileInput("shows.dat"));
				for (int i = 0; i < numberOfShows; i++) {
					Card card = (Card) ois.readObject();
					cards.add(card);
				}
			}

			// adding the new card to the list of cards
			cards.add(newCard);

			// writing the list back into memory
			ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(
					"shows.dat", 0));
			for (int i = 0; i < numberOfShows + 1; i++)
				oos.writeObject(cards.get(i));
			oos.flush();
			oos.close();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*************************
	 * SEARC BOX TEXTWATCHER
	 *************************/

	// Textwatcher that starts a search every time the user hits enter
	TextWatcher tw = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			// checking that the length is more than 2 and
			// the last character is a new line
			if (s.length() > 2 && s.charAt(s.length() - 1) == '\n') {
				// starting a search
				new SearchServCon().execute(s.toString().replaceAll("\n", "")
						.replaceAll(" ", "%20"));

				// getting rid of the new line character
				searchBox.removeTextChangedListener(tw);
				String newString = s.toString().replaceAll("\n", "");
				searchBox.setText(newString);
				searchBox.setSelection(newString.length());
				searchBox.addTextChangedListener(tw);
			}
		}
	};

	/*************************
	 * OMDB SEARCH ASYNCTASK
	 *************************/

	// ASyncTask that searches the OMDB database
	public class SearchServCon extends AsyncTask<String, Card, Void> {

		@Override
		protected void onPreExecute() {
			// reset the downloaded shows and remove all the views from the
			// scroller
			cards = new ArrayList<Card>();
			scroller.removeAllViews();

			// add a progressbar
			scroller.addView(inflater.inflate(R.layout.loading_table_row, null,
					false));
		}

		@Override
		protected Void doInBackground(String... params) {
			try {

				// create a http connection to OMDB and get the status line
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = httpclient.execute(new HttpGet(
						StartScreen.OMDB_SEARCH + params[0]));
				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

					// create a JSONObject from the input
					JSONObject jObject = new JSONObject(buildString(response));
					// get an array of the search results
					JSONArray jArray = jObject.getJSONArray("Search");

					for (int i = 0; i < jArray.length(); i++) {

						// get each seatch result and
						jObject = jArray.getJSONObject(i);

						// if the result was a tv series create a card and get
						// more information
						String type = jObject.getString("Type");
						if (type.equals("series")) {
							Card c = new Card(jObject.getString("Title"),
									jObject.getString("imdbID"),
									jObject.getString("Year"),
									jObject.getString("Type"));

							addInformation(c);
							publishProgress(c);
						}
					}

				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Card... values) {

			// setting up the card with the relevant information
			Card c = values[0];
			View singleCard = inflater.inflate(R.layout.search_screen_card,
					null, false);
			((TextView) singleCard.findViewById(R.id.search_position))
					.setText(cards.size() + "");
			((TextView) singleCard.findViewById(R.id.name_tv_search)).setText(c
					.getTitle());
			((TextView) singleCard.findViewById(R.id.years_tv_search))
					.setText(c.getYears());
			((TextView) singleCard.findViewById(R.id.description_tv_search))
					.setText(c.getPlot());

			// Loading the image that was saved in memory
			ImageView im = (ImageView) singleCard
					.findViewById(R.id.image_search);
			if (c.getBitmap() != null) {
				im.setImageBitmap(c.getBitmap());
				im.setVisibility(View.VISIBLE);
			}

			scroller.addView(singleCard);
			cards.add(c);
		}

		@Override
		protected void onPostExecute(Void result) {
			// remove the progressbar
			scroller.removeViewAt(0);
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

	// method for getting more information about the show from omdb
	private void addInformation(Card c) throws ClientProtocolException,
			IOException, JSONException {

		// creating a httpclient to get the information
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute(new HttpGet(
				StartScreen.OMDB_GET_IMDBID + c.getImdbID()));
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

			// creating a JSONObject from the response
			JSONObject jObject = new JSONObject(buildString(response));

			// Adding the data to the card
			c.setActors(jObject.getString("Actors").split(","));
			c.setRated(jObject.getString("Rated"));
			c.setGenres(jObject.getString("Genre").split(","));
			c.setPlot(jObject.getString("Plot"));
			c.setPosterLink(jObject.getString("Poster"));
			c.setRuntime(jObject.getString("Runtime"));

			// Downloading the image and saving it to the card
			if (c.getPosterLink().contains(".jpg")) {
				HttpResponse newResponse = httpclient.execute(new HttpGet(c
						.getPosterLink()));
				StatusLine newStatusLine = newResponse.getStatusLine();
				if (newStatusLine.getStatusCode() == HttpStatus.SC_OK) {
					Bitmap bm = BitmapFactory.decodeStream(newResponse
							.getEntity().getContent());
					c.setBitmap(bm);
				}
			}
		}
	}

	/*************************
	 * TVRAGE ID ASYNCTASK
	 *************************/

	// ASyncTask for getting the TVRage ID
	public class IDServCon extends AsyncTask<Card, Void, Card> {

		@Override
		protected void onPreExecute() {
			// Tell the user the show is being added
			Toast.makeText(getApplicationContext(),
					"Adding to your shows. Please wait", Toast.LENGTH_LONG)
					.show();
		}

		@Override
		protected Card doInBackground(Card... params) {
			try {

				Card c = params[0];

				// Create a http request replacing all spaces in the show name
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = httpclient.execute(new HttpGet(
						StartScreen.SEARCH
								+ c.getTitle().replaceAll("\n", "")
										.replaceAll(" ", "%20")));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

					// create an xml pull parser to decode the input
					XmlPullParserFactory factory = XmlPullParserFactory
							.newInstance();
					factory.setNamespaceAware(true);
					XmlPullParser parser = factory.newPullParser();
					parser.setInput(new InputStreamReader(response.getEntity()
							.getContent()));

					// boolean for whether we have found the show we are looking
					// for
					boolean foundShow = false;

					// fields we want to save
					String showID = "";
					String seasons = "";

					// go through the input until we reach the end
					int eventType = parser.getEventType();
					while (eventType != XmlPullParser.END_DOCUMENT) {

						if (eventType == XmlPullParser.START_TAG) {
							String tag = parser.getName();

							// save the last showID we found
							if (tag.equals("showid")) {
								parser.next();
								showID = parser.getText();
							}
							// if the name matches the name from omdb we have
							// found the same show
							else if (tag.equals("name")) {
								parser.next();
								if (parser.getText().equalsIgnoreCase(
										c.getTitle()))
									foundShow = true;
							} else if (tag.equals("seasons")) {
								// save the last number of seasons we found
								parser.next();
								seasons = parser.getText();
							}

						} else if (eventType == XmlPullParser.END_TAG) {

							// if we have reached the end of a show and
							// we found the show we were looking for break
							String tag = parser.getName();
							if (tag.equals("show") && foundShow) {
								c.setTVRageID(showID);
								c.setNumberOfSeasons(Integer.parseInt(seasons));
								break;
							}
						} else if (eventType == XmlPullParser.TEXT) {
						}
						eventType = parser.next();
					}

					// return the card so we can get the episode list details
					return c;
				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Card c) {
			// once we have the show ID get the episode list
			new EpisodesServCon().execute(c);
		}

	}

	/*************************
	 * EPISODE LIST ASYNCTASK
	 *************************/

	// ASyncTask that saves the episode list of the show
	public class EpisodesServCon extends AsyncTask<Card, Void, Card> {

		@Override
		protected Card doInBackground(Card... params) {
			try {

				// get the relevant card
				Card c = params[0];

				// create a http request
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = httpclient.execute(new HttpGet(
						StartScreen.GET_EPISODES + c.getTVRageID()));
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

					// create an xml pull parser to handle the data
					XmlPullParserFactory factory = XmlPullParserFactory
							.newInstance();
					factory.setNamespaceAware(true);
					XmlPullParser parser = factory.newPullParser();
					parser.setInput(new InputStreamReader(response.getEntity()
							.getContent()));

					// fields we want to save
					String airdate = "";
					String episodeNumber = "";
					String title = "";
					int seasonNumber = 1;

					// current date to compare with the episodes
					Date currentDate = new Date(System.currentTimeMillis());

					int eventType = parser.getEventType();

					while (eventType != XmlPullParser.END_DOCUMENT) {

						if (eventType == XmlPullParser.START_TAG) {
							String tag = parser.getName();

							// save each episode number
							if (tag.equals("seasonnum")) {
								parser.next();
								episodeNumber = parser.getText();
							}
							// save each air date
							else if (tag.equals("airdate")) {
								parser.next();
								airdate = parser.getText();
							}
							// save each title
							else if (tag.equals("title")) {
								parser.next();
								title = parser.getText();
							}
							// break on special episodes
							else if (tag.equals("Special")) {
								break;
							}

						} else if (eventType == XmlPullParser.END_TAG) {
							String tag = parser.getName();

							// if we have reached the end of a season increment
							// the counter
							if (tag.equals("Season")) {
								seasonNumber++;
							}
							// if we have reached the end of an episode save it
							// to the card
							else if (tag.equals("episode")) {
								if (StartScreen.dateFormat.parse(airdate)
										.compareTo(currentDate) > 0)
									c.addEpisode(new Episode(title, airdate,
											seasonNumber, Integer
													.parseInt(episodeNumber)));
							}
						}
						eventType = parser.next();
					}
					c.wasUpdated = true;
					return c;

				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}

			return null;

		}

		@Override
		protected void onPostExecute(Card c) {
			// after we are done save the data to the file
			saveImage(c);
			saveCard(c);
			finish();
		}
	}

	/*************************
	 * OPTIONS MENU METHODS
	 *************************/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Log.i("com.sc.showcal", "SearchScreen onPause()");
		getSharedPreferences(StartScreen.PREFS_NAME, 0).edit()
				.putBoolean("is_running", false).apply();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Log.i("com.sc.showcal", "SearchScreen onResume()");
		getSharedPreferences(StartScreen.PREFS_NAME, 0).edit()
				.putBoolean("is_running", true).apply();
	}

}
