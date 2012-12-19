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

import com.sun.javafx.tools.packager.CommonParams;
import com.sun.javafx.tools.packager.DeployParams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import sun.misc.BASE64Encoder;

public final class Utils {
    private Utils() {
    }

    public static void addResources(CommonParams commonParams,
                                    FileSet fileset) {
        for (final Iterator i = fileset.iterator(); i.hasNext();) {
            FileResource fr = (FileResource) i.next();
            commonParams.addResource(fr.getBaseDir(), fr.getFile());
        }
    }

    public static void addResources(DeployParams deployParams,
                                    FileSet fileset,
                                    String type) {
        for (final Iterator i = fileset.iterator(); i.hasNext();) {
            FileResource fr = (FileResource) i.next();
            deployParams.addResource(fr.getBaseDir(), fr.getFile(), type);
        }
    }

    public static void addResources(DeployParams deployParams,
                                    com.sun.javafx.tools.ant.FileSet fileset) {
        for (final Iterator i = fileset.iterator(); i.hasNext();) {
            FileResource fr = (FileResource) i.next();
            deployParams.addResource(fr.getBaseDir(), fr.getFile(),
                    fileset.getMode(), fileset.getTypeAsString(),
                    fileset.getOs(), fileset.getArch());
        }
    }

    public static void readFully(InputStream is) throws IOException {
        byte buf[] = new byte[10000];
        while (is.read(buf) != -1) {}
    }

    public static void readAllFully(JarFile jf) throws IOException {
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = entries.nextElement();
            readFully(jf.getInputStream(je));
        }
    }

    public static byte[] getBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int n;
        while ((n = is.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    public static String getBase64Encoded(CertPath certPath)
            throws CertificateEncodingException
    {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(certPath.getEncoded());
    }
}
