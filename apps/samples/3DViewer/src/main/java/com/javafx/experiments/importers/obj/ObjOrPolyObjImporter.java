/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.importers.obj;

import com.javafx.experiments.importers.Importer;
import java.io.IOException;
import javafx.scene.Group;

/**
 * object loader
 */
public class ObjOrPolyObjImporter extends Importer {

    final Group res = new Group();

    @Override
    public void load(String fileUrl, boolean asPolygonMesh) throws IOException {

    if (asPolygonMesh) {
        PolyObjImporter reader = new PolyObjImporter(fileUrl);
        for (String mesh : reader.getMeshes()) {
        res.getChildren().add(reader.buildPolygonMeshView(mesh));
        };
    } else {
        ObjImporter reader = new ObjImporter(fileUrl);
        for (String mesh : reader.getMeshes()) {
        res.getChildren().add(reader.buildMeshView(mesh));
        };
    }
    }

    @Override
    public Group getRoot() {
    return res;
    }

    @Override
    public boolean isSupported(String extension) {
    return extension != null && extension.equals("obj");
    }
}
