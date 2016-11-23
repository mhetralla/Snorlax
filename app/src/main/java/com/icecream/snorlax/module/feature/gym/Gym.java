package com.icecream.snorlax.module.feature.gym;

import java.util.List;

import javax.inject.Inject;

import android.content.Context;
import android.support.v4.util.Pair;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.common.rx.RxFuncitons;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.feature.mitm.MitmRelay;
import com.icecream.snorlax.module.pokemon.PokemonFactory;
import com.icecream.snorlax.module.util.Log;

import POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import POGOProtos.Inventory.InventoryDeltaOuterClass.InventoryDelta;
import POGOProtos.Inventory.InventoryItemDataOuterClass.InventoryItemData;
import POGOProtos.Inventory.InventoryItemOuterClass.InventoryItem;
import POGOProtos.Networking.Requests.RequestOuterClass.Request;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.GetInventoryResponseOuterClass.GetInventoryResponse;
import rx.Observable;
import rx.Subscription;

public class Gym implements Feature {
	private final Context mContext;
	private final MitmRelay mMitmRelay;
	private final PokemonFactory mPokemonFactory;

	private Subscription mSubscription;

	@Inject
	public Gym(@PokemonGo final Context context, final MitmRelay mitmRelay, final PokemonFactory pokemonFactory) {
		mContext = context;
		mMitmRelay = mitmRelay;
		mPokemonFactory = pokemonFactory;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		GymConfiguration.clearPokemonInGym(mContext);
		GymConfiguration.initPokemonInGym(mContext);

		mSubscription = mMitmRelay
			.getObservable()
			.flatMap(envelope -> {
				final List<Request> requests = envelope.getRequest().getRequestsList();

				for (int i = 0; i < requests.size(); i++) {
					final RequestType requestType = requests.get(i).getRequestType();

					if (requestType != RequestType.GET_INVENTORY) {
						continue;
					}

					return Observable.just(new Pair<>(requestType, envelope.getResponse().getReturns(i)));
				}

				return Observable.empty();
			})
			.subscribe(pair -> onGetInventoryBytes(pair.second), Log::e);
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscription);
	}

	@SuppressWarnings("unused")
	private void onGetInventoryBytes(final ByteString bytes) {
		try {
			onGetInventory(GetInventoryResponse.parseFrom(bytes));
		} catch (InvalidProtocolBufferException | NullPointerException e) {
			Log.d("GetInventoryResponse failed: %s", e.getMessage());
			Log.e(e);
		}
	}

	private void onGetInventory(final GetInventoryResponse response) {
		final InventoryDelta inventoryDelta = response.getInventoryDelta();
		if (inventoryDelta == null) {
			return;
		}

		for (final InventoryItem inventoryItem : inventoryDelta.getInventoryItemsList()) {
			final InventoryItemData itemData = inventoryItem.getInventoryItemData();
			if (itemData == null) {
				continue;
			}

			final PokemonData pokemonData = itemData.getPokemonData();
			if (pokemonData == null) {
				continue;
			}

			final long pokemonUID = pokemonData.getId();
			if (pokemonUID == 0) {
				continue;
			}

			final String deployedFortId = pokemonData.getDeployedFortId();
			if (Strings.isNullOrEmpty(deployedFortId) && GymConfiguration.wasPokemonInGym(pokemonUID)) {
				GymConfiguration.removePokemonInGym(mContext, pokemonUID);
			} else if (!Strings.isNullOrEmpty(deployedFortId)) {
				GymConfiguration.savePokemonInGym(mContext, pokemonUID, deployedFortId);
			}
		}
	}
}
