package com.icecream.snorlax.module.feature.gym;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

import com.icecream.snorlax.module.util.Log;

public class GymConfiguration {
	private static final String LOG_PREFIX = "[" + GymConfiguration.class.getSimpleName() + "] ";

	private static final String PREF_POKEMON_IN_GYM = "pokemonInGym";

	private static Map<String, String> mPokemonsInGym = new HashMap<>();

	static void clearPokemonInGym(final Context context) {
		mPokemonsInGym.clear();

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		editor.apply();
	}

	static void initPokemonInGym(final Context context) {
		mPokemonsInGym.clear();

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final Map<String, ?> pokemonsInGym = settings.getAll();
		for (final Map.Entry<String, ?> pokemonEntry : pokemonsInGym.entrySet()) {
			mPokemonsInGym.put(pokemonEntry.getKey(), (String) pokemonEntry.getValue());
		}
	}

	static void savePokemonInGym(final Context context, final long pokemonUID, final String gymID) {
		Log.d(LOG_PREFIX + "savePokemonInGym : " + pokemonUID + ", '" + gymID + "'");

		final String pokemonUIDString = String.valueOf(pokemonUID);

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(pokemonUIDString, gymID);
		editor.apply();

		mPokemonsInGym.put(pokemonUIDString, gymID);
	}

	static void removePokemonInGym(final Context context, final long pokemonUID) {
		Log.d(LOG_PREFIX + "removePokemonInGym : " + pokemonUID);

		final String pokemonUIDString = String.valueOf(pokemonUID);

		final SharedPreferences settings = context.getSharedPreferences(PREF_POKEMON_IN_GYM, 0);
		final SharedPreferences.Editor editor = settings.edit();
		editor.remove(pokemonUIDString);
		editor.apply();

		mPokemonsInGym.remove(pokemonUIDString);
	}

	static boolean wasPokemonInGym(final long pokemonUID) {
		return mPokemonsInGym.containsKey(String.valueOf(pokemonUID));
	}
}
