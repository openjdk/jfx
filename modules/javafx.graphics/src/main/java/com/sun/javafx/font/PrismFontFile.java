/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;


import java.lang.ref.WeakReference;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.font.FontFileReader.Buffer;
import static com.sun.javafx.font.PrismMetrics.*;

public abstract class PrismFontFile implements FontResource, FontConstants {

    // TrueType fonts can have multiple names, most notably split up by
    // platform and locale. Whilst fonts that have different names for
    // different platforms are arguable buggy, those with localised names
    // are not. This can cause problems. Suppose that a font has English,
    // French and German names, and the platform enumerates the name that
    // is most appropriate for the user locale. Then suppose a French
    // developer uses the French name, but for his German user this font
    // is not located by the platform because it reports the German name
    // for that font. At runtime we no longer have any connection to the
    // locale of the developer so we can't look for the name for that
    // locale, even if the platform have us a performant option for that.
    //
    // The English name which some might think is supposed
    // to be the interoperable name is not treated at all specially in
    // the font format and doesn't even come up for either the user or
    // the developer, and in fact doesn't even have to be present.
    // Having said that we'll probably have the best luck for most users
    // and fonts by assuming the English name if the locale name doesn't
    // work. But either way, without platform API support for this
    // then its really expensive as all font files need to be opened.
    //
    String familyName;           /* Family font name (English) */
    protected String fullName;   /* Full font name (English)   */
    String psName;               /* PostScript font name       */
    String localeFamilyName;
    String localeFullName;
    String styleName;
    String localeStyleName;
    String filename;
    int filesize;
    FontFileReader filereader;
    int numGlyphs = -1;
    short indexToLocFormat;
    int fontIndex; // into a TTC.
    boolean isCFF;
    boolean isEmbedded = false;
    boolean isCopy = false;
    boolean isTracked = false;
    boolean isDecoded = false;
    boolean isRegistered = true;

    /* The glyph image data is stored only in a texture, and we
     * manage how much of that is kept around. We clearly want
     * to keep a reference to the strike that created that data.
     */
    Map<FontStrikeDesc, WeakReference<PrismFontStrike>> strikeMap = new ConcurrentHashMap<>();

    protected PrismFontFile(String name, String filename, int fIndex,
                          boolean register, boolean embedded,
                          boolean copy, boolean tracked) throws Exception {
        this.filename = filename;
        this.isRegistered = register;
        this.isEmbedded = embedded;
        this.isCopy = copy;
        this.isTracked = tracked;
        init(name, fIndex);
    }

    WeakReference<PrismFontFile> createFileDisposer(PrismFontFactory factory,
                                                    FileRefCounter rc) {
        FileDisposer disposer = new FileDisposer(filename, isTracked, rc);
        WeakReference<PrismFontFile> ref = Disposer.addRecord(this, disposer);
        disposer.setFactory(factory, ref);
        return ref;
    }

    void setIsDecoded(boolean decoded) {
        isDecoded = decoded;
    }

    /* This is called only for fonts where a temp file was created
     */
    @SuppressWarnings("removal")
    protected synchronized void disposeOnShutdown() {
        if (isCopy || isDecoded) {
            AccessController.doPrivileged(
                    (PrivilegedAction<Void>) () -> {
                        try {
                            /* Although there is likely no harm in calling
                             * delete on a file > once, we want to refrain
                             * from deleting it until the shutdown hook
                             * code in subclasses has had an opportunity
                             * to clean up native accesses on the resource.
                             */
                            if (decFileRefCount() > 0) {
                                return null;
                            }
                            boolean delOK = (new File(filename)).delete();
                            if (!delOK && PrismFontFactory.debugFonts) {
                                 System.err.println("Temp file not deleted : "
                                                    + filename);
                            }
                            /* Embedded fonts (copy) can also be decoded.
                             * Set both flags to false to avoid double deletes.
                             */
                            isCopy = isDecoded = false;
                        } catch (Exception e) {
                        }
                        return null;
                    }
            );
            if (PrismFontFactory.debugFonts) {
                System.err.println("Temp file deleted: " + filename);
            }
        }
    }

    @Override
    public int getDefaultAAMode() {
        return AA_GREYSCALE;
    }



    /* A TTC file resource is shared, so reference count and delete
     * only when no longer using the file from any PrismFontFile instance
     */
   static class FileRefCounter {
       private int refCnt = 1; // start with 1.

       synchronized int getRefCount() {
           return refCnt;
       }

       synchronized int increment() {
           return ++refCnt;
       }

       synchronized int decrement() {
           return (refCnt == 0) ? 0 : --refCnt;
       }
    }

    private FileRefCounter refCounter = null;

    FileRefCounter getFileRefCounter() {
        return refCounter;
    }

    FileRefCounter createFileRefCounter() {
        refCounter = new FileRefCounter();
        return refCounter;
    }

    void setAndIncFileRefCounter(FileRefCounter rc) {
          this.refCounter = rc;
          this.refCounter.increment();
    }

    int decFileRefCount() {
        if (refCounter == null) {
            return 0;
         } else {
            return refCounter.decrement();
         }
    }

    static class FileDisposer implements DisposerRecord {
        String fileName;
        boolean isTracked;
        FileRefCounter refCounter;
        PrismFontFactory factory;
        WeakReference<PrismFontFile> refKey;

        public FileDisposer(String fileName, boolean isTracked,
                            FileRefCounter rc) {
            this.fileName = fileName;
            this.isTracked = isTracked;
            this.refCounter = rc;
        }

        public void setFactory(PrismFontFactory factory,
                               WeakReference<PrismFontFile> refKey) {
            this.factory = factory;
            this.refKey = refKey;
        }

        @Override
        @SuppressWarnings("removal")
        public synchronized void dispose() {
            if (fileName != null) {
                AccessController.doPrivileged(
                        (PrivilegedAction<Void>) () -> {
                            try {
                                if (refCounter != null &&
                                    refCounter.decrement() > 0)
                                {
                                    return null;
                                }
                                File file = new File(fileName);
                                int size = (int)file.length();
                                file.delete();
                                // decrement tracker only after
                                // successful deletion.
                                if (isTracked) {
                                    FontFileWriter.FontTracker.
                                        getTracker().subBytes(size);
                                }
                                if (factory != null && refKey != null) {
                                    Object o = refKey.get();
                                    if (o == null) {
                                        factory.removeTmpFont(refKey);
                                        factory = null;
                                        refKey = null;
                                    }
                                }
                                if (PrismFontFactory.debugFonts) {
                                    System.err.println("FileDisposer=" + fileName);
                                }
                            } catch (Exception e) {
                                if (PrismFontFactory.debugFonts) {
                                    e.printStackTrace();
                                }
                            }
                            return null;
                        }
                );
                fileName = null;
            }
        }
    }

