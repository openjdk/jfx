/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import java.net.URL;

public interface PolicyClient {

    public boolean permitNavigateAction(long sourceID, URL url);

    public boolean permitRedirectAction(long sourceID, URL url);

    public boolean permitAcceptResourceAction(long sourceID, URL url);

    public boolean permitSubmitDataAction(long sourceID, URL url, String httpMethod);

    public boolean permitResubmitDataAction(long sourceID, URL url, String httpMethod);

    public boolean permitEnableScriptsAction(long sourceID, URL url);

    public boolean permitNewPageAction(long sourceID, URL url);

    public boolean permitClosePageAction(long sourceID);
}
