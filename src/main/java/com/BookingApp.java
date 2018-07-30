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

	private static BookingApp engine = new BookingApp();
	public static Logger LOG;
	private static final int RETRY_ATTEMPT_MAX = 10;
	private static AppProperties p = null;
	private static String dateToBook;

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
		p = AppProperties.getInstance();
		LOG.info("Properties loaded");

		//calculate the date to book
		dateToBook = engine.getBookingDate();

		engine
				.loginAllSessions()
				.beginBookingDaemon()
				.logoutAllSessions()
				.postBooking();

		LOG.info("Booking completed see bookingApp.log for full details");
	}

	private BookingApp loginAllSessions() {
		SessionManager manager = SessionManager.getInstance();

		for (String s : p.getUsersList()) {
			int userPrefix = Integer.parseInt(s);
			final int courtToBook = p.getCourt(userPrefix);
			final String user = p.getUser(userPrefix);
			final String pass = p.getPassword(userPrefix);
			final String name = p.getName(userPrefix);
			//1st session starts at 7am for 30 mins
			//ends at 10am
			for (int session : new int[]{1, 2, 3, 4, 5, 6}) {
				UserSession userSession = loginSession(courtToBook, session, user, pass, name);
				if (userSession != null) {
					manager.getSessions().add(userSession);
				}
			}
		}
		return this;
	}

	private BookingApp postBooking() {
		SessionManager manager = SessionManager.getInstance();
		String ident = System.getenv("COMPUTERNAME");
		StringBuilder sb = new StringBuilder("ID>>>").append(ident).append("\n")
		.append("The following session(s) has been successful:").append("\n");
		manager.getSessions().stream().filter(s -> s.getStatus() == BookingStatus.SUCCESSFUL).forEach(s -> {
			sb.append(s.getBookingInfo() + " - " + s.getStatus().name()).append("\n");
		});
		LOG.info(sb.toString());

		//send successful booking
		SendHTMLEmail.send(sb.toString());
		LOG.info("Email sent");
		return this;
	}

	private String getBookingDate() {
		final String date;
		if (p.getCustomDate() != null) {
			//edge case for testing
			date = p.getCustomDate();
		} else {
			String day = p.getDay() == null ? "" : p.getDay();
			if (day.equalsIgnoreCase("saturday")) {
				date = findDayOfWeekWeek(DayOfWeek.SATURDAY, 2);
			} else if (day.equalsIgnoreCase("sunday")) {
				date = findDayOfWeekWeek(DayOfWeek.SUNDAY, 2);
			} else {
				date = findTheNearestWeekendBookingDate();
			}
		}
		LOG.info("Trying to book " + date);
		return date;
	}

	private BookingApp beginBookingDaemon() {
		final SessionManager manager = SessionManager.getInstance();
		manager.initalise();
		manager.getSessions().stream().forEach(s -> {
			manager.submit(s.getBookingJob());
		});
		return this;
	}

	private BookingApp logoutAllSessions() {
		SessionManager manager = SessionManager.getInstance();
		manager.getTasks().stream().forEach(t -> {
			try {
				t.get();
			} catch (Exception e) {
				//ignore
			}
		});
		manager.getSessions().stream().forEach(s -> s.logout());
		manager.shutdown();
		return this;
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
		if (sat.isAfter(sun)) {
			date = sun;
		} else {
			date = sat;
		}
		LOG.info("nearest booking date is: " + date.getDayOfWeek() + ", " + date.toString());
		return date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
	}


	private static UserSession loginSession(int court, int session,
	                                        String user, String password, String name) {
		int attempt = 0;
		while (attempt < RETRY_ATTEMPT_MAX) {
			try {
				UserSession session1 = UserSession.loginSession(p, user, password, name);
				session1.navigateToCalendar(dateToBook);
				session1.sessionToBook(court, session);
				return session1;
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Booking session failed, retrying " + (++attempt), e);
			}
		}
		LOG.log(Level.SEVERE, "Maximum attempt reached, abort");
		return null;
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
