/*
 * Created on 16.09.2019
 *
 */
package test.javafx.scene.control;

import javafx.scene.control.TextArea;

/**
 * Test for interplay of ENTER/ESCAPE handlers on TextArea with
 * default/cancel button actions.
 */
public class TextAreaDefaultCancelButtonTest extends DefaultCancelButtonTestBase<TextArea> {

    public TextAreaDefaultCancelButtonTest(ButtonType buttonType,
            boolean consume, boolean registerAfterShowing) {
        super(buttonType, consume, registerAfterShowing);
    }

    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @Override
    public void testFallbackFilter() {
        if (isEnter()) return;
        super.testFallbackFilter();
    }

    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @Override
    public void testFallbackHandler() {
        if (isEnter()) return;
        super.testFallbackHandler();
    }

    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @Override
    public void testFallbackSingletonHandler() {
        if (isEnter()) return;
        super.testFallbackSingletonHandler();
    }

    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @Override
    public void testFallbackNoHandler() {
        if (isEnter()) return;
        super.testFallbackNoHandler();
    }

    @Override
    protected TextArea createControl() {
        return new TextArea();
    }

}
