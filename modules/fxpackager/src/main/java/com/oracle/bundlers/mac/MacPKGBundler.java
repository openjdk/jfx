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

import com.oracle.bundlers.BundlerParamInfo;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.bundlers.ConfigException;
import com.sun.javafx.tools.packager.bundlers.IOUtils;
import com.sun.javafx.tools.packager.bundlers.UnsupportedPlatformException;

import java.io.*;
import java.util.Collection;
import java.util.Map;

import static com.oracle.bundlers.StandardBundlerParam.NAME;

public class MacPKGBundler extends MacBaseInstallerBundler {


    //@Override
    public File bundle(Map<String, ? super Object> p, File outdir) {
        Log.info("Building PKG package for " + NAME.fetchFrom(p));

        File appImageDir = APP_IMAGE_BUILD_ROOT.fetchFrom(p);
        try {
            appImageDir.mkdirs();
            prepareAppBundle(p);
            return createPKG(p, outdir);
        } catch (Exception ex) {
            return null;
        }
    }

    private File createPKG(Map<String, ? super Object> params, File outdir) {
        //generic find attempt
        try {
            String appLocation =
                    APP_IMAGE_BUILD_ROOT.fetchFrom(params) + "/" + NAME.fetchFrom(params) + ".app";
            File predefinedImage = getPredefinedImage(params);
            if (predefinedImage != null) {
                appLocation = predefinedImage.getAbsolutePath();
            }

            //productbuild --component Smoke.app /Applications  --product Smoke.app/Contents/Info.plist Smoke.pkg
            File finalPKG = new File(outdir, NAME.fetchFrom(params)+".pkg");
            outdir.mkdirs();

            ProcessBuilder pb = new ProcessBuilder("productbuild",
                    "--component",
                    appLocation,
                    "/Applications",
                    "--product",
                    appLocation+"/Contents/Info.plist",
                    finalPKG.getAbsolutePath());
            IOUtils.exec(pb, verbose);
            return finalPKG;
        } catch (Exception ignored) {
            Log.debug("PKG Failed: " + ignored.getMessage());
        }
        return null;
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Implement Bundler
    //////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return "PKG Installer";
    }

    @Override
    public String getDescription() {
        return "Mac PKG Installer Bundle.";
    }

    @Override
    public String getID() {
        return "pkg";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        //Add PKG Specific parameters as required
        return super.getBundleParameters();
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        if (params == null) throw new ConfigException("Parameters map is null.", "Pass in a non-null parameters map.");

        // hdiutil is always available so there's no need to test for availability.
        //run basic validation to ensure requirements are met

        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        APP_BUNDLER.fetchFrom(params).doValidate(params);
        return true;
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }

}
