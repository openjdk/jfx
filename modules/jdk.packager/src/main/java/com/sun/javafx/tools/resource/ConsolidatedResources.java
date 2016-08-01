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

package com.sun.javafx.tools.resource;

import java.io.File;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class ConsolidatedResources {
    private final SortedMap<ResourceKey, ResourceRecord> resourceMap;

    public ConsolidatedResources() {
        resourceMap = new TreeMap<ResourceKey, ResourceRecord>();
    }

    public void addResource(final PackagerResource resource) {
        addResource(resource, null);
    }

    public void addResource(final PackagerResource resource,
                            final ResourceFilter resourceFilter) {
        resource.traverse(new ResourceRegistration(resourceMap),
                          resourceFilter);
    }

    public boolean traverse(final ResourceTraversal resourceTraversal) {
        for (final Map.Entry<ResourceKey, ResourceRecord> mapEntry:
                resourceMap.entrySet()) {
            final ResourceRecord resourceRecord = mapEntry.getValue();
            if (!resourceTraversal.traverse(resourceRecord.getRootResource(),
                                            resourceRecord.getFile(),
                                            resourceRecord.getRelativePath())) {
                return false;
            }
        }

        return true;
    }

    private static final class ResourceRegistration
            implements ResourceTraversal {
        private final Map<ResourceKey, ResourceRecord> resourceMap;

        public ResourceRegistration(
                final Map<ResourceKey, ResourceRecord> resourceMap) {
            this.resourceMap = resourceMap;
        }

        public boolean traverse(final PackagerResource rootResource,
                                final File file,
                                final String relativePath) {
            resourceMap.put(file.isDirectory()
                                    ? ResourceKey.forDirectory(relativePath)
                                    : ResourceKey.forFile(relativePath),
                            new ResourceRecord(rootResource,
                                               file,
                                               relativePath));

            return true;
        }
    }

    private static final class ResourceRecord {
        private final PackagerResource rootResource;
        private final File file;
        private final String relativePath;

        public ResourceRecord(final PackagerResource rootResource,
                              final File file,
                              final String relativePath) {
            this.rootResource = rootResource;
            this.file = file;
            this.relativePath = relativePath;
        }

        public PackagerResource getRootResource() {
            return rootResource;
        }

        public File getFile() {
            return file;
        }

        public String getRelativePath() {
            return relativePath;
        }
    }

    private static final class ResourceKey implements Comparable<ResourceKey> {
        private final String directory;
        private final String fileName;

        private ResourceKey(final String directory,
                            final String fileName) {
            this.directory = directory;
            this.fileName = fileName;
        }

        public static ResourceKey forFile(final String relPath) {
            final int lastSlashIndex = relPath.lastIndexOf('/');
            if (lastSlashIndex == -1) {
                return new ResourceKey("", relPath);
            }

            return new ResourceKey(relPath.substring(0, lastSlashIndex),
                                   relPath.substring(lastSlashIndex + 1));
        }

        public static ResourceKey forDirectory(final String relPath) {
            return new ResourceKey(relPath, "");
        }

        public String getDirectory() {
            return directory;
        }

        public String getFileName() {
            return fileName;
        }

        public int compareTo(final ResourceKey otherKey) {
            final int dirResult = directory.compareTo(otherKey.directory);
            return (dirResult != 0) ? dirResult
                                    : fileName.compareTo(otherKey.fileName);
        }

        @Override
        public boolean equals(final Object other) {
            if (other == this) {
                return true;
            }

            if (!(other instanceof ResourceKey)) {
                return false;
            }

            return compareTo((ResourceKey) other) == 0;
        }

        @Override
        public int hashCode() {
            int hash = 7;

            hash = 97 * hash + directory.hashCode();
            hash = 97 * hash + fileName.hashCode();

            return hash;
        }
    }
}
