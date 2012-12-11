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
package com.sun.javafx.css;

import java.util.List;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableMap;
import javafx.scene.Node;

/**
 * 
 * @author dgrieve
 */
public abstract class Styleable {
    
    /**
     * The id of this {@code Styleable}. This simple string identifier is useful for
     * finding a specific Node within the scene graph. While the id of a Node
     * should be unique within the scene graph, this uniqueness is not enforced.
     * This is analogous to the "id" attribute on an HTML element 
     * (<a href="http://www.w3.org/TR/CSS21/syndata.html#value-def-identifier">CSS ID Specification</a>).
     * <p>
     *     For example, if a Node is given the id of "myId", then the lookup method can
     *     be used to find this node as follows: <code>scene.lookup("#myId");</code>.
     * </p>
     */    
    public abstract String getId();

    /**
     * A list of String identifiers which can be used to logically group
     * Nodes, specifically for an external style engine. This variable is
     * analogous to the "class" attribute on an HTML element and, as such,
     * each element of the list is a style class to which this Node belongs.
     *
     * @see <a href="http://www.w3.org/TR/css3-selectors/#class-html">CSS3 class selectors</a>
     */    
    public abstract List<String> getStyleClass(); 

    /**
     * A string representation of the CSS style associated with this
     * specific {@code Node}. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * @param value The inline CSS style to use for this {@code Node}.
     *         {@code null} is implicitly converted to an empty String. 
     * @profile common
     * @defaultvalue empty string
     */    
    public abstract String getStyle();
    
    /**
     * Return the parent of the wrapped Object as a Styleable, or null if
     * there is no parent. 
     */
    public abstract Styleable getStyleableParent();
    
    /**
     * The CssMetaData's of this Styleable
     * @return 
     */
    public abstract List<CssMetaData> getCssMetaData(); 
        
    /**
     * A Styleable typically wraps a Node that is going to be styled by CSS.
     */
    public abstract Node getNode();
    
     /**
      * RT-17293
      * @treatAsPrivate implementation detail
      * @deprecated This is an experimental API that is not intended for use
      */
     private ObservableMap<WritableValue, List<Style>> styleMap;
     
     /**
      * RT-17293
      * @treatAsPrivate implementation detail
      * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
      */
     public ObservableMap<WritableValue, List<Style>> getStyleMap() {
         return styleMap;
     }

     /**
      * RT-17293
      * @treatAsPrivate implementation detail
      * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
      */
     public void setStyleMap(ObservableMap<WritableValue, List<Style>> styleMap) {
         this.styleMap = styleMap;
     }
    
}
