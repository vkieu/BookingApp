package com;

import com.jcabi.http.Request;
import com.jcabi.http.request.JdkRequest;
import com.jcabi.http.response.RestResponse;
import com.jcabi.http.wire.CookieOptimizingWire;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserSession {
	private static Logger LOG = BookingApp.LOG;

	private static int trackingId = 0;
	private static final Pattern pattern = Pattern.compile("<TD>Member ID:.+<TD align=center>(\\d+)</TD>");
	private static final int MAX_RETRIED = 3;

	private AppProperties p;
	private String user;
	private String name;

	private Cookie cookie;
	private long membershipId;
	private long timer;
	private String allocation;
	private int id;

	private UserSession(AppProperties p, String user, Cookie cookie, long membershipId,  String name) {
		this.p = p;
		this.user = user;
		this.name = name;
		this.cookie = cookie;
		this.membershipId = membershipId;
		this.id = ++trackingId;
		//time to timer
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_WEEK, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		timer = c.getTimeInMillis();//midnight
		this.allocation = SessionManager.getInstance().getAllocationFromPool();
	}

	public static UserSession loginSession(AppProperties p, String user, String password, String name) throws IOException {
		LOG.finer("url: " + p.getLoginUrl());
		LOG.finer("LogonU: " + user);

		long membershipId = 0;
		Cookie cookie = null;

		boolean loggedIn = false;
		while (!loggedIn) {
			RestResponse response = new JdkRequest(p.getLoginUrl())
					.method(Request.POST)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.body()
					.formParam("LogonU", user)
					.formParam("LogonPW", password)
					.formParam("LOGonSubmit", "Logon")
					.back()
					.fetch()
					.as(RestResponse.class);
			String html = response.body();
			LOG.finer("\nLogin>>>\n" + html);

			membershipId = extractMembershipId(html);
			cookie = response.cookie("PHPSESSID");

			if (html.contains("Logoff") && membershipId > 0 && cookie != null) {
				loggedIn = true;
			} else {
				LOG.info("Login failed, retrying");
			}
		}
		LOG.info("Successfully logged in as `" + user + "` membership id: " + membershipId + " with session: " + cookie.getValue());
		return new UserSession(p, user, cookie, membershipId, name);
	}

	private static long extractMembershipId(String html) {
		Matcher m = pattern.matcher(html);
		if (m.find()) {
			try {
				return Long.parseLong(m.group(1));
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private String getCookies() {
		return "PHPSESSID=" + cookie.getValue() + ";MemberID=" + membershipId + "; group=" + p.getPark() + ";";
	}

	public void navigateToCalendar(String date) throws IOException {

		LOG.info("Navigate booking calendar to date: " + date);
		RestResponse response = new JdkRequest(p.getBookingScreenUrl())
				.method("POST")
				.through(CookieOptimizingWire.class)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header(HttpHeaders.COOKIE, getCookies())
				.body()
				.formParam("BtnCalendar", "Calendar")
				.back()
				.fetch()
				.as(RestResponse.class)
				.follow()
				.fetch()
				.as(RestResponse.class)
				.assertStatus(HttpURLConnection.HTTP_OK)//if we can't navigate to the calendar then something is wrong
				;
		LOG.finer("\nBookingScreen>>>\n" + response);

		response = new JdkRequest(p.getBookingCalendarUrl())
				.uri().queryParam("d", date)
				.back()
				.through(CookieOptimizingWire.class)
				.header(HttpHeaders.COOKIE, getCookies())
				.fetch()
				.as(RestResponse.class)
				.follow()
				.fetch()
				.as(RestResponse.class)
				.assertStatus(HttpURLConnection.HTTP_OK)//if we can't navigate to the calendar then something is wrong
		;
		LOG.finer("\nNavigateSelectDate>>>\n" + response);
	}

	public Runnable getBookingJob() {
		return () -> {
			LOG.info("booker info: " + getBookingInfo());
			int retrying = 0;
			while (this.allocation != null) {
				try {
					RestResponse response = new JdkRequest(p.getBookingUrl())
							.uri().queryParam("a", allocation)
							.back()
							.through(CookieOptimizingWire.class)
							.header(HttpHeaders.COOKIE, getCookies())
							.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.92 Safari/537.36")
							.fetch()
							.as(RestResponse.class);
					LOG.finer("\nBooking-request>>>\n" + response);

					BookingStatus status = BookingStatus.FAILED_RETRY;
					if (response.body().toLowerCase().contains("been booked")) {
						status = BookingStatus.FAILED_NEXT;
					} else if (response.body().toLowerCase().contains("error")) {
						status = BookingStatus.FAILED_RETRY;
					} else if (response.body().toLowerCase().contains("accept")) {
						response = new JdkRequest(p.getBookingUrl())
								.method("POST")
								.through(CookieOptimizingWire.class)
								.header("Content-Type", "application/x-www-form-urlencoded")
								.header(HttpHeaders.COOKIE, getCookies())
								.body()
								.formParam("accept", "YES")
								.back()
								.fetch()
								.as(RestResponse.class)
								.follow()
								.fetch()
								.as(RestResponse.class)
						;
						LOG.finer("\nBooking Accept>>>\n" + response);
						//response.assertStatus(HttpURLConnection.HTTP_OK);//might be too busy, just retry anyway
						String html = response.body();
						if (html.toLowerCase().contains("court period activity")) {
							status = BookingStatus.FAILED_RETRY;
						} else if (html.toLowerCase().contains("already booked")) {
							status = BookingStatus.FAILED_NEXT;
						} else if (html.toLowerCase().contains("maximum daily bookings")) {
							status = BookingStatus.FAILED_RETURN;
						} else if (html.toLowerCase().contains("booked")) {
							status = BookingStatus.SUCCESSFUL;
						}
					}
					LOG.finer(status.toString() + response);
					switch (status) {
						case FAILED_ABORT:
							LOG.info(getBookingInfo() + " ? " + status);
							return;
						case SUCCESSFUL:
							LOG.info(getBookingInfo() + " ? " + status);
							SessionManager.getInstance().getSuccessfulSessions().add(name + "-" + this.allocation);
							this.allocation = SessionManager.getInstance().getAllocationFromPool();
							retrying = 0;
							break;
						case FAILED_NEXT:
							LOG.info(getBookingInfo() + " ? " + status);
							this.allocation = SessionManager.getInstance().getAllocationFromPool();
							retrying = 0;
							break;
						case FAILED_RETURN:
							LOG.info(getBookingInfo() + " ? " + status);
							SessionManager.getInstance().returnAllocationToPool(this.allocation);
							return;
						case FAILED_RETRY:
							LOG.info(getBookingInfo() + " ? " + status);
							break;
						default:
					}
					if(this.allocation != null) {
						final long pausePeriod = getPausePeriod();
						if (pausePeriod > 0) {
							Thread.sleep(pausePeriod);
						}
						System.out.print(id + ".");
					}
					//retrying
					if(retrying++ > MAX_RETRIED) {
						LOG.info(getBookingInfo() + " ? " + status + " max retry reached!");
						return;
					}
				} catch (IOException e) {
					//retrying
					LOG.log(Level.SEVERE, "IO Exception, retrying", e);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Error abort", e);
					return;
				}
			}
		};
	}

	public String getBookingInfo() {
		return id + "-User:" + name + " assigned => " + allocation;
	}

	private long getPausePeriod() {
		boolean customDay = p.getCustomDate() != null;
		if (customDay) {
			return 1000L;//short wait
		}
		long now = Calendar.getInstance().getTimeInMillis();
		if (timer - now < (60 * 1000) || now >= timer) {//past midnight
			return 500L;//ram up
		}
		return timer - now;//wait and run after midnight
	}

	public void logout() {
		try {
			//waits for the booking task to complete before LOGging out
			LOG.info("Trying to logout session " + this.cookie.getValue());
			new JdkRequest(p.getLoginUrl())
					.through(CookieOptimizingWire.class)
					.header(HttpHeaders.COOKIE, getCookies())
					.method(Request.POST)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.body()
					.formParam("LogOnOff", "Logoff")
					.back()
					.fetch()
					.as(RestResponse.class)
			//.assertStatus(HttpURLConnection.HTTP_OK)//don't care
			;
			LOG.info("User " + user + " Successfully logoff");
		} catch (IOException ioe) {
			//ignore
			LOG.warning("User " + user + " logoff was not successful");
		}
	}

}

enum BookingStatus {
	FAILED_RETURN, FAILED_NEXT, FAILED_RETRY, FAILED_ABORT, SUCCESSFUL
}
