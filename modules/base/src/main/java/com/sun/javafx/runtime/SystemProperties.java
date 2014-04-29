/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.runtime;

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

public class  SystemProperties {
   /**
    * JavaFX System Properties table.
    * First column represents javafx property name with "javafx" prefix stripped off.
    * Second column represents underlying runtime platform equivalent. 
    * "jfx_specific" value in the runtime platform equivalent field indicates the property is JavaFX specific.
    * Empty string in   the runtime platform equivalent field indicates thete is no equivalent property for given platform.
    */
    private static final String[] sysprop_table = {
        /*"javafx.*/"application.codebase", "jfx_specific", /*"javafx.*/"debug", "javafx.debug"
    };


    /**
     * JavaFX Specific System Properties table.
     * First column represents javafx environment specific property name with "javafx" prefix stripped off.
     * Second column represents value of the property 
    */
    private static final String[] jfxprop_table = {
        /*"javafx.*/"application.codebase", "",
    };

    private static final Hashtable sysprop_list = new Hashtable();
    private static final Hashtable jfxprop_list = new Hashtable();

    private static final String versionResourceName =
            "/com/sun/javafx/runtime/resources/version.properties";

    private static boolean isDebug;

    static {
        AccessController.doPrivileged((PrivilegedAction) () -> {
            addProperties (sysprop_table, false);
            addProperties (jfxprop_table, true);
            setVersions();
            isDebug = "true".equalsIgnoreCase(getProperty("javafx.debug"));
            return null;
        });
    }

    /*
     * Populate our well known version strings
     */
    private static void setVersions() {
        int size;
        InputStream is =
                SystemProperties.class.getResourceAsStream(versionResourceName);
        try  {
            size = is.available();
        
            byte[] b = new byte[size];
            int n = is.read(b);            
            String inStr = new String(b, "utf-8");
            SystemProperties.setFXProperty("javafx.version",
                    getValue(inStr, "release="));

            SystemProperties.setFXProperty("javafx.runtime.version",
                    getValue(inStr, "full="));

        } catch (Exception ignore) {
        }
    }
    /*
     * Returns a value given a name
     */
    private static String getValue(String toSearch, String name) {
        String s = toSearch;
        int index;
        while ((index = s.indexOf(name)) != -1) {
            s = s.substring(index);
            if ((index = s.indexOf(0x0A))!= -1) {
                return (s.substring(name.length(), index)).trim();
            }
            return (s.substring(name.length(), s.length())).trim();
        }
        return "unknown";
    }
    /**
     * Registers a statically allocated System Properties table 
     * Once registered properties listed in the table are availabe for inquiry through FX.getProperty().
     * Table is defined as a String array with JavaFX property name followed by property value or property mapping identifier
     * depending on whether the table contains JavaFX specific properties or not.
     * Note that JavaFX property names have "javafx" stripped out to optimize table lookup.
     * The following identifiers are available:
     * </p>
     * 1. Underlying runtime platform property name. When listed, FX.getProperty() will invoke System.getProperty()
     *    method to retrieve property value.
     *    example:
     *    {"version", "java.version"}
     * </p>   
     * 2. "javafx_specific". When listed indicates there is no association between the property and underlying runtime
     *    platform. Rather the property is JavaFX specific. In that case another table needs to be provided with values
     *    for all JavaFX specific properties. JavaFX specific properties table is a string array containing property name
     *    and corresponding property value.
     *    example:
     *    {"hw.radio", "none"} 
     * </p>     
     * 3. Empty string. When listed, the meaning there is no association between the property and underlying runtime 
     *    platform nor the property is JavaFX specific. FX.getProperty() invoked on that property returns null.
     *    example:
     *    {"supports.mixing", "none"} 
     * @param table System Properties table
     * @param jfx_specific Indicates the table contains JavaFX specific properties
     */      
    public static void addProperties (String[] table, boolean jfx_specific) {
        if (table == null)
            return;

        Hashtable props;
                            
        if (jfx_specific) {
            props = jfxprop_list;
        } else {
            props = sysprop_list;
        }
                                  
        for (int i=0; i<table.length; i+=2) {
            props.put(table[i], table[i+1]);
        }
    } 

    public static String getProperty (String key) {
        Hashtable props = sysprop_list;
        final String prefix = "javafx.";

        if (key == null)
                return null;

        if (key.startsWith(prefix)) {
            key = key.substring(prefix.length());
        } else {
            return null;
        }
        final String found = (String)props.get(key);
        if ((found == null) || (found.equals(""))) {
        // No Java Runtime Environment property equivalent is found
            return null;
        }                        

                
        // Now check if the property is JFX specific and has no association with Runtime Environment
        if (found.equals("jfx_specific")) {
            props = jfxprop_list;
            return (String)props.get(key);
        } else {
            return System.getProperty(found);
        }
    }

   /*
    * Removes the property from JavaFX System Properties list 
    * @param key JavaFX System Property name
    */
    public static void clearProperty (String key) {
        if (key == null)
                return;

        Hashtable props = sysprop_list;
        final String prefix = "javafx.";
        
        // Remove "javafx." prefix from the key
        if (key.startsWith(prefix.toString())) {
            key = key.substring(prefix.length());
        } else {
            return;
        }

        String value = (String)props.get(key);
        if (value == null)
            return;

        props.remove(key);

        // Remove the prop from the JavaFX specific properties table if applicable
        if (value.equals("jfx_specific")) {
            props = jfxprop_list;
            props.remove(key);
        }
    }

    /**
     * Adds a new JavaFX specific property or modifyies existing property value.
     * Note that there is no method in this class to set underlying platform 
     * property as MIDP doesn't support System.setProperty() method.
     * @param key JavaFX Property name
     * @param value Property value
     * @throws NullPointerException if key or value is null
     */
    public static void setFXProperty (String key, final String value) {
        
        Hashtable props = sysprop_list;
        final String prefix = "javafx."; 
        
        // Remove "javafx." prefix from the key
        if (key.startsWith(prefix)) {
            key = key.substring(prefix.length());
       
           String k = (String)props.get(key);
           // Add new property to the list
           if (k == null) {
               props.put(key, "jfx_specific");
               props = jfxprop_list;
               props.put(key, value);
           } else if (k.equals("jfx_specific")) {
               // Change existing property value
               props = jfxprop_list;
               props.put(key, value);
               if (codebase.equals(prefix+key))
                   codebase_value = value;
           }
        } 
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static String getCodebase() {
        return codebase_value;
    }

    public static void setCodebase(String value) {
         if (value == null)
                value = "";
         codebase_value = value;
         setFXProperty("javafx.application.codebase", value);
    }

    private static String codebase_value;

    public static final String codebase = "javafx.application.codebase";
}

