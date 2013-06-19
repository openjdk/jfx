/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.web.Debugger;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.webkit.Accessor;
import com.sun.javafx.webkit.CursorManagerImpl;
import com.sun.javafx.webkit.EventLoopImpl;
import com.sun.javafx.webkit.PolicyClientImpl;
import com.sun.javafx.webkit.ThemeClientImpl;
import com.sun.javafx.webkit.UIClientImpl;
import com.sun.javafx.webkit.UtilitiesImpl;
import com.sun.javafx.webkit.WebPageClientImpl;
import com.sun.javafx.webkit.prism.PrismGraphicsManager;
import com.sun.javafx.webkit.prism.PrismInvoker;
import com.sun.javafx.webkit.prism.theme.PrismRenderer;
import com.sun.javafx.webkit.theme.RenderThemeImpl;
import com.sun.javafx.webkit.theme.Renderer;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;
import com.sun.webkit.CursorManager;
import com.sun.webkit.Disposer;
import com.sun.webkit.DisposerRecord;
import com.sun.webkit.InspectorClient;
import com.sun.webkit.Invoker;
import com.sun.webkit.LoadListenerClient;
import static com.sun.webkit.LoadListenerClient.*;
import com.sun.webkit.ThemeClient;
import com.sun.webkit.Timer;
import com.sun.webkit.Utilities;
import com.sun.webkit.WebPage;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCGraphicsManager;
import com.sun.webkit.network.URLs;
import com.sun.webkit.network.Util;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.util.Callback;
import org.w3c.dom.Document;

