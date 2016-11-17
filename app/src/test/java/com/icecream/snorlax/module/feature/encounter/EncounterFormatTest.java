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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.support.v4.util.Pair;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EncounterFormatTest {
	private static final int COLOR_POKEBALL = 0xFFF44336;
	private static final int COLOR_GREATBALL = 0xFF3F51B5;
	private static final int COLOR_ULTRABALL = 0xFFFFC107;
	private static final String SYMBOL_POKEBALL = "pokeball";
	private static final String SYMBOL_GREATBALL = "greatball";
	private static final String SYMBOL_ULTRABALL = "ultraball";
	private static final Map<String, Pair<String, Integer>> SYMBOLS;

	static {
		SYMBOLS = new HashMap<>();
		SYMBOLS.put(SYMBOL_POKEBALL, new Pair<>("⛑", COLOR_POKEBALL));
		SYMBOLS.put(SYMBOL_GREATBALL, new Pair<>("⛑", COLOR_GREATBALL));
		SYMBOLS.put(SYMBOL_ULTRABALL, new Pair<>("⛑", COLOR_ULTRABALL));
	}

	@Before
	public void setUp() throws Exception {
		// Given
	}

	@After
	public void tearDown() throws Exception {
		// When
	}

	//region Command processing
	@Test
	public void testReplaceSymbols() throws Exception {
		final String text = "{pokeball}{greatball} - {ultraball}{bullshit}";

		final String expected = "⛑⛑ - ⛑{bullshit}";

		final List<EncounterFormat.ColorPos> colorPositions = new ArrayList<>();
		final String output = EncounterFormat.replaceSymbol(text, SYMBOLS, colorPositions);

		MatcherAssert.assertThat(output, Matchers.is(expected));

		MatcherAssert.assertThat(colorPositions.size(), Matchers.is(3));

		MatcherAssert.assertThat(colorPositions.get(0).color, Matchers.is(COLOR_POKEBALL));
		MatcherAssert.assertThat(colorPositions.get(0).start, Matchers.is(0));
		MatcherAssert.assertThat(colorPositions.get(0).end, Matchers.is(1));

		MatcherAssert.assertThat(colorPositions.get(1).color, Matchers.is(COLOR_GREATBALL));
		MatcherAssert.assertThat(colorPositions.get(1).start, Matchers.is(1));
		MatcherAssert.assertThat(colorPositions.get(1).end, Matchers.is(2));

		MatcherAssert.assertThat(colorPositions.get(2).color, Matchers.is(COLOR_ULTRABALL));
		MatcherAssert.assertThat(colorPositions.get(2).start, Matchers.is(5));
		MatcherAssert.assertThat(colorPositions.get(2).end, Matchers.is(6));
	}
}
