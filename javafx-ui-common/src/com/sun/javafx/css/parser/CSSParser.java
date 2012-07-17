/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css.parser;

import com.sun.javafx.css.*;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EffectConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.InsetsConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.converters.SizeConverter.SequenceConverter;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.css.converters.URLConverter;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.layout.region.BackgroundImage;
import com.sun.javafx.scene.layout.region.BorderImage;
import com.sun.javafx.scene.layout.region.BorderStyle;
import com.sun.javafx.scene.layout.region.Margins;
import com.sun.javafx.scene.layout.region.Repeat;
import com.sun.javafx.scene.layout.region.StrokeBorder;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import javafx.collections.ObservableList;
import javafx.scene.Node;

final public class CSSParser {
    static boolean EXIT_ON_ERROR = false;

    /* Lazy instantiation */
    private static class InstanceHolder {
        final static CSSParser INSTANCE = new CSSParser();
    }

    public static CSSParser getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private CSSParser() {
        properties = new HashMap<String,String>();
    }

    // stylesheet as a string from parse method. This will be null if the
    // stylesheet is being parsed from a file; otherwise, the parser is parsing
    // a string and this is that string.    
    private String     stylesheetAsText;
    
    // the url of the stylesheet file, or the docbase of an applet. This will
    // be null if the source is not a file or from an applet.
    private URL        sourceOfStylesheet; 
    
    // the Styleable from the node with an in-line style. This will be null
    // unless the source of the styles is a Node's styleProperty. In this case,
    // the stylesheetString will also be set.
    private Styleable  sourceOfInlineStyle;
    
    // source is a file
    private void setInputSource(URL url) {
        setInputSource(url, null);
    }
    
    // source is a file
    private void setInputSource(URL url, String str) {
        stylesheetAsText = str;
        sourceOfStylesheet = url;        
        sourceOfInlineStyle = null;
    }
    
    // source as string only
    private void setInputSource(String str) {
        stylesheetAsText = str;
        sourceOfStylesheet = null;        
        sourceOfInlineStyle = null;        
    }
    
    // source is in-line style
    private void setInputSource(Styleable styleable) {
        stylesheetAsText = styleable != null ? styleable.getStyle() : null;
        sourceOfStylesheet = null;
        sourceOfInlineStyle = styleable;
    }
    
    private static final PlatformLogger LOGGER;
    static {
        LOGGER = com.sun.javafx.Logging.getCSSLogger();
        final int level = LOGGER.getLevel();
        if (LOGGER.getLevel() > PlatformLogger.WARNING &&
            LOGGER.getLevel() != PlatformLogger.OFF) {
            LOGGER.setLevel(PlatformLogger.WARNING);
        }
    }
    
    private static final class ParseException extends Exception {
        ParseException(String message) {
            this(message,null,null);
        }
        ParseException(String message, Token tok, CSSParser parser) {
            super(message);
            this.tok = tok;
            if (parser.sourceOfStylesheet != null) {
                source = parser.sourceOfStylesheet.toExternalForm();
            } else if (parser.sourceOfInlineStyle != null) {
                source = parser.sourceOfInlineStyle.toString();
            } else if (parser.stylesheetAsText != null) {
                source = parser.stylesheetAsText;
            } else {
                source = "?";
            }
        }
        @Override public String toString() {
            StringBuilder builder = new StringBuilder(super.getMessage());
            builder.append(source);
            if (tok != null) builder.append(": ").append(tok.toString());
            return builder.toString();
        }
        private final Token tok;
        private final String source;
    }

    /**
     * Creates a stylesheet from a CSS document string.
     *
     *@param stylesheetText the CSS document to parse
     *@return the Stylesheet
     */
    public Stylesheet parse(final String stylesheetText) {
        final Stylesheet stylesheet = new Stylesheet();
        if (stylesheetText != null && !stylesheetText.trim().isEmpty()) {
            setInputSource(stylesheetText);
            Reader reader = new CharArrayReader(stylesheetText.toCharArray());
            parse(stylesheet, reader);
        }
        return stylesheet;
    }
    
    /**
     * Creates a stylesheet from a CSS document string using docbase as
     * the base URL for resolving references within stylesheet.
     *
     *@param stylesheetText the CSS document to parse
     *@return the Stylesheet
     */
    public Stylesheet parse(final String docbase, final String stylesheetText) throws IOException {
        final URL url =  new URL(docbase);
        final Stylesheet stylesheet = new Stylesheet(url);
        if (stylesheetText != null && !stylesheetText.trim().isEmpty()) {
            setInputSource(url, stylesheetText);
            Reader reader = new CharArrayReader(stylesheetText.toCharArray());
            parse(stylesheet, reader);
        }
        return stylesheet;
    }

    /**
     * Updates the given stylesheet by reading a CSS document from a URL,
     * assuming UTF-8 encoding.
     *
     *@param the URL to parse
     *@return the stylesheet
     *@throws IOException
     */
    public Stylesheet parse(final URL url) throws IOException {
        
        final Stylesheet stylesheet = new Stylesheet(url);
        if (url != null) {
            setInputSource(url);
            Reader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            parse(stylesheet, reader);
        }
        return stylesheet;
    }

    /* All of the other function calls should wind up here */
    private void parse(final Stylesheet stylesheet, final Reader reader) {

        CSSLexer lex = CSSLexer.getInstance();
        lex.setReader(reader);

        try {
            this.parse(stylesheet, lex);
            reader.close();
        } catch (IOException ioe) {
        } catch (Exception ex) {
            // Sometimes bad syntax causes an exception. The code should be 
            // fixed to handle the bad syntax, but the fallback is 
            // to handle the exception here. Uncaught, the exception can cause 
            // problems like RT-20311
            reportException(ex);
        }

    }

    /** Parse an in-line style from a Node */
    public Stylesheet parseInlineStyle(final Styleable node) {
        
        Stylesheet stylesheet = new Stylesheet();
        
        final String stylesheetText = (node != null) ? node.getStyle() : null;
        if (stylesheetText != null && !stylesheetText.trim().isEmpty()) {
            setInputSource(node);
            final List<Rule> rules = new ArrayList<Rule>();
            final Reader reader = new CharArrayReader(stylesheetText.toCharArray());
            final CSSLexer lexer = CSSLexer.getInstance();
            lexer.setReader(reader);
            try {
                currentToken = nextToken(lexer);
                final List<Declaration> declarations = declarations(lexer);
                if (declarations != null && !declarations.isEmpty()) {
                    final Selector selector = Selector.getUniversalSelector();
                    final Rule rule = new Rule(
                        Collections.singletonList(selector),
                        declarations
                    );
                    rules.add(rule);
                }
                reader.close();
            } catch (IOException ioe) {
            } catch (Exception ex) {
                // Sometimes bad syntax causes an exception. The code should be 
                // fixed to handle the bad syntax, but the fallback is 
                // to handle the exception here. Uncaught, the exception can cause 
                // problems like RT-20311
                reportException(ex);
            }
            stylesheet.getRules().addAll(rules);
        }

        // don't retain reference to the styleable
        setInputSource((Styleable) null);

        return stylesheet;
    }

