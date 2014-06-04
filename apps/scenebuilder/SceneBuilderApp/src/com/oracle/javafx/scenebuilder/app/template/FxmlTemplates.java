/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.app.template;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.oracle.javafx.scenebuilder.app.SceneBuilderApp.ApplicationControlAction;

/**
 *
 */
public class FxmlTemplates {

    public static URL getContentURL(ApplicationControlAction action) {
        final String name = getTemplateFileName(action);
        return FxmlTemplates.class.getResource(name);
    }

    public static String getTemplateName(ApplicationControlAction action) {
        final String name = getTemplateFileName(action);
        return name.substring(0, name.indexOf(".fxml")); //NOI18N
    }

    public static String getTemplateFileName(ApplicationControlAction action) {
        String name;
        switch (action) {
            case NEW_ALERT_DIALOG:
                name = "AlertDialog.fxml"; //NOI18N
                break;
            case NEW_ALERT_DIALOG_CSS:
                name = "AlertDialog_css.fxml"; //NOI18N
                break;
            case NEW_ALERT_DIALOG_I18N:
                name = "AlertDialog_i18n.fxml"; //NOI18N
                break;
            case NEW_BASIC_APPLICATION:
                name = "BasicApplication.fxml"; //NOI18N
                break;
            case NEW_BASIC_APPLICATION_CSS:
                name = "BasicApplication_css.fxml"; //NOI18N
                break;
            case NEW_BASIC_APPLICATION_I18N:
                name = "BasicApplication_i18n.fxml"; //NOI18N
                break;
            case NEW_COMPLEX_APPLICATION:
                name = "ComplexApplication.fxml"; //NOI18N
                break;
            case NEW_COMPLEX_APPLICATION_CSS:
                name = "ComplexApplication_css.fxml"; //NOI18N
                break;
            case NEW_COMPLEX_APPLICATION_I18N:
                name = "ComplexApplication_i18n.fxml"; //NOI18N
                break;
            default:
                name = null;
                break;
        }
        return name;
    }

    public static Set<String> getResourceFileNames(ApplicationControlAction action) {
        final Set<String> names = new HashSet<>();
        switch (action) {
            case NEW_ALERT_DIALOG_CSS:
                names.add("AlertDialog.css"); //NOI18N
                names.add("AlertDialog.png"); //NOI18N
                break;
            case NEW_ALERT_DIALOG_I18N:
                names.add("AlertDialog.css"); //NOI18N
                names.add("AlertDialog.png"); //NOI18N
                names.add("AlertDialog_en.properties"); //NOI18N
                names.add("AlertDialog_fr.properties"); //NOI18N
                break;
            case NEW_BASIC_APPLICATION_CSS:
                names.add("BasicApplication.css"); //NOI18N
                break;
            case NEW_BASIC_APPLICATION_I18N:
                names.add("BasicApplication.css"); //NOI18N
                names.add("BasicApplication_en.properties"); //NOI18N
                names.add("BasicApplication_fr.properties"); //NOI18N
                break;
            case NEW_COMPLEX_APPLICATION_CSS:
                names.add("ComplexApplication.css"); //NOI18N
                break;
            case NEW_COMPLEX_APPLICATION_I18N:
                names.add("ComplexApplication.css"); //NOI18N
                names.add("ComplexApplication_en.properties"); //NOI18N
                names.add("ComplexApplication_fr.properties"); //NOI18N
                break;
            default:
                break;
        }
        return names;
    }
}
