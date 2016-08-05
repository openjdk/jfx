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

package hello;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloService {

    public static void main(String... args) throws IOException {
        Handler fileHandler = new FileHandler("%t/HelloService-%g.log", 1*1024*1024, 4, true);
        Logger log = Logger.getLogger("hello");
        log.addHandler(fileHandler);
        log.setUseParentHandlers(false);
        long sleepTime = 1;
        Random random = new SecureRandom();
        while (true) {
            try {
                Thread.sleep(sleepTime * 1000);
                log.info("Slept for " + sleepTime + "s and it is now " + LocalDateTime.now());
                sleepTime = random.nextInt(60);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Oppsies!", e);
            }
        }
    }
}

