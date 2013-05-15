/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.accessible;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import com.sun.javafx.Logging;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.AccessibleStageProvider;
import com.sun.javafx.accessible.utils.NavigateDirection;
import com.sun.javafx.accessible.utils.PropertyIds;
import com.sun.javafx.accessible.utils.Rect;
import sun.util.logging.PlatformLogger;

/**
 *
 * Maintains the correspondence of native object to node 
 * Walks through the scene graph and initializes the accessible hierarchy
 */

public class AccessibleStage implements AccessibleProvider,
                                        AccessibleStageProvider {
    Stage stage ;
    Scene scene ;
    Object accRoot ; // corresponding glass object
    List<AccessibleNode> accChildren ;
    
    /**
     * Constructor
     * 
     * @param stage The FX stage.
     */
    public AccessibleStage(Stage stage)
    {
        //com.sun.javafx.Logging.getAccessibilityLogger().setLevel(PlatformLogger.FINEST);
        this.stage = stage;
        this.scene = stage.getScene();
        initialize();
    }   

    /** AccessibleStage : Initialize ()
      * On instantiation it initializes the Accessible Hierarchy for all Nodes in the Scene 
      * graphs associated with the Window->Stage->Scene.
      * For native object handle it makes a down call to bridge to create its 
      * associated native accessible component and enters its value in the AccessibleMap.
      * Based upon the type of control it instantiates its accessible behavior 
      * class and enters it into AccessibleMap. The behavior classes are singleton 
      * objects for a type of control.
      * Attaches a listener for any new nodes being added to the scene graph.
      * Attaches a listener for any new Stage, Scene added to the window, which 
      * in return will add a listener to Scene for new node additions.
      * Attaches a Focus listener for tracking change of Focus.
      */    
    private void initialize()
    {
        Parent pRoot = scene.getRoot() ; 
        accRoot = stage.impl_getPeer().accessibleCreateStageProvider(this);
        
        // Note move it to a func later or redo the logic for first , the same is being used when scene graph is modified too
        // Rethink this logic in future
        AccessibleNode dummyRoot = new AccessibleNode(pRoot.getChildrenUnmodifiable().get(0));
        try {
            initAccessibleHierarchy(pRoot, dummyRoot) ;
        } catch (Exception ex) {}
        accChildren = dummyRoot.children;

        // Parent for all first child is AccStage, this is done by setting it to null
        // Hence assign it here
        for( int idx=0;idx<accChildren.size();idx++)
            accChildren.get(idx).parent = null ;
        
        //printAccHierarchy(accChildren);
        // Notes: Attach listener when scene graph changes
                
        scene.getRoot().getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {

            @Override
            public void onChanged(Change<? extends Node> c) {
                // Destroy first ? or only add and modify the new ones ?    
                // Worth creating dynamic updatable tree if we plan to stick to this model
                // If the ally implementation will be added on the controls directly 
                // Leave this as is for now : based upon above decision
                // Destroy and recreate for now
               /* destroyHierarchy(accChildren);
                
                Parent pRoot = scene.getRoot() ;
                // Note move it to a func later or redo the logic for first
                AccessibleNode dummyRoot = new AccessibleNode(pRoot.getChildrenUnmodifiable().get(0));
                try {
                    initAccessibleHierarchy(pRoot, dummyRoot) ;
                } catch (Exception ex) {}
                accChildren = dummyRoot.children;
                setParent(accChildren);
                * 
                */
            }
        });
    }
 
    // TODO: This method is not currently in use.  When it is in use should its
    //       access be package-private like it currently is?
    void destroyHierarchy(List<AccessibleNode> currChildren)
    {
        PlatformLogger logger = Logging.getAccessibilityLogger();
        for (int idx=0; idx<currChildren.size(); idx++) {
            if (logger.isLoggable(PlatformLogger.FINER)) {
                logger.finer(this.toString() + "destroyHierarchy: idx=" + idx + currChildren.get(idx));
            }
            if ( currChildren.get(idx).children.size() > 0 ) {
                if (logger.isLoggable(PlatformLogger.FINER)) {
                    logger.finer(this.toString() + "destroyHierarchy: Has Children" + currChildren.get(idx).children);
                }
                destroyHierarchy(currChildren.get(idx).children) ;
            }
            // destroy native
            stage.impl_getPeer().accessibleDestroyBasicProvider(currChildren.get(idx).accElement);
           // currChildren.get(idx).accElement.destroyAccessible(); 
        }
    }
    
    void printAccHierarchy(List<AccessibleNode> currChildren)
    {
        PlatformLogger logger = Logging.getAccessibilityLogger();        
        for (int idx=0; idx<currChildren.size(); idx++) {
            if (logger.isLoggable(PlatformLogger.FINER)) {
                logger.finer(this.toString() + "printAccHierarchy: idx=" + idx + currChildren.get(idx));
            }
            if ( currChildren.get(idx).children.size() > 0 ) {
                if (logger.isLoggable(PlatformLogger.FINER)) {
                    logger.finer(this.toString() + "printAccHierarchy: Has Children" + currChildren.get(idx).children);
                }
                printAccHierarchy(currChildren.get(idx).children) ;
            }
        }
    }

    // pRoot is the FX node currently being expanded
    // parent is the peer accessible of either this node or the closest ancestor that has a peer accessible.
    private void initAccessibleHierarchy(Parent pRoot, AccessibleNode parent) throws ClassNotFoundException
    {
        Node n;
        List<AccessibleNode> currChildren  = new ArrayList<AccessibleNode>();
        AccessibleNode curaccNode = null;
        ObservableList<Node> nodes = pRoot.getChildrenUnmodifiable();
        PlatformLogger logger = Logging.getAccessibilityLogger();        
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer(this.toString()+ "initAccessibleHierarchy1: pRoot=" + pRoot + " parent=" + parent);
        }
        if (nodes.isEmpty()) {
            if (logger.isLoggable(PlatformLogger.FINEST)) {
                logger.finer(this.toString()+ "initAccessibleHierarchy: no child, pRoot=" + pRoot);
            }
            return;
        }
        for (int idx = 0; idx < nodes.size(); idx++) {
            n = nodes.get(idx);
            if (logger.isLoggable(PlatformLogger.FINER)) {
                logger.finer(this.toString()+ "initAccessibleHierarchy: idx=" + idx + " node= " + n);
            }
            // use the logic for instanceof later when public API is reviewed. For now use reflection 
            try {
                Method method = n.getClass().getMethod("impl_getAccessible");
                if( method != null)
                    curaccNode = (AccessibleNode)method.invoke(n) ;
                /* if( n instanceof Accessible ) { // use this logic later when public API is reviewed. For now use reflection above
                * Accessible aN = (Accessible)n;
                curaccNode = (AccessibleNode)aN.impl_getAccessible() ; */
                if ( curaccNode != null ) { // Control has an accessible implementation
                    if (logger.isLoggable(PlatformLogger.FINER)) {
                        logger.finer(this.toString()+ "initAccessibleHierarchy: Found Accessible.");
                        logger.finer(this.toString()+ "  node= " + n + " curaccNode=" + curaccNode);
                        logger.finer(this.toString()+ "  control type=" + curaccNode.getPropertyValue(PropertyIds.CONTROL_TYPE));
                    }
                    // get native accessible for this node from glass
                  //  curaccNode.accElement = AccessibleBaseProvider.createProvider(curaccNode);
                    curaccNode.accElement = stage.impl_getPeer().accessibleCreateBasicProvider(curaccNode);
                    curaccNode.accController = this;
                    currChildren.add(curaccNode);
                 }
            } catch (Exception ex) {}
            if (n instanceof Parent) {
                if (curaccNode == null) {
                    curaccNode = parent;
                }
                if (logger.isLoggable(PlatformLogger.FINER)) {
                    logger.finer(this.toString()+ "initAccessibleHierarchy: idx=" + idx + " accNode.children= " + curaccNode);
                }
                try {
                initAccessibleHierarchy((Parent)n, curaccNode);    
                } catch (Exception ex) {}
            }
        }
        if (!currChildren.isEmpty()) {
            if (!parent.children.isEmpty()) {
                parent.children.addAll(currChildren);    
            } else {
                parent.children = currChildren;
            }
            for (int idx=0; idx<currChildren.size(); idx++) {
                currChildren.get(idx).parent = parent ;
            }
        }
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer(this.toString()+ "initAccessibleHierarchy: parent.children= " + parent.children );
        }
    }
    
    private void setParent(List<AccessibleNode> accTempChild)
    {
        if( accTempChild.size() <=0 ) return ;
        for (int idx=0; idx<accTempChild.size(); idx++)
        {
            if(accTempChild.get(idx).children.size() > 0 ) // has children
            {
                accTempChild.get(idx).parent = accTempChild.get(idx) ;
                setParent(accTempChild.get(idx).children);
            }
        }
    }
        
    /**
     * Gets a base provider for this element.
     *
     * @return the base provider, or null.
     */
    @Override
    public AccessibleProvider hostRawElementProvider() 
    {
        return this ;
    }

    /**
     * Retrieves an object that provides support for a control pattern on a UI Automation
     *      element.
     *
     * @param patternId identifier of the pattern.
     * 
     * @return Object that implements the pattern interface, or null if the pattern is not
     *      supported.
     */
    @Override
    public Object getPatternProvider(int patternId)
    {
        return this ;
    }

    /**
     * Retrieves the value of a property supported by the UI Automation provider.
     * 
     * @param propertyId    The property identifier.
     * 
     * @return The property value, or a null if the property is not supported by this
     *      provider, or System.Windows.Automation.AutomationElementIdentifiers.NotSupported
     *      if it is not supported at all.
     */
    @Override
    public Object getPropertyValue(int propertyId)
    {
        if (propertyId == PropertyIds.NAME)
           return stage.getTitle() ;
        else
            return null;
    }

    //////////////////////////////
    // AccessibleProvider
    //////////////////////////////
      
    /**
     * Get the bounding rectangle of this element.
     *
     * @return the bounding rectangle, in screen coordinates.
     */
    
    @Override
    public Rect boundingRectangle() {
        // The scene's x/y is window relative so need to get the window and ask for
        // its x/y and then calculate the scene's screen coordinates.  These are
        // relative to the upper left corner.  Some platforms, like OS X, use an 
        // origin of the lower left corner so adjustments may be required depeding
        // on the plaform.
        // Should the y value include the scene offset (the offset below the title bar).
        double x =
            Screen.getPrimary().getBounds().getMinX() + stage.getX() + scene.getX();
        double y =
            Screen.getPrimary().getBounds().getMinY() + stage.getY() + scene.getY();
        return new Rect(x, y, scene.getWidth(), scene.getHeight());
    }
    
    /**
     * Get the root node of the fragment.
     * 
     * @return the root node.
     */
    @Override
   // public AccessibleStageProvider fragmentRoot() {
    public Object fragmentRoot() {
        return accRoot; 
    }

    /**
     * Get an array of fragment roots that are embedded in the UI Automation
     * element tree rooted at the current element.
     * 
     * @return an array of root fragments, or null.
     */
    @Override
    public AccessibleProvider[] getEmbeddedFragmentRoots() {
        return null; // add code
    }

    /**
     * Get the runtime identifier of an element.
     * 
     * @return the unique run-time identifier of the element.
     */
    @Override
    public int[] getRuntimeId() {
        return null; // add code
    }
    
    /**
     * Get the UI Automation element in a specified direction within the tree.
     * 
     * @param direction the direction in which to navigate.
     * 
     * @return the element in the specified direction, or null if there is no element
     *         in that direction
     */
    @Override
    public Object navigate(NavigateDirection direction) {
        PlatformLogger logger = Logging.getAccessibilityLogger();
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer("this: " + this.toString());
            logger.finer("navigate direction: " + direction);
        }
        AccessibleNode accTemp = null;
        switch (direction) {
            case Parent:
            case NextSibling: // root doesnt have sibblings or parent
            case PreviousSibling:
                return accTemp ;
            case FirstChild:
                if( accChildren.size() > 0 )
                accTemp = accChildren.get(0) ;
                break;
            case LastChild:
                if( accChildren.size() > 0 )
                accTemp = accChildren.get(accChildren.size()-1) ;
                break;
        }
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer("returning: " + accTemp.accElement);
        }
        return accTemp.accElement;
    }
    
    /**
     * Set the focus to this element.
     */
    @Override
    public void setFocus() { //TODO: check logic later
//        stage.getScene().setImpl_focusOwner(stage.getScene().getRoot());
        stage.getScene().getRoot().requestFocus();
    }
    
    //////////////////////////////////
    // AccessibleStageProvider
    //////////////////////////////////
    private AccessibleNode getProviderFromPoint(List<AccessibleNode> accTempChild,
            double x, double y)
    {
        double tmpY ;
        if(accTempChild.get(0).children.size() == 0 ) // no children
        {
             tmpY = y - accTempChild.get(0).boundingRectangle().getMaxY() ;
             if(accTempChild.get(0).contains(x, tmpY)) 
           // if(accTempChild.get(0).contains(x, y)) 
                return accTempChild.get(0) ;
        }
        for (int idx=0; idx<accTempChild.size(); idx++)
        {
             tmpY = y - accTempChild.get(idx).boundingRectangle().getMaxY() ;
             if(accTempChild.get(idx).contains(x, tmpY)) 
                return accTempChild.get(idx) ;
            if(accTempChild.get(idx).children.size() > 0 ) // has children
            {
                getProviderFromPoint(accTempChild.get(idx).children, x, y);
            }
        }
        return null ;
    }

    
    @Override
    public Object elementProviderFromPoint(double x, double y) {
        // hit testing
        // return the control at the location asked
        PlatformLogger logger = Logging.getAccessibilityLogger();
        AccessibleNode aNode = getProviderFromPoint( accChildren, x, y ) ;
        if( aNode !=null)
        {  
            if (logger.isLoggable(PlatformLogger.FINER)) {
                logger.finer(this.toString()+ "Accessible Stage: elementProviderFromPoint x=" + x + " y=" + y +"Node"+ aNode.accElement);
            }
            Object aBase = aNode.getAccessibleElement(); 
            return aBase ;
        }
        return null;
    }
    
    /* return the glass object that has focus */
    @Override
    public Object getFocus() {
        Node node = stage.getScene().getFocusOwner();
        // Find this node in ally hierarchy and return its corresponding Accessible implementation.
      /*  if( node instanceof Accessible) {
            AccessibleProvider accNode = ((Accessible)node).impl_getAccessible() ;
        */
        try {
            java.lang.reflect.Method method = node.getClass().getMethod("impl_getAccessible");
            AccessibleProvider accNode = (AccessibleProvider)method.invoke(node);
            if( accNode instanceof AccessibleNode) return ((AccessibleNode)accNode).accElement ;
        } catch (Exception ex) {}
        return null ;  
    }
    
    public Object getStageAccessible() {
        return accRoot ;
    }
}
