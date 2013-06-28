/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

public abstract class WCImageFrame extends Ref {
    
    public abstract WCImage getFrame();

    protected void destroyDecodedData() {
    }
}
