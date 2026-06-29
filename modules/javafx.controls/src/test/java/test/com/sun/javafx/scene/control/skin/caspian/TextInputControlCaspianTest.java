package test.com.sun.javafx.scene.control.skin.caspian;

import com.sun.javafx.tk.Toolkit;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Collection;
import java.util.List;

import static javafx.scene.control.skin.TextInputSkinShim.getPromptNode;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextInputControlCaspianTest {
    private String userAgentStylesheet;
    private Stage stage;

    private static Collection<Class<? extends TextInputControl>> parameters() {
        return List.of(
                TextField.class,
                PasswordField.class,
                TextArea.class
        );
    }

    @BeforeEach
    public void setup() {
        userAgentStylesheet = Application.getUserAgentStylesheet();
        Application.setUserAgentStylesheet(Application.STYLESHEET_CASPIAN);
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) {
            stage.hide();
            stage = null;
        }
        Application.setUserAgentStylesheet(userAgentStylesheet);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void promptTextIsVisibleWhenEmptyFocusedTextInput(Class<? extends TextInputControl> type)
            throws Exception {
        TextInputControl control = type.getDeclaredConstructor().newInstance();
        control.setPromptText("Prompt text");

        StackPane root = new StackPane(control);
        Scene scene = new Scene(root, 400, 200);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();

        control.requestFocus();
        Toolkit.getToolkit().firePulse();
        assertTrue(control.isFocused(), type.getSimpleName() + " should be focused");

        Text promptNode = getPromptNode(control);
        assertNotNull(promptNode, type.getSimpleName() + " should have a prompt node");
        assertTrue(promptNode.isVisible(), type.getSimpleName() + " prompt text should be visible");
        assertNotEquals(Color.TRANSPARENT, promptNode.getFill(),
                type.getSimpleName() + " prompt text fill should not be transparent");
    }
}