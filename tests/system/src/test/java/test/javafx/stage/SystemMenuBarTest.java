package test.javafx.stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;
import test.util.memory.JMemoryBuddy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SystemMenuBarTest {
    @BeforeClass
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.setImplicitExit(false);

        Util.startup(startupLatch, () -> {
            startupLatch.countDown();
        });
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    CountDownLatch menubarLatch = new CountDownLatch(1);
    AtomicBoolean failed = new AtomicBoolean(false);

    @Test
    public void testFailingMenuBar() throws InterruptedException {
        Util.runAndWait(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t,e) -> {
                e.printStackTrace();
                failed.set(true);
            });
            createMenuBarStage();
        });

        menubarLatch.await();

        System.err.println("FAILED IS: " + failed.get());
        assertFalse(failed.get());
    }

    public void createMenuBarStage() {
        Stage stage = new Stage();
        VBox root = new VBox();

        root.getChildren().add(createFailingMenuBar());

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public MenuBar createFailingMenuBar() {
        MenuBar menuBar = new MenuBar();

        menuBar.setUseSystemMenuBar(true);

        Menu systemMenu = new Menu("systemMenu");
        menuBar.getMenus().add(systemMenu);

        var newItem = new MenuItem();
        newItem.setVisible(false);
        systemMenu.getItems().add(newItem);

        Platform.runLater(() -> {
            javafx.scene.control.Menu systemMenuContributions = new Menu("123");
            systemMenu.getItems().add(systemMenuContributions);
            menubarLatch.countDown();
        });

        return menuBar;
    }
}
