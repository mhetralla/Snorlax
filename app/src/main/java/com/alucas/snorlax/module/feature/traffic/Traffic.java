package com.alucas.snorlax.module.feature.traffic;

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
import android.content.res.Resources;

import com.google.protobuf.ByteString;
import com.alucas.snorlax.common.Strings;
import com.alucas.snorlax.common.rx.RxFuncitons;
import com.alucas.snorlax.module.context.pokemongo.PokemonGo;
import com.alucas.snorlax.module.context.snorlax.Snorlax;
import com.alucas.snorlax.module.feature.Feature;
import com.alucas.snorlax.module.feature.mitm.MitmRelay;
import com.alucas.snorlax.module.util.Log;
import com.alucas.snorlax.module.util.Storage;

import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope;
import POGOProtos.Networking.Envelopes.ResponseEnvelopeOuterClass.ResponseEnvelope;
import POGOProtos.Networking.Platform.PlatformRequestTypeOuterClass.PlatformRequestType;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import rx.Observable;
import rx.Subscription;

/**
 * Log Pokemon Go network traffic
 */
public class Traffic implements Feature {
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmssSS", Locale.US);
	private static final String FILE_EXTENSION = "log";

	private final Context mContext;
	private final TrafficPreferences mTrafficPreferences;
	private final MitmRelay mMitmRelay;

	private Subscription mSubscription;
	private File mTrafficDirectory;

	@Inject
	Traffic(@PokemonGo Context context, @Snorlax Resources resources, TrafficPreferences trafficPreferences, MitmRelay mitmRelay) {
		this.mContext = context;
		this.mTrafficPreferences = trafficPreferences;
		this.mMitmRelay = mitmRelay;

		this.mTrafficDirectory = new File(Storage.getPublicDirectory(resources), "traffic");
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mSubscription = mMitmRelay
			.getObservable()
			.compose(mTrafficPreferences.isEnabled())
			.flatMap(envelope -> {
				final RequestEnvelope envelopeRequest = envelope.getRequest();
				final ResponseEnvelope envelopeResponse = envelope.getResponse();

				final List<DataContainer> responses = new LinkedList<>();

				responses.add(new DataContainer(TYPE.ENVELOPE_REQUEST, envelopeRequest.toByteString()));
				responses.add(new DataContainer(TYPE.ENVELOPE_RESPONSE, envelopeResponse.toByteString()));

				final int nbPlatformRequest = envelopeRequest.getPlatformRequestsCount();
				for (int i = 0; i < nbPlatformRequest; i++) {
					final PlatformRequestType platformRequestType = envelopeRequest.getPlatformRequests(i).getType();
					final ByteString platformRequestData = envelopeRequest.getPlatformRequests(i).getRequestMessage();

					responses.add(new DataContainer(TYPE.PLATFORM_REQUEST, i, platformRequestType.toString(), platformRequestData));
				}

				final int nbPlatformResponse = envelopeResponse.getPlatformReturnsCount();
				for (int i = 0; i < nbPlatformResponse; i++) {
					final PlatformRequestType platformResponseType = envelopeResponse.getPlatformReturns(i).getType();
					final ByteString platformResponseData = envelopeResponse.getPlatformReturns(i).getResponse();

					responses.add(new DataContainer(TYPE.PLATFORM_RESPONSE, i, platformResponseType.toString(), platformResponseData));
				}

				final int nbRequest = envelopeRequest.getRequestsCount();
				for (int i = 0; i < nbRequest; i++) {
					final RequestType requestType = envelopeRequest.getRequests(i).getRequestType();
					final ByteString requestData = envelopeRequest.getRequests(i).getRequestMessage();
					final ByteString responseData = envelopeResponse.getReturns(i);

					responses.add(new DataContainer(TYPE.REQUEST, i, requestType.toString(), requestData));
					responses.add(new DataContainer(TYPE.RESPONSE, i, requestType.toString(), responseData));
				}

				return Observable.from(responses);
			})
			.subscribe(this::logData, Log::e);
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscription);
	}

	@SuppressWarnings("unused")
	private void logData(final DataContainer dataContainer) {
		if (!Storage.isExternalStorageWritable(mContext)) {
			return;
		}

		if (!mTrafficDirectory.exists()) {
			final boolean mkdirResult = mTrafficDirectory.mkdirs();
			if (!mkdirResult) {
				Log.d("Failed to create directory : " + mTrafficDirectory.getAbsolutePath());
				return;
			}
		}

		final String formattedDate = DATE_FORMAT.format(Calendar.getInstance().getTime());
		final String logFileName = getFileName(formattedDate, dataContainer);

		final File logFile = new File(mTrafficDirectory, logFileName);

		try (FileOutputStream fos = new FileOutputStream(logFile, true);
			 FileChannel channel = fos.getChannel()
		) {
			channel.write(dataContainer.data.asReadOnlyByteBuffer());
		} catch (IOException e) {
			Log.e(e);
		}
	}

	private String getFileName(final String formattedDate, final DataContainer dataContainer) {
		switch (dataContainer.type) {
			case ENVELOPE_REQUEST:
			case ENVELOPE_RESPONSE:
				return formattedDate + Strings.DOT + dataContainer.type.name() + Strings.DOT + FILE_EXTENSION;
			case PLATFORM_REQUEST:
			case PLATFORM_RESPONSE:
			case REQUEST:
			case RESPONSE:
				return formattedDate + Strings.DOT + dataContainer.type.name() + Strings.DOT + dataContainer.index + Strings.DOT + dataContainer.dataType + Strings.DOT + FILE_EXTENSION;
			default:
		}

		return Strings.EMPTY;
	}

	private enum TYPE {
		ENVELOPE_REQUEST,
		ENVELOPE_RESPONSE,
		PLATFORM_REQUEST,
		PLATFORM_RESPONSE,
		REQUEST,
		RESPONSE
	}

	private class DataContainer {
		final TYPE type;
		final int index;
		final String dataType;
		final ByteString data;

		private DataContainer(final TYPE type, final int index, final String dataType, final ByteString data) {
			this.type = type;
			this.index = index;
			this.dataType = dataType;
			this.data = data;
		}

		private DataContainer(final TYPE type, final ByteString data) {
			this(type, 0, null, data);
		}
	}
}
