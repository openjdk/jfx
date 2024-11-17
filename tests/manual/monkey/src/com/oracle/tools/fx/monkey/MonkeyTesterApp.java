/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey;

import javafx.application.Application;
import javafx.stage.Stage;
import com.oracle.tools.fx.monkey.settings.FxSettings;

/**
 * Monkey Tester Application.
 *
 * Applications stores its user preferences (window location, etc.) in ~/.MonkeyTester directory.
 * To use a different directory, redefine the "user.home" system property, -Duser.home=<...>.
 * To disable saving, specify -Ddisable.settings=true vm agrument.
 */
public class MonkeyTesterApp extends Application {
    public static void main(String[] args) {
        Application.launch(MonkeyTesterApp.class, args);
    }

    @Override
    public void init() {
        if (!Boolean.getBoolean("disable.settings")) {
            FxSettings.useDirectory(".MonkeyTester");
        }
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public void start(Stage stage) throws Exception {
        new MainWindow().show();
    }
}
