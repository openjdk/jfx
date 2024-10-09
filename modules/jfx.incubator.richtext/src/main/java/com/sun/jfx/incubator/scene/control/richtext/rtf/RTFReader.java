/*
 * Copyright (c) 1997, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
// adapted from package javax.swing.text.rtf;
package com.sun.jfx.incubator.scene.control.richtext.rtf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledInput;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

/**
 * Takes a sequence of RTF tokens and text and appends the text
 * described by the RTF to a <code>StyledDocument</code> (the <em>target</em>).
 * The RTF is lexed
 * from the character stream by the <code>RTFParser</code> which is this class's
 * superclass.
 *
 * This class is an indirect subclass of OutputStream. It must be closed
 * in order to guarantee that all of the text has been sent to
 * the text acceptor.
 */
public class RTFReader extends RTFParser {
    /** Indicates the domain of a Style */
    private static final Object STYLE_TYPE = new Object();
    /** Value for StyleType indicating a section style */
    private static final Object STYLE_SECTION = new Object();
    /** Value for StyleType indicating a paragraph style */
    private static final Object STYLE_PARAGRAPH = new Object();
    /** Value for StyleType indicating a character style */
    private static final Object STYLE_CHARACTER = new Object();
    /** The style of the text following this style */
    private static final Object STYLE_NEXT = new Object();
    /** Whether the style is additive */
    private static final Object STYLE_ADDITIVE = new Object();
    /** Whether the style is hidden from the user */
    private static final Object STYLE_HIDDEN = new Object();

    private final String text;
    private ArrayList<StyledSegment> segments;

    /** Miscellaneous information about the parser's state. This
     *  dictionary is saved and restored when an RTF group begins
     *  or ends. */
    private HashMap<Object, Object> parserState; /* Current parser state */
    /** This is the "dst" item from parserState. rtfDestination
     *  is the current rtf destination. It is cached in an instance
     *  variable for speed. */
    private Destination rtfDestination;
    /** This holds the current document attributes. */
    private AttrSet documentAttributes;

    /** This Dictionary maps Integer font numbers to String font names. */
    private HashMap<Integer, String> fontTable;
    /** This array maps color indices to Color objects. */
    private Color[] colorTable;
    /** This Map maps character style numbers to Style objects. */
    private HashMap<Integer, Style> characterStyles;
    /** This Map maps paragraph style numbers to Style objects. */
    private HashMap<Integer, Style> paragraphStyles;
    /** This Map maps section style numbers to Style objects. */
    private HashMap<Integer, Style> sectionStyles;

    /** <code>true</code> to indicate that if the next keyword is unknown,
     *  the containing group should be ignored. */
    private boolean ignoreGroupIfUnknownKeyword;

    /** The parameter of the most recently parsed \\ucN keyword,
     *  used for skipping alternative representations after a
     *  Unicode character. */
    private int skippingCharacters;

    private final AttrSet.Holder holder = new AttrSet.Holder();

    private static final String DEFAULT_STYLE = "default";
    private final HashMap<String,Style> styles = initStyles(); // TODO can init default style on demand

    private static final HashMap<String, RTFAttribute> straightforwardAttributes = RTFAttributes.attributesByKeyword();

    /** textKeywords maps RTF keywords to single-character strings,
     *  for those keywords which simply insert some text. */
    private static final HashMap<String, String> textKeywords = initTextKeywords();
    private static final HashMap<String, char[]> characterSets = initCharacterSets();

    /* TODO: per-font font encodings ( \fcharset control word ) ? */

    /**
     * Creates a new RTFReader instance.
     * @param text the RTF input string
     */
    public RTFReader(String text) {
        this.text = text;
        //System.err.println(text); // FIX

        parserState = new HashMap<>();
        fontTable = new HashMap<Integer, String>();
        documentAttributes = new AttrSet();
    }

    /**
     * Processes the RTF input and generates a StyledInput instance.
     * @return the StyledInput
     * @throws IOException when an I/O error occurs.
     */
    public StyledInput generateStyledInput() throws IOException {
        if (segments == null) {
            segments = new ArrayList<>();
            readFromReader(new StringReader(text));
        }
        return SegmentStyledInput.of(segments);
    }

    private static HashMap<String, String> initTextKeywords() {
        HashMap<String, String> m = new HashMap<>();
        m.put("\\", "\\");
        m.put("{", "{");
        m.put("}", "}");
        m.put(" ", "\u00A0"); /* not in the spec... */
        m.put("~", "\u00A0"); /* nonbreaking space */
        m.put("_", "\u2011"); /* nonbreaking hyphen */
        m.put("bullet", "\u2022");
        m.put("emdash", "\u2014");
        m.put("emspace", "\u2003");
        m.put("endash", "\u2013");
        m.put("enspace", "\u2002");
        m.put("ldblquote", "\u201C");
        m.put("lquote", "\u2018");
        m.put("ltrmark", "\u200E");
        m.put("rdblquote", "\u201D");
        m.put("rquote", "\u2019");
        m.put("rtlmark", "\u200F");
        m.put("tab", "\u0009");
        m.put("zwj", "\u200D");
        m.put("zwnj", "\u200C");
        // There is no Unicode equivalent to an optional hyphen, as far as I can tell.
        // TODO optional hyphen
        m.put("-", "\u2027");
        return m;
    }

