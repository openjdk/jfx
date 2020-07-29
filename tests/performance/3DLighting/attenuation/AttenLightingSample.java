package attenuation;

import javafx.beans.property.DoubleProperty;
import javafx.scene.PointLight;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;

/**
 * A {@code LightingSample} with additional controls for light attenuation.
 */
public class AttenLightingSample extends LightingSample {

    @Override
    protected VBox addLightControls(PointLight light) {
        var vbox = super.addLightControls(light);
        var range = createSliderControl("range", light.maxRangeProperty(), 0, 100, light.getMaxRange());
        var c = createSliderControl("constant", light.constantAttenuationProperty(), -1, 1, light.getConstantAttenuation());
        var lc = createSliderControl("linear", light.linearAttenuationProperty(), -1, 1, light.getLinearAttenuation());
        var qc = createSliderControl("quadratic", light.quadraticAttenuationProperty(), -1, 1, light.getQuadraticAttenuation());
        vbox.getChildren().addAll(range, c, lc, qc);
        return vbox;
    }

    private HBox createSliderControl(String name, DoubleProperty property, double min, double max, double start) {
        var slider = new Slider(min, max, start);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        property.bindBidirectional(slider.valueProperty());
        var tf = new TextField();
        tf.textProperty().bindBidirectional(slider.valueProperty(), new NumberStringConverter());
        tf.setMaxWidth(50);
        return new HBox(5, new Label(name), slider, tf);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
