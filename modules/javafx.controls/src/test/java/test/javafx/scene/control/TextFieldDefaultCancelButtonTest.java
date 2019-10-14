/*
 * Created on 11.10.2019
 *
 */
package test.javafx.scene.control;

import javafx.scene.control.TextField;

/**
 * Test for interplay of ENTER/ESCAPE handlers on TextField with
 * default/cancel button actions.
 */
public class TextFieldDefaultCancelButtonTest extends DefaultCancelButtonTestBase<TextField> {

    public TextFieldDefaultCancelButtonTest(ButtonType buttonType, boolean consume,
            boolean registerAfterShowing) {
        super(buttonType, consume, registerAfterShowing);
    }

    @Override
    protected TextField createControl() {
        return new TextField();
    }

}
