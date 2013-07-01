/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

public interface LoadListenerClient {
    public final static int PAGE_STARTED = 0;
    public final static int PAGE_FINISHED = 1;
    public final static int PAGE_REDIRECTED = 2;
    public final static int LOAD_FAILED = 5;
    public final static int LOAD_STOPPED = 6;
    public final static int CONTENT_RECEIVED = 10;
    public final static int TITLE_RECEIVED = 11;
    public final static int ICON_RECEIVED = 12;
    public final static int CONTENTTYPE_RECEIVED = 13;
    public final static int DOCUMENT_AVAILABLE = 14;
    public final static int RESOURCE_STARTED = 20;
    public final static int RESOURCE_REDIRECTED = 21;
    public final static int RESOURCE_FINISHED = 22;
    public final static int RESOURCE_FAILED = 23;
    public final static int PROGRESS_CHANGED = 30;


    // -- Error Code values
    /**
     * An error code indicating that the host name couldn't be resolved.
     */
    public final static int UNKNOWN_HOST = 1;
    /**
     * An error code indicating that the URL was malformed or illegal.
     */
    public final static int MALFORMED_URL = 2;
    /**
     * An error code indicating that the SSL handshake failed.
     */
    public final static int SSL_HANDSHAKE = 3;
    /**
     * An error code indicating that the connection was refused by the server.
     */
    public final static int CONNECTION_REFUSED = 4;
    /**
     * An error code indicating that the connection was reset by the server.
     */
    public final static int CONNECTION_RESET = 5;
    /**
     * An error code indicating that there was no route to the host.
     */
    public final static int NO_ROUTE_TO_HOST = 6;
    /**
     * An error code indicating that the connection timed out.
     */
    public final static int CONNECTION_TIMED_OUT = 7;
    /**
     * An error code indicating that the client was denied permission
     * to initiate connection to the server.
     */
    public final static int PERMISSION_DENIED = 8;
    /**
     * An error code indicating that the server response was invalid.
     */
    public final static int INVALID_RESPONSE = 9;
    /**
     * An error code indicating that too many redirects were encountered
     * while processing the request.
     */
    public final static int TOO_MANY_REDIRECTS = 10;
    /**
     * An error code indicating that the requested local file was not found.
     */
    public final static int FILE_NOT_FOUND = 11;
    /**
     * An error code indicating that an unknown error occurred.
     */
    public final static int UNKNOWN_ERROR = 99;


    public void dispatchLoadEvent(long frame, int state,
                                  String url, String contentType,
                                  double progress, int errorCode);

    public void dispatchResourceLoadEvent(long frame, int state,
                                          String url, String contentType,
                                          double progress, int errorCode);
}
