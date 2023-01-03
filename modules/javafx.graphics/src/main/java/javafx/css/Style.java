/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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


/**
 * A Style is just the selector and declaration from a Rule.
 *
 * @since 9
 */
final public class Style {

    /**
     * A selector might have more than one selector. This is the one that was
     * matched.
     * @return the matched selector
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * The Declaration that is the source of the style that is about
     * to be applied or has just been applied. May be null if the
     * value comes from the CSSProperty's initial (default) value.
     * @return the declaration
     */
    public Declaration getDeclaration() {
        return declaration;
    }

    /**
     * Constructs a {@code Style} object.
     * @param selector selector for this {@code Style}
     * @param declaration declaration for this {@code Style}
     */
    public Style(Selector selector, Declaration declaration) {
        this.selector = selector;
        this.declaration = declaration;
    }

    @Override public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Style other = (Style) obj;
        if (this.selector != other.selector && (this.selector == null || !this.selector.equals(other.selector))) {
            return false;
        }
        if (this.declaration != other.declaration && (this.declaration == null || !this.declaration.equals(other.declaration))) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (this.selector != null ? this.selector.hashCode() : 0);
        hash = 83 * hash + (this.declaration != null ? this.declaration.hashCode() : 0);
        return hash;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder()
                .append(String.valueOf(selector))
                .append(" { ")
                .append(String.valueOf(declaration))
                .append( " } ");
        return sb.toString();

    }

    private final Selector selector;
    private final Declaration declaration;
}