    /** convenience method for unit tests */
    public ParsedValue parseExpr(String property, String expr) {
        ParsedValue value = null;
        try {
            setInputSource(null, property + ": " + expr);
            char buf[] = new char[expr.length() + 1];
            System.arraycopy(expr.toCharArray(), 0, buf, 0, expr.length());
            buf[buf.length-1] = ';';

            Reader reader = new CharArrayReader(buf);
            CSSLexer lex = CSSLexer.getInstance();
            lex.setReader(reader);

            currentToken = nextToken(lex);
            CSSParser.Term term = this.expr(lex);
            value = valueFor(property, term);

            reader.close();
        } catch (IOException ioe) {
        } catch (ParseException e) {
            if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                LOGGER.warning("\"" +property + ": " + expr  + "\" " + e.toString());
            }
        } catch (Exception ex) {
            // Sometimes bad syntax causes an exception. The code should be 
            // fixed to handle the bad syntax, but the fallback is 
            // to handle the exception here. Uncaught, the exception can cause 
            // problems like RT-20311
            reportException(ex);
        }
        return value;
    }
    /*
     * Map of property names found while parsing. If a value matches a
     * property name, then the value is a lookup.
     */
    private final Map<String,String> properties;

    /*
     * While parsing a declaration, tokens from parsing value (that is,
     * the expr rule) are held in this tree structure which is then passed
     * to methods which convert the tree into a ParsedValue.
     *
     * Each term in expr is a Term. For simple terms, like HASH, the
     * Term is just the Token. If the term is a function, then the
     * Term is a linked-list of Term, the first being the function
     * name and each nextArg being the arguments.
     *
     * If there is more than one term in the expr (insets, for example),
     * then the terms are linked on nextInSequence. If there is more than one
     * layer (sequence of terms), then each layer becomes the nextLayer
     * to the last root in the previous sequence.
     *
     * The easiest way to think of it is that a comma starts a nextLayer (except
     * when a function arg).
     *
     * The expr part of the declaration "-fx-padding 1 2, 3 4;" would look
     * like this:
     * [1 | nextLayer | nextInSeries]-->[2 | nextLayer | nextInSeries]-->null
     *            |                            |
     *          null                           |
     *       .---------------------------------'
     *       '-->[3 | nextLayer | nextInSeries]-->[4 | nextLayer | nextInSeries]-->null
     *                    |                              |
     *                   null                           null
     *
     * The first argument in a function needs to be distinct from the
     * remaining args so that the args of a function in the middle of
     * a function will not be omitted. Consider 'f0(a, f1(b, c), d)'
     * If we relied only on nextArg, then the next arg of f0 would be a but
     * the nextArg of f1 would be d. With firstArg, the firstArg of f0 is a,
     * the nextArg of a is f1, the firstArg of f1 is b and the nextArg of f1 is d.
     *
     * TODO: now that the parser is the parser and not an adjunct to an ANTLR
     * parser, this Term stuff shouldn't be needed.
     */
    static class Term {
    	final Token token;
    	Term nextInSeries;
    	Term nextLayer;
        Term firstArg;
        Term nextArg;
    	Term(Token token) {
            this.token = token;
            this.nextLayer = null;
            this.nextInSeries = null;
            this.firstArg = null;
            this.nextArg = null;
    	}
    	Term() {
            this(null);
    	}

    	@Override public String toString() {
            StringBuilder buf = new StringBuilder();
            if (token != null) buf.append(String.valueOf(token.getText()));
            if (nextInSeries != null) {
                buf.append("<nextInSeries>");
                buf.append(nextInSeries.toString());
                buf.append("</nextInSeries>\n");
            }
            if (nextLayer != null) {
                buf.append("<nextLayer>");
                buf.append(nextLayer.toString());
                buf.append("</nextLayer>\n");
            }
            if (firstArg != null) {
                buf.append("<args>");
                buf.append(firstArg.toString());
                if (nextArg != null) {
                    buf.append(nextArg.toString());
                }
                buf.append("</args>");
            }

            return buf.toString();
    	}
        
    }

    private CssError createError(String msg) {
        
        CssError error = null; 
        if (sourceOfStylesheet != null) {
            error = new CssError.StylesheetParsingError(sourceOfStylesheet, msg);
        } else if (sourceOfInlineStyle != null) {
            error = new CssError.InlineStyleParsingError(sourceOfInlineStyle, msg);
        } else {
            error = new CssError.StringParsingError(stylesheetAsText, msg);
        }
        return error;
    }
    
    private void reportError(CssError error) {
        List<CssError> errors = null;
        if ((errors = StyleManager.getInstance().getErrors()) != null) {        
            errors.add(error);
        }
    }
    
    private void error(final Term root, final String msg) throws ParseException {

        final Token token = root != null ? root.token : null;
        final ParseException pe = new ParseException(msg,token,this);
        reportError(createError(pe.toString()));
        throw pe;
    }

    private void reportException(Exception exception) {
        
        if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
            final StackTraceElement[] stea = exception.getStackTrace();
            if (stea.length > 0) {
                final StringBuilder buf = 
                    new StringBuilder("Please report ");
                buf.append(exception.getClass().getName())
                   .append(" at:");
                int end = 0;
                while(end < stea.length) {
                    // only report parser part of the stack trace.
                    if (!getClass().getName().equals(stea[end].getClassName())) {
                        break;
                    }
                    buf.append("\n\t")
                    .append(stea[end++].toString());
                }
                LOGGER.warning(buf.toString());
            }
        }
    }
    
    private String formatDeprecatedMessage(final Term root, final String syntax) {
        final StringBuilder buf = 
            new StringBuilder("Using deprecated syntax for ");
        buf.append(syntax);
        if (sourceOfStylesheet != null){
            buf.append(" at ")
               .append(sourceOfStylesheet)
               .append("[")
               .append(root.token.getLine())
               .append(',')
               .append(root.token.getOffset())
               .append("]");
        }
        buf.append(". Refer to the CSS Reference Guide.");
        return buf.toString();
    }
    
   // Assumes string is not a lookup!
   private ParsedValue<Color,Color> colorValueOfString(String str) {

        if(str.startsWith("#") || str.startsWith("0x")) {

            double a = 1.0f;
            String c = str;
            final int prefixLength = (str.startsWith("#")) ? 1 : 2;

            final int len = c.length();
            // rgba or rrggbbaa - trim off the alpha
            if ( (len-prefixLength) == 4) {
                a = Integer.parseInt(c.substring(len-1), 16) / 15.0f;
                c = c.substring(0,len-1);
            } else if ((len-prefixLength) == 8) {
                a = Integer.parseInt(c.substring(len-2), 16) / 255.0f;
                c = c.substring(0,len-2);
            }
            // else color was rgb or rrggbb (no alpha)
            return new ParsedValue<Color,Color>(Color.web(c,a), null);
        }

        try {
            return new ParsedValue<Color,Color>(Color.web(str), null);
        } catch (final IllegalArgumentException e) {
        } catch (final NullPointerException e) {
        }

        // not a color
        return null;
    }

    private String stripQuotes(String string) {
        return com.sun.javafx.Utils.stripQuotes(string);
    }

    private double clamp(double min, double val, double max) {
        if (val < min) return min;
        if (max < val) return max;
        return val;
    }

    // Return true if the token is a size type or an identifier
    // (which would indicate a lookup).
    private boolean isSize(Token token) {
        final int ttype = token.getType();
        switch (ttype) {
        case CSSLexer.NUMBER:
        case CSSLexer.PERCENTAGE:
        case CSSLexer.EMS:
        case CSSLexer.EXS:
        case CSSLexer.PX:
        case CSSLexer.CM:
        case CSSLexer.MM:
        case CSSLexer.IN:
        case CSSLexer.PT:
        case CSSLexer.PC:
        case CSSLexer.DEG:
        case CSSLexer.GRAD:
        case CSSLexer.RAD:
        case CSSLexer.TURN:
            return true;
        default:
            return token.getType() == CSSLexer.IDENT;
        }
    }

    private Size size(final Token token) throws ParseException {
        SizeUnits units = SizeUnits.PX;
        // Amount to trim off the suffix, if any. Most are 2 chars.
        int trim = 2;
        final String sval = token.getText().trim();
        final int len = sval.length();
        final int ttype = token.getType();
        switch (ttype) {
        case CSSLexer.NUMBER:
            units = SizeUnits.PX;
            trim = 0;
            break;
        case CSSLexer.PERCENTAGE:
            units = SizeUnits.PERCENT;
            trim = 1;
            break;
        case CSSLexer.EMS:
            units = SizeUnits.EM;
            break;
        case CSSLexer.EXS:
            units = SizeUnits.EX;
            break;
        case CSSLexer.PX:
            units = SizeUnits.PX;
            break;
        case CSSLexer.CM:
            units = SizeUnits.CM;
            break;
        case CSSLexer.MM:
            units = SizeUnits.MM;
            break;
        case CSSLexer.IN:
            units = SizeUnits.IN;
            break;
        case CSSLexer.PT:
            units = SizeUnits.PT;
            break;
        case CSSLexer.PC:
            units = SizeUnits.PC;
            break;
        case CSSLexer.DEG:
            units = SizeUnits.DEG;
            trim = 3;
            break;
        case CSSLexer.GRAD:
            units = SizeUnits.GRAD;
            trim = 4;
            break;
        case CSSLexer.RAD:
            units = SizeUnits.RAD;
            trim = 3;
            break;
        case CSSLexer.TURN:
            units = SizeUnits.TURN;
            trim = 5;
            break;
        default:
            if (LOGGER.isLoggable(PlatformLogger.FINEST)) {
                LOGGER.finest("Expected \'<number>\'");
            }
            ParseException re = new ParseException("Expected \'<number>\'",token, this);
            reportError(createError(re.toString()));
            throw re;
        }
        // TODO: Handle NumberFormatException
        return new Size(
            Double.parseDouble(sval.substring(0,len-trim)),
            units
        );
    }

    // Count the number of terms in a series
    private int numberOfTerms(final Term root) {
        if (root == null) return 0;

        int nTerms = 0;
        Term term = root;
        do {
            nTerms += 1;
            term = term.nextInSeries;
        } while (term != null);
        return nTerms;
    }

    // Count the number of series of terms
    private int numberOfLayers(final Term root) {
        if (root == null) return 0;

        int nLayers = 0;
        Term term = root;
        do {
            nLayers += 1;
            while (term.nextInSeries != null) {
                term = term.nextInSeries;
            }
            term = term.nextLayer;
        } while (term != null);
        return nLayers;
    }

    // Count the number of args of terms. root is the function.
    private int numberOfArgs(final Term root) {
        if (root == null) return 0;

        int nArgs = 0;
        Term term = root.firstArg;
        while (term != null) {
            nArgs += 1;
            term = term.nextArg;
        }
        return nArgs;
    }

    // Get the next layer following this term, which may be null
    private Term nextLayer(final Term root) {
        if (root == null) return null;

        Term term = root;
        while (term.nextInSeries != null) {
            term = term.nextInSeries;
        }
        return term.nextLayer;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Parsing routines
    //
    ////////////////////////////////////////////////////////////////////////////

    ParsedValue valueFor(String property, Term root) throws ParseException {
        final String prop = property.toLowerCase();
        properties.put(prop, prop);
        if (root == null || root.token == null) {
            error(root, "Expected value for property \'" + prop + "\'");
        }

        if (root.token.getType() == CSSLexer.IDENT) {
            if ("inherit".equalsIgnoreCase(root.token.getText())) {
                return new ParsedValue<String,String>("inherit", null);
            } else if ("null".equalsIgnoreCase(root.token.getText())) {
                return new ParsedValue<String,String>("null", null);
            }
        }

        if ("-fx-background-color".equals(property)) {
            return parsePaintLayers(root);
        } else if ("-fx-background-image".equals(prop)) {
            return parseURILayers(root);
        } else if ("-fx-background-insets".equals(prop)) {
             return parseInsetsLayers(root);
        } else if ("-fx-background-position".equals(prop)) {
             return parseBackgroundPositionLayers(root);
        } else if ("-fx-background-radius".equals(prop)) {
             return parseInsetsLayers(root);
        } else if ("-fx-background-repeat".equals(prop)) {
             return parseBackgroundRepeatStyleLayers(root);
        } else if ("-fx-background-size".equals(prop)) {
             return parseBackgroundSizeLayers(root);
        } else if ("-fx-border-color".equals(prop)) {
             return parseBorderPaintLayers(root);
        } else if ("-fx-border-insets".equals(prop)) {
             return parseInsetsLayers(root);
        } else if ("-fx-border-radius".equals(prop)) {
             return parseMarginsLayers(root);
        } else if ("-fx-border-style".equals(prop)) {
             return parseBorderStyleLayers(root);
        } else if ("-fx-border-width".equals(prop)) {
             return parseMarginsLayers(root);
        } else if ("-fx-border-image-insets".equals(prop)) {
             return parseInsetsLayers(root);
        } else if ("-fx-border-image-repeat".equals(prop)) {
             return parseBorderImageRepeatStyleLayers(root);
        } else if ("-fx-border-image-slice".equals(prop)) {
             return parseBorderImageSliceLayers(root);
        } else if ("-fx-border-image-source".equals(prop)) {
             return parseURILayers(root);
        } else if ("-fx-border-image-width".equals(prop)) {
             return parseMarginsLayers(root);
        } else if ("-fx-padding".equals(prop)) {
            ParsedValue<?,Size>[] sides = parseSizeSeries(root);
            return new ParsedValue<ParsedValue<?,Size>[],Insets>(sides, InsetsConverter.getInstance());
        } else if ("-fx-label-padding".equals(prop)) {
            ParsedValue<?,Size>[] sides = parseSizeSeries(root);
            return new ParsedValue<ParsedValue<?,Size>[],Insets>(sides, InsetsConverter.getInstance());
        } else if (prop.endsWith("font-family")) {
            return parseFontFamily(root);
        } else if (prop.endsWith("font-size")) {
            ParsedValue fsize = parseFontSize(root);
            if (fsize == null) error(root, "Expected \'<font-size>\'");
            return fsize;
        } else if (prop.endsWith("font-style")) {
            ParsedValue fstyle = parseFontStyle(root);
            if (fstyle == null) error(root, "Expected \'<font-style>\'");
            return fstyle;
        } else if (prop.endsWith("font-weight")) {
            ParsedValue fweight = parseFontWeight(root);
            if (fweight == null) error(root, "Expected \'<font-style>\'");
            return fweight;
        } else if (prop.endsWith("font")) {
            return parseFont(root);
        } else if ("-fx-stroke-dash-array".equals(prop)) {
            // TODO: Figure out a way that these properties don't need to be
            // special cased.
            Term term = root;
            int nArgs = numberOfTerms(term);
            ParsedValue<?,Size>[] segments = new ParsedValue[nArgs];
            int segment = 0;
            while(term != null) {
                segments[segment++] = parseSize(term);
                term = term.nextInSeries;
            }

            return new ParsedValue<ParsedValue<?,Size>[],Double[]>(segments,SequenceConverter.getInstance());

        } else if ("-fx-stroke-line-join".equals(prop)) {
            // TODO: Figure out a way that these properties don't need to be
            // special cased.
            ParsedValue[] values = parseStrokeLineJoin(root);
            if (values == null) error(root, "Expected \'miter', \'bevel\' or \'round\'");
            return values[0];
        } else if ("-fx-stroke-line-cap".equals(prop)) {
            // TODO: Figure out a way that these properties don't need to be
            // special cased.
            ParsedValue value = parseStrokeLineCap(root);
            if (value == null) error(root, "Expected \'square', \'butt\' or \'round\'");
            return value;
        } else if ("-fx-stroke-type".equals(prop)) {
            // TODO: Figure out a way that these properties don't need to be
            // special cased.
            ParsedValue value = parseStrokeType(root);
            if (value == null) error(root, "Expected \'centered', \'inside\' or \'outside\'");
            return value;
        } else if ("-fx-font-smoothing-type".equals(prop)) {
            // TODO: Figure out a way that these properties don't need to be
            // special cased.
            String str = null;
            int ttype = -1;
            final Token token = root.token;
            
            if (root.token == null
                    || ((ttype = root.token.getType()) != CSSLexer.STRING
                         && ttype != CSSLexer.IDENT)
                    || (str = root.token.getText()) == null
                    || str.isEmpty()) {
                error(root,  "Expected STRING or IDENT");
            }
            return new ParsedValue<String, String>(stripQuotes(str), null, false);            
        }
        return parse(root);
    }

    private ParsedValue parse(Term root) throws ParseException {

        if (root.token == null) error(root, "Parse error");
        final Token token = root.token;
        ParsedValue value = null; // value to return;

        final int ttype = token.getType();
        switch (ttype) {
        case CSSLexer.NUMBER:
        case CSSLexer.PERCENTAGE:
        case CSSLexer.EMS:
        case CSSLexer.EXS:
        case CSSLexer.PX:
        case CSSLexer.CM:
        case CSSLexer.MM:
        case CSSLexer.IN:
        case CSSLexer.PT:
        case CSSLexer.PC:
        case CSSLexer.DEG:
        case CSSLexer.GRAD:
        case CSSLexer.RAD:
        case CSSLexer.TURN:
            ParsedValue sizeValue = new ParsedValue<Size,Double>(size(token), null);
            value = new ParsedValue<ParsedValue<?,Size>, Double>(sizeValue, SizeConverter.getInstance());
            break;
        case CSSLexer.STRING:
        case CSSLexer.IDENT:
            boolean isIdent = ttype == CSSLexer.IDENT;
            final String str = stripQuotes(token.getText());
            final String text = str.toLowerCase();
            if ("ladder".equals(text)) {
                value = ladder(root);
            } else if ("linear".equals(text)) {
                value = linearGradient(root);
            } else if ("radial".equals(text)) {
                value = radialGradient(root);
            } else if ("true".equals(text)) {
                // TODO: handling of boolean is really bogus
                value = new ParsedValue<String,Boolean>("true",BooleanConverter.getInstance());
            } else if ("false".equals(text)) {
                // TODO: handling of boolean is really bogus
                value = new ParsedValue<String,Boolean>("false",BooleanConverter.getInstance());
            } else {
                // if the property value is another property, then it needs to be looked up.
                boolean needsLookup = isIdent && properties.containsKey(str);
                if (needsLookup || ((value = colorValueOfString(str)) == null )) {
                    value = new ParsedValue<String,String>(str, null, isIdent || needsLookup);
                }
            }
            break;
        case CSSLexer.HASH:
            final String clr = token.getText();
            try {
                value = new ParsedValue<Color,Color>(Color.web(clr), null);
            } catch (final IllegalArgumentException e) {
                error(root, e.getMessage());
            }
            break;
        case CSSLexer.FUNCTION:
            return  parseFunction(root);
        default:
            final String msg = "Unknown token type: \'" + ttype + "\'";
            error(root, msg);
        }
        return value;

    }

    /* Parse size.
     * @throw RecongnitionExcpetion if the token is not a size type or a lookup.
     */
    private ParsedValue<?,Size> parseSize(final Term root) throws ParseException {

        if (root.token == null || !isSize(root.token)) error(root, "Expected \'<size>\'");

        ParsedValue<?,Size> value = null;

        if (root.token.getType() != CSSLexer.IDENT) {

            Size size = size(root.token);
            value = new ParsedValue<Size,Size>(size, null);

        } else {

            String key = root.token.getText();
            value = new ParsedValue<String,Size>(key, null, true);

        }

        return value;
    }

    private ParsedValue<?,Color> parseColor(final Term root) throws ParseException {

        ParsedValue<?,Color> color = null;
        if (root.token != null &&
            (root.token.getType() == CSSLexer.IDENT ||
             root.token.getType() == CSSLexer.HASH ||
             root.token.getType() == CSSLexer.FUNCTION)) {

            color = parse(root);

        } else {
            error(root,  "Expected \'<color>\'");
        }
        return color;
    }

    // rgb(NUMBER, NUMBER, NUMBER)
    // rgba(NUMBER, NUMBER, NUMBER, NUMBER)
    // rgb(PERCENTAGE, PERCENTAGE, PERCENTAGE)
    // rgba(PERCENTAGE, PERCENTAGE, PERCENTAGE, NUMBER)
    private ParsedValue rgb(Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"rgb".regionMatches(true, 0, fn, 0, 3)) {
            final String msg = "Expected \'rgb\' or \'rgba\'";
            error(root, msg);
        }

        Term arg = root;
        Token rtok, gtok, btok, atok;

        if ((arg = arg.firstArg) == null) error(root, "Expected \'<number>\' or \'<percentage>\'");
        if ((rtok = arg.token) == null ||
            (rtok.getType() != CSSLexer.NUMBER &&
             rtok.getType() != CSSLexer.PERCENTAGE)) error(arg, "Expected \'<number>\' or \'<percentage>\'");

        root = arg;

        if ((arg = arg.nextArg) == null) error(root, "Expected \'<number>\' or \'<percentage>\'");
        if ((gtok = arg.token) == null ||
            (gtok.getType() != CSSLexer.NUMBER &&
             gtok.getType() != CSSLexer.PERCENTAGE)) error(arg, "Expected \'<number>\' or \'<percentage>\'");

        root = arg;

        if ((arg = arg.nextArg) == null) error(root, "Expected \'<number>\' or \'<percentage>\'");
        if ((btok = arg.token) == null ||
            (btok.getType() != CSSLexer.NUMBER &&
             btok.getType() != CSSLexer.PERCENTAGE)) error(arg, "Expected \'<number>\' or \'<percentage>\'");

        root = arg;

        if ((arg = arg.nextArg) != null) {
            if ((atok = arg.token) == null ||
                 atok.getType() != CSSLexer.NUMBER) error(arg, "Expected \'<number>\'");
        } else {
            atok = null;
        }

        int argType = rtok.getType();
        if (argType != gtok.getType() || argType != btok.getType() ||
            (argType != CSSLexer.NUMBER && argType != CSSLexer.PERCENTAGE)) {
            error(root, "Argument type mistmatch");
        }

        final String rtext = rtok.getText();
        final String gtext = gtok.getText();
        final String btext = btok.getText();

        double rval = 0;
        double gval = 0;
        double bval = 0;
        if (argType == CSSLexer.NUMBER) {
            rval = clamp(0.0f, Double.parseDouble(rtext) / 255.0f, 1.0f);
            gval = clamp(0.0f, Double.parseDouble(gtext) / 255.0f, 1.0f);
            bval = clamp(0.0f, Double.parseDouble(btext) / 255.0f, 1.0f);
        } else {
            rval = clamp(0.0f, Double.parseDouble(rtext.substring(0,rtext.length()-1)) / 100.0f, 1.0f);
            gval = clamp(0.0f, Double.parseDouble(gtext.substring(0,gtext.length()-1)) / 100.0f, 1.0f);
            bval = clamp(0.0f, Double.parseDouble(btext.substring(0,btext.length()-1)) / 100.0f, 1.0f);
        }

        final String atext = (atok != null) ? atok.getText() : null;
        final double aval =  (atext != null) ? clamp(0.0f, Double.parseDouble(atext), 1.0f) : 1.0;

        return new ParsedValue<Color,Color>(Color.color(rval,gval,bval,aval), null);

    }

    // hsb(NUMBER, PERCENTAGE, PERCENTAGE)
    // hsba(NUMBER, PERCENTAGE, PERCENTAGE, NUMBER)
    private ParsedValue hsb(Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"hsb".regionMatches(true, 0, fn, 0, 3)) {
            final String msg = "Expected \'hsb\' or \'hsba\'";
            error(root, msg);
        }

        Term arg = root;
        Token htok, stok, btok, atok;

        if ((arg = arg.firstArg) == null) error(root, "Expected \'<number>\'");
        if ((htok = arg.token) == null || htok.getType() != CSSLexer.NUMBER) error(arg, "Expected \'<number>\'");

        root = arg;

        if ((arg = arg.nextArg) == null) error(root, "Expected \'<percent>\'");
        if ((stok = arg.token) == null || stok.getType() != CSSLexer.PERCENTAGE) error(arg, "Expected \'<percent>\'");

        root = arg;

        if ((arg = arg.nextArg) == null) error(root, "Expected \'<percent>\'");
        if ((btok = arg.token) == null || btok.getType() != CSSLexer.PERCENTAGE) error(arg, "Expected \'<percent>\'");

        root = arg;

        if ((arg = arg.nextArg) != null) {
            if ((atok = arg.token) == null || atok.getType() != CSSLexer.NUMBER) error(arg, "Expected \'<number>\'");
        } else {
            atok = null;
        }

        final Size hval = size(htok);
        final Size sval = size(stok);
        final Size bval = size(btok);

        final double hue = hval.pixels(); // no clamp - hue can be negative
        final double saturation = clamp(0.0f, sval.pixels(), 1.0f);
        final double brightness = clamp(0.0f, bval.pixels(), 1.0f);

        final Size aval = (atok != null) ? size(atok) : null;
        final double opacity =  (aval != null) ? clamp(0.0f, aval.pixels(), 1.0f) : 1.0;

        return new ParsedValue<Color,Color>(Color.hsb(hue, saturation, brightness, opacity), null);
    }

    // derive(color, pct)
    private ParsedValue<ParsedValue[],Color> derive(final Term root)
            throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"derive".regionMatches(true, 0, fn, 0, 6)) {
            final String msg = "Expected \'derive\'";
            error(root, msg);
        }

        Term arg = root;
        if ((arg = arg.firstArg) == null) error(root, "Expected \'<color>\'");

        final ParsedValue<?,Color> color = parseColor(arg);

        final Term prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<percent\'");

        final ParsedValue<?,Size> brightness = parseSize(arg);

        ParsedValue[] values = new ParsedValue[] { color, brightness };
        return new ParsedValue<ParsedValue[],Color>(values, DeriveColorConverter.getInstance());
    }

    // 'ladder' color 'stops' stop+
    private ParsedValue<ParsedValue[],Color> ladder(final Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"ladder".regionMatches(true, 0, fn, 0, 6)) {
            final String msg = "Expected \'ladder\'";
            error(root, msg);
        }

        if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
            LOGGER.warning(formatDeprecatedMessage(root, "ladder"));
        }
        
        Term term = root;

        if ((term = term.nextInSeries) == null) error(root, "Expected \'<color>\'");
        final ParsedValue<?,Color> color = parse(term);

        Term prev = term;

        if ((term = term.nextInSeries) == null) error(prev,  "Expected \'stops\'");
        if (term.token == null ||
            term.token.getType() != CSSLexer.IDENT ||
            !"stops".equalsIgnoreCase(term.token.getText())) error(term,  "Expected \'stops\'");

        prev = term;

        if ((term = term.nextInSeries) == null) error(prev, "Expected \'(<number>, <color>)\'");

        int nStops = 0;
        Term temp = term;
        do {
            nStops += 1;
            // if next token type is IDENT, then we have CycleMethod
        } while (((temp = temp.nextInSeries) != null) &&
                 ((temp.token != null) && (temp.token.getType() == CSSLexer.LPAREN)));

        ParsedValue[] values = new ParsedValue[nStops+1];
        values[0] = color;
        int stopIndex = 1;
        do {
            ParsedValue<ParsedValue[],Stop> stop = stop(term);
            if (stop != null) values[stopIndex++] = stop;
            prev = term;
        } while(((term = term.nextInSeries) != null) &&
                 (term.token.getType() == CSSLexer.LPAREN));

        // if term is not null and the last term was not an lparen,
        // then term starts a new series of Paint. Point
        // root.nextInSeries to term so the next loop skips over the
        // already parsed ladder bits.
        if (term != null) {
            root.nextInSeries = term;
        }

        // if term is null, then we are at the end of a series.
        // root points to 'ladder', now we want the next term after root
        // to be the term after the last stop, which may be another layer
        else {
            root.nextInSeries = null;
            root.nextLayer = prev.nextLayer;
        }

        return new ParsedValue<ParsedValue[], Color>(values, LadderConverter.getInstance());
    }
    
    // <ladder> = ladder(<color>, <color-stop>[, <color-stop>]+ ) 
    private ParsedValue<ParsedValue[],Color> parseLadder(final Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"ladder".regionMatches(true, 0, fn, 0, 6)) {
            final String msg = "Expected \'ladder\'";
            error(root, msg);
        }

        Term term = root;

        if ((term = term.firstArg) == null) error(root, "Expected \'<color>\'");
        final ParsedValue<?,Color> color = parse(term);

        Term prev = term;

        if ((term = term.nextArg) == null) 
            error(prev,  "Expected \'<color-stop>[, <color-stop>]+\'");

        ParsedValue<ParsedValue[],Stop>[] stops = parseColorStops(term);

        ParsedValue[] values = new ParsedValue[stops.length+1];
        values[0] = color;
        System.arraycopy(stops, 0, values, 1, stops.length);
        return new ParsedValue<ParsedValue[], Color>(values, LadderConverter.getInstance());
    }    

    // parse (<number>, <color>)+
    // root.token should be a size
    // root.token.next should be a color
    private ParsedValue<ParsedValue[], Stop> stop(final Term root)
            throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"(".equals(fn)) {
            final String msg = "Expected \'(\'";
            error(root, msg);
        }

        Term arg = null;

        if ((arg = root.firstArg) == null) error(root,  "Expected \'<number>\'");

        ParsedValue<?,Size> size = parseSize(arg);

        Term prev = arg;
        if ((arg = arg.nextArg) == null) error(prev,  "Expected \'<color>\'");

        ParsedValue<?,Color> color = parseColor(arg);

        ParsedValue[] values = new ParsedValue[] { size, color };
        return new ParsedValue<ParsedValue[],Stop>(values, StopConverter.getInstance());

    }

    // http://dev.w3.org/csswg/css3-images/#color-stop-syntax
    // <color-stop> = <color> [ <percentage> | <length> ]?
    private ParsedValue<ParsedValue[], Stop>[] parseColorStops(final Term root)
            throws ParseException {

        int nArgs = 1;
        Term temp = root;
        while(temp != null) {
            if (temp.nextArg != null) {
                nArgs += 1;
                temp = temp.nextArg;
            } else if (temp.nextInSeries != null) {
                temp = temp.nextInSeries;
            } else {
                break;
            }
        }

        if (nArgs < 2) {
            error(root, "Expected \'<color-stop>\'");
        }

        ParsedValue<?,Color>[] colors = new ParsedValue[nArgs];
        Size[] positions = new Size[nArgs];
        java.util.Arrays.fill(positions, null);

        Term stop = root;
        Term prev = root;
        SizeUnits units = null;
        for (int n = 0; n<nArgs; n++) {

            colors[n] = parseColor(stop);

            prev = stop;
            Term term = stop.nextInSeries;
            if (term != null) {
                if (isSize(term.token)) {
                    positions[n] = size(term.token);
                    if (units != null) {
                        if (units != positions[n].getUnits()) {
                            error(term, "Parser unable to handle mixed \'<percent>\' and \'<length>\'");
                        }
                    }
                } else {
                    error(prev, "Expected \'<percent>\' or \'<length>\'");
                }
                prev = term;
                stop = term.nextArg;
            } else {
                prev = stop;
                stop = stop.nextArg;
            }

        }

        //
        // normalize positions according to
        // http://dev.w3.org/csswg/css3-images/#color-stop-syntax
        //
        // If the first color-stop does not have a position, set its
        // position to 0%. If the last color-stop does not have a position,
        // set its position to 100%.
        if (positions[0] == null) positions[0] = new Size(0, SizeUnits.PERCENT);
        if (positions[nArgs-1] == null) positions[nArgs-1] = new Size(100, SizeUnits.PERCENT);

        // If a color-stop has a position that is less than the specified
        // position of any color-stop before it in the list, set its
        // position to be equal to the largest specified position of any
        // color-stop before it.
        Size max = null;
        for (int n = 1 ; n<nArgs; n++) {
            Size pos0 = positions[n-1];
            if (pos0 == null) continue;
            if (max == null || max.getValue() < pos0.getValue()) {
                // TODO: this doesn't work with mixed length and percent
                max = pos0;
            }
            Size pos1 = positions[n];
            if (pos1 == null) continue;

            if (pos1.getValue() < max.getValue()) positions[n] = max;
        }

        // If any color-stop still does not have a position, then,
        // for each run of adjacent color-stops without positions, set
        // their positions so that they are evenly spaced between the
        // preceding and following color-stops with positions.
        Size preceding = null;
        int withoutIndex = -1;
        for (int n = 0 ; n<nArgs; n++) {
            Size pos = positions[n];
            if (pos == null) {
                if (withoutIndex == -1) withoutIndex = n;
                continue;
            } else {
                if (withoutIndex > -1) {

                    int nWithout = n - withoutIndex;
                    double precedingValue = preceding.getValue();
                    double delta =
                        (pos.getValue() - precedingValue) / (nWithout + 1);

                    while(withoutIndex < n) {
                        precedingValue += delta;
                        positions[withoutIndex++] =
                            new Size(precedingValue, pos.getUnits());
                    }
                    withoutIndex = -1;
                    preceding = pos;
                } else {
                    preceding = pos;
                }
            }
        }
     
        ParsedValue<ParsedValue[],Stop>[] stops = new ParsedValue[nArgs];
        for (int n=0; n<nArgs; n++) {
            stops[n] = new ParsedValue<ParsedValue[],Stop>(
                new ParsedValue[] {
                    new ParsedValue<Size,Size>(positions[n], null),
                    colors[n]
                },
                StopConverter.getInstance()
            );
        }

        return stops;

    }
    
    // parse (<number>, <number>)
    private ParsedValue[] point(final Term root) throws ParseException {

        if (root.token == null ||
            root.token.getType() != CSSLexer.LPAREN) error(root, "Expected \'(<number>, <number>)\'");

        final String fn = root.token.getText();
        if (fn == null || !"(".equalsIgnoreCase(fn)) {
            final String msg = "Expected \'(\'";
            error(root, msg);
        }

        Term arg = null;

        // no <number>
        if ((arg = root.firstArg) == null)  error(root, "Expected \'<number>\'");

        final ParsedValue<?,Size> ptX = parseSize(arg);

        final Term prev = arg;

        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        final ParsedValue<?,Size> ptY = parseSize(arg);

        return new ParsedValue[] { ptX, ptY };
    }

    private ParsedValue parseFunction(final Term root) throws ParseException {

        // Text from parser is function name plus the lparen, e.g., 'derive('
        final String fcn = (root.token != null) ? root.token.getText() : null;
        if (fcn == null) {
            error(root, "Expected function name");
        } else if ("rgb".regionMatches(true, 0, fcn, 0, 3)) {
            return rgb(root);
        } else if ("hsb".regionMatches(true, 0, fcn, 0, 3)) {
            return hsb(root);
        } else if ("derive".regionMatches(true, 0, fcn, 0, 6)) {
            return derive(root);
        } else if ("innershadow".regionMatches(true, 0, fcn, 0, 11)) {
            return innershadow(root);
        } else if ("dropshadow".regionMatches(true, 0, fcn, 0, 10)) {
            return dropshadow(root);
        } else if ("linear-gradient".regionMatches(true, 0, fcn, 0, 15)) {
            return parseLinearGradient(root);
        } else if ("radial-gradient".regionMatches(true, 0, fcn, 0, 15)) {
            return parseRadialGradient(root);
        } else if ("ladder".regionMatches(true, 0, fcn, 0, 6)) {
            return parseLadder(root);
        } else if ("url".regionMatches(true, 0, fcn, 0, 3)) {
            return parseURI(root);
        } else {
            error(root, "Unexpected function \'" + fcn + "\'");
        }
        return null;
    }

    private ParsedValue<BlurType,BlurType> blurType(final Term root) throws ParseException {

        if (root == null) return null;
        if (root.token == null ||
            root.token.getType() != CSSLexer.IDENT ||
            root.token.getText() == null ||
            root.token.getText().isEmpty()) {
            final String msg = "Expected \'gaussian\', \'one-pass-box\', \'two-pass-box\', or \'three-pass-box\'";
            error(root, msg);
        }
        final String blurStr = root.token.getText().toLowerCase();
        BlurType blurType = BlurType.THREE_PASS_BOX;
        if ("gaussian".equals(blurStr)) {
            blurType = BlurType.GAUSSIAN;
        } else if ("one-pass-box".equals(blurStr)) {
            blurType = BlurType.ONE_PASS_BOX;
        } else if ("two-pass-box".equals(blurStr)) {
            blurType = BlurType.TWO_PASS_BOX;
        } else if ("three-pass-box".equals(blurStr)) {
            blurType = BlurType.THREE_PASS_BOX;
        } else {
            final String msg = "Expected \'gaussian\', \'one-pass-box\', \'two-pass-box\', or \'three-pass-box\'";
            error(root, msg);
        }
        return new ParsedValue<BlurType,BlurType>(blurType, null);
    }

    // innershadow <blur-type> <color> <radius> <choke> <offset-x> <offset-y>
    private ParsedValue innershadow(final Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (!"innershadow".regionMatches(true, 0, fn, 0, 11)) {
            final String msg = "Expected \'innershadow\'";
            error(root, msg);
        }

        Term arg;

        if ((arg = root.firstArg) == null) error(root, "Expected \'<blur-type>\'");
        ParsedValue<BlurType,BlurType> blurVal = blurType(arg);

        Term prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<color>\'");

        ParsedValue<?,Color> colorVal = parseColor(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> radiusVal = parseSize(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> chokeVal = parseSize(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> offsetXVal = parseSize(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> offsetYVal = parseSize(arg);

        ParsedValue[] values = new ParsedValue[] {
            blurVal,
            colorVal,
            radiusVal,
            chokeVal,
            offsetXVal,
            offsetYVal
        };
        return new ParsedValue<ParsedValue[],Effect>(values, EffectConverter.InnerShadowConverter.getInstance());
    }

    // dropshadow <blur-type> <color> <radius> <spread> <offset-x> <offset-y>
    private ParsedValue dropshadow(final Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (!"dropshadow".regionMatches(true, 0, fn, 0, 10)) {
            final String msg = "Expected \'dropshadow\'";
            error(root, msg);
        }

        Term arg;

        if ((arg = root.firstArg) == null) error(root, "Expected \'<blur-type>\'");
        ParsedValue<BlurType,BlurType> blurVal = blurType(arg);

        Term prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<color>\'");

        ParsedValue<?,Color> colorVal = parseColor(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> radiusVal = parseSize(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> spreadVal = parseSize(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> offsetXVal = parseSize(arg);

        prev = arg;
        if ((arg = arg.nextArg) == null) error(prev, "Expected \'<number>\'");

        ParsedValue<?,Size> offsetYVal = parseSize(arg);

        ParsedValue[] values = new ParsedValue[] {
            blurVal,
            colorVal,
            radiusVal,
            spreadVal,
            offsetXVal,
            offsetYVal
        };
        return new ParsedValue<ParsedValue[],Effect>(values, EffectConverter.DropShadowConverter.getInstance());
    }

    // returns null if the Term is null or is not a cycle method.
    private ParsedValue<CycleMethod, CycleMethod> cycleMethod(final Term root) {
        CycleMethod cycleMethod = null;
        if (root != null && root.token.getType() == CSSLexer.IDENT) {

            final String text = root.token.getText().toLowerCase();
            if ("repeat".equals(text)) {
                cycleMethod = CycleMethod.REPEAT;
            } else if ("reflect".equals(text)) {
                cycleMethod = CycleMethod.REFLECT;
            } else if ("no-cycle".equals(text)) {
                cycleMethod = CycleMethod.NO_CYCLE;
            }
        }
        if (cycleMethod != null)
            return new ParsedValue<CycleMethod,CycleMethod>(cycleMethod, null);
        else
            return null;
    }

    // linear <point> TO <point> STOPS <stop>+ cycleMethod?
    private ParsedValue<ParsedValue[],Paint> linearGradient(final Term root) throws ParseException {

        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"linear".equalsIgnoreCase(fn)) {
            final String msg = "Expected \'linear\'";
            error(root, msg);
        }

        if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
            LOGGER.warning(formatDeprecatedMessage(root, "linear gradient"));
        }

        Term term = root;

        if ((term = term.nextInSeries) == null) error(root, "Expected \'(<number>, <number>)\'");

        final ParsedValue<?,Size>[] startPt = point(term);

        Term prev = term;
        if ((term = term.nextInSeries) == null) error(prev, "Expected \'to\'");
        if (term.token == null ||
            term.token.getType() != CSSLexer.IDENT ||
            !"to".equalsIgnoreCase(term.token.getText())) error(root, "Expected \'to\'");

        prev = term;
        if ((term = term.nextInSeries) == null) error(prev, "Expected \'(<number>, <number>)\'");

        final ParsedValue<?,Size>[] endPt = point(term);

        prev = term;
        if ((term = term.nextInSeries) == null) error(prev, "Expected \'stops\'");
        if (term.token == null ||
            term.token.getType() != CSSLexer.IDENT ||
            !"stops".equalsIgnoreCase(term.token.getText())) error(term, "Expected \'stops\'");

        prev = term;
        if ((term = term.nextInSeries) == null) error(prev, "Expected \'(<number>, <number>)\'");

        int nStops = 0;
        Term temp = term;
        do {
            nStops += 1;
            // if next token type is IDENT, then we have CycleMethod
        } while (((temp = temp.nextInSeries) != null) &&
                 ((temp.token != null) && (temp.token.getType() == CSSLexer.LPAREN)));

        ParsedValue[] stops = new ParsedValue[nStops];
        int stopIndex = 0;
        do {
            ParsedValue<ParsedValue[],Stop> stop = stop(term);
            if (stop != null) stops[stopIndex++] = stop;
            prev = term;
        } while(((term = term.nextInSeries) != null) &&
                (term.token.getType() == CSSLexer.LPAREN));

        // term is either null or is a cycle method, or the start of another Paint.
        ParsedValue<CycleMethod,CycleMethod> cycleMethod = cycleMethod(term);

        if (cycleMethod == null) {

            cycleMethod = new ParsedValue<CycleMethod,CycleMethod>(CycleMethod.NO_CYCLE, null);

            // if term is not null and the last term was not a cycle method,
            // then term starts a new series or layer of Paint
            if (term != null) {
                root.nextInSeries = term;
            }

            // if term is null, then we are at the end of a series.
            // root points to 'linear', now we want the next term after root
            // to be the term after the last stop, which may be another layer
            else {
                root.nextInSeries = null;
                root.nextLayer = prev.nextLayer;
            }


        } else {
            // last term was a CycleMethod, so term is not null.
            // root points at 'linear', now we want the next term after root
            // to be the term after cyclemethod, which may be another series
            // of paint or another layer.
            //
            root.nextInSeries = term.nextInSeries;
            root.nextLayer = term.nextLayer;
        }

        ParsedValue[] values = new ParsedValue[5 + stops.length];
        int index = 0;
        values[index++] = (startPt != null) ? startPt[0] : null;
        values[index++] = (startPt != null) ? startPt[1] : null;
        values[index++] = (endPt != null) ? endPt[0] : null;
        values[index++] = (endPt != null) ? endPt[1] : null;
        values[index++] = cycleMethod;
        for (int n=0; n<stops.length; n++) values[index++] = stops[n];
        return new ParsedValue<ParsedValue[], Paint>(values, PaintConverter.LinearGradientConverter.getInstance());
    }

    // Based off http://dev.w3.org/csswg/css3-images/#linear-gradients
    //
    // <linear-gradient> = linear-gradient(
    //        [ [from <point> to <point>] | [ to <side-or-corner> ] ] ,]? [ [ repeat | reflect ] ,]? 
    //        <color-stop>[, <color-stop>]+ 
    // ) 
    //
    //
    // <point> = <percentage> <percentage> | <length> <length> 
    // <side-or-corner> = [left | right] || [top | bottom] 
    //
    // If neither repeat nor reflect are given, then the CycleMethod defaults "NO_CYCLE". 
    // If neither [from <point> to <point>] nor [ to <side-or-corner> ] are given, 
    // then the gradient direction defaults to 'to bottom'. 
    // Stops are per http://dev.w3.org/csswg/css3-images/#color-stop-syntax.
    private ParsedValue parseLinearGradient(final Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (!"linear-gradient".regionMatches(true, 0, fn, 0, 15)) {
            final String msg = "Expected \'linear-gradient\'";
            error(root, msg);
        }

        Term arg;

        if ((arg = root.firstArg) == null ||
             arg.token == null ||
             arg.token.getText().isEmpty()) {
            error(root, 
                "Expected \'from <point> to <point>\' or \'to <side-or-corner>\' " +
                "or \'<cycle-method>\' or \'<color-stop>\'");
        }

        Term prev = arg;
//        ParsedValue<Size,Size> angleVal = null;
        ParsedValue<?,Size>[] startPt = null;
        ParsedValue<?,Size>[] endPt = null;

        if ("from".equalsIgnoreCase(arg.token.getText())) {

            prev = arg;
            if ((arg = arg.nextInSeries) == null) error(prev, "Expected \'<point>\'");

            ParsedValue<?,Size> ptX = parseSize(arg);

            prev = arg;
            if ((arg = arg.nextInSeries) == null) error(prev, "Expected \'<point>\'");

            ParsedValue<?,Size> ptY = parseSize(arg);

            startPt = new ParsedValue[] { ptX, ptY };

            prev = arg;
            if ((arg = arg.nextInSeries) == null) error(prev, "Expected \'to\'");
            if (arg.token == null ||
                arg.token.getType() != CSSLexer.IDENT ||
                !"to".equalsIgnoreCase(arg.token.getText())) error(prev, "Expected \'to\'");

            prev = arg;
            if ((arg = arg.nextInSeries) == null) error(prev, "Expected \'<point>\'");

            ptX = parseSize(arg);

            prev = arg;
            if ((arg = arg.nextInSeries) == null) error(prev, "Expected \'<point>\'");

            ptY = parseSize(arg);

            endPt = new ParsedValue[] { ptX, ptY };

            prev = arg;
            arg = arg.nextArg;
            
        } else if("to".equalsIgnoreCase(arg.token.getText())) {

            prev = arg;
            if ((arg = arg.nextInSeries) == null ||
                arg.token == null |
                arg.token.getType() != CSSLexer.IDENT ||
                arg.token.getText().isEmpty()) {
                error (prev, "Expected \'<side-or-corner>\'");
            }


            int startX = 0;
            int startY = 0;
            int endX = 0;
            int endY = 0;

            String sideOrCorner1 = arg.token.getText().toLowerCase();
            // The keywords denote the direction.
            if ("top".equals(sideOrCorner1)) {
                // going toward the top, then start at the bottom
                startY = 100;
                endY = 0;

            } else if ("bottom".equals(sideOrCorner1)) {
                // going toward the bottom, then start at the top
                startY = 0;
                endY = 100;

            } else if ("right".equals(sideOrCorner1)) {
                // going toward the right, then start at the left
                startX = 0;
                endX = 100;

            } else if ("left".equals(sideOrCorner1)) {
                // going toward the left, then start at the right
                startX = 100;
                endX = 0;

            } else {
                error(arg, "Invalid \'<side-or-corner>\'");
            }

            prev = arg;
            if (arg.nextInSeries != null) {
                arg = arg.nextInSeries;
                if (arg.token != null &&
                    arg.token.getType() == CSSLexer.IDENT &&
                    !arg.token.getText().isEmpty()) {

                    String sideOrCorner2 = arg.token.getText().toLowerCase();

                    // if right or left has already been given,
                    // then either startX or endX will not be zero.
                    if ("right".equals(sideOrCorner2) &&
                        startX == 0 && endX == 0) {
                        // start left, end right
                        startX = 0;
                        endX = 100;
                    } else if ("left".equals(sideOrCorner2) &&
                        startX == 0 && endX == 0) {
                        // start right, end left
                        startX = 100;
                        endX = 0;

                    // if top or bottom has already been given,
                    // then either startY or endY will not be zero.
                    } else if("top".equals(sideOrCorner2) &&
                        startY == 0 && endY == 0) {
                        // start bottom, end top
                        startY = 100;
                        endY = 0;
                    } else if ("bottom".equals(sideOrCorner2) &&
                        startY == 0 && endY == 0) {
                        // start top, end bottom
                        startY = 0;
                        endY = 100;

                    } else {
                        error(arg, "Invalid \'<side-or-corner>\'");
                    }

                } else {
                    error (prev, "Expected \'<side-or-corner>\'");
                }
            }
                

            startPt = new ParsedValue[] {
                new ParsedValue<Size,Size>(new Size(startX, SizeUnits.PERCENT), null),
                new ParsedValue<Size,Size>(new Size(startY, SizeUnits.PERCENT), null)
            };

            endPt = new ParsedValue[] {
                new ParsedValue<Size,Size>(new Size(endX, SizeUnits.PERCENT), null),
                new ParsedValue<Size,Size>(new Size(endY, SizeUnits.PERCENT), null)
            };

            prev = arg;
            arg = arg.nextArg;
        }

        if (startPt == null && endPt == null) {
            // spec says defaults to bottom
            startPt = new ParsedValue[] {
                new ParsedValue<Size,Size>(new Size(0, SizeUnits.PERCENT), null),
                new ParsedValue<Size,Size>(new Size(0, SizeUnits.PERCENT), null)
            };

            endPt = new ParsedValue[] {
                new ParsedValue<Size,Size>(new Size(0, SizeUnits.PERCENT), null),
                new ParsedValue<Size,Size>(new Size(100, SizeUnits.PERCENT), null)
            };
        }

        if (arg == null ||
            arg.token == null ||
            arg.token.getText().isEmpty()) {
            error(prev, "Expected \'<cycle-method>\' or \'<color-stop>\'");
        }

        CycleMethod cycleMethod = CycleMethod.NO_CYCLE;
        if ("reflect".equalsIgnoreCase(arg.token.getText())) {
            cycleMethod = CycleMethod.REFLECT;
            prev = arg;
            arg = arg.nextArg;
        } else if ("repeat".equalsIgnoreCase(arg.token.getText())) {
            cycleMethod = CycleMethod.REFLECT;
            prev = arg;
            arg = arg.nextArg;
        }

        if (arg == null  ||
            arg.token == null ||
            arg.token.getText().isEmpty()) {
            error(prev, "Expected \'<color-stop>\'");
        }

        ParsedValue<ParsedValue[],Stop>[] stops = parseColorStops(arg);

        ParsedValue[] values = new ParsedValue[5 + stops.length];
        int index = 0;
        values[index++] = (startPt != null) ? startPt[0] : null;
        values[index++] = (startPt != null) ? startPt[1] : null;
        values[index++] = (endPt != null) ? endPt[0] : null;
        values[index++] = (endPt != null) ? endPt[1] : null;
        values[index++] = new ParsedValue<CycleMethod,CycleMethod>(cycleMethod, null);
        for (int n=0; n<stops.length; n++) values[index++] = stops[n];
        return new ParsedValue<ParsedValue[], Paint>(values, PaintConverter.LinearGradientConverter.getInstance());

    }

    // radial [focus-angle <number | percent>]? [focus-distance <size>]?
    // [center (<size>,<size>)]? <size>
    // stops [ ( <number> , <color> ) ]+ [ repeat | reflect ]?
    private ParsedValue<ParsedValue[], Paint> radialGradient(final Term root) throws ParseException {

        final String fn = (root.token != null) ? root.token.getText() : null;
        if (fn == null || !"radial".equalsIgnoreCase(fn)) {
            final String msg = "Expected \'radial\'";
            error(root, msg);
        }

        if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
            LOGGER.warning(formatDeprecatedMessage(root, "radial gradient"));
        }
        
        Term term = root;
        Term prev = root;

        if ((term = term.nextInSeries) == null) error(root, "Expected \'focus-angle <number>\', \'focus-distance <number>\', \'center (<number>,<number>)\' or \'<size>\'");
        if (term.token == null) error(term, "Expected \'focus-angle <number>\', \'focus-distance <number>\', \'center (<number>,<number>)\' or \'<size>\'");


        ParsedValue<?,Size> focusAngle = null;
        if (term.token.getType() == CSSLexer.IDENT) {
            final String keyword = term.token.getText().toLowerCase();
            if ("focus-angle".equals(keyword)) {

                prev = term;
                if ((term = term.nextInSeries) == null) error(prev, "Expected \'<number>\'");
                if (term.token == null) error(prev, "Expected \'<number>\'");

                focusAngle = parseSize(term);

                prev = term;
                if ((term = term.nextInSeries) == null) error(prev, "Expected \'focus-distance <number>\', \'center (<number>,<number>)\' or \'<size>\'");
                if (term.token == null) error(term,  "Expected \'focus-distance <number>\', \'center (<number>,<number>)\' or \'<size>\'");
            }
        }

        ParsedValue<?,Size> focusDistance = null;
        if (term.token.getType() == CSSLexer.IDENT) {
            final String keyword = term.token.getText().toLowerCase();
            if ("focus-distance".equals(keyword)) {

                prev = term;
                if ((term = term.nextInSeries) == null) error(prev, "Expected \'<number>\'");
                if (term.token == null) error(prev, "Expected \'<number>\'");

                focusDistance = parseSize(term);

                prev = term;
                if ((term = term.nextInSeries) == null) error(prev, "Expected  \'center (<number>,<number>)\' or \'<size>\'");
                if (term.token == null) error(term,  "Expected  \'center (<number>,<number>)\' or \'<size>\'");
            }
        }

        ParsedValue<?,Size>[] centerPoint = null;
        if (term.token.getType() == CSSLexer.IDENT) {
            final String keyword = term.token.getText().toLowerCase();
            if ("center".equals(keyword)) {

                prev = term;
                if ((term = term.nextInSeries) == null) error(prev, "Expected \'(<number>,<number>)\'");
                if (term.token == null ||
                    term.token.getType() != CSSLexer.LPAREN) error(term, "Expected \'(<number>,<number>)\'");

                centerPoint = point(term);

                prev = term;
                if ((term = term.nextInSeries) == null) error(prev, "Expected \'<size>\'");
                if (term.token == null) error(term,  "Expected \'<size>\'");
            }
        }

        ParsedValue<?,Size> radius = parseSize(term);

        prev = term;
        if ((term = term.nextInSeries) == null) error(prev, "Expected \'stops\' keyword");
        if (term.token == null ||
            term.token.getType() != CSSLexer.IDENT) error(term, "Expected \'stops\' keyword");

        if (!"stops".equalsIgnoreCase(term.token.getText())) error(term, "Expected \'stops\'");

        prev = term;
        if ((term = term.nextInSeries) == null) error(prev, "Expected \'(<number>, <number>)\'");

        int nStops = 0;
        Term temp = term;
        do {
            nStops += 1;
            // if next token type is IDENT, then we have CycleMethod
        } while (((temp = temp.nextInSeries) != null) &&
                 ((temp.token != null) && (temp.token.getType() == CSSLexer.LPAREN)));

        ParsedValue[] stops = new ParsedValue[nStops];
        int stopIndex = 0;
        do {
            ParsedValue<ParsedValue[],Stop> stop = stop(term);
            if (stop != null) stops[stopIndex++] = stop;
            prev = term;
        } while(((term = term.nextInSeries) != null) &&
                (term.token.getType() == CSSLexer.LPAREN));

        // term is either null or is a cycle method, or the start of another Paint.
        ParsedValue<CycleMethod,CycleMethod> cycleMethod = cycleMethod(term);

        if (cycleMethod == null) {

            cycleMethod = new ParsedValue<CycleMethod,CycleMethod>(CycleMethod.NO_CYCLE, null);

            // if term is not null and the last term was not a cycle method,
            // then term starts a new series or layer of Paint
            if (term != null) {
                root.nextInSeries = term;
            }

            // if term is null, then we are at the end of a series.
            // root points to 'linear', now we want the next term after root
            // to be the term after the last stop, which may be another layer
            else {
                root.nextInSeries = null;
                root.nextLayer = prev.nextLayer;
            }


        } else {
            // last term was a CycleMethod, so term is not null.
            // root points at 'linear', now we want the next term after root
            // to be the term after cyclemethod, which may be another series
            // of paint or another layer.
            //
            root.nextInSeries = term.nextInSeries;
            root.nextLayer = term.nextLayer;
        }

        ParsedValue[] values = new ParsedValue[6 + stops.length];
        int index = 0;
        values[index++] = focusAngle;
        values[index++] = focusDistance;
        values[index++] = (centerPoint != null) ? centerPoint[0] : null;
        values[index++] = (centerPoint != null) ? centerPoint[1] : null;
        values[index++] = radius;
        values[index++] = cycleMethod;
        for (int n=0; n<stops.length; n++) values[index++] = stops[n];
        return new ParsedValue<ParsedValue[], Paint>(values, PaintConverter.RadialGradientConverter.getInstance());
    }

    // Based off http://dev.w3.org/csswg/css3-images/#radial-gradients
    //
    // <radial-gradient> = radial-gradient( 
    //        [ focus-angle <angle>, ]? 
    //        [ focus-distance <percentage>, ]? 
    //        [ center <point>, ]? 
    //        radius <length>,
    //        [ [ repeat | reflect ] ,]? 
    //        <color-stop>[, <color-stop>]+ ) 
    //
    // Stops are per http://dev.w3.org/csswg/css3-images/#color-stop-syntax.
    private ParsedValue parseRadialGradient(final Term root) throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (!"radial-gradient".regionMatches(true, 0, fn, 0, 15)) {
            final String msg = "Expected \'radial-gradient\'";
            error(root, msg);
        }

        Term arg;

        if ((arg = root.firstArg) == null ||
             arg.token == null ||
             arg.token.getText().isEmpty()) {
            error(root, 
                "Expected \'focus-angle <angle>\' " + 
                "or \'focus-distance <percentage>\' " +
                "or \'center <point>\' " +
                "or \'radius [<length> | <percentage>]\'");
        }

        Term prev = arg;
        ParsedValue<?,Size>focusAngle = null;
        ParsedValue<?,Size>focusDistance = null;
        ParsedValue<?,Size>[] centerPoint = null;
        ParsedValue<?,Size>radius = null;

        if ("focus-angle".equalsIgnoreCase(arg.token.getText())) {

            prev = arg;
            if ((arg = arg.nextInSeries) == null ||
                    !isSize(arg.token)) error(prev, "Expected \'<angle>\'");

            Size angle = size(arg.token);
            switch(angle.getUnits()) {
                case DEG:
                case RAD:
                case GRAD:
                case TURN:
                case PX:
                    break;
                default:
                    error(arg, "Expected [deg | rad | grad | turn ]");
            }
            focusAngle = new ParsedValue<Size,Size>(angle, null);

            prev = arg;
            if ((arg = arg.nextArg) == null)
                error(prev, "Expected \'focus-distance <percentage>\' " +
                            "or \'center <point>\' " +
                            "or \'radius [<length> | <percentage>]\'");

        }
        
        if ("focus-distance".equalsIgnoreCase(arg.token.getText())) {

            prev = arg;
            if ((arg = arg.nextInSeries) == null ||
                    !isSize(arg.token)) error(prev, "Expected \'<percentage>\'");

            Size distance = size(arg.token);

            // "The focus point is always specified relative to the center
            // point by an angle and a distance relative to the radius."
            switch(distance.getUnits()) {
                case PERCENT:
                    break;
                default:
                    error(arg, "Expected \'%\'");
            }
            focusDistance = new ParsedValue<Size,Size>(distance, null);

            prev = arg;
            if ((arg = arg.nextArg) == null)
                error(prev, "Expected \'center <center>\' " +
                            "or \'radius <length>\'");

        }
        
        if ("center".equalsIgnoreCase(arg.token.getText())) {

            prev = arg;
            if ((arg = arg.nextInSeries) == null) error(prev, "Expected \'<point>\'");

            ParsedValue<?,Size> ptX = parseSize(arg);

            prev = arg;
            if ((arg = arg.nextInSeries) == null) error(prev, "Expected \'<point>\'");

            ParsedValue<?,Size> ptY = parseSize(arg);

            centerPoint = new ParsedValue[] { ptX, ptY };
            
            prev = arg;
            if ((arg = arg.nextArg) == null)
                error(prev, "Expected \'radius [<length> | <percentage>]\'");
        }
        
        if ("radius".equalsIgnoreCase(arg.token.getText())) {

            prev = arg;
            if ((arg = arg.nextInSeries) == null ||
                !isSize(arg.token)) error(prev, "Expected \'[<length> | <percentage>]\'");

            radius = parseSize(arg);
            
            prev = arg;
            if ((arg = arg.nextArg) == null)
                error(prev, "Expected \'radius [<length> | <percentage>]\'");
        }        

        CycleMethod cycleMethod = CycleMethod.NO_CYCLE;
        if ("reflect".equalsIgnoreCase(arg.token.getText())) {
            cycleMethod = CycleMethod.REFLECT;
            prev = arg;
            arg = arg.nextArg;
        } else if ("repeat".equalsIgnoreCase(arg.token.getText())) {
            cycleMethod = CycleMethod.REFLECT;
            prev = arg;
            arg = arg.nextArg;
        }

        if (arg == null  ||
            arg.token == null ||
            arg.token.getText().isEmpty()) {
            error(prev, "Expected \'<color-stop>\'");
        }

        ParsedValue<ParsedValue[],Stop>[] stops = parseColorStops(arg);

        ParsedValue[] values = new ParsedValue[6 + stops.length];
        int index = 0;
        values[index++] = focusAngle;
        values[index++] = focusDistance;
        values[index++] = (centerPoint != null) ? centerPoint[0] : null;
        values[index++] = (centerPoint != null) ? centerPoint[1] : null;
        values[index++] = radius;
        values[index++] = new ParsedValue<CycleMethod,CycleMethod>(cycleMethod, null);
        for (int n=0; n<stops.length; n++) values[index++] = stops[n];
        return new ParsedValue<ParsedValue[], Paint>(values, PaintConverter.RadialGradientConverter.getInstance());
        
    }
    
    // parse a series of paint values separated by commas.
    // i.e., <paint> [, <paint>]*
    private ParsedValue<ParsedValue<?,Paint>[],Paint[]> parsePaintLayers(Term root)
            throws ParseException {

        // how many paints in the series?
        int nPaints = numberOfLayers(root);

        ParsedValue<?,Paint>[] paints = new ParsedValue[nPaints];

        Term temp = root;
        int paint = 0;

        do {
            if (temp.token == null ||
                temp.token.getText() == null ||
                temp.token.getText().isEmpty()) error(temp, "Expected \'<paint>\'");

            paints[paint++] = (ParsedValue<?,Paint>)parse(temp);

            temp = nextLayer(temp);
        } while (temp != null);

        return new ParsedValue<ParsedValue<?,Paint>[],Paint[]>(paints, PaintConverter.SequenceConverter.getInstance());

    }

    // An size or a series of four size values
    // <size> | <size> <size> <size> <size>
    private ParsedValue<?,Size>[] parseSizeSeries(final Term root)
            throws ParseException {

        Term temp = root;
        ParsedValue<?,Size>[] sides = new ParsedValue[4];
        int side = 0;

        while (side < 4 && temp != null) {
            sides[side++] = parseSize(temp);
            temp = temp.nextInSeries;
        }

        if (side < 2) sides[1] = sides[0]; // right = top
        if (side < 3) sides[2] = sides[0]; // bottom = top
        if (side < 4) sides[3] = sides[1]; // left = right

        return sides;
    }

    // A series of inset or sets of four inset values
    // <size> | <size> <size> <size> <size> [ , [ <size> | <size> <size> <size> <size>] ]*
    private ParsedValue<ParsedValue<ParsedValue<?,Size>[],Insets>[], Insets[]> parseInsetsLayers(Term root)
            throws ParseException {

        int nLayers = numberOfLayers(root);

        Term temp = root;
        int layer = 0;
        ParsedValue<ParsedValue<?,Size>[],Insets>[] layers = new ParsedValue[nLayers];

        while(temp != null) {
            ParsedValue<?,Size>[] sides = parseSizeSeries(temp);
            layers[layer++] = new ParsedValue<ParsedValue<?,Size>[],Insets>(sides, InsetsConverter.getInstance());
            while(temp.nextInSeries != null) {
                temp = temp.nextInSeries;
            }
            temp = nextLayer(temp);
        }

        return new ParsedValue<ParsedValue<ParsedValue<?,Size>[],Insets>[], Insets[]>(layers, InsetsConverter.SequenceConverter.getInstance());
    }

    // <size> | <size> <size> <size> <size>
    private ParsedValue<ParsedValue<ParsedValue<?,Size>[],Margins>[], Margins[]> parseMarginsLayers(Term root)
            throws ParseException {

        int nLayers = numberOfLayers(root);

        Term temp = root;
        int layer = 0;
        ParsedValue<ParsedValue<?,Size>[],Margins>[] layers = new ParsedValue[nLayers];

        while(temp != null) {
            ParsedValue<?,Size>[] sides = parseSizeSeries(temp);
            layers[layer++] = new ParsedValue<ParsedValue<?,Size>[],Margins>(sides, Margins.Converter.getInstance());
            while(temp.nextInSeries != null) {
                temp = temp.nextInSeries;
            }
            temp = nextLayer(temp);
        }

        return new ParsedValue<ParsedValue<ParsedValue<?,Size>[],Margins>[], Margins[]>(layers, Margins.SequenceConverter.getInstance());
    }

    /* Constant for background position */
    private final static ParsedValue<Size,Size> ZERO_PERCENT =
            new ParsedValue<Size,Size>(new Size(0f, SizeUnits.PERCENT), null);
    /* Constant for background position */
    private final static ParsedValue<Size,Size> FIFTY_PERCENT =
            new ParsedValue<Size,Size>(new Size(50f, SizeUnits.PERCENT), null);
    /* Constant for background position */
    private final static ParsedValue<Size,Size> ONE_HUNDRED_PERCENT =
            new ParsedValue<Size,Size>(new Size(100f, SizeUnits.PERCENT), null);

    /*
     * http://www.w3.org/TR/css3-background/#the-background-position
     *
     * <bg-position> = [
     *   [ top | bottom ]
     * |
     *   [ <percentage> | <length> | left | center | right ]
     *   [ <percentage> | <length> | top  | center | bottom ]?
     * |
     *   [ center | [ left | right  ] [ <percentage> | <length> ]? ] &&
     *   [ center | [ top  | bottom ] [ <percentage> | <length> ]? ]
     * ]
     *
     * From the W3 spec:
     *
     * returned ParsedValue is [size, size, size, size] with the semantics
     * [top offset, right offset, bottom offset left offset]
     */
    private ParsedValue<ParsedValue<?,Size>[], BackgroundImage.BackgroundPosition> parseBackgroundPosition(Term term)
        throws ParseException {

        if (term.token == null ||
            term.token.getText() == null ||
            term.token.getText().isEmpty()) error(term, "Expected \'<bg-position>\'");

        Term  termOne = term;
        Token valueOne = term.token;

        Term  termTwo = termOne.nextInSeries;
        Token valueTwo = (termTwo != null) ? termTwo.token : null;

        Term termThree = (termTwo != null) ? termTwo.nextInSeries : null;
        Token valueThree = (termThree != null) ? termThree.token : null;

        Term termFour = (termThree != null) ? termThree.nextInSeries : null;
        Token valueFour = (termFour != null) ? termFour.token : null;

        ParsedValue<?,Size> top, right, bottom, left;
        top = right = bottom = left = ZERO_PERCENT;

        // http://www.w3.org/TR/css3-background/#the-background-position states:
        // If three or four values are given, then each <percentage> or<length>
        // represents an offset and must be preceded by a keyword, which specifies
        // from which edge the offset is given.
        if (valueTwo != null && valueThree != null) {

            //
            // to make this a little easier, if there are only three values,
            // the values are rearranged so that valueOne and valueThree are
            // the identifiers. So, if valueTwo is an identifier, everything
            // has to move.
            //
            if (valueTwo.getType() == CSSLexer.IDENT) {
                if (valueFour != null)
                    error(termTwo, "Unexpected value in \'<bg-position>\'");

                valueFour = valueThree; termFour = termThree;
                valueThree = valueTwo; termThree = termTwo;
                valueTwo = null; termTwo = null;
            }

            // Now valueTwo and valueFour are either null or a size...

            if (valueOne.getType() != CSSLexer.IDENT ||
                valueOne.getText() == null ||
                valueOne.getText().isEmpty())
                error(termOne, "Expected \'center\', \'left\' or \'right\'");

            ParsedValue<?,Size> sizeTwo = null;
            if (valueTwo != null && isSize(valueTwo)) {
                sizeTwo = parseSize(termTwo);
            } else {
                error(termTwo, "Expected \'<size>\'");
            }

            if (valueThree.getType() != CSSLexer.IDENT ||
                valueThree.getText() == null ||
                valueThree.getText().isEmpty())
                error(termThree, "Expected \'center\', \'left\' or \'right\'");

            ParsedValue<?,Size> sizeFour = null;
            if (valueFour != null) {
                if (isSize(valueFour)) {
                    sizeFour = parseSize(termFour);
                } else {
                    error(termFour, "Expected \'<size>\'");
                }
            }

            String keyword = valueOne.getText().toLowerCase();

            if ("center".equals(keyword)) {

                left = FIFTY_PERCENT;
                if (sizeTwo != null)
                    error(termTwo, "Unexpected \'<size>\'");

            } else if ("left".equals(keyword)) {

                if (sizeTwo != null) left = sizeTwo;
                else left = ZERO_PERCENT;

            } else if ("right".equals(keyword)) {

                if (sizeTwo != null) right = sizeTwo;
                else left = ONE_HUNDRED_PERCENT;

            } else {
                error(termOne, "Expected \'center\', \'left\' or \'right\'");
            }

            keyword = valueThree.getText().toLowerCase();

            if ("center".equals(keyword)) {

                top = FIFTY_PERCENT;
                if (sizeFour != null)
                    error(termFour, "Unexpected \'<size>\'");

            } else if ("top".equals(keyword)) {

                if (sizeFour != null) top = sizeFour;
                else top = ZERO_PERCENT;

            } else if ("bottom".equals(keyword)) {

                if (sizeFour != null) bottom = sizeFour;
                else top = ONE_HUNDRED_PERCENT;

            } else {
                error(termThree, "Expected \'center\', \'left\' or \'right\'");
            }

        }

        // http://www.w3.org/TR/css3-background/#the-background-position states:
        // If two values are given and at least one value is not a keyword, then
        // the first value represents the horizontal position (or offset) and the
        // second represents the vertical position (or offset). <percentage> and
        // <length> values here represent an offset of the top left corner of the
        // background image from the top left corner of the background positioning
        // area.
        else if (valueTwo != null) {

            if (valueOne.getType() == CSSLexer.IDENT) {

                String keyword =
                        (valueOne.getText() != null) ?
                            valueOne.getText().toLowerCase() : null;

                if ("center".equals(keyword)) {

                    left = FIFTY_PERCENT;

                } else if ("left".equals(keyword)) {

                    left = ZERO_PERCENT;

                } else if ("right".equals(keyword)) {

                    left = ONE_HUNDRED_PERCENT;

                } else {

                    error(termOne, "Expected \'center\', \'left\' or \'right\'");

                }

            } else if (isSize(valueOne)) {
                left = parseSize(termOne);
            } else {
                error(termOne, "Expected \'<size>\', \'center\', \'left\' or \'right\'");
            }


            if (valueTwo.getType() == CSSLexer.IDENT) {

                String keyword =
                        (valueTwo.getText() != null) ?
                            valueTwo.getText().toLowerCase() : null;

                if ("center".equals(keyword)) {

                    top = FIFTY_PERCENT;

                } else if ("top".equals(keyword)) {

                    top = ZERO_PERCENT;

                } else if ("bottom".equals(keyword)) {

                    top = ONE_HUNDRED_PERCENT;

                } else {

                    error(termTwo, "Expected \'center\', \'left\' or \'right\'");

                }

            } else if (isSize(valueTwo)) {

                top = parseSize(termTwo);

            } else {
                error(termTwo, "Expected \'<size>\', \'center\', \'left\' or \'right\'");
            }

        }

        // http://www.w3.org/TR/css3-background/#the-background-position states:
        // If only one value is specified, the second value is assumed to be 'center'.
        else {

            if (valueOne.getType() == CSSLexer.IDENT) {

                String keyword =
                        (valueOne.getText() != null) ?
                            valueOne.getText().toLowerCase() : null;

                if ("center".equals(keyword)) {

                    left = FIFTY_PERCENT;
                    top = FIFTY_PERCENT;

                } else if ("left".equals(keyword)) {

                    left = ZERO_PERCENT;
                    top = FIFTY_PERCENT;

                } else if ("right".equals(keyword)) {

                    left = ONE_HUNDRED_PERCENT;
                    top = FIFTY_PERCENT;

                } else if ("top".equals(keyword)) {

                    top = ZERO_PERCENT;
                    left = FIFTY_PERCENT;

                } else if ("bottom".equals(keyword)) {

                    top = ONE_HUNDRED_PERCENT;
                    left = FIFTY_PERCENT;

                } else {

                    error(termOne, "Expected \'center\', \'left\' or \'right\'");

                }

            } else if (isSize(valueOne)) {

                left = parseSize(termOne);
                top = FIFTY_PERCENT;

            } else {
                error(termOne, "Expected \'<size>\', \'center\', \'left\' or \'right\'");
            }

        }

        ParsedValue<?,Size>[] values = new ParsedValue[] {top, right, bottom, left};
        return new ParsedValue<ParsedValue<?,Size>[], BackgroundImage.BackgroundPosition>(values, BackgroundImage.BackgroundPositionConverter.getInstance());
    }

    private ParsedValue<ParsedValue<ParsedValue<?,Size>[], BackgroundImage.BackgroundPosition>[],BackgroundImage.BackgroundPosition[]>
            parseBackgroundPositionLayers(final Term root) throws ParseException {

        int nLayers = numberOfLayers(root);
        ParsedValue<ParsedValue<?,Size>[], BackgroundImage.BackgroundPosition>[] layers = new ParsedValue[nLayers];
        int layer = 0;
        Term term = root;
        while (term != null) {
            layers[layer++] = parseBackgroundPosition(term);
            term = nextLayer(term);
        }
        return new ParsedValue<ParsedValue<ParsedValue<?,Size>[], BackgroundImage.BackgroundPosition>[],BackgroundImage.BackgroundPosition[]>(layers,BackgroundImage.LayeredBackgroundPositionConverter.getInstance());
    }

    /*
    http://www.w3.org/TR/css3-background/#the-background-repeat
    <repeat-style> = repeat-x | repeat-y | [repeat | space | round | no-repeat]{1,2}
    */
    private ParsedValue<Repeat, Repeat>[] parseRepeatStyle(final Term root)
            throws ParseException {

        Repeat xAxis, yAxis;
        xAxis = yAxis = Repeat.NO_REPEAT;

        Term term = root;

        if (term.token == null ||
            term.token.getType() != CSSLexer.IDENT ||
            term.token.getText() == null ||
            term.token.getText().isEmpty()) error(term, "Expected \'<repeat-style>\'");

        String text = term.token.getText().toLowerCase();
        if ("repeat-x".equals(text)) {
            xAxis = Repeat.REPEAT;
            yAxis = Repeat.NO_REPEAT;
        } else if ("repeat-y".equals(text)) {
            xAxis = Repeat.NO_REPEAT;
            yAxis = Repeat.REPEAT;
        } else if ("repeat".equals(text)) {
            xAxis = Repeat.REPEAT;
            yAxis = Repeat.REPEAT;
        } else if ("space".equals(text)) {
            xAxis = Repeat.SPACE;
            yAxis = Repeat.SPACE;
        } else if ("round".equals(text)) {
            xAxis = Repeat.ROUND;
            yAxis = Repeat.ROUND;
        } else if ("no-repeat".equals(text)) {
            xAxis = Repeat.NO_REPEAT;
            yAxis = Repeat.NO_REPEAT;
        } else if ("stretch".equals(text)) {
            xAxis = Repeat.NO_REPEAT;
            yAxis = Repeat.NO_REPEAT;
        } else {
            error(term, "Expected  \'<repeat-style>\' " + text);
        }

        if ((term = term.nextInSeries) != null &&
             term.token != null &&
             term.token.getType() == CSSLexer.IDENT &&
             term.token.getText() != null &&
             !term.token.getText().isEmpty()) {

            text = term.token.getText().toLowerCase();
            if ("repeat-x".equals(text)) {
                error(term, "Unexpected \'repeat-x\'");
            } else if ("repeat-y".equals(text)) {
                error(term, "Unexpected \'repeat-y\'");
            } else if ("repeat".equals(text)) {
                yAxis = Repeat.REPEAT;
            } else if ("space".equals(text)) {
                yAxis = Repeat.SPACE;
            } else if ("round".equals(text)) {
                yAxis = Repeat.ROUND;
            } else if ("no-repeat".equals(text)) {
                yAxis = Repeat.NO_REPEAT;
            } else if ("stretch".equals(text)) {
                yAxis = Repeat.NO_REPEAT;
            } else {
                error(term, "Expected  \'<repeat-style>\'");
            }
        }

        return new ParsedValue[] {
            new ParsedValue<Repeat,Repeat>(xAxis, null),
            new ParsedValue<Repeat,Repeat>(yAxis, null)
        };
    }

    private ParsedValue<ParsedValue<Repeat, Repeat>[][],BorderImage.BorderImageRepeat[]>
            parseBorderImageRepeatStyleLayers(final Term root) throws ParseException {

        int nLayers = numberOfLayers(root);
        ParsedValue<Repeat, Repeat>[][] layers = new ParsedValue[nLayers][];
        int layer = 0;
        Term term = root;
        while (term != null) {
            layers[layer++] = parseRepeatStyle(term);
            term = nextLayer(term);
        }
        return new ParsedValue<ParsedValue<Repeat, Repeat>[][],BorderImage.BorderImageRepeat[]>(layers, BorderImage.RepeatConverter.getInstance());
    }

    private ParsedValue<ParsedValue<Repeat, Repeat>[][],BackgroundImage.BackgroundRepeat[]>
            parseBackgroundRepeatStyleLayers(final Term root) throws ParseException {

        int nLayers = numberOfLayers(root);
        ParsedValue<Repeat, Repeat>[][] layers = new ParsedValue[nLayers][];
        int layer = 0;
        Term term = root;
        while (term != null) {
            layers[layer++] = parseRepeatStyle(term);
            term = nextLayer(term);
        }
        return new ParsedValue<ParsedValue<Repeat, Repeat>[][],BackgroundImage.BackgroundRepeat[]>(layers, BackgroundImage.BackgroundRepeatConverter.getInstance());
    }

    /*
    http://www.w3.org/TR/css3-background/#the-background-size
    <bg-size> = [ <length> | <percentage> | auto ]{1,2} | cover | contain
    */
    private ParsedValue<ParsedValue[],BackgroundImage.BackgroundSize> parseBackgroundSize(final Term root)
        throws ParseException {

        ParsedValue<?,Size> height = ZERO_PERCENT, width = ZERO_PERCENT;
        boolean cover = false, contain = false;

        Term term = root;
        if (term.token == null) error(term, "Expected \'<bg-size>\'");

        if (term.token.getType() == CSSLexer.IDENT) {
            final String text =
                (term.token.getText() != null) ? term.token.getText().toLowerCase() : null;

            if ("auto".equals(text)) {
                width = ZERO_PERCENT;
                height = ZERO_PERCENT;
            } else if ("cover".equals(text)) {
                cover = true;
            } else if ("contain".equals(text)) {
                contain = true;
            } else if ("stretch".equals(text)) {
                width = ONE_HUNDRED_PERCENT;
                height = ONE_HUNDRED_PERCENT;
            } else {
                error(term, "Expected \'auto\', \'cover\', \'contain\', or  \'stretch\'");
            }
        } else if (isSize(term.token)) {
            width = parseSize(term);
            height = ZERO_PERCENT;
        } else {
            error(term, "Expected \'<bg-size>\'");
        }

        if ((term = term.nextInSeries) != null) {
            if (cover || contain) error(term, "Unexpected \'<bg-size>\'");

            if (term.token.getType() == CSSLexer.IDENT) {
                final String text =
                    (term.token.getText() != null) ? term.token.getText().toLowerCase() : null;

                if ("auto".equals(text)) {
                    height = ZERO_PERCENT;
                } else if ("cover".equals(text)) {
                    error(term, "Unexpected \'cover\'");
                } else if ("contain".equals(text)) {
                    error(term, "Unexpected \'contain\'");
                } else if ("stretch".equals(text)) {
                    height = ONE_HUNDRED_PERCENT;
                } else {
                    error(term, "Expected \'auto\' or \'stretch\'");
                }
            } else if (isSize(term.token)) {
                height = parseSize(term);
            } else {
                error(term, "Expected \'<bg-size>\'");
            }

        }

        ParsedValue[] values = new ParsedValue[] {
            width,
            height,
            // TODO: handling of booleans is really bogus
            new ParsedValue<String,Boolean>((cover ? "true" : "false"), BooleanConverter.getInstance()),
            new ParsedValue<String,Boolean>((contain ? "true" : "false"), BooleanConverter.getInstance())
        };
        return new ParsedValue<ParsedValue[],BackgroundImage.BackgroundSize>(values, BackgroundImage.BackgroundSizeConverter.getInstance());
    }

    private ParsedValue<ParsedValue<ParsedValue[],BackgroundImage.BackgroundSize>[], BackgroundImage.BackgroundSize[]>
            parseBackgroundSizeLayers(final Term root) throws ParseException {

        int nLayers = numberOfLayers(root);
        ParsedValue<ParsedValue[],BackgroundImage.BackgroundSize>[] layers = new ParsedValue[nLayers];
        int layer = 0;
        Term term = root;
        while (term != null) {
            layers[layer++] = parseBackgroundSize(term);
            term = nextLayer(term);
        }
        return new ParsedValue<ParsedValue<ParsedValue[],BackgroundImage.BackgroundSize>[], BackgroundImage.BackgroundSize[]>(layers, BackgroundImage.LayeredBackgroundSizeConverter.getInstance());
    }

    private ParsedValue<ParsedValue<?,Paint>[], Paint[]> parseBorderPaint(final Term root)
        throws ParseException {

        Term term = root;
        ParsedValue<?,Paint>[] paints = new ParsedValue[4];
        int paint = 0;

        while(term != null) {
            if (term.token == null) error(term, "Expected \'<paint>\'");
            paints[paint++] = parse(term);
            term = term.nextInSeries;
        }

        if (paint < 2) paints[1] = paints[0]; // right = top
        if (paint < 3) paints[2] = paints[0]; // bottom = top
        if (paint < 4) paints[3] = paints[1]; // left = right

        return new ParsedValue<ParsedValue<?,Paint>[], Paint[]>(paints, StrokeBorder.BorderPaintConverter.getInstance());
    }

    private ParsedValue<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]> parseBorderPaintLayers(final Term root)
        throws ParseException {

        int nLayers = numberOfLayers(root);
        ParsedValue<ParsedValue<?,Paint>[],Paint[]>[] layers = new ParsedValue[nLayers];
        int layer = 0;
        Term term = root;
        while (term != null) {
            layers[layer++] = parseBorderPaint(term);
            term = nextLayer(term);
        }
        return new ParsedValue<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]>(layers, StrokeBorder.LayeredBorderPaintConverter.getInstance());
    }

    // borderStyle (borderStyle (borderStyle borderStyle?)?)?
    private ParsedValue<ParsedValue<ParsedValue[],BorderStyle>[],BorderStyle[]> parseBorderStyleSeries(final Term root)
            throws ParseException {

        Term term = root;
        ParsedValue<ParsedValue[],BorderStyle>[] borders = new ParsedValue[4];
        int border = 0;
        while (term != null) {
            borders[border++] = parseBorderStyle(term);
            term = term.nextInSeries;
        }

        if (border < 2) borders[1] = borders[0]; // right = top
        if (border < 3) borders[2] = borders[0]; // bottom = top
        if (border < 4) borders[3] = borders[1]; // left = right

        return new ParsedValue<ParsedValue<ParsedValue[],BorderStyle>[],BorderStyle[]>(borders, StrokeBorder.BorderStyleSequenceConverter.getInstance());
    }


    private ParsedValue<ParsedValue<ParsedValue<ParsedValue[],BorderStyle>[],BorderStyle[]>[], BorderStyle[][]>
            parseBorderStyleLayers(final Term root) throws ParseException {

        int nLayers = numberOfLayers(root);
        ParsedValue<ParsedValue<ParsedValue[],BorderStyle>[],BorderStyle[]>[] layers = new ParsedValue[nLayers];
        int layer = 0;
        Term term = root;
        while (term != null) {
            layers[layer++] = parseBorderStyleSeries(term);
            term = nextLayer(term);
        }
        return new ParsedValue<ParsedValue<ParsedValue<ParsedValue[],BorderStyle>[],BorderStyle[]>[], BorderStyle[][]>(layers,StrokeBorder.LayeredBorderStyleConverter.getInstance());
    }

    // Only meant to be used from parseBorderStyle, but might be useful elsewhere
    private String getKeyword(final Term term) {
        if (term != null &&
             term.token != null &&
             term.token.getType() == CSSLexer.IDENT &&
             term.token.getText() != null &&
             !term.token.getText().isEmpty()) {

            return term.token.getText().toLowerCase();
        }
        return null;
    }

    //<border-style> [ , <border-style> ]*
    // where <border-style> =
    //      <dash-style> [centered | inside | outside]? [line-join [miter <number> | bevel | round]]? [line-cap [square | butt | round]]?
    // where <dash-style> =
    //      [ none | solid | dotted | dashed ]
    private ParsedValue<ParsedValue[],BorderStyle> parseBorderStyle(final Term root)
            throws ParseException {


        ParsedValue<ParsedValue<?, Size>[],Double[]> dashStyle = null;
        ParsedValue<ParsedValue<?,Size>,Double> dashPhase = null;
        ParsedValue<String,StrokeType> strokeType = null;
        ParsedValue<String,StrokeLineJoin> strokeLineJoin = null;
        ParsedValue<ParsedValue<?,Size>,Double> strokeMiterLimit = null;
        ParsedValue<String,StrokeLineCap> strokeLineCap = null;

        Term term = root;

        dashStyle = dashStyle(term);

        Term prev = term;
        term = term.nextInSeries;
        String keyword = getKeyword(term);

        // dash-style might be followed by 'phase <size>'
        if ("phase".equals(keyword)) {

            prev = term;
            if ((term = term.nextInSeries) == null ||
                 term.token == null ||
                 !isSize(term.token)) error(term, "Expected \'<size>\'");

            ParsedValue<?,Size> sizeVal = parseSize(term);
            dashPhase = new ParsedValue<ParsedValue<?,Size>,Double>(sizeVal,SizeConverter.getInstance());

            prev = term;
            term = term.nextInSeries;
        }

        // stroke type might be next
        strokeType = parseStrokeType(term);
        if (strokeType != null) {
            prev = term;
            term = term.nextInSeries;
        }

        keyword = getKeyword(term);

        if ("line-join".equals(keyword)) {

            prev = term;
            term = term.nextInSeries;

            ParsedValue[] lineJoinValues = parseStrokeLineJoin(term);
            if (lineJoinValues != null) {
                strokeLineJoin = lineJoinValues[0];
                strokeMiterLimit = lineJoinValues[1];
            } else {
                error(term, "Expected \'miter <size>?\', \'bevel\' or \'round\'");
            }
            prev = term;
            term = term.nextInSeries;
            keyword = getKeyword(term);
        }

        if ("line-cap".equals(keyword)) {

            prev = term;
            term = term.nextInSeries;

            strokeLineCap = parseStrokeLineCap(term);
            if (strokeLineCap == null) {
                error(term, "Expected \'square\', \'butt\' or \'round\'");
            }

            prev = term;
            term = term.nextInSeries;
        }

        if (term != null) {
            // if term is not null, then we have gotten to the end of
            // one border style and term is start of another border style
            root.nextInSeries = term;
        } else {
            // If term is null, then we have gotten to the end of
            // a border style with no more to follow in this series.
            // But there may be another layer.
            root.nextInSeries = null;
            root.nextLayer = prev.nextLayer;
        }

        final ParsedValue[] values = new ParsedValue[]{
            dashStyle,
            dashPhase,
            strokeType,
            strokeLineJoin,
            strokeMiterLimit,
            strokeLineCap
        };

        return new ParsedValue(values, StrokeBorder.BorderStyleConverter.getInstance());
    }

    //
    // segments(<size> [, <size>]+) | <border-style>
    //
    private ParsedValue<ParsedValue<?,Size>[],Double[]> dashStyle(final Term root) throws ParseException {

        if (root.token == null) error(root, "Expected \'<dash-style>\'");

        final int ttype = root.token.getType();

        ParsedValue<ParsedValue<?,Size>[],Double[]>  segments = null;
        if (ttype == CSSLexer.IDENT) {
            segments = borderStyle(root);
        } else if (ttype == CSSLexer.FUNCTION) {
            segments = segments(root);
        } else {
            error(root, "Expected \'<dash-style>\'");
        }

        return segments;
    }


    private static final ParsedValue<ParsedValue<?,Size>[],Double[]> DASHED =
            new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                new ParsedValue[] {
                    new ParsedValue<Size,Size>(new Size(5.0f, SizeUnits.PX), null),
                    new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.PX), null)
                }, SizeConverter.SequenceConverter.getInstance());

    private static final ParsedValue<ParsedValue<?,Size>[],Double[]> DOTTED =
            new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                new ParsedValue[]{
                    new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.PX), null),
                    new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.PX), null)
                }, SizeConverter.SequenceConverter.getInstance());

    private static final ParsedValue<ParsedValue<?,Size>[],Double[]> SOLID =
            new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                new ParsedValue[]{
                    /* empty array */
                }, SizeConverter.SequenceConverter.getInstance());


    private static final ParsedValue<ParsedValue<?,Size>[],Double[]> NONE =
            new ParsedValue<ParsedValue<?,Size>[],Double[]>(null, SizeConverter.SequenceConverter.getInstance());

    /*
    <border-style> = none | hidden | dotted | dashed | solid | double | groove | ridge | inset | outset
    */
    private ParsedValue<ParsedValue<?,Size>[],Double[]>  borderStyle(Term root)
            throws ParseException {

        if (root.token == null ||
            root.token.getType() != CSSLexer.IDENT ||
            root.token.getText() == null ||
            root.token.getText().isEmpty()) error(root, "Expected \'<border-style>\'");

        final String text = root.token.getText().toLowerCase();

        if ("none".equals(text)) {
            return NONE;
        } else if ("hidden".equals(text)) {
            return NONE;
        } else if ("dotted".equals(text)) {
            return DOTTED;
        } else if ("dashed".equals(text)) {
            return DASHED;
        } else if ("solid".equals(text)) {
            return SOLID;
        } else if ("double".equals(text)) {
            error(root, "Unsupported <border-style> \'double\'");
        } else if ("groove".equals(text)) {
            error(root, "Unsupported <border-style> \'groove\'");
        } else if ("ridge".equals(text)) {
            error(root, "Unsupported <border-style> \'ridge\'");
        } else if ("inset".equals(text)) {
            error(root, "Unsupported <border-style> \'inset\'");
        } else if ("outset".equals(text)) {
            error(root, "Unsupported <border-style> \'outset\'");
        } else {
            error(root, "Unsupported <border-style> \'" + text + "\'");
        }
        // error throws so we should never get here.
        // but the compiler wants a return, so here it is.
        return SOLID;
    }

    private ParsedValue<ParsedValue<?,Size>[],Double[]> segments(Term root)
            throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (!"segments".regionMatches(true, 0, fn, 0, 8)) {
            error(root,"Expected \'segments\'");
        }

        Term arg = root.firstArg;
        if (arg == null) error(arg, "Expected \'<size>\'");

        int nArgs = numberOfArgs(root);
        ParsedValue<?,Size>[] segments = new ParsedValue[nArgs];
        int segment = 0;
        while(arg != null) {
            segments[segment++] = parseSize(arg);
            arg = arg.nextArg;
        }

        return new ParsedValue<ParsedValue<?,Size>[],Double[]>(segments,SizeConverter.SequenceConverter.getInstance());

    }

    private ParsedValue<String,StrokeType> parseStrokeType(final Term root)
        throws ParseException {

        final String keyword = getKeyword(root);


        if ("centered".equals(keyword) ||
            "inside".equals(keyword) ||
            "outside".equals(keyword)) {

            return new ParsedValue<String,StrokeType>(keyword, new EnumConverter(StrokeType.class));

        }
        return null;
    }

    // Root term is the term just after the line-join keyword
    // Returns an array of two Values or null.
    // ParsedValue[0] is ParsedValue<StrokeLineJoin,StrokeLineJoin>
    // ParsedValue[1] is ParsedValue<Value<?,Size>,Double> if miter limit is given, null otherwise.
    // If the token is not a StrokeLineJoin, then null is returned.
    private ParsedValue[] parseStrokeLineJoin(final Term root)
        throws ParseException {

        final String keyword = getKeyword(root);

        if ("miter".equals(keyword) ||
            "bevel".equals(keyword) ||
            "round".equals(keyword)) {

            ParsedValue<String,StrokeLineJoin> strokeLineJoin =
                    new ParsedValue<String,StrokeLineJoin>(keyword, new EnumConverter(StrokeLineJoin.class));

            ParsedValue<ParsedValue<?,Size>,Double> strokeMiterLimit = null;
            if ("miter".equals(keyword)) {

                Term next = root.nextInSeries;
                if (next != null &&
                    next.token != null &&
                    isSize(next.token)) {

                    root.nextInSeries = next.nextInSeries;
                    ParsedValue<?,Size> sizeVal = parseSize(next);
                    strokeMiterLimit = new ParsedValue<ParsedValue<?,Size>,Double>(sizeVal,SizeConverter.getInstance());
                }

            }

            return new ParsedValue[] { strokeLineJoin, strokeMiterLimit };
        }
        return null;
    }

    // Root term is the term just after the line-cap keyword
    // If the token is not a StrokeLineCap, then null is returned.
    private ParsedValue<String,StrokeLineCap> parseStrokeLineCap(final Term root)
        throws ParseException {

        final String keyword = getKeyword(root);

        if ("square".equals(keyword) ||
            "butt".equals(keyword) ||
            "round".equals(keyword)) {

            return new ParsedValue<String,StrokeLineCap>(keyword, new EnumConverter(StrokeLineCap.class));
        }
        return null;
    }

    /*
     * http://www.w3.org/TR/css3-background/#the-border-image-slice
     * [<number> | <percentage>]{1,4} && fill?
     */
    private ParsedValue<ParsedValue[],BorderImage.BorderImageSlice> parseBorderImageSlice(final Term root)
        throws ParseException {

        Term term = root;
        if (term.token == null || !isSize(term.token))
                error(term, "Expected \'<size>\'");

        ParsedValue<?,Size>[] insets = new ParsedValue[4];
        Boolean fill = Boolean.FALSE;

        int inset = 0;
        while (inset < 4 && term != null) {
            insets[inset++] = parseSize(term);

            if ((term = term.nextInSeries) != null &&
                 term.token != null &&
                 term.token.getType() == CSSLexer.IDENT) {

                if("fill".equalsIgnoreCase(term.token.getText())) {
                    fill = Boolean.TRUE;
                    break;
                }
            }
        }

        if (inset < 2) insets[1] = insets[0]; // right = top
        if (inset < 3) insets[2] = insets[0]; // bottom = top
        if (inset < 4) insets[3] = insets[1]; // left = right

        ParsedValue[] values = new ParsedValue[] {
                new ParsedValue<ParsedValue<?,Size>[],Insets>(insets, InsetsConverter.getInstance()),
                new ParsedValue<Boolean,Boolean>(fill, null)
        };
        return new ParsedValue<ParsedValue[], BorderImage.BorderImageSlice>(values, BorderImage.SliceConverter.getInstance());
    }

    private ParsedValue<ParsedValue<ParsedValue[],BorderImage.BorderImageSlice>[],BorderImage.BorderImageSlice[]>
            parseBorderImageSliceLayers(final Term root) throws ParseException {

        int nLayers = numberOfLayers(root);
        ParsedValue<ParsedValue[], BorderImage.BorderImageSlice>[] layers = new ParsedValue[nLayers];
        int layer = 0;
        Term term = root;
        while (term != null) {
            layers[layer++] = parseBorderImageSlice(term);
            term = nextLayer(term);
        }
        return new ParsedValue<ParsedValue<ParsedValue[],BorderImage.BorderImageSlice>[],BorderImage.BorderImageSlice[]> (layers, BorderImage.SliceSequenceConverter.getInstance());
    }



    // parse a URI value
    // i.e., url("<uri>")
    private ParsedValue<ParsedValue[],String> parseURI(Term root)
            throws ParseException {

        // first term in the chain is the function name...
        final String fn = (root.token != null) ? root.token.getText() : null;
        if (!"url".regionMatches(true, 0, fn, 0, 3)) {
            error(root,"Expected \'url\'");
        }

        Term arg = root.firstArg;
        if (arg == null) error(root, "Expected \'url(\"<uri-string>\")\'");

        if (arg.token == null ||
            arg.token.getType() != CSSLexer.STRING ||
            arg.token.getText() == null ||
            arg.token.getText().isEmpty()) error(arg, "Excpected \'url(\"<uri-string>\")\'");

        final String uri = arg.token.getText();
        ParsedValue[] uriValues = new ParsedValue[] {
            new ParsedValue<String,String>(uri, StringConverter.getInstance()),
            new ParsedValue<URL,URL>(sourceOfStylesheet, null)
        };
        return new ParsedValue<ParsedValue[],String>(uriValues, URLConverter.getInstance());
    }

    // parse a series of URI values separated by commas.
    // i.e., <uri> [, <uri>]*
    private ParsedValue<ParsedValue<ParsedValue[],String>[],String[]> parseURILayers(Term root)
            throws ParseException {

        int nLayers = numberOfLayers(root);

        Term temp = root;
        int layer = 0;
        ParsedValue<ParsedValue[],String>[] layers = new ParsedValue[nLayers];

        while(temp != null) {
            layers[layer++] = parseURI(temp);
            temp = nextLayer(temp);
        }

        return new ParsedValue<ParsedValue<ParsedValue[],String>[],String[]>(layers, URLConverter.SequenceConverter.getInstance());
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // http://www.w3.org/TR/css3-fonts
    //
    ////////////////////////////////////////////////////////////////////////////

    /* http://www.w3.org/TR/css3-fonts/#font-size-the-font-size-property */
    private ParsedValue<ParsedValue<?,Size>,Double> parseFontSize(final Term root) throws ParseException {

        if (root == null) return null;
        final Token token = root.token;
        if (token == null || !isSize(token)) error(root, "Expected \'<font-size>\'");

        Size size = null;
        if (token.getType() == CSSLexer.IDENT) {
            final String ident = token.getText().toLowerCase();
            double value = -1;
            if ("inherit".equals(ident)) {
                value = 100;
            } else if ("xx-small".equals(ident)) {
                value = 60;
            } else if ("x-small".equals(ident)) {
                value = 75;
            } else if ("small".equals(ident)) {
                value = 80;
            } else if ("medium".equals(ident)) {
                value = 100;
            } else if ("large".equals(ident)) {
                value = 120;
            } else if ("x-large".equals(ident)) {
                value = 150;
            } else if ("xx-large".equals(ident)) {
                value = 200;
            } else if ("smaller".equals(ident)) {
                value = 80;
            } else if ("larger".equals(ident)) {
                value = 120;
            }

            if (value > -1) {
                size = new Size(value, SizeUnits.PERCENT);
            }
        }

        // if size is null, then size is not one of the keywords above.
        if (size == null) {
            size = size(token);
        }

        ParsedValue<?,Size> svalue = new ParsedValue<Size,Size>(size, null);
        return new ParsedValue<ParsedValue<?,Size>,Double>(svalue, FontConverter.SizeConverter.getInstance());
    }

    /* http://www.w3.org/TR/css3-fonts/#font-style-the-font-style-property */
    private ParsedValue<FontUnits.Style,FontPosture> parseFontStyle(Term root) throws ParseException {

        if (root == null) return null;
        final Token token = root.token;
        if (token == null ||
            token.getType() != CSSLexer.IDENT ||
            token.getText() == null ||
            token.getText().isEmpty()) error(root, "Expected \'<font-style>\'");

        final String ident = token.getText().toLowerCase();
        FontUnits.Style style = null;

        if ("normal".equals(ident)) {
            style = FontUnits.Style.NORMAL;
        } else if ("italic".equals(ident)) {
            style = FontUnits.Style.ITALIC;
        } else if ("oblique".equals(ident)) {
            style = FontUnits.Style.OBLIQUE;
        } else if ("inherit".equals(ident)) {
            style = FontUnits.Style.OBLIQUE;
        } else {
            return null;
        }

        return new ParsedValue<FontUnits.Style,FontPosture>(style, FontConverter.StyleConverter.getInstance());
    }

    /* http://www.w3.org/TR/css3-fonts/#font-weight-the-font-weight-property */
    private ParsedValue<FontUnits.Weight,FontWeight> parseFontWeight(Term root) throws ParseException {

        if (root == null) return null;
        final Token token = root.token;
        if (token == null ||
            token.getType() != CSSLexer.IDENT ||
            token.getText() == null ||
            token.getText().isEmpty()) error(root, "Expected \'<font-weight>\'");

        final String ident = token.getText().toLowerCase();
        FontUnits.Weight weight = null;

        if ("inherit".equals(ident)) {
            weight = FontUnits.Weight.INHERIT;
        } else if ("normal".equals(ident)) {
            weight = FontUnits.Weight.NORMAL;
        } else if ("bold".equals(ident)) {
            weight = FontUnits.Weight.BOLD;
        } else if ("bolder".equals(ident)) {
            weight = FontUnits.Weight.BOLDER;
        } else if ("lighter".equals(ident)) {
            weight = FontUnits.Weight.LIGHTER;
        } else if ("100".equals(ident)) {
            weight = FontUnits.Weight.SCALE_100;
        } else if ("200".equals(ident)) {
            weight = FontUnits.Weight.SCALE_200;
        } else if ("300".equals(ident)) {
            weight = FontUnits.Weight.SCALE_300;
        } else if ("400".equals(ident)) {
            weight = FontUnits.Weight.SCALE_400;
        } else if ("500".equals(ident)) {
            weight = FontUnits.Weight.SCALE_500;
        } else if ("600".equals(ident)) {
            weight = FontUnits.Weight.SCALE_600;
        } else if ("700".equals(ident)) {
            weight = FontUnits.Weight.SCALE_700;
        } else if ("800".equals(ident)) {
            weight = FontUnits.Weight.SCALE_800;
        } else {
            return null;
        }

        return new ParsedValue<FontUnits.Weight,FontWeight>(weight, FontConverter.WeightConverter.getInstance());
    }

    private ParsedValue<String,String>  parseFontFamily(Term root) throws ParseException {

        if (root == null) return null;
        final Token token = root.token;
        String text = null;
        if (token == null ||
            (token.getType() != CSSLexer.IDENT &&
             token.getType() != CSSLexer.STRING) ||
            (text = token.getText()) == null ||
            text.isEmpty()) error(root, "Expected \'<font-family>\'");

        final String fam = stripQuotes(text.toLowerCase());
        if ("inherit".equals(fam)) {
            return new ParsedValue<String,String>("inherit", StringConverter.getInstance());
        } else if ("serif".equals(fam) ||
            "sans-serif".equals(fam) ||
            "cursive".equals(fam) ||
            "fantasy".equals(fam) ||
            "monospace".equals(fam)) {
            return new ParsedValue<String,String>(fam, StringConverter.getInstance());
        } else {
            return new ParsedValue<String,String>(token.getText(), StringConverter.getInstance());
        }
    }

    // (fontStyle || fontVariant || fontWeight)* fontSize (SOLIDUS size)? fontFamily
    private ParsedValue<ParsedValue[],Font> parseFont(Term root) throws ParseException {

        // Because style, variant, weight, size and family can inherit
        // AND style, variant and weight are optional, parsing this backwards
        // is easier.
        Term next = root.nextInSeries;
        root.nextInSeries = null;
        while (next != null) {
            Term temp = next.nextInSeries;
            next.nextInSeries = root;
            root = next;
            next = temp;
        }

        // Now, root should point to fontFamily
        Token token = root.token;
        int ttype = token.getType();
        if (ttype != CSSLexer.IDENT &&
            ttype != CSSLexer.STRING) error(root, "Expected \'<font-family>\'");
        ParsedValue<String,String> ffamily = parseFontFamily(root);

        Term term = root;
        if ((term = term.nextInSeries) == null) error(root, "Expected \'<size>\'");
        if (term.token == null || !isSize(term.token)) error(term, "Expected \'<size>\'");

        // Now, term could be the font size or it could be the line-height.
        // If the next term is a forward slash, then it's line-height.
        Term temp;
        if (((temp = term.nextInSeries) != null) &&
            (temp.token != null && temp.token.getType() == CSSLexer.SOLIDUS)) {

            root = temp;

            if ((term = temp.nextInSeries) == null) error(root, "Expected \'<size>\'");
            if (term.token == null || !isSize(term.token)) error(term, "Expected \'<size>\'");

            token = term.token;
        }

        ParsedValue<ParsedValue<?,Size>,Double> fsize = parseFontSize(term);
        if (fsize == null) error(root, "Expected \'<size>\'");

        ParsedValue<FontUnits.Style,FontPosture> fstyle = null;
        ParsedValue<FontUnits.Weight,FontWeight> fweight = null;
        String fvariant = null;

        while ((term = term.nextInSeries) != null) {

            if (term.token == null ||
                term.token.getType() != CSSLexer.IDENT ||
                term.token.getText() == null ||
                term.token.getText().isEmpty())
                error(term, "Expected \'<font-weight>\', \'<font-style>\' or \'<font-variant>\'");

            if (fstyle == null && ((fstyle = parseFontStyle(term)) != null)) {
                ;
            } else if (fvariant == null && "small-caps".equalsIgnoreCase(term.token.getText())) {
                fvariant = term.token.getText();
            } else if (fweight == null && ((fweight = parseFontWeight(term)) != null)) {
                ;
            }
        }

        ParsedValue[] values = new ParsedValue[]{ ffamily, fsize, fweight, fstyle };
        return new ParsedValue<ParsedValue[],Font>(values, FontConverter.getInstance());
    }

    //
    // Parser state machine
    //
    Token currentToken = null;

    // return the next token that is not whitespace.
    private Token nextToken(CSSLexer lexer) {

        Token token = null;

        do {
            token = lexer.nextToken();
        } while ((token != null) &&
                (token.getType() == CSSLexer.WS) ||
                (token.getType() == CSSLexer.NL));

        if (LOGGER.isLoggable(PlatformLogger.FINEST)) {
            LOGGER.finest(token.toString());
        }

        return token;

    }

    private void parse(Stylesheet stylesheet, CSSLexer lexer) {

        // need to read the first token
        currentToken = nextToken(lexer);

        while ((currentToken != null) &&
               (currentToken.getType() != Token.EOF)) {

            List<Selector> selectors = selectors(lexer);
            if (selectors == null) return;

            if ((currentToken == null) ||
                (currentToken.getType() != CSSLexer.LBRACE)) {
                    final int line = currentToken != null ? currentToken.getLine() : -1;
                    final int pos = currentToken != null ? currentToken.getOffset() : -1;
                    final String msg = 
                        MessageFormat.format("Expected LBRACE at [{0,number,#},{1,number,#}]",
                        line,pos); 
                    CssError error = createError(msg);
                    if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                        LOGGER.warning(error.toString());
                    }
                    reportError(error);
                currentToken = null;
                return;
            }

            // get past the LBRACE
            currentToken = nextToken(lexer);

            List<Declaration> declarations = declarations(lexer);
            if (declarations == null) return;

            if ((currentToken != null) &&
                (currentToken.getType() != CSSLexer.RBRACE)) {
                    final int line = currentToken != null ? currentToken.getLine() : -1;
                    final int pos = currentToken != null ? currentToken.getOffset() : -1;
                    final String msg = 
                        MessageFormat.format("Expected RBRACE at [{0,number,#},{1,number,#}]",
                        line,pos);
                    CssError error = createError(msg);                    
                    if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                        LOGGER.warning(error.toString());
                    }
                    reportError(error);
                currentToken = null;
                return;
            }

            stylesheet.getRules().add(new Rule(selectors, declarations));

            currentToken = nextToken(lexer);

        }
        currentToken = null;
    }

    private List<Selector> selectors(CSSLexer lexer) {

        List<Selector> selectors = new ArrayList<Selector>();

        while(true) {
            Selector selector = selector(lexer);
            if (selector == null) {
                // some error happened, skip the rule...
                while ((currentToken != null) &&
                       (currentToken.getType() != CSSLexer.RBRACE) &&
                       (currentToken.getType() != Token.EOF)) {
                    currentToken = nextToken(lexer);
                }

                // current token is either RBRACE or EOF. Calling
                // currentToken will get the next token or EOF.
                currentToken = nextToken(lexer);

                // skipped the last rule?
                if (currentToken == null || currentToken.getType() == Token.EOF) {
                    currentToken = null;
                    return null;
                }

                continue;
            }
            selectors.add(selector);

            if ((currentToken != null) &&
                (currentToken.getType() == CSSLexer.COMMA)) {
                // get past the comma
                currentToken = nextToken(lexer);
                continue;
            }

            // currentToken was either null or not a comma
            // so we are done with selectors.
            break;
        }

        return selectors;
    }

    private Selector selector(CSSLexer lexer) {

        List<Combinator> combinators = null;
        List<SimpleSelector> sels = null;

        SimpleSelector ancestor = simpleSelector(lexer);
        if (ancestor == null) return null;

        while (true) {
            Combinator comb = combinator(lexer);
            if (comb != null) {
                if (combinators == null) {
                    combinators = new ArrayList<Combinator>();
                }
                combinators.add(comb);
                SimpleSelector descendant = simpleSelector(lexer);
                if (descendant == null) return null;
                if (sels == null) {
                    sels = new ArrayList<SimpleSelector>();
                    sels.add(ancestor);
                }
                sels.add(descendant);
            } else {
                break;
            }
        }

        // RT-15473
        // We might return from selector with a NL token instead of an
        // LBRACE, so skip past the NL here.
        if (currentToken != null && currentToken.getType() == CSSLexer.NL) {
            currentToken = nextToken(lexer);
        }


        if (sels == null) {
            return ancestor;
        } else {
            return new CompoundSelector(sels,combinators);
        }

    }

    private SimpleSelector simpleSelector(CSSLexer lexer) {

        String esel = "*"; // element selector. default to universal
        String isel = ""; // id selector
        List<String>  csels = null; // class selector
        List<String> pclasses = null; // pseudoclasses

        while (true) {

            final int ttype =
                (currentToken != null) ? currentToken.getType() : Token.INVALID;

            switch(ttype) {
                // element selector
                case CSSLexer.STAR:
                case CSSLexer.IDENT:
                    esel = currentToken.getText();
                    break;

                // class selector
                case CSSLexer.DOT:
                    currentToken = nextToken(lexer);
                    if (currentToken != null &&
                        currentToken.getType() == CSSLexer.IDENT) {
                        if (csels == null) {
                            csels = new ArrayList<String>();
                        }
                        csels.add(currentToken.getText());
                    } else {
                        currentToken = Token.INVALID_TOKEN;
                        return null;
                    }
                    break;

                // id selector
                case CSSLexer.HASH:
                    isel = currentToken.getText().substring(1);
                    break;

                case CSSLexer.COLON:
                    currentToken = nextToken(lexer);
                    if (currentToken != null &&
                        currentToken.getType() == CSSLexer.IDENT) {
                        if (pclasses == null) {
                            pclasses = new ArrayList<String>();
                        }
                        pclasses.add(currentToken.getText());
                    } else {
                        currentToken = Token.INVALID_TOKEN;
                        return null;
                    }
                    break;

                case CSSLexer.NL:
                case CSSLexer.WS:
                case CSSLexer.COMMA:
                case CSSLexer.GREATER:
                case CSSLexer.LBRACE:
                case Token.EOF:
                    return new SimpleSelector(esel, csels, pclasses, isel);

                default:
                    return null;


            }

            // get the next token, but don't skip whitespace
            // since it may be a combinator
            currentToken = lexer.nextToken();
            if (LOGGER.isLoggable(PlatformLogger.FINEST)) {
                LOGGER.finest(currentToken.toString());
            }
        }
    }

    private Combinator combinator(CSSLexer lexer) {

        Combinator combinator = null;

        while (true) {

            final int ttype =
                (currentToken != null) ? currentToken.getType() : Token.INVALID;

            switch(ttype) {

                case CSSLexer.WS:
                    // need to check if combinator is null since child token
                    // might be surrounded by whitespace.
                    if (combinator == null && " ".equals(currentToken.getText())) {
                        combinator = Combinator.DESCENDANT;
                    }
                    break;

                case CSSLexer.GREATER:
                    // no need to check if combinator is null here
                    combinator = Combinator.CHILD;
                    break;

                case CSSLexer.STAR:
                case CSSLexer.IDENT:
                case CSSLexer.DOT:
                case CSSLexer.HASH:
                case CSSLexer.COLON:
                    return combinator;

                default:
                    // only selector is expected
                    return null;

            }

            // get the next token, but don't skip whitespace
            currentToken = lexer.nextToken();
            if (LOGGER.isLoggable(PlatformLogger.FINEST)) {
                LOGGER.finest(currentToken.toString());
            }
        }
    }

    private List<Declaration> declarations(CSSLexer lexer) {

        List<Declaration> declarations = new ArrayList<Declaration>();

        while (true) {

            Declaration decl = declaration(lexer);
            if (decl != null) {
                declarations.add(decl);
            } else {
                // some error happened, skip the decl...
                while ((currentToken != null) &&
                       (currentToken.getType() != CSSLexer.SEMI) &&
                       (currentToken.getType() != CSSLexer.RBRACE) &&
                       (currentToken.getType() != Token.EOF)) {
                    currentToken = nextToken(lexer);
                }

                // current token is either SEMI, RBRACE or EOF.
                if (currentToken == null &&
                    currentToken.getType() != CSSLexer.SEMI)
                    return declarations;
            }

            // declaration; declaration; ???
            // RT-17830 - allow declaration;;
            while ((currentToken != null) &&
                    (currentToken.getType() == CSSLexer.SEMI)) {
                currentToken = nextToken(lexer);
            }

            // if it is delcaration; declaration, then the
            // next token should be an IDENT.
            if ((currentToken != null) &&
                (currentToken.getType() == CSSLexer.IDENT)) {
                continue;
            }

            break;
        }

        return declarations;
    }

    private Declaration declaration(CSSLexer lexer) {

        final int ttype =
            (currentToken != null) ? currentToken.getType() : Token.INVALID;

        if ((currentToken == null) ||
            (currentToken.getType() != CSSLexer.IDENT)) {
//            
//            RT-16547: this warning was misleading because an empty rule 
//            not invalid. Some people put in empty rules just as placeholders.
//            
//            if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
//                final int line = currentToken != null ? currentToken.getLine() : -1;
//                final int pos = currentToken != null ? currentToken.getOffset() : -1;
//                final String url = 
//                    (stylesheet != null && stylesheet.getUrl() != null) ? 
//                        stylesheet.getUrl().toExternalForm() : "?";
//                LOGGER.warning("Expected IDENT at {0}[{1,number,#},{2,number,#}]",
//                    url,line,pos);
//            }
            return null;
        }

        String property = currentToken.getText();

        currentToken = nextToken(lexer);

        if ((currentToken == null) ||
            (currentToken.getType() != CSSLexer.COLON)) {
                final int line = currentToken != null ? currentToken.getLine() : -1;
                final int pos = currentToken != null ? currentToken.getOffset() : -1;
                final String msg = 
                        MessageFormat.format("Expected COLON at [{0,number,#},{1,number,#}]",
                    line,pos);
                CssError error = createError(msg);
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning(error.toString());
                }
                reportError(error);
            return null;
        }

        currentToken = nextToken(lexer);

        Term root = expr(lexer);
        ParsedValue value = null;
        try {
            value = (root != null) ? valueFor(property, root) : null;
        } catch (ParseException re) {
                Token badToken = re.tok;
                final int line = badToken != null ? badToken.getLine() : -1;
                final int pos = badToken != null ? badToken.getOffset() : -1;
                final String msg = 
                        MessageFormat.format("{2} while parsing ''{3}'' at [{0,number,#},{1,number,#}]",
                    line,pos,re.getMessage(),property);
                CssError error = createError(msg);
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning(error.toString());
                }
                reportError(error);
            return null;
        }

        boolean important = currentToken.getType() == CSSLexer.IMPORTANT_SYM;
        if (important) currentToken = nextToken(lexer);

        Declaration decl = (value != null)
                ? new Declaration(property, value, important) : null;
        return decl;
    }

    private Term expr(CSSLexer lexer) {

        final Term expr = term(lexer);
        Term current = expr;

        while(true) {

            final int ttype =
                (currentToken != null) ? currentToken.getType() : Token.INVALID;

            if (ttype == Token.INVALID) {
                skipExpr(lexer);
                return null;
            } else if (ttype == CSSLexer.SEMI ||
                ttype == CSSLexer.IMPORTANT_SYM ||
                ttype == CSSLexer.RBRACE ||
                ttype == Token.EOF) {
                return expr;
            } else if (ttype == CSSLexer.COMMA) {
            // comma breaks up sequences of terms.
                // next series of terms chains off the last term in
                // the current series.
                currentToken = nextToken(lexer);
                current = current.nextLayer = term(lexer);
            } else {
                current = current.nextInSeries = term(lexer);
            }

        }
    }
    
    private void skipExpr(CSSLexer lexer) {
        
        while(true) {
            
            currentToken = nextToken(lexer);

            final int ttype =
                (currentToken != null) ? currentToken.getType() : Token.INVALID;
            
            if (ttype == CSSLexer.SEMI ||
                ttype == CSSLexer.RBRACE ||
                ttype == Token.EOF) {
                return;
            } 
        }
    }

    private Term term(CSSLexer lexer) {

        final int ttype =
            (currentToken != null) ? currentToken.getType() : Token.INVALID;

        switch (ttype) {

            case CSSLexer.NUMBER:
            case CSSLexer.CM:
            case CSSLexer.EMS:
            case CSSLexer.EXS:
            case CSSLexer.IN:
            case CSSLexer.MM:
            case CSSLexer.PC:
            case CSSLexer.PT:
            case CSSLexer.PX:
            case CSSLexer.DEG:
            case CSSLexer.GRAD:
            case CSSLexer.RAD:
            case CSSLexer.TURN:
            case CSSLexer.PERCENTAGE:
                break;

            case CSSLexer.STRING:
                break;
            case CSSLexer.IDENT:
                break;

            case CSSLexer.HASH:
                break;

            case CSSLexer.FUNCTION:
            case CSSLexer.LPAREN:

                Term function = new Term(currentToken);
                currentToken = nextToken(lexer);

                Term arg = term(lexer);
                function.firstArg = arg;

                while(true) {

                    final int operator =
                        currentToken != null ? currentToken.getType() : Token.INVALID;

                    if (operator == CSSLexer.RPAREN) {
                        currentToken = nextToken(lexer);
                        return function;
                    } else if (operator == CSSLexer.COMMA) {
                        // comma breaks up sequences of terms.
                        // next series of terms chains off the last term in
                        // the current series.
                        currentToken = nextToken(lexer);
                        arg = arg.nextArg = term(lexer);

                    } else {
                        arg = arg.nextInSeries = term(lexer);
                    }

                }

            default:
                final int line = currentToken != null ? currentToken.getLine() : -1;
                final int pos = currentToken != null ? currentToken.getOffset() : -1;
                final String text = currentToken != null ? currentToken.getText() : "";
                final String msg = 
                    MessageFormat.format("Unexpected token {0}{1}{0} at [{2,number,#},{3,number,#}]",
                    "\'",text,line,pos);
                CssError error = createError(msg);
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning(error.toString());
                }
                reportError(error);
                return null;
//                currentToken = nextToken(lexer);
//
//                return new Term(Token.INVALID_TOKEN);
        }

        Term term = new Term(currentToken);
        currentToken = nextToken(lexer);
        return term;
    }
}
