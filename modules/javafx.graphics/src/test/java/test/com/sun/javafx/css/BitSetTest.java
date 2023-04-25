package test.com.sun.javafx.css;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sun.javafx.css.PseudoClassState;
import com.sun.javafx.css.PseudoClassStateShim;
import com.sun.javafx.css.StyleClassSet;

import javafx.css.PseudoClass;
import javafx.css.StyleClass;

public class BitSetTest {

    @Test
    void twoNonEmptyBitSetsWithSamePatternAndSizeShouldNotBeConsideredEqualsWhenElementTypesAreDifferent() {
        StyleClassSet set1 = new StyleClassSet();
        PseudoClassState set2 = new PseudoClassState();

        PseudoClass pseudoClass = PseudoClass.getPseudoClass("abc");

        int index = PseudoClassStateShim.pseudoClassMap.get(pseudoClass.getPseudoClassName());

        set1.add(new StyleClass("xyz", index));  // no idea why this is public API, but I'll take it
        set2.add(pseudoClass);

        /*
         * The two sets above contain elements of different types (PseudoClass and StyleClass)
         * and therefore should never be equal, despite their bit pattern being the same:
         */

        assertNotEquals(set1, set2);
    }

    @Test
    void twoEmptyBitSetsShouldBeEqual() {

        /*
         * Per Set contract, the empty set is equal to any other empty set.
         */

        assertEquals(new StyleClassSet(), new PseudoClassState());
        assertEquals(new PseudoClassState(), new StyleClassSet());
        assertEquals(Set.of(), new PseudoClassState());
        assertEquals(new PseudoClassState(), Set.of());
        assertEquals(Set.of(), new StyleClassSet());
        assertEquals(new StyleClassSet(), Set.of());
    }

}
