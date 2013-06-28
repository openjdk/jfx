/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

/**
 * This class encapsulates data passed into JavaScript {@code prompt()} function:
 * a message and a default value. Instances are passed into {@code prompt}
 * handlers registered on a {@code WebEngine} using
 * {@link WebEngine#setPromptHandler} method.
 * 
 * @see WebEngine
 * @see WebEngine#setPromptHandler
 * @since JavaFX 2.0
 */
public final class PromptData {

    private final String message;
    private final String defaultValue;

    /**
     * Creates a new instance.
     */
    public PromptData(String message, String defaultValue) {
        this.message = message;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns message carried by this data object.
     */
    public final String getMessage() {
        return message;
    }

    /**
     * Returns default value carried by this data object.
     */
    public final String getDefaultValue() {
        return defaultValue;
    }
}
