/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.scene.Node;

/**
 * An interface for defining the visual representation of user interface controls.
 * <p>
 * A Skin implementation should generally avoid modifying its control outside of
 * {@link #install()} method.  The life cycle of a Skin implementation
 * is as follows:
 * <ul>
 * <li>instantiation
 * <li>configuration, such as passing of dependencies and parameters
 * <li>when the skin is set on a {@link Skinnable}:
 * <ul>
 * <li>uninstalling of the old skin via its {@link #dispose()} method
 * <li>installing of the new skin via {@link #install()}
 * </ul>
 * </ul>
 *
 * @param <C> A subtype of Skinnable that the Skin represents. This allows for
 *      Skin implementation to access the {@link Skinnable} implementation,
 *      which is usually a {@link Control} implementation.
 * @since JavaFX 2.0
 */
public interface Skin<C extends Skinnable> {
    /**
     * Gets the Skinnable to which this Skin is assigned. A Skin must be created
     * for one and only one Skinnable. This value will only ever go from a
     * non-null to null value when the Skin is removed from the Skinnable, and
     * only as a consequence of a call to {@link #dispose()}.
     * <p>
     * The caller who constructs a Skinnable must also construct a Skin and
     * properly establish the relationship between the Control and its Skin.
     *
     * @return A non-null Skinnable, or null value if disposed.
     */
    public C getSkinnable();

    /**
     * Gets the Node which represents this Skin. This must never be null, except
     * after a call to {@link #dispose()}, and must never change except when
     * changing to null.
     *
     * @return A non-null Node, except when the Skin has been disposed.
     */
    public Node getNode();

    /**
     * Called once when {@code Skin} is set.  This method is called after the previous skin,
     * if any, has been uninstalled via its {@link #dispose()} method.
     * The skin can now safely make changes to its associated control, like registering listeners,
     * adding child nodes, and modifying properties and event handlers.
     * <p>
     * Application code must not call this method.
     * <p>
     * The default implementation of this method does nothing.
     *
     * @implNote
     * Skins only need to implement {@code install} if they need to make direct changes to the control
     * like overwriting properties or event handlers. Such skins should ensure these changes are undone in
     * their {@link #dispose()} method.
     *
     * @since 20
     */
    default public void install() { }

    /**
     * Called when a previously installed skin is about to be removed from its associated control.
     * This allows the skin to do clean up, like removing listeners and bindings, and undo any changes
     * to the control's properties.
     * After this method completes, {@link #getSkinnable()} and {@link #getNode()} should return {@code null}.
     * <p>
     * Calling {@link #dispose()} more than once has no effect.
     */
    public void dispose();
}
