/*
 * Created on 16.09.2019
 *
 */
package test.javafx.scene.control;

import javafx.scene.control.PasswordField;

/**
 * Test for interplay of ENTER/ESCAPE handlers on PasswordField with
 * default/cancel button actions.
 */
public class PasswordFieldDefaultCancelButtonTest extends DefaultCancelButtonTestBase<PasswordField> {

     public PasswordFieldDefaultCancelButtonTest(ButtonType buttonType,
            boolean consume, boolean registerAfterShowing) {
        super(buttonType, consume, registerAfterShowing);
    }

    @Override
    protected PasswordField createControl() {
        return new PasswordField();
    }

}
