/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import com.sun.javafx.scene.web.Debugger;
import javafx.animation.AnimationTimer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.util.Callback;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import java.io.StringReader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javafx.beans.property.*;
import javafx.geometry.Rectangle2D;

/**
 * {@code WebEngine} is a non-visual object capable of managing one Web page
 * at a time. One can load Web pages into an engine, track loading progress,
 * access document model of a loaded page, and execute JavaScript on the page.
 *
 * <p>Loading is always asynchronous. Methods that initiate loading return
 * immediately after scheduling a job, so one must not assume loading is
 * complete by that time. {@link #getLoadWorker} method can be used to track
 * loading status.
 *
 * <p>A number of JavaScript handlers and callbacks may be registered with a
 * {@code WebEngine}. These are invoked when a script running on the page
 * accesses user interface elements that lie beyond the control of the
 * {@code WebEngine}, such as browser window, toolbar or status line.
 *
 * <p>{@code WebEngine} objects must be created and accessed solely from the
 * FXthread.
 * @since JavaFX 2.0
 */
final public class WebEngine {

    /**
     * The node associated with this engine. There is a one-to-one correspondance
     * between the WebView and its WebEngine (although not all WebEngines have
     * a WebView, every WebView has one and only one WebEngine).
     */
    private final ObjectProperty<WebView> view = new SimpleObjectProperty<WebView>(this, "view");

    /**
     * The Worker which shows progress of the web engine as it loads pages.
     */
    private final LoadWorker loadWorker = new LoadWorker();

    /**
     * Returns a {@link javafx.concurrent.Worker} object that can be used to
     * track loading progress.
     */
    public final Worker<Void> getLoadWorker() {
        return loadWorker;
    }

    private void updateProgress(double p) {
        LoadWorker lw = (LoadWorker) getLoadWorker();
        if (lw != null) {
            lw.updateProgress(p);
        }
    }

    private void updateState(Worker.State s) {
        LoadWorker lw = (LoadWorker) getLoadWorker();
        if (lw != null) {
            lw.updateState(s);
        }
    }

    /**
     * The final document. This may be null if no document has been loaded.
     */
    private final DocumentProperty document = new DocumentProperty();

    /**
     * Returns the document object for the current Web page. If the Web page
     * failed to load, returns {@code null}.
     */
    public final Document getDocument() { return document.getValue(); }

    /**
     * Document object for the current Web page. The value is {@code null}
     * if the Web page failed to load.
     */
    public final ReadOnlyObjectProperty<Document> documentProperty() {
        return document;
    }

    /**
     * The location of the current page. This may return null.
     */
    private final ReadOnlyStringWrapper location = new ReadOnlyStringWrapper(this, "location");

    /**
     * Returns URL of the current Web page. If the current page has no URL,
     * returns an empty String.
     */
    public final String getLocation() { return location.getValue(); }

    /**
     * URL of the current Web page. If the current page has no URL,
     * the value is an empty String.
     */
    public final ReadOnlyStringProperty locationProperty() { return location.getReadOnlyProperty(); }

    /**
     * The page title.
     */
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper(this, "title");

    /**
     * Returns title of the current Web page. If the current page has no title,
     * returns {@code null}.
     */
    public final String getTitle() { return title.getValue(); }

    /**
     * Title of the current Web page. If the current page has no title,
     * the value is {@code null}.
     */
    public final ReadOnlyStringProperty titleProperty() { return title.getReadOnlyProperty(); }

    //
    // Settings

    /**
     * Specifies whether JavaScript execution is enabled.
     *
     * @defaultValue true
     * @since JavaFX 2.2
     */
    private BooleanProperty javaScriptEnabled;

    public final void setJavaScriptEnabled(boolean value) {
        javaScriptEnabledProperty().set(value);
    }

    public final boolean isJavaScriptEnabled() {
        return javaScriptEnabled == null ? true : javaScriptEnabled.get();
    }

    private Debugger debugger = new DebuggerImpl();

