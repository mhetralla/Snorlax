package com.icecream.snorlax.module.feature.traffic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import android.content.Context;

import com.google.protobuf.ByteString;
import com.icecream.snorlax.common.rx.RxFuncitons;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.feature.mitm.MitmRelay;
import com.icecream.snorlax.module.util.Log;
import com.icecream.snorlax.module.util.Storage;

import POGOProtos.Networking.Requests.RequestOuterClass.Request;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import rx.Observable;
import rx.Subscription;

/**
 * Log Pokemon Go traffic datas
 */
public class Traffic implements Feature {
	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmssSS", Locale.US);

	private final MitmRelay mMitmRelay;
	private final TrafficPreferences mTrafficPreferences;
	private final Context mContext;

	private Subscription mSubscription;
	private File mTrafficDirectory;

	@Inject
	public Traffic(@PokemonGo Context context, TrafficPreferences trafficPreferences, MitmRelay mitmRelay) {
		this.mMitmRelay = mitmRelay;
		this.mTrafficPreferences = trafficPreferences;
		this.mContext = context;

		this.mTrafficDirectory = new File(Storage.getPublicDirectory(trafficPreferences.getmResources()), "traffic");
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mSubscription = mMitmRelay
			.getObservable()
			.flatMap(envelope -> {
				if (!mTrafficPreferences.isEnabled()) {
					return Observable.empty();
				}

				final List<Request> requests = envelope.getRequest().getRequestsList();
				final List<TypedResponse> responses = new LinkedList<>();
				for (int i = 0; i < requests.size(); i++) {
					final RequestType requestType = requests.get(i).getRequestType();
					final ByteString datas = envelope.getResponse().getReturns(i);

					responses.add(new TypedResponse(i, requestType, datas));
				}

				return Observable.from(responses);
			})
			.subscribe(this::logResponse, Log::e);
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscription);
	}

	private void logResponse(final TypedResponse response) {
		if (!Storage.isExternalStorageWritable(mContext)) {
			return;
		}

		final String logFileName = DATE_FORMAT.format(Calendar.getInstance().getTime()) + "." + response.index + "." + response.requestType + ".log";
		final File logFile = new File(mTrafficDirectory, logFileName);

		if (!mTrafficDirectory.exists()) {
			final boolean mkdirResult = mTrafficDirectory.mkdirs();
			if (!mkdirResult) {
				Log.d("Failed to create directory : " + mTrafficDirectory.getAbsolutePath());
				return;
			}
		}

		try {
			final FileChannel wChannel = new FileOutputStream(logFile, true).getChannel();
			wChannel.write(response.datas.asReadOnlyByteBuffer());
			wChannel.close();
		} catch (IOException e) {
			Log.e(e);
		}
	}

	private class TypedResponse {
		final int index;
		final RequestType requestType;
		final ByteString datas;

		private TypedResponse(final int index, final RequestType requestType, final ByteString datas) {
			this.index = index;
			this.requestType = requestType;
			this.datas = datas;
		}
	}
}
