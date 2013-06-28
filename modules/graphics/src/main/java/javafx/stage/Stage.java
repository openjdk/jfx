/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.image.Image;

import com.sun.javafx.beans.annotations.Default;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.robot.impl.FXRobotHelper;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.stage.StageHelper;
import com.sun.javafx.stage.StagePeerListener;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableValue;

/**
 * The JavaFX {@code Stage} class is the top level JavaFX container.
 * The primary Stage is constructed by the platform. Additional Stage
 * objects may be constructed by the application.
 *
 * <p>
 * Stage objects must be constructed and modified on the
 * JavaFX Application Thread.
 * </p>
 * <p>
 * Many of the {@code Stage} properties are read only because they can
 * be changed externally by the underlying platform and therefore must
 * not be bindable.
 * </p>
 * 
 * <p><b>Style</b></p>
 * <p>
 * A stage has one of the following styles:
 * <ul>
 * <li>{@link StageStyle#DECORATED} - a stage with a solid white background and
 * platform decorations.</li>
 * <li>{@link StageStyle#UNDECORATED} - a stage with a solid white background
 * and no decorations.</li>
 * <li>{@link StageStyle#TRANSPARENT} - a stage with a transparent background
 * and no decorations.</li>
 * <li>{@link StageStyle#UTILITY} - a stage with a solid white background and
 * minimal platform decorations.</li>
 * </ul>
 * <p>The style must be initialized before the stage is made visible.</p>
 * <p>On some platforms decorations might not be available. For example, on
 * some mobile or embedded devices. In these cases a request for a DECORATED or
 * UTILITY window will be accepted, but no decorations will be shown. </p>
 * 
 * <p><b>Owner</b></p>
 * <p>
 * A stage can optionally have an owner Window.
 * When a window is a stage's owner, it is said to be the parent of that stage.
 * When a parent window is closed, all its descendant windows are closed. 
 * The same chained behavior applied for a parent window that is iconified. 
 * A stage will always be on top of its parent window.
 * The owner must be initialized before the stage is made visible.
 *
 * <p><b>Modality</b></p>
 * <p>
 * A stage has one of the following modalities:
 * <ul>
 * <li>{@link Modality#NONE} - a stage that does not block any other window.</li>
 * <li>{@link Modality#WINDOW_MODAL} - a stage that blocks input events from 
 * being delivered to all windows from its owner (parent) to its root.
 * Its root is the closest ancestor window without an owner.</li>
 * <li>{@link Modality#APPLICATION_MODAL} - a stage that blocks input events from 
 * being delivered to all windows from the same application, except for those
 * from its child hierarchy.</li>
 * </ul> 
 * 
 * <p>When a window is blocked by a modal stage its Z-order relative to its ancestors
 * is preserved, and it receives no input events and no window activation events,
 * but continues to animate and render normally.
 * Note that showing a modal stage does not necessarily block the caller. The
 * {@link #show} method returns immediately regardless of the modality of the stage.
 * Use the {@link #showAndWait} method if you need to block the caller until
 * the modal stage is hidden (closed).
 * The modality must be initialized before the stage is made visible.</p> 
 *
 * <p><b>Example:</b></p>
 *
 *
<pre><code>
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class HelloWorld extends Application {

    &#64;Override public void start(Stage stage) {
        Text text = new Text(10, 40, "Hello World!");
        text.setFont(new Font(40));
        Scene scene = new Scene(new Group(text));

        stage.setTitle("Welcome to JavaFX!"); 
        stage.setScene(scene); 
        stage.sizeToScene(); 
        stage.show(); 
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}

 * </code></pre>
 * <p>produces the following on Windows:</p>
 * <p><img src="doc-files/Stage-win.png"/></p>
 *
 * <p>produces the following on Mac OSX:</p>
 * <p><img src="doc-files/Stage-mac.png"/></p>
 *
 * <p>produces the following on Linux:</p>
 * <p><img src="doc-files/Stage-linux.png"/></p>
 * @since JavaFX 2.0
 */
public class Stage extends Window {

    private boolean inNestedEventLoop = false;

    private static ObservableList<Stage> stages = FXCollections.<Stage>observableArrayList();

    static {
        FXRobotHelper.setStageAccessor(new FXRobotHelper.FXRobotStageAccessor() {
            @Override public ObservableList<Stage> getStages() {
                return stages;
            }
        });
        StageHelper.setStageAccessor(new StageHelper.StageAccessor() {
            @Override public ObservableList<Stage> getStages() {
                return stages;
            }
        });
    }
    