    Debugger getDebugger(){
        return debugger;
    }

    public final BooleanProperty javaScriptEnabledProperty() {
        if (javaScriptEnabled == null) {
            javaScriptEnabled = new BooleanPropertyBase(true) {
                @Override public void invalidated() {
                    checkThread();
                }

                @Override public Object getBean() {
                    return WebEngine.this;
                }

                @Override public String getName() {
                    return "javaScriptEnabled";
                }
            };
        }
        return javaScriptEnabled;
    }

    /**
     * Location of the user stylesheet as a string URL.
     *
     * <p>This should be a local URL, i.e. either {@code 'data:'},
     * {@code 'file:'}, or {@code 'jar:'}. Remote URLs are not allowed
     * for security reasons.
     *
     * @defaultValue null
     * @since JavaFX 2.2
     */
    private StringProperty userStyleSheetLocation;

    public final void setUserStyleSheetLocation(String value) {
        userStyleSheetLocationProperty().set(value);
    }

    public final String getUserStyleSheetLocation() {
        return userStyleSheetLocation == null ? null : userStyleSheetLocation.get();
    }

    public final StringProperty userStyleSheetLocationProperty() {
        if (userStyleSheetLocation == null) {
            userStyleSheetLocation = new StringPropertyBase(null) {
                private final static String DATA_PREFIX = "data:text/css;charset=utf-8;base64,";

                @Override public void invalidated() {
                    checkThread();
                    String url = get();
                    String dataUrl;
                    if (url == null || url.length() <= 0) {
                        dataUrl = null;
                    } else if (url.startsWith(DATA_PREFIX)) {
                        dataUrl = url;
                    } else if (url.startsWith("file:") ||
                               url.startsWith("jar:")  ||
                               url.startsWith("data:")) {

                    } else {
                        throw new IllegalArgumentException("Invalid stylesheet URL");
                    }
                }

                @Override public Object getBean() {
                    return WebEngine.this;
                }

                @Override public String getName() {
                    return "userStyleSheetLocation";
                }
            };
        }
        return userStyleSheetLocation;
    }

    /**
     * Specifies user agent ID string. This string is the value of the
     * {@code User-Agent} HTTP header.
     *
     * @defaultValue system dependent
     * @since JavaFX 8.0
     */
    private StringProperty userAgent;

    public final void setUserAgent(String value) {
        userAgentProperty().set(value);
    }

    public final String getUserAgent() {
        return userAgent == null ? null : userAgent.get();
    }

    public final StringProperty userAgentProperty() {
        if (userAgent == null) {
            userAgent = new StringPropertyBase() {
                @Override public void invalidated() {
                    checkThread();
                }

                @Override public Object getBean() {
                    return WebEngine.this;
                }

                @Override public String getName() {
                    return "userAgent";
                }
            };
        }
        return userAgent;
    }

    private final ObjectProperty<EventHandler<WebEvent<String>>> onAlert
            = new SimpleObjectProperty<EventHandler<WebEvent<String>>>(this, "onAlert");

    /**
     * Returns the JavaScript {@code alert} handler.
     * @see #onAlertProperty
     * @see #setOnAlert
     */
    public final EventHandler<WebEvent<String>> getOnAlert() { return onAlert.get(); }

    /**
     * Sets the JavaScript {@code alert} handler.
     * @see #onAlertProperty
     * @see #getOnAlert
     */
    public final void setOnAlert(EventHandler<WebEvent<String>> handler) { onAlert.set(handler); }

    /**
     * JavaScript {@code alert} handler property. This handler is invoked
     * when a script running on the Web page calls the {@code alert} function.
     */
    public final ObjectProperty<EventHandler<WebEvent<String>>> onAlertProperty() { return onAlert; }


    private final ObjectProperty<EventHandler<WebEvent<String>>> onStatusChanged
            = new SimpleObjectProperty<EventHandler<WebEvent<String>>>(this, "onStatusChanged");

