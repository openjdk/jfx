/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.j2d.print;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

import javafx.geometry.Rectangle2D;

import java.awt.print.PageFormat;

import javafx.print.JobSettings;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.Printer.MarginType;
import javafx.print.Collation;
import javafx.print.PageRange;
import javafx.print.PrintColor;
import javafx.print.PageOrientation;
import javafx.print.PrintQuality;
import javafx.print.PrintResolution;
import javafx.print.Paper;
import javafx.print.PaperSource;
import javafx.print.PrintSides;

import com.sun.javafx.print.PrintHelper;
import com.sun.javafx.print.PrinterImpl;
import com.sun.javafx.print.Units;


public class J2DPrinter implements PrinterImpl {

    private PrintService service;
    private Printer fxPrinter;

    public J2DPrinter(PrintService s) {
        service = s;
    }

    public Printer getPrinter() {
        return fxPrinter;
    }

    @Override
    public void setPrinter(Printer printer) {
        fxPrinter = printer;
    }

    public PrintService getService() {
        return service;
    }

    @Override
    public String getName() {
        return service.getName();
    }

    /*
     * Since JobSettings are mutable, this always returns
     * a new instance.
     */
    @Override
    public JobSettings getDefaultJobSettings() {
        return PrintHelper.createJobSettings(fxPrinter);
    }

    //-------------- BEGIN COPIES --------------------

    private int defaultCopies = 0;
    @Override
    public int defaultCopies() {
        if (defaultCopies > 0) {
            return defaultCopies;
        }
        try {
            Copies copies =
                (Copies)service.getDefaultAttributeValue(Copies.class);
            defaultCopies = copies.getValue();
        } catch (Exception e) {
            defaultCopies = 1;
        }
        return defaultCopies;
    }

    private int maxCopies = 0;
    @Override
    public int maxCopies() {
        if (maxCopies > 0) {
            return maxCopies;
        }
        CopiesSupported copies = null;
        try {
            copies = (CopiesSupported)service.getSupportedAttributeValues
                (CopiesSupported.class, null, null);
        } catch (Exception e) {
        }
        if (copies != null) {
            int[][] members = copies.getMembers();
            if (members != null &&
                members.length > 0 &&
                members[0].length > 0)
            {
                maxCopies = members[0][1];
            }
        }
        if (maxCopies == 0) {
            maxCopies = 999;
        }
        return maxCopies;
    }

    //-------------- END COPIES --------------------

    //-------------- BEGIN PAGERANGE --------------------

