package com;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SessionManager {
	private static SessionManager instance = new SessionManager();
	private static ExecutorService executor;
	private static final List<UserSession> sessions = new ArrayList<>();
	private static final List<Future> tasks = new ArrayList<>();

	private SessionManager() {
	}

	public static SessionManager getInstance() {
		return instance;
	}

	public List<UserSession> getSessions() {
		return sessions;
	}

	public void initalise() {
		executor = Executors.newFixedThreadPool(sessions.size());
	}
	public void shutdown() {
		if(executor != null) {
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
