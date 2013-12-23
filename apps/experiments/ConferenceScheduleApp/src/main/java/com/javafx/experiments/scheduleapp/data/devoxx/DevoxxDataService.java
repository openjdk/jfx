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
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Room;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionType;
import com.javafx.experiments.scheduleapp.model.Speaker;
import com.javafx.experiments.scheduleapp.model.Track;
import com.javafx.experiments.scheduleapp.model.Venue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

/**
 * A implementation of DataService that connects to the Devoxx rest service
 */
public class DevoxxDataService extends DataService {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread th = new Thread(r);
            th.setDaemon(true);
            th.setName("Devoxx Service Thread");
            return th;
        }
    });
    private final int eventId;
    private String userName;
    private String password;
    private LoginTask loginTask;
    private EventHandler<WorkerStateEvent> loginHandler = new EventHandler<WorkerStateEvent>() {
        @Override public void handle(WorkerStateEvent event) {
            events.setAll(loginTask.getEvents());
        }
    };
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final ObservableList<Session> sessions = FXCollections.observableArrayList();
    private final ObservableList<Speaker> speakers = FXCollections.observableArrayList();
    private final ObservableMap<Date,List<Session>> sessionPerTimeSlot = FXCollections.observableHashMap();
    private final ObservableList<Date> startTimes = FXCollections.observableArrayList();
    private final ObservableList<Track> tracks = FXCollections.observableArrayList();
    private final ObservableList<SessionType> sessionTypes = FXCollections.observableArrayList();
    private final ObservableList<Venue> venues = FXCollections.observableArrayList();
    
    public DevoxxDataService(final int eventId) {
        this.eventId = eventId;
        final Venue venue = new Venue("MetroPolis","Metropolis Antwerp, Groenendaallaan 394, 2030 Antwerp, Belgium",
//                                    "https://www.devoxx.com/download/attachments/6389991/metropolis.jpg");
                                    "http://www.devoxx.com/download/attachments/5013556/Metropolis.jpg");
        final GetConferenceDataTask task = new GetConferenceDataTask(venue);
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent event) {
                System.out.println("GetConferenceDataTask SUCEEDED!");
                sessions.setAll(task.getSessionMap().values());
                speakers.setAll(task.getSortedSpeakers());
                sessionPerTimeSlot.putAll(task.getSessionPerTimeSlot());
                startTimes.setAll(task.getSessionPerTimeSlot().keySet());
                tracks.setAll(task.getTrackMap().values());
                sessionTypes.setAll(task.getTypeMap().values());
                List<Room> rooms = new ArrayList<>(task.getRoomMap().values());
                Collections.sort(rooms, new Comparator<Room>() {
                    @Override public int compare(Room o1, Room o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                venue.getRooms().addAll(rooms);
                venues.add(venue);
            }
        });
        EXECUTOR.submit(task);
    }
    
    @Override public ObservableList<Event> getEvents() { return events; }
    @Override public ObservableList<Session> getSessions() { return sessions; }
    @Override public ObservableList<Speaker> getSpeakers() { return speakers; }
    @Override public ObservableList<Date> getStartTimes() { return startTimes; }
    @Override public List<Session> getSessionsAtTimeSlot(Date timeSlot) { 
        final List<Session> s = sessionPerTimeSlot.get(timeSlot);
        if (s == null) {
            return Collections.emptyList();
        }
        return s; 
    }
    @Override public ObservableList<Track> getTracks() { return tracks; }
    @Override public ObservableList<SessionType> getSessionTypes() { return sessionTypes; }
    @Override public ObservableList<Venue> getVenues() { return venues; }

    @Override public Task<Void> register(Event event) {
        if (event == null ||  event.getSession() == null) return null;
        if (!events.contains(event)) {
            events.add(event);
            event.getSessionTime().setEvent(event);
        }
        return new UpdateScheduleTask(events.toArray(new Event[events.size()]),userName,password);
    }

    @Override public Task<Void> unregister(Event event) {
        event.getSessionTime().setEvent(null);
        events.remove(event);
        return new UpdateScheduleTask(events.toArray(new Event[events.size()]),userName,password);
    }
    
    @Override public Task<Void> login(String userName, String password) {
        this.userName = userName;
        this.password = password;
        if (loginTask != null) {
            loginTask.removeEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, loginHandler);
            loginTask.cancel();
        }
        loginTask = new LoginTask(userName,  password, this);
        loginTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, loginHandler);
        return loginTask;
    }

    @Override public String getLoginBackgroundImageUrl() {
        return "https://www.devoxx.com/plugins/servlet/builder/resource/DEVOXX2012/bg.jpg?1339672736848";
    }

    @Override public String getTwitterSearch() {
        return "devoxx";
    }

    @Override public String getTwitterLocalLatLon() {
        return "51.245614,4.416544";
    }

    @Override public String getName() {
        return "Devoxx";
    }
}
