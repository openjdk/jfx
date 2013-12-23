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

import static com.javafx.experiments.scheduleapp.ConferenceScheduleApp.*;
import com.javafx.experiments.scheduleapp.TouchClickedEventAvoider;
import com.javafx.experiments.scheduleapp.Page;
import static com.javafx.experiments.scheduleapp.Theme.*;
import com.javafx.experiments.scheduleapp.control.EventPopoverPage;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.SearchBox;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import com.javafx.experiments.scheduleapp.model.SessionType;
import com.javafx.experiments.scheduleapp.model.Track;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Callback;

public class CatalogPage extends Page  implements Callback<ListView<CatalogPage.Row>, ListCell<CatalogPage.Row>>, Runnable, ChangeListener<String>  {
    private static DateFormat TIME_FORMAT = new SimpleDateFormat("hh:mma");
    private static DateFormat DAY_FORMAT = new SimpleDateFormat("EEEE");
    private static final int TIME_COLUMN_WIDTH = 100;
    private static final int SLOT_WIDTH = 200;
    private static final int SLOT_HEIGHT = 85;
    private static final int SLOT_BAR_WIDTH = 5;
    private static final int SLOT_BAR_GAP = 5;
    private static final int SLOT_GAP = 5;
    private static final int SLOT_TEXT_WRAP = SLOT_WIDTH - SLOT_BAR_WIDTH - SLOT_BAR_GAP - SLOT_GAP;
    private static final Font TITLE_FONT = BASE_FONT;
    private static final Color TITLE_COLOR = DARK_GREY;
    private static final Font SPEAKERS_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.BOLD, 12);
    private static final Color SPEAKERS_COLOR = Color.web("#5f5f5f");
    private static final Font TIME_FONT = LARGE_FONT;
    private static final Font DAY_FONT = BASE_FONT;
    private static final int MAX_TITLE_CHARS = 55;
    private static final int MAX_SPEAKER_CHARS = 50;

    /**
     * The ExecutorService used for running our filter tasks. We could have just created
     * a new thread each time, but there really isn't a need for it. In addition, by having
     * a single thread executor, we can be sure that no two tasks are operating at the
     * same time, which determinism makes it easier for us to efficiently handle updating
     * the state of the UI thread during filtering.
     */
    private static ExecutorService FILTER_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread th = new Thread(r);
            th.setDaemon(true);
            th.setName("Catalog Filter Thread");
            return th;
        }
    });

    /**
     * The ListView of all of the sessions at the conference. Each row is
     * comprised first of a Date, followed by Sessions that are associated
     * with the Date / time.
     */
    private final ListView<Row> list;

    /**
     * This button will pop up the session filter popover. The session filter differs
     * from the filter bar in that it allows you to filter out entire
     * categories of issues, whereas the "search bar" (really a filter bar) is
     * based on the text in the sessions (summary, title, speakers).
     */
    private final Button sessionFilterButton;

    /**
     * The session filter is used to store the results of the session filter pane,
     * and can filter out sessions passed to it.
     */
    private SessionFilterCriteria sessionFilter = SessionFilterCriteria.EMPTY;

    /**
     * The data items used in the ListView.
     */
    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    /**
     * A list of Rows that have been filtered out of the <code>rows</code>. That is,
     * these rows are no longer in the list view.
     */
    private final List<Row> filtered = new LinkedList<Row>();

    /**
     * The search box, used for filtering.
     */
    private final SearchBox searchBox = new SearchBox();

    /**
     * The background task which actually does the filtering. This is moved into a
     * background task so as not to cause delays in the GUI.
     */
    private Task<Runnable> filterTask = null;

    private final HBox searchBar = new HBox(10);

    private final Popover sessionFilterPopover = new Popover();
    private final SearchFilterPopoverPage filterPopoverPage;
    private final Popover popover;

    /**
     * Creats a new catalog filterPopoverPage.
     *
     * @param dataService    The dataservice to use.
     */
    public CatalogPage(final Popover popover, final DataService dataService) {
        super("Content Catalog", dataService);
        this.popover = popover;
        sessionFilterPopover.getStyleClass().add("session-popover");
        sessionFilterPopover.setPrefWidth(440);
        // set pick on bounds to false as we don't want to capture mouse events
        // that are meant for the top tabs even though those tabs are in our bounds
        // because of the filter button
        setPickOnBounds(false);
        // create list
        list = new ListView<Row>(){
            {
                getStyleClass().setAll("twitter-list-view");
                skinClassNameProperty().set("com.sun.javafx.scene.control.skin.ListViewSkin");
                setCellFactory(CatalogPage.this);
            }
        };

        if (IS_BEAGLE) {
            new TouchClickedEventAvoider(list);
        }

        // create filter button
        this.filterPopoverPage = new SearchFilterPopoverPage(dataService, this);
        sessionFilterButton = new Button();
        sessionFilterButton.setId("session-filter-button");
        sessionFilterButton.getStyleClass().clear();
        sessionFilterButton.setPrefSize(69, 31);
        sessionFilterButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                if (sessionFilterPopover.isVisible()) {
                    sessionFilterPopover.hide();
                } else {
                    sessionFilterPopover.pushPage(filterPopoverPage);
                    sessionFilterPopover.show();
                }
            }
        });
        // create search bar
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        searchBar.getChildren().addAll(searchBox, sessionFilterButton);
        searchBox.textProperty().addListener(this);
        searchBox.setFocusTraversable(false);
        // add children
        getChildren().setAll(searchBar, list, sessionFilterPopover);
        // set list to use rows as model
        list.setItems(rows);
        // populate filterPopoverPage with initial data
        buildRows();
        // listen for when session time data changes and rebuild rows
        dataService.getStartTimes().addListener(new ListChangeListener<Date>() {
            @Override public void onChanged(Change<? extends Date> c) {
                buildRows();
                filter();
            }
        });
    }
    
    private void buildRows() {
        final List<Row> items = new ArrayList<>(200);
        for (final Date startTime : dataService.getStartTimes()) {
            final List<Session> s = dataService.getSessionsAtTimeSlot(startTime);
            final List<SessionView> views = new ArrayList<>(s.size());
            for (Session session : s) {
                final SessionView view = new SessionView();
                view.session = session;
                views.add(view);
            }
            final Row row = new Row(startTime, views);
            items.add(row);
        }
        rows.setAll(items);
    }
    
    @Override public void reset() {
        // Cause the session filter to clear out
        sessionFilterPopover.hide();
        filterPopoverPage.reset();
        searchBox.setText("");
        list.scrollTo(0);
    }

    /**
     * Called by the search box whenever the text of the search box has changed.
     * If there is a filterTask already being executed, then we will cancel it.
     * We then create a new filterTask which will process all of the rows and
     * then update the UI with the results of the filter operation.
     */
    @Override public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue) {
        sessionFilter = SessionFilterCriteria.withText(searchBox.getText().toLowerCase(), sessionFilter);
        filter();
    }

    @Override public void run() {
        sessionFilter = SessionFilterCriteria.withText(sessionFilter.getText(), filterPopoverPage.getSessionFilterCriteria());
        filter();
    }

    private void filter() {
        // If there is an existing filter task running, then we need to cancel it.
        if (filterTask != null && filterTask.isRunning()) {
            filterTask.cancel();
        }

        // We will not set lastFilter until we have successfully completed a filter operation
        final SessionFilterCriteria criteria = sessionFilter;
        // The rows that we're going to search through.
        final List<Row> allRows = new ArrayList<Row>();
        allRows.addAll(rows);
        allRows.addAll(filtered);

        // Create a new filterTask
        filterTask = new Task<Runnable>() {
            @Override protected Runnable call() throws Exception {
                // A map of sessions which need to move from being filtered to being unfiltered
                final Map<Row, List<SessionView>> sessionsToRestore = new HashMap<Row, List<SessionView>>();
                // A map of sessions which need to move to being filtered
                final Map<Row, List<SessionView>> sessionsToFilter = new HashMap<Row, List<SessionView>>();
                // The filtering criteria saves as local variables
                final String text = criteria.getText();
                final Set<Track> tracks = criteria.getTracks();
                final Set<SessionType> types = criteria.getSessionTypes();
                // For each row we needed to consider, inspect all of its session views and
                // filter them appropriately.
                for (Row row : allRows) {
                    for (SessionView view : row.sessions) {
                        // If the task has been canceled, then bail. This is done on each iteration
                        // so that we do as little work after the thread has been canceled a possible
                        if (isCancelled()) return null;
                        // If the view is presently filtered, but should no longer be filtered,
                        // then add it to the sessionsToRestore map.
                        final Session s = view.session;
                        final Track t = s.getTrack();
                        final SessionType st = s.getSessionType();

                        final boolean shouldKeep = (s.getTitle().toLowerCase().contains(text) ||
                                s.getSpeakersDisplay().toLowerCase().contains(text) ||
                                s.getSummary().toLowerCase().contains(text) ||
                                s.getAbbreviation().toLowerCase().contains(text)) &&
                                (!(t != null && tracks.contains(t)) &&
                            !(st != null && types.contains(st)));

                        if (view.filtered.get() && shouldKeep) {
                            // We do not need to filter this one any more
                            List<SessionView> views = sessionsToRestore.get(row);
                            if (views == null) {
                                views = new ArrayList<SessionView>();
                                sessionsToRestore.put(row, views);
                            }
                            views.add(view);
                        // If the view is presently not being filtered, but should be, then add
                        // it to the sessionsToFilter map.
                        } else if (!view.filtered.get() && !shouldKeep) {
                            // We need to filter this one
                            List<SessionView> views = sessionsToFilter.get(row);
                            if (views == null) {
                                views = new ArrayList<SessionView>();
                                sessionsToFilter.put(row, views);
                            }
                            views.add(view);
                        }
                    }
                }

                return new Runnable() {
                    @Override public void run() {
                        // Hide all sessions that have been filtered.
                        for (Map.Entry<Row, List<SessionView>> entry : sessionsToFilter.entrySet()) {
                            final Row row = entry.getKey();
                            final List<SessionView> views = entry.getValue();
                            for (SessionView view : views) {
                                assert view.filtered.get() == false;
                                assert row.numVisible > 0;

                                row.numVisible--;
                                view.filtered.set(true);

                                if (row.numVisible == 0) {
                                    filtered.add(row);
                                    rows.remove(row);
                                }
                            }
                        }

                        // Restore all the sessions that have been restored.
                        for (Map.Entry<Row, List<SessionView>> entry : sessionsToRestore.entrySet()) {
                            final Row row = entry.getKey();
                            final List<SessionView> views = entry.getValue();
                            for (SessionView view : views) {
                                assert view.filtered.get() == true;
                                assert row.numVisible >= 0;

                                if (row.numVisible == 0) {
                                    filtered.remove(row);
                                    rows.add(row);
                                }

                                row.numVisible++;
                                view.filtered.set(false);
                            }
                        }

                        // Resort the rows so that the dates are all correct
                        FXCollections.sort(rows, new Comparator<Row>() {
                            @Override public int compare(Row o1, Row o2) {
                                return o1.date.compareTo(o2.date);
                            }
                        });
                    }
                };
            }
        };
        filterTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent event) {
                assert !filterTask.isCancelled();
                Runnable r = filterTask.getValue();
                if (r != null) {
                    filterTask.getValue().run();
                }
            }
        });
        filterTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override public void handle(WorkerStateEvent event) {
                // Debugging
                event.getSource().getException().printStackTrace();
            }
        });

        // Execute the filterTask on this single-threaded Executor
        FILTER_EXECUTOR.submit(filterTask);
    }

    @Override protected void layoutChildren() {
        final int w = (int)getWidth() - 24;
        final int h = (int)getHeight() - 24;

        sessionFilterPopover.autosize();
        sessionFilterPopover.setLayoutX(w - sessionFilterPopover.prefWidth(-1) + 17);
        sessionFilterPopover.setLayoutY(58);
        searchBar.resize(w,30);
        searchBar.setLayoutX(12);
        searchBar.setLayoutY(12);

        list.resize(w,h - 42);
        list.setLayoutX(12);
        list.setLayoutY(53);
    }

    /**
     * Part of the "View-Model", the row represents a row in the list view.
     * Each row is made up of a Date and a number of "SessionView" objects.
     * Each SessionView is a "View-Model" for a Session.
     */
    public class Row {
        final Date date;
        final List<SessionView> sessions;
        int numVisible;

        Row(Date date, List<SessionView> sessions) {
            this.date = date;
            this.sessions = sessions;
            for (SessionView v : sessions) {
                if (!v.filtered.get()) numVisible++;
            }
        }
    }

    /**
     * The "View-Model" for a Session. This contains a single observable Boolean property
     * called "filtered", and a reference to the session that this view represents.
     */
    class SessionView {
        BooleanProperty filtered = new SimpleBooleanProperty(this, "filtered", false);
        Session session;
    }

    @Override public ListCell<Row> call(ListView<Row> listView) {
        return new TimeSlotListCell();
    }

    static int counter = 0;
    private class TimeSlotListCell extends ListCell<Row> implements Skin<TimeSlotListCell> {
        private Text timeText = new Text();
        private Text dayText = new Text();
        private List<Tile> tiles = new ArrayList<Tile>();
        private String name = "TimeSlotListCell-" + counter++;

        @Override public String toString() { return name; }

        private TimeSlotListCell() {
            super();
            setSkin(this);
            getStyleClass().clear();
            timeText.setFont(TIME_FONT);
            timeText.setFill(BLUE);
            dayText.setFont(DAY_FONT);
            dayText.setFill(BLUE);
            getChildren().add(dayText);
        }

        @Override protected double computePrefWidth(double height) {
            return 330;
        }

        // TODO ListView is broken -- it doesn't support content bias! Workaround: return the width as the pref width
        @Override protected double computePrefHeight(double width) {
            final Row row = getItem();
            if (row == null || row.numVisible == 0) return (getIndex() == 0 ? 5 : 0) + SLOT_HEIGHT + 5;

            // We have a row and it has sessions. We need to figure out how many tiles
            // can be placed in the session layout area per row (right of the time column).
            // That will tell us how many rows of sessions we will have, which then gives us
            // a pref height that we can return.
            if (width == -1) width = getWidth();
            // TODO normally insets need to be taken into account but we're ignoring them
            final double sessionAreaWidth = width - TIME_COLUMN_WIDTH;
            final int tilesPerRow = (int) (sessionAreaWidth / (SLOT_WIDTH + SLOT_GAP));
            if (tilesPerRow == 0) {
                return SLOT_HEIGHT + (getIndex() == 0 ? 10 : 5);
            }
            final int numRows = (int) Math.ceil(row.numVisible / (double) tilesPerRow);
            // TODO did I get this right?
            return (getIndex() == 0 ? 5 : 0) + (numRows > 0 ? (numRows * (SLOT_HEIGHT + SLOT_GAP)) : 5);
        }

        @Override protected void layoutChildren() {
            final int top = (getIndex() == 0 ? 5 : 0);
            timeText.relocate(5, top + 5);
            dayText.relocate(5, top + 30);

            final Row row = getItem();
            final int numTiles = row == null ? 0 : row.numVisible;
            final double width = getWidth();
            final double sessionAreaWidth = width - TIME_COLUMN_WIDTH;
            final int tilesPerRow = (int) (sessionAreaWidth / (SLOT_WIDTH + SLOT_GAP));
            final int numRows = tilesPerRow == 0 ? 0 : (int) Math.ceil(numTiles / (double) tilesPerRow);

            int tileIndex = 0;
            for (int r=0; r<numRows; r++) {
                for (int c=0; c<tilesPerRow && tileIndex < tiles.size();) {
                    Tile tile = tiles.get(tileIndex++);
                    if (tile.isVisible()) {
                        double x = c * (SLOT_WIDTH + SLOT_GAP) + TIME_COLUMN_WIDTH;
                        double y = r * (SLOT_HEIGHT + SLOT_GAP) + 5;
                        tile.resizeRelocate(x, y, SLOT_WIDTH, SLOT_HEIGHT);
                        c++;
                    }
                }
            }
        }

        // CELL METHODS
        @Override protected void updateItem(Row item, boolean empty) {
            super.updateItem(item, empty);

            if (!empty) {
                List<SessionView> views = item.sessions;

                // Unlink all tiles
                for (int i=0; i<tiles.size(); i++) {
                    Tile tile = tiles.get(i);
                    tile.unlink();
                }

                // Add any missing tiles
                for (int i=tiles.size(); i<views.size(); i++) {
                    Tile tile = new Tile(this);
                    tiles.add(tile);
                    getChildren().add(tile);
                }

                // Update all tiles
                for (int i=0; i<views.size(); i++) {
                    SessionView view = views.get(i);
                    Tile tile = tiles.get(i);
                    tile.update(view);
                }

                // update date text
                if (item.date == null) {
                    timeText.setVisible(false);
                    dayText.setVisible(false);
                } else {
                    final Date date = item.date;
                    timeText.setText(TIME_FORMAT.format(date));
                    dayText.setText(DAY_FORMAT.format(date).toUpperCase());
                    timeText.setVisible(true);
                    dayText.setVisible(true);
                }
            } else {
                timeText.setVisible(false);
                dayText.setVisible(false);
                for (Tile tile : tiles) {
                    tile.unlink();
                }
            }
        }

        // SKIN METHODS
        @Override public TimeSlotListCell getSkinnable() { return this; }
        @Override public Node getNode() { return timeText; }
        @Override public void dispose() {}
    }

    class Tile extends Region implements EventHandler<MouseEvent>, InvalidationListener {
        private Text title;
        private Text speaker;
        private Rectangle bar;
        private SessionView view; // Doubly-linked between Tile & SessionView.
        private TimeSlotListCell cell;

        Tile(TimeSlotListCell cell) {
            this.cell = cell;
            title = new Text();
            title.setWrappingWidth(SLOT_TEXT_WRAP);
            title.setFont(TITLE_FONT);
            title.setFill(TITLE_COLOR);
            title.setTextOrigin(VPos.TOP);
            speaker = new Text();
            speaker.setFont(SPEAKERS_FONT);
            speaker.setFill(SPEAKERS_COLOR);
            speaker.setTextOrigin(VPos.TOP);
            bar = new Rectangle(SLOT_BAR_WIDTH, 0);
            setOnMouseClicked(this);
            getChildren().addAll(bar, title, speaker);
        }

        void update(SessionView view) {
            this.view = view;
            view.filtered.addListener(this);
            Session session = view.session;
            String s = session.getAbbreviation() + " :: " + session.getTitle();
            if (s.length() > MAX_TITLE_CHARS) s = s.substring(0, MAX_TITLE_CHARS).trim() + "...";
            title.setText(s);
            s = session.getSpeakersDisplay();
            if (s.length() > MAX_SPEAKER_CHARS) s = s.substring(0, MAX_SPEAKER_CHARS).trim() + "...";
            speaker.setText(s);
            bar.setFill(Color.web(session.getTrack().getColor()));
            setVisible(!view.filtered.get());
        }

        @Override public void invalidated(Observable observable) {
            setVisible(!this.view.filtered.get());
            cell.requestLayout();
        }

        void unlink() {
            if (this.view != null) {
                this.view.filtered.removeListener(this);
            }
            this.view = null;
            setVisible(false);
        }

        @Override protected double computePrefWidth(double height) {
            return SLOT_WIDTH;
        }

        @Override protected double computePrefHeight(double width) {
            return SLOT_HEIGHT;
        }

        @Override protected void layoutChildren() {
            // I'm ignoring the insets
            bar.setHeight(getHeight());

            final double wrappingWidth = getWidth() - SLOT_BAR_WIDTH - SLOT_BAR_GAP;
            title.setWrappingWidth(wrappingWidth);
            speaker.setWrappingWidth(wrappingWidth);

            title.setX(SLOT_BAR_WIDTH + SLOT_BAR_GAP);
            speaker.setX(SLOT_BAR_WIDTH + SLOT_BAR_GAP);
            speaker.setY(title.prefHeight(wrappingWidth));
        }

        @Override public void handle(MouseEvent mouseEvent) {
            // find SessionTime
            if (view == null || view.session == null) return;
            Session session = view.session;
            long startTime = cell.getItem().date.getTime();
            SessionTime sessionTime = null;
            for(SessionTime st : session.getSessionTimes()) {
                if (startTime == st.getStart().getTime()) {
                    sessionTime = st;
                    break;
                }
            }
            // get/create event
            Event event = sessionTime.getEvent();
            if (event == null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(sessionTime.getStart());
                calendar.add(Calendar.MINUTE, sessionTime.getLength());
                event = new Event(session, sessionTime.getStart(), calendar.getTime(), sessionTime);
            }
            // show popover
            EventPopoverPage page = new EventPopoverPage(dataService, event, false);
            popover.clearPages();
            popover.pushPage(page);
            popover.show();
        }
    }
}
