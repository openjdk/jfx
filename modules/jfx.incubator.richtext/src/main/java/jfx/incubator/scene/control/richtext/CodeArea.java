/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
package jfx.incubator.scene.control.richtext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.text.Font;
import com.sun.jfx.incubator.scene.control.richtext.Params;
import com.sun.jfx.incubator.scene.control.richtext.StringBuilderStyledOutput;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import jfx.incubator.scene.control.richtext.skin.CodeAreaSkin;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;

/**
 * CodeArea is an editable text component which supports styling (for example, syntax highlighting) of plain text.
 * <p>
 * Unlike its base class {@link RichTextArea}, the {@code CodeArea} requires a special kind of model to be used,
 * a {@link CodeTextModel}.
 *
 * <h2>Creating a CodeArea</h2>
 * The following example creates an editable control with the default {@link CodeArea}:
 * <pre>{@code    CodeArea codeArea = new CodeArea();
 *   codeArea.setWrapText(true);
 *   codeArea.setLineNumbersEnabled(true);
 *   codeArea.setText("Lorem\nIpsum");
 * }</pre>
 * Which results in the following visual representation:
 * <p>
 * <img src="doc-files/CodeArea.png" alt="Image of the CodeArea control">
 *
 * <h2>Usage Considerations</h2>
 * {@code CodeArea} extends the {@link RichTextArea} class, meaning most of the functionality works as it does
 * in the base class.
 * There are some differences that should be mentioned:
 * <ul>
 * <li>Model behavior: any direct changes to the styling, such as
 * {@link #applyStyle(TextPos, TextPos, jfx.incubator.scene.control.richtext.model.StyleAttributeMap) applyStyle()},
 * will be ignored
 * <li>Line numbers: the {@code CodeArea} sets the {@link #leftDecoratorProperty()} to support the line numbers,
 * so applications should not set or bind that property.
 * </ul>
 *
 * @since 24
 */
public class CodeArea extends RichTextArea {
    private BooleanProperty lineNumbers;
    private StyleableIntegerProperty tabSize;
    private StyleableObjectProperty<Font> font;
    private StyleableDoubleProperty lineSpacing;
    private String fontStyle;

    /**
     * This constructor creates the CodeArea with the specified {@link CodeTextModel}.
     * @param model the instance of {@link CodeTextModel} to use
     */
    public CodeArea(CodeTextModel model) {
        super(model);

        getStyleClass().add("code-area");
        setAccessibleRoleDescription("Code Area");
    }

    /**
     * This constructor creates the CodeArea with the default {@link CodeTextModel}.
     */
    public CodeArea() {
        this(new CodeTextModel());
    }

    @Override
    protected void validateModel(StyledTextModel m) {
        if ((m != null) && (!(m instanceof CodeTextModel))) {
            throw new IllegalArgumentException("CodeArea accepts models that extend CodeTextModel");
        }
    }

    @Override
    protected RichTextAreaSkin createDefaultSkin() {
        return new CodeAreaSkin(this);
    }

    /**
     * This convenience method sets the decorator property in the {@link CodeTextModel}.
     * Nothing is done if the model is null.
     *
     * @param d the syntax decorator
     * @see CodeTextModel#setDecorator(SyntaxDecorator)
     */
    public final void setSyntaxDecorator(SyntaxDecorator d) {
        CodeTextModel m = codeModel();
        if (m != null) {
            m.setDecorator(d);
        }
    }

    /**
     * This convenience method returns the syntax decorator value in the {@link CodeTextModel},
     * or null if the model is null.
     * @return the syntax decorator value, or null
     */
    public final SyntaxDecorator getSyntaxDecorator() {
        CodeTextModel m = codeModel();
        return (m == null) ? null : m.getDecorator();
    }

    /**
     * Determines whether to show line numbers.
     * Toggling this property results in changes made to the {@link RichTextArea#leftDecoratorProperty() leftDecorator}
     * property, so the application code should not bind or modify that property.
     *
     * @return the line numbers enabled property
     * @defaultValue false
     */
    // TODO should there be a way to customize the line number component? createLineNumberDecorator() ?
    // TODO should this be a styleable property?
    public final BooleanProperty lineNumbersEnabledProperty() {
        if (lineNumbers == null) {
            lineNumbers = new SimpleBooleanProperty() {
                @Override
                protected void invalidated() {
                    LineNumberDecorator d;
                    if (get()) {
                        // TODO create line number decorator method?
                        d = new LineNumberDecorator() {
                            @Override
                            public Node getMeasurementNode(int ix) {
                                return bindFont(super.getMeasurementNode(ix));
                            }

                            @Override
                            public Node getNode(int ix) {
                                return bindFont(super.getNode(ix));
                            }

                            private Node bindFont(Node n) {
                                if (n instanceof Labeled t) {
                                    t.fontProperty().bind(fontProperty());
                                }
                                return n;
                            }
                        };
                    } else {
                        d = null;
                    }
                    setLeftDecorator(d);
                }
            };
        }
        return lineNumbers;
    }

    public final boolean isLineNumbersEnabled() {
        return lineNumbers == null ? false : lineNumbers.get();
    }

    public final void setLineNumbersEnabled(boolean on) {
        lineNumbersEnabledProperty().set(on);
    }