    /**
     * Returns the JavaScript status handler.
     * @see #onStatusChangedProperty
     * @see #setOnStatusChanged
     */
    public final EventHandler<WebEvent<String>> getOnStatusChanged() { return onStatusChanged.get(); }

    /**
     * Sets the JavaScript status handler.
     * @see #onStatusChangedProperty
     * @see #getOnStatusChanged
     */
    public final void setOnStatusChanged(EventHandler<WebEvent<String>> handler) { onStatusChanged.set(handler); }

    /**
     * JavaScript status handler property. This handler is invoked when
     * a script running on the Web page sets {@code window.status} property.
     */
    public final ObjectProperty<EventHandler<WebEvent<String>>> onStatusChangedProperty() { return onStatusChanged; }


    private final ObjectProperty<EventHandler<WebEvent<Rectangle2D>>> onResized
            = new SimpleObjectProperty<EventHandler<WebEvent<Rectangle2D>>>(this, "onResized");

    /**
     * Returns the JavaScript window resize handler.
     * @see #onResizedProperty
     * @see #setOnResized
     */
    public final EventHandler<WebEvent<Rectangle2D>> getOnResized() { return onResized.get(); }

    /**
     * Sets the JavaScript window resize handler.
     * @see #onResizedProperty
     * @see #getOnResized
     */
    public final void setOnResized(EventHandler<WebEvent<Rectangle2D>> handler) { onResized.set(handler); }

    /**
     * JavaScript window resize handler property. This handler is invoked
     * when a script running on the Web page moves or resizes the
     * {@code window} object.
     */
    public final ObjectProperty<EventHandler<WebEvent<Rectangle2D>>> onResizedProperty() { return onResized; }


    private final ObjectProperty<EventHandler<WebEvent<Boolean>>> onVisibilityChanged
            = new SimpleObjectProperty<EventHandler<WebEvent<Boolean>>>(this, "onVisibilityChanged");

    /**
     * Returns the JavaScript window visibility handler.
     * @see #onVisibilityChangedProperty
     * @see #setOnVisibilityChanged
     */
    public final EventHandler<WebEvent<Boolean>> getOnVisibilityChanged() { return onVisibilityChanged.get(); }

    /**
     * Sets the JavaScript window visibility handler.
     * @see #onVisibilityChangedProperty
     * @see #getOnVisibilityChanged
     */
    public final void setOnVisibilityChanged(EventHandler<WebEvent<Boolean>> handler) { onVisibilityChanged.set(handler); }

    /**
     * JavaScript window visibility handler property. This handler is invoked
     * when a script running on the Web page changes visibility of the
     * {@code window} object.
     */
    public final ObjectProperty<EventHandler<WebEvent<Boolean>>> onVisibilityChangedProperty() { return onVisibilityChanged; }


    private final ObjectProperty<Callback<PopupFeatures, WebEngine>> createPopupHandler
            = new SimpleObjectProperty<Callback<PopupFeatures, WebEngine>>(this, "createPopupHandler",
            p -> WebEngine.this);

    /**
     * Returns the JavaScript popup handler.
     * @see #createPopupHandlerProperty
     * @see #setCreatePopupHandler
     */
    public final Callback<PopupFeatures, WebEngine> getCreatePopupHandler() { return createPopupHandler.get(); }

    /**
     * Sets the JavaScript popup handler.
     * @see #createPopupHandlerProperty
     * @see #getCreatePopupHandler
     * @see PopupFeatures
     */
    public final void setCreatePopupHandler(Callback<PopupFeatures, WebEngine> handler) { createPopupHandler.set(handler); }

    /**
     * JavaScript popup handler property. This handler is invoked when a script
     * running on the Web page requests a popup to be created.
     * <p>To satisfy this request a handler may create a new {@code WebEngine},
     * attach a visibility handler and optionally a resize handler, and return
     * the newly created engine. To block the popup, a handler should return
     * {@code null}.
     * <p>By default, a popup handler is installed that opens popups in this
     * {@code WebEngine}.
     *
     * @see PopupFeatures
     */
    public final ObjectProperty<Callback<PopupFeatures, WebEngine>> createPopupHandlerProperty() { return createPopupHandler; }