    private static HashMap<String, char[]> initCharacterSets() {
        HashMap<String, char[]> m = new HashMap<>();
        m.put("ansicpg", latin1TranslationTable);
        return m;
    }

    private HashMap<String, Style> initStyles() {
        HashMap<String, Style> m = new HashMap<>();
        m.put(DEFAULT_STYLE, new Style());
        return m;
    }

    private Style addStyle(String nm, Style parent) {
        Style s = new Style();
        s.setResolveParent(parent);
        styles.put(nm, s);
        return s;
    }

    private Style getDefaultStyle() {
        return styles.get(DEFAULT_STYLE);
    }

    /**
     * Called when the RTFParser encounters a bin keyword in the RTF stream.
     */
    @Override
    public void handleBinaryBlob(byte[] data) {
        if (skippingCharacters > 0) {
            // a blob only counts as one character for skipping purposes
            skippingCharacters--;
            return;
        }

        // TODO
    }

    /**
     * Handles any pure text (containing no control characters) in the input
     * stream. Called by the superclass. */
    @Override
    public void handleText(String text) {
        if (skippingCharacters > 0) {
            if (skippingCharacters >= text.length()) {
                skippingCharacters -= text.length();
                return;
            } else {
                text = text.substring(skippingCharacters);
                skippingCharacters = 0;
            }
        }

        if (rtfDestination != null) {
            rtfDestination.handleText(text);
            return;
        }
    }

    /** Called by the superclass when a new RTF group is begun.
     *  This implementation saves the current <code>parserState</code>, and gives
     *  the current destination a chance to save its own state.
     * @see RTFParser#begingroup
     */
    @Override
    public void begingroup() {
        if (skippingCharacters > 0) {
            /* TODO this indicates an error in the RTF. Log it? */
            skippingCharacters = 0;
        }

        /* we do this little dance to avoid cloning the entire state stack and
           immediately throwing it away. */
        Object oldSaveState = parserState.get("_savedState");
        if (oldSaveState != null) {
            parserState.remove("_savedState");
        }
        @SuppressWarnings("unchecked")
        HashMap<String, Object> saveState = (HashMap<String, Object>)parserState.clone();
        if (oldSaveState != null) {
            saveState.put("_savedState", oldSaveState);
        }
        parserState.put("_savedState", saveState);

        if (rtfDestination != null) {
            rtfDestination.begingroup();
        }
    }

    /** Called by the superclass when the current RTF group is closed.
     *  This restores the parserState saved by <code>begingroup()</code>
     *  as well as invoking the endgroup method of the current
     *  destination.
     * @see RTFParser#endgroup
     */
    @Override
    public void endgroup() {
        if (skippingCharacters > 0) {
            /* NB this indicates an error in the RTF. Log it? */
            skippingCharacters = 0;
        }

        @SuppressWarnings("unchecked")
        HashMap<Object, Object> restoredState = (HashMap<Object, Object>)parserState.get("_savedState");
        Destination restoredDestination = (Destination)restoredState.get("dst");
        if (restoredDestination != rtfDestination) {
            rtfDestination.close(); /* allow the destination to clean up */
            rtfDestination = restoredDestination;
        }
        HashMap<Object, Object> oldParserState = parserState;
        parserState = restoredState;
        if (rtfDestination != null) {
            rtfDestination.endgroup(oldParserState);
        }
    }

    protected void setRTFDestination(Destination newDestination) {
        /* Check that setting the destination won't close the
           current destination (should never happen) */
        HashMap<Object, Object> previousState = (HashMap<Object,Object>)parserState.get("_savedState");
        if (previousState != null) {
            if (rtfDestination != previousState.get("dst")) {
                //warning("Warning, RTF destination overridden, invalid RTF.");
                rtfDestination.close();
            }
        }
        rtfDestination = newDestination;
        parserState.put("dst", rtfDestination);
    }

    /** Called by the user when there is no more input (<i>i.e.</i>,
     * at the end of the RTF file.)
     *
     * @see OutputStream#close
     */
    @Override
    public void close() throws IOException {
        // FIX remove this
//        Enumeration<Object> docProps = documentAttributes.getAttributeNames();
//        while (docProps.hasMoreElements()) {
//            Object propName = docProps.nextElement();
//            //target.putProperty(propName, documentAttributes.getAttribute(propName));
//        }

        super.close();
    }

