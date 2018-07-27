package com;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppProperties {

	private static Logger LOG = BookingApp.LOG;

	private static AppProperties instance = new AppProperties();
	private Properties prop;

	private AppProperties() {
		try {
			LOG.info("Init properties");
			prop = new Properties();
			//load the app.properties
			prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("app.properties"));
			//override with system properties via -D
			prop.putAll(System.getProperties());
			//prop.list(System.out);
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Error loading properties ", e);
		}
	}

	public static AppProperties getInstance() {
		return instance;
	}

	public String getLoginUrl() {
		return prop.getProperty("booking.login.url");
	}

	public String getBookingUrl() {
		return prop.getProperty("booking.booking.url");
	}

	public String getBookingScreenUrl() {
		return prop.getProperty("booking.screen.url");
	}

	public String getBookingCalendarUrl() {
		return prop.getProperty("booking.calendar.url");
	}

	public String getName(int user) {
		return prop.getProperty(user+".name");
	}

	public String getUser(int user) {
		return prop.getProperty(user+".user");
	}

	public String getPassword(int user) {
		return prop.getProperty(user+".password");
	}

	public int getCourt(int user) {
		return Integer.parseInt(prop.getProperty(user + ".court"));
	}

	public int getPark() {
		return Integer.parseInt(prop.getProperty("park"));
	}
	public String getDay() {
		return prop.getProperty("day");
	}
	public String getCustomDate() {
		return prop.getProperty("customDate");
	}
	public String[] getUsersList() {
		String usersList = prop.getProperty("users.list");
		return usersList.split(",");
	}

}