/**
 * {@code WebEngine} is a non-visual object capable of managing one Web page
 * at a time. It loads Web pages, creates their document models, applies
 * styles as necessary, and runs JavaScript on pages. It provides access
 * to the document model of the current page, and enables two-way
 * communication between a Java application and JavaScript code of the page.
 * 
 * <h4>Loading Web Pages</h4>
 * <p>The {@code WebEngine} class provides two ways to load content into a
 * {@code WebEngine} object:
 * <ul>
 * <li>From an arbitrary URL using the {@link #load} method. This method uses
 *     the {@code java.net} package for network access and protocol handling.
 * <li>From an in-memory String using the
 *     {@link #loadContent(java.lang.String, java.lang.String)} and
 *     {@link #loadContent(java.lang.String)} methods.
 * </ul>
 * <p>Loading always happens on a background thread. Methods that initiate
 * loading return immediately after scheduling a background job. To track
 * progress and/or cancel a job, use the {@link javafx.concurrent.Worker}
 * instance available from the {@link #getLoadWorker} method.
 * 
 * <p>The following example changes the stage title when loading completes
 * successfully:
 * <pre><code>
import javafx.concurrent.Worker.State;
final Stage stage;
webEngine.getLoadWorker().stateProperty().addListener(
        new ChangeListener&lt;State&gt;() {
            public void changed(ObservableValue ov, State oldState, State newState) {
                if (newState == State.SUCCEEDED) {
                    stage.setTitle(webEngine.getLocation());
                }
            }
        });
webEngine.load("http://javafx.com");
 * </code></pre>
 * 
 * <h4>User Interface Callbacks</h4>
 * <p>A number of user interface callbacks may be registered with a
 * {@code WebEngine} object. These callbacks are invoked when a script running
 * on the page requests a user interface operation to be performed, for
 * example, opens a popup window or changes status text. A {@code WebEngine}
 * object cannot handle such requests internally, so it passes the request to
 * the corresponding callbacks. If no callback is defined for a specific
 * operation, the request is silently ignored.
 * 
 * <p>The table below shows JavaScript user interface methods and properties
 * with their corresponding {@code WebEngine} callbacks:
 * <table border="1" width="400">
 *     <th>JavaScript method/property
 *     <th>WebEngine callback
 * <tr><td>{@code window.alert()}<td>{@code onAlert}
 * <tr><td>{@code window.confirm()}<td>{@code confirmHandler}
 * <tr><td>{@code window.open()}<td>{@code createPopupHandler}
 * <tr><td>{@code window.open()} and<br>
 *         {@code window.close()}<td>{@code onVisibilityChanged}
 * <tr><td>{@code window.prompt()}<td>{@code promptHandler}
 * <tr><td>Setting {@code window.status}<td>{@code onStatusChanged}
 * <tr><td>Setting any of the following:<br>
 *         {@code window.innerWidth}, {@code window.innerHeight},<br>
 *         {@code window.outerWidth}, {@code window.outerHeight},<br>
 *         {@code window.screenX}, {@code window.screenY},<br>
 *         {@code window.screenLeft}, {@code window.screenTop}
 *         <td>{@code onResized}
 * </table>
 * 
 * <p>The following example shows a callback that resizes a browser window:
 * <pre><code>
Stage stage;
webEngine.setOnResized(
        new EventHandler&lt;WebEvent&lt;Rectangle2D&gt;&gt;() {
            public void handle(WebEvent&lt;Rectangle2D&gt; ev) {
                Rectangle2D r = ev.getData();
                stage.setWidth(r.getWidth());
                stage.setHeight(r.getHeight());
            }
        });
 * </code></pre>
 *
 * <h4>Access to Document Model</h4>
 * <p>The {@code WebEngine} objects create and manage a Document Object Model
 * (DOM) for their Web pages. The model can be accessed and modified using
 * Java DOM Core classes. The {@link #getDocument()} method provides access
 * to the root of the model. Additionally DOM Event specification is supported
 * to define event handlers in Java code.
 * 
 * <p>The following example attaches a Java event listener to an element of
 * a Web page. Clicking on the element causes the application to exit:
 * <pre><code>
EventListener listener = new EventListener() {
    public void handleEvent(Event ev) {
        Platform.exit();
    }
};

Document doc = webEngine.getDocument();
Element el = doc.getElementById("exit-app");
((EventTarget) el).addEventListener("click", listener, false);
 * </code></pre>
 * 
 * <h4>Evaluating JavaScript expressions</h4>
 * <p>It is possible to execute arbitrary JavaScript code in the context of
 * the current page using the {@link #executeScript} method. For example:
 * <pre><code>
webEngine.executeScript("history.back()");
 * </code></pre>
 * 
 * <p>The execution result is returned to the caller,
 * as described in the next section.
 *
 * <h4>Mapping JavaScript values to Java objects</h4>
 *
 * JavaScript values are represented using the obvious Java classes:
 * null becomes Java null; a boolean becomes a {@code java.lang.Boolean};
 * and a string becomes a {@code java.lang.String}.
 * A number can be {@code java.lang.Double} or a {@code java.lang.Integer},
 * depending.
 * The undefined value maps to a specific unique String
 * object whose value is {@code "undefined"}.
 * <p>
 * If the result is a
 * JavaScript object, it is wrapped as an instance of the
 * {@link netscape.javascript.JSObject} class.
 * (As a special case, if the JavaScript object is
 * a {@code JavaRuntimeObject} as discussed in the next section,
 * then the original Java object is extracted instead.)
 * The {@code JSObject} class is a proxy that provides access to
 * methods and properties of its underlying JavaScript object.
 * The most commonly used {@code JSObject} methods are
 * {@link netscape.javascript.JSObject#getMember getMember}
 * (to read a named property),
 * {@link netscape.javascript.JSObject#setMember setMember}
 * (to set or define a property),
 * and {@link netscape.javascript.JSObject#call call}
 * (to call a function-valued property).
 * <p>
 * A DOM {@code Node} is mapped to an object that both extends
 * {@code JSObject} and implements the appropriate DOM interfaces.
 * To get a {@code JSObject} object for a {@code Node} just do a cast:
 * <pre>
 * JSObject jdoc = (JSObject) webEngine.getDocument();
 * </pre>
 * <p>
 * In some cases the context provides a specific Java type that guides
 * the conversion.
 * For example if setting a Java {@code String} field from a JavaScript 
 * expression, then the JavaScript value is converted to a string.
 * 
 * <h4>Mapping Java objects to JavaScript values</h4>
 *
 * The arguments of the {@code JSObject} methods {@code setMember} and
 * {@code call} pass Java objects to the JavaScript environment.
 * This is roughly the inverse of the JavaScript-to-Java mapping
 * described above:
 * Java {@code String},  {@code Number}, or {@code Boolean} objects
 * are converted to the obvious JavaScript values. A  {@code JSObject}
 * object is converted to the original wrapped JavaScript object.
 * Otherwise a {@code JavaRuntimeObject} is created.  This is
 * a JavaScript object that acts as a proxy for the Java object,
 * in that accessing properties of the {@code JavaRuntimeObject}
 * causes the Java field or method with the same name to be accessed.
 *
 * <h4>Calling back to Java from JavaScript</h4>
 *
 * <p>The {@link netscape.javascript.JSObject#setMember JSObject.setMember}
 * method is useful to enable upcalls from JavaScript
 * into Java code, as illustrated by the following example. The Java code
 * establishes a new JavaScript object named {@code app}. This object has one
 * public member, the method {@code exit}.
 * <pre><code>
public class JavaApplication {
    public void exit() {
        Platform.exit();
    }
}
...
JSObject window = (JSObject) webEngine.executeScript("window");
window.setMember("app", new JavaApplication());
 * </code></pre>
 * You can then refer to the object and the method from your HTML page:
 * <pre><code>
&lt;a href="" onclick="app.exit()"&gt;Click here to exit application&lt;/a&gt;
 * </code></pre>
 * <p>When a user clicks the link the application is closed.
 * <p>
 * If there are multiple Java methods with the given name,
 * then the engine selects one matching the number of parameters
 * in the call.  (Varargs are not handled.) An unspecified one is
 * chosen if there are multiple ones with the correct number of parameters.
 * <p>
 * You can pick a specific overloaded method by listing the
 * parameter types in an <q>extended method name</q>, which has the
 * form <code>"<var>method_name</var>(<var>param_type1</var>,...,<var>param_typen</var>)"</code>.  Typically you'd write the JavaScript expression:
 * <pre>
 * <var>receiver</var>["<var>method_name</var>(<var>param_type1</var>,...,<var>param_typeN</var>)"](<var>arg1</var>,...,<var>argN</var>)</code>
 * </pre>
 * 
 * <h4>Threading</h4>
 * <p>{@code WebEngine} objects must be created and accessed solely from the
 * JavaFX Application thread. This rule also applies to any DOM and JavaScript
 * objects obtained from the {@code WebEngine} object.
 * @since JavaFX 2.0
 */
