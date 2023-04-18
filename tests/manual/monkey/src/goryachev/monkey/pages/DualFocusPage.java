// Copyright © 2018-2019 Andy Goryachev <andy@goryachev.com>
// https://github.com/andy-goryachev/JavaBugs/blob/master/src/goryachev/bugs/fx/DualFocus.java
// https://bugs.openjdk.org/browse/JDK-8292933
package goryachev.monkey.pages;

import goryachev.monkey.util.TestPaneBase;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

/**
 * Illustrates dual focus bug JDK-8292933
 */
public class DualFocusPage extends TestPaneBase {
    protected PopupControl popup;
    protected BorderPane popupBox;
    protected final TextField textField;

    public DualFocusPage() {
        textField = new TextField();
        textField.focusedProperty().addListener((s, p, c) -> handleFocus(c));

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setText(
            "\n\n\n" +
                "1. Click on the text field.\n" +
                "2. Notice that both the text field and the check box have focus.\n" +
                "3. Press SPACE.  Notice both both text field and check box have the input focus.\n" +
                "\n" +
                "Only one component is expected to have focus.");

        BorderPane bp = new BorderPane();
        bp.setPrefSize(700, 300);
        bp.setTop(textField);
        bp.setCenter(textArea);

        setContent(bp);
    }

    protected void handleFocus(boolean on) {
        if (on) {
            showPopup();
        } else {
            hidePopup();
        }
    }

    protected void showPopup() {
        if (popup == null) {
            popupBox = new BorderPane();
            popupBox.setLeft(new CheckBox("why do both popup and text field have the input focus?"));
            popupBox.setStyle("-fx-background-color:red; -fx-background-radius:10; -fx-padding:10px;");

            popup = new PopupControl();
            popup.getScene().setRoot(popupBox);
            popup.setConsumeAutoHidingEvents(false);
            popup.setAutoFix(true);
            popup.setAutoHide(false);

            popupBox.applyCss();

            double dx = textField.getLayoutX();
            double dy = textField.getLayoutY() + textField.getHeight();

            Point2D p = textField.localToScreen(0, 0);
            popup.show(textField, p.getX() + dx, p.getY() + dy);
        }
    }

    protected void hidePopup() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }

        if (popupBox != null) {
            popupBox = null;
        }
    }
}
