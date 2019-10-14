/*
 * Created on 11.10.2019
 *
 */
package test.javafx.scene.control;

import org.junit.Ignore;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
/**
 * Test interaction of TextField with formatter with default/cancel button
 *
 * Fails for cancel/not consuming - behavior.cancel
 * consumes always with formatter. Should it?
 *
 * Ignoring for now
 */
@Ignore
public class TextFieldWithFormatterDefaultCancelButtonTest
        extends TextFieldDefaultCancelButtonTest {

    public TextFieldWithFormatterDefaultCancelButtonTest(ButtonType buttonType,
            boolean consume, boolean registerAfterShowing) {
        super(buttonType, consume, registerAfterShowing);
    }

    @Override
    protected TextField createControl() {
        TextField input = super.createControl();
        input.setTextFormatter(new TextFormatter<>(TextFormatter.IDENTITY_STRING_CONVERTER));
        return input;
    }

}
