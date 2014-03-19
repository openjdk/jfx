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
import com.sun.javafx.tools.packager.bundlers.UnsupportedPlatformException;

import java.io.File;
import java.text.MessageFormat;import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ResourceBundle;

import static com.oracle.bundlers.StandardBundlerParam.*;

public abstract class MacBaseInstallerBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.mac.MacBaseInstallerBundler");

    //This could be generalized more to be for any type of Image Bundler
    protected final BundlerParamInfo<MacAppBundler> APP_BUNDLER = new StandardBundlerParam<>(
            I18N.getString("param.app-bundler.name"),
            I18N.getString("param.app-bundle.description"),
            "mac.app.bundler",
            MacAppBundler.class,
            null,
            params -> new MacAppBundler(),
            false,
            (s, p) -> null);

    protected final BundlerParamInfo<File> APP_IMAGE_BUILD_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.app-image-build-root.name"),
            I18N.getString("param.app-image-build-root.description"),
            "mac.app.imageRoot",
            File.class,
            null,
            params -> {
                File imageDir = IMAGES_ROOT.fetchFrom(params);
                if (!imageDir.exists()) imageDir.mkdirs();
                return new File(imageDir, getID()+ ".image");
            },
            false,
            (s, p) -> new File(s));

    public static final StandardBundlerParam<File> MAC_APP_IMAGE = new StandardBundlerParam<>(
            I18N.getString("param.app-image.name"),
            I18N.getString("param.app-image.description"),
            "mac.app.image",
            File.class,
            null,
            params -> null,
            false,
            (s, p) -> new File(s));


    protected final BundlerParamInfo<MacDaemonBundler> DAEMON_BUNDLER = new StandardBundlerParam<>(
            I18N.getString("param.daemon-bundler.name"),
            I18N.getString("param.daemon-bundler.description"),
            "mac.daemon.bundler",
            MacDaemonBundler.class,
            null,
            params -> new MacDaemonBundler(),
            false,
            (s, p) -> null);


    protected final BundlerParamInfo<File> DAEMON_IMAGE_BUILD_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.daemon-image-build-root.name"),
            I18N.getString("param.daemon-image-build-root.description"),
            "mac.daemon.image",
            File.class,
            null,
            params -> {
                File imageDir = IMAGES_ROOT.fetchFrom(params);
                if (!imageDir.exists()) imageDir.mkdirs();
                return new File(imageDir, getID()+ ".daemon");
            },
            false,
            (s, p) -> new File(s));


    protected final BundlerParamInfo<File> CONFIG_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot",
            File.class,
            null,
            params -> {
                File imagesRoot = new File(BUILD_ROOT.fetchFrom(params), "macosx");
                imagesRoot.mkdirs();
                return imagesRoot;
            },
            false, (s, p) -> null);

    public static File getPredefinedImage(Map<String, ? super Object> p) {
        File applicationImage = null;
        if (MAC_APP_IMAGE.fetchFrom(p) != null) {
            applicationImage = MAC_APP_IMAGE.fetchFrom(p);
            if (!applicationImage.exists()) {
                throw new RuntimeException(
                        MessageFormat.format(I18N.getString("message.app-image-dir-does-not-exist"), MAC_APP_IMAGE.getID(), applicationImage.toString()));
            }
        }
        return applicationImage;
    }

    protected void validateAppImageAndBundeler(Map<String, ? super Object> params) throws ConfigException, UnsupportedPlatformException {
        if (MAC_APP_IMAGE.fetchFrom(params) != null) {
            File applicationImage = MAC_APP_IMAGE.fetchFrom(params);
            if (!applicationImage.exists()) {
                throw new ConfigException(
                        MessageFormat.format(I18N.getString("message.app-image-dir-does-not-exist"), MAC_APP_IMAGE.getID(), applicationImage.toString()),
                        MessageFormat.format(I18N.getString("message.app-image-dir-does-not-exist.advice"), MAC_APP_IMAGE.getID()));
            }
        } else {
            APP_BUNDLER.fetchFrom(params).doValidate(params);
        }
    }

    protected File prepareAppBundle(Map<String, ? super Object> p) {
        if (getPredefinedImage(p) != null) {
            return null;
        }

        File appImageRoot = APP_IMAGE_BUILD_ROOT.fetchFrom(p);
        return APP_BUNDLER.fetchFrom(p).doBundle(p, appImageRoot, true);
    }

    protected File prepareDaemonBundle(Map<String, ? super Object> p) throws ConfigException {
        File daemonImageRoot = DAEMON_IMAGE_BUILD_ROOT.fetchFrom(p);
        return DAEMON_BUNDLER.fetchFrom(p).doBundle(p, daemonImageRoot, true);        
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
