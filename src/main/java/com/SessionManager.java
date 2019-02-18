package com;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SessionManager {
	private static ExecutorService executor;
	private static final List<UserSession> sessions = new ArrayList<>();
	private static final List<Future> tasks = new ArrayList<>();

	private static BlockingQueue<String> bookableAllocations = new ArrayBlockingQueue(200);
	private static Set<String> successfulSessions = new HashSet<>();
	private static SessionManager instance = new SessionManager();


	private SessionManager() {
		AppProperties p = AppProperties.getInstance();

		//generate bookable-allocations
		final String[] numberOfCourts = p.getCourtsToBook();
		final String[] availableSessions = p.getSessionsToBook();

		for (int passes = 0; passes < 2; passes++) {
			for (String session : availableSessions) {
				for (String court : numberOfCourts) {
					bookableAllocations.add(getCodedAllocation(Integer.parseInt(court), Integer.parseInt(session)));
				}
			}
		}
	}

	public String getDecodedAllocation(String codedAlloction) {
		StringTokenizer tokenizer = new StringTokenizer(codedAlloction, "|");
		if (tokenizer.hasMoreElements()) {
			final String token1 = tokenizer.nextToken();
			final String user = token1.substring(0, token1.indexOf("-"));
			final String courtStr = token1.substring(user.length() + 1);
			int court = 0;
			switch (courtStr) {
				case "30":
					court = 1;
					break;
				case "31":
					court = 2;
					break;
				case "54":
					court = 3;
					break;
				case "55":
					court = 4;
					break;
				case "56":
					court = 5;
					break;
				case "57":
					court = 6;
					break;
				default:
					new UnsupportedOperationException("Unrecognized court slot " + courtStr);
			}

			final String timeSlotStr = tokenizer.nextToken();
			final int session = (Integer.parseInt(timeSlotStr) - 390) / 30;
			String sessionTime = "";
			switch (session) {
				case 1:
					sessionTime = "7am";
					break;
				case 2:
					sessionTime = "7:30am";
					break;
				case 3:
					sessionTime = "8am";
					break;
				case 4:
					sessionTime = "8:30am";
					break;
				case 5:
					sessionTime = "9am";
					break;
				case 6:
					sessionTime = "9:30am";
					break;
				case 7:
					sessionTime = "10am";
					break;
				case 8:
					sessionTime = "10:30am";
					break;
				case 9:
					sessionTime = "11am";
					break;
				default:
					//noep
			}
			return user + " - booked court:" + court + " for " + sessionTime;
		}
		return "";
	}

	private String getCodedAllocation(int court, int session) {
		String courtStr = "";
		switch (court) {
			case 1:
				courtStr = "30";
				break;
			case 2:
				courtStr = "31";
				break;
			case 3:
				courtStr = "54";
				break;
			case 4:
				courtStr = "55";
				break;
			case 5:
				courtStr = "56";
				break;
			case 6:
				courtStr = "57";
				break;
			default:
				new UnsupportedOperationException("Court " + court + " is not supported");
		}
		String sessionStr = String.valueOf(390 + (session * 30));
		return courtStr + "|" + sessionStr + "|73|9|167|";
	}

	public synchronized String getAllocationFromPool() {
		try {
			if (!bookableAllocations.isEmpty()) {
				return bookableAllocations.take();
			}
			return null;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void returnAllocationToPool(String allocation) {
		bookableAllocations.add(allocation);
	}


	public Set<String> getSuccessfulSessions() {
		return successfulSessions;
	}

	public static SessionManager getInstance() {
		return instance;
	}

	public List<UserSession> getSessions() {
		return sessions;
	}

	public void initialise() {
		executor = Executors.newFixedThreadPool(sessions.size());
	}

	public void shutdown() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	public void submit(Runnable r) {
		tasks.add(executor.submit(r));
	}

	public List<Future> getTasks() {
		return tasks;
	}
}
