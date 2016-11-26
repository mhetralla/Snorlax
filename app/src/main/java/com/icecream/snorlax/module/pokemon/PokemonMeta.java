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

import java.util.ArrayList;
import java.util.List;

import POGOProtos.Enums.PokemonTypeOuterClass.PokemonType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static POGOProtos.Enums.PokemonFamilyIdOuterClass.PokemonFamilyId;
import static POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import static POGOProtos.Enums.PokemonMoveOuterClass.PokemonMove;

@Accessors(prefix = "m")
@SuppressWarnings({"unused", "FieldCanBeLocal", "WeakerAccess"})
public final class PokemonMeta {
	/* IDs */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private String mTemplateId;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private String mUniqueId;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private int mNumber;

	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonId mId;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonId mParentId;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonFamilyId mFamily;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonClass mPokemonClass;

	/* Type */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonType mType1;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonType mType2;

	/* Height - Weight */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mPokedexHeightM;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mPokedexWeightKg;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mHeightStdDev;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mWeightStdDev;

	/* IV */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private int mBaseAttack;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private int mBaseDefense;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private int mBaseStamina;

	/* Moves */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonMove[] mQuickMoves;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private PokemonMove[] mCinematicMoves;

	/* Capture Rate - Flee Rate */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mBaseCaptureRate;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mBaseFleeRate;

	/* Capture */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mCylGroundM;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mCylHeightM;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mCylRadiusM;

	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mCollisionHeightM;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mCollisionRadiusM;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mCollisionHeadRadiusM;

	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mDiskRadiusM;

	@Getter
	@Setter(AccessLevel.PACKAGE)
	private MovementType mMovementType;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mMovementTimerS;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private int mAttackTimerS;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mJumpTimeS;

	/* Rendering */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mModelScale;
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private double mShoulderModeScale;

	/* Others */
	@Getter
	@Setter(AccessLevel.PACKAGE)
	private int mCandyToEvolve;

	private List<PokemonId> mChildrenId;

	List<PokemonId> getChildrenId() {
		if (mChildrenId != null) {
			return mChildrenId;
		}

		mChildrenId = new ArrayList<>();
		for (final PokemonId pokemonId : PokemonId.values()) {
			final PokemonMeta pokemonMeta = PokemonMetaRegistry.getMeta(pokemonId);
			if (pokemonMeta == null) {
				continue;
			}

			if (pokemonMeta.mParentId != mId) {
				continue;
			}

			mChildrenId.add(pokemonId);
		}

		if (mChildrenId.isEmpty()) {
			mChildrenId.add(PokemonId.UNRECOGNIZED);
		}

		return mChildrenId;
	}

	PokemonMeta() {
	}

	PokemonMeta(String templateId, String uniqueId, int number, PokemonId id, PokemonId parentId, PokemonFamilyId family, PokemonClass pokemonClass, PokemonType type1, PokemonType type2, double pokedexHeightM, double pokedexWeightKg, double heightStdDev, double weightStdDev, int baseAttack, int baseDefense, int baseStamina, PokemonMove[] quickMoves, PokemonMove[] cinematicMoves, double baseCaptureRate, double baseFleeRate, double cylGroundM, double cylHeightM, double cylRadiusM, double collisionHeightM, double collisionRadiusM, double collisionHeadRadiusM, double diskRadiusM, MovementType movementType, double movementTimerS, int attackTimerS, double jumpTimeS, double modelScale, double shoulderModeScale, int candyToEvolve) {
		this.mTemplateId = templateId;
		this.mUniqueId = uniqueId;
		this.mNumber = number;

		this.mId = id;
		this.mParentId = parentId;
		this.mFamily = family;
		this.mPokemonClass = pokemonClass;

		this.mType1 = type1;
		this.mType2 = type2;

		this.mPokedexHeightM = pokedexHeightM;
		this.mPokedexWeightKg = pokedexWeightKg;
		this.mHeightStdDev = heightStdDev;
		this.mWeightStdDev = weightStdDev;

		this.mBaseAttack = baseAttack;
		this.mBaseDefense = baseDefense;
		this.mBaseStamina = baseStamina;

		this.mQuickMoves = quickMoves;
		this.mCinematicMoves = cinematicMoves;

		this.mBaseCaptureRate = baseCaptureRate;
		this.mBaseFleeRate = baseFleeRate;

		this.mCylGroundM = cylGroundM;
		this.mCylHeightM = cylHeightM;
		this.mCylRadiusM = cylRadiusM;

		this.mCollisionHeightM = collisionHeightM;
		this.mCollisionRadiusM = collisionRadiusM;
		this.mCollisionHeadRadiusM = collisionHeadRadiusM;

		this.mDiskRadiusM = diskRadiusM;

		this.mMovementType = movementType;
		this.mMovementTimerS = movementTimerS;
		this.mAttackTimerS = attackTimerS;
		this.mJumpTimeS = jumpTimeS;

		this.mModelScale = modelScale;
		this.mShoulderModeScale = shoulderModeScale;

		this.mCandyToEvolve = candyToEvolve;
	}
}
