package com.tank.flavorpairer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.tank.flavorpairer.object.Ingredient;
import com.tank.flavorpairer.object.IngredientNode;
import com.tank.flavorpairer.object.IngredientTree;
import com.tank.flavorpairer.object.PairingRank;

/**
 * Contains methods to process an {@link IngredientTree}.
 */
public class IngredientTreeProcessor {

	/**
	 * Declared private to prevent initialization.
	 */
	private IngredientTreeProcessor() {
	}

	/**
	 * Constructs a balanced {@link IngredientTree} with the given
	 * {@link Ingredient}s.
	 * 
	 * @param ingredients The List of {@link Ingredient}s to be placed in the tree.
	 * @return A non-null {@link IngredientTree}.
	 * @throws IllegalArgumentException if ingredients is null, empty, or contains
	 *                                  null.
	 */
	public static IngredientTree constructIngredientTree(final List<Ingredient> ingredients) {
		Preconditions.checkArgument(ingredients != null && !ingredients.isEmpty() && !ingredients.contains(null));

		IngredientNode rootIngredientNode = null;
		for (final IngredientNode ingredientNode : IngredientTreeUtil.createIngredients(ingredients)) {
			rootIngredientNode = addIngredientToTree(rootIngredientNode, ingredientNode);
		}
		final IngredientTree ingredientTree = new IngredientTree();
		ingredientTree.setRoot(rootIngredientNode);
		return ingredientTree;
	}

	/**
	 * Computes the pairings for the given {@link Ingredient} contained in the
	 * {@link IngredientTree}. <br>
	 * TODO: Add examples.
	 * 
	 * @param ingredient     The {@link Ingredient} to construct pairings for.
	 * @param ingredientTree The {@link IngredientTree} of ingredients to inspect.
	 * @param level          The level or degree of pairings to compute.
	 * @return The List of {@link PairingRank}s sorted by relevance. Will be null if
	 *         {@link Ingredient} or {@link IngredientNode} is null. Will be empty
	 *         if no pairings are found.
	 */
	public static List<PairingRank> computeIngredientPairingLevel(final Ingredient ingredient,
			final IngredientTree ingredientTree, int level) {
		if (ingredient == null || ingredientTree == null || ingredientTree.getRoot() == null) {
			return null;
		}

		final IngredientNode ingredientNode = IngredientTreeUtil.findIngredient(ingredient, ingredientTree.getRoot());
		if (ingredientNode == null) {
			return Collections.emptyList();
		}

		final List<PairingRank> pairingRanks = ingredientNode.getPairings().stream().map(i -> new PairingRank(i))
				.collect(Collectors.toList());
		return computeIngredientPairingLevel(ingredient, ingredientTree, level, pairingRanks);
	}

	/**
	 * Computes the pairings for the given {@link Ingredient} contained in the the
	 * {@link IngredientTree}. <br>
	 * 
	 * @param ingredient     The {@link Ingredient} to construct pairings for.
	 * @param ingredientTree The {@link IngredientTree} of ingredients to inspect.
	 * @param level          The level or degree of pairings to compute.
	 * @param pairingRanks   The List of {@link PairingRank}s to be processed.
	 * @return The List of {@link PairingRank}s sorted by relevance.
	 */
	private static List<PairingRank> computeIngredientPairingLevel(final Ingredient ingredient,
			final IngredientTree ingredientTree, int level, List<PairingRank> pairingRanks) {
		if (level == 1) {
			return pairingRanks;
		}

		final Map<Ingredient, PairingRank> pairingRanksByIngredient = pairingRanks.stream()
				.collect(Collectors.toMap(PairingRank::getIngredient, Function.identity()));
		for (final PairingRank pairingRank : pairingRanks) {
			// Level 1 computations will not contain pairing information, must attach for
			// level > 1
			if (pairingRank.getPairings().isEmpty()) {
				pairingRank.setPairings(pairingRank.getIngredient().getPairings());
			}

			final IngredientNode pairedIngredientNode = IngredientTreeUtil.findIngredient(pairingRank.getIngredient(),
					ingredientTree.getRoot());
			if (pairedIngredientNode == null) {
				// Hypothetically, the paired Ingredient does not appear in the tree.
				continue;
			}

			for (final Ingredient pairing : pairedIngredientNode.getIngredient().getPairings()) {
				if (pairingRanksByIngredient.containsKey(pairing)) {
					final PairingRank existingPairingRank = pairingRanksByIngredient.get(pairing);
					existingPairingRank.setPairings(pairing.getPairings());
					// Skip setting rank. Ingredients from previous levels will be counted multiple
					// times. Must reset.
				} else {
					pairingRanksByIngredient.put(pairing, new PairingRank(pairing, pairing.getPairings()));
				}

				pairingRanksByIngredient.get(pairing).setRank((int) pairingRanksByIngredient.values().stream()
						.filter(rank -> rank.getPairings().contains(pairing)).count());
			}
		}

		return computeIngredientPairingLevel(ingredient, ingredientTree, level - 1,
				pairingRanksByIngredient.values().stream()
						.sorted(Comparator.comparingInt(PairingRank::getRank).reversed()).collect(Collectors.toList()));
	}

