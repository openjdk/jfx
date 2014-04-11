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

package com.sun.javafx.tools.packager.bundlers;

import static com.oracle.bundlers.StandardBundlerParam.*;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import com.oracle.bundlers.AbstractBundler;
import com.oracle.bundlers.BundlerParamInfo;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.windows.WinResources;

public class WinServiceBundler extends AbstractBundler {

    private static final ResourceBundle I18N = 
            ResourceBundle.getBundle("com.oracle.bundlers.windows.WinServiceBundler");
    
    private final static String EXECUTABLE_SVC_NAME = "WinLauncherSvc.exe";
    
    public WinServiceBundler() {
        super();
        baseResourceLoader = WinResources.class;
    }
    
    @Override
    public String getName() {
        return I18N.getString("bundler.name");
    }

    @Override
    public String getDescription() {
        return I18N.getString("bundler.description");
    }

    @Override
    public String getID() {
        return "windows.service";
    }

    @Override
    public String getBundleType() {
        return "IMAGE";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        return getServiceBundleParameters();
    }

    public static Collection<BundlerParamInfo<?>> getServiceBundleParameters() {
        return Arrays.asList(
                APP_NAME,
                BUILD_ROOT
        );
    }
    
    @Override
    public boolean validate(Map<String, ? super Object> params)
            throws UnsupportedPlatformException, ConfigException
    {
        try {
            if (params == null) throw new ConfigException(
                    I18N.getString("error.parameters-null"),
                    I18N.getString("error.parameters-null.advice"));

            return doValidate(params);
        } catch (RuntimeException re) {
            throw new ConfigException(re);
        }
    }

    boolean doValidate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().startsWith("win")) {
            throw new UnsupportedPlatformException();
        }

        if (WinResources.class.getResource(EXECUTABLE_SVC_NAME) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-windows-resources"),
                    I18N.getString("error.no-windows-resources.advice"));
        }
        
        return true;
    }
    
    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return doBundle(params, outputParentDir, false);
    }
    
    static String getAppName(Map<String, ? super Object>  p) {
        return APP_NAME.fetchFrom(p);
    }
    
    static String getAppSvcName(Map<String, ? super Object>  p) {
        return APP_NAME.fetchFrom(p) + "Svc";
    }
    
    public static File getLauncherSvc(File outDir, Map<String, ? super Object> p) {
        return new File(outDir, getAppName(p) + "Svc.exe");
    }
    
    /*
     * Copies the service launcher to the output folder
     * 
     * Note that the bundler doesn't create folder structure and
     * just copies the launcher to the output folder
     */
    File doBundle(Map<String, ? super Object> p, File outputDirectory, boolean dependentTask) {
        try {
            outputDirectory.mkdirs();

            if (!dependentTask) {
                Log.info(MessageFormat.format(I18N.getString("message.creating-service-bundle"), getAppSvcName(p), outputDirectory.getAbsolutePath()));
            }
            
            // Copy executable to install application as service
            File executableSvcFile = getLauncherSvc(outputDirectory, p);
            IOUtils.copyFromURL(
                    WinResources.class.getResource(EXECUTABLE_SVC_NAME),
                    executableSvcFile);
            executableSvcFile.setExecutable(true, false);
            
            if (!dependentTask) {
                Log.info(MessageFormat.format(I18N.getString("message.result-dir"), outputDirectory.getAbsolutePath()));
            }

            return outputDirectory;
        } catch (IOException ex) {
            Log.info("Exception: "+ex);
            Log.debug(ex);
            return null;
        }
        
    }

}
