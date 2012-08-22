/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.skin.LabelSkin;
import com.sun.javafx.scene.control.skin.caspian.Caspian;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 
 * Class to load caspian User Agent Stylesheet when either a control or a 
 * popup control is first created. This is a package private class intended for 
 * internal use only.
 */
class UAStylesheetLoader {
    
    private static class Holder {
        private static UAStylesheetLoader stylesheetLoader = new UAStylesheetLoader();
    }
    
    private static boolean stylesheetLoaded = false;
    
    private UAStylesheetLoader() {}
    
    
    static void doLoad() {
        Holder.stylesheetLoader.loadUAStylesheet();
    }
    
    private void loadUAStylesheet() {
        // Ensures that the caspian.css file is set as the user agent style sheet
        // when the first control or popupcontrol is created.
        if (!stylesheetLoaded) {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    // Old approach:
                    URL caspianUrl = LabelSkin.class.getResource("caspian/caspian.css");
                    StyleManager.addUserAgentStylesheet(caspianUrl.toExternalForm());
                    
                    // New approach:
//                    StyleManager.setDefaultUserAgentStylesheet(new Caspian());
                        
                    if (com.sun.javafx.PlatformUtil.isEmbedded()) {
                        URL embeddedUrl = LabelSkin.class.getResource("caspian/embedded.css");
                        StyleManager.addUserAgentStylesheet(embeddedUrl.toExternalForm());

                        if (com.sun.javafx.Utils.isQVGAScreen()) {
                            URL qvgaUrl = LabelSkin.class.getResource("caspian/embedded-qvga.css");
                            StyleManager.addUserAgentStylesheet(qvgaUrl.toExternalForm());
                        }
                    }

                    stylesheetLoaded = true;
                    return null;
                }
            });    
        }
    }
}
