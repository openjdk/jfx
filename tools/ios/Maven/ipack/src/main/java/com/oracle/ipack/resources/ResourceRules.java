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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ResourceRules {
    private final List<Exclude> excludes;
    private final Set<String> excludesLookup;

    public ResourceRules() {
        this.excludes = new ArrayList<Exclude>();
        this.excludesLookup = new HashSet<String>();
    }

    public void addExclude(final String name, final int weight) {
        excludes.add(new Exclude(name, weight));
        excludesLookup.add(name);
    }

    public List<Exclude> getExcludes() {
        return Collections.unmodifiableList(excludes);
    }

    public List<String> collectResources(final File baseDirectory) {
        final List<String> resources = new ArrayList<String>();
        collectResources(resources, baseDirectory, "");

        return Collections.unmodifiableList(resources);
    }

    private void collectResources(final List<String> resources,
                                  final File resourceFile,
                                  final String resourceName) {
        if (excludesLookup.contains(resourceName)) {
            return;
        }

        if (resourceFile.isDirectory()) {
            if (!resourceName.isEmpty()) {
                resources.add(resourceName + '/');
            }

            final String[] childNames = resourceFile.list();
            for (final String childName: childNames) {
                collectResources(resources,
                                 new File(resourceFile, childName),
                                 constructResourceName(
                                         resourceName, childName));
            }

            return;
        }

        if (resourceFile.isFile()) {
            resources.add(resourceName);
            return;
        }

        System.err.println("Skipping " + resourceName);
    }

    public static final class Exclude {
        private final String name;
        private final int weight;

        public Exclude(final String name, final int weight) {
            this.name = name;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }

        public int getWeight() {
            return weight;
        }
    }

    private static String constructResourceName(final String parentResource,
                                                final String childResource) {
        return parentResource.isEmpty()
                ? childResource
                : parentResource + '/' + childResource;
    }
}
