package org.luxons.sevenwonders.game.resources;

import java.util.NoSuchElementException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourcesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void init_shouldBeEmpty() {
        Resources resources = new Resources();
        for (ResourceType resourceType : ResourceType.values()) {
            assertEquals(0, resources.getQuantity(resourceType));
        }
        assertEquals(0, resources.size());
        assertTrue(resources.isEmpty());
    }

    @Test
    public void add_zero() {
        Resources resources = new Resources();
        resources.add(ResourceType.CLAY, 0);
        assertEquals(0, resources.getQuantity(ResourceType.CLAY));
        assertEquals(0, resources.size());
        assertTrue(resources.isEmpty());
    }

    @Test
    public void add_simple() {
        Resources resources = new Resources();
        resources.add(ResourceType.WOOD, 3);
        assertEquals(3, resources.getQuantity(ResourceType.WOOD));
        assertEquals(3, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void add_multipleCallsStacked() {
        Resources resources = new Resources();
        resources.add(ResourceType.ORE, 3);
        resources.add(ResourceType.ORE, 2);
        assertEquals(5, resources.getQuantity(ResourceType.ORE));
        assertEquals(5, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void add_interlaced() {
        Resources resources = new Resources();
        resources.add(ResourceType.GLASS, 3);
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.WOOD, 4);
        resources.add(ResourceType.GLASS, 2);
        assertEquals(5, resources.getQuantity(ResourceType.GLASS));
        assertEquals(10, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void remove_some() {
        Resources resources = new Resources();
        resources.add(ResourceType.WOOD, 3);
        resources.remove(ResourceType.WOOD, 2);
        assertEquals(1, resources.getQuantity(ResourceType.WOOD));
        assertEquals(1, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void remove_all() {
        Resources resources = new Resources();
        resources.add(ResourceType.WOOD, 3);
        resources.remove(ResourceType.WOOD, 3);
        assertEquals(0, resources.getQuantity(ResourceType.WOOD));
        assertEquals(0, resources.size());
        assertTrue(resources.isEmpty());
    }

    @Test
    public void remove_tooMany() {
        Resources resources = new Resources();
        resources.add(ResourceType.WOOD, 2);

        thrown.expect(NoSuchElementException.class);
        resources.remove(ResourceType.WOOD, 3);
    }

    @Test
    public void addAll_empty() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources emptyResources = new Resources();

        resources.addAll(emptyResources);
        assertEquals(1, resources.getQuantity(ResourceType.STONE));
        assertEquals(3, resources.getQuantity(ResourceType.CLAY));
        assertEquals(0, resources.getQuantity(ResourceType.ORE));
        assertEquals(0, resources.getQuantity(ResourceType.GLASS));
        assertEquals(0, resources.getQuantity(ResourceType.LOOM));
        assertEquals(4, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void addAll_zeros() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources emptyResources = new Resources();
        emptyResources.add(ResourceType.STONE, 0);
        emptyResources.add(ResourceType.CLAY, 0);

        resources.addAll(emptyResources);
        assertEquals(1, resources.getQuantity(ResourceType.STONE));
        assertEquals(3, resources.getQuantity(ResourceType.CLAY));
        assertEquals(0, resources.getQuantity(ResourceType.ORE));
        assertEquals(0, resources.getQuantity(ResourceType.GLASS));
        assertEquals(0, resources.getQuantity(ResourceType.LOOM));
        assertEquals(4, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void addAll_same() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources resources2 = new Resources();
        resources.add(ResourceType.STONE, 2);
        resources.add(ResourceType.CLAY, 6);

        resources.addAll(resources2);
        assertEquals(3, resources.getQuantity(ResourceType.STONE));
        assertEquals(9, resources.getQuantity(ResourceType.CLAY));
        assertEquals(0, resources.getQuantity(ResourceType.ORE));
        assertEquals(0, resources.getQuantity(ResourceType.GLASS));
        assertEquals(0, resources.getQuantity(ResourceType.LOOM));
        assertEquals(12, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void addAll_overlap() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources resources2 = new Resources();
        resources.add(ResourceType.CLAY, 6);
        resources.add(ResourceType.ORE, 4);

        resources.addAll(resources2);
        assertEquals(1, resources.getQuantity(ResourceType.STONE));
        assertEquals(9, resources.getQuantity(ResourceType.CLAY));
        assertEquals(4, resources.getQuantity(ResourceType.ORE));
        assertEquals(0, resources.getQuantity(ResourceType.GLASS));
        assertEquals(0, resources.getQuantity(ResourceType.LOOM));
        assertEquals(14, resources.size());
        assertFalse(resources.isEmpty());
    }

    @Test
    public void contains_emptyContainsEmpty() {
        Resources emptyResources = new Resources();
        Resources emptyResources2 = new Resources();
        assertTrue(emptyResources.contains(emptyResources2));
    }

    @Test
    public void contains_singleTypeContainsEmpty() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);

        Resources emptyResources = new Resources();

        assertTrue(resources.contains(emptyResources));
    }

    @Test
    public void contains_multipleTypesContainsEmpty() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources emptyResources = new Resources();

        assertTrue(resources.contains(emptyResources));
    }

    @Test
    public void contains_self() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        assertTrue(resources.contains(resources));
    }

    @Test
    public void contains_allOfEachType() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.STONE, 1);
        resources2.add(ResourceType.CLAY, 3);

        assertTrue(resources.contains(resources2));
    }

    @Test
    public void contains_someOfEachType() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 2);
        resources.add(ResourceType.CLAY, 4);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.STONE, 1);
        resources2.add(ResourceType.CLAY, 3);

        assertTrue(resources.contains(resources2));
    }

    @Test
    public void contains_someOfSomeTypes() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 2);
        resources.add(ResourceType.CLAY, 4);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.CLAY, 3);

        assertTrue(resources.contains(resources2));
    }

    @Test
    public void contains_allOfSomeTypes() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 2);
        resources.add(ResourceType.CLAY, 4);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.CLAY, 4);

        assertTrue(resources.contains(resources2));
    }

    @Test
    public void minus_empty() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources emptyResources = new Resources();

        Resources diff = resources.minus(emptyResources);
        assertEquals(1, diff.getQuantity(ResourceType.STONE));
        assertEquals(3, diff.getQuantity(ResourceType.CLAY));
        assertEquals(0, diff.getQuantity(ResourceType.ORE));
        assertEquals(0, diff.getQuantity(ResourceType.GLASS));
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void minus_self() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources diff = resources.minus(resources);
        assertEquals(0, diff.getQuantity(ResourceType.STONE));
        assertEquals(0, diff.getQuantity(ResourceType.CLAY));
        assertEquals(0, diff.getQuantity(ResourceType.ORE));
        assertEquals(0, diff.getQuantity(ResourceType.GLASS));
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void minus_allOfEachType() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 1);
        resources.add(ResourceType.CLAY, 3);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.STONE, 1);
        resources2.add(ResourceType.CLAY, 3);

        Resources diff = resources.minus(resources2);
        assertEquals(0, diff.getQuantity(ResourceType.STONE));
        assertEquals(0, diff.getQuantity(ResourceType.CLAY));
        assertEquals(0, diff.getQuantity(ResourceType.ORE));
        assertEquals(0, diff.getQuantity(ResourceType.GLASS));
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void minus_someOfEachType() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 2);
        resources.add(ResourceType.CLAY, 4);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.STONE, 1);
        resources2.add(ResourceType.CLAY, 3);

        Resources diff = resources.minus(resources2);
        assertEquals(1, diff.getQuantity(ResourceType.STONE));
        assertEquals(1, diff.getQuantity(ResourceType.CLAY));
        assertEquals(0, diff.getQuantity(ResourceType.ORE));
        assertEquals(0, diff.getQuantity(ResourceType.GLASS));
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void minus_someOfSomeTypes() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 2);
        resources.add(ResourceType.CLAY, 4);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.CLAY, 3);

        Resources diff = resources.minus(resources2);
        assertEquals(2, diff.getQuantity(ResourceType.STONE));
        assertEquals(1, diff.getQuantity(ResourceType.CLAY));
        assertEquals(0, diff.getQuantity(ResourceType.ORE));
        assertEquals(0, diff.getQuantity(ResourceType.GLASS));
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void minus_allOfSomeTypes() {
        Resources resources = new Resources();
        resources.add(ResourceType.STONE, 2);
        resources.add(ResourceType.CLAY, 4);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.CLAY, 4);

        Resources diff = resources.minus(resources2);
        assertEquals(2, diff.getQuantity(ResourceType.STONE));
        assertEquals(0, diff.getQuantity(ResourceType.CLAY));
        assertEquals(0, diff.getQuantity(ResourceType.ORE));
        assertEquals(0, diff.getQuantity(ResourceType.GLASS));
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void minus_tooMuchOfExistingType() {
        Resources resources = new Resources();
        resources.add(ResourceType.CLAY, 4);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.CLAY, 5);

        Resources diff = resources.minus(resources2);
        assertEquals(0, diff.getQuantity(ResourceType.CLAY));
    }

    @Test
    public void minus_someOfAnAbsentType() {
        Resources resources = new Resources();

        Resources resources2 = new Resources();
        resources2.add(ResourceType.LOOM, 5);

        Resources diff = resources.minus(resources2);
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void minus_someOfATypeWithZero() {
        Resources resources = new Resources();
        resources.add(ResourceType.LOOM, 0);

        Resources resources2 = new Resources();
        resources2.add(ResourceType.LOOM, 5);

        Resources diff = resources.minus(resources2);
        assertEquals(0, diff.getQuantity(ResourceType.LOOM));
    }

    @Test
    public void isEmpty_noElement() {
        Resources resources = new Resources();
        assertTrue(resources.isEmpty());
    }

    @Test
    public void isEmpty_singleZeroElement() {
        Resources resources = new Resources();
        resources.add(ResourceType.LOOM, 0);
        assertTrue(resources.isEmpty());
    }

    @Test
    public void isEmpty_multipleZeroElements() {
        Resources resources = new Resources();
        resources.add(ResourceType.WOOD, 0);
        resources.add(ResourceType.ORE, 0);
        resources.add(ResourceType.LOOM, 0);
        assertTrue(resources.isEmpty());
    }

    @Test
    public void isEmpty_singleElementMoreThanZero() {
        Resources resources = new Resources();
        resources.add(ResourceType.LOOM, 3);
        assertFalse(resources.isEmpty());
    }

    @Test
    public void isEmpty_mixedZeroAndNonZeroElements() {
        Resources resources = new Resources();
        resources.add(ResourceType.WOOD, 0);
        resources.add(ResourceType.LOOM, 3);
        assertFalse(resources.isEmpty());
    }

    @Test
    public void isEmpty_mixedZeroAndNonZeroElements_reverseOrder() {
        Resources resources = new Resources();
        resources.add(ResourceType.ORE, 3);
        resources.add(ResourceType.PAPYRUS, 0);
        assertFalse(resources.isEmpty());
    }

    @Test
    public void equals_falseWhenNull() {
        Resources resources = new Resources();
        resources.add(ResourceType.GLASS, 1);
        //noinspection ObjectEqualsNull
        assertFalse(resources.equals(null));
    }

    @Test
    public void equals_falseWhenDifferentClass() {
        Resources resources = new Resources();
        resources.add(ResourceType.GLASS, 1);
        Production production = new Production();
        production.addFixedResource(ResourceType.GLASS, 1);
        //noinspection EqualsBetweenInconvertibleTypes
        assertFalse(resources.equals(production));
    }

    @Test
    public void equals_trueWhenSame() {
        Resources resources = new Resources();
        assertEquals(resources, resources);
    }

    @Test
    public void equals_trueWhenSameContent() {
        Resources resources1 = new Resources();
        Resources resources2 = new Resources();
        assertTrue(resources1.equals(resources2));
        resources1.add(ResourceType.GLASS, 1);
        resources2.add(ResourceType.GLASS, 1);
        assertTrue(resources1.equals(resources2));
    }

    @Test
    public void hashCode_sameWhenSameContent() {
        Resources resources1 = new Resources();
        Resources resources2 = new Resources();
        assertEquals(resources1.hashCode(), resources2.hashCode());
        resources1.add(ResourceType.GLASS, 1);
        resources2.add(ResourceType.GLASS, 1);
        assertEquals(resources1.hashCode(), resources2.hashCode());
    }
}