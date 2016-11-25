package com.icecream.snorlax.module.feature.gym;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.util.LongSparseArray;

import com.icecream.snorlax.common.Strings;

@Singleton
public class GymManager {
	private LongSparseArray<String> mPokemonsInGym;

	@Inject
	public GymManager() {
		// Must be present because dependency injection
	}

	public void initPokemonInGym(final LongSparseArray<String> pokemons) {
		mPokemonsInGym = pokemons.clone();
	}

	public void removePokemonInGym(final long pokemonUID) {
		mPokemonsInGym.remove(pokemonUID);
	}

	public void savePokemonInGym(final long pokemonUID, String gymID) {
		mPokemonsInGym.put(pokemonUID, gymID);
	}

	public boolean wasPokemonInGym(final Long pokemonUID) {
		return !Strings.isNullOrEmpty(mPokemonsInGym.get(pokemonUID));
	}
}