    @Override
    public String getFileName() {
        return filename;
    }

    protected int getFileSize() {
        return filesize;
    }

    protected int getFontIndex() {
        return fontIndex;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getPSName() {
        if (psName == null) {
            psName = fullName;
        }
        return psName;
    }

    @Override
    public String getFamilyName() {
        return familyName;
    }

    @Override
    public String getStyleName() {
        return styleName;
    }

    @Override
    public String getLocaleFullName() {
        return localeFullName;
    }

    @Override
    public String getLocaleFamilyName() {
        return localeFamilyName;
    }

    @Override
    public String getLocaleStyleName() {
        return localeStyleName;
    }

    /*
     * Returns the features the font supports.
     */
    @Override
    public int getFeatures() {
        //TODO check font file for features
        return -1;
    }

    @Override
    public Map getStrikeMap() {
        return strikeMap;
    }

    protected abstract PrismFontStrike createStrike(float size,
                                                    BaseTransform transform,
                                                    int aaMode,
                                                    FontStrikeDesc desc);

    @Override
    public FontStrike getStrike(float size, BaseTransform transform,
                                int aaMode) {
        FontStrikeDesc desc = new FontStrikeDesc(size, transform, aaMode);
        WeakReference<PrismFontStrike> ref = strikeMap.get(desc);
        PrismFontStrike strike = null;
        if (ref != null) {
            strike = ref.get();
        }
        if (strike == null) {
            strike = createStrike(size, transform, aaMode, desc);
            DisposerRecord disposer = strike.getDisposer();
            if (disposer != null) {
                ref = Disposer.addRecord(strike, disposer);
            } else {
                ref = new WeakReference<>(strike);
            }
            strikeMap.put(desc, ref);
        }
        return strike;
    }

    HashMap<Integer, int[]> bbCache = null;
    static final int[] EMPTY_BOUNDS = new int[4];

    protected abstract int[] createGlyphBoundingBox(int gc);

    @Override
    public float[] getGlyphBoundingBox(int gc, float size, float[] retArr) {
        if (retArr == null || retArr.length < 4) {
            retArr = new float[4];
        }
        if (gc >= getNumGlyphs()) {
            retArr[0] = retArr[1] = retArr[2] = retArr[3] = 0;
            return retArr;
        }
        if (bbCache == null) {
            bbCache = new HashMap<>();
        }
        int[] bb = bbCache.get(gc);
        if (bb == null) {
            bb = createGlyphBoundingBox(gc);
            if (bb == null) bb = EMPTY_BOUNDS;
            bbCache.put(gc, bb);
        }
        float scale = size / getUnitsPerEm();
        retArr[0] = bb[0] * scale;
        retArr[1] = bb[1] * scale;
        retArr[2] = bb[2] * scale;
        retArr[3] = bb[3] * scale;
        return retArr;
    }

    int getNumGlyphs() {
        if (numGlyphs == -1) {
            Buffer buffer = readTable(maxpTag);
            numGlyphs = buffer.getChar(4); // offset 4 bytes in MAXP table.
        }
        return numGlyphs;
    }

    protected boolean isCFF() {
        return isCFF;
    }

    private Object peer;
    @Override
    public Object getPeer() {
        return peer;
    }

    @Override
    public void setPeer(Object peer) {
        this.peer = peer;
    }

    int getTableLength(int tag) {
        int len = 0;
        DirectoryEntry tagDE = getDirectoryEntry(tag);
        if (tagDE != null) {
            len = tagDE.length;
        }
        return len;
    }

    synchronized Buffer readTable(int tag) {
        Buffer buffer = null;
        boolean openedFile = false;
        try {
            openedFile = filereader.openFile();
            DirectoryEntry tagDE = getDirectoryEntry(tag);
            if (tagDE != null) {
                buffer = filereader.readBlock(tagDE.offset, tagDE.length);
            }
        } catch (Exception e) {
            if (PrismFontFactory.debugFonts) {
                e.printStackTrace();
            }
        } finally {
            if (openedFile) {
                try {
                    filereader.closeFile();
                } catch (Exception e2) {
                }
            }
        }
        return buffer;
    }

    int directoryCount = 1;

    /**
     * @return number of logical fonts. Is "1" for all but TTC files
     */
    public int getFontCount() {
        return directoryCount;
    }

    int numTables;
    DirectoryEntry[] tableDirectory;
    static class DirectoryEntry {
        int tag;
        int offset;
        int length;
    }

    DirectoryEntry getDirectoryEntry(int tag) {
        for (int i=0;i<numTables;i++) {
            if (tableDirectory[i].tag == tag) {
                return tableDirectory[i];
            }
        }
        return null;
    }

    /* Called from the constructor. Does the basic work of finding
     * the right font in a TTC, the font names and enough info
     * (the table offset directory) to be able to locate tables later.
     * Throws an exception if it doesn't like what it finds.
     */
    private void init(String name, int fIndex) throws Exception {
        filereader = new FontFileReader(filename);
        WoffDecoder decoder = null;
        try {
            if (!filereader.openFile()) {
                throw new FileNotFoundException("Unable to create FontResource"
                        + " for file " + filename);
            }
            Buffer buffer = filereader.readBlock(0, TTCHEADERSIZE);
            int sfntTag = buffer.getInt();

            /* Handle wOFF files */
            if (sfntTag == woffTag) {
                decoder = new WoffDecoder();
                File file = decoder.openFile();
                decoder.decode(filereader);
                decoder.closeFile();

                /* Create a new reader with the decoded file */
                filereader.closeFile();
                filereader = new FontFileReader(file.getPath());
                if (!filereader.openFile()) {
                    throw new FileNotFoundException("Unable to create "
                            + "FontResource for file " + filename);
                }
                buffer = filereader.readBlock(0, TTCHEADERSIZE);
                sfntTag = buffer.getInt();
            }

            filesize = (int)filereader.getLength();
            int headerOffset = 0;
            if (sfntTag == ttcfTag) {
                buffer.getInt(); // skip TTC version ID
                directoryCount = buffer.getInt();
                if (fIndex >= directoryCount) {
                    throw new Exception("Bad collection index");
                }
                fontIndex = fIndex;
                buffer = filereader.readBlock(TTCHEADERSIZE+4*fIndex, 4);
                headerOffset = buffer.getInt();
                buffer = filereader.readBlock(headerOffset, 4);
                sfntTag = buffer.getInt();
            }

            switch (sfntTag) {
            case v1ttTag:
            case trueTag:
                break;

            case ottoTag:
                isCFF = true;
                break;

            default:
                throw new Exception("Unsupported sfnt " + filename);
            }

            /* Now have the offset of this TT font (possibly within a TTC)
             * After the TT version/scaler type field, is the short
             * representing the number of tables in the table directory.
             * The table directory begins at 12 bytes after the header.
             * Each table entry is 16 bytes long (4 32-bit ints)
             */
            buffer = filereader.readBlock(headerOffset+4, 2);
            numTables = buffer.getShort();
            int directoryOffset = headerOffset+DIRECTORYHEADERSIZE;
            Buffer ibuffer = filereader.
                    readBlock(directoryOffset, numTables*DIRECTORYENTRYSIZE);
            DirectoryEntry table;
            tableDirectory = new DirectoryEntry[numTables];
            for (int i=0; i<numTables;i++) {
                tableDirectory[i] = table = new DirectoryEntry();
                table.tag   =  ibuffer.getInt();
                /* checksum */ ibuffer.skip(4);
                table.offset = ibuffer.getInt();
                table.length = ibuffer.getInt();
                if ((table.offset < 0) || (table.length < 0) ||
                    (table.offset + table.length < table.length) ||
                    (table.offset + table.length > filesize))
                {
                    throw new Exception("bad table, tag="+table.tag);
                }
            }

            DirectoryEntry headDE = getDirectoryEntry(headTag);
            if (headDE == null) {
                throw new Exception("No header table - font is invalid.");
            }
            Buffer headTable = filereader.readBlock(headDE.offset,
                                                    headDE.length);
            // Important font attribute must be set in order to prevent div by zero
            upem = headTable.getShort(18) & 0xffff;
            if (!(16 <= upem && upem <= 16384)) {
                upem = 2048;
            }

            indexToLocFormat = headTable.getShort(50);
            // 0 for short offsets, 1 for long
            if (indexToLocFormat < 0 || indexToLocFormat > 1) {
                throw new Exception("Bad indexToLocFormat");
            }

            // In a conventional optimised layout, the
            // hhea table immediately follows the 'head' table.
            Buffer hhea = readTable(hheaTag);
            if (hhea == null) {
                numHMetrics = -1;
            } else {
                // the font table has the sign of ascent and descent
                // reversed from our coordinate system.
                ascent = -(float)hhea.getShort(4);
                descent = -(float)hhea.getShort(6);
                linegap = hhea.getShort(8);
                // advanceWidthMax is max horizontal advance of all glyphs in
                // font. For some fonts advanceWidthMax is much larger then "M"
                // advanceWidthMax = (float)hhea.getChar(10);
                numHMetrics = hhea.getChar(34) & 0xffff;
                /* the hmtx table may have a trailing LSB array which we don't
                 * use. But it means we must not assume these two values match.
                 * We are only concerned here with not reading more data than
                 * there is in the table.
                 */
                int hmtxEntries = getTableLength(hmtxTag) >> 2;
                if (numHMetrics > hmtxEntries) {
                    numHMetrics = hmtxEntries;
                }
            }

            // maxp table is before the OS/2 table. Read it now
            // while file is open - will be very cheap as its just
            // 32 bytes and we already have it in a byte[].
            getNumGlyphs();

            setStyle();

            // sanity check the cmap table
            checkCMAP();

            /* Get names last, as the name table is far from the file header.
             * Although its also likely too big to fit in the read cache
             * in which case that would remain valid, but also will help
             * any file read implementation which doesn't have random access.
             */
            initNames();

            if (familyName == null || fullName == null) {
                String fontName = name != null ? name : "";
                if (fullName == null) {
                    fullName = familyName != null ? familyName : fontName;
                }
                if (familyName == null) {
                    familyName = fullName != null ? fullName : fontName;
                }
                throw new Exception("Font name not found in " + filename);
            }

            /* update the font resource only if the file was decoded
             * and initialized successfully.
             */
            if (decoder != null) {
                isDecoded = true;
                filename = filereader.getFilename();
                PrismFontFactory.getFontFactory().addDecodedFont(this);
            }
        } catch (Exception e) {
            if (decoder != null) {
                decoder.deleteFile();
            }
            throw e;
        } finally {
            filereader.closeFile();
        }
    }

    /* TrueTypeFont can use the fsSelection fields of OS/2 table
     * or macStyleBits of the 'head' table to determine the style.
     */
    private static final int fsSelectionItalicBit  = 0x00001;
    private static final int fsSelectionBoldBit    = 0x00020;

    private static final int MACSTYLE_BOLD_BIT   = 0x1;
    private static final int MACSTYLE_ITALIC_BIT = 0x2;

    // Comment out some of this until we have both a need and a way to use it.
    // private int embeddingInfo;
    //private int fontWeight;
    private boolean isBold;
    private boolean isItalic;
    private float upem;
    private float ascent, descent, linegap; // in design units
    private int numHMetrics;

    private void setStyle() {
        // A number of fonts on Mac OS X do not have an OS/2
        // table. For those need to get info from a different source.
        DirectoryEntry os2_DE = getDirectoryEntry(os_2Tag);
        if (os2_DE != null) {
            // os2 Table ver 4      DataType    Offset
            //version               USHORT      0
            //xAvgCharWidth         SHORT       2
            //usWeightClass         USHORT      4
            //usWidthClass          USHORT      6
            //fsType                USHORT      8
            //ySubscriptXSize       SHORT      10
            //ySubscriptYSize       SHORT      12
            //ySubscriptXOffset     SHORT      14
            //ySubscriptYOffset     SHORT      16
            //ySuperscriptXSize     SHORT      18
            //ySuperscriptYSize     SHORT      20
            //ySuperscriptXOffset   SHORT      22
            //ySuperscriptYOffset   SHORT      24
            //yStrikeoutSize        SHORT      26
            //yStrikeoutPosition    SHORT      28
            //sFamilyClass          SHORT      30
            //panose[10]            BYTE       32
            //ulUnicodeRange1       ULONG      42
            //ulUnicodeRange2       ULONG      46
            //ulUnicodeRange3       ULONG      50
            //ulUnicodeRange4       ULONG      54
            //achVendID[4]          CHAR       58
            //fsSelection           USHORT     62
            //usFirstCharIndex      USHORT     64
            //usLastCharIndex       USHORT     66
            //sTypoAscender         SHORT      68
            //sTypoDescender        SHORT      70
            //sTypoLineGap          SHORT      72
            //usWinAscent           USHORT     74
            //usWinDescent          USHORT     76
            //ulCodePageRange1      ULONG      78
            //ulCodePageRange2      ULONG      82
            //sxHeight              SHORT      86
            //sCapHeight            SHORT      88
            //usDefaultChar         USHORT     90
            //usBreakChar           USHORT     92
            //usMaxContext          USHORT     94

            Buffer os_2Table = filereader.readBlock(os2_DE.offset,
                                                    os2_DE.length);
            int fsSelection = os_2Table.getChar(62) & 0xffff;
            isItalic = (fsSelection & fsSelectionItalicBit) != 0;
            isBold   = (fsSelection & fsSelectionBoldBit) != 0;
        } else {
            DirectoryEntry headDE = getDirectoryEntry(headTag);
            Buffer headTable = filereader.readBlock(headDE.offset,
                                                    headDE.length);
            short macStyleBits = headTable.getShort(44);
            isItalic = (macStyleBits & MACSTYLE_ITALIC_BIT) != 0;
            isBold = (macStyleBits & MACSTYLE_BOLD_BIT) != 0;
        }
    }

    @Override
    public boolean isBold() {
        return isBold;
    }

    @Override
    public boolean isItalic() {
        return isItalic;
    }

    public boolean isDecoded() {
        return isDecoded;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public boolean isEmbeddedFont() {
        return isEmbedded;
    }

    /**
     * per the OT spec. this is an unsigned short.
     */
    public int getUnitsPerEm() {
        return (int)upem;
    }

    public short getIndexToLocFormat() {
        return indexToLocFormat;
    }

    /**
     * per the OT spec. this is an unsigned short.
     */
    public int getNumHMetrics() {
        return numHMetrics;
    }

    /* -- ID's used in the 'name' table */
    public static final int UNICODE_PLATFORM_ID = 0;

    public static final int MAC_PLATFORM_ID = 1;
    public static final int MACROMAN_SPECIFIC_ID = 0;
    public static final int MACROMAN_ENGLISH_LANG = 0;

    public static final int MS_PLATFORM_ID = 3;
    /* MS locale id for US English is the "default" */
    public static final short MS_ENGLISH_LOCALE_ID = 0x0409; // 1033 decimal
    public static final int FAMILY_NAME_ID = 1;
    public static final int STYLE_NAME_ID = 2;
    public static final int FULL_NAME_ID = 4;
    public static final int PS_NAME_ID = 6;

    void initNames() throws Exception {
        byte[] name = new byte[256];

        DirectoryEntry nameDE = getDirectoryEntry(nameTag);
        Buffer buffer = filereader.readBlock(nameDE.offset, nameDE.length);

        buffer.skip(2); // format - not needed.
        short numRecords = buffer.getShort();
        /* The name table uses unsigned shorts. Many of these
         * are known small values that fit in a short.
         * The values that are sizes or offsets into the table could be
         * greater than 32767, so read and store those as ints
         */
        int stringPtr = buffer.getShort() & 0xffff;

        /* Microsoft Windows font names are preferred but numerous Mac
         * fonts do not have these, so we must also accept these in the
         * absence of the preferred Windows names.
         */
        for (int i=0; i<numRecords; i++) {
            short platformID = buffer.getShort();
            if ((platformID != UNICODE_PLATFORM_ID) &&
                (platformID != MS_PLATFORM_ID) &&
                (platformID != MAC_PLATFORM_ID)) {
                buffer.skip(10);
                continue; // skip over this record.
            }
            short encodingID = buffer.getShort();
            // only want UTF-16 (inc. symbol) encodingIDs for Windows,
            // or MacRoman on Mac.
            if ((platformID == MS_PLATFORM_ID && encodingID > 1) ||
                (platformID == MAC_PLATFORM_ID &&
                 encodingID != MACROMAN_SPECIFIC_ID)) {
                buffer.skip(8);
                continue;
            }
            short langID     = buffer.getShort();
            if (platformID == MAC_PLATFORM_ID &&
                langID != MACROMAN_ENGLISH_LANG) {
                buffer.skip(6);
                continue;
            }
            short nameID   = buffer.getShort();
            int nameLen    = buffer.getShort() & 0xffff;
            int namePtr    = (buffer.getShort() & 0xffff) + stringPtr;
            String tmpName = null;
            String enc;
            switch (nameID) {

            case FAMILY_NAME_ID:

                if (familyName == null || langID == MS_ENGLISH_LOCALE_ID ||
                    langID == nameLocaleID)
                    {
                        buffer.get(namePtr, name, 0, nameLen);
                        if (platformID == MAC_PLATFORM_ID) {
                            enc = "US-ASCII";
                        } else {
                            enc = "UTF-16BE";
                        }
                        tmpName = new String(name, 0, nameLen, enc);

                        if (familyName == null ||
                            langID == MS_ENGLISH_LOCALE_ID){
                            familyName = tmpName;
                        }
                        if (langID == nameLocaleID) {
                            localeFamilyName = tmpName;
                        }
                    }
                    break;

                case FULL_NAME_ID:

                    if (fullName == null ||
                        langID == MS_ENGLISH_LOCALE_ID ||
                        langID == nameLocaleID)
                    {
                        buffer.get(namePtr, name, 0, nameLen);
                        if (platformID == MAC_PLATFORM_ID) {
                            enc = "US-ASCII";
                        } else {
                            enc = "UTF-16BE";
                        }
                        tmpName = new String(name, 0, nameLen, enc);

                        if (fullName == null ||
                            langID == MS_ENGLISH_LOCALE_ID) {
                            fullName = tmpName;
                        }
                        if (langID == nameLocaleID) {
                            localeFullName = tmpName;
                        }
                    }
                    break;

                case PS_NAME_ID:

                    if (psName == null) {
                        buffer.get(namePtr, name, 0, nameLen);
                        if (platformID == MAC_PLATFORM_ID) {
                            enc = "US-ASCII";
                        } else {
                            enc = "UTF-16BE";
                        }
                        psName = new String(name, 0, nameLen, enc);
                    }
                    break;

                case STYLE_NAME_ID:

                    if (styleName == null ||
                        langID == MS_ENGLISH_LOCALE_ID ||
                        langID == nameLocaleID)
                    {
                        buffer.get(namePtr, name, 0, nameLen);
                        if (platformID == MAC_PLATFORM_ID) {
                            enc = "US-ASCII";
                        } else {
                            enc = "UTF-16BE";
                        }
                        tmpName = new String(name, 0, nameLen, enc);

                        if (styleName == null ||
                            langID == MS_ENGLISH_LOCALE_ID) {
                            styleName = tmpName;
                        }
                        if (langID == nameLocaleID) {
                            localeStyleName = tmpName;
                        }
                    }
                    break;

            default:
                break;
            }

            if (localeFamilyName == null) {
                localeFamilyName = familyName;
            }
            if (localeFullName == null) {
                localeFullName = fullName;
            }
            if (localeStyleName == null) {
                localeStyleName = styleName;
            }
        }
    }

    private void checkCMAP() throws Exception {
        DirectoryEntry cmapDE = getDirectoryEntry(FontConstants.cmapTag);
        if (cmapDE != null) {
            if (cmapDE.length < 4) {
                throw new Exception("Invalid cmap table length");
            }
            Buffer cmapTableHeader = filereader.readBlock(cmapDE.offset, 4);
            short version = cmapTableHeader.getShort();
            short numberSubTables = cmapTableHeader.getShort();
            int indexLength = numberSubTables * 8;
            if (numberSubTables <= 0 || cmapDE.length < indexLength + 4) {
                throw new Exception("Invalid cmap subtables count");
            }
            Buffer cmapTableIndex = filereader.readBlock(cmapDE.offset + 4, indexLength);
            for (int i = 0; i < numberSubTables; i++) {
                short platformID = cmapTableIndex.getShort();
                short encodingID = cmapTableIndex.getShort();
                int offset = cmapTableIndex.getInt();
                if (offset < 0 || offset >= cmapDE.length) {
                    throw new Exception("Invalid cmap subtable offset");
                }
            }
        }
    }

    /*** BEGIN LOCALE_ID MAPPING ****/

    private static Map<String, Short> lcidMap;

    // Return a Microsoft LCID from the given Locale.
    // Used when getting localized font data.

    private static void addLCIDMapEntry(Map<String, Short> map,
                                        String key, short value) {
        map.put(key, Short.valueOf(value));
    }

    private static synchronized void createLCIDMap() {
        if (lcidMap != null) {
            return;
        }

        Map<String, Short> map = new HashMap<>(200);
        addLCIDMapEntry(map, "ar", (short) 0x0401);
        addLCIDMapEntry(map, "bg", (short) 0x0402);
        addLCIDMapEntry(map, "ca", (short) 0x0403);
        addLCIDMapEntry(map, "zh", (short) 0x0404);
        addLCIDMapEntry(map, "cs", (short) 0x0405);
        addLCIDMapEntry(map, "da", (short) 0x0406);
        addLCIDMapEntry(map, "de", (short) 0x0407);
        addLCIDMapEntry(map, "el", (short) 0x0408);
        addLCIDMapEntry(map, "es", (short) 0x040a);
        addLCIDMapEntry(map, "fi", (short) 0x040b);
        addLCIDMapEntry(map, "fr", (short) 0x040c);
        addLCIDMapEntry(map, "iw", (short) 0x040d);
        addLCIDMapEntry(map, "hu", (short) 0x040e);
        addLCIDMapEntry(map, "is", (short) 0x040f);
        addLCIDMapEntry(map, "it", (short) 0x0410);
        addLCIDMapEntry(map, "ja", (short) 0x0411);
        addLCIDMapEntry(map, "ko", (short) 0x0412);
        addLCIDMapEntry(map, "nl", (short) 0x0413);
        addLCIDMapEntry(map, "no", (short) 0x0414);
        addLCIDMapEntry(map, "pl", (short) 0x0415);
        addLCIDMapEntry(map, "pt", (short) 0x0416);
        addLCIDMapEntry(map, "rm", (short) 0x0417);
        addLCIDMapEntry(map, "ro", (short) 0x0418);
        addLCIDMapEntry(map, "ru", (short) 0x0419);
        addLCIDMapEntry(map, "hr", (short) 0x041a);
        addLCIDMapEntry(map, "sk", (short) 0x041b);
        addLCIDMapEntry(map, "sq", (short) 0x041c);
        addLCIDMapEntry(map, "sv", (short) 0x041d);
        addLCIDMapEntry(map, "th", (short) 0x041e);
        addLCIDMapEntry(map, "tr", (short) 0x041f);
        addLCIDMapEntry(map, "ur", (short) 0x0420);
        addLCIDMapEntry(map, "in", (short) 0x0421);
        addLCIDMapEntry(map, "uk", (short) 0x0422);
        addLCIDMapEntry(map, "be", (short) 0x0423);
        addLCIDMapEntry(map, "sl", (short) 0x0424);
        addLCIDMapEntry(map, "et", (short) 0x0425);
        addLCIDMapEntry(map, "lv", (short) 0x0426);
        addLCIDMapEntry(map, "lt", (short) 0x0427);
        addLCIDMapEntry(map, "fa", (short) 0x0429);
        addLCIDMapEntry(map, "vi", (short) 0x042a);
        addLCIDMapEntry(map, "hy", (short) 0x042b);
        addLCIDMapEntry(map, "eu", (short) 0x042d);
        addLCIDMapEntry(map, "mk", (short) 0x042f);
        addLCIDMapEntry(map, "tn", (short) 0x0432);
        addLCIDMapEntry(map, "xh", (short) 0x0434);
        addLCIDMapEntry(map, "zu", (short) 0x0435);
        addLCIDMapEntry(map, "af", (short) 0x0436);
        addLCIDMapEntry(map, "ka", (short) 0x0437);
        addLCIDMapEntry(map, "fo", (short) 0x0438);
        addLCIDMapEntry(map, "hi", (short) 0x0439);
        addLCIDMapEntry(map, "mt", (short) 0x043a);
        addLCIDMapEntry(map, "se", (short) 0x043b);
        addLCIDMapEntry(map, "gd", (short) 0x043c);
        addLCIDMapEntry(map, "ms", (short) 0x043e);
        addLCIDMapEntry(map, "kk", (short) 0x043f);
        addLCIDMapEntry(map, "ky", (short) 0x0440);
        addLCIDMapEntry(map, "sw", (short) 0x0441);
        addLCIDMapEntry(map, "tt", (short) 0x0444);
        addLCIDMapEntry(map, "bn", (short) 0x0445);
        addLCIDMapEntry(map, "pa", (short) 0x0446);
        addLCIDMapEntry(map, "gu", (short) 0x0447);
        addLCIDMapEntry(map, "ta", (short) 0x0449);
        addLCIDMapEntry(map, "te", (short) 0x044a);
        addLCIDMapEntry(map, "kn", (short) 0x044b);
        addLCIDMapEntry(map, "ml", (short) 0x044c);
        addLCIDMapEntry(map, "mr", (short) 0x044e);
        addLCIDMapEntry(map, "sa", (short) 0x044f);
        addLCIDMapEntry(map, "mn", (short) 0x0450);
        addLCIDMapEntry(map, "cy", (short) 0x0452);
        addLCIDMapEntry(map, "gl", (short) 0x0456);
        addLCIDMapEntry(map, "dv", (short) 0x0465);
        addLCIDMapEntry(map, "qu", (short) 0x046b);
        addLCIDMapEntry(map, "mi", (short) 0x0481);
        addLCIDMapEntry(map, "ar_IQ", (short) 0x0801);
        addLCIDMapEntry(map, "zh_CN", (short) 0x0804);
        addLCIDMapEntry(map, "de_CH", (short) 0x0807);
        addLCIDMapEntry(map, "en_GB", (short) 0x0809);
        addLCIDMapEntry(map, "es_MX", (short) 0x080a);
        addLCIDMapEntry(map, "fr_BE", (short) 0x080c);
        addLCIDMapEntry(map, "it_CH", (short) 0x0810);
        addLCIDMapEntry(map, "nl_BE", (short) 0x0813);
        addLCIDMapEntry(map, "no_NO_NY", (short) 0x0814);
        addLCIDMapEntry(map, "pt_PT", (short) 0x0816);
        addLCIDMapEntry(map, "ro_MD", (short) 0x0818);
        addLCIDMapEntry(map, "ru_MD", (short) 0x0819);
        addLCIDMapEntry(map, "sr_CS", (short) 0x081a);
        addLCIDMapEntry(map, "sv_FI", (short) 0x081d);
        addLCIDMapEntry(map, "az_AZ", (short) 0x082c);
        addLCIDMapEntry(map, "se_SE", (short) 0x083b);
        addLCIDMapEntry(map, "ga_IE", (short) 0x083c);
        addLCIDMapEntry(map, "ms_BN", (short) 0x083e);
        addLCIDMapEntry(map, "uz_UZ", (short) 0x0843);
        addLCIDMapEntry(map, "qu_EC", (short) 0x086b);
        addLCIDMapEntry(map, "ar_EG", (short) 0x0c01);
        addLCIDMapEntry(map, "zh_HK", (short) 0x0c04);
        addLCIDMapEntry(map, "de_AT", (short) 0x0c07);
        addLCIDMapEntry(map, "en_AU", (short) 0x0c09);
        addLCIDMapEntry(map, "fr_CA", (short) 0x0c0c);
        addLCIDMapEntry(map, "sr_CS", (short) 0x0c1a);
        addLCIDMapEntry(map, "se_FI", (short) 0x0c3b);
        addLCIDMapEntry(map, "qu_PE", (short) 0x0c6b);
        addLCIDMapEntry(map, "ar_LY", (short) 0x1001);
        addLCIDMapEntry(map, "zh_SG", (short) 0x1004);
        addLCIDMapEntry(map, "de_LU", (short) 0x1007);
        addLCIDMapEntry(map, "en_CA", (short) 0x1009);
        addLCIDMapEntry(map, "es_GT", (short) 0x100a);
        addLCIDMapEntry(map, "fr_CH", (short) 0x100c);
        addLCIDMapEntry(map, "hr_BA", (short) 0x101a);
        addLCIDMapEntry(map, "ar_DZ", (short) 0x1401);
        addLCIDMapEntry(map, "zh_MO", (short) 0x1404);
        addLCIDMapEntry(map, "de_LI", (short) 0x1407);
        addLCIDMapEntry(map, "en_NZ", (short) 0x1409);
        addLCIDMapEntry(map, "es_CR", (short) 0x140a);
        addLCIDMapEntry(map, "fr_LU", (short) 0x140c);
        addLCIDMapEntry(map, "bs_BA", (short) 0x141a);
        addLCIDMapEntry(map, "ar_MA", (short) 0x1801);
        addLCIDMapEntry(map, "en_IE", (short) 0x1809);
        addLCIDMapEntry(map, "es_PA", (short) 0x180a);
        addLCIDMapEntry(map, "fr_MC", (short) 0x180c);
        addLCIDMapEntry(map, "sr_BA", (short) 0x181a);
        addLCIDMapEntry(map, "ar_TN", (short) 0x1c01);
        addLCIDMapEntry(map, "en_ZA", (short) 0x1c09);
        addLCIDMapEntry(map, "es_DO", (short) 0x1c0a);
        addLCIDMapEntry(map, "sr_BA", (short) 0x1c1a);
        addLCIDMapEntry(map, "ar_OM", (short) 0x2001);
        addLCIDMapEntry(map, "en_JM", (short) 0x2009);
        addLCIDMapEntry(map, "es_VE", (short) 0x200a);
        addLCIDMapEntry(map, "ar_YE", (short) 0x2401);
        addLCIDMapEntry(map, "es_CO", (short) 0x240a);
        addLCIDMapEntry(map, "ar_SY", (short) 0x2801);
        addLCIDMapEntry(map, "en_BZ", (short) 0x2809);
        addLCIDMapEntry(map, "es_PE", (short) 0x280a);
        addLCIDMapEntry(map, "ar_JO", (short) 0x2c01);
        addLCIDMapEntry(map, "en_TT", (short) 0x2c09);
        addLCIDMapEntry(map, "es_AR", (short) 0x2c0a);
        addLCIDMapEntry(map, "ar_LB", (short) 0x3001);
        addLCIDMapEntry(map, "en_ZW", (short) 0x3009);
        addLCIDMapEntry(map, "es_EC", (short) 0x300a);
        addLCIDMapEntry(map, "ar_KW", (short) 0x3401);
        addLCIDMapEntry(map, "en_PH", (short) 0x3409);
        addLCIDMapEntry(map, "es_CL", (short) 0x340a);
        addLCIDMapEntry(map, "ar_AE", (short) 0x3801);
        addLCIDMapEntry(map, "es_UY", (short) 0x380a);
        addLCIDMapEntry(map, "ar_BH", (short) 0x3c01);
        addLCIDMapEntry(map, "es_PY", (short) 0x3c0a);
        addLCIDMapEntry(map, "ar_QA", (short) 0x4001);
        addLCIDMapEntry(map, "es_BO", (short) 0x400a);
        addLCIDMapEntry(map, "es_SV", (short) 0x440a);
        addLCIDMapEntry(map, "es_HN", (short) 0x480a);
        addLCIDMapEntry(map, "es_NI", (short) 0x4c0a);
        addLCIDMapEntry(map, "es_PR", (short) 0x500a);

        lcidMap = map;
    }

    private static short getLCIDFromLocale(Locale locale) {
        // optimize for common case
        if (locale.equals(Locale.US) || locale.getLanguage().equals("en")) {
            return MS_ENGLISH_LOCALE_ID;
        }

        if (lcidMap == null) {
            createLCIDMap();
        }

        String key = locale.toString();
        while (!key.isEmpty()) {
            Short lcidObject = lcidMap.get(key);
            if (lcidObject != null) {
                return lcidObject.shortValue();
            }
            int pos = key.lastIndexOf('_');
            if (pos < 1) {
                return MS_ENGLISH_LOCALE_ID;
            }
            key = key.substring(0, pos);
        }

        return MS_ENGLISH_LOCALE_ID;
    }


    /* On Windows this is set to the System Locale, which matches how
     * GDI enumerates font names. For display purposes we may want
     * the user locale which could be different.
     */
    static short nameLocaleID = getSystemLCID();

    private static short getSystemLCID() {
        if (PrismFontFactory.isWindows) {
            return PrismFontFactory.getSystemLCID();
        } else {
            return getLCIDFromLocale(Locale.getDefault());
        }
    }

    private OpenTypeGlyphMapper mapper = null;

    @Override
    public CharToGlyphMapper getGlyphMapper() {
        if (mapper == null) {
            mapper = new OpenTypeGlyphMapper(this);
        }
        return mapper;
    }

    @Override
    public FontStrike getStrike(float size, BaseTransform transform) {
        return getStrike(size, transform, getDefaultAAMode());
    }

    @Override
    public float getAdvance(int glyphCode, float ptSize) {
        if (glyphCode == CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
            return 0f;
        }

        /*
         * Platform-specific but it needs to be explained why this is needed.
         * The hmtx table in the Apple Color Emoji font can be woefully off
         * compared to the size of emoji glyph CoreText generates and the advance
         * CoreText supports. So for macOS at least, we need to get those advances
         * another way. Note : I also see "small" discrepancies for ordinary
         * glyphs in the mac system font between hmtx and CoreText.
         * Limit use of this because we aren't caching the result.
         */
        if (PrismFontFactory.isMacOSX && isColorGlyph(glyphCode)) {
            return getAdvanceFromPlatform(glyphCode, ptSize);
        } else {
            return getAdvanceFromHMTX(glyphCode, ptSize);
        }
    }

    /* REMIND: We can cache here if it is slow */
    protected float getAdvanceFromPlatform(int glyphCode, float ptSize) {
        return getAdvanceFromHMTX(glyphCode, ptSize);
    }

    char[] advanceWidths = null;
    /*
     * This is returning the unhinted advance, should be OK so
     * long as we do unhinted rendering. If we are doing hinted glyphs
     * and I suppose, integer metrics, then we can use the hdmx table.
     * But since the hdmx table doesn't provide anything except integers
     * it will only be useful for some cases. Also even then the ptSize
     * alone doesn't help, since we need to know the graphics scale
     * to know the real glyph size that's required, then of course we
     * have to translate that back into user space. So all of that will
     * need to be looked into, or we reserve this path for unhinted rendering.
     * Note that if there's no hdmx entry for a given size, then we need
     * to scale the glyph to get the hinted advance. However before doing
     * so we should consult the 'gasp' table to see it its a size at
     * which hinting should be performed anyway.
     * (1) The GASP table indicates size at which hinting should be applied
     * usually this is all larger sizes so probably wouldn't help, however
     * (2) If there is a LTSH (Linear Threshold) table, we can use that
     * to see if for the requested 'ppem' size, the glyph scales linearly.
     *
     * Interestingly Amble sets the 'head' flags bit to say non-linear
     * scaling and so legitimately has a LTSH table but this all may be
     * a hold-over from when its gasp table said to apply hints at some sizes.
     * I suppose I am not 100% certain if the gasp table can be trusted to
     * use as a short-cut for when you don't need to scale, or if choosing
     * not to hint means you can always just assume linear scaling, but I
     * do find that to be consistent with the data in Microsoft fonts where
     * they do not provide hdmx entry for sizes below that where hinting is
     * required, suggesting the htmx table is fine for such cases.
     */
    private float getAdvanceFromHMTX(int glyphCode, float ptSize) {

        // If we haven't initialised yet, do so now.
        if (advanceWidths == null && numHMetrics > 0) {
            synchronized (this) {
                Buffer hmtx = readTable(hmtxTag);
                if (hmtx == null) {
                    numHMetrics = -1;
                    return 0;
                }
                char[] aw = new char[numHMetrics];
                for (int i=0; i<numHMetrics; i++) {
                    aw[i] = hmtx.getChar(i*4);
                }
                advanceWidths = aw;
            }
        }

        // If we have a valid numHMetrics, look up the advance
        if (numHMetrics > 0) {
            char cadv;
            if (glyphCode < numHMetrics) {
                cadv = advanceWidths[glyphCode];
            } else {
                cadv = advanceWidths[numHMetrics-1];
            }
            return ((cadv & 0xffff)*ptSize)/upem;
        } else { // no valid lookup.
            return 0f;
        }
    }

    public PrismMetrics getFontMetrics(float ptSize) {
        return new PrismMetrics((ascent*ptSize)/upem,
                              (descent*ptSize)/upem,
                              (linegap*ptSize)/upem,
                              this, ptSize);
    }

    private float[] styleMetrics;
    float[] getStyleMetrics(float ptSize) {
        if (styleMetrics == null) {
            float [] smetrics = new float[METRICS_TOTAL];

            Buffer os_2 = readTable(os_2Tag);
            int length = os_2 != null ? os_2.capacity() : 0;

            if (length >= 30) {
                smetrics[STRIKETHROUGH_THICKNESS] = os_2.getShort(26) / upem;
                smetrics[STRIKETHROUGH_OFFSET] = -os_2.getShort(28) / upem;
                if (smetrics[STRIKETHROUGH_THICKNESS] < 0f) {
                    smetrics[STRIKETHROUGH_THICKNESS] = 0.05f;
                }
                if (Math.abs(smetrics[STRIKETHROUGH_OFFSET]) > 2.0f) {
                    smetrics[STRIKETHROUGH_OFFSET] = -0.4f;
                }
            } else {
                smetrics[STRIKETHROUGH_THICKNESS] = 0.05f;
                smetrics[STRIKETHROUGH_OFFSET] = -0.4f;
            }
            if (length >= 74) {
                // ascent, descent, leading are set in constructor
                smetrics[TYPO_ASCENT] = -os_2.getShort(68) / upem;
                smetrics[TYPO_DESCENT] = -os_2.getShort(70) / upem;
                smetrics[TYPO_LINEGAP] = os_2.getShort(72) / upem;
            } else {
                smetrics[TYPO_ASCENT] = ascent / upem;
                smetrics[TYPO_DESCENT] = descent / upem;
                smetrics[TYPO_LINEGAP] = linegap / upem;
            }
            // REMIND : OpenType spec introduced xHeight, many fonts
            // won't have this info.
            // xHeight should be available in OS2 font table ver. 3 or greater
            if (length >= 90) {
                smetrics[XHEIGHT] = os_2.getShort(86) / upem;
                smetrics[CAPHEIGHT] = os_2.getShort(88);

                /* Some fonts have bad values for capHeight. For example,
                 * Comic Sans MS. The fix is to ignore the capHeight in the
                 * font file when it is less than half of the ascent */
                if ((smetrics[CAPHEIGHT] / ascent) < 0.5) {
                    smetrics[CAPHEIGHT] = 0;
                } else {
                    smetrics[CAPHEIGHT] /= upem;
                }
            }

            if (smetrics[XHEIGHT] == 0 || smetrics[CAPHEIGHT] == 0) {
                FontStrike strike = getStrike(ptSize, BaseTransform.IDENTITY_TRANSFORM);
                CharToGlyphMapper mapper = getGlyphMapper();
                int missingGlyph = mapper.getMissingGlyphCode();

                if (smetrics[XHEIGHT] == 0) {
                    int gc = mapper.charToGlyph('x');
                    if (gc != missingGlyph) {
                        RectBounds fbds = strike.getGlyph(gc).getBBox();
                        smetrics[XHEIGHT] = fbds.getHeight() / ptSize;
                    } else {
                        smetrics[XHEIGHT] = -ascent * 0.6f / upem;
                    }
                }
                if (smetrics[CAPHEIGHT] == 0) {
                    int gc = mapper.charToGlyph('H');
                    if (gc != missingGlyph) {
                        RectBounds fbds = strike.getGlyph(gc).getBBox();
                        smetrics[CAPHEIGHT] = fbds.getHeight() / ptSize;
                    } else {
                        smetrics[CAPHEIGHT] = -ascent * 0.9f / upem;
                    }
                }
            }

            Buffer postTable = readTable(postTag);
            if (postTable == null || postTable.capacity() < 12) {
                smetrics[UNDERLINE_OFFSET] = 0.1f;
                smetrics[UNDERLINE_THICKESS] = 0.05f;
            } else {
                smetrics[UNDERLINE_OFFSET] = -postTable.getShort(8) / upem;
                smetrics[UNDERLINE_THICKESS] = postTable.getShort(10) / upem;
                if (smetrics[UNDERLINE_THICKESS] < 0f) {
                    smetrics[UNDERLINE_THICKESS] = 0.05f;
                }
                if (Math.abs(smetrics[UNDERLINE_OFFSET]) > 2.0f) {
                    smetrics[UNDERLINE_OFFSET] = 0.1f;
                }
            }
            styleMetrics = smetrics;
        }

        float[] metrics = new float[METRICS_TOTAL];
        for (int i = 0; i < METRICS_TOTAL; i++) {
            metrics[i] = styleMetrics[i] * ptSize;
        }

        return metrics;
    }

    byte[] getTableBytes(int tag) {
        Buffer buffer = readTable(tag);
        byte[] table = null;
        if(buffer != null){
            table = new byte[buffer.capacity()];
            buffer.get(0, table, 0, buffer.capacity());
        }
        return table;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PrismFontFile)) {
            return false;
        }
        final PrismFontFile other = (PrismFontFile)obj;
        return filename.equals(other.filename) && fullName.equals(other.fullName);
    }

