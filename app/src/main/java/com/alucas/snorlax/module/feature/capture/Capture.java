/*
 * Copyright (c) 2016. Pedro Diaz <igoticecream@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alucas.snorlax.module.feature.capture;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.alucas.snorlax.common.rx.RxBus;
import com.alucas.snorlax.common.rx.RxFuncitons;
import com.alucas.snorlax.module.feature.Feature;
import com.alucas.snorlax.module.feature.mitm.MitmEnvelope;
import com.alucas.snorlax.module.feature.mitm.MitmRelay;
import com.alucas.snorlax.module.feature.mitm.MitmUtil;
import com.alucas.snorlax.module.util.Log;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Subscription;

import static POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import static POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse;
import static POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus;

@Singleton
public final class Capture implements Feature {

	private final MitmRelay mMitmRelay;
	private final CapturePreferences mPreferences;
	private final CaptureNotification mCaptureNotification;
	private final RxBus mRxBus;

	private Subscription mSubscription;

	@Inject
	Capture(MitmRelay mitmRelay, CapturePreferences preferences, CaptureNotification captureNotification, RxBus rxBus) {
		mMitmRelay = mitmRelay;
		mPreferences = preferences;
		mCaptureNotification = captureNotification;
		mRxBus = rxBus;
	}

	@Override
	public void subscribe() {
		unsubscribe();

		mSubscription = mMitmRelay
			.getObservable()
			.compose(mPreferences.isEnabled())
			.compose(getCatchPokemon())
			.filter(t -> !CatchStatus.CATCH_MISSED.equals(t.getStatus()))
			.subscribe(this::onCatchPokemon, Log::e)
		;
	}

	@Override
	public void unsubscribe() {
		RxFuncitons.unsubscribe(mSubscription);
	}

	private Transformer<MitmEnvelope, CatchPokemonResponse> getCatchPokemon() {
		return observable -> observable
			.flatMap(MitmUtil.filterResponse(RequestType.CATCH_POKEMON))
			.flatMap(messages -> Observable.fromCallable(() -> CatchPokemonResponse.parseFrom(messages.response)))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty())
			;
	}

	@SuppressWarnings("unused")
	private void onCatchPokemon(final CatchPokemonResponse response) {
		mCaptureNotification.show(formatCapture(response.getStatus().name()));
		mRxBus.post(new CaptureEvent(response.getStatus()));
	}

	private String formatCapture(String status) {
		final StringBuilder builder = new StringBuilder();
		for (String part : status.split("_")) {
			builder
				.append(part.charAt(0))
				.append(part.substring(1).toLowerCase(Locale.US))
				.append(" ");
		}

		return builder.toString().trim();
	}
}
