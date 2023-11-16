/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.incubator.scene.control.rich;

import java.util.function.Supplier;
import javafx.scene.control.ScrollBar;

/**
 * Configuration parameters for RichTextArea.
 *
 * These (immutable) parameters are passed to to the constructor,
 * affording modification of the default skin or behavior
 * without subsclassing of internal classes.
 */
public final class ConfigurationParameters {
    /** creates a horizontal scroll bar.  when set to null (default) a standard ScrollBar will be created */
    public final Supplier<ScrollBar> scrollBarGeneratorHorizontal;
    
    /** creates a vertical scroll bar.  when set to null (default) a standard ScrollBar will be created */
    public final Supplier<ScrollBar> scrollBarGeneratorVertical;

    /**
     * Creates a {@link Builder} instance.
     * @return the instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates default configuration parameters.
     * @return default parameters
     */
    public static ConfigurationParameters defaultConfig() {
        return ConfigurationParameters.builder().create();
    }

    /**
     * Creates an immutable instance from the {@code Builder}.
     * @param b configured builder
     */
    private ConfigurationParameters(Builder b) {
        scrollBarGeneratorVertical = b.verticalScrollBarGenerator;
        scrollBarGeneratorHorizontal = b.horizontalScrollBarGenerator;
    }

    /** A builder class allows for making {@code ConfigurationParameters} immutable */
    public static final class Builder {
        private Supplier<ScrollBar> verticalScrollBarGenerator;
        private Supplier<ScrollBar> horizontalScrollBarGenerator;
        
        private Builder() {
        }

        /**
         * Creates an immutable instance of {@link ConfigurationParameters}
         * @return the new instance
         */
        public ConfigurationParameters create() {
            return new ConfigurationParameters(this);
        }

        /**
         * Allows for creating custom vertical scroll bar.  A null (default) results in a standard ScrollBar.
         * @param gen the code to generate the ScrollBar, or null
         */
        public void setVerticalScrollBarGenerator(Supplier<ScrollBar> gen) {
            verticalScrollBarGenerator = gen;
        }

        /**
         * Allows for creating custom horizontal scroll bar.  A null (default) results in a standard ScrollBar.
         * @param gen the code to generate the ScrollBar, or null
         */
        public void setHorizontalScrollBarGenerator(Supplier<ScrollBar> gen) {
            horizontalScrollBarGenerator = gen;
        }
    }
}
