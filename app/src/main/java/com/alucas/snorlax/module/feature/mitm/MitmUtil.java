package com.alucas.snorlax.module.feature.mitm;

import java.util.Arrays;
import java.util.List;

import com.alucas.snorlax.module.util.Log;

import POGOProtos.Networking.Requests.RequestOuterClass.Request;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import rx.Observable;
import rx.functions.Func1;

public class MitmUtil {
	private static final String LOG_PREFIX = "[" + MitmUtil.class.getCanonicalName() + "]";

	public static Func1<MitmEnvelope, Observable<MitmMessages>> filterResponse(final RequestType... types) {
		final List<RequestType> typeList = Arrays.asList(types);
		return envelope -> {
			final List<Request> requests = envelope.getRequest().getRequestsList();
			if (requests.size() != envelope.getResponse().getReturnsCount()) {
				Log.d(LOG_PREFIX + " :  Not the same number of Requests and Responses");
				return Observable.empty();
			}

			for (int i = 0; i < requests.size(); i++) {
				final RequestType requestType = requests.get(i).getRequestType();

				if (!typeList.contains(requestType)) {
					continue;
				}

				Log.d(LOG_PREFIX + "Accepted packet : " + requestType);
				return Observable.just(new MitmMessages(requestType, requests.get(i).getRequestMessage(), envelope.getResponse().getReturns(i)));
			}

			return Observable.empty();
		};
	}
}
