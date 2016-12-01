package com.alucas.snorlax.module.feature.gamemaster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.util.Pair;

import com.alucas.snorlax.common.Files;
import com.alucas.snorlax.common.Strings;
import com.alucas.snorlax.common.rx.RxFuncitons;
import com.alucas.snorlax.module.context.pokemongo.PokemonGo;
import com.alucas.snorlax.module.context.snorlax.Snorlax;
import com.alucas.snorlax.module.feature.Feature;
import com.alucas.snorlax.module.feature.mitm.MitmEnvelope;
import com.alucas.snorlax.module.feature.mitm.MitmRelay;
import com.alucas.snorlax.module.feature.mitm.MitmUtil;
import com.alucas.snorlax.module.pokemon.MoveSettingsRegistry;
import com.alucas.snorlax.module.pokemon.PokemonSettingsRegistry;
import com.alucas.snorlax.module.util.Log;
import com.alucas.snorlax.module.util.Storage;
import com.google.protobuf.ByteString;

import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.DownloadItemTemplatesResponseOuterClass.DownloadItemTemplatesResponse;
import POGOProtos.Networking.Responses.DownloadItemTemplatesResponseOuterClass.DownloadItemTemplatesResponse.ItemTemplate;
import POGOProtos.Networking.Responses.DownloadRemoteConfigVersionResponseOuterClass.DownloadRemoteConfigVersionResponse;
import rx.Observable;
import rx.Subscription;

@Singleton
public class GameMaster implements Feature {
	private static final String LOG_PREFIX = "[" + GameMaster.class.getCanonicalName() + "]";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmssSS", Locale.US);
	private static final String REMOTE_CONFIG_CACHE_PATH = "remote_config_cache";
	private static final String GAME_MASTER_SUFFIX = "_GAME_MASTER";

	private final Context mContext;
	private final MitmRelay mMitmRelay;
	private final GameMasterPreferences mPreferences;
	private final GameMasterNotification mNotification;

	private Subscription mSubConfigVersion;
	private Subscription mSubItemTemplates;
	private Subscription mSubItemTemplatesNotification;
	private File mGameMasterDir;

	@Inject
	public GameMaster(@PokemonGo final Context context, @Snorlax final Resources resources, final MitmRelay mitmRelay, final GameMasterPreferences preferences, final GameMasterNotification notification) {
		this.mContext = context;
		this.mMitmRelay = mitmRelay;
		this.mPreferences = preferences;
		this.mNotification = notification;

		this.mGameMasterDir = new File(Storage.getPublicDirectory(resources), "game_master");
	}

	@Override
	public void subscribe() throws Exception {
		mSubConfigVersion = mMitmRelay
			.getObservable()
			.compose(getConfigVersionResponse())
			.compose(onConfigVersionResponse())
			.subscribe(this::decodeItemTemplate, Log::e)
		;

		mSubItemTemplates = mMitmRelay
			.getObservable()
			.compose(getItemTemplatesResponse())
			.subscribe(response -> decodeItemTemplate(response.second), Log::e)
		;

		mSubItemTemplatesNotification = mMitmRelay
			.getObservable()
			.compose(getItemTemplatesResponse())
			.compose(mPreferences.isEnabled())
			.subscribe(response -> {
				final long timestampMs = response.second.getTimestampMs();
				doBackup(response.first, timestampMs);
				mNotification.show(timestampMs);
			}, Log::e)
		;
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubConfigVersion);
		RxFuncitons.unsubscribe(mSubItemTemplates);
		RxFuncitons.unsubscribe(mSubItemTemplatesNotification);
	}

	private Observable.Transformer<MitmEnvelope, DownloadRemoteConfigVersionResponse> getConfigVersionResponse() {
		return observable -> observable
			.flatMap(MitmUtil.filterResponse(RequestType.DOWNLOAD_REMOTE_CONFIG_VERSION))
			.flatMap(messages -> Observable.fromCallable(() -> DownloadRemoteConfigVersionResponse.parseFrom(messages.response)))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty())
			;
	}

	private Observable.Transformer<DownloadRemoteConfigVersionResponse, DownloadItemTemplatesResponse> onConfigVersionResponse() {
		return observable -> observable
			.flatMap(response -> getLastGameMasterFiles()
				.filter(gameFile -> gameFile.first >= response.getItemTemplatesTimestampMs())
				.flatMap(pair -> Observable.just(pair.second))
			)
			.flatMap(Files::loadFileToByteString)
			.flatMap(gameBytes -> Observable.fromCallable(() -> DownloadItemTemplatesResponse.parseFrom(gameBytes)))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty())
			;
	}

	private Observable<Pair<Long, File>> getLastGameMasterFiles() {
		return Observable.just(mContext.getExternalFilesDir(null))
			.flatMap(filesDir -> Observable.just(new File(filesDir, REMOTE_CONFIG_CACHE_PATH)))
			.flatMap(cacheDir -> Observable.from(cacheDir.listFiles((dir, name) -> name.endsWith(GAME_MASTER_SUFFIX))))
			.flatMap(gameFile -> Observable.just(new Pair<>(extractTimestamp(gameFile), gameFile)))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty())
			.reduce((f1, f2) -> f1.first > f2.first ? f1 : f2)
			;
	}

	private Long extractTimestamp(final File gameMasterFile) {
		return Long.decode("0x" + gameMasterFile.getName().replace(GAME_MASTER_SUFFIX, Strings.EMPTY));
	}

	private Observable.Transformer<MitmEnvelope, Pair<ByteString, DownloadItemTemplatesResponse>> getItemTemplatesResponse() {
		return observable -> observable
			.flatMap(MitmUtil.filterResponse(RequestType.DOWNLOAD_ITEM_TEMPLATES))
			.flatMap(messages -> Observable.fromCallable(() -> new Pair<>(messages.response, DownloadItemTemplatesResponse.parseFrom(messages.response))))
			.doOnError(Log::e)
			.onErrorResumeNext(Observable.empty())
			;
	}

	@SuppressWarnings("unused")
	private void doBackup(final ByteString gameMasterBytes, final long timestampMs) {
		if (!Storage.isExternalStorageWritable(mContext)) {
			return;
		}

		if (!mGameMasterDir.exists()) {
			final boolean mkdirResult = mGameMasterDir.mkdirs();
			if (!mkdirResult) {
				Log.d("Failed to create directory : " + mGameMasterDir.getAbsolutePath());
				return;
			}
		}

		final String formattedDate = DATE_FORMAT.format(Calendar.getInstance().getTime());
		final File logFile = new File(mGameMasterDir, formattedDate + "_" + Long.toString(timestampMs) + GAME_MASTER_SUFFIX + ".raw");

		try (FileOutputStream fos = new FileOutputStream(logFile, true);
			 FileChannel channel = fos.getChannel()
		) {
			channel.write(gameMasterBytes.asReadOnlyByteBuffer());
		} catch (IOException e) {
			Log.e(e);
		}
	}

	private void decodeItemTemplate(final DownloadItemTemplatesResponse response) {
		Log.d(LOG_PREFIX + "Decode item template");
		for (final ItemTemplate itemTemplate : response.getItemTemplatesList()) {
			if (itemTemplate.hasMoveSettings()) {
				MoveSettingsRegistry.registerMoveSetting(itemTemplate.getMoveSettings());
			}

			if (itemTemplate.hasPokemonSettings()) {
				PokemonSettingsRegistry.registerPokemonSetting(itemTemplate.getPokemonSettings());
			}
		}
	}
}
