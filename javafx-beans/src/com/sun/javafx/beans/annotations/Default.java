/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Specifies the default value for the property corresponding to a constructor
 * parameter in an immutable class. This allows the generated Builder class to
 * work correctly when no value is supplied for that parameter.
 * 
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface Default {
    /**
     * <p>
     * The default value for this parameter, specified as a string value that
     * can be used in an initializer. For example, it could be {@code "1.0"} for
     * a parameter of type {@code double}. An empty string can be used to
     * indicate the default value for the parameter of this type, so
     * {@code @Default("")} would indicate {@code 0.0} for a parameter of type
     * {@code double} or {@code null} for a parameter of type {@code String}.
     * </p>
     * 
     * <p>
     * Note: for parameters of type {@code String}, it is easy to forget the
     * quotes around a string literal. A string parameter whose default is the
     * empty string would be specified like this:<br>
     * {@code @Default("\"\"") name}.
     * </p>
     */
    public String value();
}
