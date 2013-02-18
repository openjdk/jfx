/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import javafx.util.Duration;

import org.junit.Test;

public class PauseTransitionTest {
	
	private static Duration DEFAULT_DURATION = Duration.millis(400);
	
	private static double EPSILON = 1e-12;
	private static Duration ONE_SEC = Duration.millis(1000);
	private static Duration TWO_SECS = Duration.millis(2000);
	
	
	@Test
	public void testDefaultValues() {
		// empty ctor
		final PauseTransition t0 = new PauseTransition();
		assertEquals(DEFAULT_DURATION, t0.getDuration());
        assertEquals(DEFAULT_DURATION, t0.getCycleDuration());
		assertNull(t0.getOnFinished());
		
		// duration only
		final PauseTransition t1 = new PauseTransition(ONE_SEC);
		assertEquals(ONE_SEC, t1.getDuration());
		assertNull(t1.getOnFinished());
	}

    @Test
    public void testDefaultValuesFromProperties() {
        // empty ctor
        final PauseTransition t0 = new PauseTransition();
        assertEquals(DEFAULT_DURATION, t0.durationProperty().get());
        assertNull(t0.onFinishedProperty().get());

        // duration only
        final PauseTransition t1 = new PauseTransition(ONE_SEC);
        assertEquals(ONE_SEC, t1.durationProperty().get());
        assertNull(t1.onFinishedProperty().get());
    }
}
