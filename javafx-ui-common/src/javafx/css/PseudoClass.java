/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.PseudoClassImpl;
import java.util.List;

/**
 * PseudoClass represents one unique pseudo-class state. There can be at most 1020
 * unique pseudo-classes with the current implementation. Introducing a pseudo-class into
 * a JavaFX class requires implementing {@link javafx.scene.Node#getPseudoClassStates()}
 * and calling the {@link javafx.scene.Node#pseudoClassStateChanged(PseudoClass)}
 * method when the corresponding property changes value. Typically, the
 * {@code pseudoClassStateChanged} method is called from the
 * {@code protected void invalidated()} method of a {@code javafx.beans.property}
 * class.
 * <pre>
 * <b>Example:</b>
 *
 * <code>
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
 *           pseudoClassStateChanged(MAGIC_PSEUDO_CLASS);
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
 *       MAGIC_PSEUDO_CLASS = PseudoClass.getPseudoClassName("xyzzy");
 *
 *   {@literal @}Override public Set<PseudoClass> getPseudoClassStates() {
 *         Set<PseudoClass> states = super.getPseudoClassStates();
 *         if (isMagic()) states.add(MAGIC_PSEUDO_CLASS);
 *         return states;
 *    }
 * </code></pre>
 */
public abstract class PseudoClass {

    /**
     * There is only one PseudoClass instance for a given pseudoClass.
     * There can be at most 1020 unique pseudo-classes.
     * @return The PseudoClass for the given pseudoClass. Will not return null.
     * @throws IllegalArgumentException if pseudoClass parameter is null or an empty String
     * @throws IndexOutOfBoundsException if adding the pseudoClass would exceed the
     *         maximum number of allowable pseudo-classes.
     */
    public static PseudoClass getPseudoClass(String pseudoClass) {
        
        final PseudoClass instance = PseudoClassImpl.getPseudoClassImpl(pseudoClass);
        return instance;
    }

    /** @return the pseudo-class state */
    abstract public String getPseudoClassName();

}
