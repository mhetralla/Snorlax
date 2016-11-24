package com.icecream.snorlax.module.feature.mitm;

import java.util.Arrays;
import java.util.List;

import com.icecream.snorlax.module.util.Log;

import POGOProtos.Networking.Requests.RequestOuterClass;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import rx.Observable;
import rx.functions.Func1;

public class MitmUtil {
	public static Func1<MitmEnvelope, Observable<MitmMessages>> filterResponse(final RequestType... types) {
		final List<RequestType> typeList = Arrays.asList(types);
		return envelope -> {
			final List<RequestOuterClass.Request> requests = envelope.getRequest().getRequestsList();

			for (int i = 0; i < requests.size(); i++) {
				final RequestType requestType = requests.get(i).getRequestType();

				if (!typeList.contains(requestType)) {
					continue;
				}

				Log.d("[MITM Util] Packed accepte : " + requestType);
				return Observable.just(new MitmMessages(requestType, requests.get(i).getRequestMessage(), envelope.getResponse().getReturns(i)));
			}

			return Observable.empty();
		};
	}
}