    @Override
    public int hashCode() {
        return filename.hashCode() + (71 * fullName.hashCode());
    }


    private boolean checkedColorTables;
    private boolean hasColorTables;
    private synchronized boolean fontSupportsColorGlyphs() {
       if (checkedColorTables) {
           return hasColorTables;
       }
       hasColorTables =
           getDirectoryEntry(sbixTag) != null ||
           getDirectoryEntry(colrTag) != null;
       checkedColorTables = true;

       return hasColorTables;
    }

    public boolean isColorGlyph(int glyphID) {
        if (!fontSupportsColorGlyphs()) {
            return false;
        }
        if (getDirectoryEntry(sbixTag) != null) {
            return isSbixGlyph(glyphID);
        }
        return false;
   }


   private static final int USHORT_MASK = 0xffff;
   private static final int UINT_MASK   = 0xffffffff;

   static class ColorGlyphStrike {

       private int ppem;
       private int ppi;
       private int dataOffsets[];

       ColorGlyphStrike(int ppem, int ppi, int[] offsets) {
           this.ppem = ppem;
           this.ppi  = ppi ;
           dataOffsets = offsets;
       }

       boolean hasGlyph(int gid) {
           if (gid >= dataOffsets.length-1) {
              return false;
           }
           /* Per the OpenType sbix specthere's one extra offset.
            */
           return dataOffsets[gid] < dataOffsets[gid+1];
       }
   }

