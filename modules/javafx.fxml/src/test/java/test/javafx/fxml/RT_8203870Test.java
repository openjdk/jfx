package test.javafx.fxml;

import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class RT_8203870Test {

    @Test
    public void testReadOnlyCollectionsNoProxyBuilder() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_8203870_a.fxml"));
        ClassWithCollections widget = fxmlLoader.load();

        assertEquals(3, widget.getList().size());
        assertEquals(3, widget.getSet().size());
        assertEquals(2, widget.getMap().size());

        assertEquals(3, widget.getObservableList().size());
        assertEquals(3, widget.getObservableSet().size());
        assertEquals(2, widget.getObservableMap().size());

//        assertEquals(3, widget.getNames().length);
//        assertEquals(3, widget.getRatios().length);
    }

    @Test
    public void testReadOnlyCollections() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_8203870_b.fxml"));
        ClassWithCollections2 widget = fxmlLoader.load();

        assertEquals(3, widget.getList().size());
        assertEquals(3, widget.getSet().size());
        assertEquals(2, widget.getMap().size());

        assertEquals(3, widget.getObservableList().size());
        assertEquals(3, widget.getObservableSet().size());
        assertEquals(2, widget.getObservableMap().size());

//        assertEquals(3, widget.getNames().length);
//        assertEquals(3, widget.getRatios().length);
    }


}
