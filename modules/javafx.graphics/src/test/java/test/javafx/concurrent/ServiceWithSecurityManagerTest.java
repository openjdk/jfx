/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.concurrent;

import java.security.Permission;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * A test for the Service lifecycle methods with a security manager installed.
 * The security manager installed lets privileged code run (most of the time)
 * and otherwise lets the tests do what they need to but restricts the
 * runtime from doing more or less anything else other than load library.
 * It is ad-hoc, a more rigorous analysis on what the permissions should be
 * would be great, and then we could add the ability to do security manager
 * related tests on lots of different unit tests.
 */
@RunWith(ServiceWithSecurityManagerTest.ServiceTestRunner.class)
@Ignore("JDK-8234175") // This class doesn't appear to run correctly, often s.evaluate isn't called. Likely bogus test at present.
public class ServiceWithSecurityManagerTest extends ServiceLifecycleTest {

    public static final class ServiceTestRunner extends BlockJUnit4ClassRunner {
        private ThreadGroup mainThreadGroup;

        public ServiceTestRunner(Class<?> klass) throws InitializationError {
            super(klass);
        }

        @Override protected Statement methodBlock(FrameworkMethod method) {
            final Statement s = super.methodBlock(method);
            return new Statement() {
                Throwable throwable;
                @Override public void evaluate() throws Throwable {
                    SecurityManager original = System.getSecurityManager();
                    try {
                        mainThreadGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "FX Test Thread Group");
                        Thread thread = new Thread(mainThreadGroup, () -> {
                            try {
                                s.evaluate();
                            } catch (Throwable t) {
                                throwable = t;
                            }
                        });

                        System.setSecurityManager(new StrictSecurityManager());
                        thread.start();
                        thread.join();
                    } finally {
                        System.setSecurityManager(original);
                        mainThreadGroup = null;
                        if (throwable != null) {
                            throw throwable;
                        }
                    }
                }
            };
        }

        /**
         */
        private final class StrictSecurityManager extends SecurityManager {
            // If you create a Thread that is a child of mainThreadGroup, that is OK.
            // If you create a ThreadGroup that is a child of mainThreadGroup, then that is bad.
            private ThreadGroup securityThreadGroup = new ThreadGroup("Security Thread Group");

            @Override public void checkPermission(Permission permission) {
                if (isPrivileged()) return; // OK
                if (permission instanceof RuntimePermission) {
                    if ("setSecurityManager".equals(permission.getName())) {
                        return; // OK
                    }
                    if ("accessClassInPackage.sun.util.logging".equals(permission.getName())) {
                        return; // OK
                    }
                }
                super.checkPermission(permission);
            }

            @Override public void checkAccess(ThreadGroup g) {
                if (g == securityThreadGroup) return;
                if (!isPrivileged()) throw new SecurityException("ThreadGroup doesn't have permissions");
                super.checkAccess(g);
            }

            @Override public ThreadGroup getThreadGroup() {
                return securityThreadGroup;
            }

            private boolean isPrivileged() {
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                for (StackTraceElement e : stack) {
                    if (e.getClassName().equals("java.security.AccessController")
                            && e.getMethodName().equals("doPrivileged")) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
