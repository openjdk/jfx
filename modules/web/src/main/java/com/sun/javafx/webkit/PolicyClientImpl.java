/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit;

//import javafx.scene.web.Policy;
//import javafx.scene.web.Policy.Request;
//import javafx.scene.web.Policy.ContentRequest;
//import javafx.scene.web.Policy.CookieRequest;
//import javafx.scene.web.Policy.FormRequest;
//import static javafx.scene.web.Policy.Request.Type.*;

import java.net.URL;

import com.sun.webkit.PolicyClient;


public final class PolicyClientImpl implements PolicyClient {

//    private boolean permitAction(Request request) {
//        Policy policy = web.getPolicy();
//        return policy != null
//               ? policy.permitAction(request)
//               : true;
//    }

    @Override public boolean permitNavigateAction(long frameID, URL url) {
        return true; //permitAction(new Request(NAVIGATE, url));
    }

    @Override public boolean permitRedirectAction(long frameID, URL url) {
        return true; //permitAction(new Request(REDIRECT, url));
    }

    @Override public boolean permitAcceptResourceAction(long frameID, URL url) {
        return true; //permitAction(new Request(ACCEPT_RESOURCE, url));
    }

    @Override public boolean permitSubmitDataAction(long frameID, URL url, String httpMethod) {
        return true; //permitAction(new FormRequest(SUBMIT_DATA, url, httpMethod));
    }

    @Override public boolean permitResubmitDataAction(long frameID, URL url, String httpMethod) {
        return true; //permitAction(new FormRequest(RESUBMIT_DATA, url, httpMethod));
    }

    @Override public boolean permitEnableScriptsAction(long frameID, URL url) {
        return true; //permitAction(new Request(ENABLE_SCRIPTS, url));
    }

    @Override public boolean permitNewPageAction(long frameID, URL url) {
        return true; //permitAction(new Request(NEW_PAGE, url));
    }

    @Override public boolean permitClosePageAction(long frameID) {
        return true; //permitAction(new Request(CLOSE_PAGE, null));
    }
}
