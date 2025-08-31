package test.javafx.stage;

import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MacOSSystemMenuTestBase {

    /**
     * A utility record to represent a menu and its possible children (items)
     */
    protected static record Element(String name, List<Element> items) {

        public Element(String name) {
            this(name, List.of());
        }
    }

    /**
     * Test menu bar contents 0
     */
    protected static final List<Element> TEST_MENUS_0 = List.of(
            new Element("MyFile", List.of(
                    new Element("New", List.of(
                            new Element("Project"),
                            new Element("Module"),
                            new Element("File")
                    )),
                    new Element("Open"),
                    new Element("Save")
            )),
            new Element("MyEdit", List.of(
                    new Element("Undo"),
                    new Element("Redo")
            ))
    );

    /**
     * Test menu bar contents 1
     */
    protected static final List<Element> TEST_MENUS_1 = List.of(
            new Element("MyView", List.of(
                    new Element("Windows", List.of(
                            new Element("Editor"),
                            new Element("Project"),
                            new Element("Debugger")
                    )),
                    new Element("Appearance")
            )),
            new Element("MyNavigate", List.of(
                    new Element("Class..."),
                    new Element("Files..."),
                    new Element("Symbol..."),
                    new Element("Text..."),
                    new Element("File", List.of(
                            new Element("Next Method"),
                            new Element("Next Field")
                    ))
            ))
    );

    /**
     * Test menu bar contents 2
     */
    protected static final List<Element> TEST_MENUS_2 = List.of(
            new Element("MyTasks", List.of(
                    new Element("Create", List.of(
                            new Element("New")
                    )),
                    new Element("Read"),
                    new Element("Update"),
                    new Element("Delete")
            )),
            new Element("MyCalender", List.of(
                    new Element("Dates", List.of(
                            new Element("Year"),
                            new Element("Month"),
                            new Element("Day")
                    )),
                    new Element("Time")
            )),
            new Element("Opt 1"),
            new Element("Opt 2"),
            new Element("Opt 3")
    );

    /**
     * Test menu bar contents 3
     */
    protected static final List<Element> TEST_MENUS_3 = List.of(
            new Element("MyImages", List.of(
                    new Element("Scale", List.of(
                            new Element("Small"),
                            new Element("Medium"),
                            new Element("Large")
                    )),
                    new Element("Color"),
                    new Element("Pixels"),
                    new Element("Shadow"),
                    new Element("Shapes")
            ))
    );

    protected final List<MenuBar> javaFXMenuBars = new ArrayList<>();

    protected final List<Stage> javaFXWindows = new ArrayList<>();

    protected final List<JFrame> swingWindows = new ArrayList<>();

    private CountDownLatch latch = null;

    /////////////////////////////////////////////////////////
    ///
    /// Helpers for creation and focusing of windows
    ///
    /////////////////////////////////////////////////////////
    
    protected void initJavaFX(List<List<Element>> menus) {
        initJavaFX(false, menus);
    }

    protected void initJavaFX(boolean fullscreen, List<List<Element>> menus) {
        assert menus.size() > 0;
        initLock();

        Platform.startup(() -> {
            for (int i = 0; i < menus.size(); i++) {
                initJavaFXWindow(fullscreen, i, menus.get(i));
            }

            releaseLock();
        });

        awaitLock();
    }

    protected void initSwing(List<List<Element>> menus) {
        initSwing(false, menus);
    }

    protected void initSwing(boolean fullscreen, List<List<Element>> menus) {
        assert menus.size() > 0;
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        for (int i = 0; i < menus.size(); i++) {
            initSwingWindow(fullscreen, i, menus.get(i));
        }
    }

    protected void focusJavaFX(int id) {
        initLock();

        Platform.runLater(() -> {
            javaFXWindows.get(id).toFront();
            javaFXWindows.get(id).requestFocus();

            releaseLock();
        });

        awaitLock();
    }

    protected void focusSwing(int id) {
        initLock();

        SwingUtilities.invokeLater(() -> {
            swingWindows.get(id).setAlwaysOnTop(true);
            swingWindows.get(id).toFront();
            swingWindows.get(id).requestFocus();
            swingWindows.get(id).setAlwaysOnTop(false);

            releaseLock();
        });

        awaitLock();
    }

    protected void initJavaFXWindow(boolean fullscreen, int id, List<Element> menus) {
        MenuBar menuBar = new MenuBar();
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root);
        Stage window = new Stage();

        for (Element menu : menus) {
            menuBar.getMenus().add(createJavaFXMenu(menu));
        }

        menuBar.setUseSystemMenuBar(true);
        root.setTop(menuBar);
        window.setScene(scene);
        window.setTitle("JavaFX Window [" + id + "]");
        window.setFullScreen(fullscreen);
        window.show();

        javaFXMenuBars.add(menuBar);
        javaFXWindows.add(window);
    }

    protected Menu createJavaFXMenu(Element element) {
        Menu menu = new Menu(element.name);

        for (Element item : element.items) {
            if (item.items.isEmpty()) {
                menu.getItems().add(new MenuItem(item.name));
            } else {
                menu.getItems().add(createJavaFXMenu(item));
            }
        }

        return menu;
    }

    protected void initSwingWindow(boolean fullscreen, int id, List<Element> menus) {
        JMenuBar menuBar = new JMenuBar();
        JFrame window = new JFrame();

        for (Element menu : menus) {
            menuBar.add(createSwingMenu(menu));
        }

        if (fullscreen) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            window.setUndecorated(true);
            window.setResizable(false);
            gd.setFullScreenWindow(window);
        }

        window.setJMenuBar(menuBar);
        window.setTitle("Swing Window [" + id + "]");
        window.setSize(400, 200);
        window.setVisible(true);

        swingWindows.add(window);
    }

    protected JMenu createSwingMenu(Element element) {
        JMenu menu = new JMenu(element.name);

        for (Element item : element.items) {
            if (item.items.isEmpty()) {
                menu.add(new JMenuItem(item.name));
            } else {
                menu.add(createSwingMenu(item));
            }
        }

        return menu;
    }

    /////////////////////////////////////////////////////////
    ///
    /// Helpers for system menu comparison
    ///
    /////////////////////////////////////////////////////////

    /**
     * Compares the app menus of the provided menu bars. The app menu
     * is the menu after the apple menu.
     */
    protected void compareAppMenus(List<Element> first, List<Element> second) {
        assertFalse(first.isEmpty(), "No app menu present");
        assertFalse(second.isEmpty(), "No app menu present");

        Element firstElement = first.get(0);
        Element secondElement = second.get(0);

        assertEquals(firstElement, secondElement, "App menus are not identical");
    }

    /**
     * Compares two menus where the first one is with an app menu and
     * the last one is without an app menu. This is used for comparing
     * the hardcoded menus inside this file to the actual menus used
     * when launching the application on MacOS.
     */
    protected void compareMenus(List<Element> withAppMenu, List<Element> withoutAppMenu) {
        withAppMenu = new ArrayList<>(withAppMenu);

        assertFalse(withAppMenu.isEmpty(), "No app menu present");
        withAppMenu.remove(0);

        assertTrue(withAppMenu.size() == withoutAppMenu.size(), "Menu size is different: " + withAppMenu.size() + " != " + withoutAppMenu.size());

        for (int i = 0; i < withAppMenu.size(); i++) {
            assertEquals(withAppMenu.get(i), withoutAppMenu.get(i), "Menus are different");
        }
    }

    /**
     * Returns the menu bar and its menu items for the currently
     * active window. The result does not contain the apple menu.
     */
    protected List<Element> getMenusOfFocusedWindow() throws IOException {
        List<Element> result = getMenusOfFocusedWindow(new Stack<>());
        // remove apple menu
        result.remove(0);

        return result;
    }

    private List<Element> getMenusOfFocusedWindow(Stack<Integer> indices) throws IOException {
        Process process = getMenuReaderProcess(indices);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String lines = getMenuReaderProcess(process.getInputStream());
            String error = getMenuReaderProcess(process.getErrorStream());

            assertTrue(error.isEmpty(), error);

            List<Element> result = new ArrayList<>();
            List<String> parts = Arrays.stream(lines.split(", "))
                    .filter(part -> !part.equals("missing value"))
                    .collect(Collectors.toList());

            if (lines.isEmpty()) {
                return result;
            }

            for (int i = 0; i < parts.size(); i++) {
                indices.push(i + 1);
                List<Element> elements = getMenusOfFocusedWindow(indices);
                indices.pop();

                result.add(new Element(parts.get(i), elements));
            }

            return result;
        }
    }

    /**
     * Returns the process used for retreiving the menu items
     * of the currently active window. For this 'osascript' is
     * used as a java process.
     */
    private static Process getMenuReaderProcess(Stack<Integer> indices) throws IOException {
        StringBuilder arg = new StringBuilder(indices.isEmpty()
                ? "menus"
                : "menu items");

        for (int i = indices.size() - 1; i >= 0; i--) {
            if (i == 0) {
                arg.append(" of menu " + indices.get(i) + " ");
            } else {
                arg.append(" of menu of menu item " + indices.get(i) + " ");
            }
        }

        String[] command = { "osascript", "-e", "tell application \"System Events\" to tell (first process whose frontmost is true) to get name of " + arg + " of menu bar 1"};
        return new ProcessBuilder(command).start();
    }

    /**
     * Safely gets the output of a process. For this the input stream
     * of the process is supplied. This input stream can either be the
     * normal input stream or the error input stream.
     */
    private static String getMenuReaderProcess(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            return result.toString();
        }
    }

    /////////////////////////////////////////////////////////
    ///
    /// Helpers for synchronization
    ///
    /////////////////////////////////////////////////////////

    private void initLock() {
        latch = new CountDownLatch(1);
    }

    private void awaitLock() {
        try {
            latch.await();
        } catch (InterruptedException e) {}
    }

    private void releaseLock() {
        latch.countDown();
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {}
    }
}
