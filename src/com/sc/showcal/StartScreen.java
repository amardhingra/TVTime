package com.sc.showcal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract.Calendars;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("unused")
public class StartScreen extends Activity {

	ArrayList<Card> cards;

	// textview for first time users
	TextView pressButton;

	// linear layout into which to place views
	LinearLayout scroller;

	// LayoutInflater
	LayoutInflater inflater;

	// SharedPreferences for the app
	SharedPreferences prefs;

	// List of strings
	public final static String PREFS_NAME = "show_cal_prefs";
	public final static String SEARCH = "http://services.tvrage.com/feeds/search.php?show=";
	public final static String GET_EPISODES = "http://services.tvrage.com/feeds/episode_list.php?sid=";
	public final static String OMDB_SEARCH = "http://www.omdbapi.com/?s=";
	public final static String OMDB_GET_IMDBID = "http://www.omdbapi.com/?i=";

	// date format for tvrage
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd", Locale.US);

	// Objects to get users calendar choice
	private CalendarData[] userCalenders;
	private String calendarId = "0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scroller);

		// initializing the views
		pressButton = (TextView) findViewById(R.id.no_show);
		scroller = (LinearLayout) findViewById(R.id.scroller_linear_layout);

		// getting the layout inflater and shared preferences
		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

		// getting the number of shows and either setting up the cards or
		// prompting the user to add a show
		int numberOfShows = prefs.getInt("number_of_shows", 0);
		if (numberOfShows == 0) {
			pressButton.setVisibility(View.VISIBLE);
		} else {
			loadCards(numberOfShows);
		}

		// Getting the default calender id
		calendarId = prefs.getString("calender_id", "0");

