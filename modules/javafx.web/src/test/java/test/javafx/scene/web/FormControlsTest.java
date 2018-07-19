/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.WebPageShim;

import java.util.Set;
import javafx.scene.Node;
import javafx.scene.web.WebEngineShim;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class FormControlsTest extends TestBase {

    private void testControlRendering(final String html, final String selector) {
        loadContent(html);
        submit(() -> {
            // Render into null buffer.
            WebPageShim.renderContent(WebEngineShim.getPage(getEngine()), 0, 0, 800, 600);
            final Set<Node> elements = getView().lookupAll(selector);
            assertEquals(
                String.format("%s control doesn't exists as child of WebView", selector),
                1,
                elements.size());
        });
    }

    @Test public void testRadioButtonRendering() {
        testControlRendering("<input type='radio'/>", ".radio-button");
    }

    @Test public void testCheckboxRendering() {
        testControlRendering("<input type='checkbox'/>", ".check-box");
    }

    @Test public void testButtonRendering() {
        testControlRendering("<input type='button'/>", ".button");
    }

    @Test public void testTextFieldRendering() {
        testControlRendering("<input type='text'/>", ".text-field");
    }

    @Test public void testMeterRendering() {
        testControlRendering("<meter value='0.6'>60%</meter>", ".progress-bar");
    }

    @Test public void testSliderRendering() {
        testControlRendering("<input type='range'/>", ".slider");
    }
}

