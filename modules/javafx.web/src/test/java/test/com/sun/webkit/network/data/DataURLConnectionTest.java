/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.webkit.network.data;

import static com.sun.webkit.network.URLs.newURL;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Test;

/**
 * A test for the {@link DataURLConnection} class.
 */
public class DataURLConnectionTest {

    /**
     * Tests {@link DataURLConnection} on a URL with empty metadata.
     */
    @Test
    public void testEmptyMetadata() throws Exception {
        execute(new TestCase(
                "data:,a",
                "text/plain;charset=US-ASCII",
                "a".getBytes("US-ASCII")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with empty data.
     */
    @Test
    public void testEmptyData() throws Exception {
        execute(new TestCase(
                "data:,",
                "text/plain;charset=US-ASCII",
                "".getBytes("US-ASCII")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with metadata consisting
     * only of the mime type.
     */
    @Test
    public void testMimeTypeOnly() throws Exception {
        execute(new TestCase(
                "data:text/plain,a",
                "text/plain;charset=US-ASCII",
                "a".getBytes("US-ASCII")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with metadata consisting
     * only of the charset parameter.
     */
    @Test
    public void testCharsetOnly() throws Exception {
        execute(new TestCase(
                "data:charset=UTF-8,a",
                "text/plain;charset=UTF-8",
                "a".getBytes("UTF-8")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with a newline character
     * in "text/html" data.
     */
    @Test
    public void testNewLineInTextHtml() throws Exception {
        execute(new TestCase(
                "data:text/html,\n<p>Test",
                "text/html;charset=US-ASCII",
                "\n<p>Test".getBytes("US-ASCII")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with a %xy sequence.
     */
    @Test
    public void testHexSequence() throws Exception {
        execute(new TestCase(
                "data:,%7A",
                "text/plain;charset=US-ASCII",
                "z".getBytes("US-ASCII")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with an illegal
     * %xy sequence.
     */
    @Test
    public void testIllegalHexSequence() throws Exception {
        execute(new TestCase(
                "data:,%AG",
                "text/plain;charset=US-ASCII",
                "%AG".getBytes("US-ASCII")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with an incomplete
     * %xy sequence.
     */
    @Test
    public void testIncompleteHexSequence() throws Exception {
        execute(new TestCase(
                "data:,%A",
                "text/plain;charset=US-ASCII",
                "%A".getBytes("US-ASCII")));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with a base64-encoded data
     * section containing a plus sign.
     */
    @Test
    public void testBase64WithPlusSign() throws Exception {
        execute(new TestCase(
                "data:application/octet-stream;base64,+A==",
                "application/octet-stream",
                bytes(0xF8)));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with a base64-encoded data
     * section containing a slash symbol.
     */
    @Test
    public void testBase64WithSlash() throws Exception {
        execute(new TestCase(
                "data:application/octet-stream;base64,/A==",
                "application/octet-stream",
                bytes(0xFC)));
    }

    /**
     * Tests {@link DataURLConnection} on a URL with a base64-encoded data
     * section containing a "%xx" sequence.
     */
    @Test
    public void testBase64WithHex() throws Exception {
        execute(new TestCase(
                "data:application/octet-stream;base64,%2FA==",
                "application/octet-stream",
                bytes(0xFC)));
    }

    /**
     * Tests {@link DataURLConnection} on an illegal URL.
     */
    @Test
    public void testIllegalUrl() throws Exception {
        URL url = newURL("data:a");
        try {
            url.openConnection();
            fail("ProtocolException expected but not thrown");
        } catch (ProtocolException expected) {}
    }

    /**
     * Tests {@link DataURLConnection} on a URL with an unsupported charset.
     */
    @Test
    public void testUnsupportedCharset() throws Exception {
        URL url = newURL("data:charset=ABC,");
        try {
            url.openConnection();
            fail("UnsupportedEncodingException expected but not thrown");
        } catch (UnsupportedEncodingException expected) {}
    }

    /**
     * Tests {@link DataURLConnection} on some important URLs.
     */
    @Test
    public void testImportantUrls() throws Exception {

        //---------------------
        // RFC 2397 examples //
        //---------------------

        execute(new TestCase(
                "data:,A%20brief%20note",
                "text/plain;charset=US-ASCII",
                "A brief note".getBytes("US-ASCII")));

        execute(new TestCase(
                "data:image/gif;base64,R0lGODdhMAAwAPAAAAAAAP///ywAAAAAMAAw" +
                "AAAC8IyPqcvt3wCcDkiLc7C0qwyGHhSWpjQu5yqmCYsapyuvUUlvONmOZt" +
                "fzgFzByTB10QgxOR0TqBQejhRNzOfkVJ+5YiUqrXF5Y5lKh/DeuNcP5yLW" +
                "GsEbtLiOSpa/TPg7JpJHxyendzWTBfX0cxOnKPjgBzi4diinWGdkF8kjdf" +
                "nycQZXZeYGejmJlZeGl9i2icVqaNVailT6F5iJ90m6mvuTS4OK05M0vDk0" +
                "Q4XUtwvKOzrcd3iq9uisF81M1OIcR7lEewwcLp7tuNNkM3uNna3F2JQFo9" +
                "7Vriy/Xl4/f1cf5VWzXyym7PHhhx4dbgYKAAA7",
                "image/gif",
                bytes(
                        0x47, 0x49, 0x46, 0x38, 0x37, 0x61, 0x30, 0x00,
                        0x30, 0x00, 0xF0, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0xFF, 0xFF, 0xFF, 0x2C, 0x00, 0x00, 0x00, 0x00,
                        0x30, 0x00, 0x30, 0x00, 0x00, 0x02, 0xF0, 0x8C,
                        0x8F, 0xA9, 0xCB, 0xED, 0xDF, 0x00, 0x9C, 0x0E,
                        0x48, 0x8B, 0x73, 0xB0, 0xB4, 0xAB, 0x0C, 0x86,
                        0x1E, 0x14, 0x96, 0xA6, 0x34, 0x2E, 0xE7, 0x2A,
                        0xA6, 0x09, 0x8B, 0x1A, 0xA7, 0x2B, 0xAF, 0x51,
                        0x49, 0x6F, 0x38, 0xD9, 0x8E, 0x66, 0xD7, 0xF3,
                        0x80, 0x5C, 0xC1, 0xC9, 0x30, 0x75, 0xD1, 0x08,
                        0x31, 0x39, 0x1D, 0x13, 0xA8, 0x14, 0x1E, 0x8E,
                        0x14, 0x4D, 0xCC, 0xE7, 0xE4, 0x54, 0x9F, 0xB9,
                        0x62, 0x25, 0x2A, 0xAD, 0x71, 0x79, 0x63, 0x99,
                        0x4A, 0x87, 0xF0, 0xDE, 0xB8, 0xD7, 0x0F, 0xE7,
                        0x22, 0xD6, 0x1A, 0xC1, 0x1B, 0xB4, 0xB8, 0x8E,
                        0x4A, 0x96, 0xBF, 0x4C, 0xF8, 0x3B, 0x26, 0x92,
                        0x47, 0xC7, 0x27, 0xA7, 0x77, 0x35, 0x93, 0x05,
                        0xF5, 0xF4, 0x73, 0x13, 0xA7, 0x28, 0xF8, 0xE0,
                        0x07, 0x38, 0xB8, 0x76, 0x28, 0xA7, 0x58, 0x67,
                        0x64, 0x17, 0xC9, 0x23, 0x75, 0xF9, 0xF2, 0x71,
                        0x06, 0x57, 0x65, 0xE6, 0x06, 0x7A, 0x39, 0x89,
                        0x95, 0x97, 0x86, 0x97, 0xD8, 0xB6, 0x89, 0xC5,
                        0x6A, 0x68, 0xD5, 0x5A, 0x8A, 0x54, 0xFA, 0x17,
                        0x98, 0x89, 0xF7, 0x49, 0xBA, 0x9A, 0xFB, 0x93,
                        0x4B, 0x83, 0x8A, 0xD3, 0x93, 0x34, 0xBC, 0x39,
                        0x34, 0x43, 0x85, 0xD4, 0xB7, 0x0B, 0xCA, 0x3B,
                        0x3A, 0xDC, 0x77, 0x78, 0xAA, 0xF6, 0xE8, 0xAC,
                        0x17, 0xCD, 0x4C, 0xD4, 0xE2, 0x1C, 0x47, 0xB9,
                        0x44, 0x7B, 0x0C, 0x1C, 0x2E, 0x9E, 0xED, 0xB8,
                        0xD3, 0x64, 0x33, 0x7B, 0x8D, 0x9D, 0xAD, 0xC5,
                        0xD8, 0x94, 0x05, 0xA3, 0xDE, 0xD5, 0xAE, 0x2C,
                        0xBF, 0x5E, 0x5E, 0x3F, 0x7F, 0x57, 0x1F, 0xE5,
                        0x55, 0xB3, 0x5F, 0x2C, 0xA6, 0xEC, 0xF1, 0xE1,
                        0x87, 0x1E, 0x1D, 0x6E, 0x06, 0x0A, 0x00, 0x00,
                        0x3B)));

        execute(new TestCase(
                "data:text/plain;charset=iso-8859-7,%be%fg%be",
                "text/plain;charset=iso-8859-7",
                "Ύ%fgΎ".getBytes("iso-8859-7")));

        execute(new TestCase(
                "data:application/vnd-xxx-query," +
                "select_vcount,fcol_fieldtable/local",
                "application/vnd-xxx-query",
                "select_vcount,fcol_fieldtable/local".getBytes("US-ASCII")));


        //-------------------------------
        // URLs used by the Acid2 test //
        //-------------------------------

        execute(new TestCase(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAA" +
                "ACQd1PeAAAADElEQVR42mP4%2F58BAAT%2FAf9jgNErAAAAAElFTkSuQmCC",
                "image/png",
                bytes(
                        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                        0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
                        0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
                        0x54, 0x78, 0xDA, 0x63, 0xF8, 0xFF, 0x9F, 0x01,
                        0x00, 0x04, 0xFF, 0x01, 0xFF, 0x63, 0x80, 0xD1,
                        0x2B, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
                        0x44, 0xAE, 0x42, 0x60, 0x82)));

        execute(new TestCase(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAA" +
                "AD91JpzAAAABnRSTlMAAAAAAABupgeRAAAABmJLR0QA%2FwD%2FAP%2Bgva" +
                "eTAAAAEUlEQVR42mP4%2F58BCv7%2FZwAAHfAD%2FabwPj4AAAAASUVORK5" +
                "CYII%3D",
                "image/png",
                bytes(
                        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                        0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x02,
                        0x08, 0x02, 0x00, 0x00, 0x00, 0xFD, 0xD4, 0x9A,
                        0x73, 0x00, 0x00, 0x00, 0x06, 0x74, 0x52, 0x4E,
                        0x53, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x6E,
                        0xA6, 0x07, 0x91, 0x00, 0x00, 0x00, 0x06, 0x62,
                        0x4B, 0x47, 0x44, 0x00, 0xFF, 0x00, 0xFF, 0x00,
                        0xFF, 0xA0, 0xBD, 0xA7, 0x93, 0x00, 0x00, 0x00,
                        0x11, 0x49, 0x44, 0x41, 0x54, 0x78, 0xDA, 0x63,
                        0xF8, 0xFF, 0x9F, 0x01, 0x0A, 0xFE, 0xFF, 0x67,
                        0x00, 0x00, 0x1D, 0xF0, 0x03, 0xFD, 0xA6, 0xF0,
                        0x3E, 0x3E, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
                        0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82)));

        execute(new TestCase(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAA" +
                "AFSDNYfAAAAaklEQVR42u3XQQrAIAwAQeP%2F%2F6wf8CJBJTK9lnQ7FpHG" +
                "aOurt1I34nfH9pMMZAZ8BwMGEvvh%2BBsJCAgICLwIOA8EBAQEBAQEBAQEB" +
                "K79H5RfIQAAAAAAAAAAAAAAAAAAAAAAAAAAAID%2FABMSqAfj%2FsLmvAAA" +
                "AABJRU5ErkJggg%3D%3D",
                "image/png",
                bytes(
                        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                        0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x40,
                        0x08, 0x02, 0x00, 0x00, 0x01, 0x52, 0x0C, 0xD6,
                        0x1F, 0x00, 0x00, 0x00, 0x6A, 0x49, 0x44, 0x41,
                        0x54, 0x78, 0xDA, 0xED, 0xD7, 0x41, 0x0A, 0xC0,
                        0x20, 0x0C, 0x00, 0x41, 0xE3, 0xFF, 0xFF, 0xAC,
                        0x1F, 0xF0, 0x22, 0x41, 0x25, 0x32, 0xBD, 0x96,
                        0x74, 0x3B, 0x16, 0x91, 0xC6, 0x68, 0xEB, 0xAB,
                        0xB7, 0x52, 0x37, 0xE2, 0x77, 0xC7, 0xF6, 0x93,
                        0x0C, 0x64, 0x06, 0x7C, 0x07, 0x03, 0x06, 0x12,
                        0xFB, 0xE1, 0xF8, 0x1B, 0x09, 0x08, 0x08, 0x08,
                        0x08, 0xBC, 0x08, 0x38, 0x0F, 0x04, 0x04, 0x04,
                        0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
                        0xAE, 0xFD, 0x1F, 0x94, 0x5F, 0x21, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x80, 0xFF, 0x00, 0x13, 0x12,
                        0xA8, 0x07, 0xE3, 0xFE, 0xC2, 0xE6, 0xBC, 0x00,
                        0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE,
                        0x42, 0x60, 0x82)));

        execute(new TestCase(
                "data:text/css,.picture%20%7B%20background%3A%20none%3B%20%7D",
                "text/css;charset=US-ASCII",
                ".picture { background: none; }".getBytes("US-ASCII")));

        execute(new TestCase(
                "data:application/x-unknown,ERROR",
                "application/x-unknown",
                "ERROR".getBytes("US-ASCII")));

        //-------------------------------
        // Acid3 tests                 //
        //-------------------------------

        execute(new TestCase(
                "data:text/javascript,d1%20%3D%20'one'%3B",
                "text/javascript;charset=US-ASCII",
                "d1 = 'one';".getBytes("US-ASCII")));

        execute(new TestCase(
                "data:text/javascript;base64,ZDIgPSAndHdvJzs%3D",
                "text/javascript;charset=US-ASCII",
                "d2 = 'two';".getBytes("US-ASCII")));

        execute(new TestCase(
                "data:text/javascript;base64,%5a%44%4d%67%50%53%41%6e%64%47" +
                "%68%79%5a%57%55%6e%4f%77%3D%3D",
                "text/javascript;charset=US-ASCII",
                "d3 = 'three';".getBytes("US-ASCII")));

        execute(new TestCase(
                "data:text/javascript;base64,%20ZD%20Qg%0D%0APS%20An%20Zm91" +
                "cic%0D%0A%207%20",
                "text/javascript;charset=US-ASCII",
                "d4 = 'four';".getBytes("US-ASCII")));

        execute(new TestCase(
                "data:text/javascript,d5%20%3D%20'five%5C\u0027s'%3B",
                "text/javascript;charset=US-ASCII",
                "d5 = 'five\\'s';".getBytes("US-ASCII")));

        //-----------------------------------------
        // URLs that have associated Jira issues //
        //-----------------------------------------

        // RT-14528
        execute(new TestCase(
                "data:text/html,%3Ca%20id=%22a%22%20href=%22#\" onclick=\"" +
                "document.write(window != top ? '<p>FAIL</p>' : '<p>PASS</p>'" +
                "); return false\">link</a>",
                "text/html;charset=US-ASCII",
                ("<a id=\"a\" href=\"#\" onclick=\"" +
                "document.write(window != top ? '<p>FAIL</p>' : '<p>PASS</p>'" +
                "); return false\">link</a>").getBytes("US-ASCII")));
    }


    /**
     * Executes the specified test case.
     */
    private void execute(TestCase testCase) throws IOException {
        String s = ", url: " + testCase.url;
        URLConnection c = newURL(testCase.url).openConnection();
        assertEquals("Unexpected content type" + s,
                testCase.contentType, c.getContentType());
        assertEquals("Unexpected content encoding" + s,
                null, c.getContentEncoding());
        assertEquals("Unexpected content length" + s,
                testCase.content.length, c.getContentLength());
        assertArrayEquals("Unexpected content" + s,
                testCase.content, readContent(c));
    }

    /**
     * Represents a test case object that can be passed into
     * the {@link #execute} method.
     */
    private static class TestCase {
        private final String url;
        private final String contentType;
        private final byte[] content;

        private TestCase(String url, String contentType, byte[] content) {
            this.url = url;
            this.contentType = contentType;
            this.content = content;
        }
    }

    /**
     * Reads the content of a connection.
     */
    private static byte[] readContent(URLConnection c) throws IOException {
        byte[] content = new byte[0];
        byte[] buffer = new byte[1024];
        InputStream is = c.getInputStream();
        int length ;
        while ((length = is.read(buffer)) != -1) {
            byte[] newContent = new byte[content.length + length];
            System.arraycopy(content, 0, newContent, 0, content.length);
            System.arraycopy(buffer, 0, newContent, content.length, length);
            content = newContent;
        }
        return content;
    }

    /**
     * Creates an array of bytes from a sequence of integers.
     */
    private static byte[] bytes(int... ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            int n = ints[i];
            if (n < 0 || n > 255) {
                throw new IllegalArgumentException(
                        "Array component is out of byte range: " + n);
            }
            bytes[i] = (byte) n;
        }
        return bytes;
    }
}
