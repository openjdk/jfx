/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application;

import java.io.File;
import java.net.URI;
import javafx.application.Application;

public abstract class HostServicesDelegate {

    public static HostServicesDelegate getInstance(final Application app) {
        return StandaloneHostService.getInstance(app);
    }

    protected HostServicesDelegate() {
    }

    public abstract String getCodeBase();

    public abstract String getDocumentBase();

    public abstract void showDocument(String uri);

    // StandaloneHostService implementation
    private static class StandaloneHostService extends HostServicesDelegate {

        private static HostServicesDelegate instance = null;

        private Class appClass = null;

        public static HostServicesDelegate getInstance(Application app) {
            synchronized (StandaloneHostService.class) {
                if (instance == null) {
                    instance = new StandaloneHostService(app);
                }
                return instance;
            }
        }

        private StandaloneHostService(Application app) {
             appClass = app.getClass();
        }

        @Override
        public String getCodeBase() {
            // If the application was launched in standalone mode, this method
            // returns the directory containing the application jar file.
            // If the application is not packaged in a jar file, this method
            // returns the empty string.
            String theClassFile = appClass.getName();
            int idx = theClassFile.lastIndexOf(".");
            if (idx >= 0) {
                // Strip off package name prefix in class name if exists
                // getResoruce will automatically add in package name during
                // lookup; see Class.getResource javadoc for more details
                theClassFile = theClassFile.substring(idx + 1);
            }
            theClassFile = theClassFile + ".class";

            String classUrlString = appClass.getResource(theClassFile).toString();
            if (!classUrlString.startsWith("jar:file:") ||
                    classUrlString.indexOf("!") == -1) {
                return "";
            }
            // Strip out the "jar:" and everything after and including the "!"
            String urlString = classUrlString.substring(4,
                    classUrlString.lastIndexOf("!"));
            File jarFile = null;
            try {
                jarFile = new File(new URI(urlString).getPath());
            } catch (Exception e) {
                // should not happen
            }
            if (jarFile != null) {
                String codebase = jarFile.getParent();
                if (codebase != null) {
                    return toURIString(codebase);
                }
            }

            return "";
        }

        private String toURIString(String filePath) {
            try {
                return new File(filePath).toURI().toString();
            } catch (Exception e) {
                // should not happen
                // dump stack for debug purpose
                e.printStackTrace();
            }
            return "";
        }

        @Override public String getDocumentBase() {
            // If the application was launched in standalone mode,
            // this method returns the URI of the current directory.
            return toURIString(System.getProperty("user.dir"));
        }

        static final String[] browsers = {
                "xdg-open",
                "google-chrome",
                "firefox",
                "opera",
                "konqueror",
                "mozilla"
        };

        @Override
        public void showDocument(final String uri) {
            String osName = System.getProperty("os.name");
            try {
                if (osName.startsWith("Mac OS")) {
                    Runtime.getRuntime().exec(
                            "open " + uri);
                } else if (osName.startsWith("Windows")) {
                    Runtime.getRuntime().exec(
                            "rundll32 url.dll,FileProtocolHandler " + uri);
                } else { //assume Unix or Linux
                    String browser = null;
                    for (String b : browsers) {
                        if (browser == null && Runtime.getRuntime().exec(
                                new String[]{"which", b}).getInputStream().read() != -1) {
                            Runtime.getRuntime().exec(new String[]{browser = b, uri});
                        }
                    }
                    if (browser == null) {
                        throw new Exception("No web browser found");
                    }
                }
            } catch (Exception e) {
                // should not happen
                // dump stack for debug purpose
                e.printStackTrace();
            }
        }
    }
}