    /**
     * Handles a parameterless RTF keyword. This is called by the superclass
     * (RTFParser) when a keyword is found in the input stream.
     *
     * @return true if the keyword is recognized and handled;
     *         false otherwise
     * @see RTFParser#handleKeyword
     */
    @Override
    public boolean handleKeyword(String keyword) {
        String item;
        boolean ignoreGroupIfUnknownKeywordSave = ignoreGroupIfUnknownKeyword;

        if (skippingCharacters > 0) {
            skippingCharacters--;
            return true;
        }

        ignoreGroupIfUnknownKeyword = false;

        if ((item = textKeywords.get(keyword)) != null) {
            handleText(item);
            return true;
        }

        if (keyword.equals("fonttbl")) {
            setRTFDestination(new FonttblDestination());
            return true;
        }

        if (keyword.equals("colortbl")) {
            setRTFDestination(new ColortblDestination());
            return true;
        }

        if (keyword.equals("stylesheet")) {
            setRTFDestination(new StylesheetDestination());
            return true;
        }

        if (keyword.equals("info")) {
            setRTFDestination(new Destination());
            return false;
        }

        if (keyword.equals("mac")) {
            setCharacterSet("mac");
            return true;
        }

        if (keyword.equals("ansi")) {
            setCharacterSet("ansi");
            return true;
        }

        if (keyword.equals("next")) {
            setCharacterSet("NeXT");
            return true;
        }

        if (keyword.equals("pc")) {
            setCharacterSet("cpg437"); /* IBM Code Page 437 */
            return true;
        }

        if (keyword.equals("pca")) {
            setCharacterSet("cpg850"); /* IBM Code Page 850 */
            return true;
        }

        if (keyword.equals("*")) {
            ignoreGroupIfUnknownKeyword = true;
            return true;
        }

        if (rtfDestination != null) {
            if (rtfDestination.handleKeyword(keyword)) {
                return true;
            }
        }

        // this point is reached only if the keyword is unrecognized
        // other destinations we don't understand and therefore ignore
        switch(keyword) {
        case "aftncn":
        case "aftnsep":
        case "aftnsepc":
        case "annotation":
        case "atnauthor":
        case "atnicn":
        case "atnid":
        case "atnref":
        case "atntime":
        case "atrfend":
        case "atrfstart":
        case "bkmkend":
        case "bkmkstart":
        case "datafield":
        case "do":
        case "dptxbxtext":
        case "falt":
        case "field":
        case "file":
        case "filetbl":
        case "fname":
        case "fontemb":
        case "fontfile":
        case "footer":
        case "footerf":
        case "footerl":
        case "footerr":
        case "footnote":
        case "ftncn":
        case "ftnsep":
        case "ftnsepc":
        case "header":
        case "headerf":
        case "headerl":
        case "headerr":
        case "keycode":
        case "nextfile":
        case "object":
        case "pict":
        case "pn":
        case "pnseclvl":
        case "pntxtb":
        case "pntxta":
        case "revtbl":
        case "rxe":
        case "tc":
        case "template":
        case "txe":
        case "xe":
            ignoreGroupIfUnknownKeywordSave = true;
            break;
        }

        if (ignoreGroupIfUnknownKeywordSave) {
            setRTFDestination(new Destination());
        }

        return false;
    }

    /**
     * Handles an RTF keyword and its integer parameter.
     * This is called by the superclass
     * (RTFParser) when a keyword is found in the input stream.
     *
     * @return true if the keyword is recognized and handled;
     *         false otherwise
     * @see RTFParser#handleKeyword
     */
    @Override
    public boolean handleKeyword(String keyword, int parameter) {
        boolean ignoreGroupIfUnknownKeywordSave = ignoreGroupIfUnknownKeyword;

        if (skippingCharacters > 0) {
            skippingCharacters--;
            return true;
        }

        ignoreGroupIfUnknownKeyword = false;

        if (keyword.equals("uc")) {
            /* count of characters to skip after a unicode character */
            parserState.put("UnicodeSkip", Integer.valueOf(parameter));
            return true;
        }
        if (keyword.equals("u")) {
            if (parameter < 0) {
                parameter = parameter + 65536;
            }
            handleText((char)parameter);
            Number skip = (Number)(parserState.get("UnicodeSkip"));
            if (skip != null) {
                skippingCharacters = skip.intValue();
            } else {
                skippingCharacters = 1;
            }
            return true;
        }

        if (keyword.equals("rtf")) {
            //rtfversion = parameter;
            setRTFDestination(new DocumentDestination());
            return true;
        }

        if (keyword.startsWith("NeXT") || keyword.equals("private")) {
            ignoreGroupIfUnknownKeywordSave = true;
        }

        if (keyword.contains("ansicpg")) {
            setCharacterSet("ansicpg");
            return true;
        }

        if (rtfDestination != null) {
            if (rtfDestination.handleKeyword(keyword, parameter)) {
                return true;
            }
        }

        // this point is reached only if the keyword is unrecognized
        if (ignoreGroupIfUnknownKeywordSave) {
            setRTFDestination(new Destination());
        }

        return false;
    }

    /**
     * setCharacterSet sets the current translation table to correspond with
     * the named character set. The character set is loaded if necessary.
     *
     * @see AbstractFilter
     */
    public void setCharacterSet(String name) {
        Object set;

        try {
            set = getCharacterSet(name);
        } catch (Exception e) {
            //warning("Exception loading RTF character set \"" + name + "\": " + e);
            set = null;
        }

        if (set != null) {
            translationTable = (char[])set;
        } else {
            //warning("Unknown RTF character set \"" + name + "\"");
            if (!name.equals("ansi")) {
                try {
                    translationTable = (char[])getCharacterSet("ansi");
                } catch (IOException e) {
                    throw new InternalError("RTFReader: Unable to find character set resources (" + e + ")", e);
                }
            }
        }
    }

