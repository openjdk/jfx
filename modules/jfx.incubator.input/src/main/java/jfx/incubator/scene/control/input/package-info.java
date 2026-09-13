/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>Incubates a customization mechanism for the JavaFX Controls utilizing the
 * {@link jfx.incubator.scene.control.input.InputMap InputMap}.
 * <p>
 * The {@code InputMap}
 * <ul>
 * <li>allows for customization of a control behavior by changing the existing or adding new key mappings
 * <li>supports dynamic modification of the key mappings
 * <li>allows for accessing the default functionality even when it was overwritten by the application
 * <li>allows for reverting customization to the default implementation
 * <li>guarantees priorities between the application and the skin event handlers and key mappings
 * <li>allows for gradual migration of the existing controls to use the InputMap
 * </ul>
 * <p>
 * <b><a href="https://openjdk.org/jeps/11">Incubating Feature.</a>
 * Will be removed in a future release.</b>
 *
 * @since 24
 */
package jfx.incubator.scene.control.input;
