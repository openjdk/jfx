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

package com.sun.javafx.tools.ant;

import com.sun.javafx.tools.resource.PackagerResource;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Collection of resources used by application.
 * Defined as set of JavaFX FileSet's. Could be reused using id/refid.
 *
 * In the example below both rx:resource elements define collection consisting
 * of s.jar in the dist directory:
 * <pre>
 *    &lt;fx:resource id="aaa">
 *       &lt;fx:fileset dir="dist" includes="s.jar"/>
 *    &lt;/fx:resource>
 *
 *    &lt;fx:resource refid="aaa"/>
 * </pre>
 *
 * @ant.type name="resources" category="javafx"
 */
public class Resources extends DataType {
   private List<com.sun.javafx.tools.ant.FileSet> rsets =
           new LinkedList<com.sun.javafx.tools.ant.FileSet>();

    private Resources get() {
        if (isReference()) {
            return (Resources) getRefid().getReferencedObject();
        }
        return this;
    }

    public List<com.sun.javafx.tools.ant.FileSet> getResources() {
        return get().rsets;
    }

    public com.sun.javafx.tools.ant.FileSet createFileSet() {
        com.sun.javafx.tools.ant.FileSet r = new com.sun.javafx.tools.ant.FileSet();
        rsets.add(r);
        return r;
    }

    //not really accurate as platfrom specific resources are not handled
    public String exportAsClassPath() {
        StringBuffer sb = new StringBuffer();

        List<String> jars = new LinkedList<String>();
        getJars("progress", jars);
        getJars("eager", jars);
        getJars("lazy", jars);

        for (String s : jars) {
            if (sb.length() != 0) {
                sb.append(" "); //jars to be separated by space
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private void getJars(String mode, List<String> lst) {
        for (com.sun.javafx.tools.ant.FileSet r : getResources()) {
            if (mode.equals(r.getMode())) {
                for (final Iterator i = r.iterator(); i.hasNext();) {
                        FileResource fr = (FileResource) i.next();
                        PackagerResource p = new PackagerResource(fr.getBaseDir(),
                                fr.getFile());
                        lst.add(p.getRelativePath());
                }
            }
        }
    }
}
