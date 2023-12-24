package javafx.scene.control.behavior;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.scene.AccessibleAction;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.BehaviorAspect;
import javafx.scene.control.BehaviorConfiguration;
import javafx.scene.control.Spinner;
import javafx.scene.control.skin.AccessibleActionEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Standard behavior for the {@link Spinner} control.
 */
public class SpinnerBehavior implements Behavior<Spinner<?>> {
    private static final BehaviorAspect<Spinner<?>, Controller> KEYBOARD_CONTROL_ASPECT = BehaviorAspect.builder(Controller.class, Controller::new)
        .registerKeyHandler(new SimpleKeyBinder()
            .addBinding(new KeyCodeCombination(KeyCode.UP), Controller::arrowsAreVertical, Controller::increment)
            .addBinding(new KeyCodeCombination(KeyCode.DOWN), Controller::arrowsAreVertical, Controller::decrement)
            .addBinding(new KeyCodeCombination(KeyCode.RIGHT), Predicate.not(Controller::arrowsAreVertical), Controller::increment)
            .addBinding(new KeyCodeCombination(KeyCode.LEFT), Predicate.not(Controller::arrowsAreVertical), Controller::decrement)
        )
        .build();

    private static final BehaviorAspect<Spinner<?>, Controller> MOUSE_CONTROL_ASPECT = BehaviorAspect.builder(Controller.class, Controller::new)
        .registerEventHandler(MouseEvent.MOUSE_PRESSED, Controller::mousePressed)
        .registerEventHandler(MouseEvent.MOUSE_RELEASED, Controller::mouseReleased)
        .registerPropertyListener(n -> n.sceneProperty().flatMap(Scene::windowProperty).flatMap(Window::showingProperty).orElse(false), Controller::showingChanged)
        .build();

    private static final BehaviorAspect<Spinner<?>, Controller> ACCESSIBILITY_ASPECT = BehaviorAspect.builder(Controller.class, Controller::new)
        .registerEventHandler(AccessibleActionEvent.TRIGGERED, Controller::accessibleActionTriggered)
        .build();

    /**
     * A {@link BehaviorConfiguration} for {@link Spinner}s.
     */
    public static final BehaviorConfiguration<Spinner<?>> CONFIGURATION = BehaviorConfiguration.<Spinner<?>>builder()
        .add(KEYBOARD_CONTROL_ASPECT)
        .add(MOUSE_CONTROL_ASPECT)
        .add(ACCESSIBILITY_ASPECT)
        .build();

    @Override
    public BehaviorConfiguration<Spinner<?>> getConfiguration() {
        return CONFIGURATION;
    }

    /**
     * Controller class for {@link SpinnerBehavior}.
     */
    public static class Controller {
        private final Spinner<?> control;

        private Timeline timeline;  // stopped automatically when showing status changes

        /**
         * Creates a new instance.
         *
         * @param control a control, cannot be {@code null}
         */
        protected Controller(Spinner<?> control) {
            this.control = Objects.requireNonNull(control);
        }

        private void mousePressed(MouseEvent event) {
            if (isIncrementArrow(event)) {
                control.requestFocus();
                startSpinning(true);
                event.consume();
            }
            else if (isDecrementArrow(event)) {
                control.requestFocus();
                startSpinning(false);
                event.consume();
            }
        }

        private void mouseReleased(MouseEvent event) {
            if (isIncrementArrow(event) || isDecrementArrow(event)) {
                stopSpinning();
                event.consume();
            }
        }

        private void accessibleActionTriggered(AccessibleActionEvent event) {
            if (event.getAction() == AccessibleAction.FIRE) {
                if (isIncrementArrow(event)) {
                    control.increment(1);
                    event.consume();
                }
                else if (isDecrementArrow(event)) {
                    control.decrement(1);
                    event.consume();
                }
            }
        }

        private void showingChanged(boolean showing) {
            if (!showing) {
                stopSpinning();
            }
        }

        private void startSpinning(boolean increment) {
            if (timeline != null) {
                timeline.stop();
            }

            KeyFrame start = new KeyFrame(Duration.ZERO, e -> step(increment));
            KeyFrame repeat = new KeyFrame(control.getRepeatDelay());

            timeline = new Timeline();
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.setDelay(control.getInitialDelay());
            timeline.getKeyFrames().setAll(start, repeat);
            timeline.playFromStart();

            step(increment);
        }

        private void stopSpinning() {
            if (timeline != null) {
                timeline.stop();
                timeline = null;
            }
        }

        private void step(boolean isIncrementing) {
            if (control.getValueFactory() == null) {
                return;
            }

            if (isIncrementing) {
                control.increment(1);
            }
            else {
                control.decrement(1);
            }
        }

        private void increment() {
            control.increment(1);
        }

        private void decrement() {
            control.decrement(1);
        }

        private boolean arrowsAreVertical() {
            List<String> styleClasses = control.getStyleClass();

            return !(
                styleClasses.contains(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL)
                    || styleClasses.contains(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL)
                    || styleClasses.contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL)
            );
        }
    }

    private static boolean isIncrementArrow(Event event) {
        if (event.getTarget() instanceof Node n) {
            return n.getStyleClass().contains("increment-arrow-button");
        }

        return false;
    }

    private static boolean isDecrementArrow(Event event) {
        if (event.getTarget() instanceof Node n) {
            return n.getStyleClass().contains("decrement-arrow-button");
        }

        return false;
    }
}
