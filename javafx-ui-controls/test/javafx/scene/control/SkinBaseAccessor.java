/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx.scene.control;

import java.util.List;
import javafx.scene.Node;

/**
 *
 * @author Jonathan
 */
public class SkinBaseAccessor {
    
    public static List<Node> getChildren(SkinBase skin) {
        return skin.getChildren();
    }
}
