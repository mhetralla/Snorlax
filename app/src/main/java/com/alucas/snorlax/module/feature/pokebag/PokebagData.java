package com.alucas.snorlax.module.feature.pokebag;

import java.io.Serializable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import POGOProtos.Data.PokemonDataOuterClass.PokemonData;

@AutoValue
public abstract class PokebagData implements Serializable {
	static PokebagData create(final PokemonData pokemonData) {
		return new AutoValue_PokebagData(
			pokemonData.getId(),
			pokemonData.getPokemonIdValue(),
			pokemonData.getHeightM(),
			pokemonData.getWeightKg());
	}

	public static TypeAdapter<PokebagData> typeAdapter(Gson gson) {
		return new AutoValue_PokebagData.GsonTypeAdapter(gson);
	}

	@SerializedName("pokemonId")
	abstract long pokemonId();

	@SerializedName("pokemonPokedexId")
	abstract int pokemonPokedexId();

	@SerializedName("pokemonHeight")
	abstract float pokemonHeight();

	@SerializedName("pokemonWeight")
	abstract float pokemonWeight();
}
