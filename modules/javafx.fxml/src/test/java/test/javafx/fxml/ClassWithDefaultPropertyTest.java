package test.javafx.fxml;

import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassWithDefaultPropertyTest {

    @Test
    public void testDefaultListProperty() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("class_with_default_list_property.fxml"));
        ClassWithDefaultListProperty classWithDefaultProperty = fxmlLoader.load();

        Assertions.assertEquals(1, classWithDefaultProperty.getItems().size());
//        Coma separated values are currently not supported in default list properties
//        Assertions.assertEquals(3, classWithDefaultProperty.getItems().size());
    }

    @Test
    public void testDefaultSetProperty() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("class_with_default_set_property.fxml"));
        ClassWithDefaultSetProperty classWithDefaultProperty = fxmlLoader.load();

        Assertions.assertEquals(1, classWithDefaultProperty.getItems().size());
//        Coma separated values are currently not supported in default list properties
//        Assertions.assertEquals(3, classWithDefaultProperty.getItems().size());
    }

}
