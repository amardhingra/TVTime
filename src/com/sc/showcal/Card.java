package com.sc.showcal;

import java.io.Serializable;
import java.util.ArrayList;

import android.graphics.Bitmap;

public class Card implements Serializable {

	private static final long serialVersionUID = -232442193805075854L;
	String title;
	String years;
	String plot;
	String network;
	ArrayList<Episode> episodes;
	String posterLink;
	String bannerLink;
	boolean addedToCalendar;
	String TVRageID;
	long offset;
	long runtime;
	transient Bitmap bm;

	public Card(String title, String year, String plot, String network, long offset,
			long runTime, String TVRageID, String poster, String banner) {
		this.title = title;
		this.years = year + "-Present";
		this.plot = plot;
		this.network = network;
		this.runtime = runTime;
		this.offset = offset;
		this.TVRageID = TVRageID;
		this.posterLink = smallPoster(poster);
		this.bannerLink = banner;
	}

	private String smallPoster(String poster) {
		String[] split = poster.split(".jpg");

		return split[0] + "-300.jpg";
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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

	public String getPlot() {
		return plot;
	}

	public void setPlot(String plot) {
		if (plot.length() < 5)
			this.plot = "";
		else
			this.plot = plot;
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
	}

	public ArrayList<Episode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(ArrayList<Episode> eps) {
		this.episodes = eps;
	}

}
