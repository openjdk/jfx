/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.text;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Handles looking up embedded fonts, etc.
 * TODO needs to be rewritten with mobile in mind (they don't have Properties
 * class implementation)
 */
public class FontManager {
    private static FontManager instance;
    
    public static FontManager getInstance() {
        if (instance == null) instance = new FontManager();
        return instance;
    }
    
    private Properties map = new Properties();
    
    private FontManager() {
        loadEmbeddedFontDefinitions();
    }
    
    void loadEmbeddedFontDefinitions() {
        // locate the META-INF directory and search for a fonts.mf
        // located there
        URL u = FontManager.class.getResource("/META-INF/fonts.mf");
        if (u == null) return;
        
        // read in the contents of the file
        try {
            InputStream in = u.openStream();
            map.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Looks for an embedded font with the given full font name. If the font
     * is embedded, then the relative path to the font is returned If the font
     * is not embedded, then null is returned.
     * 
     * @param name
     * @return
     */
    public String findPathByName(String name) {
        return map.getProperty(name);
    }

    /**
     * @return Gets all the embedded path names that lead to fonts.
     */
    public String[] getAllPaths() {
        if (map.size() == 0) return new String[0];
        
        String[] paths = new String[map.size()];
        Enumeration values = map.elements();
        int index = 0;
        while (values.hasMoreElements()) {
            paths[index++] = (String)values.nextElement();
        }
        return paths;
    }
    
    public String[] getAllNames() {
        if (map.size() == 0) return new String[0];
        
        String[] names = new String[map.size()];
        Enumeration keys = map.keys();
        int index = 0;
        while (keys.hasMoreElements()) {
            names[index++] = (String)keys.nextElement();
        }
        return names;
    }
}
