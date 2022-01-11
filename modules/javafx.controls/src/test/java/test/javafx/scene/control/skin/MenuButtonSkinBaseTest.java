package test.javafx.scene.control.skin;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.MenuButtonSkinBase;
import javafx.scene.input.Mnemonic;
import javafx.stage.Stage;

public class MenuButtonSkinBaseTest {

    private MenuButton menubutton;
    private MenuItem menuItem;

    @Before
    public void setup() {
        menubutton = new MenuButton();
        menuItem = new MenuItem("Menu Item");
        menubutton.getItems().add(menuItem);
        menubutton.setSkin(new MenuButtonSkinBase<>(menubutton));
    }

    @Test
    public void testNoNullPointerOnRemovingFromTheSceneWhilePopupIsShowing() {
        Thread.UncaughtExceptionHandler originalExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            Assert.fail("No exception expected, but was a " + e);
            e.printStackTrace();
        });

        try {
            Scene scene = new Scene(menubutton);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();

            menubutton.show();
            menuItem.setOnAction(e -> scene.setRoot(new MenuButton()));
            menuItem.fire();

        } finally {
            Thread.currentThread().setUncaughtExceptionHandler(originalExceptionHandler);
        }
    }

    @Test
    public void testMnemonicsRemovedOnRemovingFromTheSceneWhilePopupIsShowing() {
        menuItem.setText("_Menu Item");
        menuItem.setMnemonicParsing(true);

        ObjectProperty<Mnemonic> menuItemMnemonic = new SimpleObjectProperty<>();

        Scene scene = new Scene(menubutton) {
            @Override
            public void addMnemonic(Mnemonic m) {
                if (menuItemMnemonic.get() != null) {
                    // The test is designed for only one mnemonic.
                    Assert.fail("Test failure: More than one Mnemonic registered.");
                }
                menuItemMnemonic.set(m);
                super.addMnemonic(m);
            }

            @Override
            public void removeMnemonic(Mnemonic m) {
                if (m == menuItemMnemonic.get()) {
                    menuItemMnemonic.set(null);
                }
                super.removeMnemonic(m);
            }
        };
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        menubutton.show();
        menuItem.setOnAction(e -> scene.setRoot(new MenuButton()));
        menuItem.fire();

        Assert.assertNull("Mnemonic was not removed from the scene,", menuItemMnemonic.get());
    }
}
