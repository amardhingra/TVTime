package com.sc.showcal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SingleShowScreen extends Activity {

	public final static String IMG_SERACH = "http://www.omdbapi.com/?t=";

	SharedPreferences prefs;
	SharedPreferences.Editor editor;

	LayoutInflater inflater;

	LinearLayout scroller;

	Card c;

	boolean doneImage = false;
	boolean doneEpisodes = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scroller);

		// reading the card that was savind into a temp file
		try {
			ObjectInputStream ois = new ObjectInputStream(
					openFileInput("temp.dat"));
			c = (Card) ois.readObject();

		} catch (StreamCorruptedException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}

		// initializing the scroller
		scroller = (LinearLayout) findViewById(R.id.scroller_linear_layout);

		// getting the layout inflater
		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// setting the action bar text
		getActionBar().setTitle(c.title);
		
		// creating a card for the show and adding the relevant data
		View simpleCard = inflater.inflate(R.layout.show_card, null, false);

		((TextView) simpleCard.findViewById(R.id.name_tv_single_show))
				.setText(c.getTitle());
		((TextView) simpleCard.findViewById(R.id.years_tv_single_show))
				.setText(c.getYears());
		((TextView) simpleCard.findViewById(R.id.description_single_show))
				.setText(c.getPlot());
		((TextView) simpleCard.findViewById(R.id.network_tv))
		.setText(c.network);

		// adding the image
		ImageView im = ((ImageView) simpleCard
				.findViewById(R.id.image_single_show));
		im.setVisibility(View.VISIBLE);
		im.setImageBitmap(getBitmap(c.getTitle() + ".jpg"));

		// adding the view to the scrollview
		scroller.addView(simpleCard);

		// adding every episode under the main card
		ArrayList<Episode> episodes = c.getEpisodes();
		View row;
		Episode e;
		if (episodes != null && episodes.size() > 0){
			scroller.addView(inflater.inflate(R.layout.future_episodes, null, false));
			for (int i = 0; i < episodes.size() - 1; i++) {
				row = inflater.inflate(R.layout.episode_row, null, false);

				e = episodes.get(i);
				
				// setting the relevant data
				((TextView) row.findViewById(R.id.episode_info_tv)).setText(e
						.getInfo());
				((TextView) row.findViewById(R.id.episode_name_tv))
						.setText(e.title);
				((TextView) row.findViewById(R.id.episode_airdate_tv))
						.setText(formatDate(e.airDate));
				scroller.addView(row);				
			}
			
			row = inflater.inflate(R.layout.last_episode_row, null, false);

			e = episodes.get(episodes.size() - 1);
			
			// setting the relevant data
			((TextView) row.findViewById(R.id.episode_info_tv)).setText(e
					.getInfo());
			((TextView) row.findViewById(R.id.episode_name_tv))
					.setText(e.title);
			((TextView) row.findViewById(R.id.episode_airdate_tv))
					.setText(formatDate(e.airDate));
			scroller.addView(row);	
			
		}
		else{
			row = inflater.inflate(R.layout.no_more_shows, null, false);
			scroller.addView(row);
		}

	}
	
	/****************************************
	 * METHOD TO CONVERT SAVED DATE TO TEXT
	 ****************************************/

	private String formatDate(String years) {
		String[] splitDate = years.split("-");
		String month = " ";
		if (splitDate[1].equals("01") || splitDate[1].equals("1"))
			month = " January ";
		else if (splitDate[1].equals("02") || splitDate[1].equals("2"))
			month = " February ";
		else if (splitDate[1].equals("03") || splitDate[1].equals("3"))
			month = " March ";
		else if (splitDate[1].equals("04") || splitDate[1].equals("4"))
			month = " April ";
		else if (splitDate[1].equals("05") || splitDate[1].equals("5"))
			month = " May ";
		else if (splitDate[1].equals("06") || splitDate[1].equals("6"))
			month = " June ";
		else if (splitDate[1].equals("07") || splitDate[1].equals("7"))
			month = " July ";
		else if (splitDate[1].equals("08") || splitDate[1].equals("8"))
			month = " August ";
		else if (splitDate[1].equals("09") || splitDate[1].equals("9"))
			month = " September ";
		else if (splitDate[1].equals("10"))
			month = " October ";
		else if (splitDate[1].equals("11"))
			month = " November ";
		else if (splitDate[1].equals("12"))
			month = " December ";
		else
			return splitDate[1] + "-" + splitDate[2] + "-" + splitDate[0];
		return splitDate[2] + month + splitDate[0];
	}
	
	/*************************
	 * METHOD TO LOAD BITMAP
	 *************************/

	public Bitmap getBitmap(String imageName) {
		try {
			FileInputStream in = openFileInput(imageName);
			return BitmapFactory.decodeStream(in);
		} catch (FileNotFoundException e) {
		}

		return null;
	}
	
	/*************************
	 * OPTIONS MENU METHODS
	 *************************/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.single_show_screen, menu);
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
}
