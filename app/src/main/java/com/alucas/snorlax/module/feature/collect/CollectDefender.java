package com.alucas.snorlax.module.feature.collect;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.alucas.snorlax.common.rx.RxFuncitons;
import com.alucas.snorlax.module.feature.Feature;
import com.alucas.snorlax.module.feature.mitm.MitmEnvelope;
import com.alucas.snorlax.module.feature.mitm.MitmRelay;
import com.alucas.snorlax.module.feature.mitm.MitmUtil;
import com.alucas.snorlax.module.util.Log;

import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.CollectDailyDefenderBonusResponseOuterClass.CollectDailyDefenderBonusResponse;
import POGOProtos.Networking.Responses.CollectDailyDefenderBonusResponseOuterClass.CollectDailyDefenderBonusResponse.Result;
import POGOProtos.Networking.Responses.GetPlayerResponseOuterClass.GetPlayerResponse;
import rx.Observable;
import rx.Subscription;

@Singleton
public class CollectDefender implements Feature {
	private final MitmRelay mMitmRelay;
	private final CollectDefenderPreferences mPreferences;
	private final CollectDefenderNotification mNotification;

	private Subscription mSubCollectBonus;
	private Subscription mSubGetPlayer;

	@Inject
	CollectDefender(final MitmRelay mMitmRelay, final CollectDefenderPreferences preferences, final CollectDefenderNotification notification) {
		this.mMitmRelay = mMitmRelay;
		this.mPreferences = preferences;
		this.mNotification = notification;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mSubCollectBonus = mMitmRelay
			.getObservable()
			.compose(mPreferences.isEnabled())
			.compose(getCollectDefenderBonusResponse())
			.filter(response -> response.getResult() == Result.SUCCESS)
			.subscribe(response -> mNotification.updateAlarm(Calendar.getInstance().getTimeInMillis()), Log::e);

		mSubGetPlayer = mMitmRelay
			.getObservable()
			.compose(mPreferences.isEnabled())
			.compose(getPlayerResponse())
			.flatMap(response -> Observable.just(response.getPlayerData()))
			.flatMap(response -> Observable.just(response.getDailyBonus()))
			.subscribe(response -> mNotification.updateAlarm(response.getNextDefenderBonusCollectTimestampMs()), Log::e);
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubCollectBonus);
		RxFuncitons.unsubscribe(mSubGetPlayer);
	}

	private Observable.Transformer<MitmEnvelope, CollectDailyDefenderBonusResponse> getCollectDefenderBonusResponse() {
		return observable -> observable
			.flatMap(MitmUtil.filterResponse(RequestType.COLLECT_DAILY_DEFENDER_BONUS))
			.flatMap(messages -> Observable.fromCallable(() -> CollectDailyDefenderBonusResponse.parseFrom(messages.response)))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty());
	}

	private Observable.Transformer<MitmEnvelope, GetPlayerResponse> getPlayerResponse() {
		return observable -> observable
			.flatMap(MitmUtil.filterResponse(RequestType.GET_PLAYER))
			.flatMap(messages -> Observable.fromCallable(() -> GetPlayerResponse.parseFrom(messages.response)))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty());
	}
}
