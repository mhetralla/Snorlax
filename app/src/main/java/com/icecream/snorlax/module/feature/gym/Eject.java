package com.icecream.snorlax.module.feature.gym;

import javax.inject.Inject;

import com.icecream.snorlax.common.rx.RxFuncitons;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.pokemon.Pokemon;
import com.icecream.snorlax.module.pokemon.PokemonFactory;
import com.icecream.snorlax.module.util.Log;

import rx.Subscription;

public class Eject implements Feature {
	private static final String LOG_PREFIX = "[" + Eject.class.getSimpleName() + "] ";

	private final EjectPreferences mEjectPreferences;
	private final Gym mGym;
	private final EjectNotification mEjectNotification;
	private final PokemonFactory mPokemonFactory;

	private Subscription mSubscription;

	@Inject
	public Eject(final EjectPreferences ejectPreferences, final Gym gym, final EjectNotification ejectNotification, final PokemonFactory pokemonFactory) {
		mEjectPreferences = ejectPreferences;
		mGym = gym;
		mEjectNotification = ejectNotification;
		mPokemonFactory = pokemonFactory;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();

		mSubscription = mGym
			.getObservable()
			.compose(mEjectPreferences.isEnabled())
			.filter(pair -> pair.first == Gym.ACTION.POKEMON_REMOVE)
			.subscribe(pair -> {
				final Pokemon pokemon = mPokemonFactory.with(pair.second.first);
				final GymData gymData = pair.second.second;
				if (pokemon == null) {
					Log.d(LOG_PREFIX + "Pokemon conversion failed");
					return;
				}

				mEjectNotification.show(
					pokemon.getNumber(),
					pokemon.getName(),
					gymData.name);
			});
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscription);
	}
}
