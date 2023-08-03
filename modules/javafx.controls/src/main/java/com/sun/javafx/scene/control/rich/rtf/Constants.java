/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package com.sun.javafx.scene.control.rich.rtf;

/**
   Class to hold dictionary keys used by the RTF reader/writer.
   These should be moved into StyleConstants.
*/
class Constants
{
    /** An array of TabStops */
    static final String Tabs = "tabs";

    /** The name of the character set the original RTF file was in */
    static final String RTFCharacterSet = "rtfCharacterSet";

    /** Indicates the domain of a Style */
    static final String StyleType = "style:type";

    /** Value for StyleType indicating a section style */
    static final String STSection = "section";
    /** Value for StyleType indicating a paragraph style */
    static final String STParagraph = "paragraph";
    /** Value for StyleType indicating a character style */
    static final String STCharacter = "character";

    /** The style of the text following this style */
    static final String StyleNext = "style:nextStyle";

    /** Whether the style is additive */
    static final String StyleAdditive = "style:additive";

    /** Whether the style is hidden from the user */
    static final String StyleHidden = "style:hidden";

    /* Miscellaneous character attributes */
    static final String Caps          = "caps";
    static final String Deleted       = "deleted";
    static final String Outline       = "outl";
    static final String SmallCaps     = "scaps";
    static final String Shadow        = "shad";
    static final String Strikethrough = "strike";
    static final String Hidden        = "v";

    /* Miscellaneous document attributes */
    static final String PaperWidth    = "paperw";
    static final String PaperHeight   = "paperh";
    static final String MarginLeft    = "margl";
    static final String MarginRight   = "margr";
    static final String MarginTop     = "margt";
    static final String MarginBottom  = "margb";
    static final String GutterWidth   = "gutter";

    /* This is both a document and a paragraph attribute */
    static final String WidowControl  = "widowctrl";
}