    /** Adds a character set to the RTFReader's list
     *  of known character sets */
    private static void defineCharacterSet(String name, char[] table) {
        if (table.length < 256) {
            throw new IllegalArgumentException("Translation table must have 256 entries.");
        }
        characterSets.put(name, table);
    }

    /** Looks up a named character set. A character set is a 256-entry
     *  array of characters, mapping unsigned byte values to their Unicode
     *  equivalents. The character set is loaded if necessary.
     *
     *  @return the character set
     */
    public static Object getCharacterSet(final String name) throws IOException {
        char[] set = characterSets.get(name);
        if (set == null) {
            try (InputStream in = RTFReader.class.getResourceAsStream("charsets/" + name + ".txt")) {
                set = readCharset(in);
                defineCharacterSet(name, set);
            }
        }
        return set;
    }

    /** Parses a character set from an InputStream. The character set
     * must contain 256 decimal integers, separated by whitespace, with
     * no punctuation. B- and C- style comments are allowed.
     *
     * @return the newly read character set
     */
    static char[] readCharset(InputStream strm) throws IOException {
        char[] values = new char[256];

        try (BufferedReader rd = new BufferedReader(new InputStreamReader(strm, StandardCharsets.ISO_8859_1))) {
            StreamTokenizer in = new StreamTokenizer(rd);
            in.eolIsSignificant(false);
            in.commentChar('#');
            in.slashSlashComments(true);
            in.slashStarComments(true);

            int i = 0;
            while (i < 256) {
                int ttype;
                try {
                    ttype = in.nextToken();
                } catch (Exception e) {
                    throw new IOException("Unable to read from character set file (" + e + ")");
                }
                if (ttype != StreamTokenizer.TT_NUMBER) {
                    //          System.out.println("Bad token: type=" + ttype + " tok=" + in.sval);
                    throw new IOException("Unexpected token in character set file");
                    //          continue;
                }
                values[i] = (char)(in.nval);
                i++;
            }
        }

        return values;
    }

    /**
     * The base class for an RTF destination.
     * The RTF reader always has a current destination
     * which is where text is sent.  This class provides a discarding destination:
     * it accepts all keywords and text but does nothing with them.
     */
    static class Destination {
        public void handleBinaryBlob(byte[] data) {
        }

        public void handleText(String text) {
        }

        public boolean handleKeyword(String text) {
            /* Accept and discard keywords. */
            return true;
        }

        public boolean handleKeyword(String text, int parameter) {
            /* Accept and discard parameterized keywords. */
            return true;
        }

        public void begingroup() {
        }

        public void endgroup(Map<Object, Object> oldState) {
        }

        public void close() {
        }
    }

    /**
     * Reads the fonttbl group, inserting fonts into the RTFReader's fontTable map.
     */
    class FonttblDestination extends Destination {
        private int nextFontNumber;
        private Integer fontNumberKey;
        private String nextFontFamily;

        @Override
        public void handleText(String text) {
            int semicolon = text.indexOf(';');
            String fontName;

            if (semicolon > -1) {
                fontName = text.substring(0, semicolon);
            } else {
                fontName = text;
            }

            if (nextFontNumber == -1 && fontNumberKey != null) {
                //font name might be broken across multiple calls
                fontName = fontTable.get(fontNumberKey) + fontName;
            } else {
                fontNumberKey = Integer.valueOf(nextFontNumber);
            }
            fontTable.put(fontNumberKey, fontName);

            nextFontNumber = -1;
            nextFontFamily = null;
        }

        @Override
        public boolean handleKeyword(String keyword) {
            if (keyword.charAt(0) == 'f') {
                nextFontFamily = keyword.substring(1);
                return true;
            }
            return false;
        }

        @Override
        public boolean handleKeyword(String keyword, int parameter) {
            if (keyword.equals("f")) {
                nextFontNumber = parameter;
                return true;
            }
            return false;
        }
    }

    /**
     * Reads the colortbl group. Upon end-of-group, the RTFReader's
     * color table is set to an array containing the read colors.
     */
    class ColortblDestination extends Destination {
        private int red;
        private int green;
        private int blue;
        private final ArrayList<Color> colors = new ArrayList<>();

        public ColortblDestination() {
        }

        @Override
        public void handleText(String text) {
            for (int index = 0; index < text.length(); index++) {
                if (text.charAt(index) == ';') {
                    Color newColor;
                    newColor = Color.rgb(red, green, blue);
                    colors.add(newColor);
                }
            }
        }

        @Override
        public void close() {
            int sz = colors.size();
            colorTable = colors.toArray(new Color[sz]);
        }

        @Override
        public boolean handleKeyword(String keyword, int parameter) {
            switch (keyword) {
            case "red":
                red = parameter;
                return true;
            case "green":
                green = parameter;
                return true;
            case "blue":
                blue = parameter;
                return true;
            }
            return false;
        }

        @Override
        public boolean handleKeyword(String keyword) {
            // Colortbls don't understand any parameterless keywords
            return false;
        }
    }

