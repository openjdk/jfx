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

package com.oracle.bundlers;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BundlerParamInfo<T> {

    /**
     * The user friendly name of the parameter
     */
    String name;

    /**
     * A more verbose description of the parameter
     */
    String description;

    /**
     * The command line and hashmap name of the parameter
     */
    String id;

    /**
     * Type of the parameter.  Typically String.class
     */
    Class<T> valueType;

    /**
     * If the parameter is not set, what parameter the bundler will fall back on to use
     */
    String[] fallbackIDs;

    /**
     * If the value is not set, and no fallback value is found, the parameter uses the value returned by the producer.
     */
    Function<Map<String, ? super Object>, T> defaultValueFunction;

    /**
     * Does the parameter require the user or tool to set a value?  i.e. if the parameter is
     * not set will it cause the bundler to fail?
     */
    boolean requiresUserSetting;

    /**
     * An optional string converter for command line arguments.
     */
    BiFunction<String, Map<String, ? super Object>, T> stringConverter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getID() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Class<T> getValueType() {
        return valueType;
    }

    public void setValueType(Class<T> valueType) {
        this.valueType = valueType;
    }

    public String[] getFallbackIDs() {
        return fallbackIDs;
    }

    public void setFallbackIDs(String[] fallbackID) {
        this.fallbackIDs = fallbackID;
    }

    public Function<Map<String, ? super Object>, T> getDefaultValueFunction() {
        return defaultValueFunction;
    }

    public void setDefaultValueFunction(Function<Map<String, ? super Object>, T> defaultValueFunction) {
        this.defaultValueFunction = defaultValueFunction;
    }

    public boolean isRequiresUserSetting() {
        return requiresUserSetting;
    }

    public void setRequiresUserSetting(boolean requiresUserSetting) {
        this.requiresUserSetting = requiresUserSetting;
    }

    public BiFunction<String, Map<String, ? super Object>,T> getStringConverter() {
        return stringConverter;
    }

    public void setStringConverter(BiFunction<String, Map<String, ? super Object>, T> stringConverter) {
        this.stringConverter = stringConverter;
    }

    @SuppressWarnings("unchecked")
    public final T fetchFrom(Map<String, ? super Object> params) {
        Object o = params.get(getID());
        if (o instanceof String && getStringConverter() != null) {
            return getStringConverter().apply((String)o, params);
        }

        Class klass = getValueType();
        if (klass.isInstance(o)) {
            return (T) o;
        }
        if (o != null) {
            throw new IllegalArgumentException("Param " + getID() + " should be of type " + getValueType() + " but is a " + o.getClass());
        }
        if (params.containsKey(getID())) {
            // explicit nulls are allowed
            return null;
        }

        if (getFallbackIDs() != null) {
            for (String fallback: getFallbackIDs()) {
                o = params.get(fallback);
                if (klass.isInstance(o)) {
                    return (T) o;
                } else if (o instanceof String) {
                    return getStringConverter().apply((String)o, params);
                }
            }
        }

        if (getDefaultValueFunction() != null) {
            T result =  getDefaultValueFunction().apply(params);
            if (result != null) {
                params.put(getID(), result);
            }
            return result;
        }

        // ultimate fallback
        return null;
    }
}
