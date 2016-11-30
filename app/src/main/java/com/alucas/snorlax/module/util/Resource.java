package com.alucas.snorlax.module.util;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;

import com.alucas.snorlax.common.Strings;

public class Resource {
	@DrawableRes
	public static int getPokemonResourceId(final Context context, final Resources resources, final int pokemonNumber) {
		return getPokemonResourceId(context, resources, pokemonNumber, MODIFIER.NO);
	}

	@DrawableRes
	public static int getPokemonResourceId(final Context context, final Resources resources, final int pokemonNumber, final Resource.MODIFIER modifier) {
		return resources.getIdentifier("pokemon_" + Strings.padStart(String.valueOf(pokemonNumber), 3, '0') + modifier, "drawable", context.getPackageName());
	}

	public static int getLargeIconWidth(final Resources resources) {
		return resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
	}

	public static int getLargeIconHeight(final Resources resources) {
		return resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
	}

	public enum MODIFIER {
		NO(""),
		FISHERMAN("_fisherman"),
		YOUNGSTER("_youngster"),
		FAN("_fan");

		private final String name;

		MODIFIER(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