    /**
     * Handles the stylesheet keyword. Styles are read and sorted
     * into the three style arrays in the RTFReader.
     */
    class StylesheetDestination extends Destination {
        private HashMap<Integer, StyleDefiningDestination> definedStyles = new HashMap<>();

        public StylesheetDestination() {
        }

        @Override
        public void begingroup() {
            setRTFDestination(new StyleDefiningDestination());
        }

        @Override
        public void close() {
            HashMap<Integer, Style> chrStyles = new HashMap<>();
            HashMap<Integer, Style> pgfStyles = new HashMap<>();
            HashMap<Integer, Style> secStyles = new HashMap<>();
            for (StyleDefiningDestination style : definedStyles.values()) {
                Style defined = style.realize();
                String stype = (String)defined.getAttribute(STYLE_TYPE);
                Map<Integer, Style> toMap;
                if (stype.equals(STYLE_SECTION)) {
                    toMap = secStyles;
                } else if (stype.equals(STYLE_CHARACTER)) {
                    toMap = chrStyles;
                } else {
                    toMap = pgfStyles;
                }
                toMap.put(style.number, defined);
            }
            if (!(chrStyles.isEmpty())) {
                characterStyles = chrStyles;
            }
            if (!(pgfStyles.isEmpty())) {
                paragraphStyles = pgfStyles;
            }
            if (!(secStyles.isEmpty())) {
                sectionStyles = secStyles;
            }
        }

        /** This subclass handles an individual style */
        class StyleDefiningDestination extends AttributeTrackingDestination {
            private static final int STYLENUMBER_NONE = 222;
            private boolean additive;
            private boolean characterStyle;
            private boolean sectionStyle;
            public String styleName;
            public int number;
            private int basedOn = STYLENUMBER_NONE;
            private int nextStyle = STYLENUMBER_NONE;
            private boolean hidden;
            private Style realizedStyle;

            @Override
            public void handleText(String text) {
                if (styleName != null) {
                    styleName = styleName + text;
                } else {
                    styleName = text;
                }
            }

            @Override
            public void close() {
                int semicolon = (styleName == null) ? 0 : styleName.indexOf(';');
                if (semicolon > 0) {
                    styleName = styleName.substring(0, semicolon);
                }
                definedStyles.put(Integer.valueOf(number), this);
                super.close();
            }

            @Override
            public boolean handleKeyword(String keyword) {
                switch (keyword) {
                case "additive":
                    additive = true;
                    return true;
                case "shidden":
                    hidden = true;
                    return true;
                }
                return super.handleKeyword(keyword);
            }

            @Override
            public boolean handleKeyword(String keyword, int parameter) {
                // As per http://www.biblioscape.com/rtf15_spec.htm#Heading2
                // we are restricting control word delimiter numeric value
                // to be within -32767 through 32767
                if (parameter > 32767) {
                    parameter = 32767;
                } else if (parameter < -32767) {
                    parameter = -32767;
                }

                switch (keyword) {
                case "s":
                    characterStyle = false;
                    sectionStyle = false;
                    number = parameter;
                    return true;
                case "cs":
                    characterStyle = true;
                    sectionStyle = false;
                    number = parameter;
                    return true;
                case "ds":
                    characterStyle = false;
                    sectionStyle = true;
                    number = parameter;
                    return true;
                case "sbasedon":
                    basedOn = parameter;
                    return true;
                case "snext":
                    nextStyle = parameter;
                    return true;
                }

                return super.handleKeyword(keyword, parameter);
            }

            public Style realize() {
                return realize(null);
            }

            private Style realize(Set<Integer> alreadyMetBasisIndexSet) {
                Style basis = null;
                Style next = null;

                if (alreadyMetBasisIndexSet == null) {
                    alreadyMetBasisIndexSet = new HashSet<>();
                }

                if (realizedStyle != null) {
                    return realizedStyle;
                }

                if (basedOn != STYLENUMBER_NONE && alreadyMetBasisIndexSet.add(basedOn)) {
                    StyleDefiningDestination styleDest;
                    styleDest = definedStyles.get(basedOn);
                    if (styleDest != null && styleDest != this) {
                        basis = styleDest.realize(alreadyMetBasisIndexSet);
                    }
                }

                /* NB: Swing StyleContext doesn't allow distinct styles with
                   the same name; RTF apparently does. This may confuse the
                   user. */
                realizedStyle = addStyle(styleName, basis);

                if (characterStyle) {
                    realizedStyle.addAttributes(currentTextAttributes());
                    realizedStyle.addAttribute(STYLE_TYPE, STYLE_CHARACTER);
                } else if (sectionStyle) {
                    realizedStyle.addAttributes(currentSectionAttributes());
                    realizedStyle.addAttribute(STYLE_TYPE, STYLE_SECTION);
                } else { /* must be a paragraph style */
                    realizedStyle.addAttributes(currentParagraphAttributes());
                    realizedStyle.addAttribute(STYLE_TYPE, STYLE_PARAGRAPH);
                }

                if (nextStyle != STYLENUMBER_NONE) {
                    StyleDefiningDestination styleDest;
                    styleDest = definedStyles.get(Integer.valueOf(nextStyle));
                    if (styleDest != null) {
                        next = styleDest.realize();
                    }
                }

                if (next != null) {
                    realizedStyle.addAttribute(STYLE_NEXT, next);
                }
                realizedStyle.addAttribute(STYLE_ADDITIVE, Boolean.valueOf(additive));
                realizedStyle.addAttribute(STYLE_HIDDEN, Boolean.valueOf(hidden));

                return realizedStyle;
            }
        }
    }

