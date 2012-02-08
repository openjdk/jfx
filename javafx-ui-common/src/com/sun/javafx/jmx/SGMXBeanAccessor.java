/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.jmx;

import com.oracle.javafx.jmx.SGMXBean;

import javax.management.*;
import java.lang.Object;
import java.lang.management.ManagementFactory;

/**
 * This class provides utility methods for Scene-graph JMX bean.
 */
public class SGMXBeanAccessor {

    private static final String sgBeanClass = System.getProperty("javafx.debug.jmx.class", "com.oracle.javafx.jmx.SGMXBeanImpl");

    /**
     * Instantiates and registers Scene-graph JMX bean ({@link SGMXBean})
     * into default MBean server.
     */
    public static void registerSGMXBean() {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        try {
            Object mxBean = Class.forName(sgBeanClass).newInstance();
            if (!(mxBean instanceof SGMXBean)) {
                throw new IllegalArgumentException("JMX: " + sgBeanClass + " does not implement " +
                    SGMXBean.class.getName() + " interface.");
            }

            ObjectName mbeanName = new ObjectName("com.oracle.javafx.jmx:type=SGBean");
            mbs.registerMBean(mxBean, mbeanName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
