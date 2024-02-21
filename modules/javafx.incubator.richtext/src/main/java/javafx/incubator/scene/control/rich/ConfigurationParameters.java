/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * These immutable parameters are passed to to the RichTextArea constructor.
 */
// TODO corner node?
public final class ConfigurationParameters {
    /**
     * This {@code Supplier} allows the skin to create a custom horizontal scroll bar.
     * When set to {@code null}, a regular {@code ScrollBar} will be created.
     */
    public final Supplier<ScrollBar> scrollBarGeneratorHorizontal;

    /**
     * This {@code Supplier} allows the skin to create a custom vertical scroll bar.
     * When set to {@code null}, a regular {@code ScrollBar} will be created.
     */
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
        return ConfigurationParameters.builder().build();
    }

    /**
     * Creates an immutable instance from the {@code Builder}.
     * @param b configured builder
     */
    private ConfigurationParameters(Builder b) {
        scrollBarGeneratorVertical = b.verticalScrollBarGenerator;
        scrollBarGeneratorHorizontal = b.horizontalScrollBarGenerator;
    }

    /**
     * This builder creates an instance of immutable {@code ConfigurationParameters}.
     */
    public static final class Builder {
        private Supplier<ScrollBar> verticalScrollBarGenerator;
        private Supplier<ScrollBar> horizontalScrollBarGenerator;

        private Builder() {
        }

        /**
         * Creates an immutable instance of {@link ConfigurationParameters}
         * @return the new instance
         */
        public ConfigurationParameters build() {
            return new ConfigurationParameters(this);
        }

        /**
         * Sets the {@code Supplier} for a custom vertical scroll bar for use by the skin.
         * @param gen the scroll bar generator
         * @return this Builder
         */
        public Builder verticalScrollBar(Supplier<ScrollBar> gen) {
            verticalScrollBarGenerator = gen;
            return this;
        }

        /**
         * Sets the {@code Supplier} for a custom horizontal scroll bar for use by the skin.
         * @param gen the scroll bar generator
         * @return this Builder
         */
        public Builder horizontalScrollBar(Supplier<ScrollBar> gen) {
            horizontalScrollBarGenerator = gen;
            return this;
        }
    }
}
