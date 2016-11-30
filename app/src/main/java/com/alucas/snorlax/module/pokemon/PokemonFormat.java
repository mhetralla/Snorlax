package com.alucas.snorlax.module.pokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.alucas.snorlax.common.Strings;

import POGOProtos.Enums.PokemonMoveOuterClass.PokemonMove;
import POGOProtos.Enums.PokemonRarityOuterClass.PokemonRarity;
import POGOProtos.Enums.PokemonTypeOuterClass.PokemonType;

public class PokemonFormat {
	private static final String SEPARATOR = "_";
	private static final String FAST_PREFIX = "FAST";
	private static final String POKEMON_RARITY_PREFIX = "POKEMON_RARITY_";

	private PokemonFormat() {
		throw new AssertionError("No instances");
	}

	public static String formatType(final PokemonType type) {
		final String typeString = type.toString();
		final int typeStringLastIndex = typeString.lastIndexOf('_');
		final String typeName = typeStringLastIndex != -1 ? typeString.substring(typeStringLastIndex + 1) : typeString;

		return Strings.capitalize(typeName.toLowerCase(Locale.US));
	}

	public static String formatMove(final PokemonMove move) {
		final List<String> moves = new ArrayList<>();

		for (String string : move.name().split(SEPARATOR)) {
			if (string.equalsIgnoreCase(FAST_PREFIX)) {
				continue;
			}

			moves.add(string);
		}

		return Strings.capitalize(moves.toArray(new String[0]));
	}

	public static String formatRarity(final PokemonRarity pokemonRarity) {
		return Strings.capitalize(pokemonRarity.toString().replace(POKEMON_RARITY_PREFIX, Strings.EMPTY).toLowerCase());
	}
}
