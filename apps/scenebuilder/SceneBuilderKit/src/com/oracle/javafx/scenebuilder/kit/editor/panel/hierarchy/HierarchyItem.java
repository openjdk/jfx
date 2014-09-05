/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy;

import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption.FXID;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption.INFO;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption.NODEID;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import javafx.scene.image.Image;

/**
 * Object representing the data contained within the hierarchy TreeItems.
 *
 * @treatAsPrivate
 */
public class HierarchyItem {

    protected DesignHierarchyMask mask;

    /**
     * Creates a hierarchy item. Empty constructor used by the HierarchyItem
     * subclasses.
     */
    public HierarchyItem() {
    }

    /**
     * Creates a hierarchy item.
     *
     * @param fxomObject The FX object represented by this item
     */
    public HierarchyItem(final FXOMObject fxomObject) {
        assert fxomObject != null;
        this.mask = new DesignHierarchyMask(fxomObject);
    }

    /**
     * Returns true if the specified object is a HierarchyItem and it defines
     * same non null FXOMObject.
     * This method is used to retrieve the cell corresponding to this item.
     *
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HierarchyItem item = (HierarchyItem) obj;
        // equals method is overidden in the place holder sub classes.
        assert getFxomObject() != null;
        return getFxomObject().equals(item.getFxomObject());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.mask);
        return hash;
    }

    /**
     * Returns the mask represented by this item.
     *
     * @return the mask represented by this item.
     */
    public final DesignHierarchyMask getMask() {
        return mask;
    }

    /**
     * Returns the FX object represented by this item.
     *
     * @return the FX object represented by this item.
     */
    public final FXOMObject getFxomObject() {
        // Can be null for place holder items
        return mask == null ? null : mask.getFxomObject();
    }

    /**
     * Returns the information of the FX object represented by this item.
     *
     * @return the information of the FX object represented by this item.
     */
    public String getDescription() {
        // Can be null for place holder items
        return mask == null ? null : mask.getDescription();
    }

    /**
     * Returns the single line information of the FX object represented by this
     * item.
     *
     * @return the information of the FX object represented by this item.
     */
    public String getSingleLineDescription() {
        // Can be null for place holder items
        return mask == null ? null : mask.getSingleLineDescription();
    }

    /**
     * Returns the node ID of the FX object represented by this item.
     *
     * @return the node ID of the FX object represented by this item.
     */
    public String getNodeId() {
        // Can be null for place holder items
        return mask == null ? null : mask.getNodeId();
    }

    /**
     * Returns the FX ID of the FX object represented by this item.
     *
     * @return the FX ID of the FX object represented by this item.
     */
    public String getFxId() {
        // Can be null for place holder items
        return mask == null ? null : mask.getFxId();
    }

    public String getDisplayInfo(final DisplayOption option) {
        // Place holder items do not have display info
        if (mask == null) {
            return null;
        }
        final Object sceneGraphObject = mask.getFxomObject().getSceneGraphObject();
        if (sceneGraphObject == null) {
            // For now, handle display label for scenegraph objects only
            return null;
        }
        String info = null;
        switch (option) {
            case INFO:
                info = getSingleLineDescription();
                break;
            case FXID:
                info = getFxId();
                break;
            case NODEID:
                info = getNodeId();
                break;
        }
        return info;
    }

    public PropertyName getPropertyNameForDisplayInfo(final DisplayOption option) {
        assert mask != null;
        PropertyName propertyName = null;
        switch (option) {
            case INFO:
                propertyName = mask.getPropertyNameForDescription();
                break;
            case NODEID:
                propertyName = new PropertyName("id");
                break;
            default:
                assert false;
        }
        return propertyName;
    }

    public boolean isResourceKey(final DisplayOption option) {
        // Place holder items do not have display info
        if (mask == null) {
            return false;
        }
        return option == INFO && mask.isResourceKey();
    }

    public boolean isPlaceHolder() {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isAcceptingSubComponent(final List<FXOMObject> fxomObjects) {
        return !isEmpty() && mask.isAcceptingSubComponent(fxomObjects);
    }

    public boolean isAcceptingAccessory(Accessory accessory, FXOMObject fxomObject) {
        return !isEmpty()
                && mask.isAcceptingAccessory(accessory, fxomObject)
                && mask.getAccessory(accessory) == null;
    }

    public boolean hasDisplayInfo(final DisplayOption option) {
        // Item has display info if we are not on place holder and :
        // - either we display the FX ID
        // - or we display the node ID
        // - or we display the description and the item defines one
        return mask != null
                && (option == FXID
                || option == NODEID
                || (option == INFO && mask.hasDescription()));
    }

    public Image getPlaceHolderImage() {
        // No place holder
        return null;
    }

    public String getPlaceHolderInfo() {
        // No place holder
        return null;
    }

    public Image getClassNameIcon() {
        assert mask != null;
        return mask.getClassNameIcon();
    }

    public URL getClassNameIconURL() {
        assert mask != null;
        return mask.getClassNameIconURL();
    }

    public String getClassNameInfo() {
        assert mask != null;
        return mask.getClassNameInfo();
    }
}
