package dataurl.module;

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class DataUrlWithModuleLayerLauncher {

    public static void main(String[] args) throws Exception {

        System.setProperty("javafx.verbose", "true");

        /*
         * Setup a module layer for OpenJFX and the test class
         */

        // Hack to get the classes of this programm into a module layer
        Path selfPath = Paths.get(DataUrlWithModuleLayerLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path publications = Paths.get("../../../../build/publications");
        ModuleFinder finder = ModuleFinder.of(
            publications,
            selfPath
        );

        /*
         * Load the application as a named module and invoke it
         */
        ModuleLayer parent = ModuleLayer.boot();
        Configuration cf = parent.configuration().resolve(finder, ModuleFinder.of(), Set.of("DataUrlWithModule"));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = parent.defineModulesWithOneLoader(cf, scl);
        ClassLoader moduleClassLoader = layer.findLoader("DataUrlWithModule");
        Class appClass = moduleClassLoader.loadClass("javafx.application.Application");
        Class testClass = moduleClassLoader.loadClass("dataurl.module.DataUrlWithModuleLayer");
        Method launchMethod = appClass.getMethod("launch", Class.class, String[].class);
        launchMethod.invoke(null, new Object[]{testClass, args});
    }
}
