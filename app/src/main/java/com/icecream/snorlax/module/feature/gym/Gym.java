package com.icecream.snorlax.module.feature.gym;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import android.util.Pair;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.feature.mitm.MitmRelay;
import com.icecream.snorlax.module.feature.mitm.MitmUtil;
import com.icecream.snorlax.module.pokemon.PokemonFactory;
import com.icecream.snorlax.module.util.Log;

import POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import POGOProtos.Inventory.InventoryDeltaOuterClass.InventoryDelta;
import POGOProtos.Inventory.InventoryItemDataOuterClass.InventoryItemData;
import POGOProtos.Inventory.InventoryItemOuterClass.InventoryItem;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.GetInventoryResponseOuterClass.GetInventoryResponse;
import rx.Observable;

@Singleton
public class Gym implements Feature {
	private final Context mContext;
	private final MitmRelay mMitmRelay;
	private final GymConfiguration mGymConfiguration;
	private final PokemonFactory mPokemonFactory;

	private Observable<Pair<ACTION, Pair<Long, String>>> mObservable;

	@Inject
	public Gym(@PokemonGo final Context context, final MitmRelay mitmRelay, final GymConfiguration gymConfiguration, final PokemonFactory pokemonFactory) {
		Log.d("[GYM] New instance");
		mContext = context;
		mMitmRelay = mitmRelay;
		mGymConfiguration = gymConfiguration;
		mPokemonFactory = pokemonFactory;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mObservable = mMitmRelay
			.getObservable()
			.flatMap(MitmUtil.filterResponse(RequestType.GET_INVENTORY))
			.flatMap(pair -> getPokemons(pair.second))
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

	private Observable<PokemonData> getPokemons(final ByteString bytes) {
		try {
			return getPokemons(GetInventoryResponse.parseFrom(bytes));
		} catch (InvalidProtocolBufferException | NullPointerException e) {
			Log.d("GetInventoryResponse failed: %s", e.getMessage());
			Log.e(e);
		}

		return Observable.empty();
	}

	private Observable<PokemonData> getPokemons(final GetInventoryResponse response) {
		final InventoryDelta inventoryDelta = response.getInventoryDelta();
		if (inventoryDelta == null) {
			return Observable.empty();
		}

		final List<PokemonData> pokemons = new ArrayList<>();
		for (final InventoryItem inventoryItem : inventoryDelta.getInventoryItemsList()) {
			final InventoryItemData itemData = inventoryItem.getInventoryItemData();
			if (itemData == null) {
				continue;
			}

			final PokemonData pokemonData = itemData.getPokemonData();
			if (pokemonData == null) {
				continue;
			}

			pokemons.add(pokemonData);
		}

		return Observable.from(pokemons);
	}

	private Observable<Pair<ACTION, Pair<Long, String>>> getGymAction(final PokemonData pokemonData) {
		final long pokemonUID = pokemonData.getId();
		if (pokemonUID == 0) {
			return Observable.empty();
		}

		final String deployedFortId = pokemonData.getDeployedFortId();
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