    /**
     * An abstract RTF destination which simply tracks the attributes specified by the RTF control words
     * in internal form and can produce acceptable attribute sets for the
     * current character, paragraph, and section attributes.
     * It is up to the subclasses to determine what is done with the actual text.
     */
    abstract class AttributeTrackingDestination extends Destination {
        @Override
        public abstract void handleText(String text);

        /** This is the "chr" element of parserState, cached for more efficient use */
        private AttrSet characterAttributes;
        /** This is the "pgf" element of parserState, cached for more efficient use */
        private AttrSet paragraphAttributes;
        /** This is the "sec" element of parserState, cached for more efficient use */
        private AttrSet sectionAttributes;

        public AttributeTrackingDestination() {
            characterAttributes = rootCharacterAttributes();
            parserState.put("chr", characterAttributes);
            paragraphAttributes = rootParagraphAttributes();
            parserState.put("pgf", paragraphAttributes);
            sectionAttributes = rootSectionAttributes();
            parserState.put("sec", sectionAttributes);
        }

        @Override
        public void begingroup() {
            AttrSet characterParent = currentTextAttributes();
            AttrSet paragraphParent = currentParagraphAttributes();
            AttrSet sectionParent = currentSectionAttributes();

            /* update the cached attribute dictionaries */
            characterAttributes = new AttrSet();
            characterAttributes.addAttributes(characterParent);
            parserState.put("chr", characterAttributes);

            paragraphAttributes = new AttrSet();
            paragraphAttributes.addAttributes(paragraphParent);
            parserState.put("pgf", paragraphAttributes);

            sectionAttributes = new AttrSet();
            sectionAttributes.addAttributes(sectionParent);
            parserState.put("sec", sectionAttributes);
        }

        @Override
        public void endgroup(Map<Object, Object> oldState) {
            characterAttributes = (AttrSet)parserState.get("chr");
            paragraphAttributes = (AttrSet)parserState.get("pgf");
            sectionAttributes = (AttrSet)parserState.get("sec");
        }

        @Override
        public boolean handleKeyword(String keyword) {
            if (keyword.equals("ulnone")) {
                return handleKeyword("ul", 0);
            }

            {
                RTFAttribute attr = straightforwardAttributes.get(keyword);
                if (attr != null) {
                    boolean ok;

                    switch (attr.domain()) {
                    case RTFAttribute.D_CHARACTER:
                        ok = attr.set(characterAttributes);
                        break;
                    case RTFAttribute.D_PARAGRAPH:
                        ok = attr.set(paragraphAttributes);
                        break;
                    case RTFAttribute.D_SECTION:
                        ok = attr.set(sectionAttributes);
                        break;
                    case RTFAttribute.D_META:
                        holder.backing = parserState;
                        ok = attr.set(holder);
                        holder.backing = null;
                        break;
                    case RTFAttribute.D_DOCUMENT:
                        ok = attr.set(documentAttributes);
                        break;
                    default:
                        // should never happen
                        ok = false;
                        break;
                    }
                    if (ok) {
                        return true;
                    }
                }
            }

            switch (keyword) {
            case "plain":
                resetCharacterAttributes();
                return true;
            case "pard":
                resetParagraphAttributes();
                return true;
            case "sectd":
                resetSectionAttributes();
                return true;
            }

            return false;
        }

