/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.font.directwrite;

import com.sun.javafx.font.directwrite.DWNativeShim;
import com.sun.javafx.font.directwrite.DWNativeShim.Api;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The runtime proof of the {@code DWNative} COM machinery: that word 0 of an interface pointer really
 * is its vtable, that every slot number the facade uses names the method it claims, and that the
 * reference-counting rule holds.
 * <p>
 * The slot table is derived from {@code dwrite.h} by counting declarations, which is exactly the kind
 * of arithmetic that can be silently wrong: a wrong slot calls a different method of the same object,
 * with different argument types, and on Win64 that usually does <em>not</em> fault - it returns
 * nonsense or corrupts memory. So every interface the facade binds is checked here against an answer
 * that is knowable in advance: "Arial" is family "Arial", its regular face weighs 400, stretches 5 and
 * slants 0 (three different values in three adjacent slots, so no permutation of them can pass),
 * {@code GetString} returns the family name where the neighbouring {@code GetLocaleName} would return
 * a locale, and {@code arial.ttf} analyses as a supported one-face font file.
 * <p>
 * Parity against the {@code OS} natives was a separate test class, deleted in the change set that
 * flipped the peers onto this facade: from that point both sides of the comparison were this code.
 */
@EnabledOnOs(OS.WINDOWS)
public class DWNativeTest {

    /** {@code dwrite.h}: {@code DWRITE_FONT_WEIGHT_NORMAL}, {@code _STRETCH_NORMAL}, {@code _STYLE_NORMAL}. */
    private static final int WEIGHT_NORMAL = 400;
    private static final int STRETCH_NORMAL = 5;
    private static final int STYLE_NORMAL = 0;

    /** {@code DWRITE_INFORMATIONAL_STRING_WIN32_FAMILY_NAMES}. */
    private static final int WIN32_FAMILY_NAMES = 11;

    private static final Api API = DWNativeShim.ffm();

    private static long factory;
    private static long collection;

    @BeforeAll
    static void createFactory() {
        DWNativeShim.ensureLoaded();
        assertTrue(DWNativeShim.isAvailable(),
                "dwrite.dll must load and export DWriteCreateFactory on Windows");
        factory = API.createFactory(DWNativeShim.factoryTypeShared());
        assertNotEquals(0L, factory, "DWriteCreateFactory(DWRITE_FACTORY_TYPE_SHARED)");
        collection = API.getSystemFontCollection(factory, false);
        assertNotEquals(0L, collection, "IDWriteFactory::GetSystemFontCollection");
    }

