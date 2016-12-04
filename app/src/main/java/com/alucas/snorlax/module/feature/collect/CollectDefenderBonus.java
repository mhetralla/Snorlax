package com.alucas.snorlax.module.feature.collect;

import javax.inject.Inject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.alucas.snorlax.common.rx.RxFuncitons;
import com.alucas.snorlax.module.context.pokemongo.PokemonGo;
import com.alucas.snorlax.module.feature.Feature;
import com.alucas.snorlax.module.feature.mitm.MitmEnvelope;
import com.alucas.snorlax.module.feature.mitm.MitmRelay;
import com.alucas.snorlax.module.feature.mitm.MitmUtil;
import com.alucas.snorlax.module.util.Log;

import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.CollectDailyDefenderBonusResponseOuterClass.CollectDailyDefenderBonusResponse;
import POGOProtos.Networking.Responses.CollectDailyDefenderBonusResponseOuterClass.CollectDailyDefenderBonusResponse.Result;
import rx.Observable;
import rx.Subscription;

public class CollectDefenderBonus implements Feature {
	private final String ACTION_COLLECT_DEFENDER_BONUS = "com.alucas.snorlax.BROADCAST_COLLECT_DEFENDER_BONUS";

	private final MitmRelay mMitmRelay;

	private final Context mContext;
	private AlarmManager mAlarmManager;
	private PendingIntent alarmIntent;

	private Subscription mSubscriber;

	@Inject
	public CollectDefenderBonus(@PokemonGo final Context context, final AlarmManager alarmManager, final MitmRelay mMitmRelay) {
		this.mContext = context;
		this.mAlarmManager = alarmManager;
		this.mMitmRelay = mMitmRelay;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mSubscriber = mMitmRelay
			.getObservable()
			.compose(getCollectDefenderBonus())
			.filter(response -> response.getResult() == Result.SUCCESS)
			.subscribe(response -> {
				final Intent intent = new Intent(ACTION_COLLECT_DEFENDER_BONUS).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
				alarmIntent = PendingIntent.getBroadcast(mContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 5, alarmIntent);
			});
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscriber);
	}

	private Observable.Transformer<MitmEnvelope, CollectDailyDefenderBonusResponse> getCollectDefenderBonus() {
		return observable -> observable
			.flatMap(MitmUtil.filterResponse(RequestType.COLLECT_DAILY_DEFENDER_BONUS))
			.flatMap(messages -> Observable.fromCallable(() -> CollectDailyDefenderBonusResponse.parseFrom(messages.response)))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty());
	}
}
