/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

/**
 * Marker interface for attached properties.
 * <p>
 * An attached property is {@linkplain ReadOnlyProperty#getDeclaringClass() declared} on one class, but associated
 * with instances of another class (referred to as the <em>target</em> class). It is usually used to describe an
 * aspect about the target <em>as it relates to</em> the declaring class. Property implementations that represent
 * attached properties must implement this interface to expose the class of objects the property can be attached to.
 * <p>
 * Note that the {@code AttachedProperty} interface might not be visible in the static type of the property, so
 * a dynamic test via {@code instanceof AttachedProperty} might be required at runtime to detect its presence.
 *
 * @since 27
 */
public interface AttachedProperty {

    /**
     * Returns the class of objects to which this property can be attached.
     *
     * @return the target class
     */
    Class<?> getTargetClass();
}
