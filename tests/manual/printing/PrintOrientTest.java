import java.util.Set;

import javafx.print.JobSettings;
import javafx.print.Printer;
import javafx.print.PrinterAttributes;
import javafx.print.PrinterJob;
import javafx.print.PrintColor;
import javafx.print.PageOrientation;
import javafx.print.PageLayout;
import static javafx.print.PageOrientation.*;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class PrintOrientTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    static int WIDTH=400;
    static int HEIGHT=400;

    public void start(Stage stage) {
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setTitle("Printing Orientation Test");
        Rectangle2D bds = Screen.getPrimary().getVisualBounds();
        stage.setX((bds.getWidth() - WIDTH) / 2);
        stage.setY((bds.getHeight() - HEIGHT) / 2);
        stage.setScene(createScene(stage));
        stage.show();
    }

    static final String instructions =
       "This tests that paper orientation is correct for all supported "+
       "cases of portrait, reverse portrait, landscape, and reverse "+
       "landscape. Since not all printers support the 4 cases, pay attention "+
       "to the text on the printed page. If it says 'unsupported on this "+
       "printer', you can ignore that page. For the other cases, "+
       "the test passes if the rectangle has uniform margins 1\" from "+
       "the edge the paper. Also the 'reverse' orientations should emerge "+
       "from the printer 180 degrees rotated from the non-reversed cases."+
       "Take care to examine this as it came out from the printer.";

    static final String noprinter =
        "There are no printers installed. This test cannot run";

    private Text createInfo(String msg) {
        Text t = new Text(msg);
        t.setWrappingWidth(WIDTH-50);
        t.setLayoutX(20);
        t.setLayoutY(20);
        return t;
    }

    private Scene createScene(final Stage stage) {

        Group g = new Group();
        final Scene scene = new Scene(new Group());
        scene.setFill(Color.WHITE);

        String msg = instructions;
        if (Printer.getDefaultPrinter() == null) {
          msg = noprinter;
        }
        Text info = createInfo(msg);
        ((Group)scene.getRoot()).getChildren().add(info);

        Button print = new Button("Print");
        print.setLayoutX(80);
        print.setLayoutY(200);
        print.setOnAction(e -> {
            createJob(PORTRAIT);
            createJob(REVERSE_PORTRAIT);
            createJob(LANDSCAPE);
            createJob(REVERSE_LANDSCAPE);
        });
        ((Group)scene.getRoot()).getChildren().add(print);
        return scene;
    }

    public void createJob(PageOrientation orient) {

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return;
        }

        JobSettings settings = job.getJobSettings();
        PageLayout layout = settings.getPageLayout();
        String orientText = orient.toString();
        if (!orient.equals(layout.getPageOrientation())) {
            Printer printer = job.getPrinter();
            PrinterAttributes attributes = printer.getPrinterAttributes();       
            Set<PageOrientation> poSet =
                attributes.getSupportedPageOrientations();
            if (poSet.contains(orient)) {
               layout = printer.createPageLayout(layout.getPaper(),
                                                 orient,
                                                 Printer.MarginType.DEFAULT);
               settings.setPageLayout(layout);
            } else {
                orientText = orientText + " unsupported on this printer.";
            }
        }
        Group root = new Group();

        double w = layout.getPrintableWidth();
        double h = layout.getPrintableHeight();
        Rectangle rect = new Rectangle(1, 1, w-2, h-2);
        rect.setFill(null);
        rect.setStroke(Color.BLACK);
        root.getChildren().add(rect);
        Text l = new Text("Left");
        l.setLayoutX(5);
        l.setLayoutY(h/2);
        root.getChildren().add(l);
        Text r = new Text("Right");
        r.setLayoutX(w-40);
        r.setLayoutY(h/2);
        root.getChildren().add(r);
        Text t = new Text("Top");
        t.setLayoutX(w/2);
        t.setLayoutY(5);
        t.setTextOrigin(VPos.TOP);
        root.getChildren().add(t);
        Text b = new Text("Bottom");
        b.setLayoutX(w/2);
        b.setLayoutY(h-5);
        root.getChildren().add(b);
        Text o = new Text(orientText);
        o.setLayoutX(w/4);
        o.setLayoutY(h/2);
        root.getChildren().add(o);

        Group printingRoot = new Group();
        printingRoot.getChildren().add(root);
        boolean success = job.printPage(printingRoot);
        job.endJob();
    }
}
