/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism.theme;

import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.Graphics;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.javafx.webkit.theme.Renderer;
import javafx.scene.Scene;
import javafx.scene.control.Control;

public final class PrismRenderer extends Renderer {

    @Override
    protected void render(Control control, WCGraphicsContext g) {
        Scene.impl_setAllowPGAccess(true);
        // The peer is not modified.
        NGNode peer = control.impl_getPeer();
        Scene.impl_setAllowPGAccess(false);

        peer.render((Graphics)g.getPlatformGraphics());
    }
}