    private static final StagePeerListener.StageAccessor STAGE_ACCESSOR = new StagePeerListener.StageAccessor() {

        @Override
        public void setIconified(Stage stage, boolean iconified) {
            stage.iconifiedPropertyImpl().set(iconified);
        }

        @Override
        public void setMaximized(Stage stage, boolean maximized) {
            stage.maximizedPropertyImpl().set(maximized);
        }

        @Override
        public void setResizable(Stage stage, boolean resizable) {
            ((ResizableProperty)stage.resizableProperty()).setNoInvalidate(resizable);
        }

        @Override
        public void setFullScreen(Stage stage, boolean fs) {
            stage.fullScreenPropertyImpl().set(fs);
        }
    };

    /**
     * Creates a new instance of decorated {@code Stage}.
     *
     * @throws IllegalStateException if this constructor is called on a thread
     * other than the JavaFX Application Thread.
     */
    public Stage() {
        this(StageStyle.DECORATED);
    }

    /**
     * Creates a new instance of {@code Stage}.
     *
     * @param style The style of the {@code Stage}
     *
     * @throws IllegalStateException if this constructor is called on a thread
     * other than the JavaFX Application Thread.
     */
    public Stage(@Default("javafx.stage.StageStyle.DECORATED") StageStyle style) {
        super();

        Toolkit.getToolkit().checkFxUserThread();

        // Set the style
        initStyle(style);
    }
    
    /**
     * Specify the scene to be used on this stage.
     */
    @Override final public void setScene(Scene value) {
        super.setScene(value);
    }
    
    /**
     * @inheritDoc
     */
    @Override public final void show() {
        super.show();
    }
    
    private boolean primary = false;

