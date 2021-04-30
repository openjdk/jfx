/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.scene.web;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.security.AccessControlContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;


class JS2JavaBridge {
    private String TOKEN = null;//Security token; used to secure JavaBridge in JavaScript
    private String javaBridge = null; //"window.mustek('"+TOKEN+"')"

    private final WebEngine webEngine;
    private static AtomicInteger objectIdCounter = new AtomicInteger(0);
    // map for call (key is objId)
    private Map<String, ExportedJavaObject> exportedObjectsById = new HashMap<String, ExportedJavaObject>();
    // map for getExportedObject()
    private Map<Object, ExportedJavaObject> exportedObjectsByJavaObject = new HashMap<Object, ExportedJavaObject>();

    private JSONEncoder encoder = null;
    private JSONDecoder decoder = null;
    private boolean bridgeExported = false;//JavaBridge is already expoerted to JS
    //mapping of exported java objects <-> javascript ids (exportedJSObjects[] )
    private Map<String,ExportedJavaObject> exportedObjectsByJSIds = new HashMap<String, ExportedJavaObject>();
    private Map<ExportedJavaObject,String> jsIdsByExportedObjects = new HashMap<ExportedJavaObject, String>();

    public JS2JavaBridge(WebEngine webEngine) {
        this.webEngine = webEngine;
        encoder = new JSONEncoder(this);
        decoder = new JSONDecoder(this);
        webEngine.setPageListener(new PageListenerImpl());
        //Generate secure key
        Random ranGen = new SecureRandom();
        byte[] aesKey = new byte[16]; // 16 bytes = 128 bits
        ranGen.nextBytes(aesKey);
        try {
            TOKEN = UUID.nameUUIDFromBytes(aesKey).toString();
        } catch (Error ex) {
            TOKEN = null;
        }
        javaBridge = "window.mustek('"+TOKEN+"')";
    }

    // used by exportObject() and by JSONEncoder
    public ExportedJavaObject createExportedJavaObject(Object object) {
        ExportedJavaObject jsObj = exportedObjectsByJavaObject.get(object);
        if (jsObj == null) {
            String objId = Integer.toString(objectIdCounter.incrementAndGet());
            jsObj = new ExportedJavaObject(this, objId, object);
            exportedObjectsById.put(objId, jsObj);
            exportedObjectsByJavaObject.put(object, jsObj);
        }
        return jsObj;
    }

    // the method is called from WebEngine.exportObject, syncronized with loadedLock
    public void exportObject(String jsName, Object object) {
        log(">>exportObject: " + jsName);
        ExportedJavaObject jsObj = createExportedJavaObject(object);
        jsObj.addJSName(jsName);
        if (bridgeExported) {
            populateObject(jsName, jsObj);
        }
        log("<<exportObject");
    }

    Object executeScript(String script) {
        return webEngine.executeScript(script);
    }
    String getJavaBridge() {
        return javaBridge;
    }

    AccessControlContext getAccessControlContext() {
        return webEngine.getAccessControlContext();
    }

    private void populateObject(String jsName, ExportedJavaObject jsObj) {
        if (exportedObjectsByJSIds.containsValue(jsObj)) {
            return;
        }
        StringBuilder sb;
        sb = new StringBuilder(1024).append(getJavaBridge());
        String script = sb.append("['").append(jsName).append("'] = ").append(jsObj.getJSDecl()).toString();
        log(script);
        log("populateObject>>executeScript");
        webEngine.executeScriptDirect(script);
        sb.delete(0, sb.length());
        sb = sb.append(getJavaBridge()).append(".exportJSObject(").append(
             getJavaBridge()).append("['").append(jsName).append("'])");

        String jsId = String.valueOf(webEngine.executeScript(sb.toString()));
        if (!jsId.equals("null")) {
            exportedObjectsByJSIds.put(jsId, jsObj);
            jsIdsByExportedObjects.put(jsObj, jsId);
        }
        else {
            System.out.println("[JVDBG] Error, jsId = null for "+jsName);
        }
        log("populateObject<<executeScript");
    }

    private StringBuilder getInitScript(String scriptFile) {
        StringBuilder script= new StringBuilder();
        try {
            script.append(loadResource(scriptFile));
        } catch (Exception ex) {
            log(ex);
        }
        return script;
    }

    Object decode(String retVal) {
        return decoder.decode(retVal);
    }

    void encode(Object arg, StringBuilder script) {
        encoder.encode(script, arg);
    }


