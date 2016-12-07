package com.alucas.snorlax.module.feature.collect;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.alucas.snorlax.module.context.pokemongo.PokemonGo;

@Singleton
public class CollectDefenderNotification {
	private static final String ACTION_COLLECT_DEFENDER_BONUS = "com.alucas.snorlax.BROADCAST_COLLECT_DEFENDER_BONUS";

	private final AlarmManager mAlarmManager;
	private final PendingIntent mAlarmPendingIntent;

	@Inject
	CollectDefenderNotification(@PokemonGo final Context context, final AlarmManager alarmManager) {
		this.mAlarmManager = alarmManager;

		final Intent alarmIntent = new Intent(ACTION_COLLECT_DEFENDER_BONUS).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		this.mAlarmPendingIntent = PendingIntent.getBroadcast(context, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	void updateAlarm(final Long nextCollectTimestamp) {
		final long currentTimestamp = Calendar.getInstance().getTimeInMillis();
		if (nextCollectTimestamp <= currentTimestamp) {
			return;
		}

		mAlarmManager.set(AlarmManager.RTC_WAKEUP, nextCollectTimestamp, mAlarmPendingIntent);
	}
}
