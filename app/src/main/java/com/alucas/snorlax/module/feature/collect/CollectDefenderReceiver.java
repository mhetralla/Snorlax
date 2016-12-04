package com.alucas.snorlax.module.feature.collect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.widget.Toast;

public class CollectDefenderReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Toast.makeText(context, "Receive alarm", Toast.LENGTH_SHORT).show();
	}
}
