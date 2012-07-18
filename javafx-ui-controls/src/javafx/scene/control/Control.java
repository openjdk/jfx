/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import com.sun.javafx.Utils;
import com.sun.javafx.css.*;
import com.sun.javafx.css.converters.StringConverter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.control.Logging;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.input.ContextMenuEvent;


/**
 * Base class for all user interface controls. A "Control" is a node in the
 * scene graph which can be manipulated by the user. Controls provide
 * additional variables and behaviors beyond those of Node to support
 * common user interactions in a manner which is consistent and predictable
 * for the user.
 * <p>
 * Additionally, controls support explicit skinning to make it easy to
 * leverage the functionality of a control while customizing its appearance.
 * <p>
 * See specific Control subclasses for information on how to use individual
 * types of controls.
 * <p> Most controls have their focusTraversable property set to true by default, however
 * read-only controls such as {@link Label} and {@link ProgressIndicator}, and some
 * controls that are containers {@link ScrollPane} and {@link ToolBar} do not.
 * Consult individual control documentation for details.
 */
public abstract class Control extends Region implements Skinnable {

    static {
        // Ensures that the caspian.css file is set as the user agent style sheet
        // when the first control is created.
        UAStylesheetLoader.doLoad();
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/  
    
    private List<StyleableProperty> styleableProperties;

    /**
     * A private reference directly to the SkinBase instance that is used as the
     * Skin for this Control. A Control's Skin doesn't have to be of type
     * SkinBase, although 98% of the time or greater it probably will be.
     * Because instanceof checks and reading a value from a property are
     * not cheap (on interpreters on slower hardware or mobile devices)
     * it pays to have a direct reference here to the skinBase. We simply
     * need to check this variable -- if it is not null then we know the
     * Skin is a SkinBase and this is a direct reference to it. If it is null
     * then we know the skin is not a SkinBase and we need to call getSkin().
     */
    private SkinBase skinBase;

    /***************************************************************************
    *                                                                         *
    * Event Handlers / Listeners                                              *
    *                                                                         *
    **************************************************************************/
    
    /**
     * Handles context menu requests by popping up the menu.
     * Note that we use this pattern to remove some of the anonymous inner
     * classes which we'd otherwise have to create. When lambda expressions
     * are supported, we could do it that way instead (or use MethodHandles).
     */
    private final EventHandler<ContextMenuEvent> contextMenuHandler = new EventHandler<ContextMenuEvent>() {
        @Override public void handle(ContextMenuEvent event) {
            // If a context menu was shown, consume the event to prevent multiple context menus
            if (getContextMenu() != null) {
                getContextMenu().show(Control.this, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        }
    };
    

    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/    
    
    
    
    // --- skin
    /**
     * Skin is responsible for rendering this {@code Control}. From the
     * perspective of the {@code Control}, the {@code Skin} is a black box.
     * It listens and responds to changes in state in a {@code Control}.
     * <p>
     * There is a one-to-one relationship between a {@code Control} and its
     * {@code Skin}. Every {@code Skin} maintains a back reference to the
     * {@code Control} via the {@link Skin#getSkinnable()} method.
     * <p>
     * A skin may be null.
     */
    @Override public final ObjectProperty<Skin<?>> skinProperty() { return skin; }
    @Override public final void setSkin(Skin<?> value) { 
        skinProperty().set(value); 
    }
    @Override public final Skin<?> getSkin() { return skinProperty().getValue(); }
    private ObjectProperty<Skin<?>> skin = new StyleableObjectProperty<Skin<?>>() {
        // We store a reference to the oldValue so that we can handle
        // changes in the skin properly in the case of binding. This is
        // only needed because invalidated() does not currently take
        // a reference to the old value.
        private Skin<?> oldValue;

        @Override
        public void set(Skin<?> v) {
            if (v == null 
                ? oldValue == null
                : oldValue != null && v.getClass().equals(oldValue.getClass()))
                return;

            super.set(v);
            
            // Collect the name of the currently installed skin class. We do this
            // so that subsequent updates from CSS to the same skin class will not
            // result in reinstalling the skin
            currentSkinClassName = v == null ? null : v.getClass().getName();
            
            // if someone calls setSkin, we need to make it look like they 
            // called set on skinClassName in order to keep CSS from overwriting
            // the skin. 
            skinClassNameProperty().set(currentSkinClassName);
        }

        @Override protected void invalidated() {
            // Dispose of the old skin
            if (oldValue != null) oldValue.dispose();
            
            // Get the new value, and save it off as the new oldValue
            final Skin<?> skin = oldValue = getValue();

            // We have two paths, one for "legacy" Skins, and one for
            // any Skin which extends from SkinBase. Legacy Skins will
            // produce a single node which will be the only child of
            // the Control via the getNode() method on the Skin. A
            // SkinBase will manipulate the children of the Control
            // directly. Further, we maintain a direct reference to
            // the skinBase for more optimal updates later.
            if (skin instanceof SkinBase) {
                // record a reference of the skin, if it is a SkinBase, for
                // performance reasons
                skinBase = (SkinBase) skin;
                // Note I do not remove any children here, because the
                // skin will have already configured all the children
                // by the time setSkin has been called. This is because
                // our Skin interface was lacking an initialize method (doh!)
                // and so the Skin constructor is where it adds listeners
                // and so forth. For SkinBase implementations, the
                // constructor is also where it will take ownership of
                // the children.
            } else {
                final Node n = getSkinNode();
                if (n != null) {
                    getChildren().setAll(n);
                } else {
                    getChildren().clear();
                }
                skinBase = null;
            }

            // DEBUG: Log that we've changed the skin
            final PlatformLogger logger = Logging.getControlsLogger();
            if (logger.isLoggable(PlatformLogger.FINEST)) {
                logger.finest("Stored skin[" + getValue() + "] on " + this);
            }
        }

        @Override 
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.SKIN;
        }
        
        @Override
        public Object getBean() {
            return Control.this;
        }

        @Override
        public String getName() {
            return "skin";
        }
    };

    
    // --- tooltip
    /**
     * The ToolTip for this control.
     */
    public final ObjectProperty<Tooltip> tooltipProperty() {
        if (tooltip == null) {
            tooltip = new ObjectPropertyBase<Tooltip>() {
                private Tooltip old = null;
                @Override protected void invalidated() {
                    Tooltip t = get();
                    // install / uninstall
                    if (t != old) {
                        if (old != null) {
                            Tooltip.uninstall(Control.this, old);
                        }
                        if (t != null) {
                            Tooltip.install(Control.this, t);
                        }
                        old = t;
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "tooltip";
                }
            };
        }
        return tooltip;
    }
    private ObjectProperty<Tooltip> tooltip;
    public final void setTooltip(Tooltip value) { tooltipProperty().setValue(value); }
    public final Tooltip getTooltip() { return tooltip == null ? null : tooltip.getValue(); }

    
    // --- context menu
    /**
     * The ContextMenu to show for this control.
     */
    private ObjectProperty<ContextMenu> contextMenu = new SimpleObjectProperty<ContextMenu>(this, "contextMenu") {
        @Override protected void invalidated() {
            // set this flag so contextmenu show will be relative to parent window not anchor
            ContextMenu ctx = get();
            if (ctx != null) ctx.setImpl_showRelativeToWindow(true); //RT-15160
        }
        @Override
        public Object getBean() {
            return Control.this;
        }
        @Override
        public String getName() {
            return "contextMenu";
        }
    };
    public final ObjectProperty<ContextMenu> contextMenuProperty() { return contextMenu; }
    public final void setContextMenu(ContextMenu value) { contextMenu.setValue(value); }
    public final ContextMenu getContextMenu() { return contextMenu == null ? null : contextMenu.getValue(); }

    

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     *  Create a new Control.
     */
    protected Control() {
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not 
        // override. Initializing focusTraversable by calling set on the 
        // StyleableProperty ensures that css will be able to override the value.        
        final StyleableProperty prop = StyleableProperty.getStyleableProperty(focusTraversableProperty());
        prop.set(this, Boolean.TRUE);  
        
        // we add a listener for menu request events to show the context menu
        // that may be set on the Control
        this.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuHandler);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Returns <code>true</code> since all Controls are resizable.
     * @return whether this node can be resized by its parent during layout
     */
    @Override public boolean isResizable() {
        return true;
    }

    // Implementation of the Resizable interface.
    // Because only the skin can know the min, pref, and max sizes, these
    // functions are implemented to delegate to skin. If there is no skin then
    // we simply return 0 for all the values since a Control without a Skin
    // doesn't render
    /**
     * Computes the minimum allowable width of the Control, based on the provided
     * height. The minimum width is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#minWidth(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param height The height of the Control, in case this value might dictate
     *      the minimum width.
     * @return A double representing the minimum width of this control.
     */
    @Override protected double computeMinWidth(final double height) {
        if (skinBase != null) {    
            return skinBase.computeMinWidth(height);
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.minWidth(height);
        }
    }
    /**
     * Computes the minimum allowable height of the Control, based on the provided
     * width. The minimum height is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#minHeight(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param width The width of the Control, in case this value might dictate
     *      the minimum height.
     * @return A double representing the minimum height of this control.
     */
    @Override protected double computeMinHeight(final double width) {
        if (skinBase != null) {    
            return skinBase.computeMinHeight(width);
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.minHeight(width);
        }
    }
    /**
     * Computes the maximum allowable width of the Control, based on the provided
     * height. The maximum width is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#maxWidth(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param height The height of the Control, in case this value might dictate
     *      the maximum width.
     * @return A double representing the maximum width of this control.
     */
    @Override protected double computeMaxWidth(double height) {
        if (skinBase != null) {  
            return skinBase.computeMaxWidth(height);
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.maxWidth(height);
        }
    }
    /**
     * Computes the maximum allowable height of the Control, based on the provided
     * width. The maximum height is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#maxHeight(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param width The width of the Control, in case this value might dictate
     *      the maximum height.
     * @return A double representing the maximum height of this control.
     */
    @Override protected double computeMaxHeight(double width) {
        if (skinBase != null) {  
            return skinBase.computeMaxHeight(width);
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.maxHeight(width);
        }
    }
    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        if (skinBase != null) {  
            return skinBase.computePrefWidth(height);
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null? 0 : skinNode.prefWidth(height);
        }
    }
    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        if (skinBase != null) {  
            return skinBase.computePrefHeight(width);
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null? 0 : skinNode.prefHeight(width);
        }
    }
    
    /** {@inheritDoc} */
    @Override public double getBaselineOffset() { 
        if (skinBase != null) {  
            return skinBase.getBaselineOffset();
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null? 0 : skinNode.getBaselineOffset(); 
        }
    }

    /***************************************************************************
     * Implementation of layout bounds for the Control. We want to preserve    *
     * the lazy semantics of layout bounds. So whenever the width/height       *
     * changes on the node, we end up invalidating layout bounds. We then      *
     * recompute it on demand.                                                 *
     **************************************************************************/

//    /**
//     * @treatAsPrivate
//     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
//     */
//    @Deprecated
//    @Override protected void impl_notifyLayoutBoundsChanged() {
//        // override Node's default behavior of having a geometric bounds change
//        // trigger a change in layoutBounds. For Resizable nodes, layoutBounds
//        // is unrelated to geometric bounds.
//    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected Bounds impl_computeLayoutBounds() {
        return new BoundingBox(0, 0, getWidth(), getHeight());
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        if (skinBase != null) {
            final Insets padding = getInsets();
            final double x = snapSize(padding.getLeft());
            final double y = snapSize(padding.getTop());
            final double w = snapSize(getWidth()) - snapSize(padding.getLeft()) - snapSize(padding.getRight());
            final double h = snapSize(getHeight()) - snapSize(padding.getTop()) - snapSize(padding.getBottom());
            skinBase.layoutChildren(x, y, w, h);
        } else {
            Node n = getSkinNode();
            if (n != null) {
                n.resizeRelocate(0, 0, getWidth(), getHeight());
            }
        }
    }

