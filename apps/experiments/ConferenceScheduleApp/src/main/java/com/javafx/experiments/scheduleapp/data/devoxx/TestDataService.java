/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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

import com.javafx.experiments.scheduleapp.model.Availability;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * This pre-populated data service is only useful for doing demos when you have no real
 * login credentials and whatnot.
 */
public class TestDataService extends DevoxxDataService {
    private Map<Integer, Integer> availability = new HashMap<Integer, Integer>();
    private DateFormat formatterDT = new SimpleDateFormat("dd-MMM-yy HH:mm");

    public TestDataService() {
        super(7);
    }

    private long calculateTestNow() {
        long now = System.currentTimeMillis();
        Calendar temp = Calendar.getInstance();
        temp.set(2012, 8, 30, 8, 0, 0);
        long showStart = temp.getTimeInMillis();
        System.out.println("showStart = " + formatterDT.format(temp.getTime()));
        temp.set(2012, 9, 3, 15, 0, 0);
        long showEnd = temp.getTimeInMillis();
        System.out.println("showEnd = " + formatterDT.format(temp.getTime()));
        if (now < showStart || now > showEnd) {
            // pick a random now while the show is on for testing
            now = showStart + (long)((showEnd-showStart)*Math.random());
            System.out.println("now = " + formatterDT.format(new Date(now)));
        }
        return now;
    }

    private long now = calculateTestNow();
    @Override public long getNow() {
        return now;
    }

    @Override public Task<Void> register(Event event) {
        return new Task<Void>() {
            @Override protected Void call() throws Exception {
                Thread.sleep(1000);
                return null;
            }
        };
    }

    @Override public Task<Void> unregister(Event event) {
        return new Task<Void>() {
            @Override protected Void call() throws Exception {
                Thread.sleep(1000);
                return null;
            }
        };
    }

    @Override public Task<Availability> checkAvailability(final Event event) {
        return new Task<Availability>() {
            @Override protected Availability call() throws Exception {
                Thread.sleep(200);
                if (!availability.containsKey(event.getSession().getId())) {
                    availability.put(event.getSession().getId(), (int)((Math.random() * 300) - 30));
                }
                int seatsAvailable = availability.get(event.getSession().getId());
                return new Availability(seatsAvailable <= 0, seatsAvailable <= 0 ? Math.abs(seatsAvailable) : -1);
            }
        };
    }

    @Override public Task<Void> login(String userName, String password) {
        return new Task<Void>() {
            ObservableList<Event> events = FXCollections.observableArrayList();
            @Override protected Void call() throws Exception {
                try {
                    events.add(
                        new Event(
                            "Lunch with FX Team",
                            "Thirsty Bear",
                            "Jasper Potts",
                            new String[]{"Richard Bair","Jonathan Giles"},
                            formatterDT.parse("1-Oct-12 12:00"),
                            formatterDT.parse("1-Oct-12 13:30")
                        ));
                    events.add(
                        new Event(
                            "Dinner with Java User Group",
                            "Jasper's Bar",
                            "Duke",
                            new String[]{"Richard Bair","Jonathan Giles"},
                            formatterDT.parse("2-Oct-12 18:00"),
                            formatterDT.parse("2-Oct-12 20:30")
                        ));
                    // Add a couple Example sessions
                    int sessionCount = 10;
                    while(sessionCount > 0) {
                        System.out.println("getSessions().size()=  "+getSessions().size());
                        Session session = getSessions().get((int)(Math.random()*getSessions().size()));
                        if (session.getSessionTimes().length > 0) {
                            SessionTime sessionTime = session.getSessionTimes()[0];
                            // check start is not the same as a session we already have
                            boolean startMatches = false;
                            long eventStart = sessionTime.getStart().getTime();
                            for(Event event : getEvents()) {
                                if (eventStart == event.getStart().getTime()) {
                                    startMatches = true;
                                    break;
                                }
                            }
                            // add if at new time
                            if (startMatches == false) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(sessionTime.getStart());
                                calendar.add(Calendar.MINUTE, sessionTime.getLength());
                                Event event = new Event(session,sessionTime.getStart(),calendar.getTime(), sessionTime);
                                sessionTime.setEvent(event);
                                events.add(event);
                                sessionTime.setEvent(event);
                                sessionCount --;
                            }
                            Thread.sleep(200);
                            updateProgress(50 - sessionCount, 50);
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                // sort by start time
                Collections.sort(events, new Comparator<Event>() {
                    @Override public int compare(Event o1, Event o2) {
                        return o1.getStart().compareTo(o2.getStart());
                    }
                });

                return null;
            }

            @Override protected void succeeded() {
                getEvents().setAll(events);
            }
        };
    }
}
