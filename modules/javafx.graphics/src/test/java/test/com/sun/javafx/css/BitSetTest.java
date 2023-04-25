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
    void shouldBeEqual() {
        StyleClassSet set1 = new StyleClassSet();
        StyleClassSet set2 = new StyleClassSet();

        set1.add(StyleClassSet.getStyleClass("abc"));
        set2.add(StyleClassSet.getStyleClass("abc"));

        assertEquals(set1, set2);

        for (int i = 0; i < 1000; i++) {
            // grow internal bit set array:
            set1.add(StyleClassSet.getStyleClass("" + i));

            assertNotEquals(set1, set2);
        }

        for (int i = 0; i < 1000; i++) {
            set1.remove(StyleClassSet.getStyleClass("" + i));
        }

        // still equal despite internal array sizes being different size:
        assertEquals(set1, set2);
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
