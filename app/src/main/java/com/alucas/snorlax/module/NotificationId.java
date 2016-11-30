package com.alucas.snorlax.module;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificationId {
	public static final int ID_ENCOUNTER = 100;
	public static final int ID_MAX = 1000;

	private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(ID_MAX);

	public static int getUniqueID() {
		return ATOMIC_INTEGER.incrementAndGet();
	}
}
