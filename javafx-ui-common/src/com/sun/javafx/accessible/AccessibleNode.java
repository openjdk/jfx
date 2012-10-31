/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.accessible;

import com.sun.javafx.accessible.utils.NavigateDirection;
import com.sun.javafx.accessible.utils.PropertyIds;
import com.sun.javafx.accessible.utils.Rect;
import com.sun.javafx.Logging;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.geometry.Bounds;
import com.sun.javafx.logging.PlatformLogger;
import java.util.ArrayList;
import com.sun.javafx.accessible.providers.AccessibleProvider;

/**
 *
 * @author rrprasad
 * 
 */
public class AccessibleNode implements AccessibleProvider
{
    Object accElement ; // the Glass peer
    Node node ; // scene graph node 
    AccessibleNode parent; // parent of this child in accessible graph
    AccessibleStage accController ; // not sure if this can be worked arnd in future
    List<AccessibleNode> children; // if this control has children
            
    public AccessibleNode(Node n)
    {
        this.node = n ;
        this.parent = null ;
        this.children = new ArrayList<AccessibleNode>();
    }

    //public AccessibleBaseProvider getAccessibleElement()
    public Object getAccessibleElement()
    {
        return this.accElement ;
    }
    
    public void fireEvent(int EventId) {
        accController.stage.impl_getPeer().accessibleFireEvent(accElement, EventId);
//        accElement.fireEvent(EventId);
    }
    
    public void firePropertyChange(int propertyId, int oldProperty, int newProperty) {
        accController.stage.impl_getPeer().accessibleFirePropertyChange(accElement, propertyId, oldProperty, newProperty);
    //    accElement.firePropertyChange(propertyId, oldProperty, newProperty);
    }

    public void firePropertyChange(int propertyId, boolean oldProperty, boolean newProperty) {
        accController.stage.impl_getPeer().accessibleFirePropertyChange(accElement, propertyId, oldProperty, newProperty);
    //    accElement.firePropertyChange(propertyId, oldProperty, newProperty);
    }
   
     
    // Summary:
    //     Gets a base provider for this element.
    //
    // Returns:
    //     The base provider, or null.
    @Override
    public AccessibleProvider hostRawElementProvider() 
    {
        return this ;
    }

    // Summary:
    //     Retrieves an object that provides support for a control pattern on a UI Automation
    //     element.
    //
    // Parameters:
    //   patternId:
    //     Identifier of the pattern.
    //
    // Returns:
    //     Object that implements the pattern interface, or null if the pattern is not
    //     supported.
    @Override
    public Object getPatternProvider(int patternId)
    {
        return this ;
    }
    //
    // Summary:
    //     Retrieves the value of a property supported by the UI Automation provider.
    //
    // Parameters:
    //   propertyId:
    //     The property identifier.
    //
    // Returns:
    //     The property value, or a null if the property is not supported by this provider,
    //     or System.Windows.Automation.AutomationElementIdentifiers.NotSupported if
    //     it is not supported at all.
    @Override
    public Object getPropertyValue(int propertyId)
    {
        if (propertyId == PropertyIds.NAME)
            return node.getClass().toString();
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
        Scene scene = node.getScene();
        Bounds bounds = node.getBoundsInParent() ;
        double x =
            Screen.getPrimary().getBounds().getMinX() +
            scene.getWindow().getX() +
            scene.getX() +
            bounds.getMinX();
        double y =
            Screen.getPrimary().getBounds().getMinY() +
            scene.getWindow().getY() +
            scene.getY() +
            bounds.getMinY();
        
        PlatformLogger logger = Logging.getAccessibilityLogger();
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer(this.toString()+ "MinX="+ bounds.getMinX() + "MinY="+bounds.getMinY() + 
             "Width="+ bounds.getWidth() +"Height="+ bounds.getHeight());
        }
        return new Rect(x, y, bounds.getWidth(), bounds.getHeight());
    }
    
    public boolean contains(double x, double y)
    {
        Bounds bounds = node.getBoundsInParent() ;
        return bounds.contains(x, y);
    }
    
    /**
     * Get the root node of the fragment.
     * 
     * @return the root node.
     */
    @Override
    public Object fragmentRoot() {
        return accController.accRoot ;
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
    public Object navigate(NavigateDirection direction) { // this is not focussed driven
        PlatformLogger logger = Logging.getAccessibilityLogger();
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer(this.toString()+ "navigate direction: " + direction);
        }
        AccessibleNode accTemp =null;
        switch (direction) {
            case Parent:
                if(parent != null)
                    accTemp = parent ;
                else // return Controllers FragmentRoot as parent
                    return accController.accRoot ;
                break;
            case NextSibling: 
            case PreviousSibling:
                if(( parent != null) &&( parent.children.size() > 0 ) )
                {
                    int idx = parent.children.indexOf(this);
                    // PTB: This should never happen so should we just remove this
                    // or raise an exception?
                    if (idx == -1) {
                        if (logger.isLoggable(PlatformLogger.FINER)) {
                            logger.finer(this.toString()+ "  children.indexOf returned -1");
                        }
                    }
                    if( direction == NavigateDirection.NextSibling )
                        idx++ ;
                    else
                        idx--;
                    if( (idx >= 0) && (idx < parent.children.size()) )
                        accTemp = parent.children.get(idx);
                }
                else if(( parent == null) && ( accController.accChildren.size() > 0)) // when Parent is FragmentRoot
                {
                    int idx = accController.accChildren.indexOf(this);
                    // PTB: This should never happen so should we just remove this
                    // or raise an exception?
                    if (idx == -1) {
                        if (logger.isLoggable(PlatformLogger.FINER)) {
                            logger.finer(this.toString()+ "  children.indexOf returned -1");
                        }
                    }
                    if( direction == NavigateDirection.NextSibling )
                        idx++ ;
                    else
                        idx--;
                    if( (idx >= 0) && (idx < accController.accChildren.size()) )
                        accTemp = accController.accChildren.get(idx);
                }
                    
                break;
           case FirstChild:
               if( children.size() > 0 ) 
                    accTemp = children.get(0) ;
                break;
            case LastChild:
               if( children.size() > 0 ) 
                    accTemp = children.get(children.size()-1) ;
                break;
        }
        if( accTemp != null )
            return accTemp.accElement ;
        return null;
    }
    
    /**
     * Set the focus to this element.
     */
    @Override
    public void setFocus() {
//        node.getScene().setImpl_focusOwner(node);
        PlatformLogger logger = Logging.getAccessibilityLogger();
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer(this.toString()+ "In AccessibleNode.setFocus");
        }
        node.requestFocus();
    }
}
