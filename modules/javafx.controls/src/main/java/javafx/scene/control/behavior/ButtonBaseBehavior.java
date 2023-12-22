package javafx.scene.control.behavior;

import java.util.Objects;

import javafx.scene.control.BehaviorAspect;
import javafx.scene.control.BehaviorConfiguration;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Standard behavior for {@link ButtonBase}.
 */
public class ButtonBaseBehavior implements Behavior<ButtonBase> {
    private static final BehaviorAspect<ButtonBase, Controller> KEYBOARD_CONTROL_ASPECT = BehaviorAspect.builder(Controller.class, Controller::new)
        .registerEventHandler(KeyEvent.KEY_PRESSED, Controller::keyPressed)
        .registerEventHandler(KeyEvent.KEY_RELEASED, Controller::keyReleased)
        .build();

    private static final BehaviorAspect<ButtonBase, Controller> MOUSE_CONTROL_ASPECT = BehaviorAspect.builder(Controller.class, Controller::new)
        .registerEventHandler(MouseEvent.MOUSE_PRESSED, Controller::mousePressed)
        .registerEventHandler(MouseEvent.MOUSE_RELEASED, Controller::mouseReleased)
        .registerEventHandler(MouseEvent.MOUSE_ENTERED, Controller::mouseEntered)
        .registerEventHandler(MouseEvent.MOUSE_EXITED, Controller::mouseExited)
        .build();

    private static final BehaviorAspect<ButtonBase, Controller> FOCUS_ASPECT = BehaviorAspect.builder(Controller.class, Controller::new)
        .registerPropertyListener(ButtonBase::focusedProperty, Controller::focusChanged)
        .build();

    public static final BehaviorConfiguration<ButtonBase> CONFIGURATION = new BehaviorConfiguration<>(KEYBOARD_CONTROL_ASPECT, MOUSE_CONTROL_ASPECT, FOCUS_ASPECT);

    @Override
    public BehaviorConfiguration<ButtonBase> getConfiguration() {
        return CONFIGURATION;
    }

    /**
     * Controller class for {@link ButtonBaseBehavior}.
     */
    public static class Controller {
        private final ButtonBase control;

        private boolean keyDown;

        /**
         * Constructs a new controller.
         *
         * @param control a control, cannot be {@code null}
         */
        protected Controller(ButtonBase control) {
            this.control = Objects.requireNonNull(control);
        }

        private void keyPressed(KeyEvent event) {
            if (!event.isConsumed() && event.getCode() == KeyCode.SPACE) {
                if (!control.isPressed() && !control.isArmed()) {
                    keyDown = true;
                    control.arm();
                    event.consume();
                }
            }
        }

        private void keyReleased(KeyEvent event) {
            if (!event.isConsumed() && event.getCode() == KeyCode.SPACE) {
                if (keyDown) {
                    keyDown = false;

                    if (control.isArmed()) {
                        control.disarm();
                        control.fire();
                        event.consume();
                    }
                }
            }
        }

        private void focusChanged(boolean focused) {
            if (keyDown && !focused) {
                keyDown = false;
                control.disarm();
            }
        }

        private void mousePressed(MouseEvent event) {
            if (!event.isConsumed() && control.isFocusTraversable()) {
                control.requestFocus();
            }

            // arm the button if it is a valid mouse event
            // Note there appears to be a bug where if I press and hold and release
            // then there is a clickCount of 0 on the release, whereas a quick click
            // has a release clickCount of 1. So here I'll check clickCount <= 1,
            // though it should really be == 1 I think.
            boolean valid = (event.getButton() == MouseButton.PRIMARY &&
                    !(event.isMiddleButtonDown() || event.isSecondaryButtonDown() ||
                            event.isShiftDown() || event.isControlDown() || event.isAltDown() || event.isMetaDown()));

            if (!control.isArmed() && valid) {
                control.arm();
                event.consume();
            }
        }

        /*
         * Invoked when a mouse release has occurred. We determine whether this
         * was done in a manner that would fire the button's action. This happens
         * only if the button was armed by a corresponding mouse press.
         */
        private void mouseReleased(MouseEvent event) {
            // if armed by a mouse press instead of key press, then fire!
            if (!event.isConsumed() && !keyDown && control.isArmed()) {
                control.disarm();
                control.fire();
                event.consume();
            }
        }

        /*
         * Invoked when the mouse enters the Button. If the Button had been armed
         * by a mouse press and the mouse is still pressed, then this will cause
         * the button to be rearmed.
         */
        private void mouseEntered(MouseEvent event) {
            // rearm if necessary
            if (!event.isConsumed() && !keyDown && control.isPressed()) {
                control.arm();
                // event purposely not consumed
            }
        }

        /*
         * Invoked when the mouse exits the Button. If the Button is armed due to
         * a mouse press, then this function will disarm the button upon the mouse
         * exiting it.
         */
        private void mouseExited(MouseEvent event) {
            // Disarm if necessary
            if (!event.isConsumed() && !keyDown && control.isArmed()) {
                control.disarm();
                // event purposely not consumed
            }
        }
    }
}
