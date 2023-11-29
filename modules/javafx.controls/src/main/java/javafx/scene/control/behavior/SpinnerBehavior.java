package javafx.scene.control.behavior;

import java.util.List;
import java.util.function.Predicate;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.scene.AccessibleAction;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.control.skin.AccessibleActionEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Standard behavior for the {@link Spinner} control.
 */
public class SpinnerBehavior implements Behavior<Spinner<?>> {
    private static final SpinnerBehavior INSTANCE = new SpinnerBehavior();

    public static SpinnerBehavior getInstance() {
        return INSTANCE;
    }

    private SpinnerBehavior() {
    }

    @Override
    public void install(BehaviorContext<Spinner<?>> context) {
        State state = new State();

        context.registerKeyPressedHandler(new KeyCodeCombination(KeyCode.UP), SpinnerBehavior::arrowsAreVertical, c -> c.increment(1));
        context.registerKeyPressedHandler(new KeyCodeCombination(KeyCode.DOWN), SpinnerBehavior::arrowsAreVertical, c -> c.decrement(1));
        context.registerKeyPressedHandler(new KeyCodeCombination(KeyCode.RIGHT), Predicate.not(SpinnerBehavior::arrowsAreVertical), c -> c.increment(1));
        context.registerKeyPressedHandler(new KeyCodeCombination(KeyCode.LEFT), Predicate.not(SpinnerBehavior::arrowsAreVertical), c -> c.decrement(1));
        context.registerEventHandler(MouseEvent.MOUSE_PRESSED, state::mousePressed);
        context.registerEventHandler(MouseEvent.MOUSE_RELEASED, state::mouseReleased);
        context.registerEventHandler(AccessibleActionEvent.TRIGGERED, state::accessibleActionTriggered);
        context.registerPropertyListener(Node::sceneProperty, (scene, c) -> state.stopSpinning());
    }

    private class State {
        private boolean isIncrementing;
        private Timeline timeline;

        void mousePressed(MouseEvent event, Spinner<?> control) {
            if (isIncrementArrow(event)) {
                control.requestFocus();
                startSpinning(control, true);
                event.consume();
            }
            else if (isDecrementArrow(event)) {
                control.requestFocus();
                startSpinning(control, false);
                event.consume();
            }
        }

        void mouseReleased(MouseEvent event, Spinner<?> control) {
            if (isIncrementArrow(event) || isDecrementArrow(event)) {
                stopSpinning();
                event.consume();
            }
        }

        void accessibleActionTriggered(AccessibleActionEvent event, Spinner<?> control) {
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

        void startSpinning(Spinner<?> control, boolean increment) {
            isIncrementing = increment;

            if (timeline != null) {
                timeline.stop();
            }

            KeyFrame start = new KeyFrame(Duration.ZERO, e -> step(control));
            KeyFrame repeat = new KeyFrame(control.getRepeatDelay());

            timeline = new Timeline();
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.setDelay(control.getInitialDelay());
            timeline.getKeyFrames().setAll(start, repeat);
            timeline.playFromStart();

            step(control);
        }

        void stopSpinning() {
            if (timeline != null) {
                timeline.stop();
                timeline = null;
            }
        }

        void step(Spinner<?> control) {
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
    }

    private static boolean isIncrementArrow(Event event) {
        if (!(event.getTarget() instanceof Node n)) {
            return false;
        }

        return n.getStyleClass().contains("increment-arrow-button");
    }

    private static boolean isDecrementArrow(Event event) {
        if (!(event.getTarget() instanceof Node n)) {
            return false;
        }

        return n.getStyleClass().contains("decrement-arrow-button");
    }

    private static boolean arrowsAreVertical(Spinner<?> control) {
        List<String> styleClasses = control.getStyleClass();

        return !(
            styleClasses.contains(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL)
                || styleClasses.contains(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL)
                || styleClasses.contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL)
        );
    }
}
