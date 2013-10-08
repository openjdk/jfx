package javafx.scene.control;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Created with IntelliJ IDEA.
 * User: dgrieve
 * Date: 9/30/13
 * Time: 2:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class MiscellaneousTests {

    @Test
    public void test_RT_31168() {
        //
        // Make sure that a control added and removed from the scene-graph before css is processed
        // gets css processed when it is added back in.

        Button button = new Button("RT-31168");
        Rectangle rectangle = new Rectangle(50,50);

        Group container = new Group();
        container.getChildren().add(rectangle);

        Scene scene = new Scene(new Group(container, new Button("button")));

        //
        // Gotta put this in a window for the pulse listener to get hooked up (see Scene#impl_initPeer().
        // Need the pulse listener since we want to enter root via Scene#doCSSPass()
        //
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        //
        // Has to be a pulse since we want to enter from Scene.doCSSPass()
        //
        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        container.getChildren().set(0, button);
        container.getChildren().set(0, rectangle);

        //
        // Has to be a pulse since we want to enter from Scene.doCSSPass()
        //
        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        // Should be null since the button was added and removed before the pulse processed css
        assertNull(button.getBackground());

        container.getChildren().set(0, button);

        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        // Should no longer be null
        assertNotNull(button.getBackground());

    }

    @Test public void test_RT_33103() {

        HBox box = new HBox();

        TextField field = new TextField();
        Label badLabel = new Label("Field:", field);

        box.getChildren().addAll(badLabel, field);

        Scene scene = new Scene(box);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        assertSame(badLabel, field.getParent());

    }
}