   ColorGlyphStrike[] sbixStrikes = null;

   private boolean isSbixGlyph(int glyphID) {
       if (sbixStrikes == null) {
           synchronized (this) {
               buildSbixStrikeTables();
               if (sbixStrikes == null) {
                   sbixStrikes = new ColorGlyphStrike[0];
               }
           }
       }
       for (int i=0; i<sbixStrikes.length; i++) {
          if (sbixStrikes[i].hasGlyph(glyphID)) {
              return true;
          }
       }
       return false;
   }

   private void buildSbixStrikeTables() {

       Buffer sbixTable = readTable(sbixTag);

       if (sbixTable == null) {
           return;
       }
       int sz = sbixTable.capacity();
       sbixTable.skip(4); // past version and flags
       int numStrikes = sbixTable.getInt() & UINT_MASK;
       if (numStrikes <= 0 || numStrikes >= sz) {
           return;
       }
       int[] strikeOffsets = new int[numStrikes];
       for (int i=0; i<numStrikes; i++) {
           strikeOffsets[i] = sbixTable.getInt() & UINT_MASK;
           if (strikeOffsets[i] >= sz) {
               return;
           }
       }
       int numGlyphs = getNumGlyphs();
       ColorGlyphStrike[] strikes = new ColorGlyphStrike[numStrikes];
       for (int i=0; i<numStrikes; i++) {
           if (strikeOffsets[i] + 4 + (4*(numGlyphs+1)) > sz) {
                return;
           }
           sbixTable.position(strikeOffsets[i]);

           int ppem = sbixTable.getChar() & USHORT_MASK;
           int ppi  = sbixTable.getChar() & USHORT_MASK;
           int[] glyphDataOffsets = new int[numGlyphs+1];
           for (int g=0; g<=numGlyphs; g++) {
               glyphDataOffsets[g] = sbixTable.getInt() & UINT_MASK;
           }
           strikes[i] = new ColorGlyphStrike(ppem, ppi, glyphDataOffsets);
       }
       sbixStrikes = strikes;
   }

}
