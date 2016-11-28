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

import POGOProtos.Data.Gym.GymMembershipOuterClass.GymMembership;
import POGOProtos.Data.Gym.GymStateOuterClass.GymState;
import POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Inventory.InventoryDeltaOuterClass.InventoryDelta;
import POGOProtos.Inventory.InventoryItemDataOuterClass.InventoryItemData;
import POGOProtos.Inventory.InventoryItemOuterClass.InventoryItem;
import POGOProtos.Networking.Requests.Messages.FortDeployPokemonMessageOuterClass.FortDeployPokemonMessage;
import POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;
import POGOProtos.Networking.Responses.FortDeployPokemonResponseOuterClass.FortDeployPokemonResponse;
import POGOProtos.Networking.Responses.FortDetailsResponseOuterClass.FortDetailsResponse;
import POGOProtos.Networking.Responses.GetInventoryResponseOuterClass.GetInventoryResponse;
import rx.Observable;

@Singleton
public class Gym implements Feature {
	private static final String LOG_PREFIX = "[" + Gym.class.getSimpleName() + "] ";
	private static final String GYM_NAME_UNKNOWN = "?";

	private final MitmRelay mMitmRelay;
	private final GymManager mGymManager;

	private Observable<Pair<ACTION, Pair<PokemonData, GymData>>> mObservable;

	@Inject
	public Gym(final MitmRelay mitmRelay, final GymManager gymManager) {
		mMitmRelay = mitmRelay;
		mGymManager = gymManager;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mObservable = mMitmRelay
			.getObservable()
			.flatMap(MitmUtil.filterResponse(RequestType.GET_INVENTORY, RequestType.FORT_DEPLOY_POKEMON))
			.flatMap(this::getPokemonsData)
			.flatMap(this::getGymAction)
			.share()
		;
	}

	@Override
	public void unsubscribe() throws Exception {
	}

	public Observable<Pair<ACTION, Pair<PokemonData, GymData>>> getObservable() {
		return mObservable;
	}

	private Observable<Pair<PokemonData, GymData>> getPokemonsData(final MitmMessages messages) {
		switch (messages.requestType) {
			case FORT_DEPLOY_POKEMON:
				try {
					return getPokemonsData(FortDeployPokemonMessage.parseFrom(messages.request), FortDeployPokemonResponse.parseFrom(messages.response));
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

	private Observable<Pair<PokemonData, GymData>> getPokemonsData(final FortDeployPokemonMessage request, final FortDeployPokemonResponse response) {
		Log.d(LOG_PREFIX + "Pokemon deploy : " + request.getPokemonId());

		final GymState gymState = response.getGymState();
		if (gymState == null) {
			Log.d(LOG_PREFIX + "Item Data not found");
			return Observable.empty();
		}

		for (GymMembership membership : gymState.getMembershipsList()) {
			final PokemonData pokemonData = membership.getPokemonData();
			if (pokemonData == null) {
				Log.d(LOG_PREFIX + "Pokemon Data not found");
				continue;
			}

			if (pokemonData.getId() != request.getPokemonId()) {
				continue;
			}

			final FortDetailsResponse gymDetails = response.getFortDetails();
			final String gymName = gymDetails != null ? gymDetails.getName() : GYM_NAME_UNKNOWN;
			final String gymId = gymDetails.getFortId();

			return Observable.just(new Pair<>(pokemonData, new GymData(gymId, gymName)));
		}

		return Observable.empty();
	}

	private Observable<Pair<PokemonData, GymData>> getPokemonsData(final GetInventoryResponse response) {
		final InventoryDelta inventoryDelta = response.getInventoryDelta();
		if (inventoryDelta == null) {
			return Observable.empty();
		}

		final List<Pair<PokemonData, GymData>> pokemons = new ArrayList<>();
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

			if (pokemonData.getPokemonId() == PokemonId.UNRECOGNIZED || pokemonData.getPokemonId() == PokemonId.MISSINGNO) {
				continue;
			}

			pokemons.add(new Pair<>(pokemonData, new GymData(pokemonData.getDeployedFortId(), GYM_NAME_UNKNOWN)));
		}

		return Observable.from(pokemons);
	}

	@SuppressWarnings("unused")
	private Observable<Pair<ACTION, Pair<PokemonData, GymData>>> getGymAction(final Pair<PokemonData, GymData> pokemonInfo) {
		final PokemonData pokemonData = pokemonInfo.first;
		final GymData gymData = pokemonInfo.second;

		if (Strings.isNullOrEmpty(gymData.id) && mGymManager.wasPokemonInGym(pokemonData.getId())) {
			return Observable.just(new Pair<>(ACTION.POKEMON_REMOVE, new Pair<>(pokemonData, mGymManager.getPokemonInGym(pokemonData.getId()))));
		} else if (!Strings.isNullOrEmpty(gymData.id) && !mGymManager.wasPokemonInGym(pokemonData.getId())) {
			return Observable.just(new Pair<>(ACTION.POKEMON_ADD, new Pair<>(pokemonData, gymData)));
		}

		return Observable.empty();
	}

	enum ACTION {
		POKEMON_ADD,
		POKEMON_REMOVE
	}
}