    /**
     * sets this stage to be the primary stage.
     * When run as an applet, this stage will appear in the broswer
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * Returns whether this stage is the primary stage.
     * When run as an applet, the primary stage will appear in the broswer
     *
     * @return true if this stage is the primary stage for the application.
     */
    boolean isPrimary() {
        return primary;
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public String impl_getMXWindowType() {
        return (primary) ? "PrimaryStage" : getClass().getSimpleName();
    }

    private boolean important = true;

    /**
     * Sets a flag indicating whether this stage is an "important" window for
     * the purpose of determining whether the application is idle and should
     * exit. The application is considered finished when the last important
     * window is closed.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setImportant(boolean important) {
        this.important = important;
    }

    private boolean isImportant() {
        return important;
    }

    /**
     * Shows this stage and waits for it to be hidden (closed) before returning
     * to the caller. This method temporarily blocks processing of the current
     * event, and starts a nested event loop to handle other events.
     * This method must be called on the FX Application thread.
     * <p>
     * A Stage is hidden (closed) by one of the following means:
     * <ul>
     * <li>the application calls the {@link #hide} or {@link #close} method on
     * this stage</li>
     * <li>this stage has a non-null owner window, and its owner is closed</li>
     * <li>the user closes the window via the window system (for example,
     * by pressing the close button in the window decoration)</li>
     * </ul>
     * </p>
     *
     * <p>
     * After the Stage is hidden, and the application has returned from the
     * event handler to the event loop, the nested event loop terminates
     * and this method returns to the caller.
     * </p>
     * <p>
     * For example, consider the following sequence of operations for different
     * event handlers, assumed to execute in the order shown below:
     * <pre>void evtHander1(...) {
     *     stage1.showAndWait();
     *     doSomethingAfterStage1Closed(...)
     * }
     *
     * void evtHander2(...) {
     *     stage1.hide();
     *     doSomethingElseHere(...)
     * }</pre>
     * evtHandler1 will block at the call to showAndWait. It will resume execution
     * after stage1 is hidden and the current event handler, in this case evtHandler2,
     * returns to the event loop. This means that doSomethingElseHere will
     * execute before doSomethingAfterStage1Closed.
     * </p>
     *
     * <p>
     * More than one stage may be shown with showAndWait. Each call
     * will start a new nested event loop. The stages may be hidden in any order,
     * but a particular nested event loop (and thus the showAndWait method
     * for the associated stage) will only terminate after all inner event loops
     * have also terminated.
     * </p>
     * <p>
     * For example, consider the following sequence of operations for different
     * event handlers, assumed to execute in the order shown below:
     * <ul>
     * <pre>void evtHander1() {
     *     stage1.showAndWait();
     *     doSomethingAfterStage1Closed(...)
     * }
     *
     * void evtHander2() {
     *     stage2.showAndWait();
     *     doSomethingAfterStage2Closed(...)
     * }
     *
     * void evtHander3() {
     *     stage1.hide();
     *     doSomethingElseHere(...)
     * }
     *
     * void evtHander4() {
     *     stage2.hide();
     *     doSomethingElseHereToo(...)
     * }</pre>
     * </ul>
     * evtHandler1 will block at the call to stage1.showAndWait, starting up
     * a nested event loop just like in the previous example. evtHandler2 will
     * then block at the call to stage2.showAndWait, starting up another (inner)
     * nested event loop. The first call to stage1.showAndWait will resume execution
     * after stage1 is hidden, but only after the inner nested event loop started
     * by stage2.showAndWait has terminated. This means that the call to
     * stage1.showAndWait won't return until after evtHandler2 has returned.
     * The order of execution is: stage1.showAndWait, stage2.showAndWait,
     * stage1.hide, doSomethingElseHere, stage2.hide, doSomethingElseHereToo,
     * doSomethingAfterStage2Closed, doSomethingAfterStage1Closed.
     * </p>
     *
     * <p>
     * This method must not be called on the primary stage or on a stage that
     * is already visible.
     * </p>
     *
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     * @throws IllegalStateException if this method is called on the
     *     primary stage.
     * @throws IllegalStateException if this stage is already showing.
     * @since JavaFX 2.2
     */
    public void showAndWait() {

        Toolkit.getToolkit().checkFxUserThread();

        if (isPrimary()) {
            throw new IllegalStateException("Cannot call this method on primary stage");
        }

        if (isShowing()) {
            throw new IllegalStateException("Stage already visible");
        }

        // TODO: file a new bug; the following assertion can fail if this
        // method is called from an event handler that is listening to a
        // WindowEvent.WINDOW_HIDING event.
        assert !inNestedEventLoop;

        show();
        inNestedEventLoop = true;
        Toolkit.getToolkit().enterNestedEventLoop(this);
    }

    private StageStyle style; // default is set in constructor

    /**
     * Specifies the style for this stage. This must be done prior to making
     * the stage visible. The style is one of: StageStyle.DECORATED,
     * StageStyle.UNDECORATED, StageStyle.TRANSPARENT, or StageStyle.UTILITY.
     *
     * @param style the style for this stage.
     *
     * @throws IllegalStateException if this property is set after the stage
     * has ever been made visible.
     *
     * @defaultValue StageStyle.DECORATED
     */
    public final void initStyle(StageStyle style) {
        if (hasBeenVisible) {
            throw new IllegalStateException("Cannot set style once stage has been set visible");
        }
        this.style = style;
    }

    /**
     * Retrieves the style attribute for this stage.
     *
     * @return the stage style.
     */
    public final StageStyle getStyle() {
        return style;
    }

    private Modality modality = Modality.NONE;

    /**
     * Specifies the modality for this stage. This must be done prior to making
     * the stage visible. The modality is one of: Modality.NONE,
     * Modality.WINDOW_MODAL, or Modality.APPLICATION_MODAL.
     *
     * @param modality the modality for this stage.
     *
     * @throws IllegalStateException if this property is set after the stage
     * has ever been made visible.
     *
     * @throws IllegalStateException if this stage is the primary stage.
     *
     * @defaultValue Modality.NONE
     */
    public final void initModality(Modality modality) {
        if (hasBeenVisible) {
            throw new IllegalStateException("Cannot set modality once stage has been set visible");
        }

        if (isPrimary()) {
            throw new IllegalStateException("Cannot set modality for the primary stage");
        }

        this.modality = modality;
    }

    /**
     * Retrieves the modality attribute for this stage.
     *
     * @return the modality.
     */
    public final Modality getModality() {
        return modality;
    }

    private Window owner = null;

    /**
     * Specifies the owner Window for this stage, or null for a top-level,
     * unowned stage. This must be done prior to making the stage visible.
     *
     * @param owner the owner for this stage.
     *
     * @throws IllegalStateException if this property is set after the stage
     * has ever been made visible.
     *
     * @throws IllegalStateException if this stage is the primary stage.
     *
     * @defaultValue null
     */
    public final void initOwner(Window owner) {
        if (hasBeenVisible) {
            throw new IllegalStateException("Cannot set owner once stage has been set visible");
        }

        if (isPrimary()) {
            throw new IllegalStateException("Cannot set owner for the primary stage");
        }

        this.owner = owner;
        
        final Scene sceneValue = getScene();
        if (sceneValue != null) {
            SceneHelper.parentEffectiveOrientationInvalidated(sceneValue);
        }
    }

    /**
     * Retrieves the owner Window for this stage, or null for an unowned stage.
     *
     * @return the owner Window.
     */
    public final Window getOwner() {
        return owner;
    }

    /**
     * Specifies whether this {@code Stage} should be a full-screen,
     * undecorated window.
     * <p>
     * The implementation of full-screen mode is platform and profile-dependent.
     * </p>
     * <p>
     * When set to {@code true}, the {@code Stage} will attempt to enter
     * full-screen mode when visible. Set to {@code false} to return {@code Stage}
     * to windowed mode.
     * An {@link IllegalStateException} is thrown if this property is set
     * on a thread other than the JavaFX Application Thread.
     * </p>
     * <p>
     * The full-screen mode will be exited (and the {@code fullScreen} attribute
     * will be set to {@code false}) if the full-screen
     * {@code Stage} loses focus or if another {@code Stage} enters
     * full-screen mode on the same {@link Screen}. Note that a {@code Stage}
     * in full-screen mode can become invisible without losing its
     * full-screen status and will again enter full-screen mode when the
     * {@code Stage} becomes visible.
     * </p>
     * If the platform supports multiple screens an application can control
     * which {@code Screen} the Stage will enter full-screen mode on by
     * setting its position to be within the bounds of that {@code Screen}
     * prior to entering full-screen mode.
     * <p>
     * However once in full-screen mode, {@code Stage}'s {@code x}, {@code y},
     * {@code width}, and {@code height} variables will continue to represent
     * the non-full-screen position and size of the window; the same for
     * {@code iconified}, {@code resizable}, {@code style}, and {@code
     * opacity}. If changes are made to any of these attributes while in
     * full-screen mode, upon exiting full-screen mode the {@code Stage} will
     * assume those attributes.
     * </p>
     * <p>
     * In case that more {@code Stage} modes are set simultaneously their order
     * of importance is {@code iconified}, fullScreen, {@code maximized} (from
     * strongest to weakest).
     * </p>
     * <p>
     * The property is read only because it can be changed externally
     * by the underlying platform and therefore must not be bindable.
     * </p>
     *
     * Notes regarding desktop profile implementation.
     * <p>
     * For desktop profile the runtime will attempt to enter full-screen
     * exclusive mode (FSEM) if such is supported by the platform and it is
     * allowed for this application. If either is not the case a
     * simulated full-screen window will be used instead; the window will be
     * maximized, made undecorated if possible, and moved to the front.
     * </p>
     * For desktop profile the user can unconditionally exit full-screen mode
     * at any time by pressing {@code ESC}.
     * <p>
     * There are differences in behavior between signed and unsigned
     * applications. Signed applications are allowed to enter full-screen
     * exclusive mode unrestricted while unsigned applications will
     * have the following restrictions:
     * </p>
     * <ul>
     *  <li>Applications can only enter FSEM in response
     *   to user input. More specifically, entering is allowed from mouse
     *   ({@code Node.mousePressed/mouseReleased/mouseClicked}) or keyboard
     *   ({@code Node.keyPressed/keyReleased/keyTyped}) event handlers. It is
     *   not allowed to enter FSEM in response to {@code ESC}
     *   key. Attempting to enter FSEM from any other context will result in
     *   emulated full-screen mode.
     *   <p>
     *   If {@code Stage} was constructed as full-screen but not visible
     *   it will enter full-screen mode upon becoming visible, with the same
     *   limitations to when this is allowed to happen as when setting
     *   {@code fullScreen} to {@code true}.
     *   </p>
     *  </li>
     *  <li> If the application was allowed to enter FSEM
     *   it will have limited keyboard input. It will only receive KEY_PRESSED
     *   and KEY_RELEASED events from the following keys:
     *   {@code UP, DOWN, LEFT, RIGHT, SPACE, TAB, PAGE_UP, PAGE_DOWN, HOME, END, ENTER}
     *  </li>
     * </ul>
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper fullScreen;

    public final void setFullScreen(boolean value) {
        Toolkit.getToolkit().checkFxUserThread();
        fullScreenPropertyImpl().set(value);
        if (impl_peer != null)
            impl_peer.setFullScreen(value);
    }

    public final boolean isFullScreen() {
        return fullScreen == null ? false : fullScreen.get();
    }

    public final ReadOnlyBooleanProperty fullScreenProperty() {
        return fullScreenPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper fullScreenPropertyImpl () {
        if (fullScreen == null) {
            fullScreen = new ReadOnlyBooleanWrapper(Stage.this, "fullScreen");
        }
        return fullScreen;
    }

    /**
     * Defines the icon images to be used in the window decorations and when
     * minimized. The images should be different sizes of the same image and
     * the best size will be chosen, eg. 16x16, 32,32.
     *
     * @defaultValue empty
     */
    private ObservableList<Image> icons = new TrackableObservableList<Image>() {
        @Override protected void onChanged(Change<Image> c) {
            List<Object> platformImages = new ArrayList<Object>();
            for (Image icon : icons) {
                platformImages.add(icon.impl_getPlatformImage());
            }
            if (impl_peer != null) {
                impl_peer.setIcons(platformImages);
            }
        }
    };

    /**
     * Gets the icon images to be used in the window decorations and when
     * minimized. The images should be different sizes of the same image and
     * the best size will be chosen, eg. 16x16, 32,32.
     * @return An observable list of icons of this window
     */
    public final ObservableList<Image> getIcons() {
        return icons;
    }

    /**
     * Defines the title of the {@code Stage}.
     *
     * @defaultValue empty string
     */
    private StringProperty title;

    public final void setTitle(String value) {
        titleProperty().set(value);
    }

    public final String getTitle() {
        return title == null ? null : title.get();
    }

    public final StringProperty titleProperty() {
        if (title == null) {
            title = new StringPropertyBase() {

                @Override
                protected void invalidated() {
                    if (impl_peer != null) {
                        impl_peer.setTitle(get());
                    }
                }

                @Override
                public Object getBean() {
                    return Stage.this;
                }

                @Override
                public String getName() {
                    return "title";
                }
            };
        }
        return title;
    }

    /**
     * Defines whether the {@code Stage} is iconified or not.
     * <p>
     * In case that more {@code Stage} modes are set simultaneously their order
     * of importance is iconified} {@code fullScreen}, {@code maximized} (from
     * strongest to weakest).
     * </p>
     * <p>
     * On some mobile and embedded platforms setting this property to true will
     * hide the {@code Stage} but not show an icon for it.
     * </p>
     * <p>
     * The property is read only because it can be changed externally
     * by the underlying platform and therefore must not be bindable.
     * </p>
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper iconified;

    public final void setIconified(boolean value) {
        iconifiedPropertyImpl().set(value);
        if (impl_peer != null)
            impl_peer.setIconified(value);
    }

    public final boolean isIconified() {
        return iconified == null ? false : iconified.get();
    }

    public final ReadOnlyBooleanProperty iconifiedProperty() {
        return iconifiedPropertyImpl().getReadOnlyProperty();
    }

    private final ReadOnlyBooleanWrapper iconifiedPropertyImpl() {
        if (iconified == null) {
            iconified = new ReadOnlyBooleanWrapper(Stage.this, "iconified");
        }
        return iconified;
    }

    /**
     * Defines whether the {@code Stage} is maximized or not.
     * <p>
     * In case that more {@code Stage} modes are set simultaneously their order
     * of importance is {@code iconified}, {@code fullScreen}, maximized (from
     * strongest to weakest).
     * </p>
     * <p>
     * The property is read only because it can be changed externally
     * by the underlying platform and therefore must not be bindable.
     * </p>
     *
     * @defaultValue false
     * @since JavaFX 8.0
     */
    private ReadOnlyBooleanWrapper maximized;

    public final void setMaximized(boolean value) {
        maximizedPropertyImpl().set(value);
        if (impl_peer != null)
            impl_peer.setMaximized(value);
    }

    public final boolean isMaximized() {
        return maximized == null ? false : maximized.get();
    }

    public final ReadOnlyBooleanProperty maximizedProperty() {
        return maximizedPropertyImpl().getReadOnlyProperty();
    }

    private final ReadOnlyBooleanWrapper maximizedPropertyImpl() {
        if (maximized == null) {
            maximized = new ReadOnlyBooleanWrapper(Stage.this, "maximized");
        }
        return maximized;
    }

    /**
     * Defines whether the {@code Stage} is resizable or not by the user.
     * Programatically you may still change the size of the Stage. This is
     * a hint which allows the implementation to optionally make the Stage
     * resizable by the user.
     * <p>
     * <b>Warning:</b> Since 8.0 the property cannot be bound and will throw
     * {@code RuntimeException} on an attempt to do so. This is because
     * the setting of resizable is asynchronous on some systems or generally
     * might be set by the system / window manager.
     * <br>
     * Bidirectional binds are still allowed, as they don't block setting of the
     * property by the system.
     * 
     * @defaultValue true
     */
    private BooleanProperty resizable;

    public final void setResizable(boolean value) {
        resizableProperty().set(value);
    }

    public final boolean isResizable() {
        return resizable == null ? true : resizable.get();
    }

    public final BooleanProperty resizableProperty() {
        if (resizable == null) {
            resizable = new ResizableProperty();
        }
        return resizable;
    }

    //We cannot return ReadOnlyProperty in resizable, as this would be
    // backward incompatible. All we can do is to create this custom property
    // implementation that disallows binds
    private class ResizableProperty extends SimpleBooleanProperty {
        private boolean noInvalidate;

        public ResizableProperty() {
            super(Stage.this, "resizable", true);
        }

        void setNoInvalidate(boolean value) {
            noInvalidate = true;
            set(value);
            noInvalidate = false;
        }

        @Override
        protected void invalidated() {
            if (noInvalidate) {
                return;
            }
            if (impl_peer != null) {
                applyBounds();
                impl_peer.setResizable(get());
            }
        }

        @Override
        public void bind(ObservableValue<? extends Boolean> rawObservable) {
            throw new RuntimeException("Resizable property cannot be bound");
        }

    }

    /**
     * Defines the minimum width of this {@code Stage}.
     *
     * @defaultValue 0
     * @since JavaFX 2.1
     */
    private DoubleProperty minWidth;

    public final void setMinWidth(double value) {
        minWidthProperty().set(value);
    }

    public final double getMinWidth() {
        return minWidth == null ? 0 : minWidth.get();
    }

    public final DoubleProperty minWidthProperty() {
        if (minWidth == null) {
            minWidth = new DoublePropertyBase(0) {

                @Override
                protected void invalidated() {
                    if (impl_peer != null) {
                        impl_peer.setMinimumSize((int) Math.ceil(get()),
                                (int) Math.ceil(getMinHeight()));
                    }
                    if (getWidth() < getMinWidth()) {
                        setWidth(getMinWidth());
                    }
                }

                @Override
                public Object getBean() {
                    return Stage.this;
                }

                @Override
                public String getName() {
                    return "minWidth";
                }
            };
        }
        return minWidth;
    }

    /**
     * Defines the minimum height of this {@code Stage}.
     *
     * @defaultValue 0
     * @since JavaFX 2.1
     */
    private DoubleProperty minHeight;

    public final void setMinHeight(double value) {
        minHeightProperty().set(value);
    }

    public final double getMinHeight() {
        return minHeight == null ? 0 : minHeight.get();
    }

    public final DoubleProperty minHeightProperty() {
        if (minHeight == null) {
            minHeight = new DoublePropertyBase(0) {

                @Override
                protected void invalidated() {
                    if (impl_peer != null) {
                        impl_peer.setMinimumSize(
                                (int) Math.ceil(getMinWidth()),
                                (int) Math.ceil(get()));
                    }
                    if (getHeight() < getMinHeight()) {
                        setHeight(getMinHeight());
                    }
                }

                @Override
                public Object getBean() {
                    return Stage.this;
                }

                @Override
                public String getName() {
                    return "minHeight";
                }
            };
        }
        return minHeight;
    }

    /**
     * Defines the maximum width of this {@code Stage}.
     *
     * @defaultValue Double.MAX_VALUE
     * @since JavaFX 2.1
     */
    private DoubleProperty maxWidth;

    public final void setMaxWidth(double value) {
        maxWidthProperty().set(value);
    }

    public final double getMaxWidth() {
        return maxWidth == null ? Double.MAX_VALUE : maxWidth.get();
    }

    public final DoubleProperty maxWidthProperty() {
        if (maxWidth == null) {
            maxWidth = new DoublePropertyBase(Double.MAX_VALUE) {

                @Override
                protected void invalidated() {
                    if (impl_peer != null) {
                        impl_peer.setMaximumSize((int) Math.floor(get()),
                                (int) Math.floor(getMaxHeight()));
                    }
                    if (getWidth() > getMaxWidth()) {
                        setWidth(getMaxWidth());
                    }
                }

                @Override
                public Object getBean() {
                    return Stage.this;
                }

                @Override
                public String getName() {
                    return "maxWidth";
                }
            };
        }
        return maxWidth;
    }

    /**
     * Defines the maximum height of this {@code Stage}.
     *
     * @defaultValue Double.MAX_VALUE
     * @since JavaFX 2.1
     */
    private DoubleProperty maxHeight;

    public final void setMaxHeight(double value) {
        maxHeightProperty().set(value);
    }

    public final double getMaxHeight() {
        return maxHeight == null ? Double.MAX_VALUE : maxHeight.get();
    }

    public final DoubleProperty maxHeightProperty() {
        if (maxHeight == null) {
            maxHeight = new DoublePropertyBase(Double.MAX_VALUE) {

                @Override
                protected void invalidated() {
                    if (impl_peer != null) {
                        impl_peer.setMaximumSize(
                                (int) Math.floor(getMaxWidth()),
                                (int) Math.floor(get()));
                    }
                    if (getHeight() > getMaxHeight()) {
                        setHeight(getMaxHeight());
                    }
                }

                @Override
                public Object getBean() {
                    return Stage.this;
                }

                @Override
                public String getName() {
                    return "maxHeight";
                }
            };
        }
        return maxHeight;
    }

    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_visibleChanging(boolean value) {
        super.impl_visibleChanging(value);
        Toolkit toolkit = Toolkit.getToolkit();
        if (value && (impl_peer == null)) {
            // Setup the peer
            Window window = getOwner();
            TKStage tkStage = (window == null ? null : window.impl_getPeer());
            Scene scene = getScene();
            boolean rtl = scene != null && scene.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT;

            impl_peer = toolkit.createTKStage(getStyle(), isPrimary(), getModality(), tkStage, rtl);
            peerListener = new StagePeerListener(this, STAGE_ACCESSOR);
            
           // Insert this into stages so we have a references to all created stages
            stages.add(this);
        }
    }

    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_visibleChanged(boolean value) {
        super.impl_visibleChanged(value);

        if (value) {
            // Finish initialization
            impl_peer.setImportant(isImportant());
            impl_peer.setResizable(isResizable());
            impl_peer.setFullScreen(isFullScreen());
            impl_peer.setIconified(isIconified());
            impl_peer.setMaximized(isMaximized());
            impl_peer.setTitle(getTitle());
            impl_peer.setMinimumSize((int) Math.ceil(getMinWidth()),
                    (int) Math.ceil(getMinHeight()));
            impl_peer.setMaximumSize((int) Math.floor(getMaxWidth()),
                    (int) Math.floor(getMaxHeight()));

            List<Object> platformImages = new ArrayList<Object>();
            for (Image icon : icons) {
                platformImages.add(icon.impl_getPlatformImage());
            }
            if (impl_peer != null) {
                impl_peer.setIcons(platformImages);
            }
        }

        if (!value) {
            // Remove form active stage list
            stages.remove(this);
        }

        if (!value && inNestedEventLoop) {
            inNestedEventLoop = false;
            Toolkit.getToolkit().exitNestedEventLoop(this, null);
        }
    }

    /**
     * Bring the {@code Window} to the foreground.  If the {@code Window} is
     * already in the foreground there is no visible difference.
     */
    public void toFront() {
        if (impl_peer != null) {
            impl_peer.toFront();
        }
    }

    /**
     * Send the {@code Window} to the background.  If the {@code Window} is
     * already in the background there is no visible difference.  This action
     * places this {@code Window} at the bottom of the stacking order on
     * platforms that support stacking.
     */
    public void toBack() {
        if (impl_peer != null) {
            impl_peer.toBack();
        }
    }

    /**
     * Closes this {@code Stage}.
     * This call is equivalent to {@code hide()}.
     */
    public void close() {
        hide();
    }

    @Override
    Window getWindowOwner() {
        return getOwner();
    }
}