    @AfterAll
    static void releaseFactory() {
        if (collection != 0) {
            API.release(collection);
            collection = 0;
        }
        if (factory != 0) {
            API.release(factory);
            factory = 0;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Linkage
     * ------------------------------------------------------------------------------------------- */

    /**
     * The named surface of the whole facade. {@code dwrite.dll!DWriteCreateFactory} is always bound -
     * this class initializes it - while the four {@code ole32.dll} and {@code d2d1.dll} entry points
     * of the Direct2D/WIC path are behind lazy holder classes, so whether they are present here
     * depends on whether anything has needed COM or Direct2D yet in this JVM.
     * {@code DWRenderTest.ole32AndD2D1ResolveTheirEntryPoints} pins the full set after forcing both.
     * Everything else in the package is a vtable slot and has no name to resolve.
     */
    @Test
    public void bindsTheExportedEntryPointsAndNothingElse() {
        assertTrue(DWNativeShim.boundSymbols().contains("dwrite.dll!DWriteCreateFactory"),
                () -> "the one symbol the factory bindings bind by name: " + DWNativeShim.boundSymbols());
        assertTrue(Set.of("dwrite.dll!DWriteCreateFactory", "ole32.dll!CoInitializeEx",
                        "ole32.dll!CoUninitialize", "ole32.dll!CoCreateInstance",
                        "d2d1.dll!D2D1CreateFactory").containsAll(DWNativeShim.boundSymbols()),
                () -> "no symbol is resolved by name that is not one of the five: "
                        + DWNativeShim.boundSymbols());
    }

    @Test
    public void aMissingLibraryIsNullRatherThanAnError() {
        assertTrue(DWNativeShim.canLoad("dwrite.dll"));
        assertFalse(DWNativeShim.canLoad("no-such-library-xyz.dll"),
                "libraryLookup throws for a library that will not load; the facade must catch it, "
                        + "because DWFactory reads a null factory as 'DirectWrite unavailable'");
    }

    @Test
    public void theFacadeIsUsableFromAnyThread() throws InterruptedException {
        // The first thread to touch DWNative may be the "Prism Font Disposer" daemon, whose loop
        // catches Exception but not Error: a throwing class initializer there would kill it for good.
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                DWNativeShim.ensureLoaded();
                assertTrue(API.getFontFamilyCount(collection) > 0);
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "not-the-fx-thread");
        thread.start();
        thread.join();
        assertNull(failure.get(), () -> "DWNative failed on a plain thread: " + failure.get());
    }

    /* ---------------------------------------------------------------------------------------------
     * GUID marshalling - the one struct that crosses the boundary in the factory and font bindings
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void guidLayoutMatchesGuiddef() {
        assertEquals(16, DWNativeShim.guidLayoutByteSize());
        assertEquals(0, DWNativeShim.guidLayoutOffset("Data1"));
        assertEquals(4, DWNativeShim.guidLayoutOffset("Data2"));
        assertEquals(6, DWNativeShim.guidLayoutOffset("Data3"));
        assertEquals(8, DWNativeShim.guidLayoutOffset("Data4"));
    }

    @Test
    public void guidBytesAreLittleEndianInTheFirstThreeFields() {
        assertArrayEquals(HexFormat.of().parseHex("5aee59b838d85b4ba2e81adc7d93db48"),
                DWNativeShim.guidBytes(DWNativeShim.iidDWriteFactory()),
                "IID_IDWriteFactory {b859ee5a-d838-4b5b-a2e8-1adc7d93db48}, guiddef.h byte order");
        assertArrayEquals(HexFormat.of().parseHex("0000000000000000c000000000000046"),
                DWNativeShim.guidBytes(DWNativeShim.iidUnknown()),
                "IID_IUnknown {00000000-0000-0000-c000-000000000046}");
        assertThrows(IllegalArgumentException.class, () -> DWNativeShim.guidBytes("not-a-uuid"));
    }

    /* ---------------------------------------------------------------------------------------------
     * IUnknown: slots 0, 1, 2
     * ------------------------------------------------------------------------------------------- */

    /**
     * COM's identity rule is that {@code QueryInterface(IID_IUnknown)} answers <em>the same</em> pointer
     * from every interface of one object - not that it answers the pointer it was called on. On this
     * machine {@code dwrite.dll}'s factory is a multiple-inheritance object whose canonical
     * {@code IUnknown} subobject sits eight bytes into it, so the two differ; what must hold is that the
     * round trip comes back to the interface pointer we started from and that the canonical
     * {@code IUnknown} is stable. This is also why the facade reads the vtable per call: a second
     * interface of one object has a <em>different</em> vtable at a different address.
     */
    @Test
    public void slotZeroIsQueryInterface() {
        long asUnknown = DWNativeShim.queryInterface(factory, DWNativeShim.iidUnknown());
        assertNotEquals(0L, asUnknown, "every COM object answers IID_IUnknown");
        long backToFactory = DWNativeShim.queryInterface(asUnknown, DWNativeShim.iidDWriteFactory());
        assertEquals(factory, backToFactory,
                "IUnknown -> IDWriteFactory must land back on the interface pointer we hold");
        long asUnknownAgain = DWNativeShim.queryInterface(backToFactory, DWNativeShim.iidUnknown());
        assertEquals(asUnknown, asUnknownAgain, "the canonical IUnknown of one object is one pointer");

        // Each of the three QueryInterface calls added exactly one reference; give all three back.
        int count = API.release(asUnknownAgain);
        assertEquals(count - 1, API.release(backToFactory));
        assertEquals(count - 2, API.release(asUnknown));

        assertEquals(0L, DWNativeShim.queryInterface(factory, "11111111-2222-3333-4444-555555555555"),
                "an IID the object does not implement gives E_NOINTERFACE, which the facade maps to 0");
    }

    @Test
    public void slotsOneAndTwoAreAddRefAndRelease() {
        int first = API.addRef(factory);
        int second = API.addRef(factory);
        assertEquals(first + 1, second, "AddRef must increment by exactly one");
        assertEquals(first, API.release(factory));
        assertEquals(first - 1, API.release(factory), "Release must undo AddRef exactly");
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFactory, IDWriteFontCollection, IDWriteFontList, IDWriteFontFamily
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void systemFontCollectionHasFamilies() {
        int count = API.getFontFamilyCount(collection);
        assertTrue(count > 0 && count < 100_000, "implausible family count: " + count);
    }

    @Test
    public void findFamilyNameLocatesArialAndRejectsAFictionalFamily() {
        int index = API.findFamilyName(collection, wide("Arial"));
        assertTrue(index >= 0 && index < API.getFontFamilyCount(collection), "Arial index: " + index);

        long family = API.getFontFamily(collection, index);
        assertNotEquals(0L, family);
        try {
            assertEquals("Arial", familyName(family));
        } finally {
            API.release(family);
        }

        assertEquals(-1, API.findFamilyName(collection, wide("No Such Family 4f3c9a2b")),
                "a family that does not exist must be -1, never 0: DWFontFile branches on -1");
        // A null char[] is not tested: both paths pass a NULL WCHAR const*, which FindFamilyName
        // dereferences (EXCEPTION_ACCESS_VIOLATION in dwrite.dll+0x4c4a5, the same on the JNI path).
        // It was reproduced once, identically on both paths, and is documented rather than pinned.
    }

    @Test
    public void fontListSlotsAreReachedThroughAFamilyPointer() {
        long family = arial();
        try {
            int fonts = API.getFontCount(family);
            assertTrue(fonts >= 1 && fonts < 1_000, "Arial face count: " + fonts);
            long font = API.getFont(family, 0);
            assertNotEquals(0L, font);
            try {
                assertEquals("Arial", familyNameOf(font));
            } finally {
                API.release(font);
            }
        } finally {
            API.release(family);
        }
    }

    @Test
    public void theRegularFaceOfArialHasTheKnownWeightStretchAndStyle() {
        long family = arial();
        try {
            long font = API.getFirstMatchingFont(family, WEIGHT_NORMAL, STRETCH_NORMAL, STYLE_NORMAL);
            assertNotEquals(0L, font);
            try {
                // Three adjacent slots with three different answers: no permutation can pass.
                assertEquals(WEIGHT_NORMAL, API.getWeight(font), "IDWriteFont::GetWeight, slot 4");
                assertEquals(STRETCH_NORMAL, API.getStretch(font), "IDWriteFont::GetStretch, slot 5");
                assertEquals(STYLE_NORMAL, API.getStyle(font), "IDWriteFont::GetStyle, slot 6");
                assertEquals(0, API.getSimulations(font), "a real regular face is not simulated");
                assertEquals("Arial", familyNameOf(font));
            } finally {
                API.release(font);
            }
        } finally {
            API.release(family);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFont and IDWriteLocalizedStrings
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void localizedStringsReturnTheNameAndItsTerminator() {
        long family = arial();
        try {
            long names = API.getFamilyNames(family);
            assertNotEquals(0L, names);
            try {
                int index = API.findLocaleName(names, wide("en-us"));
                assertTrue(index >= 0, "en-us must be present for a Windows system font");
                int length = API.getStringLength(names, index);
                assertEquals("Arial".length(), length, "IDWriteLocalizedStrings::GetStringLength, slot 7");

                char[] buffer = API.getString(names, index, length + 1);
                assertEquals(length + 1, buffer.length, "GetString returns exactly size characters");
                assertEquals('\0', buffer[length], "the last one is the terminator");
                assertEquals("Arial", new String(buffer, 0, length),
                        "slot 8 is GetString; the neighbouring GetLocaleName would answer 'en-us'");

                assertEquals(-1, API.findLocaleName(names, wide("zz-nowhere")));
            } finally {
                API.release(names);
            }
        } finally {
            API.release(family);
        }
    }

    @Test
    public void faceNamesAndInformationalStringsReadAsNames() {
        long family = arial();
        try {
            long font = API.getFirstMatchingFont(family, WEIGHT_NORMAL, STRETCH_NORMAL, STYLE_NORMAL);
            try {
                long faceNames = API.getFaceNames(font);
                assertNotEquals(0L, faceNames, "IDWriteFont::GetFaceNames, slot 8");
                try {
                    assertEquals("Regular", englishString(faceNames));
                } finally {
                    API.release(faceNames);
                }

                long win32Family = API.getInformationalStrings(font, WIN32_FAMILY_NAMES);
                assertNotEquals(0L, win32Family, "IDWriteFont::GetInformationalStrings, slot 9");
                try {
                    assertEquals("Arial", englishString(win32Family));
                } finally {
                    API.release(win32Family);
                }
            } finally {
                API.release(font);
            }
        } finally {
            API.release(family);
        }
    }

    @Test
    public void aFontMakesAFontFaceThatTheCollectionMapsBack() {
        long family = arial();
        try {
            long font = API.getFirstMatchingFont(family, WEIGHT_NORMAL, STRETCH_NORMAL, STYLE_NORMAL);
            try {
                long face = API.createFontFace(font);
                assertNotEquals(0L, face, "IDWriteFont::CreateFontFace, slot 13");
                try {
                    long roundTrip = API.getFontFromFontFace(collection, face);
                    assertNotEquals(0L, roundTrip, "IDWriteFontCollection::GetFontFromFontFace, slot 6");
                    try {
                        assertEquals("Arial", familyNameOf(roundTrip));
                        assertEquals(WEIGHT_NORMAL, API.getWeight(roundTrip));
                    } finally {
                        API.release(roundTrip);
                    }
                } finally {
                    API.release(face);
                }
            } finally {
                API.release(font);
            }
        } finally {
            API.release(family);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * IDWriteFontFile and the font-face path
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void analyzeReadsARealFontFileAndTheFaceRoundTrips() {
        long file = API.createFontFileReference(factory, wide(windowsFont("arial.ttf").toString()));
        assertNotEquals(0L, file, "IDWriteFactory::CreateFontFileReference, slot 7");
        try {
            boolean[] supported = new boolean[1];
            int[] fileType = new int[1];
            int[] faceType = new int[1];
            int[] faces = new int[1];
            assertEquals(DWNativeShim.sOk(), API.analyze(file, supported, fileType, faceType, faces),
                    "IDWriteFontFile::Analyze, slot 5 - four out-pointers, the fourth on the stack");
            assertTrue(supported[0], "arial.ttf is a supported font file");
            assertEquals(1, faces[0], "arial.ttf holds one face");

            long face = API.createFontFace(factory, faceType[0], file, 0, 0);
            assertNotEquals(0L, face, "IDWriteFactory::CreateFontFace, slot 9");
            try {
                long font = API.getFontFromFontFace(collection, face);
                assertNotEquals(0L, font, "IDWriteFontCollection::GetFontFromFontFace, slot 6");
                try {
                    assertEquals("Arial", familyNameOf(font));
                } finally {
                    API.release(font);
                }
            } finally {
                API.release(face);
            }
        } finally {
            API.release(file);
        }
    }

    @Test
    public void analyzeRejectsSomethingThatIsNotAFont(@TempDir Path directory) throws IOException {
        Path notAFont = directory.resolve("not-a-font.ttf");
        Files.writeString(notAFont, "this is not a font file");
        long file = API.createFontFileReference(factory, wide(notAFont.toString()));
        assertNotEquals(0L, file);
        try {
            boolean[] supported = new boolean[1];
            API.analyze(file, supported, new int[1], new int[1], new int[1]);
            assertFalse(supported[0]);
        } finally {
            API.release(file);
        }
    }

    @Test
    public void analyzeOnANullPointerIsEFail() {
        assertEquals(DWNativeShim.eFail(), API.analyze(0, new boolean[1], new int[1], new int[1], new int[1]),
                "directwrite.cpp:1961 guards a null this and never calls");
    }

    @Test
    public void analyzeIgnoresOutArraysThatAreNotExactlyOneLong() {
        long file = API.createFontFileReference(factory, wide(windowsFont("arial.ttf").toString()));
        try {
            int[] twoFaces = new int[2];
            assertEquals(DWNativeShim.sOk(), API.analyze(file, null, null, null, twoFaces));
            assertArrayEquals(new int[2], twoFaces,
                    "the C copied an out-param only into an array of length exactly 1");
        } finally {
            API.release(file);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Lifetime
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void aFreshlyCreatedFamilyIsOwnedByExactlyOneReference() {
        long family = API.getFontFamily(collection, API.findFamilyName(collection, wide("Arial")));
        assertNotEquals(0L, family);
        assertEquals(0, API.release(family),
                "an out-parameter arrives with one reference for the caller, so one Release frees it");
    }

    @Test
    public void thePeerReleasesOnceAndToleratesADoubleDispose() {
        long family = arial();
        long[] result = DWNativeShim.releaseTwiceThroughPeer(family);
        assertEquals(0L, result[0], "the peer's first Release is the last reference");
        assertEquals(0L, result[1], "the second returns 0 without a native call");
        assertEquals(0L, result[2], "and the peer has zeroed its pointer, which is what makes "
                + "DWDisposer.dispose() and an explicit Release() safe together");
    }

    @Test
    public void creatingAndReleasingManyObjectsDoesNotDriftTheCollectionsCount() {
        int before = API.addRef(collection) - 1;
        API.release(collection);
        for (int i = 0; i < 200; i++) {
            long family = arial();
            long names = API.getFamilyNames(family);
            API.release(names);
            API.release(family);
        }
        int after = API.addRef(collection) - 1;
        API.release(collection);
        assertEquals(before, after, "200 create/release round trips must leave no reference behind");
    }

    /* ---------------------------------------------------------------------------------------------
     * Helpers
     * ------------------------------------------------------------------------------------------- */

    private static long arial() {
        int index = API.findFamilyName(collection, wide("Arial"));
        assertTrue(index >= 0, "Arial must be installed");
        long family = API.getFontFamily(collection, index);
        assertNotEquals(0L, family);
        return family;
    }

    private static String familyName(long family) {
        long names = API.getFamilyNames(family);
        assertNotEquals(0L, names, "IDWriteFontFamily::GetFamilyNames, slot 6");
        try {
            return englishString(names);
        } finally {
            API.release(names);
        }
    }

    private static String familyNameOf(long font) {
        long family = API.getFontFamily(font);
        assertNotEquals(0L, family, "IDWriteFont::GetFontFamily, slot 3");
        try {
            return familyName(family);
        } finally {
            API.release(family);
        }
    }

    private static String englishString(long strings) {
        int index = API.findLocaleName(strings, wide("en-us"));
        if (index < 0) {
            index = 0;
        }
        int length = API.getStringLength(strings, index);
        char[] buffer = API.getString(strings, index, length + 1);
        assertNotNull(buffer);
        return new String(buffer, 0, length);
    }

    /** What the peers pass: UTF-16 with the NUL the C never added itself. */
    private static char[] wide(String text) {
        return (text + '\0').toCharArray();
    }

    private static Path windowsFont(String fileName) {
        String root = System.getenv("SystemRoot");
        Path path = Path.of(root == null ? "C:\\Windows" : root, "Fonts", fileName);
        assertTrue(Files.exists(path), () -> "missing system font " + path);
        return path;
    }
}
