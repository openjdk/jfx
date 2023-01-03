/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class CapsLockTest {

    private static BufferedReader reader;

    public static class App extends Application {
        private void checkCapsLock(boolean expected) throws Exception {
            Optional<Boolean> capsLock = Platform.isKeyLocked(KeyCode.CAPS);
            if (capsLock.isPresent()) {
                System.out.println("isKeyLocked(CAPS) is " + capsLock.get());
                if (capsLock.get() != expected) {
                    System.out.println("TEST FAILED");
                    System.exit(1);
                }
            } else {
                System.out.println("ERROR: isKeyLocked(CAPS) is empty");
                System.out.println("TEST FAILED");
                System.exit(1);
            }
        }

        @Override
        public void start(Stage stage) throws Exception {
            checkCapsLock(true);
            System.out.println("Disable Caps Lock on your system then press ENTER");
            reader.readLine();
            checkCapsLock(false);
            Platform.exit();
        }

    }

    public static void main(String[] args) {
        System.out.println("Enable Caps Lock on your system then press ENTER");
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            reader.readLine();
            Application.launch(App.class, args);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            System.out.println("TEST FAILED");
            System.exit(1);
        }
        System.out.println();
        System.out.println("TEST PASSED");
    }
}
