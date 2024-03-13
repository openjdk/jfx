package attenuation;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;

/**
 * Utility class for creating screenshots.
 */
final class CaptureUtils {

    enum Format {
        BMP,
        GIF,
        JPG,
        PNG,
        TIF
    }

    private static final Path FOLDER = Path.of("screenshots");

    private static int imageNum = 1;

    static void capture(Image fxImage, Format extension) {
        BufferedImage image = SwingFXUtils.fromFXImage(fxImage, null);
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        rgbImage.getGraphics().drawImage(image, 0, 0, null);

        String formatName = extension.name().toLowerCase();
        var file = FOLDER.resolve(Path.of("screenshot" + imageNum + "." + formatName)).toAbsolutePath().toFile();
        try {
            Files.createDirectories(FOLDER);
            if (!ImageIO.write(rgbImage, formatName, file)) {
                throw new IOException("No writer found for " + formatName);
            }
            Desktop.getDesktop().open(FOLDER.toAbsolutePath().toFile());
        } catch (IOException e) {
            e.printStackTrace();
            var alert = new Alert(AlertType.ERROR);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
        imageNum++;
    }
}
