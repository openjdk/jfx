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

package com.sun.javafx.css;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;


public class StyleConverterTest {

    public StyleConverterTest() {
    }

    private ClassLoader loader;
    Class converterClass;
    List<Class> converterClassList = new ArrayList<Class>();

    private void findStyleConverters(File dir, String pkgPrefix) {

        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                String name = files[i].getName();
                if (! name.endsWith("class")) continue;
                try {
                    // strip .class
                    name = (name.substring(0, name.length()-6));
                    name = (pkgPrefix != null) ? pkgPrefix.concat(".").concat(name) : name;
                    Class cl = Class.forName(name, false, loader);
                    if (converterClass.isAssignableFrom(cl)) converterClassList.add(cl);
                } catch (Exception any) {
                    System.out.println(any.toString());
                }
            } else if (files[i].isDirectory()) {
                String pkg =
                    (pkgPrefix != null) ? pkgPrefix.concat(".").concat(files[i].getName()) : files[i].getName();
                findStyleConverters(files[i], pkg);
            }
        }
    }

    @Test
    public void testGetInstance_Class_ForAllInstancesOfStyleConverter() {
        try {
            converterClass = Class.forName("com.sun.javafx.css.StyleConverter");
            loader = converterClass.getClassLoader();
            URL url = converterClass.getClassLoader().getResource("");
            if ("file".equals(url.getProtocol())) {
                String path = url.getPath();
                File dir = new File(path);
                findStyleConverters(dir, null);

                assertFalse(converterClassList.isEmpty());

                for(Class cl : converterClassList) {
                    if (cl == com.sun.javafx.css.converters.EnumConverter.class) continue;
                    if (cl.isAnonymousClass()) continue;
                    StyleConverter result = StyleConverter.getInstance(cl);
                    assertNotNull(cl.getName(), result);
                }
            }
        } catch(Exception any) {
            fail(any.toString());
        }
    }
}