    String getjsIdForJavaObject(Object object) {

        exportObject("helper_export_Object", object);

        if (exportedObjectsByJavaObject.containsKey(object)) {
            ExportedJavaObject ejo = exportedObjectsByJavaObject.get(object);
            if (jsIdsByExportedObjects.containsKey(ejo)) {
                return jsIdsByExportedObjects.get(ejo);
            }
        }
        return null;
    }

    Object getJavaObjectForjsId(String value) {
        if (exportedObjectsByJSIds.containsKey(value)) {
            ExportedJavaObject ejo = exportedObjectsByJSIds.get(value);
            return ejo.getJavaObject();
        }
        return null;
    }

    class JSEventHandler {
        JSEventHandler() {}
        public void onAlertNotify(String message) {
            webEngine.onAlertNotify(message);
        }
    }

    private class PageListenerImpl implements WebEngine.PageListener {

        void populateJavaObjects() {
            //Create JavaBridge Object by running init javascript
            StringBuilder sb = getInitScript("init.js");
            sb.append("('").append(TOKEN).append("')");
            webEngine.executeScriptDirect(sb.toString());

            // receives  alert(message), etc.
            exportObject("jsEventHandler", new JSEventHandler());

            // populate all "named" ExportedJavaObjects
            for (Map.Entry<String, ExportedJavaObject> entry: exportedObjectsById.entrySet()) {
                ExportedJavaObject jsObj = entry.getValue();
                List<String> jsNames = jsObj.getJSNames();
                for (String name: jsNames) {
                    populateObject(name, jsObj);
                }
            }

            bridgeExported = true;
        }

        @Override
        public void onLoadStarted() {
            objectIdCounter.set(0);
            exportedObjectsByJavaObject.clear();
            // TODO: free all unnamed ExportedJavaObjects
        }

        @Override
        public void onLoadFinished() {
            populateJavaObjects();
        }

        @Override
        public void onLoadFailed() {
        }

        @Override
        public void onJavaCall(String s) {
            // javacall:uuid:callbackID:objId:methodId:args
            String[] splitted = s.split(":", 6);
            //splitted[0]: javacall
            String uuid = splitted[1];
            String callbackID = splitted[2];
            try {
                if (TOKEN != null && !TOKEN.equals(uuid)) {
                    throw new SecurityException("Wrong javacall arguments.");
                }
                String objId = splitted[3];
                String methodId = splitted[4];
                String args = URLDecoder.decode(splitted[5], "UTF-8");

                ExportedJavaObject jsObj = exportedObjectsById.get(objId);
                // NPE if object not found
                String result = jsObj.call(methodId, args);
                if (!"0".equals(callbackID)) {
                    String script;
                    StringBuilder sb = new StringBuilder(1024);
                    script = sb.append(getJavaBridge()).append(".callBack(").append(callbackID).append(", true").append(
                         (result == null ? "" : ", " + result)).append(")").toString();
                    if (_log) {
                        log("result callback script (success): >" + script + "<");
                        log(">>executeScript");
                    }
                    webEngine.executeScriptDirect(script);
                    log("<<executeScript");
                }
            } catch (Exception ex) {
                log("onJavaCall: exception:");
                log(ex);
                // report failure
                if (!"".equals(callbackID)) {
                    StringBuilder sb = new StringBuilder(1024).append(getJavaBridge()).append(".callBack(").
                            append(callbackID).append(", false, ");
                    encoder.encode(sb,ex.getMessage());
                    String script = sb.append(")").toString();
                    log("result callback script (failure): >" + script + "<");
                    log(">>executeScript");
                    webEngine.executeScriptDirect(script);
                    log("<<executeScript");
                }
            }
        }
    }

    private String loadResource(String name) throws Exception {
        StringBuilder sb = new StringBuilder();
        InputStream inStream = getClass().getResourceAsStream(name);
        InputStreamReader reader = new InputStreamReader(inStream, "utf-8");
        char[] buffer = new char[1024];
        int read;
        try {
            while ((read = reader.read(buffer)) >= 0) {
                sb.append(buffer, 0, read);
            }
        } finally {
            reader.close();
        }
        String content = sb.toString();
        log("Loaded resource (\"" + name + "\"):");
        log(content);
        return content;
    }

    static boolean _log = false;

    static void log(String s) {
        if (!_log) {
            return;
        }
        System.err.println(s);
        System.err.flush();
    }
    static void log(Exception ex) {
        if (!_log) {
            return;
        }
        ex.printStackTrace(System.err);
        System.err.flush();
    }
}