	/**
	 * Add the given node to the tree.
	 * 
	 * @param root                   The root {@link IngredientNode}.
	 * @param ingredientNodeToInsert The {@link IngredientNode} to insert into the
	 *                               tree.
	 * @return The non-null root {@link IngredientNode}.
	 */
	private static IngredientNode addIngredientToTree(final IngredientNode root,
			final IngredientNode ingredientNodeToInsert) {
		if (root == null) {
			return ingredientNodeToInsert;
		}

		if (root.getName().compareToIgnoreCase(ingredientNodeToInsert.getName()) > 0) {
			root.setLeftNode(addIngredientToTree(root.getLeftNode(), ingredientNodeToInsert));

			if (needsRebalancing(root.getLeftNode(), root.getRightNode())) {
				if (root.getLeftNode().getName().compareToIgnoreCase(ingredientNodeToInsert.getName()) > 0) {
					return rotateToRight(root);
				} else {
					return doubleRotateToRight(root);
				}
			}
		} else if (root.getName().compareToIgnoreCase(ingredientNodeToInsert.getName()) < 0) {
			root.setRightNode(addIngredientToTree(root.getRightNode(), ingredientNodeToInsert));

			if (needsRebalancing(root.getRightNode(), root.getLeftNode())) {
				if (root.getRightNode().getName().compareToIgnoreCase(ingredientNodeToInsert.getName()) < 0) {
					return rotateToLeft(root);
				} else {
					return doubleRotateToLeft(root);
				}
			}
		} else {
			throw new RuntimeException("duplicate ingredient: " + ingredientNodeToInsert.getName());
		}
		return root;
	}

	/**
	 * Determines if the nodes are off balance.
	 * 
	 * @param node1 First {@link IngredientNode} to inspect.
	 * @param node2 Second {@link IngredientNode} to inspect.
	 * @return True if the tree needs to be rebalanced, false otherwise.
	 */
	private static boolean needsRebalancing(final IngredientNode node1, final IngredientNode node2) {
		return (IngredientTreeUtil.getDepth(node1) - IngredientTreeUtil.getDepth(node2)) > 1;
	}

	/**
	 * Rotates the {@link IngredientNode} to the right.
	 * 
	 * <pre>
	 *       P            C2 
	 *   C2     C5   -> C1     P
	 * C1  C3 C4  C6        C3   C5
	 *                          C4 C6
	 * </pre>
	 * 
	 * @param node The {@link IngredientNode} to rotate.
	 * @return The non-null rotated {@link IngredientNode}.
	 */
	private static IngredientNode rotateToRight(final IngredientNode node) {
		final IngredientNode leftNode = node.getLeftNode();
		node.setLeftNode(leftNode.getRightNode());
		leftNode.setRightNode(node);
		return leftNode;
	}

	/**
	 * Rotates the {@link IngredientNode} to the left.
	 * 
	 * <pre>
	 *       P                C5 
	 *   C2     C5   ->     P    C6
	 * C1  C3 C4  C6     C2  C4
	 *                 C1 C3
	 * </pre>
	 * 
	 * @param node The {@link IngredientNode} to rotate.
	 * @return The non-null rotated {@link IngredientNode}.
	 */
	private static IngredientNode rotateToLeft(final IngredientNode node) {
		final IngredientNode rightNode = node.getRightNode();
		node.setRightNode(rightNode.getLeftNode());
		rightNode.setLeftNode(node);
		return rightNode;
	}

	/**
	 * Rotates the {@link IngredientNode}'s left node to the left, then the parent
	 * to the right.
	 * 
	 * <pre>
	 *            8                 
	 *      4          12       ->   ugh  
	 *   2    6     10     14  
	 * 1  3  5 7   9  11  13 15
	 * </pre>
	 * 
	 * @param node The {@link IngredientNode} to rotate.
	 * @return The non-null rotated {@link IngredientNode}.
	 */
	private static IngredientNode doubleRotateToRight(final IngredientNode node) {
		node.setLeftNode(rotateToLeft(node.getLeftNode()));
		return rotateToRight(node);
	}

	/**
	 * Rotates the {@link IngredientNode}'s right node to the right, then the parent
	 * to the left.
	 * 
	 * @param node The {@link IngredientNode} to rotate.
	 * @return The non-null rotated {@link IngredientNode}.
	 */
	private static IngredientNode doubleRotateToLeft(final IngredientNode node) {
		node.setRightNode(rotateToRight(node.getRightNode()));
		return rotateToLeft(node);
	}

	/**
	 * Deletes the ingredient from the tree.
	 * 
	 * @param root       The {@link IngredientNode} to inspect.
	 * @param ingredient The {@link Ingredient} to delete.
	 * @return The new {@link IngredientNode} without the given {@link Ingredient}.
	 *         Will be null if no ingredients remain.
	 * @throws IllegalArgumentException if root or ingredient are null.
	 */
	@VisibleForTesting
	public static IngredientNode deleteNodeFromTree(final IngredientNode root, final Ingredient ingredient) {
		Preconditions.checkArgument(root != null);
		Preconditions.checkArgument(ingredient != null);

		final List<Ingredient> ingredients = IngredientTreeUtil.getIngredients(root);
		ingredients.remove(ingredient);

		if (ingredients.isEmpty()) {
			return null;
		}

		return constructIngredientTree(ingredients).getRoot();
	}
}
