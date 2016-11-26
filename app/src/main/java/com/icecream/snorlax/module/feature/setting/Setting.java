package com.icecream.snorlax.module.feature.setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.common.rx.RxFuncitons;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.feature.mitm.MitmMessages;
import com.icecream.snorlax.module.feature.mitm.MitmRelay;
import com.icecream.snorlax.module.feature.mitm.MitmUtil;
import com.icecream.snorlax.module.pokemon.MoveSettingsRegistry;
import com.icecream.snorlax.module.pokemon.PokemonSettingsRegistry;
import com.icecream.snorlax.module.util.Log;

import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.DownloadItemTemplatesResponseOuterClass.DownloadItemTemplatesResponse;
import POGOProtos.Networking.Responses.DownloadItemTemplatesResponseOuterClass.DownloadItemTemplatesResponse.ItemTemplate;
import POGOProtos.Networking.Responses.DownloadRemoteConfigVersionResponseOuterClass.DownloadRemoteConfigVersionResponse;
import rx.Subscription;

@Singleton
public class Setting implements Feature {
	private static final String LOG_PREFIX = "[" + Setting.class.getCanonicalName() + "]";
	private static final String REMOTE_CONFIG_CACHE_PATH = "remote_config_cache";
	private static final String GAME_MASTER_SUFIX = "_GAME_MASTER";

	private final Context mContext;
	private final MitmRelay mMitmRelay;

	private Subscription mSubscription;

	@Inject
	public Setting(@PokemonGo Context context, final MitmRelay mitmRelay) {
		this.mContext = context;
		this.mMitmRelay = mitmRelay;
	}

	@Override
	public void subscribe() throws Exception {
		mSubscription = mMitmRelay
			.getObservable()
			.flatMap(MitmUtil.filterResponse(RequestType.DOWNLOAD_REMOTE_CONFIG_VERSION, RequestType.DOWNLOAD_ITEM_TEMPLATES))
			.subscribe(messages -> {
				switch (messages.requestType) {
					case DOWNLOAD_REMOTE_CONFIG_VERSION:
						onConfigVersionBytes(messages);
						break;
					case DOWNLOAD_ITEM_TEMPLATES:
						onItemTemplateBytes(messages);
						break;
					default:
						break;
				}
			}, Log::e);
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscription);
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
		final File[] gameMasterArray = remoteConfigCacheDir.listFiles((dir, name) -> {
			return name.endsWith(GAME_MASTER_SUFIX);
		});
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
	}

	private void decodeItemTemplate(final DownloadItemTemplatesResponse response) {
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
