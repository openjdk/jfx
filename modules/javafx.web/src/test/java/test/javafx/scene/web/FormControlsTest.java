/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.web.WebEngineShim;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

public final class FormControlsTest extends TestBase {

    private static final PrintStream ERR = System.err;

    static Stream<Arguments> dataProvider() {
        return Stream.of(
                Arguments.of("<input type='checkbox'/>", "check-box"),
                Arguments.of("<input type='radio'/>", "radio-button"),
                Arguments.of("<input type='button'/>", "button"),
                Arguments.of("<input type='text'/>", "text-field"),
                Arguments.of("<meter value='06'>60%</meter>", "progress-bar"),
                Arguments.of("<input type='range'/>", "slider")
                // TODO: Add other form controls once it is enabled from WebKit.
        );
    }

    private void printWithFormControl(final Runnable testBody, String element, String selector) {
        final ByteArrayOutputStream errStream = new ByteArrayOutputStream();

        System.setErr(new PrintStream(errStream));
        loadContent(String.format("<body>%s</body>", element));
        submit(testBody);
        System.setErr(ERR);

        final String exMessage = errStream.toString();
        assertFalse(exMessage.contains("Exception") || exMessage.contains("Error"),
                String.format("%s: Test failed with exception:\n%s", selector, exMessage));
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testRendering(String element, String selector) {
        final Runnable testBody = () -> {
            final WebPage page = WebEngineShim.getPage(getEngine());
            assertNotNull(page);
            WebPageShim.mockPrint(page, 0, 0, 800, 600);
            final Set<Node> elements = getView().lookupAll("." + selector);
            // Check whether control is added as a child of WebView.
            assertEquals(1, elements.size(),
                    String.format("%s control doesn't exist as child of WebView", selector));
            final Node node = (Node) elements.toArray()[0];
            // Check whether Node's styleClass contains the given selector.
            assertTrue(node.getStyleClass().contains(selector),
                    String.format("%s styleClass=%s is incorrect", node.getTypeSelector(), selector));
        };

        printWithFormControl(testBody, element, selector);
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testPrint(String element, String selector) {
        final Runnable testBody = () -> {
            final WebPage page = WebEngineShim.getPage(getEngine());
            assertNotNull(page);
            WebPageShim.mockPrint(page, 0, 0, 800, 600);
        };

        printWithFormControl(testBody, element, selector);
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testPrintByPageNumber(String element, String selector) {
        final Runnable testBody = () -> {
            final WebPage page = WebEngineShim.getPage(getEngine());
            assertNotNull(page);
            WebPageShim.mockPrintByPage(page, 0, 0, 0, 800, 600);
        };

        printWithFormControl(testBody, element, selector);
    }

    @AfterEach
    public void teardown() {
        System.setErr(ERR);
    }
}
