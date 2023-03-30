package test.com.sun.javafx.css;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sun.javafx.css.ImmutablePseudoClassSetsCache;

import javafx.css.PseudoClass;

public class ImmutablePseudoClassSetsCacheTest {

    @Test
    void shouldCacheSets() {
        Set<PseudoClass> myOwnSet = Set.of(PseudoClass.getPseudoClass("a"));

        Set<PseudoClass> set1 = ImmutablePseudoClassSetsCache.of(new HashSet<>(Set.of(PseudoClass.getPseudoClass("a"))));
        Set<PseudoClass> set2 = ImmutablePseudoClassSetsCache.of(new HashSet<>(myOwnSet));
        Set<PseudoClass> set3 = ImmutablePseudoClassSetsCache.of(myOwnSet);
        Set<PseudoClass> set4 = ImmutablePseudoClassSetsCache.of(Set.of(PseudoClass.getPseudoClass("b")));

        assertEquals(set1, set2);
        assertEquals(set2, set3);
        assertNotEquals(set1, set4);
        assertNotEquals(set2, set4);
        assertNotEquals(set3, set4);

        assertTrue(set1 == set2);
        assertTrue(set2 == set3);

        assertEquals(myOwnSet, set1);

        // this does not need to be true if this set was not the first one cached
        assertFalse(myOwnSet == set1);
    }
}
