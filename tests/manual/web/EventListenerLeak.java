/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

public class EventListenerLeak extends Application {

    /**
     * List of WeakReferences to EventListener objects so we can count which
     * ones are active.
     */
    static List<WeakReference<EventListener>> weakRefs = new ArrayList<>();

    /**
     * Listener shared by all WebView instances.
     */
    static WeakReference<EventListener> sharedListener = null;

    /**
     * Count of number of active listeners.
     * Must be updated in the FX app thread
     */
    static IntegerProperty activeListenerCount = new SimpleIntegerProperty(0);

    /**
     * EventListener class use to test for leaks and correct
     * delivery of events.
     */
    static class MyEventListener implements EventListener {
        private final String id;

        MyEventListener(String id) {
            this.id = id;
        }

        @Override
        public void handleEvent(Event event) {
            System.out.println("[" + id + "] click");
        }
    }

    /**
     * Encapsulates a WebView instance and controls
     * to remove a listner or the entire webview
     */
    static class LeakTestPanel extends VBox {
        private static int count = 0;

        private String name;
        private WebView webView = null;
        private List<WeakReference<EventListener>> myListeners = new ArrayList<>();
        private List<EventTarget> domNodes = new ArrayList<>();

        LeakTestPanel() {
            ++count;
            name = "WebView #" + count;

            createContent();
            setupListeners();
        }

        private static final String HTML =
                "<body><html>" +
                        "Link: <a href=click>click me A</a><br>" +
                        "Link: <a href=click>click me B</a><br>" +
                        "Link: <a href=click>click me C</a><br>" +
                        "Link: <a href=click>click me SHARED</a><br>" +
                        "</html></body>";
        private static final int NUM_DOM_NODES = 4;

        private void createContent() {
            this.setSpacing(5);
            this.setPadding(new Insets(5));
            this.setPrefSize(350, 300);

            webView = new WebView();

            VBox controlBox = new VBox();
            controlBox.setSpacing(5);
            controlBox.setPadding(new Insets(5));

            Button removeWebViewButton = new Button("Remove " + name);
            removeWebViewButton.setOnAction(e -> {
                if (webView != null) {
                    System.out.println("Removing " + name);
                    this.getChildren().remove(1);
                    domNodes.clear();
                    webView = null;
                }
                updateActiveListenerCount();
            });

            Label listenerStatusLabel = new Label("DOM Event Listeners: " + NUM_DOM_NODES + " active");

            Button removeListenerButton = new Button("Remove listener");
            removeListenerButton.setOnAction(e -> {
                if (domNodes.isEmpty()) {
                    System.out.println("No more listeners to remove");
                } else {
                    EventTarget node = domNodes.remove(0);
                    EventListener listener = myListeners.remove(0).get();
                    if (node != null && listener != null) {
                        System.out.println("Removing listener");
                        node.removeEventListener("click", listener, false);
                    } else {
                        System.err.println("*** Unable to remove listener");
                    }
                    listenerStatusLabel.setText("DOM Event Listeners: " + domNodes.size() + " active, " + (NUM_DOM_NODES - domNodes.size()) + " inactive");
                }
                updateActiveListenerCount();
            });
            controlBox.getChildren().addAll(removeWebViewButton, removeListenerButton, listenerStatusLabel);

            this.getChildren().addAll(controlBox, webView);
        }

        void setupListeners() {
            final List<ChangeListener<Worker.State>> stateListeners =
                    new ArrayList<>();
            stateListeners.add((obs, oldState, newState) -> {
                WebEngine engine = webView.getEngine();
                if (newState == Worker.State.SUCCEEDED) {
                    Document doc = engine.getDocument();
                    if (doc != null) {
                        NodeList nodeList = doc.getElementsByTagName("a");
                        if (nodeList != null) {
                            // Adding an EventListener creates a JNI
                            // global reference, which needs to be released
                            // either when the listener is removed or when
                            // the WebView does out of scope
                            for (int i = 0; i < nodeList.getLength(); i++) {
                                EventListener listener;
                                if (i < 2) {
                                    // Create a new listener
                                    listener = new MyEventListener("" + name + " listener " + i);
                                    weakRefs.add(new WeakReference<>(listener));
                                } else if (i == 2) {
                                    // Reuse exising listener
                                    listener = myListeners.get(0).get();
                                } else {
                                    // Create or use a listener shared by
                                    // all WebView instances
                                    if (sharedListener == null) {
                                        // Create a new listener
                                        listener = new MyEventListener("Shared Listener");
                                        weakRefs.add(new WeakReference<>(listener));
                                        sharedListener = (new WeakReference<>(listener));
                                    } else {
                                        listener = sharedListener.get();
                                    }
                                }
                                myListeners.add(new WeakReference<>(listener));

                                EventTarget node = (EventTarget) nodeList.item(i);
                                domNodes.add(node);
                                System.err.println("" + node.getClass() + "::addEventListener");
                                node.addEventListener("click", listener, false);
                            }
                        }
                    }

                    // Remove ChangeListener so we don't hold reference to the EventListener
                    engine.getLoadWorker().stateProperty()
                            .removeListener(stateListeners.get(0));
                    stateListeners.clear();
                    updateActiveListenerCount();
                }
            });
            webView.getEngine().getLoadWorker().stateProperty().addListener(stateListeners.get(0));
            webView.getEngine().loadContent(HTML);
            webView.setPrefSize(300, 200);
        }

    }

    static void updateActiveListenerCount() {
        System.gc();
        System.gc();

        int count = 0;
        for (WeakReference<EventListener> ref : weakRefs) {
            if (ref.get() != null) {
                count++;
            }
        }

        final int newCount = count;
        Platform.runLater(() -> {
            if (newCount != activeListenerCount.get()) {
                activeListenerCount.set(newCount);
                System.err.println("Active MyEventListeners: " + newCount);
            }
        });
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("JavaFXEventListenerLeak");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));
        Scene scene = new Scene(root);

        VBox instructions = new VBox(
                new Label(" This test is for EventListener memory leak manual testing "),
                new Label("Issue: calling eventtarget.removeEventListener doesn't remove the Eventlistener"),
                new Label(" "),
                new Label(" STEPS:"),
                new Label("  1. In one panel, remove the WebView."),
                new Label("  2.  In the other panel, remove the listeners one at a time, making \n" +
                        "\t sure that the removed link is not active, and the other links are. \n" +
                        "\t The count should not change when the first of the three listeners \n" +
                        "\tis removed (because that listener is still in use),\n" +
                        "\t but then should decrease when the second and third are removed.\n"),
                new Label("  3. The count of number of listeners should go to 0 after doing both of the above."));


        root.setTop(instructions);

        // Create content panel with 2 WebView leak test panels
        HBox contentPanel = new HBox();
        contentPanel.setSpacing(5);
        contentPanel.setPadding(new Insets(5));

        LeakTestPanel leakTest1 = new LeakTestPanel();
        LeakTestPanel leakTest2 = new LeakTestPanel();
        contentPanel.getChildren().addAll(leakTest1, leakTest2);

        root.setCenter(contentPanel);

        // Add status line
        Label activeListenerLabel = new Label();
        activeListenerLabel.textProperty().bind(activeListenerCount.asString("Active Listener Count: %d"));
        root.setBottom(activeListenerLabel);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        Thread thr = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }

                updateActiveListenerCount();
            }
        });
        thr.setDaemon(true);
        thr.start();

        Application.launch(args);
    }
}
