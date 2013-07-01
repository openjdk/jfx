/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.web;

import javafx.util.Callback;

/**
 * An object that can be used to debug a web page loaded into
 * a {@link WebEngine}.
 */
public interface Debugger {

    /**
     * Determines whether the debugger is enabled.
     * A debugger's {@link #sendMessage} method may only be called
     * while the debugger is enabled.
     * The message callback object registered with a debugger is only called
     * while the debugger is enabled.
     * @return {@code true} if the debugger is enabled,
     *         {@code false} otherwise.
     */
    boolean isEnabled();

    /**
     * Enables or disables the debugger.
     * A debugger's {@link #sendMessage} method may only be called
     * while the debugger is enabled.
     * The message callback object registered with a debugger is only called
     * while the debugger is enabled.
     * <p>
     * This method has no effect
     * if the {@code enabled} parameter is {@code true}
     * and the debugger is already enabled,
     * or if the {@code enabled} parameter is {@code false}
     * and the debugger is already disabled.
     * @param enabled specifies whether the debugger should be enabled
     *        or disabled.
     */
    void setEnabled(boolean enabled);

    /**
     * Sends a message to the debugger.
     * The message is a text string in the format specified by
     * the WebKit Remote Debugging Protocol.
     * <p>
     * This method may only be called while the debugger is enabled.
     * @param message the message to be sent to the debugger.
     *        May not be {@code null}.
     * @throws IllegalStateException if the debugger is not enabled.
     * @throws NullPointerException if {@code message} is {@code null}.
     */
    void sendMessage(String message);

    /**
     * Returns the message callback object registered with the debugger.
     * The debugger calls the message callback object's
     * {@link Callback#call} method to deliver a message to
     * the debugger frontend.
     * The message passed to the message callback is a text string
     * in the format specified by the WebKit Remote Debugging Protocol.
     * @return the message callback object registered with the debugger,
     *         or {@code null} if there is no such object.
     */
    Callback<String,Void> getMessageCallback();

    /**
     * Registers a message callback object with the debugger.
     * The debugger calls the message callback object's
     * {@link Callback#call} method to deliver a message to
     * the debugger frontend.
     * The message passed to the message callback is a text string
     * in the format specified by the WebKit Remote Debugging Protocol.
     * @param callback the message callback object to be registered with
     *        the debugger. May be {@code null}.
     */
    void setMessageCallback(Callback<String,Void> callback);
}
