/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import com.sun.javafx.css.PseudoClassState;

/**
 * PseudoClass represents one unique pseudo-class state. Introducing a
 * pseudo-class into a JavaFX class only requires that the method
 * {@link javafx.scene.Node#pseudoClassStateChanged(javafx.css.PseudoClass, boolean)}
 * be called when the pseudo-class state changes. Typically, the
 * {@code pseudoClassStateChanged} method is called from the
 * {@code protected void invalidated()} method of one of the property base
 * classes in the {@code javafx.beans.property} package.
 * <p>
 * Note that if a node has a default pseudo-class state, a horizontal orientation
 * for example, {@code pseudoClassStateChanged} should be called from the
 * constructor to set the initial state.
 * <p>
 * The following example would allow &quot;xyzzy&quot; to be used as a
 *  pseudo-class in a CSS selector.
 * <pre><code>
 *  public boolean isMagic() {
 *       return magic.get();
 *   }
 *
 *   public BooleanProperty magicProperty() {
 *       return magic;
 *   }
 *
 *   public BooleanProperty magic =
 *       new BooleanPropertyBase(false) {
 *
 *       {@literal @}Override protected void invalidated() {
 *           pseudoClassStateChanged(MAGIC_PSEUDO_CLASS. get());
 *       }
 *
 *       {@literal @}Override public Object getBean() {
 *           return MyControl.this;
 *       }
 *
 *       {@literal @}Override public String getName() {
 *           return "magic";
 *       }
 *   }
 *
 *   private static final PseudoClass
 *       MAGIC_PSEUDO_CLASS = PseudoClass.getPseudoClass("xyzzy");
 * </code></pre>
 * @since JavaFX 8.0
 */
public abstract class PseudoClass {

    /**
     * Constructor for subclasses to call.
     */
    public PseudoClass() {
    }

    /**
     * There is only one PseudoClass instance for a given pseudoClass.
     * @param pseudoClass the pseudo-class
     * @return The PseudoClass for the given pseudoClass. Will not return null.
     * @throws IllegalArgumentException if pseudoClass parameter is null or an empty String
     */
    public static PseudoClass getPseudoClass(String pseudoClass) {

        return PseudoClassState.getPseudoClass(pseudoClass);

    }

    /** @return the pseudo-class state */
    abstract public String getPseudoClassName();

}
