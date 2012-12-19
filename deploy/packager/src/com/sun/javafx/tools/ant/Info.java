/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.ant;

import com.sun.javafx.tools.packager.DeployParams.RunMode;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

/**
 * Application description for users. These details are shown in the system dialogs
 * (if they need to be shown).
 *
 * Examples:
 * <pre>
 *    &lt;info vendor="Uncle Joe" description="Test program">
 *       &lt;splash href="customsplash.gif>
 *    &lt;/info>
 * </pre>
 *
 * @ant.type name="info" category="javafx"
 */

public class Info extends DataType {
    String title;
    String vendor;
    String appDescription;
    String category;
    String licenseType;
    String copyright;
    List<Icon> icons = new LinkedList<Icon>();

    /**
     * Application category.
     * Category of application is platform specific.
     * Currently used by native bundlers only.
     *
     * In particular:
     *    Mac AppStore:
     *       http://developer.apple.com/library/mac/#releasenotes/General/SubmittingToMacAppStore/_index.html
     *    Linux (for desktop shortucts)
     *       http://standards.freedesktop.org/menu-spec/latest/ar01s03.html#desktop-entry-extensions-examples
     *
     * @ant.not-required
     */
    public void setCategory(String v) {
        category = v;
    }

    /**
     * Type of application license. Format is platform specific.
     * E.g. see Fedora guidelines - http://fedoraproject.org/wiki/Packaging:LicensingGuidelines
     *
     * Currently used by SOME of native bundlers only.
     *
     * @ant.not-required
     */
    public void setLicense(String v) {
        licenseType = v;
    }

    /**
     * Application copyright. Format is platform specific.
     * Currently used by SOME of native bundlers only.
     *
     * @ant.not-required
     */
    public void setCopyright(String v) {
        copyright = v;
    }

    /**
     * Title of the application
     *
     * @ant.required
     */
    public void setTitle(String v) {
        title = v;
    }

    /**
     * Provider of the application.
     *
     * @ant.required
     */
    public void setVendor(String v) {
        vendor = v;
    }

    /**
     * A short statement describing the application.
     *
     * @ant.required
     */
    public void setDescription(String v) {
        appDescription = v;
    }

    final static Set<String> iconTypes;

    static {
        iconTypes = new HashSet<String>() {
           {
               add("default");
               add("selected");
               add("disabled");
               add("rollover");
               add("splash");
               add("shortcut");
           }
        };
    }

    /**
      * Describes an icon that can be used to identify the application to the user.
      * Supported icon formats: gif, jpg, png, ico.
      */
    public class Icon extends DataType {
        String href;
        String kind;
        int width = UNDEFINED;
        int height = UNDEFINED;
        int depth = UNDEFINED;

        final static int UNDEFINED = -1;

        /**
         * A URL pointing to the icon file.
         *
         * @ant.required
         */
        public void setHref(String v) {
            href = v;
        }

        /**
         * Indicates the suggested use of the icon, can be:
         * default, selected, disabled, rollover, or shortcut.
         *
         */
        public void setKind(String v) {
            if (iconTypes.contains(v.toLowerCase())) {
                kind = v.toLowerCase();
            } else {
                StringBuffer msg = new StringBuffer("Usupported type of icon [");
                msg.append(v.toLowerCase());
                msg.append("]. Supported types: ");
                for (String s: iconTypes) {
                    msg.append("\""+s+"\" ");
                }

                throw new BuildException(msg.toString());
            }
        }

        /**
         * Can be used to indicate the width of the image.
         */
        public void setWidth(int v) {
            if (v > 0) {
                width = v;
            } else {
                throw new BuildException("Width must be positive number");
            }
        }

        /**
         * Can be used to indicate the height of the image.
         */
        public void setHeight(int v) {
            if (v > 0) {
                height = v;
            } else {
                throw new BuildException("Height must be positive number");
            }
        }

        /**
         * Can be used to indicate the resolution of the image.
         */
        public void setDepth(int v) {
            if (v > 0) {
                depth = v;
            } else {
                throw new BuildException("Depth must be positive number");
            }
        }
    }

    public Icon createIcon() {
        Icon ic = new Icon();
        icons.add(ic);
        return ic;
    }

    /**
     * Splash image to be shown on the application start.
     * In addition to icon properties can be also specific for particular
     * execution mode.
     */
    public class Splash extends Icon {
        RunMode mode;

        public Splash() {
            super();
            kind = "splash";
        }

        public void setKind() {
            throw new BuildException("Can not change kind of splash. Use icon instead.");
        }

        /**
         * Define execution mode to use splash for.
         * Supported modes are: "webstart", "embedded", "standalone" and "any".
         *
         * Default value is "webstart"
         */
        public void setMode(String v) {
            String l = v.toLowerCase();
            if ("webstart".equals(l)) {
                mode = RunMode.WEBSTART;
            } else if ("embedded".equals(l)) {
                mode = RunMode.EMBEDDED;
            } else if ("standalone".equals(l)) {
                mode = RunMode.STANDALONE;
            } else if ("any".equals(l)) {
                mode = RunMode.ALL;
            } else {
                throw new BuildException("Unsupported run mode: ["+v+"].");
            }
        }
    }

    public Splash createSplash() {
        Splash s = new Splash();
        icons.add(s);
        return s;
    }
}
