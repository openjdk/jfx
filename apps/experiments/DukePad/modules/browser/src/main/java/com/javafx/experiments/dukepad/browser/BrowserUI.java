/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.browser;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Browser UI
 */
public class BrowserUI extends VBox {

    private final TabPane tabPane = new TabPane();
    private final TextField addressField = new TextField();
    private WebView webView = null;
    private HashMap<Tab, String> tabsData = new HashMap<>();
    private int count = 0;

    public BrowserUI() {
        try {
            webView = new WebView();
        } catch (Throwable e) {
            e.printStackTrace();
            webView = null;
        }
        System.out.println("Ok I Made it past webView = "+webView);

        // make buttons
        Button back = new Button("<");
        back.getStyleClass().add("left-pill");
        Button forward = new Button(">");
        forward.getStyleClass().add("right-pill");
        Button refresh = new Button("Refresh");

        //make textfield
        HBox.setHgrow(addressField, Priority.ALWAYS); // because Toolbar is made from a HBox
        HBox.setMargin(addressField, new Insets(0, 5, 0, 5));
        addressField.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                tabsData.put(selectedTab, addressField.getText());
//                loadURL( tabsData.get(selectedTab) );
                loadURL(addressField.getText());
            }
        });

        // make the ToolBar
        ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(back, forward, addressField, refresh);

        // add the + tab that makes fresh tabs
        final Tab freshTab = new Tab("+");
        freshTab.setClosable(false);
        tabPane.getTabs().add(freshTab);

        // add a listener that detects when the tab selection changes
        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override public void changed(ObservableValue<? extends Tab> observableValue, Tab oldSelectedTab, Tab newSelectedTab) {
                if (newSelectedTab == freshTab) {
                    makeFreshTab();
                } else {
                    switchTab(newSelectedTab);
                }
            }
        });

        // add the nodes to the root VBox
        this.getChildren().addAll(toolBar, tabPane);
        if (webView != null) {
            VBox.setVgrow(webView, Priority.ALWAYS); // make the webview fill the vertical space
            this.getChildren().add(webView);
        } else {
            back.setDisable(true);
            forward.setDisable(true);
            addressField.setDisable(true);
            StackPane spacer = new StackPane();
            VBox.setVgrow(spacer, Priority.ALWAYS); // make the spacer fill the vertical space
            this.getChildren().add(spacer);
        }

        // todo - read prefs to get previous list of tabs else make a single fresh one
        makeFreshTab();
    }

    private void makeFreshTab() {
        System.out.println( (count++) + " In BrowserUI.makeFreshTab, tabPane.getTabs().size(): " + tabPane.getTabs().size() );
        Tab tab = new Tab("Untitled");

        tab.setOnClosed(new EventHandler<Event>() {
            @Override public void handle(Event event) {
                closeTab( (Tab) event.getSource());
            }
        });

        tabsData.put(tab, "");
        int newTabIndex = tabPane.getTabs().size() - 1;
        tabPane.getTabs().add(newTabIndex, tab);
        tabPane.getSelectionModel().select(tab);
    }

    private void switchTab( Tab tab ) {
        System.out.println( (count++) + " In BrowserUI.switchTab" );
        loadURL(tabsData.get(tab));
        addressField.setText(tabsData.get(tab));
    }

    private void closeTab( Tab tab ) {
        System.out.println( (count++) + " In BrowserUI.closeTab" );
        tabsData.remove(tab);
    }

    private void loadURL( String address ) {
        System.out.println( (count++) + " In BrowserUI.addressFieldInput");
        // Validate url else use it as a search string
        if ( !address.isEmpty() ) {
            if ( !URL_MATCH.matcher(address).find() ) {
                try {
                    address = new URI("http", null, "www.google.com", 80,"/search", "q="+address, null).toString();
                } catch (URISyntaxException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else if ( !address.matches("^http(s)?://.*") ) {
                address = "http://" + address;
            }
        }
        WebEngine webEngine = webView.getEngine();
        webEngine.load(address);
    }


    public static final Pattern URL_MATCH = Pattern.compile(".*(.AC|\\.AD|\\.AE|\\.AERO|\\.AF|\\.AG|\\.AI|\\.AL|\\.AM|\\.AN|\\.AO|\\.AQ|\\.AR|\\.ARPA|\\.AS|\\.ASIA|\\.AT|\\.AU|" +
            "\\.AW|\\.AX|\\.AZ|\\.BA|\\.BB|\\.BD|\\.BE|\\.BF|\\.BG|\\.BH|\\.BI|\\.BIZ|\\.BJ|\\.BM|\\.BN|\\.BO|\\.BR|\\.BS|\\.BT|\\.BV|\\.BW|\\.BY|\\.BZ|" +
            "\\.CA|\\.CAT|\\.CC|\\.CD|\\.CF|\\.CG|\\.CH|\\.CI|\\.CK|\\.CL|\\.CM|\\.CN|\\.CO|\\.COM|\\.COOP|\\.CR|\\.CU|\\.CV|\\.CW|\\.CX|\\.CY|\\.CZ|\\.DE|" +
            "\\.DJ|\\.DK|\\.DM|\\.DO|\\.DZ|\\.EC|\\.EDU|\\.EE|\\.EG|\\.ER|\\.ES|\\.ET|\\.EU|\\.FI|\\.FJ|\\.FK|\\.FM|\\.FO|\\.FR|\\.GA|\\.GB|\\.GD|\\.GE|" +
            "\\.GF|\\.GG|\\.GH|\\.GI|\\.GL|\\.GM|\\.GN|\\.GOV|\\.GP|\\.GQ|\\.GR|\\.GS|\\.GT|\\.GU|\\.GW|\\.GY|\\.HK|\\.HM|\\.HN|\\.HR|\\.HT|\\.HU|\\.ID|" +
            "\\.IE|\\.IL|\\.IM|\\.IN|\\.INFO|\\.INT|\\.IO|\\.IQ|\\.IR|\\.IS|\\.IT|\\.JE|\\.JM|\\.JO|\\.JOBS|\\.JP|\\.KE|\\.KG|\\.KH|\\.KI|\\.KM|\\.KN|\\.KP|" +
            "\\.KR|\\.KW|\\.KY|\\.KZ|\\.LA|\\.LB|\\.LC|\\.LI|\\.LK|\\.LR|\\.LS|\\.LT|\\.LU|\\.LV|\\.LY|\\.MA|\\.MC|\\.MD|\\.ME|\\.MG|\\.MH|\\.MIL|\\.MK|" +
            "\\.ML|\\.MM|\\.MN|\\.MO|\\.MOBI|\\.MP|\\.MQ|\\.MR|\\.MS|\\.MT|\\.MU|\\.MUSEUM|\\.MV|\\.MW|\\.MX|\\.MY|\\.MZ|\\.NA|\\.NAME|\\.NC|\\.NE|\\.NET|" +
            "\\.NF|\\.NG|\\.NI|\\.NL|\\.NO|\\.NP|\\.NR|\\.NU|\\.NZ|\\.OM|\\.ORG|\\.PA|\\.PE|\\.PF|\\.PG|\\.PH|\\.PK|\\.PL|\\.PM|\\.PN|\\.POST|\\.PR|\\.PRO|" +
            "\\.PS|\\.PT|\\.PW|\\.PY|\\.QA|\\.RE|\\.RO|\\.RS|\\.RU|\\.RW|\\.SA|\\.SB|\\.SC|\\.SD|\\.SE|\\.SG|\\.SH|\\.SI|\\.SJ|\\.SK|\\.SL|\\.SM|\\.SN|" +
            "\\.SO|\\.SR|\\.ST|\\.SU|\\.SV|\\.SX|\\.SY|\\.SZ|\\.TC|\\.TD|\\.TEL|\\.TF|\\.TG|\\.TH|\\.TJ|\\.TK|\\.TL|\\.TM|\\.TN|\\.TO|\\.TP|\\.TR|\\.TRAVEL|" +
            "\\.TT|\\.TV|\\.TW|\\.TZ|\\.UA|\\.UG|\\.UK|\\.US|\\.UY|\\.UZ|\\.VA|\\.VC|\\.VE|\\.VG|\\.VI|\\.VN|\\.VU|\\.WF|\\.WS|\\.XXX|\\.YE|\\.YT|\\.ZA|" +
            "\\.ZM|\\.ZW)([?/].*)?$",Pattern.CASE_INSENSITIVE);


}
