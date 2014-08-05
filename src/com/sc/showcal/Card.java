package com.sc.showcal;

import java.io.Serializable;
import java.util.ArrayList;

import android.graphics.Bitmap;

public class Card implements Serializable {

	private static final long serialVersionUID = -232442193805075854L;
	String title;
	String years;
	String rated;
	String runtime;
	String imdbID;
	String type;
	String plot;
	String[] actors;
	String[] genres;
	ArrayList<Episode> episodes;
	String posterLink;
	int numberOfSeasons;
	long lastUpdated;
	boolean syncCalender;
	boolean wasUpdated;
	String TVRageID;
	transient Bitmap bm;

	public Card(String title, String imdbID, String years, String type) {
		this.title = title;
		this.imdbID = imdbID;
		if (years.length() == 5)
			years += "Present";
		this.years = years;
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImdbID() {
		return imdbID;
	}

	public void setImdbID(String imdbID) {
		this.imdbID = imdbID;
	}

	public String getYears() {
		return years;
	}

	public void setYears(String years) {
		if (years.length() == 5)
			this.years = years + "Present";
		else
			this.years = years;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRated() {
		return rated;
	}

	public void setRated(String rated) {
		this.rated = rated;
	}

	public String getRuntime() {
		return runtime;
	}

	public void setRuntime(String runtime) {
		this.runtime = runtime;
	}

	public String getPlot() {
		return plot;
	}

	public void setPlot(String plot) {
		if (plot.length() < 5)
			this.plot = "";
		else
			this.plot = plot;
	}

	public String[] getActors() {
		return actors;
	}

	public void setActors(String[] actors) {
		this.actors = actors;
	}

	public String[] getGenres() {
		return genres;
	}

	public void setGenres(String[] genres) {
		this.genres = genres;
	}

	public String getPosterLink() {
		return posterLink;
	}

	public void setPosterLink(String posterLink) {
		this.posterLink = posterLink;
	}

	public Bitmap getBitmap() {
		return bm;
	}

	public void setBitmap(Bitmap bm) {
		this.bm = bm;
	}

	public String toString() {
		return title + " " + years;
	}

	public int getNumberOfSeasons() {
		return numberOfSeasons;
	}

	public void setNumberOfSeasons(int numberOfSeasons) {
		this.numberOfSeasons = numberOfSeasons;
	}

	public String getTVRageID() {
		return TVRageID;
	}

	public void setTVRageID(String tVRageID) {
		TVRageID = tVRageID;
	}

	public void addEpisode(Episode e) {
		if (episodes == null)
			episodes = new ArrayList<Episode>();

		episodes.add(e);
		this.lastUpdated = System.currentTimeMillis();
	}

	public ArrayList<Episode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(ArrayList<Episode> eps) {
		this.episodes = eps;
		this.lastUpdated = System.currentTimeMillis();
	}

}
