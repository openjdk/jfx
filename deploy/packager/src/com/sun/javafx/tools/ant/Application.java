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

package com.sun.javafx.tools.ant;

import com.sun.javafx.tools.packager.HtmlParam;
import com.sun.javafx.tools.packager.Param;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;

/**
 * Basic application descriptor.
 * <p>
 * Defines main components of application and default set of parameters.
 *
 *
 * Examples:
 * <pre>
 *    &lt;info vendor="Uncle Joe" description="Test program"/>
 * </pre>
 *
 * @ant.type name="application" category="javafx"
 */
public class Application extends DataType implements Cloneable {
    String mainClass = null;
    String preloaderClass = null;
    String name = null;
    List<Param> parameters = new LinkedList<Param>();
    List<HtmlParam> htmlParameters = new LinkedList<HtmlParam>();
    public List<Argument> arguments = new LinkedList<Argument>();
    String fallbackApp = null;
    String id = null;
    boolean embeddedIntoSwing = false;
    String version = null;

    public void setVersion(String v) {
        version = v;
    }

    public void setToolkit(String v) {
        embeddedIntoSwing = "swing".equalsIgnoreCase(v);
    }

    /**
     * Main class of AWT-based applet to be used if application fail to launch
     * due to missing FX runtime and installation of JavaFX is not possible.
     *
     * @ant.not-required
     */
    public void setFallbackClass(String v) {
        fallbackApp = v;
    }

    public void setName(String v) {
        name = v;
    }

    public Param createParam() {
        Param p = new Param();
        parameters.add(p);
        return p;
    }

    public void setParams(Properties props) {
        if (props != null) {
            for (Map.Entry en : props.entrySet()) {
                Param p = new Param();
                p.setName((String)en.getKey());
                p.setValue((String)en.getValue());
                parameters.add(p);
            }
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
        List<String> lst = new LinkedList();
        for(Argument a: arguments) {
            lst.add(a.value);
        }
        return lst;
    }

    public Object clone() {
        try {
            Application result = (Application) super.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }

    public HtmlParam createHtmlParam() {
        HtmlParam p = new HtmlParam();
        htmlParameters.add(p);
        return p;
    }

    /**
     * Application id that can be used to obtain Javascript reference to the application in HTML.
     * Same id can be also used to refer to application object in the ant task (using refid).
     *
     * @ant.not-required
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setRefid(Reference id) {
        this.id = id.getRefId();
        super.setRefid(id);
    }

    /**
     * Main application class.
     *
     * @ant.required
     */
    public void setMainClass(String v) {
        mainClass = v;
    }

    /**
     * Preloader class to be used.
     *
     * @ant.not-required Default is preloader shipped in JavaFX Runtime.
     */
    public void setPreloaderClass(String v) {
        preloaderClass = v;
    }

    //return instance that actually has data. Could be referenced object ...
    public Application get() {
        return isReference() ?
                (Application) getRefid().getReferencedObject() : this;
    }

    public void selfcheck() {
        if (get().mainClass == null) {
            throw new BuildException("Application main class is required.");
        }
    }

    @Override
    public String toString() {
        return "Application[id="+id+", mainClass="+mainClass+"]";
    }
}
