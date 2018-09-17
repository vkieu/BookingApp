package com;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
					bookableAllocations.add(generateAllocation(Integer.parseInt(court), Integer.parseInt(session)));
				}
			}
		}
	}

	private String generateAllocation(int court, int session) {
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
