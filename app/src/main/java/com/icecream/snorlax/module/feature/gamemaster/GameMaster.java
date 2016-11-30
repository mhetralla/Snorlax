package com.icecream.snorlax.module.feature.gamemaster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import android.content.res.Resources;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.common.rx.RxFuncitons;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.context.snorlax.Snorlax;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.feature.mitm.MitmMessages;
import com.icecream.snorlax.module.feature.mitm.MitmRelay;
import com.icecream.snorlax.module.feature.mitm.MitmUtil;
import com.icecream.snorlax.module.pokemon.MoveSettingsRegistry;
import com.icecream.snorlax.module.pokemon.PokemonSettingsRegistry;
import com.icecream.snorlax.module.util.Log;
import com.icecream.snorlax.module.util.Storage;

import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.DownloadItemTemplatesResponseOuterClass.DownloadItemTemplatesResponse;
import POGOProtos.Networking.Responses.DownloadItemTemplatesResponseOuterClass.DownloadItemTemplatesResponse.ItemTemplate;
import POGOProtos.Networking.Responses.DownloadRemoteConfigVersionResponseOuterClass.DownloadRemoteConfigVersionResponse;
import rx.Subscription;

@Singleton
public class GameMaster implements Feature {
	private static final String LOG_PREFIX = "[" + GameMaster.class.getCanonicalName() + "]";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmssSS", Locale.US);
	private static final String REMOTE_CONFIG_CACHE_PATH = "remote_config_cache";
	private static final String GAME_MASTER_SUFIX = "_GAME_MASTER";

	private final Context mContext;
	private final MitmRelay mMitmRelay;
	private final GameMasterPreferences mPreferences;
	private final GameMasterNotification mNotification;

	private Subscription mSubConfigVersion;
	private Subscription mSubItemTemplates;
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
			.flatMap(MitmUtil.filterResponse(RequestType.DOWNLOAD_REMOTE_CONFIG_VERSION))
			.subscribe(this::onConfigVersionBytes, Log::e);

		mSubItemTemplates = mMitmRelay
			.getObservable()
			.flatMap(MitmUtil.filterResponse(RequestType.DOWNLOAD_ITEM_TEMPLATES))
			.subscribe(this::onItemTemplateBytes, Log::e);
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubConfigVersion);
		RxFuncitons.unsubscribe(mSubItemTemplates);
	}

	private void onConfigVersionBytes(final MitmMessages messages) {
		final ByteString responseBytes = messages.response;
		try {
			onConfigVersion(DownloadRemoteConfigVersionResponse.parseFrom(responseBytes));
		} catch (InvalidProtocolBufferException | NullPointerException e) {
			Log.d("DownloadItemTemplatesResponse failed: %s", e.getMessage());
			Log.e(e);
		}
	}

	private void onConfigVersion(final DownloadRemoteConfigVersionResponse response) {
		final File gameMasterFile = getValidGameMaster(response.getItemTemplatesTimestampMs());
		if (gameMasterFile == null) {
			Log.d(LOG_PREFIX + "GameMaster not found");
			return;
		}

		try (
			FileInputStream fis = new FileInputStream(gameMasterFile);
			FileChannel channel = fis.getChannel()
		) {
			final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
			buffer.load();

			decodeItemTemplate(DownloadItemTemplatesResponse.parseFrom(ByteString.copyFrom(buffer)));
		} catch (NullPointerException | IOException e) {
			Log.d("InvalidProtocolBufferException failed: %s", e.getMessage());
			Log.e(e);
		}
	}

	private File getValidGameMaster(final long lastTimestamp) {
		final File filesDir = mContext.getExternalFilesDir(null);
		final File remoteConfigCacheDir = new File(filesDir, REMOTE_CONFIG_CACHE_PATH);
		final File[] gameMasterArray = remoteConfigCacheDir.listFiles((dir, name) -> name.endsWith(GAME_MASTER_SUFIX));
		if (gameMasterArray == null) {
			return null;
		}

		final List<File> gameMasterFiles = Arrays.asList(gameMasterArray);

		File lastGameMasterFile = null;
		long lastGameMasterTimestamp = 0;
		for (File gameMasterFile : gameMasterFiles) {
			final String hexTimeStamp = "0x" + gameMasterFile.getName().replace(GAME_MASTER_SUFIX, Strings.EMPTY);
			try {
				final long timestamp = Long.decode(hexTimeStamp);

				if (timestamp > lastGameMasterTimestamp) {
					lastGameMasterTimestamp = timestamp;
					lastGameMasterFile = gameMasterFile;
				}
			} catch (NumberFormatException e) {
				Log.e(e);
			}
		}

		if (lastGameMasterTimestamp < lastTimestamp) {
			return null;
		}

		return lastGameMasterFile;
	}

	private void onItemTemplateBytes(final MitmMessages messages) {
		final ByteString responseBytes = messages.response;
		try {
			decodeItemTemplate(DownloadItemTemplatesResponse.parseFrom(responseBytes));
		} catch (InvalidProtocolBufferException | NullPointerException e) {
			Log.d("DownloadItemTemplatesResponse failed: %s", e.getMessage());
			Log.e(e);
		}


		if (mPreferences.isEnabled()) {
			doBackup(responseBytes);

			mNotification.show();
		}
	}

	private void doBackup(final ByteString gameMasterBytes) {
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
		final File logFile = new File(mGameMasterDir, formattedDate + "_GAME_MASTER.raw");

		try (FileOutputStream fos = new FileOutputStream(logFile, true);
			 FileChannel channel = fos.getChannel()
		) {
			channel.write(gameMasterBytes.asReadOnlyByteBuffer());
		} catch (IOException e) {
			Log.e(e);
		}
	}

	private long decodeItemTemplate(final DownloadItemTemplatesResponse response) {
		for (final ItemTemplate itemTemplate : response.getItemTemplatesList()) {
			if (itemTemplate.hasMoveSettings()) {
				MoveSettingsRegistry.registerMoveSetting(itemTemplate.getMoveSettings());
			}

			if (itemTemplate.hasPokemonSettings()) {
				PokemonSettingsRegistry.registerPokemonSetting(itemTemplate.getPokemonSettings());
			}
		}

		return response.getTimestampMs();
	}
}
