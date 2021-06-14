package test.javafx.application;

import javafx.application.Application;
import javafx.application.Theme;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Test;
import test.com.sun.javafx.pgstub.StubToolkit;

import java.util.Map;

import static org.junit.Assert.*;

public class ThemeTest {

    public static class PublicConstructor implements Theme {
        @Override public ObservableList<String> getStylesheets() { return FXCollections.observableArrayList(); }
        @Override public void platformThemeChanged(Map<String, String> properties) {}
    }

    /**
     * The properties injected into this class are defined in {@link StubToolkit#getPlatformThemeProperties()}.
     */
    public static class PublicConstructorWithProperties implements Theme {
        public PublicConstructorWithProperties(Map<String, String> properties) {
            assertEquals(2, properties.size());
            assertEquals(properties.get("prop1"), "true");
            assertEquals(properties.get("prop2"), "false");
        }
        @Override public ObservableList<String> getStylesheets() { return FXCollections.observableArrayList(); }
        @Override public void platformThemeChanged(Map<String, String> properties) {}
    }

    public static class PrivateConstructor implements Theme {
        private PrivateConstructor() {}
        @Override public ObservableList<String> getStylesheets() { return null; }
        @Override public void platformThemeChanged(Map<String, String> properties) {}
    }

    public static class UnsuitableConstructor implements Theme {
        public UnsuitableConstructor(int value) {}
        @Override public ObservableList<String> getStylesheets() { return null; }
        @Override public void platformThemeChanged(Map<String, String> properties) {}
    }

    @Test
    public void testThemeWithPublicConstructorIsLoaded() {
        Application.setUserAgentStylesheet("theme:" + PublicConstructor.class.getName());
        assertTrue(Theme.currentTheme() instanceof PublicConstructor);
    }

    @Test
    public void testThemeWithPublicConstructorWithPropertiesIsLoaded() {
        Application.setUserAgentStylesheet("theme:" + PublicConstructorWithProperties.class.getName());
        assertTrue(Theme.currentTheme() instanceof PublicConstructorWithProperties);
    }

    @Test(expected = RuntimeException.class)
    public void testThemeWithPrivateConstructorCannotBeLoaded() {
        Application.setUserAgentStylesheet("theme:" + PrivateConstructor.class.getName());
    }

    @Test(expected = RuntimeException.class)
    public void testThemeWithUnsuitableConstructorCannotBeLoaded() {
        Application.setUserAgentStylesheet("theme:" + UnsuitableConstructor.class.getName());
    }

}
