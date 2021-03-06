package com.tank.flavorpairer;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.tank.flavorpairer.object.Ingredient;
import com.tank.flavorpairer.object.IngredientPairingResponse;
import com.tank.flavorpairer.object.IngredientTree;
import com.tank.flavorpairer.object.PairingRank;

public class IngredientPairingEngineTest {
	@Test
	public void testFirstLevelPairings_Zucchini() {
		final IngredientPairingResponse pairingResponse = computePairings(Ingredient.ZUCCHINI);
		Assertions.assertThat(pairingResponse).isNotNull();
		Assertions.assertThat(pairingResponse.getFirstLevelIngredientPairings())
				.containsAll(Ingredient.ZUCCHINI.getPairings()).doesNotContain(Ingredient.BACON);
	}

	@Test
	public void testSecondLevelPairings_Zucchini() {
		final IngredientPairingResponse pairingResponse = computePairings(Ingredient.ZUCCHINI);

		Assertions.assertThat(pairingResponse).isNotNull();
		final List<PairingRank> pairingRanks = pairingResponse.getSecondLevelIngredientPairingRanks();
		Assertions.assertThat(pairingRanks).isNotEmpty();
	}

	@Test
	public void testFirstCopy() {
		final IngredientTree ingredientTree = IngredientTreeProcessor
				.constructIngredientTree(Arrays.asList(Ingredient.values()));

		final List<PairingRank> x3LevelPairings = IngredientTreeProcessor
				.computeIngredientPairingLevel(Ingredient.ZUCCHINI, ingredientTree, 3);
		// x3LevelPairings.stream().forEach(System.out::println);
	}

	private static IngredientPairingResponse computePairings(Ingredient ingredient) {
		final IngredientPairingEngine engine = new IngredientPairingEngine();
		return engine.computePairings(ingredient);
	}
}
