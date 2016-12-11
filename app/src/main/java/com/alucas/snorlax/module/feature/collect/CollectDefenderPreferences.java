package com.alucas.snorlax.module.feature.collect;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.content.res.Resources;

import com.alucas.snorlax.R;
import com.alucas.snorlax.module.context.snorlax.Snorlax;
import com.alucas.snorlax.module.feature.PreferencesUtil;

import de.robv.android.xposed.XSharedPreferences;
import rx.Observable;

@Singleton
final class CollectDefenderPreferences {
	private final Resources mResources;
	private final XSharedPreferences mPreferences;

	@Inject
	CollectDefenderPreferences(@Snorlax final Resources mResources, final XSharedPreferences mPreferences) {
		this.mResources = mResources;
		this.mPreferences = mPreferences;
	}

	<T> Observable.Transformer<T, T> isEnabled() {
		return PreferencesUtil.isEnabled(mPreferences, mResources, R.bool.preference_collect_defender_enable_default, R.string.preference_collect_defender_enable_key);
	}
}