    private final ObjectProperty<Callback<String, Boolean>> confirmHandler
            = new SimpleObjectProperty<Callback<String, Boolean>>(this, "confirmHandler");

    /**
     * Returns the JavaScript {@code confirm} handler.
     * @see #confirmHandlerProperty
     * @see #setConfirmHandler
     */
    public final Callback<String, Boolean> getConfirmHandler() { return confirmHandler.get(); }

    /**
     * Sets the JavaScript {@code confirm} handler.
     * @see #confirmHandlerProperty
     * @see #getConfirmHandler
     */
    public final void setConfirmHandler(Callback<String, Boolean> handler) { confirmHandler.set(handler); }

    /**
     * JavaScript {@code confirm} handler property. This handler is invoked
     * when a script running on the Web page calls the {@code confirm} function.
     * <p>An implementation may display a dialog box with Yes and No options,
     * and return the user's choice.
     */
    public final ObjectProperty<Callback<String, Boolean>> confirmHandlerProperty() { return confirmHandler; }


    private final ObjectProperty<Callback<PromptData, String>> promptHandler
            = new SimpleObjectProperty<Callback<PromptData, String>>(this, "promptHandler");

    /**
     * Returns the JavaScript {@code prompt} handler.
     * @see #promptHandlerProperty
     * @see #setPromptHandler
     * @see PromptData
     */
    public final Callback<PromptData, String> getPromptHandler() { return promptHandler.get(); }

    /**
     * Sets the JavaScript {@code prompt} handler.
     * @see #promptHandlerProperty
     * @see #getPromptHandler
     * @see PromptData
     */
    public final void setPromptHandler(Callback<PromptData, String> handler) { promptHandler.set(handler); }

    /**
     * JavaScript {@code prompt} handler property. This handler is invoked
     * when a script running on the Web page calls the {@code prompt} function.
     * <p>An implementation may display a dialog box with an text field,
     * and return the user's input.
     *
     * @see PromptData
     */
    public final ObjectProperty<Callback<PromptData, String>> promptHandlerProperty() { return promptHandler; }

    /**
     * The event handler called when an error occurs.
     *
     * @defaultValue {@code null}
     * @since JavaFX 8.0
     */
    private final ObjectProperty<EventHandler<WebErrorEvent>> onError =
            new SimpleObjectProperty<>(this, "onError");

    public final EventHandler<WebErrorEvent> getOnError() {
        return onError.get();
    }

    public final void setOnError(EventHandler<WebErrorEvent> handler) {
        onError.set(handler);
    }

    public final ObjectProperty<EventHandler<WebErrorEvent>> onErrorProperty() {
        return onError;
    }


    /**
     * Creates a new engine.
     */
    public WebEngine() {
        this(null);
    }

    /**
     * Creates a new engine and loads a Web page into it.
     */
    public WebEngine(String url) {
        accessControlContext = AccessController.getContext();
        js2javaBridge = new JS2JavaBridge(this);
        load(url);
    }

    /**
     * Loads a Web page into this engine. This method starts asynchronous
     * loading and returns immediately.
     * @param url URL of the web page to load
     */
    public void load(String url) {
        checkThread();

        if (url == null) {
            url = "";
        }

        if (view.get() != null) {
            _loadUrl(view.get().getNativeHandle(), url);
        }
    }

    /* Loads a web page */
    private native void _loadUrl(long handle, String url);

    /**
     * Loads the given HTML content directly. This method is useful when you have an HTML
     * String composed in memory, or loaded from some system which cannot be reached via
     * a URL (for example, the HTML text may have come from a database). As with
     * {@link #load(String)}, this method is asynchronous.
     */
    public void loadContent(String content) {
        loadContent(content, "text/html");
    }

