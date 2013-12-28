/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.scheduleapp.data;

import com.javafx.experiments.scheduleapp.model.Availability;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionType;
import com.javafx.experiments.scheduleapp.model.Speaker;
import com.javafx.experiments.scheduleapp.model.Track;
import com.javafx.experiments.scheduleapp.model.Venue;
import java.util.Date;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * Central store of all data. Abstract base class for all DataServices.
 */
public abstract class DataService {

    public abstract String getName(); // eg. Devoxx or JavaOne
    public abstract String getLoginBackgroundImageUrl();
    public abstract String getTwitterSearch(); // eg "java"
    public abstract String getTwitterLocalLatLon(); // eg "37.785313,-122.409459"
    
    public abstract ObservableList<Event> getEvents();
    public abstract ObservableList<Session> getSessions();
    public abstract ObservableList<Speaker> getSpeakers();
    public abstract ObservableList<Date> getStartTimes();
    public abstract List<Session> getSessionsAtTimeSlot(Date timeSlot);
    public abstract ObservableList<Track> getTracks();
    public abstract ObservableList<SessionType> getSessionTypes();
    public abstract ObservableList<Venue> getVenues();
    public abstract Task<Void> register(Event event);
    public abstract Task<Void> unregister(Event event);
    public abstract Task<Void> login(String userName, String password);
    
    // default impl that returns that all sessions are always available
    public Task<Availability> checkAvailability(Event event) {
        return new Task<Availability>(){
            @Override protected Availability call() throws Exception {
                return new Availability(false, -1);
            }
        };
    }
    
    public long getNow() {
        return System.currentTimeMillis();
    }
}
