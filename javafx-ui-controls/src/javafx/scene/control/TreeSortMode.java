/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx.scene.control;

/**
 * Specifies how the tree items in tree-like UI controls should be sorted.
 */
public enum TreeSortMode {
    /** 
     * Default; sort all nodes.
     */
    ALL_DESCENDANTS,

    /**
     * Sort first level nodes only regardless of whether the root is 
     * actually being shown or not.
     */
    ONLY_FIRST_LEVEL;
}
