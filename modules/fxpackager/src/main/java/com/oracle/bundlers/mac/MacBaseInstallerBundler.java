/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.bundlers.mac;

import com.oracle.bundlers.AbstractBundler;
import com.oracle.bundlers.BundlerParamInfo;
import com.oracle.bundlers.StandardBundlerParam;
import com.sun.javafx.tools.packager.bundlers.BundleType;
import com.sun.javafx.tools.packager.bundlers.ConfigException;
import com.sun.javafx.tools.packager.bundlers.MacAppBundler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

public abstract class MacBaseInstallerBundler extends AbstractBundler {

    //This could be generalized more to be for any type of Image Bundler
    protected final BundlerParamInfo<MacAppBundler> APP_BUNDLER = new StandardBundlerParam<>(
            "Mac App Bundler",
            "Creates a .app bundle for the Mac",
            "MacAppBundler",
            MacAppBundler.class,
            null,
            params -> new MacAppBundler(),
            false,
            s -> null);

    protected final BundlerParamInfo<File> APP_IMAGE_BUILD_ROOT = new StandardBundlerParam<>(
            "",
            "This is temporary location built by the packager that is the root of the image application",
            "appImageRoot", File.class, null,
            params -> {
                File imageDir = IMAGES_ROOT.fetchFrom(params);
                return new File(imageDir, getID()+ ".image");
            },
            false, File::new);



    public static final StandardBundlerParam<File> MAC_APP_IMAGE =
            new StandardBundlerParam<>(
                    "Image Directory",
                    "Location of the image that will be used to build either a DMG or PKG installer.",
                    "mac.app.image",
                    File.class,
                    null,
                    params -> null,
                    false,
                    File::new
            );



    protected final BundlerParamInfo<File> CONFIG_ROOT = new StandardBundlerParam<>(
            "", "", "configRoot", File.class, null,
            params -> {
                File imagesRoot = new File(StandardBundlerParam.BUILD_ROOT.fetchFrom(params), "macosx");
                imagesRoot.mkdirs();
                return imagesRoot;
            },
            false, s -> null);


    public static File getPredefinedImage(Map<String, ? super Object> p) throws ConfigException {
        File applicationImage = null;
        if (MAC_APP_IMAGE.fetchFrom(p) != null) {
            applicationImage = MAC_APP_IMAGE.fetchFrom(p);
            if (!applicationImage.exists()) {
                throw new ConfigException(
                        "Specified image directory " + MAC_APP_IMAGE.getID()+ ": " + applicationImage.toString() + " does not exists",
                        "Confirm that the value for " + MAC_APP_IMAGE.getID()+ " exists");
            }
        }
        return applicationImage;
    }

    protected boolean prepareAppBundle(Map<String, ? super Object> p) throws ConfigException {
        if (getPredefinedImage(p) != null) {
            return true;
        }

        File appImageRoot = APP_IMAGE_BUILD_ROOT.fetchFrom(p);
        File appDir = APP_BUNDLER.fetchFrom(p).doBundle(p, appImageRoot, true);
        return appDir != null;
    }


    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();

        results.addAll(MacAppBundler.getAppBundleParameters());
        results.addAll(Arrays.asList(
                APP_BUNDLER,
                CONFIG_ROOT,
                APP_IMAGE_BUILD_ROOT,
                MAC_APP_IMAGE
        ));

        return results;
    }

    @Override
    public BundleType getBundleType() {
        return BundleType.INSTALLER;
    }
}