    /***************************************************************************
     * Forward the following to the skin                                       *
     **************************************************************************/

    // overridden to base containment on the Skin rather than on the bounds
    // of the Skin, allowing the Skin to dictate containment.
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_computeContains(double localX, double localY) {
        Node n = getSkinNode();
        return n == null ? false : n.contains(localX, localY);
    }

    // overridden to base intersects on the Skin rather than on the bounds
    // of the Skin, allowing the Skin to dictate intersection
    /** {@inheritDoc} */
    @Override public boolean intersects(double localX, double localY, double localWidth, double localHeight) {
        Node n = getSkinNode();
        return n == null ? false : n.intersects(localX, localY, localWidth, localHeight);
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        
        if (skinBase != null) {
            mask |= skinBase.impl_getPseudoClassState();
        }
        
        return mask;
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Package API for SkinBase                                                *
     *                                                                         *
     **************************************************************************/       
    
    // package private for SkinBase
    ObservableList<Node> getControlChildren() {
        return getChildren();
    }
    

    
    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/    

    /**
     * Gets the Skin's node, or returns null if there is no Skin.
     * Convenience method for getting the node of the skin. This is null-safe,
     * meaning if skin is null then it will return null instead of throwing
     * a NullPointerException.
     *
     * @return The Skin's node, or null.
     */
    private Node getSkinNode() {
        assert skinBase == null;
        Skin skin = getSkin();
        return skin == null ? null : skin.getNode();
    }

    /**
     * Keeps a reference to the name of the class currently acting as the skin.
     */
    private String currentSkinClassName = null;
    private StringProperty skinClassName;
    protected StringProperty skinClassNameProperty() {
        if (skinClassName == null) {
            skinClassName = new StyleableStringProperty() {

                @Override
                public void set(String v) {
                    // do not allow the skin to be set to null through CSS
                    if (v == null || v.isEmpty() || v.equals(get())) return;
                    super.set(v);
                }
                
                @Override
                public void invalidated() {
                    
                    // reset the styleable properties so we get the new ones from
                    // the new skin
                    styleableProperties = null;

                    if (get() != null) {
                        if (!get().equals(currentSkinClassName)) {
                            loadSkinClass();
                        }
                      // CSS should not set skin to null
//                    } else {
//                        setSkin(null);
                    }
                }
                
                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "skinClassName";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.SKIN;
                }
                
            };
        }
        return skinClassName;
    }
    
