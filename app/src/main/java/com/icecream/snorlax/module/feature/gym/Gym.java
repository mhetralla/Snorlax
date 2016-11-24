package com.icecream.snorlax.module.feature.gym;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.util.Pair;

import com.google.protobuf.InvalidProtocolBufferException;
import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.feature.mitm.MitmMessages;
import com.icecream.snorlax.module.feature.mitm.MitmRelay;
import com.icecream.snorlax.module.feature.mitm.MitmUtil;
import com.icecream.snorlax.module.util.Log;

import POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import POGOProtos.Inventory.InventoryDeltaOuterClass.InventoryDelta;
import POGOProtos.Inventory.InventoryItemDataOuterClass.InventoryItemData;
import POGOProtos.Inventory.InventoryItemOuterClass.InventoryItem;
import POGOProtos.Networking.Requests.Messages.FortDeployPokemonMessageOuterClass.FortDeployPokemonMessage;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.GetInventoryResponseOuterClass.GetInventoryResponse;
import rx.Observable;

@Singleton
public class Gym implements Feature {
	private static final String LOG_PREFIX = "[" + Gym.class.getSimpleName() + "] ";

	private final MitmRelay mMitmRelay;
	private final GymConfiguration mGymConfiguration;

	private Observable<Pair<ACTION, Pair<Long, String>>> mObservable;

	@Inject
	public Gym(final MitmRelay mitmRelay, final GymConfiguration gymConfiguration) {
		mMitmRelay = mitmRelay;
		mGymConfiguration = gymConfiguration;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mObservable = mMitmRelay
			.getObservable()
			.flatMap(MitmUtil.filterResponse(RequestType.GET_INVENTORY, RequestType.FORT_DEPLOY_POKEMON))
			.flatMap(this::getPokemonsData)
			.flatMap(this::getGymAction)
		;

		mGymConfiguration.subscribe(this);
	}

	@Override
	public void unsubscribe() throws Exception {
		mGymConfiguration.unsubscribe();
	}

	public Observable<Pair<ACTION, Pair<Long, String>>> getObservable() {
		return mObservable;
	}

	private Observable<Pair<Long, String>> getPokemonsData(final MitmMessages messages) {
		switch (messages.requestType) {
			case FORT_DEPLOY_POKEMON:
				try {
					return getPokemonsData(FortDeployPokemonMessage.parseFrom(messages.request));
				} catch (InvalidProtocolBufferException | NullPointerException e) {
					Log.d("FortDeployPokemonMessage / FortDeployPokemonResponse failed: %s", e.getMessage());
					Log.e(e);
				}
				break;
			case GET_INVENTORY: // for Pokemon added while app disabled / not present
				try {
					return getPokemonsData(GetInventoryResponse.parseFrom(messages.response));
				} catch (InvalidProtocolBufferException | NullPointerException e) {
					Log.d("GetInventoryResponse failed: %s", e.getMessage());
					Log.e(e);
				}
				break;
			default:
				break;
		}

		return Observable.empty();
	}

	private Observable<Pair<Long, String>> getPokemonsData(final FortDeployPokemonMessage request) {
		Log.d(LOG_PREFIX + "Pokemon deploy : " + request.getPokemonId());
		return Observable.just(new Pair<>(request.getPokemonId(), request.getFortId()));
	}

	private Observable<Pair<Long, String>> getPokemonsData(final GetInventoryResponse response) {
		final InventoryDelta inventoryDelta = response.getInventoryDelta();
		if (inventoryDelta == null) {
			return Observable.empty();
		}

		final List<Pair<Long, String>> pokemons = new ArrayList<>();
		for (final InventoryItem inventoryItem : inventoryDelta.getInventoryItemsList()) {
			final InventoryItemData itemData = inventoryItem.getInventoryItemData();
			if (itemData == null) {
				Log.d(LOG_PREFIX + "Item Data not found");
				continue;
			}

			final PokemonData pokemonData = itemData.getPokemonData();
			if (pokemonData == null) {
				Log.d(LOG_PREFIX + "Pokemon Data not found");
				continue;
			}

			Log.d(LOG_PREFIX + "Pokemon : " + pokemonData.getPokemonId());
			pokemons.add(new Pair<>(pokemonData.getId(), pokemonData.getDeployedFortId()));
		}

		return Observable.from(pokemons);
	}

	private Observable<Pair<ACTION, Pair<Long, String>>> getGymAction(final Pair<Long, String> pokemonData) {
		final long pokemonUID = pokemonData.first;
		if (pokemonUID == 0) {
			return Observable.empty();
		}

		final String deployedFortId = pokemonData.second;
		if (Strings.isNullOrEmpty(deployedFortId) && mGymConfiguration.wasPokemonInGym(pokemonUID)) {
			return Observable.just(new Pair<>(ACTION.POKEMON_REMOVE, new Pair<>(pokemonUID, deployedFortId)));
		} else if (!Strings.isNullOrEmpty(deployedFortId) && !mGymConfiguration.wasPokemonInGym(pokemonUID)) {
			return Observable.just(new Pair<>(ACTION.POKEMON_ADD, new Pair<>(pokemonUID, deployedFortId)));
		}

		return Observable.empty();
	}

	enum ACTION {
		POKEMON_ADD,
		POKEMON_REMOVE
	}
}