		Intent intent = new Intent();
		intent.setAction("com.sc.showcal.APPLICATION_STARTED");
		sendBroadcast(intent);

	}

	/********************
	 * ACTIVITY METHODS
	 ********************/

	// load the cards from the file when the activity is resumed
	@Override
	protected void onResume() {
		super.onResume();
		// Log.i("com.sc.showcal", "StartScreen onResume()");

		loadCards(prefs.getInt("number_of_shows", 0));
		prefs.edit().putBoolean("is_running", true).apply();
	}

	// Save the cards to the outfile when the activity is paused
	@Override
	protected void onPause() {
		super.onPause();
		// Log.i("com.sc.showcal", "StartScreen onPause()");
		saveCards();
		prefs.edit().putBoolean("is_running", false).apply();
	}

	/******************************
	 * METHODS FOR BUTTON PRESSES
	 ******************************/

	// method that gets called when user clicks more information button on any
	// card
	public void moreInformation(View v) {

		// Creating an Intent to go to the single show screen
		Intent singleShowScreen = new Intent(this, SingleShowScreen.class);

		// Getting the card the user selected
		Card c = cards.get(Integer.parseInt(((TextView) ((LinearLayout) v
				.getParent()).findViewById(R.id.position_tv_single_card))
				.getText().toString()) - 1);

		// writing the card to a temporary file
		try {
			ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(
					"temp.dat", 0));
			oos.writeObject(c);
			oos.flush();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

		// starting the activity
		startActivity(singleShowScreen);
	}

	// method that gets called when user tries to delete show
	public void deleteShow(final View v) {

		// creating an alert dialog confirming the user wants to delete the show
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setMessage("Are you sure you want to delete this show?");
		build.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteShow(Integer.parseInt(((TextView) ((TableRow) v
						.getParent())
						.findViewById(R.id.position_tv_single_card)).getText()
						.toString()));
			}
		});
		build.setNegativeButton("No", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		// displaying the alert dialogue
		build.create().show();
	}

	// helper method that deletes the show and all the calendar information
	private void deleteShow(int position) {

		// removing the card from the arraylist of cards
		Card c = cards.remove(position - 1);

		// getting the episode list
		ArrayList<Episode> episodes = c.getEpisodes();
		if (episodes != null)
			// deleting all the episodes for which a calendar event has been
			// created
			for (Episode e : episodes) {
				if (e.calenderID != null)
					CalendarEditor.deleteEvent(this, e.calenderID);
			}

		// reducing the number of shows saved in the apps shared preferences
		int currentNumberOfShows = prefs.getInt("number_of_shows", 0);
		prefs.edit().putInt("number_of_shows", currentNumberOfShows - 1)
				.apply();

		saveCards();
		loadCards(currentNumberOfShows - 1);

	}

	// TODO: get calendar choice
	// method that gets called when user adds the show to their calendar
	public void addShowToCalender(View v) {

		// get the card the user has selected
		int position = Integer.parseInt(((TextView) ((TableRow) v.getParent())
				.findViewById(R.id.position_tv_single_card)).getText()
				.toString());
		Card card = cards.get(position - 1);
		card.syncCalender = true;

		if (prefs.getString("calender_id", "0").equals("0")) {
			showListView(position);
		} else
		// start an asynctask that adds all the episodes to the calendar
		if (card.wasUpdated)
			new CalenderAdder().execute(card);
	}

	private void showListView(final int position) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select calendar");
		final String[] items = getCalenders();
		View v = inflater.inflate(R.layout.calendar_selecter, null, false);
		final CheckBox cb = (CheckBox) v.findViewById(R.id.choice_checkbox);
		builder.setView(v);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				System.out.println(which);
				System.out.println(cb.isChecked());
				System.out.println(userCalenders[which].id + "");
				System.out.println(which != items.length - 1);
				if (which != items.length - 1) {
					calendarId = userCalenders[which].id + "";
					if (cb.isChecked()) {
						prefs.edit()
								.putString("calender_id",
										userCalenders[which].id + "").apply();
					}
					new CalenderAdder().execute(cards.get(position - 1));

				}
			}
		});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	/****************************************
	 * METHODS FOR LOADING AND SAVING CARDS
	 ****************************************/

	// method to save all the cards to a file
	private void saveCards() {

		// make sure that the array has been initialized and contains values
		if (cards != null && cards.size() > 0) {
			// write all the cards to a file
			try {
				ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(
						"shows.dat", 0));
				for (int i = 0; i < cards.size(); i++)
					oos.writeObject(cards.get(i));
				oos.flush();
				oos.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
	}

	// method to load all the cards from a file
	private void loadCards(int numberOfShows) {

		// removing all the views from the scrollview
		scroller.removeAllViews();
		try {
			int numOfShows = prefs.getInt("number_of_shows", 0);
			if (numOfShows > 0) {

				// remove the prompt
				pressButton.setVisibility(View.GONE);

				// Add the header
				scroller.addView(inflater.inflate(R.layout.my_shows, null,
						false));

				// initialize the arraylist of cards
				cards = new ArrayList<Card>();

				// open the object input stream
				ObjectInputStream ois = new ObjectInputStream(
						openFileInput("shows.dat"));

				// Setting up all the shows
				View simpleCard;
				for (int i = 0; i < numOfShows; i++) {

					// adding each card to the array list
					Card c = (Card) ois.readObject();
					cards.add(c);

					// if the episode list hasn't been updated in over a week
					// get all the latest episodes
					if (c.syncCalender && c.wasUpdated) {
						new CalenderAdder().execute(c);
					}

					// setting up the views for each card
					simpleCard = inflater.inflate(R.layout.simple_card, null,
							false);

					// putting in the relevant data into each view
					((TextView) simpleCard
							.findViewById(R.id.name_tv_single_show)).setText(c
							.getTitle());
					((TextView) simpleCard
							.findViewById(R.id.years_tv_single_show)).setText(c
							.getYears());
					((TextView) simpleCard
							.findViewById(R.id.description_single_show))
							.setText(c.getPlot());
					((TextView) simpleCard
							.findViewById(R.id.position_tv_single_card))
							.setText(cards.size() + "");
					// setting the image
					ImageView im = ((ImageView) simpleCard
							.findViewById(R.id.image_single_show));
					im.setVisibility(View.VISIBLE);
					im.setImageBitmap(getBitmap(c.getTitle() + ".jpg"));

					// adding the newly created view to the scrollview
					scroller.addView(simpleCard);
				}

			}
			// if there are no cards make prompt the user to add shows
			else
				pressButton.setVisibility(View.VISIBLE);

		} catch (StreamCorruptedException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
	}

	// method for loading a bitmap from an image saved in memory
	public Bitmap getBitmap(String imageName) {
		try {
			FileInputStream in = openFileInput(imageName);
			return BitmapFactory.decodeStream(in);
		} catch (FileNotFoundException e) {
		}

		return null;
	}

	/*************************
	 * GET USER CALENDARS
	 *************************/

	// method for getting all the calendars saved on the phone
	public String[] getCalenders() {

		// Creating the projection and obtaining a cursor to the internal
		// calendars
		String[] projection = new String[] { Calendars._ID, Calendars.NAME,
				Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE };
		Cursor calCursor = getContentResolver().query(Calendars.CONTENT_URI,
				projection, Calendars.VISIBLE + " = 1", null,
				Calendars._ID + " ASC");

		// initializing the array of calenders
		String[] cals = new String[calCursor.getCount() + 1];
		userCalenders = new CalendarData[calCursor.getCount()];
		if (calCursor.moveToFirst()) {
			int index = 0;
			do {
				// saving the name and calendar id of each calendar
				long id = calCursor.getLong(0);
				String displayName = calCursor.getString(1);
				cals[index] = displayName;
				userCalenders[index++] = new CalendarData(displayName, id);
			} while (calCursor.moveToNext());
		}
		cals[cals.length - 1] = "None";
		return cals;

	}

	/********************
	 * CALENDAR ADDER
	 ********************/

	// ASyncTask to add all the episodes of a show to the users calendar
	public class CalenderAdder extends AsyncTask<Card, Void, Integer> {

		@Override
		protected Integer doInBackground(Card... params) {

			System.out.println(params[0]);

			// get the list of episodes
			ArrayList<Episode> episodes = params[0].getEpisodes();
			boolean addedToCalendar = false;
			Date currentDate = new Date(System.currentTimeMillis());
			if (episodes != null)
				for (Episode episode : episodes) {
					try {
						// get the episodes date
						Date epDate = dateFormat.parse(episode.airDate);
						// if the episode has not already been added to the
						// calendar add it
						if (episode.calenderID != null) {
							CalendarEditor.deleteEvent(getApplicationContext(),
									episode.calenderID);
							episode.calenderID = null;
						}

						if (epDate.compareTo(currentDate) > 0)
							if (episode.calenderID == null) {
								String calID = CalendarEditor.addEvent(
										getApplicationContext(), calendarId,
										params[0].title, episode.title, epDate,
										"Season " + episode.seasonNumber
												+ ": Episode "
												+ episode.episodeNumber);
								episode.setCalenderID(calID);
								addedToCalendar = true;
							}
						params[0].wasUpdated = false;

					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			else {
				return Integer.valueOf(0);
			}

			if (addedToCalendar)
				return Integer.valueOf(1);
			else
				return Integer.valueOf(2);
		}

		@Override
		protected void onPostExecute(Integer result) {
			// Check the result code and make a toast to tell them if the
			// episodes were added
			if (result == 1)
				Toast.makeText(getApplicationContext(), "Added to calendar",
						Toast.LENGTH_SHORT).show();
			else if (result == 0)
				Toast.makeText(getApplicationContext(),
						"This series has no episodes", Toast.LENGTH_SHORT)
						.show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.add_show) {
			Intent searchScreen = new Intent(this, SearchScreen.class);
			startActivity(searchScreen);
		}
		return super.onOptionsItemSelected(item);
	}

}
