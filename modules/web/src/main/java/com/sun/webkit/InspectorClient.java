/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

public interface InspectorClient {
    boolean sendMessageToFrontend(String message);
}
