package dataurl.module;

import java.lang.module.ModuleDescriptor;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DataUrlWithModuleLayer extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Module module = Application.class.getModule();

        if (module != null) {
            ModuleDescriptor moduleDesc = module.getDescriptor();

            System.out.println("==========================================================================");
            System.out.println("==");
            System.out.printf("== Module is named:  %b%n", module.isNamed());
            System.out.printf("== Module name:      %s%n", module.getName());

            if (moduleDesc != null) {
                System.out.printf("== Automatic module: %b%n", moduleDesc.isAutomatic());
                System.out.printf("== Module is opened: %b%n", moduleDesc.isOpen());
            }
            System.out.println("==");
            System.out.println("==========================================================================");
        }

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root);

        WebView webview = new WebView();
        root.setCenter(webview);

        // Data URL is green tick taken from
        // https://en.m.wikibooks.org/wiki/File:Tick_green_modern.svg
        // The image was released into the public domain
        String script = "<html>"
            + "<body>"
            + "<h1>Test for loading a data URL</h1>"
            + "<p>The test is successful, if a green tick is displayed below and the JVM does not crash with a segfault.</p>"
            + "<img src=\"data:image/svg+xml,%3C%3Fxml version='1.0' encoding='utf-8'%3F%3E%3C!-- Generator: Adobe Illustrator 12.0.1, SVG Export Plug-In . SVG Version: 6.00 Build 51448) --%3E%3C!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN' 'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd' %5B %3C!ENTITY ns_svg 'http://www.w3.org/2000/svg'%3E%3C!ENTITY ns_xlink 'http://www.w3.org/1999/xlink'%3E%0A%5D%3E%3Csvg version='1.1' id='Layer_1' xmlns='&ns_svg;' xmlns:xlink='&ns_xlink;' width='364.167' height='318.333' viewBox='0 0 364.167 318.333' overflow='visible' enable-background='new 0 0 364.167 318.333' xml:space='preserve'%3E%3Cfilter id='AI_GaussianBlur_4'%3E%3CfeGaussianBlur stdDeviation='4'%3E%3C/feGaussianBlur%3E%3C/filter%3E%3Cg%3E%3Cg%3E%3ClinearGradient id='XMLID_65_' gradientUnits='userSpaceOnUse' x1='20.8413' y1='160.2158' x2='338.6797' y2='160.2158'%3E%3Cstop offset='0' style='stop-color:%235C6470'/%3E%3Cstop offset='1' style='stop-color:%23797F8D'/%3E%3C/linearGradient%3E%3Cpath fill='url(%23XMLID_65_)' filter='url(%23AI_GaussianBlur_4)' d='M279.594,15.998c-1.821,0.246-3.521,1.213-4.615,2.717 c0,0-123.926,167.272-132.245,178.522c-9.5-8.416-65.03-57.29-65.03-57.29c-1.395-1.223-3.24-1.846-5.098-1.684 c-1.846,0.138-3.563,1.04-4.744,2.464L22.4,196.163c-2.393,2.92-1.986,7.214,0.913,9.618l110.188,92.295 c3.994,4.036,9.931,6.44,16.231,6.418c6.225,0.042,12.162-2.362,16.637-6.827c0.677-0.773,171.14-230.113,171.14-230.113 c2.203-2.983,1.126-7.546-1.803-9.853l-50.938-40.275C283.324,16.267,281.442,15.751,279.594,15.998z M144.525,214.134 c1.912-0.195,3.649-1.183,4.777-2.728c0,0,124.518-168.068,132.493-178.802c23.026,18.356,34.801,27.651,40.631,32.182 c-7.709,10.36-166.415,223.93-166.415,223.93c-1.181,1.074-3.619,2.061-6.248,2.038c-2.737,0-5.185-0.988-7.01-2.812 c-0.376-0.365-83.749-70.182-105.412-88.322c6.28-7.685,30.056-36.675,36.637-44.662c9.125,8.052,65.287,57.521,65.287,57.521 C140.693,213.725,142.593,214.348,144.525,214.134z'/%3E%3ClinearGradient id='XMLID_66_' gradientUnits='userSpaceOnUse' x1='178.3188' y1='19.8276' x2='178.3188' y2='269.0803'%3E%3Cstop offset='0' style='stop-color:%236ABD45'/%3E%3Cstop offset='0.6685' style='stop-color:%2306AD4D'/%3E%3Cstop offset='1' style='stop-color:%23055448'/%3E%3C/linearGradient%3E%3Cpath fill='url(%23XMLID_66_)' d='M277.949,13.479l-3.35,4.491L143.842,194.424l-0.965,0.528l-1.017-0.339l-68.624-60.441 l-43.14,52.561l108.058,90.533c2.562,2.562,6.376,4.111,10.497,4.111l9.81-3.413c0.711-0.783,164.943-221.761,164.943-221.761 l3.137-4.226L277.949,13.479z'/%3E%3ClinearGradient id='XMLID_67_' gradientUnits='userSpaceOnUse' x1='121.9199' y1='-31.5498' x2='192.1555' y2='164.109'%3E%3Cstop offset='0' style='stop-color:%236ABD45'/%3E%3Cstop offset='0.6348' style='stop-color:%2306AD4D'/%3E%3Cstop offset='0.7442' style='stop-color:%230C864E'/%3E%3Cstop offset='0.8524' style='stop-color:%23096A4C'/%3E%3Cstop offset='0.9414' style='stop-color:%23045A49'/%3E%3Cstop offset='1' style='stop-color:%23055448'/%3E%3C/linearGradient%3E%3Cpath fill='url(%23XMLID_67_)' d='M275.201,6.393c-1.797,0.243-3.475,1.196-4.555,2.679c0,0-122.301,165.075-130.511,176.177 c-9.376-8.306-64.178-56.532-64.178-56.532c-1.376-1.208-3.198-1.822-5.031-1.664c-1.822,0.138-3.517,1.028-4.681,2.431 l-44.866,54.706c-2.361,2.881-1.96,7.119,0.901,9.492l108.743,91.085c3.941,3.982,9.801,6.355,16.019,6.334 c6.144,0.042,12.003-2.331,16.419-6.738c0.668-0.763,168.896-227.088,168.896-227.088c2.174-2.946,1.111-7.449-1.779-9.726 L280.309,7.8C278.883,6.656,277.025,6.147,275.201,6.393z M141.904,201.924c1.887-0.192,3.602-1.167,4.715-2.692 c0,0,122.885-165.858,130.756-176.452c22.725,18.114,34.344,27.289,40.098,31.758c-7.607,10.224-164.233,220.99-164.233,220.99 c-1.166,1.059-3.571,2.033-6.166,2.011c-2.701,0-5.116-0.975-6.918-2.774c-0.371-0.361-82.651-69.262-104.03-87.164 c6.198-7.585,29.662-36.188,36.157-44.069c9.005,7.944,64.431,56.759,64.431,56.759 C138.122,201.521,139.997,202.136,141.904,201.924z'/%3E%3C/g%3E%3Crect fill='none' width='364.167' height='318.333'/%3E%3C/g%3E%3C/svg%3E%0A\"/>"
            + "</body>"
            + "</html>";

        webview.getEngine().loadContent(script);

        primaryStage.setScene(scene);
        primaryStage.setWidth(1024);
        primaryStage.setHeight(768);
        primaryStage.show();
    }
}
