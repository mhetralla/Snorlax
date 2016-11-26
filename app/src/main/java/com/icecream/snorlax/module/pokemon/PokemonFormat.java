package com.icecream.snorlax.module.pokemon;

import java.util.ArrayList;
import java.util.List;

import com.icecream.snorlax.common.Strings;

import POGOProtos.Enums.PokemonMoveOuterClass.PokemonMove;
import POGOProtos.Enums.PokemonTypeOuterClass.PokemonType;

public class PokemonFormat {
	private static final String SEPARATOR = "_";
	private static final String FAST = "FAST";

	private PokemonFormat() {
		throw new AssertionError("No instances");
	}

	public static String formatType(final PokemonType type) {
		final String typeString = type.toString();
		final int typeStringLastIndex = typeString.lastIndexOf('_');
		final String typeName = typeStringLastIndex != -1 ? typeString.substring(typeStringLastIndex + 1) : typeString;

		return Strings.capitalize(typeName.toLowerCase());
	}

	public static String formatMove(PokemonMove move) {
		List<String> moves = new ArrayList<>();

		for (String string : move.name().split(SEPARATOR)) {
			if (string.equalsIgnoreCase(FAST)) {
				continue;
			}

			moves.add(string);
		}

		return Strings.capitalize(moves.toArray(new String[0]));
	}
}
