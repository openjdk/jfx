/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import java.io.IOException;
import org.junit.Test;

public class RT_25494_Cycle_DetectionTest {
    
    @Test(expected=IOException.class) 
    public void test_dummy_cycle() throws Exception {
        FXMLLoader.load(RT_25494_Cycle_DetectionTest.class.getResource("dummy-cycle.fxml"));
    }
    
    @Test(expected=IOException.class) 
    public void test_one_2_one_cycle() throws Exception {
        FXMLLoader.load(RT_25494_Cycle_DetectionTest.class.getResource("one-2-one-cycle.fxml"));
    }
    
    @Test(expected=IOException.class) 
    public void test_cycle() throws Exception {
        FXMLLoader.load(RT_25494_Cycle_DetectionTest.class.getResource("cycle.fxml"));
    }
}
