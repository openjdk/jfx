/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.css;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dgrieve
 */
public class StyleManagerTest {
    
    public StyleManagerTest() {
    }
    
    @Before
    public void setUp() {
//        rect = new Rectangle();
//        rect.setId("rectangle");
//
//        text = new Text();
//        text.setId("text");
//
//        Group group = new Group();
//        group.getChildren().addAll(rect, text);
//
//        scene = new Scene(group);/* {
//            TestWindow window;
//            {
//                window = new TestWindow();
//                window.setScene(HonorDeveloperSettingsTest.this.scene);
//                impl_setWindow(window);
//            }
//        };*/
//        
//        System.setProperty("binary.css", "false");
//        String url = getClass().getResource("HonorDeveloperSettingsTest_UA.css").toExternalForm();
//        StyleManager.setDefaultUserAgentStylesheet(url);
//        
//        Stage stage = new Stage();
//        stage.setScene(scene);
//        stage.show();
    }
    
    @Test
    public void testMethod_getStyleManager() {
        Scene scene = new Scene(new Group());
        StyleManager sm = StyleManager.getStyleManager(scene);
        assert (scene == sm.getScene());
    }
    
    @Test
    public void testMethod_getStyleManager_forPopup() {
        Popup popup = new Popup();
        Scene scene = popup.getScene();
        StyleManager sm = StyleManager.getStyleManager(scene);
        assert (scene == sm.getScene());
    }
    
    @Test
    public void testRootStyleManager() {
        StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        Stage stage = new Stage();
        
        Scene rootScene = new Scene(new Group());
        stage.setScene(rootScene);
        
        Popup popup = new Popup();
        popup.show(stage);
        Scene scene = popup.getScene();        

        StyleManager sm = StyleManager.getStyleManager(scene);
        assert (rootScene == sm.getRootScene());    
    }
    
}
