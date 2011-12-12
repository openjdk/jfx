/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Richard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Event {
    /**
     * A constant unlikely to be used in actual practice which is used to
     * indicate to the meta-data loading system that the value for the
     * Bean annotation value so specified should be computed with a reasonable
     * default value.
     */
    public static final String COMPUTE = "^^^^^COMPUTE^^^^^";

    /**
     * The displayName of the JavaFX Bean. If prefixed with %,
     * the name will be looked up via a resource bundle. The resource bundle
     * must either be named "resources" and located in the same package as the
     * JavaFX Bean, or it must be named after the JavaFX Bean with "Resources"
     * as the suffix.
     * <p>
     * If the displayName is set to <code>COMPUTE</code>, then the displayName
     * will be computed. First, any resource bundles defined for this JavaFX
     * Bean will be queried for an entry by first looking in the most specific
     * resource bundle ([BeanName]Resources) for an entry called "displayName",
     * and then looking in the generic "resources" bundle for an entry titled
     * "[BeanName]-displayName". Otherwise, the name will be computed
     * based on the name of the class.
     */
    String displayName() default COMPUTE;

    /**
     * A short description of the JavaFX Bean. If prefixed with %,
     * the short description will be looked up via a resource bundle. The
     * resource bundle must either be named "resources" and located in the same
     * package as the JavaFX Bean, or it must be named after the JavaFX Bean
     * with "Resources" as the suffix.
     * <p>
     * If the shortDescription is set to <code>COMPUTE</code>, then the
     * shortDescription will be computed. First, any resource bundles defined
     * for this JavaFX Bean will be queried for an entry by first looking in the
     * most specific resource bundle ([BeanName]Resources) for an entry called
     * "shortDescription", and then looking in the generic "resources" bundle
     * for an entry titled "[BeanName]-shortDescription". Otherwise, the
     * shortDescription will be computed and may be empty.
     */
    String shortDescription() default COMPUTE;

    /**
     * The category for this JavaFX Bean. Categories help to organize beans
     * within a tool palette or other system. This can be any String value.
     * If the category is set to <code>COMPUTE</code> then any resource bundles
     * defined for this JavaFX Bean will be queried for an entry by first
     * looking in the most specific resource bundle ([BeanName]Resources) for an
     * entry called "category", and then looking in the generic "resources"
     * bundle for an entry titled "[BeanName]-category". Otherwise, the category
     * will be set to some default value, which may be the same as the empty
     * string.
     */
    String category() default COMPUTE;

    /**
     * Allows the bean author to specify in the annotation what the type of the
     * event is passed to the annotated event handler property. Because of type
     * erasure, it is not possible for a framework reflecting on a JavaBean
     * to understand what this type would be, without some means of specifying
     * it. This annotation allows you to thus specify the type.
     *
     * @return The type of the event
     */
    Class<? extends javafx.event.Event> eventType() default javafx.event.Event.class;
}
