package com.alucas.snorlax.app.receiver;

import javax.inject.Inject;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.alucas.snorlax.R;
import com.alucas.snorlax.app.SnorlaxApp;
import com.alucas.snorlax.module.NotificationId;

public class CollectDefenderReceiver extends BroadcastReceiver {
	@Inject
	@SuppressWarnings("squid:S3306")
	public Resources mResources;
	@Inject
	@SuppressWarnings("squid:S3306")
	public NotificationManager mNotificationManager;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		((SnorlaxApp) context.getApplicationContext()).getComponent().inject(this);

		createNotification(context);
	}

	private void createNotification(final Context context) {
		new Handler(Looper.getMainLooper()).post(() -> {
			Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_pokeball)
				.setContentTitle(mResources.getString(R.string.notification_collect_defender_title))
				.setContentText(mResources.getString(R.string.notification_collect_defender_description))
				.setColor(ContextCompat.getColor(context, R.color.red_700))
				.setAutoCancel(true)
				.setVibrate(new long[]{0, 1000})
				.setPriority(Notification.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.build();

			mNotificationManager.notify(NotificationId.ID_COLLECT_DEFENDER_BONUS, notification);
		});
	}
}
