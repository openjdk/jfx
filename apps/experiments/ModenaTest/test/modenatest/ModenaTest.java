/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package modenatest;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import modena.Modena;
import org.jemmy.Rectangle;
import org.jemmy.control.Wrap;
import org.jemmy.env.Environment;
import org.jemmy.env.Timeout;
import org.jemmy.fx.NodeDock;
import org.jemmy.image.AWTImage;
import org.jemmy.image.FilesystemImageLoader;
import org.jemmy.image.Image;
import org.jemmy.image.ImageCapturer;
import org.jemmy.image.ImageLoader;
import org.jemmy.image.StrictImageComparator;
import org.jemmy.timing.State;
import org.jemmy.timing.Waiter;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


/**
 * This test runs Modena app and waits for snapshots obtained from the app content
 * to match the golden files.
 * 
 * Unless they perfectly match test will fail. The following information is collected
 * for failed comparisons:
 * current snapshot in build/test/results folder
 * observed image difference in build/test/results/diffs folder
 * 
 * Not all failures necessary mean JavaFX is broken. Here are possible exceptions:
 * - Animated content can't be compared exactly using this approach.
 * - Some minor variations in layout/colors are possible.
 * Manual image comparison is required for all such cases. If observed image is
 * correct you could copy it to corresponding golden images folder. Golden images
 * are located in golden/seven, golden/xp, golden/mac or golden/linux folders.
 * 
 * This project requires Jemmy libraries which are downloaded from SQE repository
 * on first run (see build.xml or lib/readme.txt)
 * 
 * @author akouznet
 */
@RunWith(Parameterized.class)
public class ModenaTest {
    
    private static Map<String, Node> content;
    private static File goldenFolder;
    private static File resFolder;
    private static File diffFolder;
    private static ImageLoader imageLoader;
    private static final String DEFAULT_OS_FOLDER_NAME = "seven";
    
    /*
     * The following method copied from
     * SharedTestUtils\src\test\javaclient\shared\screenshots\ImagesManager.java
     */
    private static String osFolder() {
        String os = System.getProperty("os.name").toLowerCase();
        String version = System.getProperty("os.version").toLowerCase();
        if (os.indexOf("win") >= 0) {
            if (version.startsWith("5.")) {
                return "xp";
            } else if (version.startsWith("6.")) {
                return "seven";
            }
        } else if (os.indexOf("mac") >= 0) {
            return "mac";
        } else if ((os.indexOf("linux") >= 0) || (os.indexOf("ubuntu") >= 0)) {
            return "linux";
        }
        return DEFAULT_OS_FOLDER_NAME;
    }
    
    @Parameterized.Parameters
    public static List<String[]> imageNames() {
        
        goldenFolder = new File(new File("golden"), osFolder());
        File buildTest = new File(new File("build"), "test");
        resFolder = new File(buildTest, "results");
        diffFolder = new File(resFolder, "diffs");
        resFolder.mkdirs();
        
        System.out.println("Golden folder = " + goldenFolder.getAbsolutePath());
        System.out.println("Diffs  folder = " + diffFolder.getAbsolutePath());
        System.out.println("Result folder = " + resFolder.getAbsolutePath());
        
        imageLoader = new FilesystemImageLoader();
        
        new Thread(new Runnable() {

            @Override
            public void run() {
                Modena.launch(Modena.class);
                System.out.println("Modena UI launched");
            }
        }).start();

        AWTImage.setComparator(new StrictImageComparator());
        Environment.getEnvironment().setImageCapturer(new ImageCapturer() {
            
            @Override
            public Image capture(Wrap<?> wrap, Rectangle rctngl) {
                final Node node = (Node) wrap.getControl();
                FutureTask<WritableImage> futureTask = new FutureTask<>(new Callable<WritableImage>() {

                            @Override
                            public WritableImage call() throws Exception {
                                return node.snapshot(null, null);
                            }
                        });
                Platform.runLater(futureTask);
                try {
                    while(true) {
                        try {
                            return new AWTImage(SwingFXUtils.fromFXImage(futureTask.get(), null));
                        } catch (InterruptedException ex) {
                        }
                    }
                } catch (ExecutionException ex) {
                    throw new RuntimeException("Failed to obtain snapshot for " + wrap, ex);
                }
            }
        });
        
        content = new Waiter(new Timeout(null, 60_000))
                .ensureState(new State<Modena>() {

            @Override
            public String toString() {
                return "Waiting for Modena instance";
            }

            @Override
            public Modena reached() {
                return Modena.getInstance();
            }
        }).getContent();    
        
        ArrayList<String[]> arrayList = new ArrayList<>(content.keySet().size());
        for (String name : content.keySet()) {
            arrayList.add(new String[] { name });
        }
        return arrayList;
    }
    
    
    
    private String imageName;
    
    public ModenaTest(String imageName) {
        this.imageName = imageName;
    }

    @AfterClass
    public static void afterClass() {
        System.out.println("Golden images folder = " + goldenFolder.getAbsolutePath());
        System.out.println("Diffs folder = " + diffFolder.getAbsolutePath());
        System.out.println("Folder with images from this run = " + resFolder.getAbsolutePath());
    }
    
    @Test
    public void testNodeImage() {
        String filenameBase = imageName.replaceAll("[^A-Za-z_0-9]+", " ").trim();
        String goldenFilename = filenameBase + ".png";
        String resultFilename = filenameBase + ".png";
        String diffFilename = filenameBase + "_diff.png";
        Node node = content.get(imageName);
        NodeDock nodeDock = new NodeDock(Environment.getEnvironment(), node);
        File file = new File(goldenFolder, goldenFilename);
        if (file.exists()) { 
            try {
                nodeDock.wrap().waitImage(imageLoader.load(file.getAbsolutePath()), 
                        new File(resFolder, resultFilename).getAbsolutePath(), 
                        new File(diffFolder, diffFilename).getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace(System.err);
                throw new AssertionError(filenameBase + " image doesn't match golden", e);
            }
        } else {
            nodeDock.wrap().getScreenImage().save(new File(resFolder, resultFilename).getAbsolutePath());
            throw new AssertionError("No golden file for " + filenameBase);
        }
    }
}