        @Override
        public boolean handleKeyword(String keyword, int parameter) {
            if (keyword.equals("fc")) {
                keyword = "cf";
            }

            switch (keyword) {
            case "f":
                parserState.put(keyword, Integer.valueOf(parameter));
                return true;
            case "cf":
                parserState.put(keyword, Integer.valueOf(parameter));
                return true;
            case "cb":
                parserState.put(keyword, Integer.valueOf(parameter));
                return true;
            }

            {
                RTFAttribute attr = straightforwardAttributes.get(keyword);
                if (attr != null) {
                    boolean ok;

                    switch (attr.domain()) {
                    case RTFAttribute.D_CHARACTER:
                        ok = attr.set(characterAttributes, parameter);
                        break;
                    case RTFAttribute.D_PARAGRAPH:
                        ok = attr.set(paragraphAttributes, parameter);
                        break;
                    case RTFAttribute.D_SECTION:
                        ok = attr.set(sectionAttributes, parameter);
                        break;
                    case RTFAttribute.D_META:
                        holder.backing = parserState;
                        ok = attr.set(holder, parameter);
                        holder.backing = null;
                        break;
                    case RTFAttribute.D_DOCUMENT:
                        ok = attr.set(documentAttributes, parameter);
                        break;
                    default:
                        // should never happen
                        ok = false;
                        break;
                    }
                    if (ok) {
                        return true;
                    }
                }
            }

            /* TODO: superscript/subscript */

            // TODO
//            if (keyword.equals("sl")) {
//                if (parameter == 1000) { /* magic value! */
//                    characterAttributes.removeAttribute(StyleConstants.LineSpacing);
//                } else {
//                    /* TODO: The RTF sl attribute has special meaning if it's
//                       negative. Make sure that SwingText has the same special
//                       meaning, or find a way to imitate that. When SwingText
//                       handles this, also recognize the slmult keyword. */
//                    StyleConstants.setLineSpacing(characterAttributes, parameter / 20f);
//                }
//                return true;
//            }

            /* TODO: Other kinds of underlining */

//            if (keyword.equals("tx") || keyword.equals("tb")) {
//                float tabPosition = parameter / 20f;
//                int tabAlignment, tabLeader;
//                Number item;
//
//                tabAlignment = TabStop.ALIGN_LEFT;
//                item = (Number)(parserState.get("tab_alignment"));
//                if (item != null)
//                    tabAlignment = item.intValue();
//                tabLeader = TabStop.LEAD_NONE;
//                item = (Number)(parserState.get("tab_leader"));
//                if (item != null)
//                    tabLeader = item.intValue();
//                if (keyword.equals("tb"))
//                    tabAlignment = TabStop.ALIGN_BAR;
//
//                parserState.remove("tab_alignment");
//                parserState.remove("tab_leader");
//
//                TabStop newStop = new TabStop(tabPosition, tabAlignment, tabLeader);
//                Dictionary<Object, Object> tabs;
//                Integer stopCount;
//
//                @SuppressWarnings("unchecked")
//                Dictionary<Object, Object> tmp = (Dictionary)parserState.get("_tabs");
//                tabs = tmp;
//                if (tabs == null) {
//                    tabs = new Hashtable<Object, Object>();
//                    parserState.put("_tabs", tabs);
//                    stopCount = Integer.valueOf(1);
//                } else {
//                    stopCount = (Integer)tabs.get("stop count");
//                    stopCount = Integer.valueOf(1 + stopCount.intValue());
//                }
//                tabs.put(stopCount, newStop);
//                tabs.put("stop count", stopCount);
//                parserState.remove("_tabs_immutable");
//
//                return true;
//            }

            switch (keyword) {
            case "fs":
                characterAttributes.addAttribute(StyleAttributeMap.FONT_SIZE, (parameter / 2));
                return true;
            }

            if (keyword.equals("s") && paragraphStyles != null) {
                parserState.put("paragraphStyle", paragraphStyles.get(parameter));
                return true;
            }

            if (keyword.equals("cs") && characterStyles != null) {
                parserState.put("characterStyle", characterStyles.get(parameter));
                return true;
            }

            if (keyword.equals("ds") && sectionStyles != null) {
                parserState.put("sectionStyle", sectionStyles.get(parameter));
                return true;
            }

            return false;
        }

        /** Returns a new AttrSet containing the
         *  default character attributes */
        protected AttrSet rootCharacterAttributes() {
            AttrSet a = new AttrSet();
            /* TODO: default font */
            a.setItalic(false);
            a.setBold(false);
            a.setUnderline(false);
            a.setForeground(Color.BLACK);
            return a;
        }

        /** Returns a new AttrSet containing the
         *  default paragraph attributes */
        protected AttrSet rootParagraphAttributes() {
            AttrSet a = new AttrSet();
            a.setLeftIndent(0.0);
            a.setRightIndent(0.0);
            a.setFirstLineIndent(0.0);
            a.setResolveParent(getDefaultStyle());
            return a;
        }

        /** Returns a new AttrSet containing the
         *  default section attributes */
        protected AttrSet rootSectionAttributes() {
            AttrSet a = new AttrSet();
            return a;
        }

        /**
         * Calculates the current text (character) attributes in a form suitable
         * for SwingText from the current parser state.
         *
         * @return a new AttrSet containing the text attributes.
         */
        AttrSet currentTextAttributes() {
            AttrSet attributes = new AttrSet(characterAttributes);

            /* figure out the font name */
            /* TODO: catch exceptions for undefined attributes,
               bad font indices, etc.? (as it stands, it is the caller's
               job to clean up after corrupt RTF) */
            Integer fontnum = (Integer)parserState.get("f");
            /* note setFontFamily() can not handle a null font */
            String fontFamily;
            if (fontnum != null) {
                fontFamily = fontTable.get(fontnum);
            } else {
                fontFamily = null;
            }
            attributes.setFontFamily(fontFamily);

            if (colorTable != null) {
                Integer stateItem = (Integer)parserState.get("cf");
                if (stateItem != null) {
                    Color fg = colorTable[stateItem.intValue()];
                    attributes.setForeground(fg);
                } else {
                    attributes.setForeground(null);
                }
            }

            if (colorTable != null) {
                Integer stateItem = (Integer)parserState.get("cb");
                if (stateItem != null) {
                    Color bg = colorTable[stateItem.intValue()];
                    attributes.setBackground(bg);
                } else {
                    attributes.setBackground(null);
                }
            }

            Style characterStyle = (Style)parserState.get("characterStyle");
            if (characterStyle != null) {
                attributes.setResolveParent(characterStyle);
            }

            /* Other attributes are maintained directly in "attributes" */

            return attributes;
        }

