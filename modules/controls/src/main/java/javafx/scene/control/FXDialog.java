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
package javafx.scene.control;

import java.net.URL;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

abstract class FXDialog {

    /**************************************************************************
     * 
     * Static fields
     * 
     **************************************************************************/


    /**************************************************************************
     * 
     * Private fields
     * 
     **************************************************************************/

    protected Object owner;


    /**************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/

    protected FXDialog() {
        // pretty much a no-op, but we expect subclasses to call init(...) once 
        // they have initialised their abstract property methods.
    }



    /**************************************************************************
     * 
     * Public API
     * 
     **************************************************************************/




    /***************************************************************************
     * 
     * Abstract API
     * 
     **************************************************************************/

    public abstract void show();
    
    public abstract void showAndWait();

    public abstract void close(boolean closeWasNormal);
    
    public abstract void initOwner(Window owner);

    public abstract Window getOwner();
    
    public abstract void initModality(Modality modality);
    
    public abstract Modality getModality();
    
    public abstract ReadOnlyBooleanProperty showingProperty();

    public abstract Window getWindow();

    public abstract void sizeToScene();

    // --- x
    public abstract double getX();
    public abstract void setX(double x);
    public abstract ReadOnlyDoubleProperty xProperty();
    
    // --- y
    public abstract double getY();
    public abstract void setY(double y);
    public abstract ReadOnlyDoubleProperty yProperty();

    // --- resizable
    abstract BooleanProperty resizableProperty();


    // --- focused
    abstract ReadOnlyBooleanProperty focusedProperty();


    // --- title
    abstract StringProperty titleProperty();

    // --- content
    public abstract void setDialogPane(DialogPane node);

    // --- root
    public abstract Node getRoot();


    // --- width
    /**
     * Property representing the width of the dialog.
     */
    abstract ReadOnlyDoubleProperty widthProperty();
    
    abstract void setWidth(double width);


    // --- height
    /**
     * Property representing the height of the dialog.
     */
    abstract ReadOnlyDoubleProperty heightProperty();
    
    abstract void setHeight(double height);


    // stage style
    abstract void initStyle(StageStyle style);
    abstract StageStyle getStyle();




    /***************************************************************************
     *                                                                         
     * Implementation                                                 
     *                                                                         
     **************************************************************************/




    /***************************************************************************
     *                                                                         
     * Support Classes                                                 
     *                                                                         
     **************************************************************************/



    /***************************************************************************
     *                                                                         
     * Stylesheet Handling                                                     
     *                                                                         
     **************************************************************************/
}
