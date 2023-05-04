package test.com.sun.javafx.scene.layout;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import com.sun.javafx.scene.layout.ScaledMath;

public class ScaledMathTest {

    @Test
    void ceilShouldBeStable() {
        for (double scale : new double[] {0.5, 2.0 / 3.0, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 4.0 / 3.0}) {
            for (double d = 0; d < 1e13; d++, d *= 1.1) {  // larger values break down because there are not enough fractional digits anymore
                double expected = Math.ceil(d * scale) / scale;

                assertEquals(expected, ScaledMath.ceil(d, scale), 0.0);
                assertEquals(expected, ScaledMath.ceil(ScaledMath.ceil(d, scale), scale), 0.0);
            }
        }
    }

    @Test
    void floorShouldBeStable() {
        for (double scale : new double[] {0.5, 2.0 / 3.0, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 4.0 / 3.0}) {
            for (double d = 0; d < 1e13; d++, d *= 1.1) {  // larger values break down because there are not enough fractional digits anymore
                double expected = Math.floor(d * scale) / scale;

                assertEquals(expected, ScaledMath.floor(d, scale), 0.0);
                assertEquals(expected, ScaledMath.floor(ScaledMath.floor(d, scale), scale), 0.0);
            }
        }
    }

    @Test
    void ceilShouldHandleLargeMagnitudeValuesWithoutReturningNaN() {
        assertEquals(Double.MAX_VALUE, ScaledMath.ceil(Double.MAX_VALUE, 0.5), Math.ulp(Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, ScaledMath.ceil(Double.MAX_VALUE, 1.0), Math.ulp(Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE, ScaledMath.ceil(Double.MAX_VALUE, 1.5), Math.ulp(Double.MAX_VALUE));

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(-Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(-Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(-Double.MAX_VALUE, ScaledMath.ceil(-Double.MAX_VALUE, 1.5), Math.ulp(-Double.MAX_VALUE));

        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.ceil(Double.POSITIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.ceil(Double.POSITIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.ceil(Double.POSITIVE_INFINITY, 1.5), 0.0);

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(Double.NEGATIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(Double.NEGATIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.ceil(Double.NEGATIVE_INFINITY, 1.5), 0.0);
    }

    @Test
    void floorShouldHandleLargeMagnitudeValuesWithoutReturningNaN() {
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(Double.MAX_VALUE, ScaledMath.floor(Double.MAX_VALUE, 1.5), Math.ulp(Double.MAX_VALUE));

        assertEquals(-Double.MAX_VALUE, ScaledMath.floor(-Double.MAX_VALUE, 0.5), Math.ulp(-Double.MAX_VALUE));
        assertEquals(-Double.MAX_VALUE, ScaledMath.floor(-Double.MAX_VALUE, 1.0), Math.ulp(-Double.MAX_VALUE));
        assertEquals(-Double.MAX_VALUE, ScaledMath.floor(-Double.MAX_VALUE, 1.5), Math.ulp(-Double.MAX_VALUE));

        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.POSITIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.POSITIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.floor(Double.POSITIVE_INFINITY, 1.5), 0.0);

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.floor(Double.NEGATIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.floor(Double.NEGATIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.floor(Double.NEGATIVE_INFINITY, 1.5), 0.0);
    }

    @Test
    void rintShouldHandleLargeMagnitudeValuesWithoutReturningNaN() {
        assertEquals(Double.MAX_VALUE, ScaledMath.rint(Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(Double.MAX_VALUE, ScaledMath.rint(Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.MAX_VALUE, 1.5), 0.0);

        assertEquals(-Double.MAX_VALUE, ScaledMath.rint(-Double.MAX_VALUE, 0.5), 0.0);
        assertEquals(-Double.MAX_VALUE, ScaledMath.rint(-Double.MAX_VALUE, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(-Double.MAX_VALUE, 1.5), 0.0);

        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.POSITIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.POSITIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, ScaledMath.rint(Double.POSITIVE_INFINITY, 1.5), 0.0);

        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(Double.NEGATIVE_INFINITY, 0.5), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(Double.NEGATIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, ScaledMath.rint(Double.NEGATIVE_INFINITY, 1.5), 0.0);
    }
}
