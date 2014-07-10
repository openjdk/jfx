/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.scene.control.ControlAcceleratorSupport;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Node;
//import javafx.scene.accessibility.Action;
//import javafx.scene.accessibility.Attribute;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Region;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.css.CssError;
import javafx.css.CssMetaData;
import com.sun.javafx.css.StyleManager;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableStringProperty;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.scene.control.Logging;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;


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
 * @since JavaFX 2.0
 */
public abstract class Control extends Region implements Skinnable {

    static {
        // Ensures that the default application user agent stylesheet is loaded
        if (Application.getUserAgentStylesheet() == null) {
            PlatformImpl.setDefaultPlatformUserAgentStylesheet();
        }
    }

    /**
     * Utility for loading a class in a manner that will work with multiple
     * class loaders, as is typically found in OSGI modular applications.
     * In particular, this method will attempt to just load the class
     * identified by className. If that fails, it attempts to load the
     * class using the current thread's context class loader. If that fails,
     * it attempts to use the class loader of the supplied "instance", and
     * if it still fails it walks up the class hierarchy of the instance
     * and attempts to use the class loader of each class in the super-type
     * hierarchy.
     *
     * @param className The name of the class we want to load
     * @param instance An optional instance used to help find the class to load
     * @return The class. Cannot return null
     * @throws ClassNotFoundException If the class cannot be found using any technique.
     */
    static Class<?> loadClass(final String className, final Object instance)
            throws ClassNotFoundException
    {
        try {
            // Try just loading the class
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            // RT-17525 : Use context class loader only if Class.forName fails.
            if (Thread.currentThread().getContextClassLoader() != null) {
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass(className);
                } catch (ClassNotFoundException ex2) {
                    // Do nothing, just fall through
                }
            }

            // RT-14177: Try looking up the class using the class loader of the
            //           current class, walking up the list of superclasses
            //           and checking each of them, before bailing and using
            //           the context class loader.
            if (instance != null) {
                Class<?> currentType = instance.getClass();
                while (currentType != null) {
                    try {
                        return currentType.getClassLoader().loadClass(className);
                    } catch (ClassNotFoundException ex2) {
                        currentType = currentType.getSuperclass();
                    }
                }
            }

            // We failed to find the class using any of the above means, so we're going
            // to just throw the ClassNotFoundException that we caught earlier
            throw ex;
        }
    }

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private List<CssMetaData<? extends Styleable, ?>> styleableProperties;

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
    private SkinBase<?> skinBase;

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
    private final static EventHandler<ContextMenuEvent> contextMenuHandler = event -> {
        if (event.isConsumed()) return;

        // If a context menu was shown, consume the event to prevent multiple context menus
        Object source = event.getSource();
        if (source instanceof Control) {
            Control c = (Control) source;
            if (c.getContextMenu() != null) {
                c.getContextMenu().show(c, event.getScreenX(), event.getScreenY());
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
        //This code is basically a kind of optimization that prevents a Skin that is equal but not instance equal.
        //Although it's not kosher from the property perspective (bindings won't pass through set), it should not do any harm.
        //But it should be evaluated in the future.
        public void set(Skin<?> v) {
            if (v == null
                ? oldValue == null
                : oldValue != null && v.getClass().equals(oldValue.getClass()))
                return;

            super.set(v);
        }

        @Override protected void invalidated() {
            Skin<?> skin = get();
            // Collect the name of the currently installed skin class. We do this
            // so that subsequent updates from CSS to the same skin class will not
            // result in reinstalling the skin
            currentSkinClassName = skin == null ? null : skin.getClass().getName();

            // If skinClassName is null, then someone called setSkin directly
            // rather than the skin being set via css. We know this is because
            // impl_processCSS ensures the skin is set, and impl_processCSS
            // expands the skin property to see if the skin has been set.
            // If skinClassName is null, then we need to see if there is
            // a UA stylesheet at this point since the logic in impl_processCSS
            // depends on skinClassName being null.
            if (skinClassName == null) {
                final String url = Control.this.getUserAgentStylesheet();
                if (url != null) {
                    StyleManager.getInstance().addUserAgentStylesheet(url);
                }
            }
            // if someone calls setSkin, we need to make it look like they
            // called set on skinClassName in order to keep CSS from overwriting
            // the skin.
            skinClassNameProperty().set(currentSkinClassName);
            
            
            // Dispose of the old skin
            if (oldValue != null) oldValue.dispose();

            // Get the new value, and save it off as the new oldValue
            oldValue = skin;

            // Reset skinBase to null - it will be set to the new Skin if it
            // is a SkinBase, otherwise it will remain null, as expected
            skinBase = null;

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
                skinBase = (SkinBase<?>) skin;
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
            }

            // clear out the styleable properties so that the list is rebuilt
            // next time they are requested.
            styleableProperties = null;

            // calling impl_reapplyCSS() as the styleable properties may now
            // be different, as we will now be able to return styleable properties
            // belonging to the skin. If impl_reapplyCSS() is not called, the
            // getCssMetaData() method is never called, so the
            // skin properties are never exposed.
            impl_reapplyCSS();

            // DEBUG: Log that we've changed the skin
            final PlatformLogger logger = Logging.getControlsLogger();
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Stored skin[" + getValue() + "] on " + this);
            }
        }

        // This method should be CssMetaData<Control,Skin> getCssMetaData(),
        // but SKIN is CssMetaData<Control,String>. This does not matter to
        // the CSS code which doesn't care about the actual type. Hence,
        // we'll suppress the warnings
        @Override @SuppressWarnings({"unchecked", "rawtype"})
        public CssMetaData getCssMetaData() {
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
        private WeakReference<ContextMenu> contextMenuRef;

        @Override protected void invalidated() {
            ContextMenu oldMenu = contextMenuRef == null ? null : contextMenuRef.get();
            if (oldMenu != null) {
                ControlAcceleratorSupport.removeAcceleratorsFromScene(oldMenu.getItems(), Control.this);
            }

            ContextMenu ctx = get();
            contextMenuRef = new WeakReference<>(ctx);

            if (ctx != null) {
                // set this flag so contextmenu show will be relative to parent window not anchor
                ctx.setImpl_showRelativeToWindow(true); //RT-15160

                // if a context menu is set, we need to install any accelerators
                // belonging to its menu items ASAP into the scene that this
                // Control is in (if the control is not in a Scene, we will need
                // to wait until it is and then do it).
                ControlAcceleratorSupport.addAcceleratorsIntoScene(ctx.getItems(), Control.this);
            }
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
        // override. Initializing focusTraversable by calling applyStyle
        // with null for StyleOrigin ensures that css will be able to override
        // the value.
        final StyleableProperty<Boolean> prop = (StyleableProperty<Boolean>)(WritableValue<Boolean>)focusTraversableProperty();
        prop.applyStyle(null, Boolean.TRUE);

        // we add a listener for menu request events to show the context menu
        // that may be set on the Control
        this.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuHandler);
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    // Proposed dispose() API.
    // Note that there is impl code for a dispose method in TableRowSkinBase
    // and TableCell (just search for dispose())
//    public void dispose() {
//        Skin skin = getSkin();
//        if (skin != null) {
//            skin.dispose();
//        }
//    }

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
            return skinBase.computeMinWidth(height, snappedTopInset(), snappedRightInset(), snappedBottomInset(), snappedLeftInset());
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
            return skinBase.computeMinHeight(width, snappedTopInset(), snappedRightInset(), snappedBottomInset(), snappedLeftInset());
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
            return skinBase.computeMaxWidth(height, snappedTopInset(), snappedRightInset(), snappedBottomInset(), snappedLeftInset());
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
            return skinBase.computeMaxHeight(width, snappedTopInset(), snappedRightInset(), snappedBottomInset(), snappedLeftInset());
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.maxHeight(width);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        if (skinBase != null) {
            return skinBase.computePrefWidth(height, snappedTopInset(), snappedRightInset(), snappedBottomInset(), snappedLeftInset());
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.prefWidth(height);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        if (skinBase != null) {
            return skinBase.computePrefHeight(width, snappedTopInset(), snappedRightInset(), snappedBottomInset(), snappedLeftInset());
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.prefHeight(width);
        }
    }

    /** {@inheritDoc} */
    @Override public double getBaselineOffset() {
        if (skinBase != null) {
            return skinBase.computeBaselineOffset(snappedTopInset(), snappedRightInset(), snappedBottomInset(), snappedLeftInset());
        } else {
            final Node skinNode = getSkinNode();
            return skinNode == null ? 0 : skinNode.getBaselineOffset();
        }
    }

    /***************************************************************************
     * Implementation of layout bounds for the Control. We want to preserve    *
     * the lazy semantics of layout bounds. So whenever the width/height       *
     * changes on the node, we end up invalidating layout bounds. We then      *
     * recompute it on demand.                                                 *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        if (skinBase != null) {
            final double x = snappedLeftInset();
            final double y = snappedTopInset();
            final double w = snapSize(getWidth()) - x - snappedRightInset();
            final double h = snapSize(getHeight()) - y - snappedBottomInset();
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

    /**
     * Create a new instance of the default skin for this control. This is called to create a skin for the control if
     * no skin is provided via CSS {@code -fx-skin} or set explicitly in a sub-class with {@code  setSkin(...)}.
     *
     * @return  new instance of default skin for this control. If null then the control will have no skin unless one
     *          is provided by css.
     * @since JavaFX 8.0
     */
    protected Skin<?> createDefaultSkin() {
        return null;
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
        Skin<?> skin = getSkin();
        return skin == null ? null : skin.getNode();
    }

    /**
     * Keeps a reference to the name of the class currently acting as the skin.
     */
    private String currentSkinClassName = null;
    private StringProperty skinClassName;

    /**
     * @treatAsPrivate
     * @since JavaFX 2.1
     */
    @Deprecated protected StringProperty skinClassNameProperty() {
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

                    if (get() != null) {
                        if (!get().equals(currentSkinClassName)) {
                            loadSkinClass(Control.this, skinClassName.get());
                        }
                        // Note: CSS should not set skin to null
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
                public CssMetaData<Control,String> getCssMetaData() {
                    return StyleableProperties.SKIN;
                }

            };
        }
        return skinClassName;
    }

    static void loadSkinClass(final Skinnable control, final String skinClassName) {
        if (skinClassName == null || skinClassName.isEmpty()) {
            final String msg =
                "Empty -fx-skin property specified for control " + control;
            final List<CssError> errors = StyleManager.getErrors();
            if (errors != null) {
                CssError error = new CssError(msg);
                errors.add(error); // RT-19884
            }
            Logging.getControlsLogger().severe(msg);
            return;
        }

        try {
            final Class<?> skinClass = Control.loadClass(skinClassName, control);
            Constructor<?>[] constructors = skinClass.getConstructors();
            Constructor<?> skinConstructor = null;
            for (Constructor<?> c : constructors) {
                Class<?>[] parameterTypes = c.getParameterTypes();
                if (parameterTypes.length == 1 && Skinnable.class.isAssignableFrom(parameterTypes[0])) {
                    skinConstructor = c;
                    break;
                }
            }

            if (skinConstructor == null) {
                final String msg =
                    "No valid constructor defined in '" + skinClassName + "' for control " + control +
                        ".\r\nYou must provide a constructor that accepts a single "
                        + "Skinnable (e.g. Control or PopupControl) parameter in " + skinClassName + ".";
                final List<CssError> errors = StyleManager.getErrors();
                if (errors != null) {
                    CssError error = new CssError(msg);
                    errors.add(error); // RT-19884
                }
                Logging.getControlsLogger().severe(msg);
            } else {
                Skin<?> skinInstance = (Skin<?>) skinConstructor.newInstance(control);
                // Do not call setSkin here since it has the side effect of
                // also setting the skinClassName!
                control.skinProperty().set(skinInstance);
            }
        } catch (InvocationTargetException e) {
            final String msg =
                "Failed to load skin '" + skinClassName + "' for control " + control;
            final List<CssError> errors = StyleManager.getErrors();
            if (errors != null) {
                CssError error = new CssError(msg + " :" + e.getLocalizedMessage());
                errors.add(error); // RT-19884
            }
            Logging.getControlsLogger().severe(msg, e.getCause());
        } catch (Exception e) {
            final String msg =
                "Failed to load skin '" + skinClassName + "' for control " + control;
            final List<CssError> errors = StyleManager.getErrors();
            if (errors != null) {
                CssError error = new CssError(msg + " :" + e.getLocalizedMessage());
                errors.add(error); // RT-19884
            }
            Logging.getControlsLogger().severe(msg, e);
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
        private static final CssMetaData<Control,String> SKIN =
            new CssMetaData<Control,String>("-fx-skin",
                StringConverter.getInstance()) {

            @Override
            public boolean isSettable(Control n) {
                return (n.skin == null || !n.skin.isBound());
            }

            @Override
            public StyleableProperty<String> getStyleableProperty(Control n) {
                return (StyleableProperty<String>)(WritableValue<String>)n.skinClassNameProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
            styleables.add(SKIN);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * This method returns a {@link List} containing all {@link CssMetaData} for
     * both this Control (returned from {@link #getControlCssMetaData()} and its
     * {@link Skin}, assuming the {@link #skinProperty() skin property} is a
     * {@link SkinBase}.
     *
     * <p>Developers who wish to provide custom CssMetaData are therefore
     * encouraged to override {@link Control#getControlCssMetaData()} or
     * {@link SkinBase#getCssMetaData()}, depending on where the CssMetaData
     * resides.
     * @since JavaFX 8.0
     */
    @Override
    public final List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        if (styleableProperties == null) {

            // RT-29162: make sure properties only show up once in the list
            java.util.Map<String, CssMetaData<? extends Styleable, ?>> map =
                new java.util.HashMap<String, CssMetaData<? extends Styleable, ?>>();

            List<CssMetaData<? extends Styleable, ?>> list =  getControlCssMetaData();

            for (int n=0, nMax = list != null ? list.size() : 0; n<nMax; n++) {

                CssMetaData<? extends Styleable, ?> metaData = list.get(n);
                if (metaData == null) continue;

                map.put(metaData.getProperty(), metaData);
            }

            //
            // if both control and skin base have the same property, use the
            // one from skin base since it may be a specialization of the
            // property in the control. For instance, Label has -fx-font and
            // so does LabeledText which is Label's skin.
            //
            list =  skinBase != null ? skinBase.getCssMetaData() : null;

            for (int n=0, nMax = list != null ? list.size() : 0; n<nMax; n++) {

                CssMetaData<? extends Styleable, ?> metaData = list.get(n);
                if (metaData == null) continue;

                map.put(metaData.getProperty(), metaData);
            }

            styleableProperties = new ArrayList<CssMetaData<? extends Styleable, ?>>();
            styleableProperties.addAll(map.values());
        }
        return styleableProperties;
    }

    /**
     * @return unmodifiable list of the controls css styleable properties
     * @since JavaFX 8.0
     */
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_processCSS(WritableValue<Boolean> unused) {
        // don't muck with this if block without first reading the comments in skin property's set method!
        if (skinClassNameProperty().get() == null) {
            // TODO: using skinClassName as a flag in skin property's set method is probably a bad idea
            final String url = Control.this.getUserAgentStylesheet();
            if (url != null) {
                StyleManager.getInstance().addUserAgentStylesheet(url);
            }
        }

        super.impl_processCSS(unused);

        if (getSkin() == null) {
            // try to create default skin
            final Skin<?> defaultSkin = createDefaultSkin();
            if (defaultSkin != null) {
                skinProperty().set(defaultSkin);
                super.impl_processCSS(unused);
            } else {
                final String msg = "The -fx-skin property has not been defined in CSS for " + this +
                                   " and createDefaultSkin() returned null.";
                final List<CssError> errors = StyleManager.getErrors();
                if (errors != null) {
                    CssError error = new CssError(msg);
                    errors.add(error); // RT-19884
                }
                Logging.getControlsLogger().severe(msg);
            }
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


    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

//    /** @treatAsPrivate */
//    @Override public Object accGetAttribute(Attribute attribute, Object... parameters) {
//        switch (attribute) {
//            case TOOLTIP:
//                Tooltip tooltip = getTooltip();
//                return tooltip == null ? "" : tooltip.getText();
//            default:
//        }
//        if (skinBase != null) {
//            Object result = skinBase.accGetAttribute(attribute, parameters);
//            if (result != null) return result;
//        }
//        return super.accGetAttribute(attribute, parameters);
//    }
//
//    /** @treatAsPrivate */
//    @Override public void accExecuteAction(Action action, Object... parameters) {
//        switch (action) {
//            case SHOW_MENU:
//                ContextMenu menu = getContextMenu();
//                if (menu != null) {
//                    menu.show(this, Side.RIGHT, 0, 0);
//                }
//                break;
//            default:
//        }
//        if (skinBase != null) {
//            skinBase.accExecuteAction(action, parameters);
//        }
//        super.accExecuteAction(action, parameters);
//    }
}