    protected void setSkinClassName(String skinClassName) {
        skinClassNameProperty().set(skinClassName);        
    }
    
    private void loadSkinClass() {
        
        if (skinClassName == null 
                || skinClassName.get() == null 
                || skinClassName.get().isEmpty()) {
            final String msg = 
                "Empty -fx-skin property specified for control " + this;   
            final List<CssError> errors = StyleManager.getInstance().getErrors();
            if (errors != null) {
                CssError error = new CssError(msg);
                errors.add(error); // RT-19884
            } 
            Logging.getControlsLogger().severe(msg);
            return;
        }

        try {
            final Class<?> skinClass = Utils.loadClass(skinClassName.get(), this);
            Constructor<?>[] constructors = skinClass.getConstructors();
            Constructor<?> skinConstructor = null;
            for (Constructor<?> c : constructors) {
                Class<?>[] parameterTypes = c.getParameterTypes();
                if (parameterTypes.length == 1 && Control.class.isAssignableFrom(parameterTypes[0])) {
                    skinConstructor = c;
                    break;
                }
            }

            if (skinConstructor == null) {
                final String msg = 
                    "No valid constructor defined in '" + skinClassName + "' for control " + this +
                        ".\r\nYou must provide a constructor that accepts a single "
                        + "Control parameter in " + skinClassName + ".";
                final List<CssError> errors = StyleManager.getInstance().getErrors();
                if (errors != null) {
                    CssError error = new CssError(msg);
                    errors.add(error); // RT-19884
                } 
                Logging.getControlsLogger().severe(msg);
            } else {
                Skin<?> skinInstance = (Skin<?>) skinConstructor.newInstance(this);
                // Do not call setSkin here since it has the side effect of
                // also setting the skinClassName!
                skinProperty().set(skinInstance);
            }
        } catch (InvocationTargetException e) {
            final String msg = 
                "Failed to load skin '" + skinClassName + "' for control " + this;
            final List<CssError> errors = StyleManager.getInstance().getErrors();
            if (errors != null) {
                CssError error = new CssError(msg + " :" + e.getLocalizedMessage());
                errors.add(error); // RT-19884
            } 
            Logging.getControlsLogger().severe(msg, e.getCause());
        } catch (Exception e) {
            final String msg = 
                "Failed to load skin '" + skinClassName + "' for control " + this;
            final List<CssError> errors = StyleManager.getInstance().getErrors();
            if (errors != null) {
                CssError error = new CssError(msg + " :" + e.getLocalizedMessage());
                errors.add(error); // RT-19884
            } 
            Logging.getControlsLogger().severe(msg, e);            
//            Node parent = this;
//            int n = 0;
//            while (parent != null) {
//                for(int j=0; j<n*2; j++) {
//                    System.err.print(' ');
//                }
//                n+=1;
//                System.err.println(parent);
//                parent = parent.getParent();
//            }
            
        }

    }

