/*
 * Copyright (c) 2016. Pedro Diaz <igoticecream@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icecream.snorlax.module.feature.encounter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.NotificationCompat;
import android.view.View;

import com.icecream.snorlax.R;
import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.module.NotificationId;
import com.icecream.snorlax.module.context.pokemongo.PokemonGo;
import com.icecream.snorlax.module.context.snorlax.Snorlax;
import com.icecream.snorlax.module.util.Resource;
import com.icecream.snorlax.module.util.Resource.MODIFIER;

import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Enums.PokemonTypeOuterClass.PokemonType;

@Singleton
final class EncounterNotification {
	private final Context mContext;
	private final Resources mResources;
	private final NotificationManager mNotificationManager;

	@Inject
	EncounterNotification(@Snorlax Context context, @Snorlax Resources resources, @PokemonGo NotificationManager notificationManager) {
		mContext = context;
		mResources = resources;
		mNotificationManager = notificationManager;
	}

	void cancel() {
		mNotificationManager.cancel(NotificationId.ID_ENCOUNTER);
	}

	@SuppressWarnings("deprecation")
	void show(int pokemonNumber, String pokemonName, double iv, int attack, int defense, int stamina, int cp, double level, int hp, double baseWeight, double weight, double baseHeight, double height, String move1, String move1Type, float move1Power, String move2, String move2Type, float move2Power, double fleeRate, double pokeRate, double greatRate, double ultraRate, String type1, String type2, String pokemonClass) {
		final double weightRatio = weight / baseWeight;
		final double heightRatio = height / baseHeight;
		final MODIFIER resourceModifier = (pokemonNumber == PokemonId.PIKACHU_VALUE ? MODIFIER.FAN
			: pokemonNumber == PokemonId.RATTATA_VALUE && heightRatio < 0.80 ? MODIFIER.YOUNGSTER
			: pokemonNumber == PokemonId.MAGIKARP_VALUE && weightRatio > 1.25 ? MODIFIER.FISHERMAN
			: MODIFIER.NO);

		final Map<String, Pair<String, Integer>> symbols = getSymbolReplacementTable();
		new Handler(Looper.getMainLooper()).post(() -> {
			Notification notification = new NotificationCompat.Builder(mContext)
				.setSmallIcon(R.drawable.ic_pokeball)
				.setLargeIcon(Bitmap.createScaledBitmap(
					BitmapFactory.decodeResource(
						mResources,
						Resource.getPokemonResourceId(mContext, mResources, pokemonNumber, resourceModifier)
					),
					Resource.getLargeIconWidth(mResources),
					Resource.getLargeIconHeight(mResources),
					false
				))
				.setContentTitle(EncounterFormat.format(mContext.getString(R.string.notification_title, pokemonName, cp, level), symbols))
				.setContentText(EncounterFormat.format(mContext.getString(R.string.notification_content, iv, fleeRate, pokeRate, greatRate, ultraRate), symbols))
				.setStyle(new NotificationCompat.InboxStyle()
					.addLine(EncounterFormat.format(mContext.getString(R.string.notification_categoty_stats_content_iv, iv, attack, defense, stamina), symbols))
					.addLine(EncounterFormat.format(mContext.getString(R.string.notification_categoty_stats_content_hp, hp, fleeRate), symbols))
					.addLine(EncounterFormat.bold(mContext.getString(R.string.notification_categoty_moves_title)))
					.addLine(EncounterFormat.format(mContext.getString(R.string.notification_categoty_moves_fast, move1, move1Type, move1Power), symbols))
					.addLine(EncounterFormat.format(mContext.getString(R.string.notification_categoty_moves_charge, move2, move2Type, move2Power), symbols))
					.addLine(EncounterFormat.bold(mContext.getString(R.string.notification_categoty_catch_title)))
					.addLine(EncounterFormat.format(mContext.getString(R.string.notification_categoty_catch_content, pokeRate, greatRate, ultraRate), symbols))
					.setSummaryText(getFooter(type1, type2, pokemonClass))
				)
				.setColor(ContextCompat.getColor(mContext, R.color.red_700))
				.setAutoCancel(true)
				.setVibrate(new long[]{0})
				.setPriority(Notification.PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_ALARM)
				.build();

			hideIcon(notification);

			mNotificationManager.notify(NotificationId.ID_ENCOUNTER, notification);
		});
	}

	private Map<String, Pair<String, Integer>> getSymbolReplacementTable() {
		final Map<String, Pair<String, Integer>> symbolTable = new HashMap<>();
		symbolTable.put(mContext.getString(R.string.notification_symbol_pokeball_key), new Pair<>(mContext.getString(R.string.notification_symbol_pokeball_value), ContextCompat.getColor(mContext, R.color.notification_symbol_pokeball_color)));
		symbolTable.put(mContext.getString(R.string.notification_symbol_greatball_key), new Pair<>(mContext.getString(R.string.notification_symbol_greatball_value), ContextCompat.getColor(mContext, R.color.notification_symbol_greatball_color)));
		symbolTable.put(mContext.getString(R.string.notification_symbol_ultraball_key), new Pair<>(mContext.getString(R.string.notification_symbol_ultraball_value), ContextCompat.getColor(mContext, R.color.notification_symbol_ultraball_color)));
		symbolTable.put(mContext.getString(R.string.notification_symbol_flee_key), new Pair<>(mContext.getString(R.string.notification_symbol_flee_value), ContextCompat.getColor(mContext, R.color.notification_symbol_flee_color)));
		symbolTable.put(mContext.getString(R.string.notification_symbol_iv_key), new Pair<>(mContext.getString(R.string.notification_symbol_iv_value), ContextCompat.getColor(mContext, R.color.notification_symbol_iv_color)));
		symbolTable.put(mContext.getString(R.string.notification_symbol_hp_key), new Pair<>(mContext.getString(R.string.notification_symbol_hp_value), ContextCompat.getColor(mContext, R.color.notification_symbol_hp_color)));

		return symbolTable;
	}

	@SuppressWarnings("StringBufferReplaceableByString")
	private String getFooter(String type1, String type2, String pokemonClass) {
		return new StringBuilder()
			.append(type1)
			.append(type2.equalsIgnoreCase(PokemonType.POKEMON_TYPE_NONE.toString()) ? Strings.EMPTY : String.format(Locale.US, "/%s", type2))
			.append(" - ")
			.append(pokemonClass)
			.toString();
	}

	@SuppressWarnings("deprecation")
	private void hideIcon(Notification notification) {
		int iconId = mResources.getIdentifier("right_icon", "id", android.R.class.getPackage().getName());
		if (iconId != 0) {
			if (notification.contentView != null) {
				notification.contentView.setViewVisibility(iconId, View.INVISIBLE);
			}
			if (notification.bigContentView != null) {
				notification.bigContentView.setViewVisibility(iconId, View.INVISIBLE);
			}
		}
	}
}
