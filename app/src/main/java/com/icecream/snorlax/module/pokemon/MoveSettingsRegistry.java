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

package com.icecream.snorlax.module.pokemon;

import java.util.EnumMap;

import com.icecream.snorlax.module.util.Log;

import POGOProtos.Settings.Master.MoveSettingsOuterClass.MoveSettings;

import static POGOProtos.Enums.PokemonMoveOuterClass.PokemonMove;

@SuppressWarnings({"unused", "FieldCanBeLocal", "WeakerAccess"})
public final class MoveSettingsRegistry {

	private static EnumMap<PokemonMove, MoveSettings> sMeta = new EnumMap<>(PokemonMove.class);

	private MoveSettingsRegistry() {
		throw new AssertionError("No instances");
	}

	static MoveSettings getMeta(PokemonMove id) {
		return sMeta.get(id);
	}

	public static void registerMoveSetting(final MoveSettings moveSettings) {
		Log.d("Register move " + moveSettings.getMovementId());
		sMeta.put(moveSettings.getMovementId(), moveSettings);
	}
}