        /**
         * Calculates the current paragraph attributes (with keys
         * as given in StyleConstants) from the current parser state.
         *
         * @return a newly created AttrSet.
         */
        AttrSet currentParagraphAttributes() {
            /* NB if there were a mutableCopy() method we should use it */
            AttrSet a = new AttrSet(paragraphAttributes);

            /*** Tab stops ***/
//            TabStop[] tabs = (TabStop[])parserState.get("_tabs_immutable");
//            if (tabs == null) {
//                @SuppressWarnings("unchecked")
//                Dictionary<Object, Object> workingTabs = (Dictionary)parserState.get("_tabs");
//                if (workingTabs != null) {
//                    int count = ((Integer)workingTabs.get("stop count")).intValue();
//                    tabs = new TabStop[count];
//                    for (int ix = 1; ix <= count; ix++)
//                        tabs[ix - 1] = (TabStop)workingTabs.get(Integer.valueOf(ix));
//                    parserState.put("_tabs_immutable", tabs);
//                }
//            }
//            if (tabs != null) {
//                a.addAttribute(Constants.Tabs, tabs);
//            }

            Style paragraphStyle = (Style)parserState.get("paragraphStyle");
            if (paragraphStyle != null) {
                a.setResolveParent(paragraphStyle);
            }

            return a;
        }

        /**
         * Calculates the current section attributes
         * from the current parser state.
         *
         * @return a newly created AttrSet.
         */
        public AttrSet currentSectionAttributes() {
            AttrSet attributes = new AttrSet(sectionAttributes);

            Style sectionStyle = (Style)parserState.get("sectionStyle");
            if (sectionStyle != null) {
                attributes.setResolveParent(sectionStyle);
            }

            return attributes;
        }

        /** Resets the filter's internal notion of the current character
         *  attributes to their default values. Invoked to handle the
         *  \plain keyword. */
        protected void resetCharacterAttributes() {
            handleKeyword("f", 0);
            handleKeyword("cf", 0);
            handleKeyword("fs", 24); /* 12 pt. */

            for (RTFAttribute attr : straightforwardAttributes.values()) {
                if (attr.domain() == RTFAttribute.D_CHARACTER) {
                    attr.setDefault(characterAttributes);
                }
            }

            handleKeyword("sl", 1000);

            parserState.remove("characterStyle");
        }

        /** Resets the filter's internal notion of the current paragraph's
         *  attributes to their default values. Invoked to handle the
         *  \pard keyword. */
        protected void resetParagraphAttributes() {
            parserState.remove("_tabs");
            parserState.remove("_tabs_immutable");
            parserState.remove("paragraphStyle");

            paragraphAttributes.setAlignment(TextAlignment.LEFT);

            for (RTFAttribute attr : straightforwardAttributes.values()) {
                if (attr.domain() == RTFAttribute.D_PARAGRAPH) {
                    attr.setDefault(characterAttributes);
                }
            }
        }

        /** Resets the filter's internal notion of the current section's
         *  attributes to their default values. Invoked to handle the
         *  \sectd keyword. */
        protected void resetSectionAttributes() {
            for (RTFAttribute attr : straightforwardAttributes.values()) {
                if (attr.domain() == RTFAttribute.D_SECTION) {
                    attr.setDefault(characterAttributes);
                }
            }

            parserState.remove("sectionStyle");
        }
    }

    /**
     * This Destination accumulates the styled segments within this reader.
     */
    class DocumentDestination extends AttributeTrackingDestination {
        /** <code>true</code> if the reader has not just finished a paragraph; false upon startup */
        private boolean inParagraph;

        public DocumentDestination() {
        }

        public void deliverText(String text, AttrSet characterAttributes) {
            StyleAttributeMap a = characterAttributes.getStyleAttributeMap();
            StyledSegment seg = StyledSegment.of(text, a);
            segments.add(seg);
        }

        public void finishParagraph(AttrSet pgfAttributes, AttrSet chrAttributes) {
            // characterAttributes are ignored here
            // TODO we could supply paragraph attributes either
            // with a special StyledSegment (before the paragraph starts), or
            // as a part of insertLineBreak.  but for now, let's ignore them all
            // TODO pgfAttributes
            segments.add(StyledSegment.LINE_BREAK);
        }

        protected void endSection() {
            // no-op
        }

        @Override
        public void handleText(String text) {
            if (!inParagraph) {
                beginParagraph();
            }
            deliverText(text, currentTextAttributes());
        }

        @Override
        public void close() {
            if (inParagraph) {
                endParagraph();
            }
            super.close();
        }

        @Override
        public boolean handleKeyword(String keyword) {
            switch (keyword) {
            case "\r":
            case "\n":
            case "par":
                endParagraph();
                return true;
            case "sect":
                endSection();
                return true;
            }

            return super.handleKeyword(keyword);
        }

        protected void beginParagraph() {
            inParagraph = true;
        }

        protected void endParagraph() {
            AttrSet pgfAttributes = currentParagraphAttributes();
            AttrSet chrAttributes = currentTextAttributes();
            finishParagraph(pgfAttributes, chrAttributes);
            inParagraph = false;
        }
    }
}
