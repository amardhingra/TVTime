package com.sc.showcal;

public class CalendarData {

	public String name;
	public long id;

	public CalendarData(String name, long id) {
		this.name = name;
		this.id = id;
	}

	@Override
	public String toString() {
		return name + " " + id;
	}

}
