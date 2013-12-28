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
package com.javafx.experiments.scheduleapp.pages;

import com.javafx.experiments.scheduleapp.control.EventPopoverPage;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.PopoverTreeList;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import java.util.Calendar;
import java.util.List;
import javafx.scene.Node;

public class SessionListPage extends PopoverTreeList<SessionTime> implements Popover.Page {
    private Popover popover;
    private DataService dataService;
    
    public SessionListPage(List<SessionTime> sessionTimes, DataService dataService) {
        this.dataService = dataService;
        getItems().addAll(sessionTimes);
    }

    @Override protected void itemClicked(SessionTime newSession) {
        if (newSession != null) {
            // get/create event
            Session session = newSession.getSession();
            com.javafx.experiments.scheduleapp.model.Event event = newSession.getEvent();
            if (event == null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(newSession.getStart());
                calendar.add(Calendar.MINUTE, newSession.getLength());
                event = new com.javafx.experiments.scheduleapp.model.Event(session, newSession.getStart(), calendar.getTime(), newSession);
            }
            // go to event page
            popover.pushPage(new EventPopoverPage(dataService, event, true));
            // clear selection
            getSelectionModel().clearSelection();
        }
    }

    @Override public void setPopover(Popover popover) {
        this.popover = popover;
    }

    @Override public Popover getPopover() {
        return popover;
    }

    @Override public Node getPageNode() {
        return this;
    }

    @Override public String getPageTitle() {
        return "Sessions";
    }

    @Override public String leftButtonText() {
        return null;
    }

    @Override public void handleLeftButton() {
    }

    @Override public String rightButtonText() {
        return "Done";
    }

    @Override public void handleRightButton() {
        popover.hide();
    }

    @Override public void handleShown() { }
    @Override public void handleHidden() { }
}
