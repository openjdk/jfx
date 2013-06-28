/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package modena;

import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.shape.SVGPath;

/**
 * Helper class for creating tree views for testing
 */
public class SamplePageTreeHelper {
    
    private static final String RSS = "M13.33,13.334h-1.693c0-4.954-4.016-8.97-8.97-8.97V2.66"
            + "9l0.243-0.002c5.757,0,10.423,4.667,10.423,10.425L13.33,13.334z M9.45,13.334H7."
            + "758c0-2.812-2.279-5.091-5.091-5.091V6.551l0.243-0.005c3.615,0,6.545,2.93,6.545"
            + ",6.546L9.45,13.334z M2.667,11.878c0-0.802,0.651-1.455,1.455-1.455c0.803,0,1.45"
            + "4,0.653,1.454,1.455c0,0.804-0.651,1.456-1.454,1.456C3.318,13.334,2.667,12.682,"
            + "2.667,11.878z M1.6,0C0.716,0,0,0.716,0,1.6v12.8C0,15.283,0.716,16,1.6,16H14.4c"
            + "0.885,0,1.6-0.717,1.6-1.6V1.6C16,0.716,15.285,0,14.4,0H1.6z";
    private static final String CLOUD = "M8.972,8.088h1.91v2.39l1.433-0.956l1.06,0.986l-3.448"
            + ",3.313l-3.418-3.299l1.03-1l1.434,0.956V8.088z M-0.104,12.685c0,2.211,1.563,4.0"
            + "03,3.489,4.003c0.112,0,12.275,0,12.275,0c2.382-0.044,4.299-2.089,4.299-4.607c0"
            + "-2.542-1.961-4.605-4.379-4.605l-0.173,0.002c-0.673-2.396-3.037-4.165-5.849-4.1"
            + "65c-3.268,0-5.931,2.389-6.037,5.374L3.385,8.682C1.459,8.682-0.104,10.475-0.104"
            + ",12.685z";
    
    private static Node createRSS() { 
        SVGPath sp = new SVGPath();
        sp.setContent(RSS);
        return sp;
    }
    
    private static Node createCLOUD() { 
        SVGPath sp = new SVGPath();
        sp.setContent(CLOUD);
        return sp;
    }
    
    static TreeView createTreeView(int width) {
        final TreeItem<String> root = new TreeItem<String>("Root node");
        final TreeItem<String> childNode1 = new TreeItem<String>("Child Node 1", createCLOUD());
        final TreeItem<String> childNode2 = new TreeItem<String>("Child Node 2", createCLOUD());
        final TreeItem<String> childNode3 = new TreeItem<String>("Child Node 3", createCLOUD());
        final TreeItem<String> childNode4 = new TreeItem<String>("Child Node 4", createRSS());
        final TreeItem<String> childNode5 = new TreeItem<String>("Child Node 5", createRSS());
        final TreeItem<String> childNode6 = new TreeItem<String>("Child Node 6", createRSS());
        final TreeItem<String> childNode7 = new TreeItem<String>("Child Node 7", createRSS());
        final TreeItem<String> childNode8 = new TreeItem<String>("Child Node 8", createRSS());
        final TreeItem<String> childNode9 = new TreeItem<String>("Child Node 9", createRSS());
        final TreeItem<String> childNode10 = new TreeItem<String>("Child Node 10");
        final TreeItem<String> childNode11 = new TreeItem<String>("Child Node 11");
        final TreeItem<String> childNode12 = new TreeItem<String>("Child Node 12");
        final TreeItem<String> childNode13 = new TreeItem<String>("Child Node 13");
        final TreeItem<String> childNode14 = new TreeItem<String>("Child Node 14");
        final TreeItem<String> childNode15 = new TreeItem<String>("Child Node 15");
        final TreeItem<String> childNode16 = new TreeItem<String>("Child Node 16");
        final TreeItem<String> childNode17 = new TreeItem<String>("Child Node 17");
        final TreeItem<String> childNode18 = new TreeItem<String>("Child Node 18");
        final TreeItem<String> childNode19 = new TreeItem<String>("Child Node 19");
        final TreeItem<String> childNode20 = new TreeItem<String>("Child Node 20");
        final TreeItem<String> childNode21 = new TreeItem<String>("Child Node 21");
    
        root.setExpanded(true);
        root.getChildren().setAll(childNode1, childNode2, childNode3);
        childNode3.setExpanded(true);
        childNode3.getChildren().setAll(childNode4, childNode5, childNode6,
                childNode7, childNode8, childNode9,
                childNode10, childNode11, childNode12,
                childNode13, childNode14, childNode15,
                childNode16, childNode17, childNode18,
                childNode19, childNode20, childNode21);
        
        final TreeView treeView = new TreeView(root);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setShowRoot(true);
        treeView.setPrefSize(width, 300);
        treeView.getSelectionModel().selectRange(5, 8);
        return treeView;
    }
}
