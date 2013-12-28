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
package com.javafx.experiments.scheduleapp.data.devoxx;

import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.data.JSONParserJP;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * A Task which performs login authentication.
 */
class LoginTask extends Task<Void> {

    /**
     * The user name to login with. This must be supplied via the constructor.
     */
    private final String userName;

    /**
     * The password to login with. This must be supplied via the constructor.
     */
    private final String password;

    private final DataService dataService;
    private Map<Integer,Session> sessionMap = new HashMap<>();

    /**
     * The list of all events that this logged in user is registered for.
     */
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    public ObservableList<Event> getEvents() {
        return events;
    }

    /**
     * Creates a new LoginTask.
     *
     * @param userName The user name.
     * @param password The password.
     */
    LoginTask(String userName, String password, DataService dataService) {
        this.userName = userName;
        this.password = password;
        this.dataService = dataService;
        for (Session session: dataService.getSessions()) {
            sessionMap.put(session.getId(), session);
        }
    }

    @Override protected Void call() throws Exception {
        updateProgress(1, 10);

        try {
            loadEvents();
        } catch(Exception e) {
            throw e;
        }

        updateProgress(10, 10);

        return null;
    }
    
    private void loadEvents() throws Exception {
        System.out.println("LOGIN TASK loadEvents()");
        events.clear();
        JSONParserJP.parse("http://cfp.devoxx.com/rest/v1/events/7/schedule/"+userName, new EventCallback());
    }
    
    private class EventCallback extends JSONParserJP.CallbackAdapter {
        @Override public void keyValue(String key, String value, int depth) {
            if (depth == 2 && "id".equals(key)){
                int sessionId = Integer.parseInt(value);
                Session session = sessionMap.get(sessionId);
                if (session != null) {
                    SessionTime sessionTime = session.getSessionTimes()[0];
                    Date end = new Date(
                            sessionTime.getStart().getTime() + (60000*sessionTime.getLength()));
                    Event event = new Event(session, sessionTime.getStart(), end, sessionTime);
                    sessionTime.setEvent(event);
                    events.add(event);
                }
            }
        }
    }
}
