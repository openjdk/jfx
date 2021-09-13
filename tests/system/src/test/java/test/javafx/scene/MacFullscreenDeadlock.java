package test.javafx.scene;

import junit.framework.Assert;
import org.junit.Test;
import javax.swing.*;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.embed.swing.JFXPanel;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MacFullscreenDeadlock {

    JFrame jframe;
    VBox swingVBox;
    CountDownLatch finishedLatch = new CountDownLatch(1);

    @Test
    public void JDK_8273485() {
        JFrame jframe = new JFrame();

        JFXPanel jfxpanel = new JFXPanel();
        Platform.runLater(() -> {
            swingVBox = new VBox();
            Scene scene = new Scene(swingVBox, 500,500);
            jfxpanel.setScene(scene);
        });

        jframe.add(jfxpanel);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.setSize(300,300);
        jframe.show();

        Platform.runLater(() -> {
            createJavaFXDialogAndClose();
        });

        try {
            Assert.assertTrue("Timeout, JavaFX-Thread blocked.", finishedLatch.await(15, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void createJavaFXDialogAndClose() {
        VBox pin = new VBox();
        Scene scene = new Scene(pin, 200,200);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> {
            stage.setFullScreen(true);
            Platform.runLater(() -> {
                swingVBox.getChildren().add(new Button());
                stage.close();

                Platform.runLater (() -> {
                    finishedLatch.countDown();
                });
            });
        });
    }
}