    /***************************************************************************
     *                                                                         *
     * StyleSheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * Implementors may specify their own user-agent stylesheet. The return
     * value is a string URL. A relative URL is resolved using the class
     * loader of the implementing Control.
     * @return A string URL
     */
    protected String getUserAgentStylesheet() {
        return null;
    }

    private static class StyleableProperties {
        private static final StyleableProperty<Control,String> SKIN = 
            new StyleableProperty<Control,String>("-fx-skin",
                StringConverter.getInstance()) {

            @Override
            public boolean isSettable(Control n) {
                return (n.skin == null || !n.skin.isBound());
            }

            @Override
            public WritableValue<String> getWritableValue(Control n) {
                return n.skinClassNameProperty();
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Region.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                SKIN
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Control.StyleableProperties.STYLEABLES;
    }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        if (styleableProperties == null) {
            styleableProperties = new ArrayList<StyleableProperty>();
            styleableProperties.addAll(Control.StyleableProperties.STYLEABLES);
            
            if (skinBase != null) {
                styleableProperties.addAll(skinBase.impl_getStyleableProperties());
            }
        }
        return styleableProperties;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_processCSS(boolean reapply) {
        if (reapply && getUserAgentStylesheet() != null) {
            StyleManager.getInstance().addUserAgentStylesheet(getUserAgentStylesheet());
        }

        super.impl_processCSS(reapply);
        
//        if (skinBase != null) {
//            skinBase.impl_processCSS(reapply);
//        }

        if (getSkin() == null) {
            final String msg = 
                "The -fx-skin property has not been defined in CSS for " + this;
            final List<CssError> errors = StyleManager.getInstance().getErrors();
            if (errors != null) {
                CssError error = new CssError(msg);
                errors.add(error); // RT-19884
            } 
            Logging.getControlsLogger().severe(msg);
        }
    }
    
    /**
      * Most Controls return true for focusTraversable initial value. 
      * This method is called from CSS code to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Boolean impl_cssGetFocusTraversableInitialValue() {
        return Boolean.TRUE;
    }
}
