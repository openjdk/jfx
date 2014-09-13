/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.types.DataType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.oracle.tools.packager.StandardBundlerParam.*;

/**
 *
 * Created by dferrin on 7/31/14.
 */
public class FileAssociation extends DataType implements DynamicAttribute {

    String extension;
    String mimeType;
    String description;
    File icon;

    List<DeployFXTask.BundleArgument> bundleArgumentList = new ArrayList<>();

    public DeployFXTask.BundleArgument createBundleArgument() {
        DeployFXTask.BundleArgument ba = new DeployFXTask.BundleArgument();
        bundleArgumentList.add(ba);
        return ba;
    }

    @Override
    public void setDynamicAttribute(String name, String value) throws BuildException {
        //Use qName and value - can't really validate anything until we know which bundlers we have, so this has
        //to done (way) downstream
        bundleArgumentList.add(new DeployFXTask.BundleArgument(name, value));
    }

    public Map<String, ? super Object> createLauncherMap() {
        Map<String, ? super Object> fileAssociations = new HashMap<>();

        putUnlessNull(fileAssociations, FA_EXTENSIONS.getID(), extension);
        putUnlessNull(fileAssociations, FA_CONTENT_TYPE.getID(), mimeType);
        putUnlessNull(fileAssociations, FA_DESCRIPTION.getID(), description);
        putUnlessNull(fileAssociations, FA_ICON.getID(), icon);

        for (DeployFXTask.BundleArgument ba : bundleArgumentList) {
            // TODO check and complain about collisions
            putUnlessNull(fileAssociations, ba.arg, ba.value);
        }

        return fileAssociations;
    }

    public void putUnlessNull(Map<String, ? super Object> params, String param, Object value) {
        if (value != null) {
            params.put(param, value);
        }
    }

    public void putUnlessNullOrEmpty(Map<String, ? super Object> params, String param, Collection value) {
        if (value != null && !value.isEmpty()) {
            params.put(param, value);
        }
    }

    public void putUnlessNullOrEmpty(Map<String, ? super Object> params, String param, Map value) {
        if (value != null && !value.isEmpty()) {
            params.put(param, value);
        }
    }

    /**
     * The file extension or extensions (separated by spaces) that the application requests it be registered to handle
     *
     * @ant.not-required
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * The mime-type that the application requests it be registered to handle.
     *
     * @ant.not-required
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * The description the Operation System may show for files of the associated extension and mime-type.
     *
     * @ant.optional
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The icon the Operation System may show for files of the associated extension and mime-type.
     *
     * @ant.optional
     */
    public void setIcon(File icon) {
        this.icon = icon;
    }

    /**
     * Extra arguments that the bundler may interpret to provide for better
     * integration with specific operating systems.
     *
     * @ant.optional
     */
    public DeployFXTask.BundleArgument createArg() {
        DeployFXTask.BundleArgument ba = new DeployFXTask.BundleArgument();
        bundleArgumentList.add(ba);
        return ba;
    }
}
