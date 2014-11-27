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

package com.oracle.tools.packager;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 
 * The class contains key-value pairs (elements) where keys are "displayable" keys
 * which the IDE can display/choose and values are "identifier" values which can be stored
 * in parameters' map.
 *
 * For instance the Mac has a predefined set of categories which can be applied
 * to LSApplicationCategoryType which is required for the mac app store.
 * 
 * The following example illustrates a simple usage of the MAC_CATEGORY parameter
 * 
 *     Set<String> keys = MAC_CATEGORY.getDisplayableKeys();
 *
 *     String key = getLastValue(keys); // get last value for example
 *
 *     String value = MAC_CATEGORY.getValueForDisplayableKey(key);
 *     params.put(MAC_CATEGORY.getID(), value);
 *
 */
public class EnumeratedBundlerParam<T> extends BundlerParamInfo<T> {
    //Not sure if this is the correct order, my idea is that from and IDE's perspective
    //the string to display to the user is the key and then the value is some type of
    //object (although probably a String in most cases)
    private Map<String, T> elements;
    private boolean strict;

    public EnumeratedBundlerParam(String name,
                                  String description,
                                  String id,
                                  Class<T> valueType,
                                  Function<Map<String, ? super Object>, T> defaultValueFunction,
                                  BiFunction<String, Map<String, ? super Object>, T> stringConverter,
                                  Map<String, T> elements,
                                  boolean strict) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.valueType = valueType;
        this.defaultValueFunction = defaultValueFunction;
        this.stringConverter = stringConverter;
        this.elements = elements;
        this.strict = strict;
    }

    public boolean isInPossibleValues(T value) {
        return elements.values().contains(value);
    }

    //Having the displayable values as the keys seems a bit wacky
    public Set<String> getDisplayableKeys() {
        return Collections.unmodifiableSet(elements.keySet());
    }

    // mapping from a "displayable" key to an "identifier" value.
    public T getValueForDisplayableKey(String displayableKey) {
        return elements.get(displayableKey);
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean isLoose() {
        return !isStrict();
    }

    public T validatedFetchFrom(Map<String, ? super Object> params)
            throws InvalidBundlerParamException {
        if (isStrict()) {
            T value = fetchFrom(params);
            if (!isInPossibleValues(value)) {
                throw new InvalidBundlerParamException("Parameter " + value.toString() + " not in valid set of values for BundlerParam " + name);
            }
            return value;
        }
        return fetchFrom(params);
    }

}
