package com.javafx.experiments.importers;

import java.io.IOException;
import java.net.URL;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Pair;
import com.javafx.experiments.importers.dae.DaeImporter;
import com.javafx.experiments.importers.max.MaxLoader;
import com.javafx.experiments.importers.maya.MayaGroup;
import com.javafx.experiments.importers.maya.MayaImporter;
import com.javafx.experiments.importers.obj.ObjImporter;
import com.javafx.experiments.importers.obj.PolyObjImporter;

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
     * Load a 3D file, always loaded as TriangleMesh.
     *
     * @param fileUrl The url of the 3D file to load
     * @return The loaded Node which could be a MeshView or a Group
     * @throws IOException if issue loading file
     */
    public static Node load(String fileUrl) throws IOException {
        return load(fileUrl,false);
    }

    /**
     * Load a 3D file.
     *
     * @param fileUrl The url of the 3D file to load
     * @param asPolygonMesh When true load as a PolygonMesh if the loader supports
     * @return The loaded Node which could be a MeshView or a Group
     * @throws IOException if issue loading file
     */
    public static Node load(String fileUrl, boolean asPolygonMesh) throws IOException {
        return loadIncludingAnimation(fileUrl,asPolygonMesh).getKey();
    }

    /**
     * Load a 3D file.
     *
     * @param fileUrl The url of the 3D file to load
     * @param asPolygonMesh When true load as a PolygonMesh if the loader supports
     * @return The loaded Node which could be a MeshView or a Group and the Timeline animation
     * @throws IOException if issue loading file
     */
    public static Pair<Node,Timeline> loadIncludingAnimation(String fileUrl, boolean asPolygonMesh) throws IOException {
        // get extension
        final int dot = fileUrl.lastIndexOf('.');
        if (dot <= 0) {
            throw new IOException("Unknown 3D file format, url missing extension ["+fileUrl+"]");
        }
        final String extension = fileUrl.substring(dot + 1, fileUrl.length()).toLowerCase();
        switch (extension) {
            case "ma":
                final MayaImporter mayaImporter = new MayaImporter();
                mayaImporter.load(fileUrl);
                final Timeline timeline = mayaImporter.getTimeline();
                return new Pair<Node, Timeline>(mayaImporter.getRoot(),timeline);
            case "ase":
                return new Pair<Node, Timeline>(new MaxLoader().loadMaxUrl(fileUrl),null);
            case "obj":
                final Group res = new Group();
                if (asPolygonMesh) {
                    PolyObjImporter reader = new PolyObjImporter(fileUrl);
                    for (String key : reader.getMeshes()) {
                        res.getChildren().add(reader.buildPolygonMeshView(key));
                    }
                } else {
                    ObjImporter reader = new ObjImporter(fileUrl);
                    for (String key : reader.getMeshes()) {
                        res.getChildren().add(reader.buildMeshView(key));
                    }
                }
                return new Pair<Node, Timeline>(res,null);
            case "fxml":
                final Object fxmlRoot = FXMLLoader.load(new URL(fileUrl));
                if (fxmlRoot instanceof Node) {
                    return new Pair<Node, Timeline>((Node)fxmlRoot,null);
                } else if (fxmlRoot instanceof TriangleMesh) {
                    return new Pair<Node, Timeline>(new MeshView((TriangleMesh)fxmlRoot),null);
                }
                throw new IOException("Unknown object in FXML file ["+fxmlRoot.getClass().getName()+"]");
            case "dae":
                final DaeImporter daeImporter = new DaeImporter(fileUrl, true);
                return new Pair<Node, Timeline>(
                        daeImporter.getRootNode(),
                        null);
            default:
                throw new IOException("Unknown 3D file format ["+extension+"]");
        }
    }
}