    /**
     * Loads the given content directly. This method is useful when you have content
     * composed in memory, or loaded from some system which cannot be reached via
     * a URL (for example, the SVG text may have come from a database). As with
     * {@link #load(String)}, this method is asynchronous. This method also allows you to
     * specify the content type of the string being loaded, and so may optionally support
     * other types besides just HTML.
     */
    public void loadContent(String content, String contentType) {
        checkThread();
        LoadWorker lw = (LoadWorker) getLoadWorker();
        if (lw != null) {
            lw.cancelAndReset();
        }
        _loadContent(view.get().getNativeHandle(), content);
    }

    /* Loads the given content directly */
    private native void _loadContent(long handle, String content);

    /* Reloads the current content directly */
    private native void _reload(long handle);

    /**
     * Reloads the current page, whether loaded from URL or directly from a String in
     * one of the {@code loadContent} methods.
     */
    public void reload() {
        checkThread();
        _reload(view.get().getNativeHandle());
    }

    /**
     * Executes a script in the context of the current page.
     * @return execution result, converted to a Java object using the following
     * rules:
     * <ul>
     * <li>JavaScript Int32 is converted to {@code java.lang.Integer}
     * <li>Other JavaScript numbers to {@code java.lang.Double}
     * <li>JavaScript string to {@code java.lang.String}
     * <li>JavaScript boolean to {@code java.lang.Boolean}
     * <li>JavaScript {@code null} to {@code null}
     * <li>Most JavaScript objects get wrapped as
     *     {@code netscape.javascript.JSObject}
     * <li>JavaScript JSNode objects get mapped to instances of
     *     {@code netscape.javascript.JSObject}, that also implement
     *     {@code org.w3c.dom.Node}
     * <li>A special case is the JavaScript class {@code JavaRuntimeObject}
     *     which is used to wrap a Java object as a JavaScript value - in this
     *     case we just extract the original Java value.
     * </ul>
     */
    public Object executeScript(String script) {
        checkThread();

        StringBuilder b = new StringBuilder(js2javaBridge.getJavaBridge()).append(".fxEvaluate('");
        b.append(escapeScript(script));
        b.append("')");
        String retVal = _executeScript(view.get().getNativeHandle(), b.toString());
        if (retVal != null) {
            try {
                return js2javaBridge.decode(retVal);
            } catch (Exception ex) {
                System.err.println("Couldn't parse arguments. " + ex);
            }
        }
        return null;
    }

    void executeScriptDirect(String script) {
        _executeScript(view.get().getNativeHandle(), script);
    }

    /* Executes a script in the context of the current page */
    private native String _executeScript(long handle, String script);

    void setView(WebView view) {
        this.view.setValue(view);
    }

    private void stop() {
        checkThread();
    }

