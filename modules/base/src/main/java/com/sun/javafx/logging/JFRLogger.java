/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.logging;

import com.oracle.jrockit.jfr.EventToken;
import com.oracle.jrockit.jfr.Producer;

/**
 * Logs pulse related information with Java Flight Recorder.
 */
class JFRLogger extends Logger {
    
    private static final String PRODUCER_URI = "http://www.oracle.com/technetwork/java/javafx/index.html";
    private static JFRLogger jfrLogger;
    
    private final Producer producer;
    private final EventToken pulseEventToken;
    private final EventToken inputEventToken;
    private final ThreadLocal<JFRPulseEvent> curPhaseEvent;
    private final ThreadLocal<JFRInputEvent> curInputEvent;
        
    private JFRLogger() throws Exception {
        producer = new Producer("JavaFX producer", "JavaFX producer.", PRODUCER_URI);
        pulseEventToken = producer.addEvent(JFRPulseEvent.class);
        inputEventToken = producer.addEvent(JFRInputEvent.class);
        producer.register();
        curPhaseEvent = new ThreadLocal() {
            @Override
            public JFRPulseEvent initialValue() {
                return new JFRPulseEvent(pulseEventToken);
            }
        };
        curInputEvent = new ThreadLocal(){
            @Override
            public JFRInputEvent initialValue() {
                return new JFRInputEvent(inputEventToken);
            }
        };
    }
    
    public static JFRLogger getInstance() {
        if (jfrLogger == null) {
            /* Guards against exceptions in the constructor and the absence of jfr.jar at run time */
            try {
                Class klass = Class.forName("com.oracle.jrockit.jfr.FlightRecorder");
                if (klass != null && com.oracle.jrockit.jfr.FlightRecorder.isActive()) {
                    jfrLogger = new JFRLogger();
                }
            }
            catch (Exception e) {
                jfrLogger = null;
            }
        }
        return jfrLogger;
    }
    
    /**
     *  Pulse number reconstruction for the render thread relies on the current synchronization 
     *  between the FX and render threads: renderStart() is called on the FX thread after all
     *  previous RenderJobs have finished and before any new RenderJob is pushed.
     */
    private int pulseNumber;
    private int fxPulseNumber;
    private int renderPulseNumber;
    private Thread fxThread;
    
    @Override
    public void pulseStart() {
        ++pulseNumber;
        fxPulseNumber = pulseNumber;
        if (fxThread == null) {
            fxThread = Thread.currentThread();
        }
        newPhase("Pulse start");
    }
    
    @Override
    public void pulseEnd() {
        newPhase(null);
        fxPulseNumber = 0;
    }
    
    @Override
    public void renderStart() {
        renderPulseNumber = fxPulseNumber;
    }

    @Override
    public void renderEnd() {
        newPhase(null);
        renderPulseNumber = 0;
    }
    
    /**
     * Finishes the current phase and starts a new one if phaseName is not null.
     * @param phaseName The name for the new phase.
     */
    @Override
    public void newPhase(String phaseName) {
        if (pulseEventToken == null) {
            return;
        }
        
        JFRPulseEvent event = curPhaseEvent.get();

        /* Cleanup if recording has finished */
        if (!pulseEventToken.isEnabled()) {
            event.setPhase(null);
            return;
        }
        
        /* Finish the previous phase if any */
        if (event.getPhase() != null) {
            event.end();
            event.commit();
        }

        /* Done if the new phase name is null */
        if (phaseName == null) {
            event.setPhase(null);
            return;
        }
                
        event.reset();
        event.begin();
        event.setPhase(phaseName);
        event.setPulseNumber(Thread.currentThread() == fxThread ? fxPulseNumber : renderPulseNumber);
    }

    @Override
    public void newInput(String input) {
        if (inputEventToken == null) {
            return;
        }

        JFRInputEvent event = curInputEvent.get();

        /* Cleanup if recording has finished */
        if (!inputEventToken.isEnabled()) {
            event.setInput(null);
            return;
        }
        
        /* Finish the previous input event if any */
        if (event.getInput() != null) {
            event.end();
            event.commit();
        }

        /* Done if the new input is null */
        if (input == null) {
            event.setInput(null);
            return;
        }
        
        event.reset();
        event.begin();
        event.setInput(input);
    }    
}
