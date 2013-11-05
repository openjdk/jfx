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

package com.oracle.ipack.packer;

import com.oracle.ipack.resources.CodeResources;
import com.oracle.ipack.resources.ResourceRules;
import com.oracle.ipack.util.DataCopier;
import com.oracle.ipack.util.HashingOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ResourcePacker {
    private final ZipOutputStream zipStream;
    private final File baseDir;
    private final String appPath;

    private final DataCopier dataCopier;
    private final ResourceRules resourceRules;
    private final HashingOutputStream dataStream;

    private byte[] codeResourcesHash;
    private byte[] infoPlistHash;

    ResourcePacker(final ZipOutputStream zipStream,
                   final File baseDir,
                   final String appPath,
                   final String appName) {
        this.zipStream = zipStream;
        this.baseDir = baseDir;
        this.appPath = appPath;

        dataCopier = new DataCopier();
        dataStream = new HashingOutputStream(zipStream);

        resourceRules = new ResourceRules();
        resourceRules.addExclude(appName, -1);
        resourceRules.addExclude("_CodeSignature", -1);
        resourceRules.addExclude("Info.plist", 10);
        resourceRules.addExclude("ResourceRules.plist", 100);
    }

    void execute() throws IOException {
        final CodeResources codeResources = new CodeResources(resourceRules);

        storeResourceFiles(codeResources);
        codeResourcesHash = storeCodeResources(codeResources);

        final String infoPlistName = appPath + "Info.plist";
        infoPlistHash =
                storeFileEntry(infoPlistName, new File(baseDir, infoPlistName));
    }

    byte[] getCodeResourcesHash() {
        return codeResourcesHash;
    }

    byte[] getInfoPlistHash() {
        return infoPlistHash;
    }

    private void storeResourceFiles(final CodeResources codeResources)
            throws IOException {
        final List<String> resources =
                resourceRules.collectResources(
                        new File(baseDir, appPath));

        for (final String resourceName: resources) {
            final String fullResourceName =
                    appPath + resourceName;
            if (resourceName.endsWith("/")) {
                storeDirEntry(fullResourceName);
                continue;
            }

            final byte[] resourceHash =
                    storeFileEntry(fullResourceName,
                                   new File(baseDir, fullResourceName));
            codeResources.addHashedResource(resourceName, resourceHash);
        }
    }

    private byte[] storeCodeResources(final CodeResources codeResources)
            throws IOException {
        storeDirEntry(appPath + "_CodeSignature/");

        final String codeResourcesName =
                appPath + "_CodeSignature/CodeResources";

        System.out.println("Adding " + codeResourcesName);
        zipStream.putNextEntry(new ZipEntry(codeResourcesName));
        try {
            codeResources.write(dataStream);
        } finally {
            dataStream.flush();
            zipStream.closeEntry();
        }

        return dataStream.calculateHash();
    }

    private void storeDirEntry(final String entryName) throws IOException {
        zipStream.putNextEntry(new ZipEntry(entryName));
        zipStream.closeEntry();
    }

    private byte[] storeFileEntry(final String entryName,
                                  final File file) throws IOException {
        System.out.println("Adding " + entryName);
        zipStream.putNextEntry(new ZipEntry(entryName));
        try {
            dataCopier.copyFile(dataStream, file);
        } finally {
            dataStream.flush();
            zipStream.closeEntry();
        }

        return dataStream.calculateHash();
    }
}
