package com.alucas.snorlax.module.feature.gamemaster;

import java.text.DateFormat;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.alucas.snorlax.R;
import com.alucas.snorlax.module.NotificationId;
import com.alucas.snorlax.module.context.pokemongo.PokemonGo;
import com.alucas.snorlax.module.context.snorlax.Snorlax;
import com.alucas.snorlax.module.util.Log;

@Singleton
public class GameMasterNotification {
	private final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US);
	private final Context mContext;
	private final NotificationManager mNotificationManager;

	@Inject
	public GameMasterNotification(@Snorlax final Context mContext, final @PokemonGo NotificationManager mNotificationManager) {
		this.mContext = mContext;
		this.mNotificationManager = mNotificationManager;
	}

	void show(final long timestampMs) {
		new Handler(Looper.getMainLooper()).post(() -> {
			try {
				final Notification notification = createNotification(timestampMs);

				mNotificationManager.notify(NotificationId.getUniqueID(), notification);
			} catch (Exception e) {
				Log.e(e);
			}
		});
	}

	private Notification createNotification(final long timestampMs) {
		return new NotificationCompat.Builder(mContext)
			.setSmallIcon(R.drawable.ic_pokeball)
			.setContentTitle(mContext.getString(R.string.notification_pokemondata_title))
			.setContentText(mContext.getString(R.string.notification_pokemondata_content, DATE_FORMAT.format(timestampMs)))
			.setColor(ContextCompat.getColor(mContext, R.color.red_700))
			.setAutoCancel(true)
			.setPriority(Notification.PRIORITY_MAX)
			.setCategory(NotificationCompat.CATEGORY_ALARM)
			.build();
	}
}
