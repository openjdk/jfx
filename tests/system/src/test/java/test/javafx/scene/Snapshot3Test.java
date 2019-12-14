package test.javafx.scene;


import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import org.junit.*;
import test.util.Util;

import static org.junit.Assert.*;

/**
 * Tests for tiled snapshots (i.e. capturing snapshots larger than maxTextureSize. See JDK-8088198)
 */
public class Snapshot3Test extends SnapshotCommon {

    public static int VALUE_LARGER_THAN_TEXTURE_SIZE = 40000;

    @BeforeClass
    public static void setupOnce() {
        doSetupOnce();
    }

    @AfterClass
    public static void teardownOnce() {
        doTeardownOnce();
    }

    @Before
    public void setupEach() {
        assertNotNull(myApp);
        assertNotNull(myApp.primaryStage);
        assertTrue(myApp.primaryStage.isShowing());
    }

    @After
    public void teardownEach() {
    }

    Rectangle rect = new Rectangle(1, 1);

    @Test
    public void testTiledWidthSnapshot() {
        Util.runAndWait(() -> {
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(new Scale(VALUE_LARGER_THAN_TEXTURE_SIZE, 1));
            Image image = rect.snapshot(params, null);
            assertEquals(VALUE_LARGER_THAN_TEXTURE_SIZE, (int) image.getWidth());
        });
    }

    @Test
    public void testTiledHeightSnapshot() {
        Util.runAndWait(() -> {
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(new Scale(1, VALUE_LARGER_THAN_TEXTURE_SIZE));
            Image image = rect.snapshot(params, null);
            assertEquals(VALUE_LARGER_THAN_TEXTURE_SIZE, (int) image.getHeight());
        });
    }

}


