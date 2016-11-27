package com.icecream.snorlax.module.feature.gym;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LongSparseArray;
import android.util.Pair;

import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.common.rx.RxFuncitons;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.util.Log;

import POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import rx.Subscription;

@Singleton
public class GymPersistence implements Feature {
	private static final String LOG_PREFIX = "[" + GymPersistence.class.getSimpleName() + "] ";
	private static final String PREF_POKEMON_IN_GYM = "pokemonInGym";

	private final Context mContext;
	private final Gym mGym;
	private final GymManager mGymManager;

	private Subscription mSubscription;

	@Inject
	public GymPersistence(@PokemonGo final Context context, final Gym gym, final GymManager gymManager) {
		this.mContext = context;
		this.mGym = gym;
		this.mGymManager = gymManager;

		gymManager.initPokemonInGym(loadPokemonInGym(mContext));
	}

	@Override
	public void subscribe() {
		mSubscription = mGym
			.getObservable()
			.subscribe(pair -> {
				final Pair<PokemonData, GymData> pokemonInfo = pair.second;
				final PokemonData pokemon = pokemonInfo.first;
				final GymData gymData = pokemonInfo.second;
				switch (pair.first) {
					case POKEMON_ADD:
						savePokemonInGym(mContext, pokemon.getId(), gymData);
						break;
					case POKEMON_REMOVE:
						removePokemonInGym(mContext, pokemon.getId());
						break;
					default:
						break;
				}
			});
	}

	@Override
	public void unsubscribe() {
		RxFuncitons.unsubscribe(mSubscription);
	}

	private LongSparseArray<GymData> loadPokemonInGym(final Context context) {
		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final LongSparseArray<GymData> pokemonsInGym = new LongSparseArray<>();
		final Map<String, ?> pokemonsInGymRaw = settings.getAll();
		for (final Map.Entry<String, ?> pokemonEntry : pokemonsInGymRaw.entrySet()) {
			try {
				pokemonsInGym.put(Long.decode(pokemonEntry.getKey()), new GymData(Strings.EMPTY, (String) pokemonEntry.getValue()));
				Log.d(LOG_PREFIX + "Load : " + pokemonEntry.getKey() + ", " + pokemonEntry.getValue());
			} catch (NumberFormatException e) {
				Log.e(e);
			}
		}

		return pokemonsInGym;
	}

	private void savePokemonInGym(final Context context, final long pokemonUID, final GymData gymData) {
		Log.d(LOG_PREFIX + "savePokemonInGym : " + pokemonUID + ", '" + gymData + "'");

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(String.valueOf(pokemonUID), gymData.name);
		editor.apply();

		mGymManager.savePokemonInGym(pokemonUID, gymData);
	}

	private void removePokemonInGym(final Context context, final long pokemonUID) {
		Log.d(LOG_PREFIX + "removePokemonInGym : " + pokemonUID);

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.remove(String.valueOf(pokemonUID));
		editor.apply();

		mGymManager.removePokemonInGym(pokemonUID);
	}
}
