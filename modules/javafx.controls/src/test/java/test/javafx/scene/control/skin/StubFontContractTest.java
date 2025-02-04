package test.javafx.scene.control.skin;

import com.sun.javafx.scene.control.skin.Utils;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is a test for fonts, font loading and their computed sizes.
 * Note that while we only test the stub font loading here, we still want to verify some basic rules that apply
 * for all headless tests. Some rules are somewhat derived from the real font loading in JavaFX.
 * <p>
 * See the javadoc for every method to get more details.
 * @see test.com.sun.javafx.pgstub.StubFontLoader
 * @see test.com.sun.javafx.pgstub.StubTextLayout
 */
class StubFontContractTest {

    /**
     * An unknown font will fall back to the system font. Note that this is the same behaviour in JavaFX.
     */
    @Test
    public void testUnknownFont() {
        Font font = new Font("bla", 10);

        assertEquals("bla", font.getName());
        assertEquals("System", font.getFamily());
        assertEquals("Regular", font.getStyle());
        assertEquals(10, font.getSize());

        assertEquals(100, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(10, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * Even if the font name is equal with a family name, we do not know the font by name.
     */
    @Test
    public void testFamilyAsFontName() {
        Font font = new Font("System", 10);

        assertEquals("System", font.getName());
        assertEquals("System", font.getFamily());
        assertEquals("Regular", font.getStyle());
        assertEquals(10, font.getSize());

        assertEquals(100, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(10, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * System Regular is the default font for headless testing but also for JavaFX itself.
     * 12 is the default font size for headless testing. This is different from JavaFX, as size from the OS will be used.
     */
    @Test
    public void testDefaultFont() {
        Label lbl = new Label();
        Font defaultFont = lbl.getFont();

        assertEquals("System Regular", defaultFont.getName());
        assertEquals("System", defaultFont.getFamily());
        assertEquals("Regular", defaultFont.getStyle());
        assertEquals(12, defaultFont.getSize());

        assertEquals(120, Utils.computeTextWidth(defaultFont, "ABCDEFGHIJ", -1));
        assertEquals(12, Utils.computeTextHeight(defaultFont, "ABCDEFGHIJ", 0, null));
    }

    @Test
    public void testDefaultFontSet() {
        Label lbl = new Label();
        Font font = lbl.getFont();

        assertEquals("System Regular", font.getName());
        assertEquals(12, font.getSize());

        lbl.setFont(Font.font("system", FontWeight.BOLD, 20));

        font = lbl.getFont();
        assertEquals("System Bold", font.getName());
        assertEquals(20, font.getSize());

        assertEquals(210, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(20, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    @Test
    public void testDefaultFontCssSet() {
        Label lbl = new Label();
        Font font = lbl.getFont();

        assertEquals("System Regular", font.getName());
        assertEquals(12, font.getSize());

        StageLoader stageLoader = new StageLoader(lbl);

        lbl.setStyle("-fx-font: bold 20px System;");
        lbl.applyCss();

        stageLoader.dispose();

        font = lbl.getFont();
        assertEquals("System Bold", font.getName());
        assertEquals(20, font.getSize());

        assertEquals(210, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(20, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * System Regular is a font that is available for testing.
     */
    @Test
    public void testFontByName() {
        Font font = new Font("System Regular", 11);

        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The System family is available for testing.
     */
    @Test
    public void testFontByFamily() {
        Font font = Font.font("System", 11);

        assertEquals("Regular", font.getStyle());
        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can be loaded with a normal weight.
     */
    @Test
    public void testFontByFamilyNormal() {
        Font font = Font.font("System", FontWeight.NORMAL, 11);

        assertEquals("Regular", font.getStyle());
        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can also be bold. In that case it is a bit wider than the normal one.
     */
    @Test
    public void testFontByFamilyBold() {
        Font font = Font.font("System", FontWeight.BOLD, 13);

        assertEquals("Bold", font.getStyle());
        assertEquals(140, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(13, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can also be italic.
     */
    @Test
    public void testFontByFamilyItalic() {
        Font font = Font.font("System", FontPosture.ITALIC, 11);

        assertEquals("Italic", font.getStyle());
        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * The system font can also be bold and Italic. In that case it is a bit wider than the normal one.
     */
    @Test
    public void testFontByFamilyBoldItalic() {
        Font font = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 13);

        assertEquals("Bold Italic", font.getStyle());
        assertEquals(140, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(13, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

    /**
     * Amble is the other font we support for headless testing.
     */
    @Test
    public void testAmbleFont() {
        Font font = Font.font("Amble", 11);

        assertEquals(110, Utils.computeTextWidth(font, "ABCDEFGHIJ", -1));
        assertEquals(11, Utils.computeTextHeight(font, "ABCDEFGHIJ", 0, null));
    }

}
