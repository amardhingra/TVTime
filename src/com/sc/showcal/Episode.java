package com.sc.showcal;

import java.io.Serializable;

public class Episode implements Serializable {

		public Episode(String title, String airDate, int seasonNumber, int episodeNumber){
			this.title = title;
			this.seasonNumber = seasonNumber;
			this.episodeNumber = episodeNumber;
			this.airDate = airDate;
		}
	
		private static final long serialVersionUID = 3050651365030345367L;
		String title;
		String airDate;
		int seasonNumber;
		int episodeNumber;
		String calenderID;
		
		public String toString(){
			return title;
		}

		public void setCalenderID(String calID) {
			this.calenderID = calID;
		}
		
		public String getCalenderID() {
			return calenderID;
		}
		
		public String getInfo(){
			return "Season "+ seasonNumber +": Episode " + episodeNumber;
		}

	}