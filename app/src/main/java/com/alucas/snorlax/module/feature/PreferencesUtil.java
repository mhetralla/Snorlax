package com.alucas.snorlax.module.feature;

import android.content.res.Resources;

import de.robv.android.xposed.XSharedPreferences;
import rx.Observable.Transformer;

public class PreferencesUtil {
	private PreferencesUtil() {
	}

	public static <T> Transformer<T, T> isEnabled(final XSharedPreferences preferences, final Resources resources, final int defaultKey, final int key) {
		return observable -> observable
			.doOnNext(t -> preferences.reload())
			.filter(t -> {
				final boolean defaultValue = resources.getBoolean(defaultKey);
				return preferences.getBoolean(resources.getString(key), defaultValue);
			});
	}
}