final public class WebEngine {
    static {
        Accessor.setPageAccessor(new Accessor.PageAccessor() {
            @Override public WebPage getPage(WebEngine w) {
                return w == null ? null : w.getPage();
            }
        });

        Invoker.setInvoker(new PrismInvoker());
        Renderer.setRenderer(new PrismRenderer());
        WCGraphicsManager.setGraphicsManager(new PrismGraphicsManager());
        CursorManager.setCursorManager(new CursorManagerImpl());
        com.sun.webkit.EventLoop.setEventLoop(new EventLoopImpl());
        ThemeClient.setDefaultRenderTheme(new RenderThemeImpl());
        Utilities.setUtilities(new UtilitiesImpl());
    }

    /**
     * The number of instances of this class.
     * Used to start and stop the pulse timer.
     */
    private static int instanceCount = 0;

    /**
     * The node associated with this engine. There is a one-to-one correspondence
     * between the WebView and its WebEngine (although not all WebEngines have
     * a WebView, every WebView has one and only one WebEngine).
     */
    private final ObjectProperty<WebView> view = new SimpleObjectProperty<WebView>(this, "view");

    /**
     * The Worker which shows progress of the web engine as it loads pages.
     */
    private final LoadWorker loadWorker = new LoadWorker();

    /**
     * The object that provides interaction with the native webkit core.
     */
    private final WebPage page;

    /**
     * The debugger associated with this web engine.
     */
    private final DebuggerImpl debugger = new DebuggerImpl();

