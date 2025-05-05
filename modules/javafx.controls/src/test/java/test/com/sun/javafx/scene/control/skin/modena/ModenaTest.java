package test.com.sun.javafx.scene.control.skin.modena;

import com.sun.javafx.tk.Toolkit;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ModenaTest {

    private static Collection<Class> parameters() {
        return List.of(
                TextField.class,
                PasswordField.class,
                TextArea.class
        );
    }

    private TextInputControl textInput;

    //@BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Class<?> type) {
        setUncaughtExceptionHandler();
        try {
            textInput = (TextInputControl)type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            fail(e);
        }
    }

    @AfterEach
    public void cleanup() {
        removeUncaughtExceptionHandler();
    }

    private void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    private void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    /******************************************************
     * Test for highlight-text-fill                       *
     *****************************************************/

    @ParameterizedTest
    @CsvSource({
            "#999999, 333333",
            "#222222, ffffff",
            "#777777, 000000"
    })
    public void testHighlightTextInput(String accentColor, String expectedTextColor) {
        for (Class<?> type : parameters()) {
            setup(type);
            textInput.setText("This is a text");
            textInput.selectAll();

            StackPane root = new StackPane(textInput);
            Scene scene = new Scene(root, 400, 200);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();

            scene.getRoot().setStyle("-fx-accent: " + accentColor + ";");
            textInput.requestFocus();
            Toolkit.getToolkit().firePulse();

            Text internalText = (Text) textInput.lookup(".text");
            Paint fill = internalText.getSelectionFill();

            String resolved = fill.toString().toLowerCase();
            assertTrue(resolved.contains(expectedTextColor));
        }
    }
}
