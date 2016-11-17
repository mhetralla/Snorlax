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

import java.util.List;

import static POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import static POGOProtos.Enums.PokemonIdOuterClass.PokemonId;

@SuppressWarnings({"unused", "FieldCanBeLocal", "WeakerAccess"})
public final class Pokemon {
	public static final int ERROR_LAST_EVOL_CP_NO_EVOL = -1;
	public static final int ERROR_LAST_EVOL_CP_MULTI = -2;

	private final String[] mNames;
	private final PokemonData mPokemonData;

	Pokemon(PokemonData pokemonData, String[] names) {
		mPokemonData = pokemonData;
		mNames = names;
	}

	public float getLevel() {
		if (mPokemonData.getIsEgg()) {
			return 0;
		}

		float level = 1;
		float cpMultiplier = mPokemonData.getCpMultiplier() + mPokemonData.getAdditionalCpMultiplier();

		for (double currentCpM : PokemonCp.CpM) {
			if (Math.abs(cpMultiplier - currentCpM) < 0.0001) {
				return level;
			}
			level += 0.5;
		}
		return level;
	}

	public double getIv() {
		return ((double) (getIVAttack() + getIVDefense() + getIVStamina())) / 45.0;
	}

	public int getBaseAttack() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getBaseAttack();
	}

	public int getIVAttack() {
		return mPokemonData.getIndividualAttack();
	}

	public int getBaseDefence() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getBaseDefense();
	}

	public int getIVDefense() {
		return mPokemonData.getIndividualDefense();
	}

	public int getBaseStamina() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getBaseStamina();
	}

	public int getIVStamina() {
		return mPokemonData.getIndividualStamina();
	}

	public int getCp() {
		return mPokemonData.getCp();
	}

	public int getLastEvolutionCp() {
		final PokemonId currentPokemonId = mPokemonData.getPokemonId();
		PokemonId pokemonId = currentPokemonId;
		while (true) {
			final PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonId);
			final List<PokemonId> childrenId = pokemonMeta.getChildrenId();
			if (childrenId.contains(PokemonId.UNRECOGNIZED)) {
				break;
			}

			if (childrenId.size() > 1) {
				return ERROR_LAST_EVOL_CP_MULTI;
			}

			pokemonId = childrenId.get(0);
		}

		if (pokemonId == currentPokemonId) {
			return ERROR_LAST_EVOL_CP_NO_EVOL;
		}

		final PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonId);
		return PokemonCp.computeCP(
			pokemonMeta.getBaseAttack() + getIVAttack(),
			pokemonMeta.getBaseDefense() + getIVDefense(),
			pokemonMeta.getBaseStamina() + getIVStamina(),
			mPokemonData.getCpMultiplier() + mPokemonData.getAdditionalCpMultiplier()
		);
	}

	public int getHp() {
		return mPokemonData.getStaminaMax();
	}

	public String getName() {
		return mNames[getNumber() - 1];
	}

	public int getNumber() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getNumber();
	}

	public PokemonMoveMeta getMoveFast() {
		return PokemonMoveMetaRegistry.getMeta(mPokemonData.getMove1());
	}

	public PokemonMoveMeta getMoveCharge() {
		return PokemonMoveMetaRegistry.getMeta(mPokemonData.getMove2());
	}

	public PokemonType getType1() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getType1();
	}

	public PokemonType getType2() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getType2();
	}

	public PokemonClass getPokemonClass() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getPokemonClass();
	}

	public double getBaseWeight() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getPokedexWeightKg();
	}

	public double getWeight() {
		return mPokemonData.getWeightKg();
	}

	public double getBaseHeight() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getPokedexHeightM();
	}

	public double getHeight() {
		return mPokemonData.getHeightM();
	}

	public double getFleeRate() {
		return PokemonMetaRegistry.getMeta(mPokemonData.getPokemonId()).getBaseFleeRate();
	}
}
