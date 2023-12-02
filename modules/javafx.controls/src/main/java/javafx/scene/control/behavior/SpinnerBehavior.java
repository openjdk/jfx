package javafx.scene.control.behavior;

import java.util.List;
import java.util.function.Predicate;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.scene.AccessibleAction;
import javafx.scene.Node;
import javafx.scene.Scene;
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
    private static final KeyHandler<Spinner<?>> KEY_HANDLER;

    static {
        SimpleKeyBinder<Spinner<?>> keyBinder = new SimpleKeyBinder<>();

        // TODO allow to be stateful?
        keyBinder.addBinding(new KeyCodeCombination(KeyCode.UP), SpinnerBehavior::arrowsAreVertical, c -> c.increment(1));
        keyBinder.addBinding(new KeyCodeCombination(KeyCode.DOWN), SpinnerBehavior::arrowsAreVertical, c -> c.decrement(1));
        keyBinder.addBinding(new KeyCodeCombination(KeyCode.RIGHT), Predicate.not(SpinnerBehavior::arrowsAreVertical), c -> c.increment(1));
        keyBinder.addBinding(new KeyCodeCombination(KeyCode.LEFT), Predicate.not(SpinnerBehavior::arrowsAreVertical), c -> c.decrement(1));

        KEY_HANDLER = keyBinder;
    }

    public static SpinnerBehavior getInstance() {
        return INSTANCE;
    }

    private SpinnerBehavior() {
    }

    @Override
    public StateFactory<? super Spinner<?>> configure(BehaviorInstaller<? extends Spinner<?>> installer) {
        installer.registerKeyHandler(KEY_HANDLER);
        installer.registerEventHandler(MouseEvent.MOUSE_PRESSED, State::mousePressed);
        installer.registerEventHandler(MouseEvent.MOUSE_RELEASED, State::mouseReleased);
        installer.registerEventHandler(AccessibleActionEvent.TRIGGERED, State::accessibleActionTriggered);
        installer.registerPropertyListener(Node::sceneProperty, State::sceneChanged);

        return State::new;
    }

    protected static class State {
        private boolean isIncrementing;
        private Timeline timeline;
        private Spinner<?> control;

        public State(Spinner<?> control) {
            this.control = control;
        }

        void mousePressed(MouseEvent event) {
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

        void mouseReleased(MouseEvent event) {
            if (isIncrementArrow(event) || isDecrementArrow(event)) {
                stopSpinning();
                event.consume();
            }
        }

        void accessibleActionTriggered(AccessibleActionEvent event) {
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

        protected void startSpinning(boolean increment) {
            isIncrementing = increment;

            if (timeline != null) {
                timeline.stop();
            }

            KeyFrame start = new KeyFrame(Duration.ZERO, e -> step());
            KeyFrame repeat = new KeyFrame(control.getRepeatDelay());

            timeline = new Timeline();
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.setDelay(control.getInitialDelay());
            timeline.getKeyFrames().setAll(start, repeat);
            timeline.playFromStart();

            step();
        }

        void sceneChanged(Scene scene) {
            stopSpinning();
        }

        protected void stopSpinning() {
            if (timeline != null) {
                timeline.stop();
                timeline = null;
            }
        }

        protected void step() {
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

    protected static boolean isIncrementArrow(Event event) {
        if (!(event.getTarget() instanceof Node n)) {
            return false;
        }

        return n.getStyleClass().contains("increment-arrow-button");
    }

    protected static boolean isDecrementArrow(Event event) {
        if (!(event.getTarget() instanceof Node n)) {
            return false;
        }

        return n.getStyleClass().contains("decrement-arrow-button");
    }

    protected static boolean arrowsAreVertical(Spinner<?> control) {
        List<String> styleClasses = control.getStyleClass();

        return !(
            styleClasses.contains(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL)
                || styleClasses.contains(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL)
                || styleClasses.contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL)
        );
    }
}
