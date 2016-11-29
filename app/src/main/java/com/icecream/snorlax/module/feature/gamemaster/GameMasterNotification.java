package com.icecream.snorlax.module.feature.gamemaster;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.icecream.snorlax.R;
import com.icecream.snorlax.module.NotificationId;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.context.snorlax.Snorlax;
import com.icecream.snorlax.module.util.Log;

@Singleton
public class GameMasterNotification {
	private final Context mContext;
	private final NotificationManager mNotificationManager;

	@Inject
	public GameMasterNotification(@Snorlax final Context mContext, final @PokemonGo NotificationManager mNotificationManager) {
		this.mContext = mContext;
		this.mNotificationManager = mNotificationManager;
	}

	void show() {
		new Handler(Looper.getMainLooper()).post(() -> {
			try {
				final Notification notification = createNotification();

				mNotificationManager.notify(NotificationId.getUniqueID(), notification);
			} catch (Exception e) {
				Log.e(e);
			}
		});
	}

	private Notification createNotification() {
		return new NotificationCompat.Builder(mContext)
			.setSmallIcon(R.drawable.ic_pokeball)
			.setContentTitle(mContext.getString(R.string.notification_pokemondata_title))
			.setContentText(mContext.getString(R.string.notification_pokemondata_content))
			.setColor(ContextCompat.getColor(mContext, R.color.red_700))
			.setAutoCancel(true)
			.setPriority(Notification.PRIORITY_MAX)
			.setCategory(NotificationCompat.CATEGORY_ALARM)
			.build();
	}
}
