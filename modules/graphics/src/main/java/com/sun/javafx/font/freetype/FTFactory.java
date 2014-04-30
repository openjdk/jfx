package com.sun.javafx.font.freetype;

import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.text.GlyphLayout;
import com.sun.javafx.text.TextRun;

public class FTFactory extends PrismFontFactory {

    static boolean LCD_SUPPORT;

    public static PrismFontFactory getFactory() {
        PrismFontFactory factory = null;
        long[] ptr = new long[1];
        int error = OSFreetype.FT_Init_FreeType(ptr);
        long library = ptr[0];
        int[] major = new int[1], minor = new int[1], patch = new int[1];
        if (error == 0) {
            factory = new FTFactory();
            OSFreetype.FT_Library_Version(library, major, minor, patch);

            /* This implementation only supports LCD if freetype has support. */
            error = OSFreetype.FT_Library_SetLcdFilter(library, OSFreetype.FT_LCD_FILTER_DEFAULT);
            LCD_SUPPORT = error == 0;
            OSFreetype.FT_Done_FreeType(library);
        }
        if (PrismFontFactory.debugFonts) {
            if (factory != null) {
                String version = major[0] + "." + minor[0] + "." + patch[0];
                System.err.println("Freetype2 Loaded (version " + version + ")");
                String lcdSupport = LCD_SUPPORT ? "Enabled" : "Disabled";
                System.err.println("LCD support " + lcdSupport);
            } else {
                System.err.println("Freetype2 Failed (error " + error + ")");
            }
        }
        return factory;
    }

    private FTFactory() {
    }

    @Override
    protected PrismFontFile createFontFile(String name, String filename,
                                           int fIndex, boolean register,
                                           boolean embedded, boolean copy,
                                           boolean tracked) throws Exception {
        return new FTFontFile(name, filename, fIndex, register,
                              embedded, copy, tracked);
    }

    @Override
    public GlyphLayout createGlyphLayout() {
        if (OSFreetype.isPangoEnabled()) {
            return new PangoGlyphLayout();
        }
        if (OSFreetype.isHarfbuzzEnabled()) {
            return new HBGlyphLayout();
        }
        return new StubGlyphLayout();
    }

    @Override
    public boolean isLCDTextSupported() {
        return LCD_SUPPORT && super.isLCDTextSupported();
    }

    @Override
    protected boolean registerEmbeddedFont(String path) {
        long[] ptr = new long[1];
        int error = OSFreetype.FT_Init_FreeType(ptr);
        if (error != 0) return false;
        long library = ptr[0];
        byte[] buffer = (path+"\0").getBytes();
        error = OSFreetype.FT_New_Face(library, buffer, 0, ptr);
        if (error != 0) {
            long face = ptr[0];
            OSFreetype.FT_Done_Face(face);
        }
        OSFreetype.FT_Done_FreeType(library);
        return error == 0;
    }

    private static class StubGlyphLayout extends GlyphLayout {

        public StubGlyphLayout() {
        }

        @Override
        public void layout(TextRun run, PGFont font, FontStrike strike, char[] text) {
        }
    }

}
