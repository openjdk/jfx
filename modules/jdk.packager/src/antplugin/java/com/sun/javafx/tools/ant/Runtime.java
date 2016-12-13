/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.DynamicElement;
import org.apache.tools.ant.DynamicAttribute;


public class Runtime extends DataType implements Cloneable, DynamicElement, DynamicAttribute {
    private List<Argument> addModules = new LinkedList<>();
    private List<Argument> limitModules = new LinkedList<>();
    private List<Argument> modulePath = new LinkedList<>();
    private Boolean stripNativeCommands;
    private Boolean detectModules;

    public class Argument {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    private List<String> processArguments(List<Argument> args, String pattern) {
        List<String> result = new LinkedList();

        for (Argument a: args) {
            for (String s : a.getValue().split(pattern)) {
                result.add(s);
            }
        }

        return result;
    }

    /**
     * "addMod" declaration for the application's runtime.
     *
     * Modules can be specified per-element, or comma/colon/semi-colon/space separated
     *
     * @ant.not-required Default is to bundle the whole platform
     */
    private Argument createAddModules() {
        Argument a = new Argument();
        this.addModules.add(a);
        return a;
    }

    /**
     * "addMod" declaration for the application's runtime
     *
     * @ant.not-required Default is to bundle the whole platform
     */
    List<String> getAddModules() {
        return processArguments(this.addModules, "[,;: ]+");
    }

    /**
     * "limitMod" declaration for the application's runtime.
     *
     * Modules can be specified per-element, or comma/colon/semi-colon/space separated
     *
     * @ant.not-required Default is to bundle the whole platform
     */
    private Argument createLimitModules() {
        Argument a = new Argument();
        this.limitModules.add(a);
        return a;
    }

    /**
     * "limitMod" declaration for the application's runtime
     *
     * @ant.not-required Default is to bundle the whole platform
     */
    List<String> getLimitModules() {
        return processArguments(this.limitModules, "[,;: ]+");
    }

    /**
     * "modulePath" declaration for the application's runtime.
     *
     * Modules can be specified per-element, or colon/semi-colon separated
     *
     * @ant.not-required Default is to bundle the whole platform
     */
    private Argument createModulePath() {
        Argument a = new Argument();
        this.modulePath.add(a);
        return a;
    }

    /**
     *
     */
    public String getModulePath() {
        String result = "";
        List<String> paths = processArguments(this.modulePath, "["+File.pathSeparator+"]+");

        for (String s : paths) {
            if (!result.isEmpty()) {
                result += File.pathSeparator;
            }

            result += s;
        }

        return result;
    }

    /**
     * Whether or not the bundler should remove native commands.
     */
    public Boolean getStripNativeCommands() {
        return this.stripNativeCommands;
    }

    /**
     * Whether or not the bundler should remove native commands.
     * @ant.not-required default is true
     */
    public void setStripNativeCommands(boolean value) {
        this.stripNativeCommands = value;
    }

    /**
     * Whether or not the bundler should attempt to detect and add used modules.
     */
    public Boolean getDetectModules() {
        return this.detectModules;
    }

    /**
     * Whether or not the bundler should attempt to detect and add used modules.
     * @ant.not-required default is false. This is experimental.
     */
    public void setDetectModules(boolean value) {
        this.detectModules = value;
    }

    public Object clone() {
        try {
            Application result = (Application) super.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }

    public Runtime get() {
        return isReference() ?
                (Runtime) getRefid().getReferencedObject() : this;
    }

    @Override
    public Object createDynamicElement(String name) {
        if (name.equals("add-modules")) {
            return createAddModules();
        }
        else if (name.equals("limit-modules")) {
            return createLimitModules();
        }
        else if (name.equals("module-path")) {
            return createModulePath();
        }

        return null;
    }

    public void setDynamicAttribute(String name, String value) {
        if (name.equals("strip-native-commands")) {
            this.stripNativeCommands = Boolean.valueOf(value);
        }
        else if (name.equals("detect-modules")) {
            this.detectModules = Boolean.valueOf(value);
        }
    }
}
