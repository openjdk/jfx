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

import com.javafx.experiments.scheduleapp.data.JSONParserJP;
import com.javafx.experiments.scheduleapp.model.Event;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.concurrent.Task;
import javafx.util.Pair;

class UpdateScheduleTask extends Task<Void> {
    private final Event[] events;
    private final String userName;
    private final String password;

    UpdateScheduleTask(Event[] events,String userName, String password) {
        this.events = events;
        this.userName = userName;
        this.password = password;
    }

    @Override protected Void call() throws Exception {
        System.out.println("UpdateScheduleTask events = "+Arrays.toString(events));
        
        final StringBuilder urlParameters = new StringBuilder();
        urlParameters.append("code=");
        urlParameters.append(password);
        for (Event event: events) {
            if (event.getSession() != null) {
                urlParameters.append("&favorites=");
                urlParameters.append(Integer.toString(event.getSession().getId()));
            }
        }
        System.out.println("urlParameters = " + urlParameters);

        final URL urlObj = new URL("http://cfp.devoxx.com/rest/v1/events/7/schedule/"+userName);
        HttpURLConnection connection = (HttpURLConnection)urlObj.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.toString().getBytes().length));
        connection.setRequestProperty("Content-Language", "en-US");  
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        try (DataOutputStream out = new DataOutputStream (connection.getOutputStream ())) {
            out.writeBytes(urlParameters.toString());
            out.flush();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw exception;
        }
        System.out.println("====== RESPONSE "+connection.getResponseCode()+" ["+connection.getResponseMessage()+"] =========");
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            System.err.println("====== ERROR WITH HTTP POST TO ["+urlObj+"] -- params="+urlParameters+" =========");
            System.err.println("====== RESPONSE "+connection.getResponseCode()+" ["+connection.getResponseMessage()+"] =========");
            try (InputStream in = connection.getErrorStream()) {
                //Get Response	
                BufferedReader rd = new BufferedReader(new InputStreamReader(in));
                String line;
                StringBuilder response = new StringBuilder(); 
                while((line = rd.readLine()) != null) {
                  response.append(line);
                  response.append('\r');
                }
                System.err.println("RESPONSE:\n"+response);
            }
        }
        return null;
    }
}
