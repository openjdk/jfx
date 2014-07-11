/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import com.sun.javafx.Utils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.*;
import netscape.javascript.*;

public class ScreenAndWindowTest extends TestBase {

    // called on FX thread
    private void checkScreenProperties(Rectangle2D screenSize, Rectangle2D availSize) {
        JSObject screen = (JSObject) getEngine().executeScript("screen");
        int depth = (Integer) screen.getMember("colorDepth");
        int width = (Integer) screen.getMember("width");
        int height = (Integer) screen.getMember("height");
        int availWidth = (Integer) screen.getMember("availWidth");
        int availHeight = (Integer) screen.getMember("availHeight");

        assertEquals("screen.width", (int)screenSize.getWidth(), width);
        assertEquals("screen.height", (int)screenSize.getHeight(), height);
        assertEquals("screen.availWidth", (int)availSize.getWidth(), availWidth);
        assertEquals("screen.availHeight", (int)availSize.getHeight(), availHeight);
        
        // do some basic checking, too
        assertTrue("screen.depth >= 0", depth >= 0);
        assertTrue("screen.width >= screen.availWidth", width >= availWidth);
        assertTrue("screen.height >= screen.availHeight", height >= availHeight);
    }

    // called on FX thread
    private void checkWindowProperties(int windowWidth, int windowHeight) {
        JSObject window = (JSObject)getEngine().executeScript("window");
        int innerWidth = (Integer)window.getMember("innerWidth");
        int innerHeight = (Integer)window.getMember("innerHeight");
        int outerWidth = (Integer)window.getMember("outerWidth");
        int outerHeight = (Integer)window.getMember("outerHeight");

        if (windowWidth >= 0) {
            assertEquals("window.outerWidth", windowWidth, outerWidth);
        }
        if (windowHeight >= 0) {
            assertEquals("window.outerHeight", windowHeight, outerHeight);
        }
        
        // do some sanity checks
        assertTrue("window.outerWidth >= window.innerWidth", outerWidth >= innerWidth);
        assertTrue("window.outerHeight >= window.innerHeight", outerHeight >= innerHeight);
    }

    // called on FX thread
    private void checkProperties(Rectangle2D screenSize, Rectangle2D availSize,
                                int windowWidth, int windowHeight) {
        checkScreenProperties(screenSize, availSize);
        checkWindowProperties(windowWidth, windowHeight);
    }

    /**
     * Checks that no exceptions, crashes etc occur when accessing
     * screen.* and window.* properties from inside a WebEngine.
     */
    @Test public void test() throws InterruptedException {
        submit(new Runnable() { public void run() {
            Node view = getView();

            // test WebView not added to a Scene
            checkWindowProperties(-1, -1);

            // add WebView to a Scene with no Window
            Scene scene = new Scene(new Group(view));
            checkWindowProperties(-1, -1);

            // add Scene to a 0x0 Window
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setWidth(0);
            stage.setHeight(0);

            Screen screen = Utils.getScreen(view);
            Rectangle2D screenSize = screen.getBounds();
            Rectangle2D availSize = screen.getVisualBounds();

            checkProperties(screenSize, availSize, 0, 0);

            // resize the Window
            stage.setWidth(400);
            stage.setHeight(300);
            checkProperties(screenSize, availSize, 400, 300);
        }});
    }
}
