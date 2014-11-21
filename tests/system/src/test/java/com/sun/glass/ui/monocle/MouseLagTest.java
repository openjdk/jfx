/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class MouseLagTest {

    private UInput ui;
    @Rule public TestName name = new TestName();

    @Before public void setUpScreen() throws Exception {
        TestLog.reset();
        TestLog.log(name.getMethodName());
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.movePointerTo(300, 300);
        initDevice();
    }

    public void initDevice() throws Exception {
        ui = new UInput();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("KEYBIT BTN_LEFT");
        ui.processLine("EVBIT EV_REL");
        ui.processLine("RELBIT REL_X");
        ui.processLine("RELBIT REL_Y");
        ui.processLine("PROPERTY ID_INPUT_MOUSE 1");
        ui.processLine("CREATE");
    }

    @After public void destroyDevice() throws Exception {
        if (ui != null) {
            try {
                ui.processLine("DESTROY");
            } catch (RuntimeException e) { }
            ui.processLine("CLOSE");
            ui.dispose();
        }
    }

    /** Make sure we can process 500 mouse motion events per second. We are
     * not required to report all these events, but must report the last one.
     */
    @Test
    public void testMouseLag() throws Exception {
        byte[] moveLeft = new byte[256];
        int offset;
        offset = ui.writeLine(moveLeft, 0, "EV_REL REL_X -1");
        offset = ui.writeLine(moveLeft, offset, "EV_REL REL_Y 0");
        int moveLeftLength = ui.writeLine(moveLeft, offset, "EV_SYN");
        byte[] moveRight = new byte[256];
        offset = ui.writeLine(moveRight, 0, "EV_REL REL_X 1");
        offset = ui.writeLine(moveRight, offset, "EV_REL REL_Y 0");
        int moveRightLength = ui.writeLine(moveRight, offset, "EV_SYN");
        byte[] moveDown = new byte[256];
        offset = ui.writeLine(moveDown, 0, "EV_REL REL_X 0");
        offset = ui.writeLine(moveDown, offset, "EV_REL REL_Y 1");
        int moveDownLength = ui.writeLine(moveDown, offset, "EV_SYN");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 150; j++) {
                ui.write(moveLeft, 0, moveLeftLength);
            }
            ui.write(moveDown, 0, moveDownLength);
            for (int j = 0; j < 150; j++) {
                ui.write(moveRight, 0, moveRightLength);
            }
        }

        long t = System.currentTimeMillis() - startTime;
        // Make sure events could be sent in the required time
        Assert.assertTrue("Took " + t + "ms to send 3000 events, of which "
                          + TestLog.countLogContaining("moved")
                          + " were received",
                          t < 6000l * TestApplication.getTimeScale());
        TestLog.log("Sent 3000 events in " + t + "ms");
        // Make sure events could be delivered in the required time
        TestLog.waitForLog("Mouse moved: 300, 310", 6000l - t);
    }

}
