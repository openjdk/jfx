package javafx.scene.control.behavior;

import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Standard behavior for the {@link Button} control.
 */
public class ButtonBehavior implements Behavior<Button> {
    private static final ButtonBehavior INSTANCE = new ButtonBehavior();

    public static ButtonBehavior getInstance() {
        return INSTANCE;
    }

    private ButtonBehavior() {
    }

    @Override
    public void install(BehaviorContext<Button> context) {
        State state = new State();

        context.registerPropertyListener(Button::focusedProperty, state::focusChanged);

        context.registerEventHandler(KeyEvent.KEY_PRESSED, state::keyPressed);
        context.registerEventHandler(KeyEvent.KEY_RELEASED, state::keyReleased);
        context.registerEventHandler(MouseEvent.MOUSE_PRESSED, state::mousePressed);
        context.registerEventHandler(MouseEvent.MOUSE_RELEASED, state::mouseReleased);
        context.registerEventHandler(MouseEvent.MOUSE_ENTERED, state::mouseEntered);
        context.registerEventHandler(MouseEvent.MOUSE_EXITED, state::mouseExited);
    }

    private static class State {
        private boolean keyDown;

        protected void keyPressed(KeyEvent event, Button button) {
            if (!event.isConsumed() && event.getCode() == KeyCode.SPACE) {
                if (!button.isPressed() && !button.isArmed()) {
                    keyDown = true;
                    button.arm();
                    event.consume();
                }
            }
        }

        protected void keyReleased(KeyEvent event, Button button) {
            if (!event.isConsumed() && event.getCode() == KeyCode.SPACE) {
                if (keyDown) {
                    keyDown = false;

                    if (button.isArmed()) {
                        button.disarm();
                        button.fire();
                        event.consume();
                    }
                }
            }
        }

        protected void focusChanged(boolean focused, Button button) {
            if (keyDown && !button.isFocused()) {
                keyDown = false;
                button.disarm();
            }
        }

        protected void mousePressed(MouseEvent event, Button button) {
            if (!event.isConsumed() && button.isFocusTraversable()) {
                button.requestFocus();
            }

            // arm the button if it is a valid mouse event
            // Note there appears to be a bug where if I press and hold and release
            // then there is a clickCount of 0 on the release, whereas a quick click
            // has a release clickCount of 1. So here I'll check clickCount <= 1,
            // though it should really be == 1 I think.
            boolean valid = (event.getButton() == MouseButton.PRIMARY &&
                    !(event.isMiddleButtonDown() || event.isSecondaryButtonDown() ||
                            event.isShiftDown() || event.isControlDown() || event.isAltDown() || event.isMetaDown()));

            if (!button.isArmed() && valid) {
                button.arm();
                event.consume();
            }
        }

        /**
         * Invoked when a mouse release has occurred. We determine whether this
         * was done in a manner that would fire the button's action. This happens
         * only if the button was armed by a corresponding mouse press.
         */
        protected void mouseReleased(MouseEvent event, Button button) {
            // if armed by a mouse press instead of key press, then fire!
            if (!event.isConsumed() && !keyDown && button.isArmed()) {
                button.disarm();
                button.fire();
                event.consume();
            }
        }

        /**
         * Invoked when the mouse enters the Button. If the Button had been armed
         * by a mouse press and the mouse is still pressed, then this will cause
         * the button to be rearmed.
         */
        protected void mouseEntered(MouseEvent event, Button button) {
            // rearm if necessary
            if (!event.isConsumed() && !keyDown && button.isPressed()) {
                button.arm();
                // event purposely not consumed
            }
        }

        /**
         * Invoked when the mouse exits the Button. If the Button is armed due to
         * a mouse press, then this function will disarm the button upon the mouse
         * exiting it.
         */
        protected void mouseExited(MouseEvent event, Button button) {
            // Disarm if necessary
            if (!event.isConsumed() && !keyDown && button.isArmed()) {
                button.disarm();
                // event purposely not consumed
            }
        }
    }
}
