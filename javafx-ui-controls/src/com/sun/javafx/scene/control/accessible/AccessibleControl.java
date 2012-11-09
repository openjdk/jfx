/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.scene.control.accessible;

import com.sun.javafx.Logging;
import com.sun.javafx.accessible.AccessibleNode;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.accessible.utils.EventIds;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.utils.ProviderOptions;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;

/**
 *
 * @author rrprasad
 */
public class AccessibleControl extends AccessibleNode {
    Control control ;
    public AccessibleControl(Control control)
    {
	super(control);
        this.control = control ;
        
       // Initialize focus listener
        InvalidationListener focusListener = new InvalidationListener() {
            @Override public void invalidated(Observable property) {
                PlatformLogger logger = Logging.getAccessibilityLogger();
                if (logger.isLoggable(PlatformLogger.FINER)) {
                    logger.finer(this.toString() + " Focus Change");
                }
                fireEvent(EventIds.AUTOMATION_FOCUS_CHANGED);
        }
        };
        
        control.focusedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                PlatformLogger logger = Logging.getAccessibilityLogger();
                if(!t && t1)
                {
                    if (logger.isLoggable(PlatformLogger.FINER)) {
                        logger.finer(this.toString() + " Focus Change: true");
                    }
                    fireEvent(EventIds.AUTOMATION_FOCUS_CHANGED);
                }
                else
                    if (logger.isLoggable(PlatformLogger.FINER)) {
                        logger.finer(this.toString() + " Focus Change: false");
                    }
            }
        } );        
    }
        
    public void fireEvent(int id)
    {
        super.fireEvent(id);
    }

    public void firePropertyChange(int propertyId, int oldProperty, int newProperty) {
        super.firePropertyChange(propertyId, oldProperty, newProperty);
    }
    public void firePropertyChange(int propertyId, boolean oldProperty, boolean newProperty) {
        super.firePropertyChange(propertyId, oldProperty, newProperty);
    }
    
}