    private String escapeScript(String script) {
        final int len = script.length();
        StringBuilder sb = new StringBuilder((int) (len * 1.2));
        for (int i = 0; i < len; i++) {
            char ch = script.charAt(i);
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Drives the {@code Timer} when {@code Timer.Mode.PLATFORM_TICKS} is set.
     */
    private static final class PulseTimer {

        // Used just to guarantee constant pulse activity. See RT-14433.
        private static final AnimationTimer animation =
            new AnimationTimer() {
                @Override public void handle(long l) {}
            };

        private static final TKPulseListener listener =
                () -> {
                    // Note, the timer event is executed right in the notifyTick(),
                    // that is during the pulse event. This makes the timer more
                    // repsonsive, though prolongs the pulse. So far it causes no
                    // problems but nevertheless it should be kept in mind.
                    //Timer.getTimer().notifyTick();
                };

        private static void start(){
            Toolkit.getToolkit().addSceneTkPulseListener(listener);
            animation.start();
        }

        private static void stop() {
            Toolkit.getToolkit().removeSceneTkPulseListener(listener);
            animation.stop();
        }
    }

    static void checkThread() {
        Toolkit.getToolkit().checkFxUserThread();
    }

    private final class LoadWorker implements Worker<Void> {

        private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<State>(this, "state", State.READY);
        @Override public final State getState() { checkThread(); return state.get(); }
        @Override public final ReadOnlyObjectProperty<State> stateProperty() { checkThread(); return state.getReadOnlyProperty(); }
        private void updateState(State value) {
            checkThread();
            this.state.set(value);
            running.set(value == State.SCHEDULED || value == State.RUNNING);
        }

        /**
         * @InheritDoc
         */
        private final ReadOnlyObjectWrapper<Void> value = new ReadOnlyObjectWrapper<Void>(this, "value", null);
        @Override public final Void getValue() { checkThread(); return value.get(); }
        @Override public final ReadOnlyObjectProperty<Void> valueProperty() { checkThread(); return value.getReadOnlyProperty(); }

        /**
         * @InheritDoc
         */
        private final ReadOnlyObjectWrapper<Throwable> exception = new ReadOnlyObjectWrapper<Throwable>(this, "exception");
        @Override public final Throwable getException() { checkThread(); return exception.get(); }
        @Override public final ReadOnlyObjectProperty<Throwable> exceptionProperty() { checkThread(); return exception.getReadOnlyProperty(); }

        /**
         * @InheritDoc
         */
        private final ReadOnlyDoubleWrapper workDone = new ReadOnlyDoubleWrapper(this, "workDone", -1);
        @Override public final double getWorkDone() { checkThread(); return workDone.get(); }
        @Override public final ReadOnlyDoubleProperty workDoneProperty() { checkThread(); return workDone.getReadOnlyProperty(); }

        /**
         * @InheritDoc
         */
        private final ReadOnlyDoubleWrapper totalWorkToBeDone = new ReadOnlyDoubleWrapper(this, "totalWork", -1);
        @Override public final double getTotalWork() { checkThread(); return totalWorkToBeDone.get(); }
        @Override public final ReadOnlyDoubleProperty totalWorkProperty() { checkThread(); return totalWorkToBeDone.getReadOnlyProperty(); }

        /**
         * @InheritDoc
         */
        private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(this, "progress", -1);
        @Override public final double getProgress() { checkThread(); return progress.get(); }
        @Override public final ReadOnlyDoubleProperty progressProperty() { checkThread(); return progress.getReadOnlyProperty(); }
        private void updateProgress(double p) {
            totalWorkToBeDone.set(100.0);
            workDone.set(p * 100.0);
            progress.set(p);
        }

        /**
         * @InheritDoc
         */
        private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(this, "running", false);
        @Override public final boolean isRunning() { checkThread(); return running.get(); }
        @Override public final ReadOnlyBooleanProperty runningProperty() { checkThread(); return running.getReadOnlyProperty(); }

        /**
         * @InheritDoc
         */
        private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper(this, "message", "");
        @Override public final String getMessage() { return message.get(); }
        @Override public final ReadOnlyStringProperty messageProperty() { return message.getReadOnlyProperty(); }

        /**
         * @InheritDoc
         */
        private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper(this, "title", "WebEngine Loader");
        @Override public final String getTitle() { return title.get(); }
        @Override public final ReadOnlyStringProperty titleProperty() { return title.getReadOnlyProperty(); }

        /**
         * Cancels the loading of the page. If called after the page has already
         * been loaded, then this call takes no effect.
         */
        @Override public boolean cancel() {
            if (isRunning()) {
                stop(); // this call indirectly sets state
                return true;
            } else {
                return false;
            }
        }

        private void cancelAndReset() {
            cancel();
            exception.set(null);
            message.set("");
            totalWorkToBeDone.set(-1);
            workDone.set(-1);
            progress.set(-1);
            updateState(State.READY);
            running.set(false);
        }

        private void dispatchLoadEvent(long frame, int state,
                String url, String contentType, double workDone, int errorCode)
        {
        }

        Throwable describeError(int errorCode) {
            String reason = "Unknown error";

            return new Throwable(reason);
        }
    }


    private final class DocumentProperty
            extends ReadOnlyObjectPropertyBase<Document> {

        private boolean available;
        private Document document;

        private void invalidate(boolean available) {
            if (this.available || available) {
                this.available = available;
                this.document = null;
                fireValueChangedEvent();
            }
        }

        public Document get() {
            if (!this.available) {
                return null;
            }
            this.document = getCurrentDocument();
            // if (this.document == null) {
                // if (this.document == null) {
                    // this.available = false;
                // }
            // }
            return this.document;
        }

        public Object getBean() {
            return WebEngine.this;
        }

        public String getName() {
            return "document";
        }
    }

    ///////////////////////////////////////////////
    // JavaScript to Java bridge
    ///////////////////////////////////////////////

    private JS2JavaBridge js2javaBridge = null;

    public void exportObject(String jsName, Object object) {
        synchronized (loadedLock) {
            if (js2javaBridge == null) {
                js2javaBridge = new JS2JavaBridge(this);
            }
            js2javaBridge.exportObject(jsName, object);
        }
    }


    interface PageListener {
        void onLoadStarted();
        void onLoadFinished();
        void onLoadFailed();
        void onJavaCall(String args);
    }

    private PageListener pageListener = null;
    private boolean loaded = false;
    private final Object loadedLock = new Object();

    void setPageListener(PageListener listener) {
        synchronized (loadedLock) {
            pageListener = listener;
            if (loaded) {
                updateProgress(0.0);
                updateState(Worker.State.SCHEDULED);
                updateState(Worker.State.RUNNING);
                pageListener.onLoadStarted();
                pageListener.onLoadFinished();
                updateProgress(1.0);
                updateState(Worker.State.SUCCEEDED);
            }
        }
    }

    boolean isLoaded() {
        return loaded;
    }

    // notifications are called from WebView
    void notifyLoadStarted() {
        synchronized (loadedLock) {
            loaded = false;
            updateProgress(0.0);
            updateState(Worker.State.SCHEDULED);
            updateState(Worker.State.RUNNING);
            if (pageListener != null) {
                pageListener.onLoadStarted();
            }
        }
    }

    private String pageContent;

    Document getCurrentDocument () {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(pageContent)));
        }
        catch (Exception e) {
            System.err.println("Error parsing html: " + e.getLocalizedMessage());
        }
        return document;
    }

    void notifyLoadFinished(String loc, String content) {
        synchronized (loadedLock) {
            this.pageContent = "<html>"+content+"</html>";
            loaded = true;
            if (pageListener != null) {
                pageListener.onLoadFinished();
            }
            updateProgress(1.0);
            location.set(loc);
            document.invalidate(true);
            updateState(Worker.State.SUCCEEDED);
        }
    }

    void notifyLoadFailed() {
        synchronized (loadedLock) {
            loaded = false;
            updateProgress(0.0);
            updateState(Worker.State.FAILED);
            if (pageListener != null) {
                pageListener.onLoadFailed();
            }
        }
    }

    void notifyJavaCall(String arg) {
        if (pageListener != null) {
            pageListener.onJavaCall(arg);
        }
    }

    void onAlertNotify(String text) {
        if (getOnAlert() != null) {
            dispatchWebEvent(
                    getOnAlert(),
                    new WebEvent<String>(this, WebEvent.ALERT, text));
        }
    }

    final private AccessControlContext accessControlContext;

    AccessControlContext getAccessControlContext() {
        return accessControlContext;
    }

    private void dispatchWebEvent(final EventHandler handler, final WebEvent ev) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                handler.handle(ev);
                return null;
            }
        }, getAccessControlContext());
    }

    private class DebuggerImpl implements Debugger {
        private Callback<String, Void> callback;

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void setEnabled(boolean enabled) {

        }

        @Override
        public void sendMessage(String message) {
        }

        @Override
        public Callback<String, Void> getMessageCallback() {
            return callback;
        }

        @Override
        public void setMessageCallback(Callback<String, Void> callback) {
            this.callback = callback;
        }
    };
}
