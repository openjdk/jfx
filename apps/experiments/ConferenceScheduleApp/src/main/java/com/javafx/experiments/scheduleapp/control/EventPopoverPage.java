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
package com.javafx.experiments.scheduleapp.control;

import com.javafx.experiments.scheduleapp.ConferenceScheduleApp;
import com.javafx.experiments.scheduleapp.PlatformIntegration;
import static com.javafx.experiments.scheduleapp.Theme.*;
import com.javafx.experiments.scheduleapp.model.Availability;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Session;
import java.text.DateFormat;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * The content for displaying a event in a popover.
 */
public class EventPopoverPage extends Region implements Popover.Page {
    private static final DateFormat DAY_TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);
    /**
     * The ExecutorService used for running our get availability tasks. We could have just created
     * a new thread each time, but there really isn't a need for it.
     */
    private static ExecutorService FILTER_EXECUTOR = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setDaemon(true);
                    th.setName("Check Availability Thread");
                    return th;
                }
            });
    private static Task<Availability> CHECK_AVAIL_TASK;
    
    private Popover popover;
    private final ScrollPane scrollPane;
    private final Node content;
    private boolean showBack;
    private BooleanProperty full = new SimpleBooleanProperty(this, "full", true);

    public EventPopoverPage(final DataService dataService, final Event event, boolean showBack) {
        this.showBack = showBack;
        getStyleClass().add("event-popover-page");

        final Session session = event.getSession();

        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        content = session == null ? new EventContent(event, dataService) : new SessionContent(session, event, dataService);
        scrollPane.setContent(content);
        getChildren().add(scrollPane);

        if (CHECK_AVAIL_TASK != null) {
            CHECK_AVAIL_TASK.cancel();
        }

        if (!ConferenceScheduleApp.getInstance().getSessionManagement().isGuestProperty().get() &&
                content instanceof SessionContent) {
            CHECK_AVAIL_TASK = dataService.checkAvailability(event);
            CHECK_AVAIL_TASK.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override public void handle(WorkerStateEvent event) {
                    final SessionContent sc = (SessionContent) content;
                    final Availability avail = CHECK_AVAIL_TASK.getValue();
                    if (!sc.fav) {
                        sc.button.setText(avail.full ? "Session Full" : "Register");
                        full.set(avail.full);
                    }
                }
            });
            FILTER_EXECUTOR.submit(CHECK_AVAIL_TASK);
        }
    }

    @Override public void setPopover(Popover popover) {
        this.popover = popover;
    }

    @Override public Popover getPopover() {
        return popover;
    }

    @Override protected double computePrefHeight(double width) {
        final Insets insets = getInsets();
        return insets.getTop() + content.prefHeight(width) + insets.getBottom();
    }

    @Override protected double computePrefWidth(double height) {
        final Insets insets = getInsets();
        return insets.getLeft() + content.prefWidth(height) + insets.getRight();
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        scrollPane.relocate(insets.getLeft(), insets.getTop());
        scrollPane.resize(
                getWidth() - insets.getLeft() - insets.getRight(),
                getHeight() - insets.getTop() - insets.getBottom());
    }

    @Override public Node getPageNode() {
        return this;
    }

    @Override public String getPageTitle() {
        return "Event";
    }

    @Override public String leftButtonText() {
        return showBack ? "Back" : null;
    }

    @Override public void handleLeftButton() {
        popover.popPage();
    }

    @Override public String rightButtonText() {
        return "Done";
    }

    @Override public void handleRightButton() {
        popover.hide();
    }

    @Override public void handleShown() { }
    @Override public void handleHidden() { }

    private class Content extends Region {
        static final double GAP = 12;
        Text eventTitle;
        Text item1Label;
        Text item1Value;
        Text item2Label;
        Text item2Value;
        Text item3Label;
        Text item3Value;
        Text desc;
        Button button;

        public Content() {
            setPadding(new Insets(24));
        }

        // The content is biased such that the height depends on the width
        @Override public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override protected double computePrefWidth(double height) {
            // We're just going to ignore the height parameter, because the
            // content is width-biased.
            return 400;
        }

        @Override protected double computePrefHeight(double width) {
            if (width == -1) width = 400;
            final Insets insets = getInsets();
            width = width - insets.getLeft() - insets.getRight();

            double h = eventTitle.prefHeight(width) + GAP;

            final double labelWidth = computeLabelWidth();
            final double valueWidth = width - labelWidth - GAP;

            // Add the heights of the 3 values, plus the 12 pixel gap between them
            h += item1Value.prefHeight(valueWidth);

            if (item2Value != null) {
                h += GAP;
                h += item2Value.prefHeight(valueWidth);
            }

            if (item3Value != null) {
                h += GAP;
                h += item3Value.prefHeight(valueWidth);
            }

            // Add the height of the desc plus padding
            if (desc != null) {
                h += GAP;
                h += desc.prefHeight(width);
            }

            // Add the height of the button plus padding
            if (button != null) {
                h += GAP;
                h += button.prefHeight(width);
            }

            return insets.getTop() + h + insets.getBottom();
        }

        @Override protected void layoutChildren() {
            final Insets insets = getInsets();
            final double left = insets.getLeft();
            final double width = getWidth() - left - insets.getRight();
            final double labelWidth = computeLabelWidth();
            final double valueWidth = width - labelWidth - GAP;
            final double valueStartX = left + labelWidth + GAP;

            double y = insets.getTop();
            double labelHeight = eventTitle.prefHeight(width);
            eventTitle.resizeRelocate(left, y, width, labelHeight);
            y += labelHeight + GAP;

            labelHeight = item1Label.prefHeight(-1);
            double valueHeight = item1Value.prefHeight(valueWidth);
            item1Value.resizeRelocate(valueStartX, y, valueWidth, valueHeight);
            item1Label.resizeRelocate(left, y + ((valueHeight - labelHeight) / 2), labelWidth, labelHeight);

            if (item2Label != null) {
                y += valueHeight + GAP;
                labelHeight = item2Label.prefHeight(-1);
                valueHeight = item2Value.prefHeight(valueWidth);
                item2Value.resizeRelocate(valueStartX, y, valueWidth, valueHeight);
                item2Label.resizeRelocate(left, y + ((valueHeight - labelHeight) / 2), labelWidth, labelHeight);
            }

            if (item3Label != null) {
                y += valueHeight + GAP;
                labelHeight = item3Label.prefHeight(-1);
                valueHeight = item3Value.prefHeight(valueWidth);
                item3Value.resizeRelocate(valueStartX, y, valueWidth, valueHeight);
                item3Label.resizeRelocate(left, y + ((valueHeight - labelHeight) / 2), labelWidth, labelHeight);
            }

            if (desc != null) {
                y += valueHeight + GAP;
                valueHeight = desc.prefHeight(width);
                desc.resizeRelocate(left, y, width, valueHeight);
            }

            if (button != null) {
                y += valueHeight + GAP;
                valueHeight = button.prefHeight(width);
                button.resizeRelocate(left, y, width, valueHeight);
            }
        }

        private double computeLabelWidth() {
            double labelWidth = item1Label.prefWidth(-1); // single row of text only
            if (item2Label != null) {
                labelWidth = Math.max(labelWidth, item2Label.prefWidth(-1));
            }
            if (item3Label != null) {
                labelWidth = Math.max(labelWidth, item3Label.prefWidth(-1));
            }
            return labelWidth;
        }
    }

    private final class SessionContent extends Content {
        private boolean fav;
        public SessionContent(Session session, Event event, DataService dataService) {
            // event title
            eventTitle = new ResizableWrappingText(session.getTitle());
            eventTitle.setFont(BASE_FONT);
            eventTitle.setFill(Color.WHITE);

            item1Label = new Text(session == null ? "Time" : "Speakers");
            item1Label.setFill(VLIGHT_GRAY);
            item1Label.setFont(BASE_FONT);

            item1Value = new ResizableWrappingText(session.getSpeakersDisplay());
            item1Value.setFill(Color.WHITE);
            item1Value.setFont(LIGHT_FONT);

            item2Label = new Text("Time");
            item2Label.setFill(VLIGHT_GRAY);
            item2Label.setFont(BASE_FONT);

            item2Value = new ResizableWrappingText(
                    DAY_TIME_FORMAT.format(event.getStart())+ " to "+ TIME_FORMAT.format(event.getEnd()));
            item2Value.setFill(Color.WHITE);
            item2Value.setFont(LIGHT_FONT);

            item3Label = new Text("Location");
            item3Label.setFill(VLIGHT_GRAY);
            item3Label.setFont(BASE_FONT);

            item3Value = new ResizableWrappingText(session.getSessionTimes()[0].getRoom().toString());
            item3Value.setFill(Color.WHITE);
            item3Value.setFont(LIGHT_FONT);

            // description
            desc = new ResizableWrappingText(session.getSummary());
            desc.setFill(Color.WHITE);
            desc.setFont(LIGHT_FONT);

            // Button is either a delete button or a favorite button
            fav = event.getSessionTime().getEvent() != null;
            button = fav ? new DeleteButton(event, dataService) : new AddButton(event, dataService);

            getChildren().addAll(eventTitle, item1Label, item1Value, item2Label, item2Value,
                                 item3Label, item3Value, desc, button);
        }
    }

    private final class EventContent extends Content {
        public EventContent(Event event, DataService dataService) {
            // event title
            eventTitle = new ResizableWrappingText(event.getTitle());
            eventTitle.setFont(BASE_FONT);
            eventTitle.setFill(Color.WHITE);

            item1Label = new Text("Time");
            item1Label.setFill(VLIGHT_GRAY);
            item1Label.setFont(BASE_FONT);

            item1Value = new ResizableWrappingText(
                    DAY_TIME_FORMAT.format(event.getStart())+ " to "+ TIME_FORMAT.format(event.getEnd()));
            item1Value.setFill(Color.WHITE);
            item1Value.setFont(LIGHT_FONT);

            getChildren().addAll(eventTitle, item1Label, item1Value);

            final String organizer = event.getOrganizer();
            if (organizer != null && !"null".equals(organizer)) {
                item2Label = new Text("Organizer");
                item2Label.setFill(VLIGHT_GRAY);
                item2Label.setFont(BASE_FONT);

                item2Value = new ResizableWrappingText(organizer);
                item2Value.setFill(Color.WHITE);
                item2Value.setFont(LIGHT_FONT);

                getChildren().addAll(item2Label, item2Value);
            }

            final String location = event.getLocation();
            if (location != null && !"null".equals(location)) {
                item3Label = new Text("Location");
                item3Label.setFill(VLIGHT_GRAY);
                item3Label.setFont(BASE_FONT);

                item3Value = new ResizableWrappingText(location);
                item3Value.setFill(Color.WHITE);
                item3Value.setFont(LIGHT_FONT);

                getChildren().addAll(item3Label, item3Value);
            }
        }
    }

    private class AddButton extends Button {
        public AddButton(final Event event, final DataService dataService) {
            super(ConferenceScheduleApp.getInstance().getSessionManagement().isGuestProperty().get() ? "Login to Register" : "Checking Availability");
            setPrefWidth(Double.MAX_VALUE);
            // TODO we shouldn't allow anybody to register for a session which is already
            // in the past.
            disableProperty().bind(ConferenceScheduleApp.getInstance().getSessionManagement().isGuestProperty().or(full));
            getStyleClass().setAll("large-blue-button");
            setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent t) {
                    final Task<Void> task = dataService.register(event);
                    final ProgressIndicator pi = new ProgressIndicator();
                    pi.setPrefSize(16, 16);
                    setGraphic(pi);
                    task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override public void handle(WorkerStateEvent workerStateEvent) {
                            if (PlatformIntegration.supportsSystemCalendar()) {
                                PlatformIntegration.writeEvents(Collections.singletonList(event));
                            }
                            // mark sessionTime as saved
                            event.getSessionTime().setEvent(event);
                            // insert new
                            boolean added = false;
                            final long start = event.getStart().getTime();
                            final ObservableList<Event> events = dataService.getEvents();
                            for (int i = 0; i < events.size(); i++) {
                                final Event e = events.get(i);
                                if (e.getStart().getTime() > start) {
                                    System.out.println("        Adding event before [" + i + "]  " + e.getTitle());
                                    events.add(i, event);
                                    added = true;
                                    break;
                                }
                            }
                            System.out.println("added = " + added);
                            if (!added) { // no events are after so just append to end
                                events.add(event);
                            }
                            popover.hide();
                            setGraphic(null);
                        }
                    });
                    task.setOnFailed(new EventHandler<WorkerStateEvent>() {
                        @Override public void handle(WorkerStateEvent workerStateEvent) {
                            Throwable th = task.getException();
                            if (th != null) th.printStackTrace();
                            setGraphic(null);
                        }
                    });
                    new Thread(task).start();
                }
            });
        }
    }

    private class DeleteButton extends Button {
        public DeleteButton(final Event event, final DataService dataService) {
            super("Unregister");
            setPrefWidth(Double.MAX_VALUE);
            disableProperty().bind(ConferenceScheduleApp.getInstance().getSessionManagement().isGuestProperty());
            getStyleClass().setAll("large-red-button");
            setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent t) {
                    final Task<Void> task = dataService.unregister(event);
                    final ProgressIndicator pi = new ProgressIndicator();
                    pi.setPrefSize(16, 16);
                    setGraphic(pi);
                    task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override public void handle(WorkerStateEvent workerStateEvent) {
                            if (PlatformIntegration.supportsSystemCalendar())
                                PlatformIntegration.deleteEvents(Collections.singletonList(event));
                            event.getSessionTime().setEvent(null);
                            dataService.getEvents().remove(event);
                            popover.hide();
                            setGraphic(null);
                        }
                    });
                    task.setOnFailed(new EventHandler<WorkerStateEvent>() {
                        @Override public void handle(WorkerStateEvent workerStateEvent) {
                            Throwable th = task.getException();
                            if (th != null) th.printStackTrace();
                            setGraphic(null);
                        }
                    });
                    new Thread(task).start();
                }
            });
        }
    }
}
