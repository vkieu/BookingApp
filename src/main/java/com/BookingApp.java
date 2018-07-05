package com;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class BookingApp {
	public static Logger LOG;
	private static final int RETRY_ATTEMPT_MAX = 10;

	public static void main(String[] args) throws IOException {
		//config logger
		LOG = Logger.getLogger(BookingApp.class.getName());
		LOG.setUseParentHandlers(false);
		MyFormatter formatter = new MyFormatter();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(formatter);
		LOG.addHandler(handler);
		//
		FileHandler fileHandler = new FileHandler("bookingApp.log", true);
		fileHandler.setFormatter(formatter);
		LOG.addHandler(fileHandler);

		LOG.info("Booking app started");
		AppProperties p = AppProperties.getInstance();

		final String date;
		if (p.getCustomDate() != null) {
			//edge case for testing
			date = p.getCustomDate();
		} else {
			String day = p.getDay() == null ? "" : p.getDay();
			if(day.equalsIgnoreCase("saturday")) {
				date = findDayOfWeekWeek(DayOfWeek.SATURDAY, 2);
			} else if(day.equalsIgnoreCase("sunday")) {
				date = findDayOfWeekWeek(DayOfWeek.SUNDAY, 2);
			} else {
				date = findTheNearestWeekendBookingDate();
			}
		}
		LOG.info("Trying to book " + date);

		for(String s :  p.getUsersList()) {
			int userPrefix = Integer.parseInt(s);
			final int courtToBook = p.getCourt(userPrefix);
			final String user = p.getUser(userPrefix);
			final String pass = p.getPassword(userPrefix);

			bookSessionAsync(p, date, courtToBook, 1, user, pass);//7am for 30 mins
			bookSessionAsync(p, date, courtToBook, 2, user, pass);
			bookSessionAsync(p, date, courtToBook, 3, user, pass);
			bookSessionAsync(p, date, courtToBook, 4, user, pass);
			bookSessionAsync(p, date, courtToBook, 5, user, pass);
			bookSessionAsync(p, date, courtToBook, 6, user, pass);
		}

		//logout all sessions
		UserSession.logoutAllSessions();
		LOG.info("Booking completed see bookingApp.log for full details");
	}

	private static String findDayOfWeekWeek(DayOfWeek dayOfWeek, int week) {
		LocalDate next = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek));
		return next.plusWeeks(week).format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
	}

	private static String findTheNearestWeekendBookingDate() {
		LocalDate date;
		final int weekCount = 2;
		LocalDate sat = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).plusWeeks(weekCount);
		LocalDate sun = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).plusWeeks(weekCount);
		if(sat.isAfter(sun)) {
			date = sun;
		} else {
			date = sat;
		}
		LOG.info("nearest booking date is: " + date.getDayOfWeek() + ", " + date.toString());
		return date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
	}


	private static void bookSessionAsync(AppProperties p, String date, int court, int session,
	                                     String user, String password) {
		int attempt = 0;
		while (attempt < RETRY_ATTEMPT_MAX) {
			try {
				UserSession session1 = UserSession.loginSession(p, user, password);
				session1.navigateToCalendar(date);
				//try to book
				session1.bookAsync(court, session);
				return;
			} catch (Exception e) {
				LOG.log(Level.SEVERE,"Booking session failed, retrying " + (++attempt), e);
			}
		}
		LOG.log(Level.SEVERE,"Maximum attempt reached, abort");
	}

	static class MyFormatter extends Formatter {
		// Create a DateFormat to format the logger timestamp.
		private final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

		public String format(LogRecord record) {
			StringBuilder builder = new StringBuilder(1000);
			builder.append(df.format(new Date(record.getMillis()))).append(" - ");
			builder.append("[").append(record.getLevel()).append("] - ");
			builder.append(formatMessage(record));
			builder.append("\n");
			return builder.toString();
		}
	}
}
