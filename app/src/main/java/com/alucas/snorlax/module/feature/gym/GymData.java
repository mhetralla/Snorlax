package com.alucas.snorlax.module.feature.gym;

import java.io.Serializable;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

class GymData implements Serializable {
	private static final String GYM_NAME_UNKNOWN = "?";

	public final transient String id;
	@SerializedName("gymName")
	@Nullable
	public final String name;
	@SerializedName("gymLatitude")
	@Nullable
	public final Double latitude;
	@SerializedName("gymLongitude")
	@Nullable
	public final Double longitude;

	GymData(final String id, final String name, final Double latitude, final Double longitude) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	GymData(final String id) {
		this(id, GYM_NAME_UNKNOWN, 0.0, 0.0);
	}
}
