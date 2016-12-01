package com.alucas.snorlax.module.feature.collect;

import javax.inject.Inject;

import com.alucas.snorlax.common.rx.RxFuncitons;
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
	private final MitmRelay mMitmRelay;

	private Subscription mSubscriber;

	@Inject
	public CollectDefenderBonus(MitmRelay mMitmRelay) {
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
				// create alarm
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
