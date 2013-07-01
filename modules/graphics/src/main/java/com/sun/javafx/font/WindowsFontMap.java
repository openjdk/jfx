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

package com.sun.javafx.font;

import java.util.HashMap;

class WindowsFontMap {

    static class FamilyDescription {
        public String familyName;
        public String plainFullName;
        public String boldFullName;
        public String italicFullName;
        public String boldItalicFullName;
        public String plainFileName;
        public String boldFileName;
        public String italicFileName;
        public String boldItalicFileName;
    }

    static HashMap<String, FamilyDescription> platformFontMap;

    /**
     * populate the map with the most common windows fonts.
     */
    static HashMap<String, FamilyDescription> populateHardcodedFileNameMap() {

        HashMap<String, FamilyDescription> platformFontMap
            = new HashMap<String, FamilyDescription>();

        FamilyDescription fd;
        fd = new FamilyDescription();
        fd.familyName = "Segoe UI";
        fd.plainFullName = "Segoe UI";
        fd.plainFileName = "segoeui.ttf";
        fd.boldFullName = "Segoe UI Bold";
        fd.boldFileName = "segoeuib.ttf";
        fd.italicFullName = "Segoe UI Italic";
        fd.italicFileName = "segoeuii.ttf";
        fd.boldItalicFullName = "Segoe UI Bold Italic";
        fd.boldItalicFileName = "segoeuiz.ttf";
        platformFontMap.put("segoe", fd);

        fd = new FamilyDescription();
        fd.familyName = "Tahoma";
        fd.plainFullName = "Tahoma";
        fd.plainFileName = "tahoma.ttf";
        fd.boldFullName = "Tahoma Bold";
        fd.boldFileName = "tahomabd.ttf";
        platformFontMap.put("tahoma", fd);

        fd = new FamilyDescription();
        fd.familyName = "Verdana";
        fd.plainFullName = "Verdana";
        fd.plainFileName = "verdana.TTF";
        fd.boldFullName = "Verdana Bold";
        fd.boldFileName = "verdanab.TTF";
        fd.italicFullName = "Verdana Italic";
        fd.italicFileName = "verdanai.TTF";
        fd.boldItalicFullName = "Verdana Bold Italic";
        fd.boldItalicFileName = "verdanaz.TTF";
        platformFontMap.put("verdana", fd);

        fd = new FamilyDescription();
        fd.familyName = "Arial";
        fd.plainFullName = "Arial";
        fd.plainFileName = "ARIAL.TTF";
        fd.boldFullName = "Arial Bold";
        fd.boldFileName = "ARIALBD.TTF";
        fd.italicFullName = "Arial Italic";
        fd.italicFileName = "ARIALI.TTF";
        fd.boldItalicFullName = "Arial Bold Italic";
        fd.boldItalicFileName = "ARIALBI.TTF";
        platformFontMap.put("arial", fd);

        fd = new FamilyDescription();
        fd.familyName = "Times New Roman";
        fd.plainFullName = "Times New Roman";
        fd.plainFileName = "times.ttf";
        fd.boldFullName = "Times New Roman Bold";
        fd.boldFileName = "timesbd.ttf";
        fd.italicFullName = "Times New Roman Italic";
        fd.italicFileName = "timesi.ttf";
        fd.boldItalicFullName = "Times New Roman Bold Italic";
        fd.boldItalicFileName = "timesbi.ttf";
        platformFontMap.put("times", fd);


        fd = new FamilyDescription();
        fd.familyName = "Courier New";
        fd.plainFullName = "Courier New";
        fd.plainFileName = "cour.ttf";
        fd.boldFullName = "Courier New Bold";
        fd.boldFileName = "courbd.ttf";
        fd.italicFullName = "Courier New Italic";
        fd.italicFileName = "couri.ttf";
        fd.boldItalicFullName = "Courier New Bold Italic";
        fd.boldItalicFileName = "courbi.ttf";
        platformFontMap.put("courier", fd);

        return platformFontMap;
    }

    static String getPathName(String filename) {
        return PrismFontFactory.getPathNameWindows(filename);
    }

    /* If style is > 0 then name must be a family name */
    // REMIND REMIND REMIND .. Need to flag that these are PLATFORM
    // fonts else they are not being tagged for LCD.
    // Also the lookup via GDI still happens in order to make that
    // determination so we lose the perf. gain. Need to make the
    // is it a platform font call see if it came from here first.
    static String findFontFile(String lcName, int style) {
        if (platformFontMap == null) {
            platformFontMap = populateHardcodedFileNameMap();
        }

        if (platformFontMap == null || platformFontMap.size() == 0) {
            return null;
        }

        int spaceIndex = lcName.indexOf(' ');
        String firstWord = lcName;
        if (spaceIndex > 0) {
            firstWord = lcName.substring(0, spaceIndex);
        }

        FamilyDescription fd = platformFontMap.get(firstWord);
        if (fd == null) {
            return null;
        }
        /* Once we've established that its at least the first word,
         * we need to dig deeper to make sure its a match for either
         * a full name, or the family name, to make sure its not
         * a request for some other font that just happens to start
         * with the same first word.
         * If you request a full name such as 'Tahoma Italic',
         * which doesn't exist, we bail on you.
         * But if you request Tahoma + the STYLE italic, we'll do our best
         */

        String file = null;

        if (style < 0) { /* we expect a full name */
            if (lcName.equalsIgnoreCase(fd.plainFullName)) {
                file = fd.plainFileName;
            } else if (lcName.equalsIgnoreCase(fd.boldFullName)) {
                file = fd.boldFileName;
            } else if (lcName.equalsIgnoreCase(fd.italicFullName)) {
                file = fd.italicFileName;
            } else if (lcName.equalsIgnoreCase(fd.boldItalicFullName)) {
                file = fd.boldItalicFileName;
            }
            if (file != null) {
                return getPathName(file);
            } else {
                return null;
            }
        } else if (!lcName.equalsIgnoreCase(fd.familyName)) {
            return null;
        }

        /* If we get here its a family name + style */

        switch (style) {
            case 0 :
                file = fd.plainFileName;
                break;
            case 1 :
                file = fd.boldFileName;
                if (file == null) {
                    file = fd.plainFileName;
                }
                break;
            case 2 :
                file = fd.italicFileName;
                if (file == null) {
                    file = fd.plainFileName;
                }
                break;
            case 3:
                file = fd.boldItalicFileName;
                if (file == null) {
                    file = fd.italicFileName;
                }
                if (file == null) {
                    file = fd.boldFileName;
                }
                if (file == null) {
                    file = fd.plainFileName;
                }
                break;
        }

        if (file != null) {
            return getPathName(file);
        } else {
            return null;
        }
    }
}
