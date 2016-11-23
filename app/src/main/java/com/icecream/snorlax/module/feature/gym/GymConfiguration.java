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
import com.icecream.snorlax.module.util.Log;

import rx.Subscription;

@Singleton
public class GymConfiguration {
	private static final String LOG_PREFIX = "[" + GymConfiguration.class.getSimpleName() + "] ";

	private static final String PREF_POKEMON_IN_GYM = "pokemonInGym";

	private final Context mContext;
	private final LongSparseArray<String> mPokemonsInGym;

	private Subscription mSubscription;

	@Inject
	public GymConfiguration(@PokemonGo final Context context) {
		Log.d("[GYM Configuration] New instance");

		this.mContext = context;

		this.clearPokemonInGym(mContext);
		this.mPokemonsInGym = loadPokemonInGym(mContext);
	}

	public void subscribe(final Gym fym) throws Exception {
		mSubscription = fym
			.getObservable()
			.subscribe(pair -> {
				final Pair<Long, String> pokemon = pair.second;
				switch (pair.first) {
					case POKEMON_ADD:
						savePokemonInGym(mContext, pokemon.first, pokemon.second);
						break;
					case POKEMON_REMOVE:
						removePokemonInGym(mContext, pokemon.first);
						break;
					default:
						break;
				}
			});
	}

	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscription);
	}

	private LongSparseArray<String> loadPokemonInGym(final Context context) {
		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final LongSparseArray<String> pokemonsInGym = new LongSparseArray();
		final Map<String, ?> pokemonsInGymRaw = settings.getAll();
		for (final Map.Entry<String, ?> pokemonEntry : pokemonsInGymRaw.entrySet()) {
			try {
				pokemonsInGym.put(Long.decode(pokemonEntry.getKey()), (String) pokemonEntry.getValue());
			} catch (NumberFormatException e) {
				Log.e(e);
			}
		}

		return pokemonsInGym;
	}

	private void clearPokemonInGym(final Context context) {
		mPokemonsInGym.clear();

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		editor.apply();
	}

	private void savePokemonInGym(final Context context, final long pokemonUID, final String gymID) {
		Log.d(LOG_PREFIX + "savePokemonInGym : " + pokemonUID + ", '" + gymID + "'");

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(String.valueOf(pokemonUID), gymID);
		editor.apply();

		mPokemonsInGym.put(pokemonUID, gymID);
	}

	private void removePokemonInGym(final Context context, final long pokemonUID) {
		Log.d(LOG_PREFIX + "removePokemonInGym : " + pokemonUID);

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.remove(String.valueOf(pokemonUID));
		editor.apply();

		mPokemonsInGym.remove(pokemonUID);
	}

	public boolean wasPokemonInGym(final long pokemonUID) {
		return !Strings.isNullOrEmpty(mPokemonsInGym.get(pokemonUID));
	}
}
