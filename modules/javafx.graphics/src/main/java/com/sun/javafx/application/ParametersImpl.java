/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Application.Parameters;

/**
 * Implementation class for Application parameters. This is called by the
 * application launcher to create the startup parameters for the
 * given class.
 */
public class ParametersImpl extends Parameters {

    private List<String> rawArgs = new ArrayList<String>();
    private Map<String, String> namedParams = new HashMap<String, String>();
    private List<String> unnamedParams = new ArrayList<String>();

    private List<String> readonlyRawArgs = null;
    private Map<String, String> readonlyNamedParams = null;
    private List<String> readonlyUnnamedParams = null;

    // Set of parameters for each application
    private static Map<Application, Parameters> params =
        new HashMap<Application, Parameters>();


    /**
     * Constructs an empty Parameters object.
     */
    public ParametersImpl() {
    }

    /**
     * Constructs an Parameters object from the specified list of arguments.
     * The list may be null.
     *
     * @param args list of command line arguments
     */
    public ParametersImpl(List<String> args) {
        if (args != null) {
            init(args);
        }
    }

    /**
     * Constructs an Parameters object from the specified array of unnamed
     * parameters. The array may be null.
     *
     * @param args array of command line arguments
     */
    public ParametersImpl(String[] args) {
        if (args != null) {
            init(Arrays.asList(args));
        }
    }

    /**
     * Constructs an Parameters object from the specified map of named
     * parameters.
     *
     * @param params a map of parameters from which to initialize this
     * object.
     */
    public ParametersImpl(Map params, String[] arguments) {
        init(params, arguments);
    }

    /**
     * Initialize this Parameters object from the set of command line arguments.
     * Null elements are elided.
     *
     * @param args list of command line arguments
     */
    private void init(List<String>args) {
        for (String arg: args) {
            if (arg != null) {
                rawArgs.add(arg);
            }
        }
        computeNamedParams();
        computeUnnamedParams();
    }

    /**
     * Constructs an Parameters object from the specified map of named
     * parameters.
     *
     * @param params a map of parameters from which to initialize this
     * object.
     */
    private void init(Map params, String[] arguments) {
        for (Object e : params.entrySet()) {
            Object key = ((Map.Entry)e).getKey();
            if (validKey(key)) {
                Object value = params.get(key);
                if (value instanceof String) {
                    namedParams.put((String)key, (String)value);
                }
            }
        }

        computeRawArgs();
        if (arguments != null) {
            for (String arg : arguments) {
                unnamedParams.add(arg);
                rawArgs.add(arg);
            }
        }
    }

    /**
     * Validate the first character of a key. It is valid if it is a letter or
     * an "_" character.
     *
     * @param c the first char of a key string
     *
     * @return whether or not it is valid
     */
    private boolean validFirstChar(char c) {
        return Character.isLetter(c) || c == '_';
    }

    /**
     * Validate the key. A key is valid if it is a String object that starts
     * with a letter or "_" character and does not contain an "=" character.
     *
     * @param key Object representing a potential key
     *
     * @return true if key is a valid key, otherwise false
     */
    private boolean validKey(Object key) {
        if (key instanceof String) {
            String keyStr = (String)key;
            if (keyStr.length() > 0 && keyStr.indexOf('=') < 0) {
                return validFirstChar(keyStr.charAt(0));
            }
        }

        return false;
    }

    /**
     * Returns true if the specified string is a named parameter of the
     * form: --name=value
     *
     * @param arg the string to check
     *
     * @return true if the string matches the pattern for a named parameter.
     */
    private boolean isNamedParam(String arg) {
        if (arg.startsWith("--")) {
            return (arg.indexOf('=') > 2 && validFirstChar(arg.charAt(2)));
        } else {
            return false;
        }
    }

    /**
     * This method computes the list of unnamed parameters, by filtering the
     * list of raw arguments, stripping out the named parameters.
     */
    private void computeUnnamedParams() {
        for (String arg : rawArgs) {
            if (!isNamedParam(arg)) {
                unnamedParams.add(arg);
            }
        }
    }

    /**
     * This method parses the current array of raw arguments looking for
     * name,value pairs. These name,value pairs are then added to the map
     * for this parameters object, and are of the form: --name=value.
     */
    private void computeNamedParams() {
        for (String arg : rawArgs) {
            if (isNamedParam(arg)) {
                final int eqIdx = arg.indexOf('=');
                String key = arg.substring(2, eqIdx);
                String value = arg.substring(eqIdx + 1);
                namedParams.put(key, value);
            }
        }
    }

    /**
     * This method creates string representations of the name,value pairs in
     * the map for this Parameters object, and appends those strings to the
     * raw arguments array. The newly added strings are of the form:
     * "--name=value".
     */
    private void computeRawArgs() {
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(namedParams.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            rawArgs.add("--" + key + "=" + namedParams.get(key));
        }
    }

    @Override public List<String> getRaw() {
        if (readonlyRawArgs == null) {
            readonlyRawArgs = Collections.unmodifiableList(rawArgs);
        }
        return readonlyRawArgs;
    }

    @Override public Map<String, String> getNamed() {
        if (readonlyNamedParams == null) {
            readonlyNamedParams = Collections.unmodifiableMap(namedParams);
        }
        return readonlyNamedParams;
    }

    @Override public List<String> getUnnamed() {
        if (readonlyUnnamedParams == null) {
            readonlyUnnamedParams = Collections.unmodifiableList(unnamedParams);
        }
        return readonlyUnnamedParams;
    }

    // Accessor methods

    public static Parameters getParameters(Application app) {
        Parameters p = params.get(app);
        return p;
    }

    public static void registerParameters(Application app, Parameters p) {
        params.put(app, p);
    }

}
