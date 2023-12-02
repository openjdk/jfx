package javafx.scene.control.behavior;

import javafx.scene.control.Button;

/**
 * Standard behavior for {@link Button}.
 */
public class ButtonBehavior implements Behavior<Button> {
    private static final ButtonBehavior INSTANCE = new ButtonBehavior();

    public static ButtonBehavior getInstance() {
        return INSTANCE;
    }

    private ButtonBehavior() {
    }

    @Override
    public StateFactory<? super Button> configure(BehaviorInstaller<? extends Button> installer) {
        return ButtonBaseBehavior.getInstance().configure(installer);
    }
}