    /**
     * The size of a tab stop in spaces.
     * Values less than 1 are treated as 1.
     * @return the tab size property
     * @defaultValue 8
     */
    public final IntegerProperty tabSizeProperty() {
        if (tabSize == null) {
            tabSize = new StyleableIntegerProperty(Params.DEFAULT_TAB_SIZE) {
                @Override
                public Object getBean() {
                    return CodeArea.this;
                }

                @Override
                public String getName() {
                    return "tabSize";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.TAB_SIZE;
                }
            };
        }
        return tabSize;
    }

    public final int getTabSize() {
        return tabSize == null ? Params.DEFAULT_TAB_SIZE : tabSize.get();
    }

    public final void setTabSize(int spaces) {
        tabSizeProperty().set(spaces);
    }

    /**
     * The font to use for text in the {@code CodeArea}.
     * @return the font property
     * @defaultValue the Monospaced font with the default size
     * @see Font#font(String, double)
     */
    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new StyleableObjectProperty<Font>(defaultFont())
            {
                private boolean fontSetByCss;

                @Override
                public void applyStyle(StyleOrigin newOrigin, Font value) {
                    // TODO perhaps this is not needed
                    // RT-20727 JDK-8127428
                    // if CSS is setting the font, then make sure invalidate doesn't call NodeHelper.reapplyCSS
                    try {
                        // super.applyStyle calls set which might throw if value is bound.
                        // Have to make sure fontSetByCss is reset.
                        fontSetByCss = true;
                        super.applyStyle(newOrigin, value);
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        fontSetByCss = false;
                    }
                }

                @Override
                public void set(Font value) {
                    Font old = get();
                    if (value == null ? old == null : value.equals(old)) {
                        return;
                    }
                    super.set(value);
                }

                @Override
                public CssMetaData<CodeArea, Font> getCssMetaData() {
                    return StyleableProperties.FONT;
                }

                @Override
                public Object getBean() {
                    return CodeArea.this;
                }

                @Override
                public String getName() {
                    return "font";
                }
            };
        }
        return font;
    }

    public final void setFont(Font value) {
        fontProperty().setValue(value);
    }

    public final Font getFont() {
        return font == null ? defaultFont() : font.getValue();
    }

    private static Font defaultFont() {
        return Font.font("Monospaced", -1);
    }

    /**
     * Defines the vertical space in pixels between lines.
     *
     * @return the property instance
     * @defaultValue 0
     */
    public final DoubleProperty lineSpacingProperty() {
        if (lineSpacing == null) {
            lineSpacing = new StyleableDoubleProperty(0) {
                @Override
                public Object getBean() {
                    return CodeArea.this;
                }

                @Override
                public String getName() {
                    return "lineSpacing";
                }

                @Override
                public CssMetaData<CodeArea, Number> getCssMetaData() {
                    return StyleableProperties.LINE_SPACING;
                }
            };
        }
        return lineSpacing;
    }

    public final void setLineSpacing(double spacing) {
        lineSpacingProperty().set(spacing);
    }

    public final double getLineSpacing() {
        return lineSpacing == null ? 0 : lineSpacing.get();
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
        case FONT:
            return getFont();
        default:
            return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** styleable properties */
    private static class StyleableProperties {
        private static final CssMetaData<CodeArea,Number> LINE_SPACING =
            new CssMetaData<>("-fx-line-spacing", SizeConverter.getInstance(), 0) {

            @Override public boolean isSettable(CodeArea n) {
                return n.lineSpacing == null || !n.lineSpacing.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(CodeArea n) {
                return (StyleableProperty<Number>)n.lineSpacingProperty();
            }
        };

        private static final FontCssMetaData<CodeArea> FONT =
            new FontCssMetaData<>("-fx-font", defaultFont())
        {
            @Override
            public boolean isSettable(CodeArea n) {
                return n.font == null || !n.font.isBound();
            }

            @Override
            public StyleableProperty<Font> getStyleableProperty(CodeArea n) {
                return (StyleableProperty<Font>)(WritableValue<Font>)n.fontProperty();
            }
        };

        private static final CssMetaData<CodeArea, Number> TAB_SIZE =
            new CssMetaData<>("-fx-tab-size", SizeConverter.getInstance(), Params.DEFAULT_TAB_SIZE)
        {
            @Override
            public boolean isSettable(CodeArea n) {
                return n.tabSize == null || !n.tabSize.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(CodeArea n) {
                return (StyleableProperty<Number>)n.tabSizeProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES = RichUtils.combine(
            RichTextArea.getClassCssMetaData(),
            FONT,
            LINE_SPACING,
            TAB_SIZE
        );
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * Returns plain text.  This method returns an empty string when the model is {@code null}.
     * @return plain text
     */
    public final String getText() {
        StyledTextModel m = getModel();
        if (m == null) {
            return "";
        }
        TextPos end = m.getDocumentEnd();
        try (StringBuilderStyledOutput out = new StringBuilderStyledOutput()) {
            out.setLineSeparator("\n");
            m.export(TextPos.ZERO, end, out);
            return out.toString();
        } catch (IOException e) {
            // should not happen
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Replaces text in this CodeArea.
     * <p>
     * The caret gets reset to the start of the document, selection gets cleared, and an undo event gets created.
     * @param text the text string
     */
    public final void setText(String text) {
        TextPos end = getDocumentEnd();
        getModel().replace(null, TextPos.ZERO, end, text, true);
    }

    private CodeTextModel codeModel() {
        return (CodeTextModel)getModel();
    }
}
