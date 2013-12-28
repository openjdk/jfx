/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

public abstract class WCFontCustomPlatformData {
    protected abstract WCFont createFont(int size, boolean bold,
                                         boolean italic);
}
