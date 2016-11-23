package com.icecream.snorlax.module.feature.gym;

import javax.inject.Inject;

import com.icecream.snorlax.common.rx.RxFuncitons;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.pokemon.PokemonFactory;

import rx.Subscription;

public class Eject implements Feature {
	private final Gym mGym;
	private final EjectNotification mEjectNotification;
	private final PokemonFactory mPokemonFactory;

	private Subscription mSubscription;

	@Inject
	public Eject(final Gym gym, final EjectNotification ejectNotification, final PokemonFactory pokemonFactory) {
		mGym = gym;
		mEjectNotification = ejectNotification;
		mPokemonFactory = pokemonFactory;
	}

	@Override
	public void subscribe() throws Exception {
		unsubscribe();
	}

	@Override
	public void unsubscribe() throws Exception {
		RxFuncitons.unsubscribe(mSubscription);
	}
}
