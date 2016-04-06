/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

package dragdrop;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class SimpleTextEdit extends Group {

    private static final int MAX_LENGTH = 35;

    protected Group skin;
    private Text[] letters;
    private Rectangle selection;
    private Rectangle background;
    private Line caret;

    private String content;
    private int length = 0;
    private int caretPos = 0;
    private int selStart = -1;
    private int selStop = -1;
    private int selPivot = -1;

    public SimpleTextEdit() {

        skin = new Group();
        getChildren().add(skin);

        background = new Rectangle(-10, -10, 420, 40);
        background.setStroke(Color.GRAY);
        background.setFill(Color.WHITE);

        letters = new Text[MAX_LENGTH + 1];
        for (int i = 0; i <= MAX_LENGTH; i++) {
            Text t = new Text();
            t.setScaleX(2.0);
            t.setScaleY(2.0);
            t.setTextOrigin(VPos.TOP);
            letters[i] = t;
        }

        caret = new Line(0, -5, 0, 25);

        selection = new Rectangle(0, -5, 0, 30);
        selection.setFill(Color.LIGHTBLUE);
        selection.setOpacity(0.5);

        skin.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                requestFocus();
                int pos = getPos(event.getX());
                if (pos >= 0) {
                    setCaretPos(pos);
                }
                clearSelection();
            }
        });

        skin.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (selPivot >= 0) {
                    selPivot = -1;
                    if (selStart == selStop) {
                        clearSelection();
                    }
                } else {
                    clearSelection();
                    int pos = getPos(event.getX());
                    if (pos >= 0) {
                        setCaretPos(pos);
                    }
                }
            }
        });

        skin.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                int pos = getPos(event.getX());

                if (selPivot < 0 && isInSelection(pos)) {
                    return;
                }

                setCaretPos(pos);

                if (selStart < 0) {
                    selStart = caretPos;
                    selStop = caretPos;
                    selPivot = caretPos;
                } else if (caretPos >= selPivot) {
                    selStart = selPivot;
                    selStop = caretPos;
                } else {
                    selStart = caretPos;
                    selStop = selPivot;
                }
                updateSelection();
            }
        });

        setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent event) {
                if (event.isControlDown() || event.isAltDown()) {
                    return;
                }
                char c = event.getCharacter().charAt(0);
                if (!Character.isISOControl(c) && length < MAX_LENGTH) {
                    insert(Character.toString(c));
                    setCaretPos(caretPos + 1);
                    clearSelection();
                }
            }
        });

        setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.DELETE) {
                    if (caretPos < length || selStart >= 0) {
                        if (selStart >= 0) {
                            removeSelection();
                        } else {
                            setText(content.substring(0, caretPos) +
                                    content.substring(caretPos + 1));
                        }
                    }
                } else if (event.getCode() == KeyCode.BACK_SPACE) {
                    if (caretPos > 0 || selStart >= 0) {
                        if (selStart >= 0) {
                            removeSelection();
                        } else {
                            setCaretPos(caretPos - 1);
                            setText(content.substring(0, caretPos) +
                                    content.substring(caretPos + 1));
                        }
                    }
                } else if (event.getCode() == KeyCode.RIGHT) {
                    if (event.isShiftDown()) {
                        if (selStart < 0) {
                            selStart = caretPos;
                            selStop = caretPos;
                        }
                        if (caretPos < length) {
                            if (selStart < caretPos || selStart == selStop) {
                                selStop = caretPos + 1;
                            } else {
                                selStart = caretPos + 1;
                            }
                        }
                        updateSelection();
                    } else {
                        clearSelection();
                    }
                    setCaretPos(caretPos + 1);
                } else if (event.getCode() == KeyCode.LEFT) {
                    if (event.isShiftDown()) {
                        if (selStart < 0) {
                            selStart = caretPos;
                            selStop = caretPos;
                        }
                        if (caretPos > 0) {
                            if (selStart == caretPos) {
                                selStart = caretPos - 1;
                            } else {
                                selStop = caretPos - 1;
                            }
                        }
                        updateSelection();
                    } else {
                        clearSelection();
                    }
                    setCaretPos(caretPos - 1);
                }
            }
        });

        focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                clearSelection();
                update();
            }
        });
    }

    public void setText(String text) {
        length = text.length();
        if (length > MAX_LENGTH) {
            length = MAX_LENGTH;
        }
        content = text.substring(0, length);

        for (int i = 0; i < length; i++) {
            letters[i].setText(text.substring(i, i+1));
        }

        letters[length].setText(" ");

        update();
    }

    public void insert(String s) {
        setText(content.substring(0, caretPos) + s +
                content.substring(caretPos));
        if (selStart >= caretPos) {
            selStart += s.length();
            selStop += s.length();
        }
    }

    public String getSelection() {
        return content.substring(selStart, selStop);
    }

    public void removeSelection() {
        if (caretPos > selStart) {
            if (caretPos < selStop) {
                caretPos = selStart;
            } else {
                caretPos -= selStop - selStart;
            }
        }
        if (selStart >= 0) {
            setText(content.substring(0, selStart) +
                    content.substring(selStop));
        }
        clearSelection();
    }

    public void clearSelection() {
        selStart = -1;
        selStop = -1;
        updateSelection();
    }

    protected void setCaretPos(int pos) {
        if (pos > length) {
            pos = length;
        }
        if (pos < 0) {
            pos = 0;
        }

        caretPos = pos;
        caret.setStartX(letters[pos].getX() - 2);
        caret.setEndX(letters[pos].getX() - 2);
    }

    protected int getCaretPos() {
        return caretPos;
    }

    protected void showCaret() {
        skin.getChildren().add(caret);
    }

    protected void removeCaret() {
        skin.getChildren().remove(caret);
    }

    protected int getPos(double x) {
        for (int i = 0; i < length; i++) {
            Bounds b = letters[i].getBoundsInParent();
            if (b.getMinX() <= x + 1 && b.getMaxX() >= x) {
                return i;
            } else if (i == length - 1 && b.getMaxX() <= x) {
                return i + 1;
            }
        }
        return -1;
    }

    protected boolean isInSelection(int pos) {
        return pos >= selStart && pos < selStop;
    }

    private void update() {
        skin.getChildren().clear();
        skin.getChildren().add(background);
        int pos = 0;

        for (int i = 0; i <= length; i++) {
            letters[i].setX(pos);
            skin.getChildren().add(letters[i]);
            pos += letters[i].getBoundsInParent().getWidth();
        }

        setCaretPos(caretPos);
        if (isFocused()) {
            skin.getChildren().add(caret);
        }
        skin.getChildren().add(selection);
    }

    private void updateSelection() {
        if (selStart < 0) {
            selection.setWidth(0);
            return;
        }

        selection.setX(letters[selStart].getBoundsInParent().getMinX());
        selection.setWidth(letters[selStop].getBoundsInParent().getMinX() -
                selection.getX());

    }

}