    /**
     * Returns a {@link javafx.concurrent.Worker} object that can be used to
     * track loading progress.
     */
    public final Worker<Void> getLoadWorker() {
        return loadWorker;
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
    
    private void updateLocation(String value) {
        this.location.set(value);
        this.document.invalidate(false);
        this.title.set(null);
    }

    
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
    
    private void updateTitle() {
        title.set(page.getTitle(page.getMainFrame()));
    }
    
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

    public final BooleanProperty javaScriptEnabledProperty() {
        if (javaScriptEnabled == null) {
            javaScriptEnabled = new BooleanPropertyBase(true) {
                @Override public void invalidated() {
                    checkThread();
                    page.setJavaScriptEnabled(get());
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
                               url.startsWith("data:"))
                    {
                        try {
                            URLConnection conn = URLs.newURL(url).openConnection();
                            conn.connect();

                            BufferedInputStream in =
                                    new BufferedInputStream(conn.getInputStream());
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            new sun.misc.BASE64Encoder().encodeBuffer(in, out);
                            dataUrl = DATA_PREFIX + out.toString();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid stylesheet URL");
                    }
                    page.setUserStyleSheetLocation(dataUrl);
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
        return userAgent == null ? page.getUserAgent() : userAgent.get();
    }

    public final StringProperty userAgentProperty() {
        if (userAgent == null) {
            userAgent = new StringPropertyBase(page.getUserAgent()) {
                @Override public void invalidated() {
                    checkThread();
                    page.setUserAgent(get());
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
                new Callback<PopupFeatures, WebEngine>() {
                    public WebEngine call(PopupFeatures p) {
                        return WebEngine.this;
                    }
                });
    
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
     * Creates a new engine.
     */
    public WebEngine() {
        this(null);
    }

    /**
     * Creates a new engine and loads a Web page into it.
     */
    public WebEngine(String url) {
        if (instanceCount == 0 &&
            Timer.getMode() == Timer.Mode.PLATFORM_TICKS)
        {
            PulseTimer.start();
        }
        Accessor accessor = new AccessorImpl(this);
        page = new WebPage(
            new WebPageClientImpl(accessor),
            new UIClientImpl(accessor),
            new PolicyClientImpl(),
            new InspectorClientImpl(this),
            new ThemeClientImpl(accessor),
            false);
        page.addLoadListenerClient(new PageLoadListener(this));
        
        history = new WebHistory(page);

        Disposer.addRecord(this, new SelfDisposer(page));

        load(url);
        
        instanceCount++;
    }

    /**
     * Loads a Web page into this engine. This method starts asynchronous
     * loading and returns immediately.
     * @param url URL of the web page to load
     */
    public void load(String url) {
        checkThread();
        loadWorker.cancelAndReset();

        if (url == null) {
            url = "";
        } else {
            // verify and, if possible, adjust the url on the Java
            // side, otherwise it may crash native code
            try {
                url = Util.adjustUrlForWebKit(url);
            } catch (MalformedURLException e) {
                loadWorker.dispatchLoadEvent(getMainFrame(),
                        PAGE_STARTED, url, null, 0.0, 0);
                loadWorker.dispatchLoadEvent(getMainFrame(),
                        LOAD_FAILED, url, null, 0.0, MALFORMED_URL);
            }
        }
        page.open(page.getMainFrame(), url);
    }

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
        loadWorker.cancelAndReset();
        page.load(page.getMainFrame(), content, contentType);
    }

    /**
     * Reloads the current page, whether loaded from URL or directly from a String in
     * one of the {@code loadContent} methods.
     */
    public void reload() {
        // TODO what happens if this is called while currently loading a page?
        checkThread();
        page.refresh(page.getMainFrame());
    }
    
    private final WebHistory history;
    
    /**
     * Returns the session history object.
     * 
     * @return history object
     * @since JavaFX 2.2
     */
    public WebHistory getHistory() {
        return history;
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
        return page.executeScript(page.getMainFrame(), script);
    }

    private long getMainFrame() {
        return page.getMainFrame();
    }

    WebPage getPage() {
        return page;
    }

    void setView(WebView view) {
        this.view.setValue(view);
    }
    
    private void stop() {
        checkThread();
        page.stop(page.getMainFrame());
    }

    private static final class SelfDisposer implements DisposerRecord {
        private final WebPage page;

        private SelfDisposer(WebPage page) {
            this.page = page;
        }

        @Override public void dispose() {
            page.dispose();
            instanceCount--;
            if (instanceCount == 0 &&
                Timer.getMode() == Timer.Mode.PLATFORM_TICKS)
            {
                PulseTimer.stop();
            }
        }
    }

    private static final class AccessorImpl extends Accessor {
        private final WeakReference<WebEngine> engine;

        private AccessorImpl(WebEngine w) {
            this.engine = new WeakReference<WebEngine>(w);
        }

        @Override public WebEngine getEngine() {
            return engine.get();
        }

        @Override public WebPage getPage() {
            WebEngine w = getEngine();
            return w == null ? null : w.page;
        }

        @Override public WebView getView() {
            WebEngine w = getEngine();
            return w == null ? null : w.view.get();
        }

        @Override public void addChild(Node child) {
            WebView view = getView();
            if (view != null) {
                view.getChildren().add(child);
            }
        }

        @Override public void removeChild(Node child) {
            WebView view = getView();
            if (view != null) {
                view.getChildren().remove(child);
            }
        }

        @Override public void addViewListener(InvalidationListener l) {
            WebEngine w = getEngine();
            if (w != null) {
                w.view.addListener(l);
            }
        }
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
            new TKPulseListener() {
                public void pulse() {
                    // Note, the timer event is executed right in the notifyTick(),
                    // that is during the pulse event. This makes the timer more
                    // repsonsive, though prolongs the pulse. So far it causes no
                    // problems but nevertheless it should be kept in mind.
                    Timer.getTimer().notifyTick();
                }
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


    /**
     * The page load event listener. This object references the owner
     * WebEngine weakly so as to avoid referencing WebEngine from WebPage
     * strongly.
     */
    private static final class PageLoadListener implements LoadListenerClient {
        
        private final WeakReference<WebEngine> engine;
    

        private PageLoadListener(WebEngine engine) {
            this.engine = new WeakReference<WebEngine>(engine);
        }


        @Override public void dispatchLoadEvent(long frame, int state,
                String url, String contentType, double progress, int errorCode)
        {
            WebEngine w = engine.get();
            if (w != null) {
                w.loadWorker.dispatchLoadEvent(frame, state, url,
                        contentType, progress, errorCode);
            }
        }

        @Override public void dispatchResourceLoadEvent(long frame,
                int state, String url, String contentType, double progress,
                int errorCode)
        {
        }
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
            if (frame != getMainFrame()) {
                return;
            }

            switch (state) {
                case PAGE_STARTED:
                    message.set("Loading " + url);
                    updateLocation(url);
                    updateProgress(0.0);
                    updateState(State.SCHEDULED);
                    updateState(State.RUNNING);
                    break;
                case PAGE_REDIRECTED:
                    message.set("Loading " + url);
                    updateLocation(url);
                    break;
                case PAGE_FINISHED:
                    message.set("Loading complete");
                    updateProgress(1.0);
                    updateState(State.SUCCEEDED);
                    break;
                case LOAD_FAILED:
                    message.set("Loading failed");
                    exception.set(describeError(errorCode));
                    updateState(State.FAILED);
                    break;
                case LOAD_STOPPED:
                    message.set("Loading stopped");
                    updateState(State.CANCELLED);
                    break;
                case PROGRESS_CHANGED:
                    updateProgress(workDone);
                    break;
                case TITLE_RECEIVED:
                    updateTitle();
                    break;
                case DOCUMENT_AVAILABLE:
                    document.invalidate(true);
                    break;
            }
        }

        private Throwable describeError(int errorCode) {
            String reason = "Unknown error";

            switch (errorCode) {
                case UNKNOWN_HOST:
                    reason = "Unknown host";
                    break;
                case MALFORMED_URL:
                    reason = "Malformed URL";
                    break;
                case SSL_HANDSHAKE:
                    reason = "SSL handshake failed";
                    break;
                case CONNECTION_REFUSED:
                    reason = "Connection refused by server";
                    break;
                case CONNECTION_RESET:
                    reason = "Connection reset by server";
                    break;
                case NO_ROUTE_TO_HOST:
                    reason = "No route to host";
                    break;
                case CONNECTION_TIMED_OUT:
                    reason = "Connection timed out";
                    break;
                case PERMISSION_DENIED:
                    reason = "Permission denied";
                    break;
                case INVALID_RESPONSE:
                    reason = "Invalid response from server";
                    break;
                case TOO_MANY_REDIRECTS:
                    reason = "Too many redirects";
                    break;
                case FILE_NOT_FOUND:
                    reason = "File not found";
                    break;
            }
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
            if (this.document == null) {
                this.document = page.getDocument(page.getMainFrame());
                if (this.document == null) {
                    this.available = false;
                }
            }
            return this.document;
        }

        public Object getBean() {
            return WebEngine.this;
        }

        public String getName() {
            return "document";
        }
    }


    /**
     * Returns the debugger associated with this web engine.
     * The debugger is an object that can be used to debug
     * the web page currently loaded into the web engine.
     * <p>
     * All methods of the debugger must be called on
     * the JavaFX Application Thread.
     * The message callback object registered with the debugger
     * is always called on the JavaFX Application Thread.
     * @return the debugger associated with this web engine.
     *         The return value cannot be {@code null}.
     * @treatAsPrivate This is an internal API that can be changed or
     *                 removed in the future.
     * @deprecated This is an internal API that can be changed or
     *             removed in the future.
     */
    @Deprecated
    public Debugger impl_getDebugger() {
        return debugger;
    }

    /**
     * The debugger implementation.
     */
    private final class DebuggerImpl implements Debugger {

        private boolean enabled;
        private Callback<String,Void> messageCallback;


        @Override
        public boolean isEnabled() {
            checkThread();
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            checkThread();
            if (enabled != this.enabled) {
                if (enabled) {
                    page.setDeveloperExtrasEnabled(true);
                    page.connectInspectorFrontend();
                } else {
                    page.disconnectInspectorFrontend();
                    page.setDeveloperExtrasEnabled(false);
                }
                this.enabled = enabled;
            }
        }

        @Override
        public void sendMessage(String message) {
            checkThread();
            if (!enabled) {
                throw new IllegalStateException("Debugger is not enabled");
            }
            if (message == null) {
                throw new NullPointerException("message is null");
            }
            page.dispatchInspectorMessageFromFrontend(message);
        }

        @Override
        public Callback<String,Void> getMessageCallback() {
            checkThread();
            return messageCallback;
        }

        @Override
        public void setMessageCallback(Callback<String,Void> callback) {
            checkThread();
            messageCallback = callback;
        }
    }

    /**
     * The inspector client implementation. This object references the owner
     * WebEngine weakly so as to avoid referencing WebEngine from WebPage
     * strongly.
     */
    private static final class InspectorClientImpl implements InspectorClient {

        private final WeakReference<WebEngine> engine;


        private InspectorClientImpl(WebEngine engine) {
            this.engine = new WeakReference<WebEngine>(engine);
        }


        @Override
        public boolean sendMessageToFrontend(final String message) {
            boolean result = false;
            WebEngine webEngine = engine.get();
            if (webEngine != null) {
                final Callback<String,Void> messageCallback =
                        webEngine.debugger.messageCallback;
                if (messageCallback != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override public Void run() {
                            messageCallback.call(message);
                            return null;
                        }
                    }, webEngine.page.getAccessControlContext());
                    result = true;
                }
            }
            return result;
        }
    }

    /**
     * Prints the current Web page using the given printer job.
     * <p>This method does not modify the state of the job, nor does it call
     * {@link PrinterJob#endJob}, so the job may be safely reused afterwards.
     * 
     * @param job printer job used for printing
     * @since JavaFX 8.0
     */
    public void print(PrinterJob job) {
        PageLayout pl = job.getJobSettings().getPageLayout();
        int pageCount = page.beginPrinting(
                (float) pl.getPrintableWidth(), (float) pl.getPrintableHeight());
        
        for (int i = 0; i < pageCount; i++) {
            Node printable = new Printable(i);
            job.printPage(printable);
        }
        page.endPrinting();
    }
    
    final class Printable extends Node {
        private final PGNode peer;

        Printable(int pageIndex) {
            peer = new Peer(pageIndex);
        }
        
        @Override protected PGNode impl_createPGNode() {
            return peer;
        }

        @Override public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
            return null;
        }

        @Override public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
            return bounds;
        }

        @Override protected boolean impl_computeContains(double d, double d1) {
            return false;
        }
        
        private final class Peer extends NGNode {
            private final int pageIndex;
            
            Peer(int pageIndex) {
                this.pageIndex = pageIndex;
            }
            
            @Override protected void renderContent(Graphics g) {
                WCGraphicsContext gc = WCGraphicsManager.getGraphicsManager().
                        createGraphicsContext(g);
                page.print(gc, pageIndex);
            }

            @Override protected boolean hasOverlappingContents() {
                return false;
            }
        }
    }
}
