/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import org.junit.Ignore;

@Ignore
public class UTITest {

//    @Test
//    public void testNullIdentifierBuilder() {
//        try {
//            DataFormat.Builder b = new DataFormat.Builder(null);
//            fail("Identifier in DataFormat builder can't be null");
//        } catch (IllegalArgumentException e) {    }
//    }
//
//    @Test
//    public void testEmptyIdentifierBuilder() {
//        try {
//            DataFormat.Builder b = new DataFormat.Builder(null);
//            fail("Identifier in DataFormat builder can't be null");
//        } catch (IllegalArgumentException e) {    }
//    }
//
//    @Test
//    public void testDuplicateIdentifierBuilder() {
//        // first builder should builder fine
//        DataFormat uti1 = new DataFormat.Builder("testDuplicateIdentifierBuilder").build();
//
//        // second attempt to use the same identifier should fail
//        try {
//            DataFormat uti2 = new DataFormat.Builder("testDuplicateIdentifierBuilder").build();
//            fail("Identifier in DataFormat builder can't be reused.");
//        } catch (IllegalArgumentException e) {    }
//    }
//
//    @Test
//    public void testEmptyBuilder() {
//        DataFormat.Builder b = new DataFormat.Builder("testEmptyBuilder");
//        DataFormat emptyUTI = b.build();
//
//        assertEquals(1, size(emptyUTI.getConformsTo()));
//        assertEquals(0, size(emptyUTI.getExtensions()));
//        assertEquals(0, size(emptyUTI.getMimeTypes()));
//    }
//
//    @Test
//    public void testBuilderWithNullConformsTo() {
//        DataFormat.Builder b = new DataFormat.Builder("testBuilderWithNullConformsTo");
//        b.conformsTo(null);
//        DataFormat uti = b.build();
//
//        // the DataFormat will always conform to itself, hence the 1 here
//        assertEquals(1, size(uti.getConformsTo()));
//        assertTrue(uti.conformsTo(uti));
//    }
//
//    @Test
//    public void testBuilderConformsToParents() {
//        DataFormat.Builder b = new DataFormat.Builder("testBuilderConformsToParents");
//        b.conformsTo(DataFormat.IMAGE_JPEG);
//        DataFormat uti = b.build();
//
//        // the DataFormat will always conform to itself, as well as any UTIs specified,
//        // and any DataFormat's that the given DataFormat conforms to.
//        // In the case of IMAGE_PNG, we know that it has the following hierarchy:
//        // IMAGE_PNG -> IMAGE -> DATA    -> ITEM
//        //                    -> CONTENT
//        // so, it should be a total size of 5 + itself = 6
//        assertEquals(6, size(uti.getConformsTo()));
//    }
//
//    @Test
//    public void testNoExtensionBuilder() {
//        DataFormat uti = new DataFormat.Builder("testNoExtensionBuilder").build();
//        assertNotNull(uti.getExtensions());
//        assertEquals(0, size(uti.getExtensions()));
//    }
//
//    @Test
//    public void testNullExtensionBuilder() {
//        DataFormat uti = new DataFormat.Builder("testNullExtensionBuilder").extension(null).build();;
//        assertEquals(0, size(uti.getExtensions()));
//    }
//
//    @Test
//    public void testEmptyExtensionBuilder() {
//        DataFormat uti = new DataFormat.Builder("testEmptyExtensionBuilder").extension("").build();
//        assertEquals(0, size(uti.getExtensions()));
//    }
//
//    @Test
//    public void testDuplicateExtensionBuilder() {
//        DataFormat uti = new DataFormat.Builder("testDuplicateExtensionBuilder").
//                extension("ext1").extension("ext2").extension("ext1").
//                build();
//
//        // size should be 2, not 3, as we don't allow for duplicates
//        assertEquals(2, size(uti.getExtensions()));
//    }
//
//    @Test
//    public void testNoMimeTypeBuilder() {
//        DataFormat uti = new DataFormat.Builder("testNoMimeTypeBuilder").build();
//        assertNotNull(uti.getMimeTypes());
//        assertEquals(0, size(uti.getMimeTypes()));
//    }
//
//    @Test
//    public void testNullMimeTypeBuilder() {
//        DataFormat uti = new DataFormat.Builder("testNullMimeTypeBuilder").mimeType(null).build();;
//        assertEquals(0, size(uti.getMimeTypes()));
//    }
//
//    @Test
//    public void testEmptyMimeTypeBuilder() {
//        DataFormat uti = new DataFormat.Builder("testEmptyMimeTypeBuilder").mimeType("").build();
//        assertEquals(0, size(uti.getMimeTypes()));
//    }
//
//    @Test
//    public void testDuplicateMimeTypeBuilder() {
//        DataFormat uti = new DataFormat.Builder("testDuplicateMimeTypeBuilder").
//                mimeType("test/mime").mimeType("test/mime2").mimeType("test/mime").
//                build();
//
//        // size should be 2, not 3, as we don't allow for duplicates
//        assertEquals(2, size(uti.getMimeTypes()));
//    }
//
//    @Test
//    public void testStaticLookupExtension() {
//        assertNull(DataFormat.lookupExtension(null));
//        assertNull(DataFormat.lookupExtension(""));
//        assertNull(DataFormat.lookupExtension(".gobblygook"));
//        assertEquals(DataFormat.PLAIN_TEXT, DataFormat.lookupExtension(".txt"));
//    }
//
//    @Test
//    public void testStaticLookupChildren() {
//        assertNotNull(DataFormat.lookupChildren(null));
//        assertEquals(0, size(DataFormat.lookupChildren(null)));
//
//        // there are no children of the IMAGE_PNG type, so we expect 0
//        Iterable<DataFormat> it = DataFormat.lookupChildren(DataFormat.IMAGE_PNG);
//        assertEquals("Expected no children, but got " + toString(it), 0, size(it));
//
//        // there are is one child of the DIRECTORY type - a FOLDER
//        it = DataFormat.lookupChildren(DataFormat.DIRECTORY);
//        assertEquals(1, size(it));
//        assertEquals(DataFormat.FOLDER, it.iterator().next());
//    }
//
//    @Test
//    public void testStaticLookupMimeType() {
//        assertNull(DataFormat.lookupMimeType(null));
//        assertNull(DataFormat.lookupMimeType(""));
//        assertNull(DataFormat.lookupMimeType("test/gobblygook"));
//        assertEquals(DataFormat.PLAIN_TEXT, DataFormat.lookupMimeType("text/plain"));
//    }
//
//    @Test
//    public void testStaticLookupOSType() {
//        assertNull(DataFormat.lookupOSType(null));
//        assertNull(DataFormat.lookupOSType(""));
//        assertNull(DataFormat.lookupOSType("Gobblygook"));
//        assertEquals(DataFormat.IMAGE_PNG, DataFormat.lookupOSType("PNGf"));
//        assertFalse("Must be case-sensitive, pngf != PNGf", DataFormat.IMAGE_PNG.equals(DataFormat.lookupOSType("pngf")));
//    }
//
//    @Test
//    public void testEquals() {
//        assertEquals(DataFormat.ITEM, DataFormat.ITEM);
//        assertSame(DataFormat.ITEM, DataFormat.ITEM);
//
//        assertFalse(DataFormat.PLAIN_TEXT.equals(DataFormat.ITEM));
//    }
//
//     @Test
//    public void testConformsTo() {
//        assertTrue(DataFormat.ITEM.conformsTo(DataFormat.ITEM));
//        assertTrue(DataFormat.PLAIN_TEXT.conformsTo(DataFormat.ITEM));
//        assertFalse(DataFormat.ITEM.conformsTo(null));
//    }
//
//    @Test
//    public void testPublicItem() {
//        assertNotNull(DataFormat.ITEM);
//        assertTrue(DataFormat.ITEM.conformsTo(DataFormat.ITEM));
//        assertEquals(1, size(DataFormat.ITEM.getConformsTo()));
//        assertEquals(0, size(DataFormat.ITEM.getExtensions()));
//        assertEquals(0, size(DataFormat.ITEM.getMimeTypes()));
//    }
//
//    @Test
//    public void testPublicText() {
//        assertNotNull(DataFormat.TEXT);
//        assertTrue(DataFormat.TEXT.conformsTo(DataFormat.TEXT));
//        assertTrue(DataFormat.TEXT.conformsTo(DataFormat.CONTENT));
//        assertTrue(DataFormat.TEXT.conformsTo(DataFormat.DATA));
//        assertTrue(DataFormat.TEXT.conformsTo(DataFormat.ITEM));
//        assertEquals(4, size(DataFormat.TEXT.getConformsTo()));
//        assertEquals(0, size(DataFormat.TEXT.getExtensions()));
//        assertEquals(0, size(DataFormat.TEXT.getMimeTypes()));
//    }
//
//    @Test
//    public void testPublicPlainText() {
//        assertNotNull(DataFormat.PLAIN_TEXT);
//        assertTrue(DataFormat.PLAIN_TEXT.conformsTo(DataFormat.PLAIN_TEXT));
//        assertTrue(DataFormat.PLAIN_TEXT.conformsTo(DataFormat.TEXT));
//        assertTrue(DataFormat.PLAIN_TEXT.conformsTo(DataFormat.CONTENT));
//        assertTrue(DataFormat.PLAIN_TEXT.conformsTo(DataFormat.DATA));
//        assertTrue(DataFormat.PLAIN_TEXT.conformsTo(DataFormat.ITEM));
//        assertEquals(5, size(DataFormat.PLAIN_TEXT.getConformsTo()));
//
//        assertEquals(1, size(DataFormat.PLAIN_TEXT.getExtensions()));
//        assertArrayEquals(toStringArray(".txt"), toStringArray(DataFormat.PLAIN_TEXT.getExtensions()));
//
//        assertEquals(1, size(DataFormat.PLAIN_TEXT.getMimeTypes()));
//        assertArrayEquals(toStringArray("text/plain"), toStringArray(DataFormat.PLAIN_TEXT.getMimeTypes()));
//    }
//
//    private String toString(Iterable<DataFormat> it) {
//        StringBuilder sb = new StringBuilder("[ ");
//
//        Iterator<DataFormat> iterator = it.iterator();
//        while (iterator.hasNext()) {
//            sb.append(iterator.next());
//
//            if (iterator.hasNext()) {
//                sb.append(", ");
//            }
//        }
//
//        sb.append(" ]");
//        return sb.toString();
//    }
//
//    private String[] toStringArray(String... s) {
//        return s;
//    }
//
//    private String[] toStringArray(Iterable<String> it) {
//        return toList(it).toArray(new String[] { });
//    }
//
//    private <T> List<T> toList(Iterable<T> it) {
//        List<T> l = new ArrayList<T>();
//
//        if (it != null) {
//            for (T i : it) {
//                l.add(i);
//            }
//        }
//
//        return l;
//    }
//
//    private int size(Iterable it) {
//        return toList(it).size();
//    }
}
