/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import javafx.scene.control.rich.model.StyleAttribute;
import javafx.scene.control.rich.model.StyleAttrs;

class RTFAttributes {
    static RTFAttribute[] attributes;

    static {
        ArrayList<RTFAttribute> a = new ArrayList<RTFAttribute>();
        int CHR = RTFAttribute.D_CHARACTER;
        int PGF = RTFAttribute.D_PARAGRAPH;
        int SEC = RTFAttribute.D_SECTION;
        int DOC = RTFAttribute.D_DOCUMENT;
        int PST = RTFAttribute.D_META;
        Boolean True = Boolean.valueOf(true);
        Boolean False = Boolean.valueOf(false);

        a.add(new BooleanAttribute(CHR, StyleAttrs.ITALIC, "i"));
        a.add(new BooleanAttribute(CHR, StyleAttrs.BOLD, "b"));
        a.add(new BooleanAttribute(CHR, StyleAttrs.UNDERLINE, "ul"));
//        a.add(NumericAttribute.NewTwips(PGF, StyleConstants.LeftIndent, "li", 0f, 0));
//        a.add(NumericAttribute.NewTwips(PGF, StyleConstants.RightIndent, "ri", 0f, 0));
//        a.add(NumericAttribute.NewTwips(PGF, StyleConstants.FirstLineIndent, "fi", 0f, 0));

//        a.add(new AssertiveAttribute(PGF, StyleConstants.Alignment, "ql", StyleConstants.ALIGN_LEFT));
//        a.add(new AssertiveAttribute(PGF, StyleConstants.Alignment, "qr", StyleConstants.ALIGN_RIGHT));
//        a.add(new AssertiveAttribute(PGF, StyleConstants.Alignment, "qc", StyleConstants.ALIGN_CENTER));
//        a.add(new AssertiveAttribute(PGF, StyleConstants.Alignment, "qj", StyleConstants.ALIGN_JUSTIFIED));
//        a.add(NumericAttribute.NewTwips(PGF, StyleConstants.SpaceAbove, "sa", 0));
//        a.add(NumericAttribute.NewTwips(PGF, StyleConstants.SpaceBelow, "sb", 0));

//        a.add(new AssertiveAttribute(PST, RTFReader.TabAlignmentKey, "tqr", TabStop.ALIGN_RIGHT));
//        a.add(new AssertiveAttribute(PST, RTFReader.TabAlignmentKey, "tqc", TabStop.ALIGN_CENTER));
//        a.add(new AssertiveAttribute(PST, RTFReader.TabAlignmentKey, "tqdec", TabStop.ALIGN_DECIMAL));

//        a.add(new AssertiveAttribute(PST, RTFReader.TabLeaderKey, "tldot", TabStop.LEAD_DOTS));
//        a.add(new AssertiveAttribute(PST, RTFReader.TabLeaderKey, "tlhyph", TabStop.LEAD_HYPHENS));
//        a.add(new AssertiveAttribute(PST, RTFReader.TabLeaderKey, "tlul", TabStop.LEAD_UNDERLINE));
//        a.add(new AssertiveAttribute(PST, RTFReader.TabLeaderKey, "tlth", TabStop.LEAD_THICKLINE));
//        a.add(new AssertiveAttribute(PST, RTFReader.TabLeaderKey, "tleq", TabStop.LEAD_EQUALS));

        /* The following aren't actually recognized by Swing */
//        a.add(new BooleanAttribute(CHR, Constants.Caps, "caps"));
//        a.add(new BooleanAttribute(CHR, Constants.Outline, "outl"));
//        a.add(new BooleanAttribute(CHR, Constants.SmallCaps, "scaps"));
//        a.add(new BooleanAttribute(CHR, Constants.Shadow, "shad"));
//        a.add(new BooleanAttribute(CHR, Constants.Hidden, "v"));
        a.add(new BooleanAttribute(CHR, StyleAttrs.STRIKE_THROUGH, "strike"));
//        a.add(new BooleanAttribute(CHR, Constants.Deleted, "deleted"));

//        a.add(new AssertiveAttribute(DOC, "saveformat", "defformat", "RTF"));
//        a.add(new AssertiveAttribute(DOC, "landscape", "landscape"));

//        a.add(NumericAttribute.NewTwips(DOC, Constants.PaperWidth, "paperw", 12240));
//        a.add(NumericAttribute.NewTwips(DOC, Constants.PaperHeight, "paperh", 15840));
//        a.add(NumericAttribute.NewTwips(DOC, Constants.MarginLeft, "margl", 1800));
//        a.add(NumericAttribute.NewTwips(DOC, Constants.MarginRight, "margr", 1800));
//        a.add(NumericAttribute.NewTwips(DOC, Constants.MarginTop, "margt", 1440));
//        a.add(NumericAttribute.NewTwips(DOC, Constants.MarginBottom, "margb", 1440));
//        a.add(NumericAttribute.NewTwips(DOC, Constants.GutterWidth, "gutter", 0));

//        a.add(new AssertiveAttribute(PGF, Constants.WidowControl, "nowidctlpar", False));
//        a.add(new AssertiveAttribute(PGF, Constants.WidowControl, "widctlpar", True));
//        a.add(new AssertiveAttribute(DOC, Constants.WidowControl, "widowctrl", True));

        attributes = a.toArray(new RTFAttribute[0]);
    }

