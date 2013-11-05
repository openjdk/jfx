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

package com.oracle.ipack.resources;

import com.oracle.ipack.resources.ResourceRules.Exclude;
import com.oracle.ipack.util.Base64;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public final class CodeResources {
    private final ResourceRules resourceRules;
    private final List<HashedResource> hashedResources;

    public CodeResources(final ResourceRules resourceRules) {
        this.resourceRules = resourceRules;
        this.hashedResources = new ArrayList<HashedResource>();
    }

    public void addHashedResource(
            final String resourceName,
            final byte[] resourceHash) {
        hashedResources.add(
                new HashedResource(resourceName,
                                   Base64.byteArrayToBase64(resourceHash)));
    }

    public void write(final OutputStream os) throws IOException {
        final PrintWriter pw =
                new PrintWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8")));

        pw.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                      + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\""
                      + " \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                      + "<plist version=\"1.0\">\n"
                      + "<dict>\n"
                      + "\t<key>files</key>\n"
                      + "\t<dict>\n");

        for (final HashedResource hashedResource: hashedResources) {
            appendResource(pw, hashedResource);
        }

        pw.append("\t</dict>\n"
                      + "\t<key>rules</key>\n"
                      + "\t<dict>\n"
                      + "\t\t<key>.*</key>\n"
                      + "\t\t<true/>\n");

        for (final Exclude exclude: resourceRules.getExcludes()) {
            if (exclude.getWeight() >= 0) {
                appendExclude(pw, exclude);
            }
        }

        pw.append("\t</dict>\n"
                      + "</dict>\n"
                      + "</plist>\n");
        pw.flush();
    }

    private static void appendResource(final PrintWriter pw,
                                       final HashedResource hashedResource) {
        pw.append("\t\t<key>").append(hashedResource.getName())
                              .append("</key>\n")
          .append("\t\t<data>\n")
          .append("\t\t").append(hashedResource.getHash()).append('\n')
          .append("\t\t</data>\n");
    }

    private static void appendExclude(final PrintWriter pw,
                                      final Exclude exclude) {
        pw.append("\t\t<key>").append(exclude.getName()).append("</key>\n")
          .append("\t\t<dict>\n")
          .append("\t\t\t<key>omit</key>\n")
          .append("\t\t\t<true/>\n")
          .append("\t\t\t<key>weight</key>\n")
          .append("\t\t\t<real>");
        pw.print(exclude.getWeight());
        pw.append("</real>\n")
          .append("\t\t</dict>\n");
    }

    private static final class HashedResource {
        private final String name;
        private final String hash;

        public HashedResource(final String name, final String hash) {
            this.name = name;
            this.hash = hash;
        }

        public String getName() {
            return name;
        }

        public String getHash() {
            return hash;
        }
    }
}
