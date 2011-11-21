/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javafx.scene.control;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.skin.SkinBase;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 *
 * @author paru
 */
public class UAStylesheetLoader {
    
    private static class Holder {
        private static UAStylesheetLoader stylesheetLoader = new UAStylesheetLoader();
    }
    
    private static boolean stylesheetLoaded = false;
    
    private UAStylesheetLoader() {}
    
    
    static void doLoad() {
        Holder.stylesheetLoader.loadUAStylesheet();
    }
    
    private void loadUAStylesheet() {
        // Ensures that the caspian.css file is set as the user agent style sheet
        // when the first control or popupcontrol is created.
        if (!stylesheetLoaded) {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                        URL url = SkinBase.class.getResource("caspian/caspian.css");
                        StyleManager.getInstance().setDefaultUserAgentStylesheet(url.toExternalForm());
                        stylesheetLoaded = true;
                    return null;
                }
            });    
        }
    }
}
