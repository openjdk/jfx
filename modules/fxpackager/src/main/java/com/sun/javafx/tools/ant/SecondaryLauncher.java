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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.oracle.tools.packager.StandardBundlerParam.*;

/**
 *
 * Created by dferrin on 7/31/14.
 */
public class SecondaryLauncher extends DataType implements DynamicAttribute {

    File icon;
    private String mainClass;
    private String name;
    private String version;
    private String title;
    private String vendor;
    private String appDescription;
    private String copyright;

    private Boolean menu;
    private Boolean shortcut;

    List<DeployFXTask.BundleArgument> bundleArgumentList = new ArrayList<>();
    private List<Argument> arguments = new ArrayList<>();
    private List<Platform.Property> properties = new ArrayList<>();
    private List<Platform.Jvmarg> jvmargs = new ArrayList<>();
    private List<Platform.Property> jvmUserArgs = new ArrayList<>();

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
        Map<String, ? super Object> bundleParams = new HashMap<>();

        putUnlessNull(bundleParams, MAIN_CLASS.getID(), mainClass);
//??        putUnlessNull(bundleParams, Preloader, preloaderClass);
//??        putUnlessNull(bundleParams, AppId, id);
        putUnlessNull(bundleParams, APP_NAME.getID(), name);
        putUnlessNull(bundleParams, VERSION.getID(), version);
//??        putUnlessNull(bundleParams, IDENTIFIER, id);
//??        putUnlessNull(bundleParams, ServiceHint, daemon);

        putUnlessNull(bundleParams, TITLE.getID(), title);
        putUnlessNull(bundleParams, VENDOR.getID(), vendor);
        putUnlessNull(bundleParams, DESCRIPTION.getID(), appDescription);
        putUnlessNull(bundleParams, COPYRIGHT.getID(), copyright);

        putUnlessNull(bundleParams, ICON.getID(), icon);

        putUnlessNull(bundleParams, SHORTCUT_HINT.getID(), shortcut);
        putUnlessNull(bundleParams, MENU_HINT.getID(), menu);


        Map<String, String> props = new HashMap<>();
        for (Platform.Property p: properties) {
            props.put(p.name, p.value);
        }
        putUnlessNullOrEmpty(bundleParams, JVM_PROPERTIES.getID(), props);

        List<String> args = new ArrayList<>();
        for (Platform.Jvmarg a: jvmargs) {
            args.add(a.value);
        }
        putUnlessNullOrEmpty(bundleParams, JVM_OPTIONS.getID(), args);

        Map<String, String> userArgs = new HashMap<>();
        for (Platform.Property a: jvmUserArgs) {
            userArgs.put(a.name, a.value);
        }
        putUnlessNullOrEmpty(bundleParams, USER_JVM_OPTIONS.getID(), userArgs);

        List<String> clargs = new ArrayList<>();
        for (Argument a: arguments) {
            clargs.add(a.value);
        }
        putUnlessNullOrEmpty(bundleParams, ARGUMENTS.getID(), clargs);


        for (DeployFXTask.BundleArgument ba : bundleArgumentList) {
            // TODO check and complain about collisions
            putUnlessNull(bundleParams, ba.arg, ba.value);
        }

        return bundleParams;
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

    public class Argument {
        String value;

        public void addText(String v) {
            value = getProject().replaceProperties(v);
        }
    }

    public Argument createArgument() {
        Argument a = new Argument();
        arguments.add(a);
        return a;
    }

    List<String> getArguments() {
        List<String> lst = new LinkedList<>();
        for(Argument a: arguments) {
            lst.add(a.value);
        }
        return lst;
    }

    public Platform.Property createProperty() {
        Platform.Property t = new Platform.Property();
        properties.add(t);
        return t;
    }

    public Platform.Jvmarg createJvmarg() {
        Platform.Jvmarg t = new Platform.Jvmarg();
        jvmargs.add(t);
        return t;
    }

    public Platform.Property createJVMUserArg() {
        Platform.Property t = new Platform.Property();
        jvmUserArgs.add(t);
        return t;
    }



    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getAppDescription() {
        return appDescription;
    }

    public void setAppDescription(String appDescription) {
        this.appDescription = appDescription;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public File getIcon() {
        return icon;
    }

    public void setIcon(File icon) {
        this.icon = icon;
    }

    public Boolean getMenu() {
        return menu;
    }

    public void setMenu(Boolean menu) {
        this.menu = menu;
    }

    public Boolean getShortcut() {
        return shortcut;
    }

    public void setShortcut(Boolean shortcut) {
        this.shortcut = shortcut;
    }
}