    @Override
    public PageRange defaultPageRange() {
        try {
            PageRanges ranges =
                (PageRanges)service.getDefaultAttributeValue(PageRanges.class);
            if (ranges == null) {
                return null;
            }
            int s = ranges.getMembers()[0][0];
            int e = ranges.getMembers()[0][1];
            if (s == 1 && e == Integer.MAX_VALUE) {
                return null;
            } else {
                return new PageRange(s, e);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean supportsPageRanges() {
        return true;
    }

    //-------------- BEGIN COLLATION --------------------

    SheetCollate getDefaultSheetCollate() {
        SheetCollate collate = null;
        try {
            collate = (SheetCollate)
                service.getDefaultAttributeValue(SheetCollate.class);
        } catch (Exception e) {
            collate = SheetCollate.UNCOLLATED;
        }
        return collate;
    }

    private Collation defaultCollation;
    @Override
    public Collation defaultCollation() {
        if (defaultCollation != null) {
            return defaultCollation;
        }
        SheetCollate collate = getDefaultSheetCollate();
        defaultCollation = (collate == SheetCollate.COLLATED) ?
            Collation.COLLATED : Collation.UNCOLLATED;
        return defaultCollation;
    }


    private Set<Collation> collateSet;
    @Override
    public Set<Collation> supportedCollations() {
        if (collateSet == null) {
            Set<Collation> cSet = new TreeSet<>();
            SheetCollate[] sc = null;
            try {
                sc = (SheetCollate[])
                    service.getSupportedAttributeValues(SheetCollate.class,
                                                        null, null);
            } catch (Exception e) {
            }
            if (sc != null) {
                for (int i=0;i<sc.length;i++) {
                    if (sc[i] == SheetCollate.UNCOLLATED) {
                        cSet.add(Collation.UNCOLLATED);
                    }
                    if (sc[i] == SheetCollate.COLLATED) {
                        cSet.add(Collation.COLLATED);
                    }
                }
            }
            collateSet = Collections.unmodifiableSet(cSet);
        }
        return collateSet;
    }
    //-------------- END COLLATION --------------------

    //-------------- BEGIN COLOR --------------------

    Chromaticity getDefaultChromaticity() {
        Chromaticity color = null;
        try {
            color = (Chromaticity)
                service.getDefaultAttributeValue(Chromaticity.class);
        } catch (Exception e) {
            color = Chromaticity.COLOR;
        }
        return color;
    }

    private PrintColor defColor;
    @Override
    public PrintColor defaultPrintColor() {
        if (defColor != null) {
            return defColor;
        }
        Chromaticity color = getDefaultChromaticity();
        defColor = (color == Chromaticity.COLOR) ?
            PrintColor.COLOR : PrintColor.MONOCHROME;
        return defColor;
    }


    private Set<PrintColor> colorSet;
    @Override
    public Set<PrintColor> supportedPrintColor() {
        if (colorSet == null) {
            Set<PrintColor> cSet = new TreeSet<>();
            Chromaticity[] sc = null;
            try {
                sc = (Chromaticity[])
                    service.getSupportedAttributeValues(Chromaticity.class,
                                                        null, null);
            } catch (Exception e) {
            }
            if (sc != null) {
                for (int i=0;i<sc.length;i++) {
                    if (sc[i] == Chromaticity.COLOR) {
                        cSet.add(PrintColor.COLOR);
                    }
                    if (sc[i] == Chromaticity.MONOCHROME) {
                        cSet.add(PrintColor.MONOCHROME);
                    }
                }
            }
            colorSet = Collections.unmodifiableSet(cSet);
        }
        return colorSet;
    }

    //-------------- END COLOR --------------------

    //-------------- BEGIN SIDES --------------------

    private PrintSides defSides;
    @Override
    public PrintSides defaultSides() {
        if (defSides != null) {
            return defSides;
        }
        Sides sides = (Sides)service.getDefaultAttributeValue(Sides.class);
        if (sides == null || sides == Sides.ONE_SIDED) {
            defSides = PrintSides.ONE_SIDED;
        } else if (sides == Sides.DUPLEX) {
            defSides = PrintSides.DUPLEX;
        } else {
            defSides = PrintSides.TUMBLE;
        }
        return defSides;
    }

    private Set<PrintSides> sidesSet;
    @Override
    public Set<PrintSides> supportedSides() {
        if (sidesSet == null) {
            Set<PrintSides> sSet = new TreeSet<>();
            Sides[] ss = null;
            try {
                ss = (Sides[])
                    service.getSupportedAttributeValues(Sides.class,
                                                        null, null);
            } catch (Exception e) {
            }
            if (ss != null) {
                for (int i=0;i<ss.length;i++) {
                    if (ss[i] == Sides.ONE_SIDED) {
                        sSet.add(PrintSides.ONE_SIDED);
                    }
                    if (ss[i] == Sides.DUPLEX) {
                        sSet.add(PrintSides.DUPLEX);
                    }
                    if (ss[i] == Sides.TUMBLE) {
                        sSet.add(PrintSides.TUMBLE);
                    }
                }
            }
            sidesSet = Collections.unmodifiableSet(sSet);
        }
        return sidesSet;
    }
    //-------------- END SIDES --------------------

    //-------------- BEGIN ORIENTATION --------------------

    static int getOrientID(PageOrientation o) {
        if (o == PageOrientation.LANDSCAPE) {
            return PageFormat.LANDSCAPE;
        } else if (o == PageOrientation.REVERSE_LANDSCAPE) {
            return PageFormat.REVERSE_LANDSCAPE;
        } else {
            return PageFormat.PORTRAIT;
        }
    }

    static OrientationRequested mapOrientation(PageOrientation o) {
        if (o == PageOrientation.REVERSE_PORTRAIT) {
            return OrientationRequested.REVERSE_PORTRAIT;
        } else if (o == PageOrientation.LANDSCAPE) {
            return OrientationRequested.LANDSCAPE;
        } else if (o == PageOrientation.REVERSE_LANDSCAPE) {
            return OrientationRequested.REVERSE_LANDSCAPE;
        } else {
            return OrientationRequested.PORTRAIT;
        }
    }

    static PageOrientation reverseMapOrientation(OrientationRequested o) {
        if (o == OrientationRequested.REVERSE_PORTRAIT) {
            return PageOrientation.REVERSE_PORTRAIT;
        } else if (o == OrientationRequested.LANDSCAPE) {
            return PageOrientation.LANDSCAPE;
        } else if (o == OrientationRequested.REVERSE_LANDSCAPE) {
            return PageOrientation.REVERSE_LANDSCAPE;
        } else {
            return PageOrientation.PORTRAIT;
        }
    }

    private PageOrientation defOrient;
    @Override
    public PageOrientation defaultOrientation() {
        if (defOrient == null) {
            OrientationRequested orient = (OrientationRequested)
                service.getDefaultAttributeValue(OrientationRequested.class);
            defOrient = reverseMapOrientation(orient);
        }
        return defOrient;
    }

    private Set<PageOrientation> orientSet;
    @Override
    public Set<PageOrientation> supportedOrientation() {
        if (orientSet != null) {
            return orientSet;
        }

        Set<PageOrientation> oset = new TreeSet<>();
        OrientationRequested[] or = null;
        try {
            or = (OrientationRequested[])
                service.getSupportedAttributeValues
                (OrientationRequested.class, null, null);
        } catch (Exception e) {
        }
        if (or == null || or.length == 0) {
            oset.add(defaultOrientation());
        } else {
            for (int i=0;i<or.length;i++) {
                if (or[i] == OrientationRequested.PORTRAIT) {
                    oset.add(PageOrientation.PORTRAIT);
                } else if (or[i] == OrientationRequested.REVERSE_PORTRAIT) {
                    oset.add(PageOrientation.REVERSE_PORTRAIT);
                } else if (or[i] == OrientationRequested.LANDSCAPE) {
                    oset.add(PageOrientation.LANDSCAPE);
                } else {
                    oset.add(PageOrientation.REVERSE_LANDSCAPE);
                }
            }
        }
        orientSet = Collections.unmodifiableSet(oset);
        return orientSet;
    }

    //-------------- END ORIENTATION --------------------

    //-------------- BEGIN RESOLUTIONS --------------------

    PrinterResolution getDefaultPrinterResolution() {
        PrinterResolution res = (PrinterResolution)
            service.getDefaultAttributeValue(PrinterResolution.class);
        /* I think it may be possible for this to be just unsupported,
         * so do I need to allow for that somehow ?
         */
        if (res == null) {
            res = new PrinterResolution(300, 300, ResolutionSyntax.DPI);
        }
        return res;
    }

    private PrintResolution defRes;
    @Override
    public PrintResolution defaultPrintResolution() {
        if (defRes != null) {
            return defRes;
        }
        PrinterResolution res = getDefaultPrinterResolution();
        int cfr = res.getCrossFeedResolution(ResolutionSyntax.DPI);
        int fr = res.getFeedResolution(ResolutionSyntax.DPI);
        defRes = PrintHelper.createPrintResolution(cfr, fr);
        return defRes;
    }

    private static class
        PrintResolutionComparator implements Comparator<PrintResolution> {

        final static PrintResolutionComparator
            theComparator = new PrintResolutionComparator();

        /**
         * Is used to approximate a sort of resolutions from
         * lowest to highest overall resolution.
         * The feed and cross feed resolutions are combined so a
         * where M and N represent cross feed and feed  dpi values,
         * a resolution MxN will equal NxM.
         * @param other resolution to compare.
         * @return whether this resolution is less, equal or
         * greater than the other.
         */
        @Override
        public int compare(PrintResolution r1, PrintResolution r2) {
            long r1Res =
                r1.getCrossFeedResolution() * r1.getFeedResolution();
            long r2Res =
                r2.getCrossFeedResolution() * r2.getFeedResolution();
            if (r1Res == r2Res) {
                return 0;
            } else if (r1Res < r2Res) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private Set<PrintResolution> resSet;
    @Override
    public Set<PrintResolution> supportedPrintResolution() {
        if (resSet != null) {
            return resSet;
        }
        Set<PrintResolution> rSet = new
            TreeSet<>(PrintResolutionComparator.theComparator);
        PrinterResolution[] pr = null;
        try {
            pr = (PrinterResolution[])
                service.getSupportedAttributeValues
                (PrinterResolution.class, null, null);
        } catch (Exception e) {
        }
        if (pr == null || pr.length == 0) {
            rSet.add(defaultPrintResolution());
        } else {
            for (int i=0;i<pr.length;i++) {
                int cfr = pr[i].getCrossFeedResolution(ResolutionSyntax.DPI);
                int fr = pr[i].getFeedResolution(ResolutionSyntax.DPI);
                rSet.add(PrintHelper.createPrintResolution(cfr, fr));
            }
        }
        resSet = Collections.unmodifiableSet(rSet);
        return resSet;
    }
    //-------------- END RESOLUTIONS --------------------

    //-------------- BEGIN PRINTQUALITY --------------------

    javax.print.attribute.standard.PrintQuality getDefaultPrintQuality() {

     javax.print.attribute.standard.PrintQuality quality = null;
        try {
            quality = (javax.print.attribute.standard.PrintQuality)
                service.getDefaultAttributeValue
                (javax.print.attribute.standard.PrintQuality.class);
        } catch (Exception e) {
            quality = javax.print.attribute.standard.PrintQuality.NORMAL;
        }
        return quality;
    }

    private PrintQuality defQuality;
    @Override
    public PrintQuality defaultPrintQuality() {
        if (defQuality != null) {
            return defQuality;
        }
        javax.print.attribute.standard.PrintQuality
            quality = getDefaultPrintQuality();

        if (quality == javax.print.attribute.standard.PrintQuality.DRAFT) {
            defQuality = PrintQuality.DRAFT;
        } else if
            (quality == javax.print.attribute.standard.PrintQuality.HIGH) {
            defQuality = PrintQuality.HIGH;
        } else {
            defQuality = PrintQuality.NORMAL;
        }
        return defQuality;
    }


    private Set<PrintQuality> qualitySet;
    @Override
    public Set<PrintQuality> supportedPrintQuality() {
        if (qualitySet == null) {
            Set<PrintQuality> set = new TreeSet<>();
           javax.print.attribute.standard.PrintQuality[] arr = null;
            try {
                arr = (javax.print.attribute.standard.PrintQuality[])
                    service.getSupportedAttributeValues
                    (javax.print.attribute.standard.PrintQuality.class,
                     null, null);
            } catch (Exception e) {
            }
            if (arr == null || arr.length == 0) {
                set.add(PrintQuality.NORMAL);
            } else {
                for (int i=0;i<arr.length;i++) {
                    if (arr[i] ==
                        javax.print.attribute.standard.PrintQuality.NORMAL)
                        {
                        set.add(PrintQuality.NORMAL);
                    }
                    if (arr[i] ==
                        javax.print.attribute.standard.PrintQuality.DRAFT)
                        {
                        set.add(PrintQuality.DRAFT);
                    }
                    if (arr[i] ==
                        javax.print.attribute.standard.PrintQuality.HIGH)
                        {
                        set.add(PrintQuality.HIGH);
                    }
                }
            }
            qualitySet = Collections.unmodifiableSet(set);
        }
        return qualitySet;
    }
    //-------------- END PRINTQUALITY --------------------

    //-------------- START PAPERS --------------------

    private static class PaperComparator implements Comparator<Paper> {

        final static PaperComparator theComparator = new PaperComparator();

        /**
         * This sorts papers lexically based on name, not size.
         */
        @Override
        public int compare(Paper p1, Paper p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }

    private static class
        PaperSourceComparator implements Comparator<PaperSource> {

        final static PaperSourceComparator
            theComparator = new PaperSourceComparator();

        /**
         * This sorts papers lexically based on name, not size.
         */
        @Override
        public int compare(PaperSource p1, PaperSource p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }

    Paper getPaperForMedia(Media media) {
        populateMedia();
        if (media == null || !(media instanceof MediaSizeName)) {
            return defaultPaper();
        } else {
            return getPaper((MediaSizeName)media);
        }
    }

    private Paper defPaper;
    @Override
    public Paper defaultPaper() {
        if (defPaper != null) {
            return defPaper;
        }
        Media m = (Media)service.getDefaultAttributeValue(Media.class);
        if (m == null || !(m instanceof MediaSizeName)) {
            defPaper = Paper.NA_LETTER;
        } else {
            defPaper = getPaper((MediaSizeName)m);
        }
        return defPaper;
    }

    private Set<Paper> paperSet;
    @Override
    public Set<Paper> supportedPapers() {
        if (paperSet == null) {
            populateMedia();
        }
        return paperSet;
    }

    private static Map<MediaSizeName, Paper> predefinedPaperMap;
    private static Map<MediaTray, PaperSource> predefinedTrayMap;

    private static void initPredefinedMediaMaps() {
        if (predefinedPaperMap == null) {
            // North American papers
            predefinedPaperMap = Map.ofEntries(
                Map.entry(MediaSizeName.NA_LETTER,             Paper.NA_LETTER),
                Map.entry(MediaSizeName.TABLOID,               Paper.TABLOID),
                Map.entry(MediaSizeName.NA_LEGAL,              Paper.LEGAL),
                Map.entry(MediaSizeName.EXECUTIVE,             Paper.EXECUTIVE),
                Map.entry(MediaSizeName.NA_8X10,               Paper.NA_8X10),
                // Envelopes
                Map.entry(MediaSizeName.MONARCH_ENVELOPE,      Paper.MONARCH_ENVELOPE),
                Map.entry(MediaSizeName.NA_NUMBER_10_ENVELOPE, Paper.NA_NUMBER_10_ENVELOPE),
                // ISO sizes.
                Map.entry(MediaSizeName.ISO_A0,                Paper.A0),
                Map.entry(MediaSizeName.ISO_A1,                Paper.A1),
                Map.entry(MediaSizeName.ISO_A2,                Paper.A2),
                Map.entry(MediaSizeName.ISO_A3,                Paper.A3),
                Map.entry(MediaSizeName.ISO_A4,                Paper.A4),
                Map.entry(MediaSizeName.ISO_A5,                Paper.A5),
                Map.entry(MediaSizeName.ISO_A6,                Paper.A6),
                Map.entry(MediaSizeName.C,                     Paper.C), // Eng. size
                // I've seen this as "Envelope DL" on HP inkjet drivers for OS X and WIndows.
                Map.entry(MediaSizeName.ISO_DESIGNATED_LONG,   Paper.DESIGNATED_LONG),
                // Common Japanese sizes.
                Map.entry(MediaSizeName.JIS_B4,                Paper.JIS_B4),
                Map.entry(MediaSizeName.JIS_B5,                Paper.JIS_B5),
                Map.entry(MediaSizeName.JIS_B6,                Paper.JIS_B6),
                Map.entry(MediaSizeName.JAPANESE_POSTCARD,     Paper.JAPANESE_POSTCARD));
        }

        if (predefinedTrayMap == null) {
            predefinedTrayMap = Map.of(
                MediaTray.MAIN,           PaperSource.MAIN,
                MediaTray.MANUAL,         PaperSource.MANUAL,
                MediaTray.BOTTOM,         PaperSource.BOTTOM,
                MediaTray.MIDDLE,         PaperSource.MIDDLE,
                MediaTray.TOP,            PaperSource.TOP,
                MediaTray.SIDE,           PaperSource.SIDE,
                MediaTray.ENVELOPE,       PaperSource.ENVELOPE,
                MediaTray.LARGE_CAPACITY, PaperSource.LARGE_CAPACITY);
        }
    }

    private void populateMedia() {
        initPredefinedMediaMaps();

        if (paperSet != null) {
            return; // already inited
        }
        Media[] media =
            (Media[])service.getSupportedAttributeValues(Media.class,
                                                         null, null);
        Set<Paper> pSet = new TreeSet<>(PaperComparator.theComparator);
        Set<PaperSource> tSet =
            new TreeSet<>(PaperSourceComparator.theComparator);
        /* We will get back a list of Media and want to look for
         * MediaSizeName and MediaTray instances and map to FX classes.
         * We will hard code here recognising the set we've chosen to
         * expose in FX API.
         * For the rest we'll need to create custom instances.
         */

        if (media != null) {
            for (int i=0; i<media.length; i++) {
                Media m = media[i];
                if (m instanceof MediaSizeName) {
                    Paper p = addPaper(((MediaSizeName)m));
                    if (p != null) {
                        pSet.add(p);
                     }
                } else if (m instanceof MediaTray) {
                    tSet.add(addPaperSource((MediaTray)m));
                }
            }
        }
        paperSet = Collections.unmodifiableSet(pSet);
        paperSourceSet = Collections.unmodifiableSet(tSet);
    }

    private PaperSource defPaperSource;
    @Override
    public PaperSource defaultPaperSource() {
        if (defPaperSource != null) {
            return defPaperSource;
        }
        defPaperSource = PaperSource.AUTOMATIC;
        return defPaperSource;
    }

    private Set<PaperSource> paperSourceSet;
    @Override
    public Set<PaperSource> supportedPaperSources() {
        if (paperSourceSet == null) {
            populateMedia();
        }
        return paperSourceSet;
    }

    /*
     * We have a static map from pre-defined javax.print trays to
     * pre-defined javafx.print trays. For all other trays we create
     * a printer specific instance.
     */
    private Map<PaperSource, MediaTray> sourceToTrayMap;
    private Map<MediaTray, PaperSource> trayToSourceMap;
    synchronized final PaperSource getPaperSource(MediaTray tray) {
        if (paperSourceSet == null) {
            populateMedia();
        }

        PaperSource source = trayToSourceMap.get(tray);
        if (source != null) {
            return source;
        } else {
            return addPaperSource(tray);
        }
    }

    MediaTray getTrayForPaperSource(PaperSource source) {
        if (paperSourceSet == null) {
            populateMedia();
        }
        return sourceToTrayMap.get(source);
    }

    private synchronized final PaperSource addPaperSource(MediaTray tray) {

        PaperSource source = predefinedTrayMap.get(tray);

        if (source == null) {
            source = PrintHelper.createPaperSource(tray.toString());
        }

        if (trayToSourceMap == null) {
            trayToSourceMap = new HashMap<>();
        }
        trayToSourceMap.put(tray, source);

        if (sourceToTrayMap == null) {
            sourceToTrayMap = new HashMap<>();
        }
        sourceToTrayMap.put(source, tray);
        return source;
    }

    /*
     * We have a static map from pre-defined javax.print MediaSizeName
     * to pre-defined javafx.print Papers. For all other reported media we
     * create a printer-specific instance and store it in a per-printer map.
     */
    private final Map<MediaSizeName, Paper> mediaToPaperMap
         = new HashMap<>();
    private final Map<Paper, MediaSizeName> paperToMediaMap
         = new HashMap<>();

    private Paper createPaper(MediaSizeName media) {
        Paper paper = null;
        MediaSize sz = MediaSize.getMediaSizeForName(media);
        if (sz != null) {
            double pw = sz.getX(1) / 1000.0;
            double ph = sz.getY(1) / 1000.0;
            paper = PrintHelper.createPaper(media.toString(),
                                            pw, ph, Units.MM);
        }
        return paper;
   }

    private synchronized final Paper addPaper(MediaSizeName media) {
        Paper paper = predefinedPaperMap.get(media);
        if (paper == null) {
           paper = createPaper(media);
        }
        /* If that failed create a Paper from the default MediaSizeName */
        if (paper == null) {
            Media m = (Media)service.getDefaultAttributeValue(Media.class);
            if (m instanceof MediaSizeName) {
                paper = createPaper((MediaSizeName)m);
            }
        }
        if (paper != null) {
            paperToMediaMap.put(paper, media);
            mediaToPaperMap.put(media, paper);
        }
        return paper;
    }

    private Paper getPaper(MediaSizeName m) {
        populateMedia();
        Paper paper = mediaToPaperMap.get(m);
        if (paper == null) {
            paper = Paper.NA_LETTER;
        }
        return paper;
    }

    private MediaSizeName getMediaSizeName(Paper paper) {
        populateMedia();
        MediaSizeName m = paperToMediaMap.get(paper);
        if (m == null) {
            m = MediaSize.findMedia((float)paper.getWidth(),
                                    (float)paper.getHeight(),
                                    (int)(MediaSize.INCH/72.0));
        }
        return m;
    }

    /**
     * For any given paper, this retrieves the hardware margins,
     * or a reasonable and safe guess if they aren't available.
     */
    @Override
    public Rectangle2D printableArea(Paper paper) {
        Rectangle2D area = null;

        MediaSizeName msn = getMediaSizeName(paper);
        if (msn != null) {
            PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            pras.add(msn);
            MediaPrintableArea[] mpa = (MediaPrintableArea[])service.
                getSupportedAttributeValues(MediaPrintableArea.class,
                                            null, pras);
            if (mpa != null && mpa.length > 0 && mpa[0] != null) {
                int MPA_INCH = MediaPrintableArea.INCH;
                area = new Rectangle2D(mpa[0].getX(MPA_INCH),
                                       mpa[0].getY(MPA_INCH),
                                       mpa[0].getWidth(MPA_INCH),
                                       mpa[0].getHeight(MPA_INCH));
            }
        }
        // If we could not get the area for whatever reason,
        // then go with 0.75" margins unless they are too large
        // ie its a really small paper.
        if (area == null) {
            double pw = (paper.getWidth() / 72.0);
            double ph = (paper.getHeight() / 72.0);
            double iw, ih;
            if (pw < 3.0) {
                iw = 0.75 * pw;
            } else {
                iw = pw - 1.5;
            }
            if (ph < 3.0) {
                ih = 0.75 * ph;
            } else {
                ih = ph - 1.5;
            }
            double lm = (pw - iw) / 2.0;
            double tm = (ph - ih) / 2.0;
            area = new Rectangle2D(lm, tm, iw, ih);
        }
        return area;
    }

    private PageLayout defaultLayout;
    PageLayout defaultPageLayout() {
        if (defaultLayout == null) {
            Paper paper = defaultPaper();
            PageOrientation orient = defaultOrientation();
            defaultLayout =
                fxPrinter.createPageLayout(paper, orient, MarginType.DEFAULT);
        }
        return defaultLayout;
    }
    //-------------- END PAPERS --------------------


}
