package test.com.sun.javafx.scene.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.sun.javafx.scene.layout.SpaceDistributor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpaceDistributorTest {

    @Test
    void shouldGrowSpaceDeterministicallyAndExactlyWhenUnscaled() {
        assertGrow(
            1.0,
            "A.[.]B.....[......]C....[..]",
            new Expected(15, "A--B------C-----"),
            new Expected(16, "A--B------C-----"),
            new Expected(17, "A---B------C-----"),  // deterministic, A receives first new space
            new Expected(18, "A---B-------C-----"), // B receives second new space, etc.
            new Expected(19, "A---B-------C------"),
            new Expected(20, "A----B-------C------"),
            new Expected(21, "A----B--------C------"),
            new Expected(22, "A----B--------C-------"),
            new Expected(23, "A----B---------C-------"),
            new Expected(24, "A----B---------C--------"),
            new Expected(25, "A----B----------C--------"),
            new Expected(26, "A----B-----------C--------"),
            new Expected(27, "A----B------------C--------"),
            new Expected(28, "A----B-------------C--------"),
            new Expected(29, "A----B-------------C--------")
        );

        assertGrow(
            1.0,
            "A...]B.....[......]C....[..]",
            new Expected(13, "B------C-----"),
            new Expected(14, "AB------C-----")
        );
    }

    @Test
    void shouldShrinkSpaceDeterministicallyAndExactlyWhenUnscaled() {
        assertShrink(
            1.0,
            "A.[.]B.....[......]C....[..]",
            new Expected(29, "A----B-------------C--------"),
            new Expected(28, "A----B-------------C--------"),
            new Expected(27, "A---B-------------C--------"),  // A first
            new Expected(26, "A---B------------C--------"),   // then B
            new Expected(25, "A---B------------C-------"),    // then C
            new Expected(24, "A--B------------C-------"),     // then A again
            new Expected(23, "A--B-----------C-------"),
            new Expected(22, "A--B-----------C------"),
            new Expected(21, "A--B----------C------"),
            new Expected(20, "A--B----------C-----"),
            new Expected(19, "A--B---------C-----"),
            new Expected(18, "A--B--------C-----"),
            new Expected(17, "A--B-------C-----"),
            new Expected(16, "A--B------C-----"),
            new Expected(15, "A--B------C-----")
        );

        assertShrink(
            1.0,
            "A...]B.....[......]C....[..]",
            new Expected(16, "B---------C-----"),
            new Expected(17, "AB---------C-----")
        );
    }

    @Test
    void shouldGrowSpaceDeterministicallyAndExactlyWhenScaled200Percent() {
        assertGrow(
            2.0,
            "A.[.]B.....[......]C....[..]",
            new Expected(15.0, "A-----B-------------C-----------"),
            new Expected(15.5, "A-----B-------------C-----------"),
            new Expected(16.0, "A-----B-------------C-----------"),
            new Expected(16.5, "A------B-------------C-----------"), // deterministic, A receives first new space
            new Expected(17.0, "A------B--------------C-----------"), // B receives second new space, etc.
            new Expected(17.5, "A------B--------------C------------"),
            new Expected(18.0, "A-------B--------------C------------"),
            new Expected(18.5, "A-------B---------------C------------"),
            new Expected(19.0, "A-------B---------------C-------------"),
            new Expected(19.5, "A--------B---------------C-------------"),
            new Expected(20.0, "A--------B----------------C-------------"),
            new Expected(21.0, "A---------B----------------C--------------"),
            new Expected(22.0, "A---------B-----------------C---------------"),
            new Expected(23.0, "A---------B------------------C----------------"),
            new Expected(24.0, "A---------B-------------------C-----------------"),
            new Expected(25.0, "A---------B---------------------C-----------------"),
            new Expected(26.0, "A---------B-----------------------C-----------------"),
            new Expected(27.0, "A---------B-------------------------C-----------------"),
            new Expected(27.5, "A---------B--------------------------C-----------------"),
            new Expected(28.0, "A---------B---------------------------C-----------------"),
            new Expected(28.5, "A---------B---------------------------C-----------------"),
            new Expected(29.0, "A---------B---------------------------C-----------------")
        );

        assertGrow(
            2.0,
            "A...]B.....[......]C....[..]",
            new Expected(13.0, "B-------------C-----------"),
            new Expected(13.5, "AB-------------C-----------"),
            new Expected(14.0, "AB--------------C-----------")
        );
    }

    @Test
    void shouldShrinkSpaceDeterministicallyAndExactlyWhenScaled200Percent() {
        assertShrink(
            2.0,
            "A.[.]B.....[......]C....[..]",
            new Expected(29.0, "A---------B---------------------------C-----------------"),
            new Expected(28.5, "A---------B---------------------------C-----------------"),
            new Expected(28.0, "A---------B---------------------------C-----------------"),
            new Expected(27.5, "A--------B---------------------------C-----------------"),  // A shrinks first
            new Expected(27.0, "A--------B--------------------------C-----------------"),   // then B
            new Expected(26.5, "A--------B--------------------------C----------------"),    // then C
            new Expected(26.0, "A-------B--------------------------C----------------"),
            new Expected(25.5, "A-------B-------------------------C----------------"),
            new Expected(25.0, "A-------B-------------------------C---------------"),
            new Expected(24.5, "A------B-------------------------C---------------"),
            new Expected(24.0, "A------B------------------------C---------------"),
            new Expected(23.5, "A------B------------------------C--------------"),
            new Expected(23.0, "A-----B------------------------C--------------"),
            new Expected(22.5, "A-----B-----------------------C--------------"),
            new Expected(22.0, "A-----B-----------------------C-------------"),
            new Expected(21.5, "A-----B----------------------C-------------"),  // A reached minimum, B shrinks first
            new Expected(21.0, "A-----B----------------------C------------"),
            new Expected(20.0, "A-----B---------------------C-----------"),  // C reached minimum
            new Expected(19.0, "A-----B-------------------C-----------"),
            new Expected(18.0, "A-----B-----------------C-----------"),
            new Expected(17.0, "A-----B---------------C-----------"),
            new Expected(16.5, "A-----B--------------C-----------"),
            new Expected(16.0, "A-----B-------------C-----------"),  // B reached minimum
            new Expected(15.5, "A-----B-------------C-----------"),
            new Expected(15.0, "A-----B-------------C-----------")
        );

        assertShrink(
            2.0,
            "A...]B.....[......]C....[..]",
            new Expected(18.0, "A--B--------------------C-----------"),
            new Expected(16.0, "AB------------------C-----------"),
            new Expected(15.5, "B------------------C-----------"),  // A disappeared
            new Expected(15.0, "B-----------------C-----------")
        );
    }

    @Test
    void shouldGrowSpaceDeterministicallyAndExactlyWhenScaled() {
        assertGrow(
            1.5,
            "A.[.]B.....[......]C....[..]",
            new Expected( 0.00, "A----B----------C--------"),
            new Expected(15.33, "A----B----------C--------"),
            new Expected(16.00, "A----B----------C--------"),
            new Expected(16.66, "A----B----------C--------"), // minimum
            new Expected(17.33, "A-----B----------C--------"), // A first to grow
            new Expected(18.00, "A-----B-----------C--------"), // then B
            new Expected(18.66, "A-----B-----------C---------"), // then C, etc..
            new Expected(19.33, "A------B-----------C---------"), // A
            new Expected(20.00, "A------B------------C---------"), // B
            new Expected(20.66, "A------B------------C----------"), // C
            new Expected(21.33, "A-------B------------C----------"), // A
            new Expected(22.00, "A-------B-------------C----------"), // B
            new Expected(22.66, "A-------B-------------C-----------"), // C
            new Expected(23.33, "A-------B--------------C-----------"), // B because A reached maximum
            new Expected(24.00, "A-------B--------------C------------"), // C
            new Expected(24.66, "A-------B---------------C------------"), // B
            new Expected(25.33, "A-------B---------------C-------------"), // C
            new Expected(26.00, "A-------B----------------C-------------"), // B
            new Expected(26.66, "A-------B-----------------C-------------"), // B
            new Expected(27.33, "A-------B------------------C-------------"), // B
            new Expected(28.00, "A-------B-------------------C-------------"), // B
            new Expected(28.66, "A-------B--------------------C-------------"), // maximum
            new Expected(29.33, "A-------B--------------------C-------------"),
            new Expected(Double.MAX_VALUE, "A-------B--------------------C-------------"),
            new Expected(Double.POSITIVE_INFINITY, "A-------B--------------------C-------------")
        );
    }

    @Test
    void shouldShrinkSpaceDeterministicallyAndExactlyWhenScaled() {
        assertShrink(
            1.5,
            "A.[.]B.....[......]C....[..]",  // sizes specified in unscaled values: 28 preferred, 13 minimum
            // Preferred sizes : 5.0,  14.0, 9.0
            // Rounds to       : 5.33, 14.0, 9.33 -> to physical pixels * 1.5 -> 8, 21, 14 (43)
            // Minimum sizes   : 3.0,  7.0,  6.0
            // Rounds to       : 3.33, 7.33, 6.0  -> to physical pixels * 1.5 -> 5, 11, 9
            new Expected(Double.POSITIVE_INFINITY, "A-------B--------------------C-------------"),
            new Expected(Double.MAX_VALUE, "A-------B--------------------C-------------"),
            new Expected(29.33, "A-------B--------------------C-------------"),
            new Expected(28.66, "A-------B--------------------C-------------"), // maximum (8, 21, 14 pixels)
            new Expected(28.00, "A------B--------------------C-------------"),  // A shrinks first
            new Expected(27.33, "A------B-------------------C-------------"),   // then B
            new Expected(26.66, "A------B-------------------C------------"),    // then C
            new Expected(26.00, "A-----B-------------------C------------"),
            new Expected(25.33, "A-----B------------------C------------"),
            new Expected(24.66, "A-----B------------------C-----------"),
            new Expected(24.00, "A----B------------------C-----------"),
            new Expected(23.33, "A----B-----------------C-----------"),
            new Expected(22.66, "A----B-----------------C----------"),
            new Expected(22.00, "A----B----------------C----------"),  // A reached minimum, so B shrinks first
            new Expected(21.33, "A----B----------------C---------"),   // then C
            new Expected(20.66, "A----B---------------C---------"),
            new Expected(20.00, "A----B---------------C--------"),
            new Expected(19.33, "A----B--------------C--------"),  // C reached minimum
            new Expected(18.66, "A----B-------------C--------"),
            new Expected(18.00, "A----B------------C--------"),
            new Expected(17.33, "A----B-----------C--------"),
            new Expected(16.66, "A----B----------C--------"),  // minimum (5, 11, 9 pixels)
            new Expected(16.00, "A----B----------C--------"),
            new Expected(15.33, "A----B----------C--------")
        );
    }

    @Test
    void shouldDistributeSpaceScaled() {
        assertArrayEquals(
            new double[] {10.000},
            SpaceDistributor.distribute(10, 1.5, new double[] {0}, new double[] {100}),
            0.001
        );

        assertArrayEquals(
            new double[] {11.333},
            SpaceDistributor.distribute(11, 1.5, new double[] {0}, new double[] {100}),
            0.001
        );
    }

    @Test
    void shouldDistributeSpaceCorrectlyUnscaled() {
        // Cases with a single child:
        assertArrayEquals(
            new double[] {10.0},
            SpaceDistributor.distribute(10, 0, new double[] {0}, new double[] {100}),
            0.001
        );

        assertArrayEquals(
            new double[] {11.0},
            SpaceDistributor.distribute(11, 0, new double[] {0}, new double[] {100}),
            0.001
        );

        // Cases with a two children:
        assertArrayEquals(
            new double[] {5.0, 5.0},
            SpaceDistributor.distribute(10, 0, new double[] {0, 0}, new double[] {100, 100}),
            0.001
        );

        assertArrayEquals(
            new double[] {5.5, 5.5},
            SpaceDistributor.distribute(11, 0, new double[] {0, 0}, new double[] {100, 100}),
            0.001
        );
    }

    private static final Pattern PATTERN = Pattern.compile(".*?[\\|\\]]");

    /**
     * Given an input which represents a number of children, verifies they
     * have matching final sizes for each element in the expected array.<p>
     *
     * The input string has a format that represents both the number of
     * children and their respective minimum and maximum sizes. A child is
     * represented by a range of characters terminated by either a pipe or
     * square closing bracket. Characters other than the square brackets or pipe
     * have no significance, and whatever is visually pleasing can be used.<p>
     *
     * A single child can be specified as follows:
     *
     * <table>
     * <tr><th>Input</th><th>Minimum size</th><th>Maximum size</th></tr>
     * <tr><td><pre>....]</pre></td><td align="center">0</td><td align="center">5</td></tr>
     * <tr><td><pre>......]</pre></td><td align="center">0</td><td align="center">7</td></tr>
     * <tr><td><pre>[.....]</pre></td><td align="center">1</td><td align="center">7</td></tr>
     * <tr><td><pre>.[....]</pre></td><td align="center">2</td><td align="center">7</td></tr>
     * <tr><td><pre>....[.]</pre></td><td align="center">5</td><td align="center">7</td></tr>
     * <tr><td><pre>......|</pre></td><td align="center">7</td><td align="center">7</td></tr>
     * <tr><td><pre>|</pre></td><td align="center">1</td><td align="center">1</td></tr>
     * <tr><td><pre>[]</pre></td><td align="center">1</td><td align="center">2</td></tr>
     * </table>
     *
     * To specify multiple children, simply concatenate them:
     *
     * <table>
     * <tr><th>Input</th><th>Number of children</th><th>Description</th></tr>
     * <tr><td><pre>....]....[.]</pre></td><td align="center">2</td><td>Child 1 has a maximum of 5, and Child 2 has a minimum of 5 and maximum of 7</td></tr>
     * <tr><td><pre>|||</pre></td><td align="center">3</td><td>Three children all with a minimum and maximum size of 1</td></tr>
     * </table>
     *
     * Note that the expected values must take the pixel scale into account. In other words,
     * if pixel scale is 2, the expected values are twice as long.
     *
     * @param pixelScale a size multiplier
     * @param input a string representing the number of children and their minimum and maximum sizes, cannot be {@code null}
     * @param grow whether to shrink or grow
     * @param expecteds expected sizes for all children for different available space sizes
     */
    private void assertSizes(double pixelScale, String input, boolean grow, Expected... expecteds) {
        Matcher matcher = PATTERN.matcher(input);
        List<Double> minimumSizes = new ArrayList<>();
        List<Double> maximumSizes = new ArrayList<>();

        while (matcher.find()) {
            String match = matcher.group();
            double max = match.length();
            double min = match.indexOf("[") + 1;
            boolean fixedSize = match.endsWith("|");

            minimumSizes.add(fixedSize ? max : min == -1 ? 0 : min);
            maximumSizes.add(max);
        }

        for (Expected expected : expecteds) {
            System.out.println("--- expected " + expected);
            double[] spaces = SpaceDistributor.distribute(
                expected.space,
                pixelScale,
                (grow ? minimumSizes : maximumSizes).stream().mapToDouble(Double::doubleValue).toArray(),
                (grow ? maximumSizes : minimumSizes).stream().mapToDouble(Double::doubleValue).toArray()
            );

            String result = "";
            char c = 'A';

            for (double space : spaces) {
                int count = (int) Math.round(space * pixelScale);

                if (count > 0) {
                    result += ("" + c) + "-".repeat(count - 1);
                }

                c++;
            }

            assertEquals(expected.expected, result, "for " + expected.space + "\n" + expected.expected + " <- expected\n" + result + " <- got\n");
        }
    }

    private void assertGrow(double pixelScale, String input, Expected... expecteds) {
        assertSizes(pixelScale, input, true, expecteds);
    }

    private void assertShrink(double pixelScale, String input, Expected... expecteds) {
        assertSizes(pixelScale, input, false, expecteds);
    }

    record Expected(double space, String expected) {}
}
