/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

public final class SubresourceIntegrityTest extends TestBase {

    private File htmlFile;
    // Expectations
    private final static String LOADED = "hello";
    private final static String NOT_LOADED = "not loaded";

    static Stream<Arguments> dataProvider() {
        return Stream.of(
                // shasum -b -a 1 subresource-integrity-test.js | awk '{ print $1 }' | xxd -r -p | base64
                Arguments.of("sha1-/kpzvnGzRkcE9OFn5j8qRE61nZY=", LOADED),
                // shasum -b -a 224 subresource-integrity-test.js | awk '{ print $1 }' | xxd -r -p | base64
                Arguments.of("sha224-zgiBbbuKJixMVEkaOXnvpSYZGsx7SbSZ0QOckg==", LOADED),
                // shasum -b -a 256 subresource-integrity-test.js | awk '{ print $1 }' | xxd -r -p | base64
                Arguments.of("sha256-vcl3cFaIDAtcQBkUZFdY+tW/bjrg6vX1R+hQ8uB5tHc=", LOADED),
                // shasum -b -a 384 subresource-integrity-test.js | awk '{ print $1 }' | xxd -r -p | base64
                Arguments.of("sha384-+GrI+cacF05VlQitRghQhs1by9CSIyc8XgZTbymUg2oA0EYdLiPMtilnFP3LDbkY", LOADED),
                // shasum -b -a 512 subresource-integrity-test.js | awk '{ print $1 }' | xxd -r -p | base64
                Arguments.of("sha512-V8m3j61x5soaVcO83NuHavY7Yn4MQYoUgrqJe38f6QYG9QzzgWbVDB1SrZsZ2CVR1IsOnV2MLhnDaZhWOwHDsw==", LOADED),
                // Only sha256, sha384, sha512 are validated, rest will be ignored and loaded
                // Ref. https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity#Using_Subresource_Integrity
                Arguments.of("sha1-0000000000000000000000000000", LOADED),
                Arguments.of("sha224-0000000000000000000000000000000000000000", LOADED),
                // negative tests, change the hash value and ensure it fails
                Arguments.of("sha256-Vcl3cFaIDAtcQBkUZFdY+tW/bjrg6vX1R+hQ8uB5tHc=", NOT_LOADED),
                Arguments.of("sha384-+grI+cacF05VlQitRghQhs1by9CSIyc8XgZTbymUg2oA0EYdLiPMtilnFP3LDbkY", NOT_LOADED),
                Arguments.of("sha512-v8m3j61x5soaVcO83NuHavY7Yn4MQYoUgrqJe38f6QYG9QzzgWbVDB1SrZsZ2CVR1IsOnV2MLhnDaZhWOwHDsw==", NOT_LOADED),
                // should load for invalid hash algorithm
                Arguments.of("unknown-0000", LOADED),
                Arguments.of("", LOADED)
        );
    }

    // @BeforeEach
    public void setup(String hashValue, String expected) throws Exception {
        // loadContent won't work with CORS, use file:// for main resource.
        htmlFile = new File("subresource-integrity-test.html");
        final FileOutputStream out = new FileOutputStream(htmlFile);
        final String scriptUrl =
                new File("src/test/resources/test/html/subresource-integrity-test.js").toURI().toASCIIString();
        final String html =
                String.format("<html>\n" +
                        "<head><script src='%s' integrity='%s' crossorigin='anonymous'></script></head>\n" +
                        "<body>%s</body>\n" +
                        "</html>", scriptUrl, hashValue, NOT_LOADED);
        out.write(html.getBytes());
        out.close();
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testScriptTagWithCorrectHashValue(String hashValue, String expected) throws Exception {
        setup(hashValue, expected);
        load(htmlFile);
        final String bodyText = (String) executeScript("document.body.innerText");
        assertNotNull("document.body.innerText must be non null for " + hashValue, bodyText);
        assertEquals(expected, bodyText, hashValue);
    }

    @AfterEach
    public void tearDown() {
        if (!htmlFile.delete()) {
            htmlFile.deleteOnExit();
        }
    }
}
