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

/**
 * Provides common models for
 * {@link jfx.incubator.scene.control.richtext.RichTextArea RichTextArea} and
 * {@link jfx.incubator.scene.control.richtext.CodeArea CodeArea} controls.
 * <p>
 * The {@link jfx.incubator.scene.control.richtext.RichTextArea RichTextArea}
 * control separates data model from the view by providing the
 * {@link jfx.incubator.scene.control.richtext.RichTextArea#modelProperty() model} property.
 * The base class for any data model is
 * {@link jfx.incubator.scene.control.richtext.model.StyledTextModel StyledTextModel}.
 * This abstract class provides no data storage, focusing instead on providing common functionality
 * to the actual models, such as dealing with styled segments, keeping track of markers, sending events, etc.
 * <p>
 * This package provides a number of standard models are provided, each designed for a specific use case.
 * <ul>
 * <li>The {@link jfx.incubator.scene.control.richtext.model.RichTextModel RichTextModel} stores the data in memory,
 *     in the form of text segments styled with attributes defined in
 *     {@link jfx.incubator.scene.control.richtext.model.StyleAttributeMap StyleAttributeMap} class.
 *     This is a default model for RichTextArea.
 * <li>The {@link jfx.incubator.scene.control.richtext.model.BasicTextModel BasicTextModel}
 *     could be used as a base class for in-memory or virtualized text models based on plain text.
 *     This class provides foundation for the
 *     {@link jfx.incubator.scene.control.richtext.model.CodeTextModel CodeTextModel},
 *     which supports styling using a pluggable
 *     {@link jfx.incubator.scene.control.richtext.SyntaxDecorator SyntaxDecorator}.
 * <li>The abstract
 *     {@link jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase StyledTextModelViewOnlyBase}
 *     is a base class for immutable models.  This class is used by
 *     {@link jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel SimpleViewOnlyStyledModel}
 *     which simplifies building of in-memory view-only styled documents.
 * </ul>
 * <b><a href="https://openjdk.org/jeps/11">Incubating Feature.</a>
 * Will be removed in a future release.</b>
 *
 * @since 24
 */
package jfx.incubator.scene.control.richtext.model;
