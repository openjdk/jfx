package com.javafx.experiments.importers;

import java.io.IOException;
import java.net.URL;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import com.javafx.experiments.importers.dae.DaeImporter;
import com.javafx.experiments.importers.max.MaxLoader;
import com.javafx.experiments.importers.maya.MayaGroup;
import com.javafx.experiments.importers.maya.MayaImporter;
import com.javafx.experiments.importers.obj.ObjImporter;

/**
 * Base Importer for all supported 3D file formats
 */
public final class Importer3D {

    /**
     * Get array of extension filters for supported file formats.
     *
     * @return array of extension filters for supported file formats.
     */
    public static String[] getSupportedFormatExtensionFilters() {
        return new String[]{"*.ma", "*.ase", "*.obj", "*.fxml", "*.dae"};
    }

    /**
     * Load a 3D file.
     *
     * @param fileUrl The url of the 3D file to load
     * @return The loaded Node which could be a MeshView or a Group
     * @throws IOException if issue loading file
     */
    public static Node load(String fileUrl) throws IOException {
        // get extension
        final int dot = fileUrl.lastIndexOf('.');
        if (dot <= 0) {
            throw new IOException("Unknown 3D file format, url missing extension ["+fileUrl+"]");
        }
        final String extension = fileUrl.substring(dot + 1, fileUrl.length()).toLowerCase();
        switch (extension) {
            case "ma":
                return loadMayaFile(fileUrl);
            case "ase":
                return loadMaxFile(fileUrl);
            case "obj":
                return loadObjFile(fileUrl);
            case "fxml":
                Object fxmlRoot = FXMLLoader.load(new URL(fileUrl));
                if (fxmlRoot instanceof Node) {
                    return (Node)fxmlRoot;
                } else if (fxmlRoot instanceof TriangleMesh) {
                    return new MeshView((TriangleMesh)fxmlRoot);
                }
                throw new IOException("Unknown object in FXML file ["+fxmlRoot.getClass().getName()+"]");
            case "dae":
                return loadDaeFile(fileUrl);
            default:
                throw new IOException("Unknown 3D file format ["+extension+"]");
        }
    }

    private static MayaGroup loadMayaFile(String fileUrl) throws IOException {
        MayaImporter mayaImporter = new MayaImporter();
        mayaImporter.load(fileUrl);
        Timeline timeline = mayaImporter.getTimeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        return mayaImporter.getRoot();
    }

    private static Node loadMaxFile(String fileUrl) throws IOException {
        return new MaxLoader().loadMaxUrl(fileUrl);
    }

    private static Node loadObjFile(String fileUrl) throws IOException {
        ObjImporter reader = new ObjImporter(fileUrl);
        Group res = new Group();
        for (String key : reader.getMeshes()) {
            res.getChildren().add(reader.buildMeshView(key));
        }
        return res;
    }

    private static Node loadDaeFile(String fileUrl) throws IOException {
        DaeImporter importer = new DaeImporter(fileUrl, true);
        return importer.getRootNode();
    }
}
