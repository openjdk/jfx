/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.glass.ui.monocle;

import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.monocle.LinuxInputDeviceRegistryShim;
import test.com.sun.glass.ui.monocle.NativeUInput;

class MonocleUInput extends NativeUInput {

    private Pipe pipe;
    private LinuxInputDeviceRegistryShim.LinuxInputDeviceShim device;

    MonocleUInput() {
        super();
    }

    @Override
    protected void createDevice() {
        try {
            pipe = Pipe.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        uevent.put("PRODUCT",
                   Integer.toHexString(bus) + "/"
                   + Integer.toHexString(vendor) + "/"
                   + Integer.toHexString(product) + "/"
                   + Integer.toHexString(version));
        Application.invokeAndWait(() -> device
                = LinuxInputDeviceRegistryShim.addDevice(capabilities,
                        absCaps,
                        pipe.source(),
                        udevManifest,
                        uevent,
                        "Simulated Linux Input Device"));
    }

    @Override
    protected void openConnection() {
    }

    @Override
    protected void closeConnection() {
    }

    @Override
    protected void destroyDevice() {
        try {
            if (pipe != null) {
                pipe.sink().close();
                pipe.source().close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        pipe = null;
        if (device != null) {
            final LinuxInputDeviceRegistryShim.LinuxInputDeviceShim d = device;
            if (Platform.isFxApplicationThread()) {
                LinuxInputDeviceRegistryShim.removeDevice(d);
            } else {
                CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    LinuxInputDeviceRegistryShim.removeDevice(d);
                    latch.countDown();
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    fail(e);
                }
            }
            device = null;
        }
    }

    @Override
    public void setup() {
    }

    @Override
    public void dispose() {
        destroyDevice();
        closeConnection();
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        pipe.sink().write(buffer);
    }

    @Override
    public void waitForQuiet() throws InterruptedException {
        if (device != null) {
            AtomicReference<Boolean> done = new AtomicReference<>(Boolean.FALSE);
            do {
                Application.invokeAndWait(() -> done.set(device.isQuiet()));
                if (!done.get()) {
                    TestApplication.waitForNextPulse();
                }
            } while (!done.get());
        }
    }
}
