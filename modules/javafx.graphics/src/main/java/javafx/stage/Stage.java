/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;

import com.sun.javafx.collections.VetoableListDecorator;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.stage.StageHelper;
import com.sun.javafx.stage.StagePeerListener;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;
import static com.sun.javafx.FXPermissions.CREATE_TRANSPARENT_WINDOW_PERMISSION;
import javafx.beans.NamedArg;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
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
 * The JavaFX Application Thread is created as part of the startup process for
 * the JavaFX runtime. See the {@link javafx.application.Application} class and
 * the {@link Platform#startup(Runnable)} method for more information.
 * </p>
 * <p>
 * Some {@code Stage} properties are read-only, even though they have
 * corresponding set methods, because they can be changed externally by the
 * underlying platform, and therefore must not be bindable.
 * Further, these properties might be ignored on some platforms, depending on
 * whether or not there is a window manager and how it is configured.
 * For example, a platform without a window manager might ignore the
 * {@code iconified} property.
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
 * <p>
 * Owned Stages are tied to the parent Window.
 * An owned stage will always be on top of its parent window.
 * When a parent window is closed or iconified, then all owned windows will be affected as well.
 * Owned Stages cannot be independantly iconified.
 * <p>
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

    {@literal @Override} public void start(Stage stage) {
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
 * <p><img src="doc-files/Stage-win.png" alt="A visual rendering
     * of a JavaFX Stage on Windows"></p>
 *
 * <p>produces the following on Mac OSX:</p>
 * <p><img src="doc-files/Stage-mac.png" alt="A visual rendering
     * of a JavaFX Stage on Mac OSX"></p>
 *
 * <p>produces the following on Linux:</p>
 * <p><img src="doc-files/Stage-linux.png" alt="A visual rendering
     * of a JavaFX Stage on Linux"></p>
 * @since JavaFX 2.0
 */
public class Stage extends Window {

    private boolean inNestedEventLoop = false;

    static {
        StageHelper.setStageAccessor(new StageHelper.StageAccessor() {
            @Override public void doVisibleChanging(Window window, boolean visible) {
                ((Stage) window).doVisibleChanging(visible);
            }

            @Override public void doVisibleChanged(Window window, boolean visible) {
                ((Stage) window).doVisibleChanged(visible);
            }

            @Override public void initSecurityDialog(Stage stage, boolean securityDialog) {
                stage.initSecurityDialog(securityDialog);
            }

            @Override
            public void setPrimary(Stage stage, boolean primary) {
                stage.setPrimary(primary);
            }

            @Override
            public void setImportant(Stage stage, boolean important) {
                stage.setImportant(important);
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

        @Override
        public void setAlwaysOnTop(Stage stage, boolean aot) {
            stage.alwaysOnTopPropertyImpl().set(aot);
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
    public Stage(@NamedArg(value="style", defaultValue="DECORATED") StageStyle style) {
        super();

        Toolkit.getToolkit().checkFxUserThread();

        // Set the style
        initStyle(style);
        StageHelper.initHelper(this);
    }

    /**
     * Specify the scene to be used on this stage.
     */
    @Override final public void setScene(Scene value) {
        Toolkit.getToolkit().checkFxUserThread();
        super.setScene(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void show() {
        super.show();
    }

    private boolean primary = false;

    ////////////////////////////////////////////////////////////////////

    // Flag indicating that this stage is being used to show a security dialog
    private boolean securityDialog = false;

    /**
     * Sets a flag indicating that this stage is used for a security dialog and
     * must always be on top. If set, this will cause the window to be always
     * on top, regardless of the setting of the alwaysOnTop property, and
     * whether or not permissions are granted when the dialog is shown.
     * NOTE: this flag must be set prior to showing the stage the first time.
     *
     * @param securityDialog flag indicating that this Stage is being used to
     * show a security dialog that should be always-on-top
     *
     * @throws IllegalStateException if this property is set after the stage
     * has ever been made visible.
     *
     * @defaultValue false
     */
    final void initSecurityDialog(boolean securityDialog) {
        if (hasBeenVisible) {
            throw new IllegalStateException("Cannot set securityDialog once stage has been set visible");
        }

        this.securityDialog = securityDialog;
    }

    /**
     * Returns the state of the securityDialog flag.
     *
     * @return a flag indicating whether or not this is a security dialog
     */
    final boolean isSecurityDialog() {
        return securityDialog;
    }

    /*
     * Sets this stage to be the primary stage.
     */
    void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /*
     * Returns whether this stage is the primary stage.
     *
     * @return true if this stage is the primary stage for the application.
     */
    boolean isPrimary() {
        return primary;
    }

    private boolean important = true;

    /*
     * Sets a flag indicating whether this stage is an "important" window for
     * the purpose of determining whether the application is idle and should
     * exit. The application is considered finished when the last important
     * window is closed.
     */
    void setImportant(boolean important) {
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
     *
     * <p>
     * This method must not be called on the primary stage or on a stage that
     * is already visible.
     * Additionally, it must either be called from an input event handler or
     * from the run method of a Runnable passed to
     * {@link javafx.application.Platform#runLater Platform.runLater}.
     * It must not be called during animation or layout processing.
     * </p>
     *
     * @throws IllegalStateException if this method is called on a thread
     *     other than the JavaFX Application Thread.
     * @throws IllegalStateException if this method is called during
     *     animation or layout processing.
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

        if (!Toolkit.getToolkit().canStartNestedEventLoop()) {
            throw new IllegalStateException("showAndWait is not allowed during animation or layout processing");
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
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     *
     * The user can unconditionally exit full-screen mode
     * at any time by pressing {@code ESC}.
     * <p>
     * If a security manager is present, the application must have the
     * {@link javafx.util.FXPermission} "unrestrictedFullScreen" in order
     * to enter full-screen mode with no restrictions. Applications without
     * permission will have the following restrictions:
     * </p>
     * <ul>
     *  <li>Applications can only enter full-screen mode in response
     *   to user input. More specifically, entering is allowed from mouse
     *   ({@code Node.mousePressed/mouseReleased/mouseClicked}) or keyboard
     *   ({@code Node.keyPressed/keyReleased/keyTyped}) event handlers. It is
     *   not allowed to enter full-screen mode in response to {@code ESC}
     *   key. Attempting to enter full-screen mode from any other context will
     *   be ignored.
     *   <p>
     *   If {@code Stage} was constructed as full-screen but not visible
     *   it will enter full-screen mode upon becoming visible, with the same
     *   limitations to when this is allowed to happen as when setting
     *   {@code fullScreen} to {@code true}.
     *   </p>
     *  </li>
     *  <li> If the application was allowed to enter full-screen mode
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
        if (getPeer() != null)
            getPeer().setFullScreen(value);
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
    private ObservableList<Image> icons = new VetoableListDecorator<Image>(new TrackableObservableList<Image>() {
        @Override protected void onChanged(Change<Image> c) {
            List<Object> platformImages = new ArrayList<Object>();
            for (Image icon : icons) {
                platformImages.add(Toolkit.getImageAccessor().getPlatformImage(icon));
            }
            if (getPeer() != null) {
                getPeer().setIcons(platformImages);
            }
        }
    }) {
        @Override protected void onProposedChange(
                final List<Image> toBeAddedIcons, int[] indices) {
            for (Image icon : toBeAddedIcons) {
                if (icon == null) {
                    throw new NullPointerException("icon can not be null.");
                }
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
                    if (getPeer() != null) {
                        getPeer().setTitle(get());
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
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper iconified;

    public final void setIconified(boolean value) {
        iconifiedPropertyImpl().set(value);
        if (getPeer() != null)
            getPeer().setIconified(value);
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
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     *
     * @defaultValue false
     * @since JavaFX 8.0
     */
    private ReadOnlyBooleanWrapper maximized;

    public final void setMaximized(boolean value) {
        maximizedPropertyImpl().set(value);
        if (getPeer() != null) {
            getPeer().setMaximized(value);
        }
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
     * Defines whether this {@code Stage} is kept on top of other windows.
     * <p>
     * If some other window is already always-on-top then the
     * relative order between these windows is unspecified (depends on
     * platform).
     * </p>
     * <p>
     * If a security manager is present, the application must have the
     * {@link javafx.util.FXPermission} "setWindowAlwaysOnTop" in order for
     * this property to have any effect. If the application does not have
     * permission, attempting to set this property will be ignored
     * and the property value will be restored to {@code false}.
     * </p>
     * <p>
     * This property is read-only because it can be changed externally
     * by the underlying platform.
     * Further, setting this property might be ignored on some platforms.
     * </p>
     *
     * @defaultValue false
     * @since JavaFX 8u20
     */
    private ReadOnlyBooleanWrapper alwaysOnTop;

    public final void setAlwaysOnTop(boolean value) {
        alwaysOnTopPropertyImpl().set(value);
        if (getPeer() != null) {
            getPeer().setAlwaysOnTop(value);
        }
    }

    public final boolean isAlwaysOnTop() {
        return alwaysOnTop == null ? false : alwaysOnTop.get();
    }

    public final ReadOnlyBooleanProperty alwaysOnTopProperty() {
        return alwaysOnTopPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper alwaysOnTopPropertyImpl() {
        if (alwaysOnTop == null) {
            alwaysOnTop = new ReadOnlyBooleanWrapper(Stage.this, "alwaysOnTop");
        }
        return alwaysOnTop;
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
            if (getPeer() != null) {
                applyBounds();
                getPeer().setResizable(get());
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
                    if (getPeer() != null) {
                        getPeer().setMinimumSize((int) Math.ceil(get()),
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
                    if (getPeer() != null) {
                        getPeer().setMinimumSize(
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
                    if (getPeer() != null) {
                        getPeer().setMaximumSize((int) Math.floor(get()),
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
                    if (getPeer() != null) {
                        getPeer().setMaximumSize(
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

    /*
     * This can be replaced by listening for the onShowing/onHiding events
     * Note: This method MUST only be called via its accessor method.
     */
    private void doVisibleChanging(boolean value) {
        Toolkit toolkit = Toolkit.getToolkit();
        if (value && (getPeer() == null)) {
            // Setup the peer
            Window window = getOwner();
            TKStage tkStage = (window == null ? null : window.getPeer());
            Scene scene = getScene();
            boolean rtl = scene != null && scene.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT;

            StageStyle stageStyle = getStyle();
            if (stageStyle == StageStyle.TRANSPARENT) {
                @SuppressWarnings("removal")
                final SecurityManager securityManager =
                        System.getSecurityManager();
                if (securityManager != null) {
                    try {
                        securityManager.checkPermission(CREATE_TRANSPARENT_WINDOW_PERMISSION);
                    } catch (final SecurityException e) {
                        stageStyle = StageStyle.UNDECORATED;
                    }
                }
            }
            setPeer(toolkit.createTKStage(this, isSecurityDialog(),
                    stageStyle, isPrimary(), getModality(), tkStage, rtl, acc));
            getPeer().setMinimumSize((int) Math.ceil(getMinWidth()),
                    (int) Math.ceil(getMinHeight()));
            getPeer().setMaximumSize((int) Math.floor(getMaxWidth()),
                    (int) Math.floor(getMaxHeight()));
            setPeerListener(new StagePeerListener(this, STAGE_ACCESSOR));
        }
    }


    /*
     * This can be replaced by listening for the onShown/onHidden events
     * Note: This method MUST only be called via its accessor method.
     */
    private void doVisibleChanged(boolean value) {
        if (value) {
            // Finish initialization
            TKStage peer = getPeer();
            peer.setImportant(isImportant());
            peer.setResizable(isResizable());
            peer.setFullScreen(isFullScreen());
            peer.setAlwaysOnTop(isAlwaysOnTop());
            peer.setIconified(isIconified());
            peer.setMaximized(isMaximized());
            peer.setTitle(getTitle());

            List<Object> platformImages = new ArrayList<Object>();
            for (Image icon : icons) {
                platformImages.add(Toolkit.getImageAccessor().getPlatformImage(icon));
            }
            if (peer != null) {
                peer.setIcons(platformImages);
            }
        }

        if (!value && inNestedEventLoop) {
            inNestedEventLoop = false;
            Toolkit.getToolkit().exitNestedEventLoop(this, null);
        }
    }

    /**
     * Brings this {@code Stage} to the front if the stage is visible.
     * This action places this {@code Stage} at the top of the stacking
     * order and shows it in front of any other {@code Stage} created by this
     * application.
     * <p>
     * Some platforms do not allow applications to control the stacking order
     * at all, in which case this method does nothing.
     * Other platforms have restrictions on stacking order, so might not
     * place a window above another application's windows
     * nor allow a window that owns other windows to appear on top of those
     * owned windows.
     * Every attempt will be made to move this {@code Stage} as high as
     * possible in the stacking order; however, developers should not assume
     * that this method will move this {@code Stage} above all other windows
     * in every situation.
     * </p>
     */
    public void toFront() {
        if (getPeer() != null) {
            getPeer().toFront();
        }
    }

    /**
     * Sends this {@code Stage} to the back if the stage is visible.
     * This action places this {@code Stage} at the bottom of the stacking
     * order and shows it behind any other {@code Stage} created by this
     * application.
     * <p>
     * Some platforms do not allow applications to control the stacking order
     * at all, in which case this method does nothing.
     * Other platforms have restrictions on stacking order, so might not
     * place a window below another application's windows
     * nor allow a window that is owned by another window to appear below their
     * owner.
     * Every attempt will be made to move this {@code Stage} as low as
     * possible in the stacking order; however, developers should not assume
     * that this method will move this {@code Stage} below all other windows
     * in every situation.
     * </p>
     */
    public void toBack() {
        if (getPeer() != null) {
            getPeer().toBack();
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


    private final ObjectProperty<KeyCombination> fullScreenExitCombination =
            new SimpleObjectProperty<KeyCombination>(this, "fullScreenExitCombination", null);

    /**
     * Specifies the KeyCombination that will allow the user to exit full screen
     * mode. A value of KeyCombination.NO_MATCH will not match any KeyEvent and
     * will make it so the user is not able to escape from Full Screen mode.
     * A value of null indicates that the default platform specific key combination
     * should be used.
     * <p>
     * An internal copy of this value is made when entering full-screen mode and will be
     * used to trigger the exit from the mode.
     * If a security manager is present, the application must have the
     * {@link javafx.util.FXPermission} "unrestrictedFullScreen" to modify the
     * exit key combination. If the application does not have permission, the
     * value of this property will be ignored, in which case the
     * default key combination will be used.
     * </p>
     * @param keyCombination the key combination to exit on
     * @since JavaFX 8.0
     */
    public final void setFullScreenExitKeyCombination(KeyCombination keyCombination) {
        fullScreenExitCombination.set(keyCombination);
    }

    /**
     * Get the current sequence used to exit Full Screen mode.
     * @return the current setting (null for system default)
     * @since JavaFX 8.0
     */
    public final KeyCombination getFullScreenExitKeyCombination() {
        return fullScreenExitCombination.get();
    }

    /**
     * Get the property for the Full Screen exit key combination.
     * @return the property.
     * @since JavaFX 8.0
     */
    public final ObjectProperty<KeyCombination> fullScreenExitKeyProperty() {
        return fullScreenExitCombination;
    }

    /**
     * Specifies the text to show when a user enters full screen mode, usually
     * used to indicate the way a user should go about exiting out of full
     * screen mode. A value of null will result in the default per-locale
     * message being displayed.
     * If set to the empty string, then no message will be displayed.
     * <p>
     * If a security manager is present, the application must have the
     * {@link javafx.util.FXPermission} "unrestrictedFullScreen" to modify the
     * exit hint. If the application does not have permission, the
     * value of this property will be ignored, in which case the
     * default message will be displayed.
     * </p>
     * @since JavaFX 8.0
     */
    private final ObjectProperty<String> fullScreenExitHint =
            new SimpleObjectProperty<String>(this, "fullScreenExitHint", null);

    public final void setFullScreenExitHint(String value) {
        fullScreenExitHint.set(value);
    }

    public final String getFullScreenExitHint() {
        return fullScreenExitHint.get();
    }

    public final ObjectProperty<String> fullScreenExitHintProperty() {
        return fullScreenExitHint;
    }
}