    public static HashMap<String, RTFAttribute> attributesByKeyword() {
        HashMap<String, RTFAttribute> d = new HashMap<String, RTFAttribute>(attributes.length);

        for (RTFAttribute attribute : attributes) {
            d.put(attribute.rtfName(), attribute);
        }

        return d;
    }

    /************************************************************************/
    /************************************************************************/

    static class BooleanAttribute extends RTFAttribute {
        boolean rtfDefault;
        boolean swingDefault;

        protected static final Boolean True = Boolean.valueOf(true);
        protected static final Boolean False = Boolean.valueOf(false);

        public BooleanAttribute(int domain, StyleAttribute s, String rtfName, boolean ds, boolean dr) {
            super(domain, s, rtfName);
            swingDefault = ds;
            rtfDefault = dr;
        }

        public BooleanAttribute(int d, StyleAttribute s, String r) {
            super(d, s, r);

            swingDefault = false;
            rtfDefault = false;
        }

        public boolean set(AttrSet target) {
            /* TODO: There's some ambiguity about whether this should
               *set* or *toggle* the attribute. */
            target.addAttribute(swingName, True);

            return true; /* true indicates we were successful */
        }

        public boolean set(AttrSet target, int parameter) {
            /* See above note in the case that parameter==1 */
            Boolean value = (parameter != 0 ? True : False);

            target.addAttribute(swingName, value);

            return true; /* true indicates we were successful */
        }

        public boolean setDefault(AttrSet target) {
            if (swingDefault != rtfDefault || (target.getAttribute(swingName) != null))
                target.addAttribute(swingName(), Boolean.valueOf(rtfDefault));
            return true;
        }
    }

    static class AssertiveAttribute extends RTFAttribute {
        Object swingValue;

        public AssertiveAttribute(int d, StyleAttribute s, String r) {
            super(d, s, r);
            swingValue = Boolean.valueOf(true);
        }

        public AssertiveAttribute(int d, StyleAttribute s, String r, Object v) {
            super(d, s, r);
            swingValue = v;
        }

        public AssertiveAttribute(int d, StyleAttribute s, String r, int v) {
            super(d, s, r);
            swingValue = Integer.valueOf(v);
        }

        public boolean set(AttrSet target) {
            if (swingValue == null)
                target.removeAttribute(swingName);
            else
                target.addAttribute(swingName, swingValue);

            return true;
        }

        public boolean set(AttrSet target, int parameter) {
            return false;
        }

        public boolean setDefault(AttrSet target) {
            target.removeAttribute(swingName);
            return true;
        }
    }

    static class NumericAttribute extends RTFAttribute {
        int rtfDefault;
        Number swingDefault;
        float scale;

        protected NumericAttribute(int d, StyleAttribute s, String r) {
            super(d, s, r);
            rtfDefault = 0;
            swingDefault = null;
            scale = 1f;
        }

        public NumericAttribute(int d, StyleAttribute s, String r, int ds, int dr) {
            this(d, s, r, Integer.valueOf(ds), dr, 1f);
        }

        public NumericAttribute(int d, StyleAttribute s, String r, Number ds, int dr, float sc) {
            super(d, s, r);
            swingDefault = ds;
            rtfDefault = dr;
            scale = sc;
        }

//        public static NumericAttribute NewTwips(int d, Object s, String r, float ds, int dr) {
//            return new NumericAttribute(d, s, r, Float.valueOf(ds), dr, 20f);
//        }
//
//        public static NumericAttribute NewTwips(int d, Object s, String r, int dr) {
//            return new NumericAttribute(d, s, r, null, dr, 20f);
//        }

        public boolean set(AttrSet target) {
            return false;
        }

        public boolean set(AttrSet target, int parameter) {
            Number swingValue;

            if (scale == 1f)
                swingValue = Integer.valueOf(parameter);
            else
                swingValue = Float.valueOf(parameter / scale);
            target.addAttribute(swingName, swingValue);
            return true;
        }

        public boolean setDefault(AttrSet target) {
            Number old = (Number)target.getAttribute(swingName);
            if (old == null)
                old = swingDefault;
            if (old != null && ((scale == 1f && old.intValue() == rtfDefault)
                || (Math.round(old.floatValue() * scale) == rtfDefault)))
                return true;
            set(target, rtfDefault);
            return true;
        }
    }
}
