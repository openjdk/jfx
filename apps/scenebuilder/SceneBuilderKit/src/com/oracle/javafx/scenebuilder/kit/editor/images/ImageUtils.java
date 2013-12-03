/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.images;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.WeakHashMap;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 *
 */
public abstract class ImageUtils  extends ImageUtilsBase {
    
    private static final Image WARNING_BADGE_IMAGE;
    
    static {
        final URL url = ImageUtils.class.getResource(UI_DIR + "/" + WARNING_BADGE); //NOI18N
        WARNING_BADGE_IMAGE = new Image(url.toExternalForm());
    }

    private static final WeakHashMap<String, Reference<Image>> imageCache = new WeakHashMap<>();

    public static Image getImage(URL resource) {
        // No resource found for the specified name
        if (resource == null) {
            resource = ImageUtils.class.getResource(NODE_ICONS_DIR + "/" + MISSING_ICON); //NOI18N
        }
        final String imageUrl = resource.toExternalForm();
        final Reference<Image> ref = imageCache.get(imageUrl);
        Image image = ref != null ? ref.get() : null;
        if (image == null) {
            image = new Image(imageUrl);
            imageCache.put(imageUrl, new SoftReference<>(image));
        }
        return image;
    }

    /**
     * Returns the image corresponding to the specified name.
     * The file MUST be located in the NODE_ICONS_DIR.
     * @param name
     * @return 
     */
    public static Image getNodeIcon(String name) {
        final URL resource = getNodeIconURL(name);
        return getImage(resource);
    }

    public static Image getImageFromNode(Node visualNode) {
        visualNode.setOpacity(0.75);
        final Group visualGroup = new Group();
        visualGroup.getChildren().add(visualNode);
        final Scene hiddenScene = new Scene(visualGroup);
        Stage hiddenStage = new Stage();
        hiddenStage.setScene(hiddenScene);
        final Image contentImage = visualNode.snapshot(null, null);
        // Detach the scene !
        hiddenScene.setRoot(new Group());
        hiddenStage.close();
        return contentImage;
    }
    
    public static Image getWarningBadgeImage() {
        return WARNING_BADGE_IMAGE;
    }
}
