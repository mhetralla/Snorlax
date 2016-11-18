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

package com.icecream.snorlax.app.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.icecream.snorlax.R;

@SuppressWarnings({"unused", "FieldCanBeLocal", "WeakerAccess"})
public class SummaryEditTextPreference extends EditTextPreference {

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setWidgetLayoutResource(R.layout.preference_edittext_help);
	}

	public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setWidgetLayoutResource(R.layout.preference_edittext_help);
	}

	public SummaryEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWidgetLayoutResource(R.layout.preference_edittext_help);
	}

	public SummaryEditTextPreference(Context context) {
		super(context);
		setWidgetLayoutResource(R.layout.preference_edittext_help);
	}

	@Override
	public CharSequence getSummary() {
		return getText();
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		setSummary(text);
	}

	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		final View buttonHelp = holder.findViewById(R.id.help);
		if (buttonHelp == null) {
			return;
		}

		buttonHelp.setClickable(true);
		buttonHelp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				final Context context = getContext();
				new AlertDialog.Builder(context)
					.setTitle(R.string.format_info)
					.setView(LayoutInflater.from(context).inflate(R.layout.dialog_format, null, false))
					.setPositiveButton(android.R.string.ok, null)
					.setCancelable(true)
					.show();
			}
		});
	}
}
