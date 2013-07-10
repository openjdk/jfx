/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.io.File;
import java.net.MalformedURLException;
import javafx.beans.value.ObservableValue;
import javafx.scene.web.WebHistory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import java.util.Date;

import java.util.concurrent.Callable;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HistoryTest extends TestBase {
    WebHistory history = getEngine().getHistory();
    
    AtomicBoolean entriesChanged = new AtomicBoolean(false);
    AtomicBoolean titleChanged = new AtomicBoolean(false);
    AtomicBoolean dateChanged = new AtomicBoolean(false);
    AtomicBoolean indexChanged = new AtomicBoolean(false);
    
    @Test public void test() {

        //
        // before history is populated
        //
        submit(new Runnable() {
            public void run() {
                try {
                    history.go(-1);
                    fail("go: IndexOutOfBoundsException is not thrown");                    
                } catch (IndexOutOfBoundsException ex) {}
                try {
                    history.go(1);
                    fail("go: IndexOutOfBoundsException is not thrown");                    
                } catch (IndexOutOfBoundsException ex) {}

                history.setMaxSize(99);
                assertEquals("max size is wrong", history.getMaxSize(), 99);
            }
        });        
        
        // [1*]
        checkLoad(new File("src/test/resources/html/h1.html"), 1, 0, "1");
                        
        // [1, 2*]
        checkLoad(new File("src/test/resources/html/h2.html"), 2, 1, "2");
        
        //
        // check the list update
        //
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>() {
            public void onChanged(ListChangeListener.Change<? extends WebHistory.Entry> c) {
                c.next();
                assertTrue("entries: change is wrong", c.wasAdded());
                assertTrue("entries: size is wrong", c.getAddedSubList().size() == 1);
                history.getEntries().removeListener(this);
                entriesChanged.set(true);
            }
        });        
        
        // [1, 2, 3*]
        checkLoad(new File("src/test/resources/html/h3.html"), 3, 2, "3");
        
        ensureValueChanged(entriesChanged, "entries not changed after load");
        
        //
        // check the title update
        //
        history.getEntries().get(history.getCurrentIndex()).titleProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                assertEquals("entries: old title is wrong", "3", oldValue);
                assertEquals("entries: new title is wrong", "hello", newValue);
                observable.removeListener(this);
                titleChanged.set(true);
            }
        });
        executeScript("document.title='hello'");
        
        ensureValueChanged(titleChanged, "title not changed from JS");

        //
        // check the date & index updates
        //
        history.getEntries().get(history.getCurrentIndex() - 1).lastVisitedDateProperty().addListener(newDateListener());
        
        try { Thread.sleep(150); } catch (Exception e) {} // ensure the next date doesn't fit into the same millisecond

        history.currentIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends java.lang.Number> observable, Number oldValue, Number newValue) {
                assertEquals("currentIndexProperty: old index is wrong", 2, oldValue);
                assertEquals("currentIndexProperty: new index is wrong", 1, newValue);
                observable.removeListener(this);
                indexChanged.set(true);
            }
        });

        submit(new Runnable() {
            public void run() {
                // [1, 2*, 3]
                history.go(-1);
            }
        });
        waitLoadFinished();
        check(new File("src/test/resources/html/h2.html"), 3, 1, "2");
                
        ensureValueChanged(dateChanged, "date not changed after go(-1)");
        ensureValueChanged(indexChanged, "index not changed after go(-1)");
        
        //
        // more go() checks
        //        
        
        submit(new Runnable() {
            public void run() {
                // [1, 2, 3*]
                history.go(1);
            }
        });
        waitLoadFinished();
        check(new File("src/test/resources/html/h3.html"), 3, 2, "3");
        
        submit(new Runnable() {
            public void run() {
                // [1*, 2, 3]
                history.go(-2);
            }
        });
        waitLoadFinished();
        check(new File("src/test/resources/html/h1.html"), 3, 0, "1");
        
        submit(new Runnable() {
            public void run() {
                // [1*, 2, 3]
                history.go(0); // no-op
            }
        });
        
        submit(new Runnable() {
            public void run() {
                // [1*, 2, 3]
                try {
                    history.go(-1);
                    fail("go: IndexOutOfBoundsException is not thrown");                    
                } catch (IndexOutOfBoundsException ex) {}
            }
        });
        
        submit(new Runnable() {
            public void run() {
                // [1, 2, 3*]
                history.go(2);
            }
        });
        waitLoadFinished();
        check(new File("src/test/resources/html/h3.html"), 3, 2, "3");
        
        submit(new Runnable() {
            public void run() {
                // [1, 2, 3*]
                try {
                    history.go(1);
                    fail("go: IndexOutOfBoundsException is not thrown");
                } catch (IndexOutOfBoundsException ex) {}
            }
        });
        
        //
        // check the maxSize
        //
        
        submit(new Runnable() {
            public void run() {
                // [1, 2, 3*]
                history.setMaxSize(3);
            }
        });
        // [2, 3, 1*]
        checkLoad(new File("src/test/resources/html/h1.html"), 3, 2, "1");
        
        submit(new Runnable() {
            public void run() {
                // [2, 3*]
                history.setMaxSize(2);
                assertEquals("entries: size is wrong", 2, history.getEntries().size());
                assertEquals("entries: title is wrong", "2", history.getEntries().get(0).getTitle());
            }
        });
        
        submit(new Runnable() {
            public void run() {
                // [2, 3*]
                history.setMaxSize(3);
                // [2*, 3]
                history.go(-1);
            }
        });
        waitLoadFinished();
        
        // [2, 1*]
        checkLoad(new File("src/test/resources/html/h1.html"), 2, 1, "1");
        // [2, 1, 3*]
        checkLoad(new File("src/test/resources/html/h3.html"), 3, 2, "3");
        
        submit(new Runnable() {
            public void run() {
                // [2*, 1, 3]
                history.go(-2);
            }
        });
        waitLoadFinished();
        
        //
        // check for load in-beetwen
        //
        
        // [2, 3*]
        checkLoad(new File("src/test/resources/html/h3.html"), 2, 1, "3");

        //
        // check the date update on reload
        //
        
        // [2, 3, 4*]
        load(new File("src/test/resources/html/h4.html"));
        
        history.getEntries().get(history.getCurrentIndex()).lastVisitedDateProperty().addListener(newDateListener());
        
        try { Thread.sleep(150); } catch (Exception e) {} // ensure the next date doesn't fit into the same millisecond
        
        reload();
        
        ensureValueChanged(dateChanged, "date not changed after reload");
        
        //
        // finally, check zero and invalid maxSize
        //
        
        submit(new Runnable() {
            public void run() {
                // []
                history.setMaxSize(0);
                assertEquals("maxSizeProperty: wrong value", 0, history.getEntries().size());
                
                // []
                try {
                    history.maxSizeProperty().set(-1);
                    fail("maxSizeProperty: IllegalArgumentException is not thrown");
                } catch (IllegalArgumentException ex) {}
            }
        });
    }
    
    void checkLoad(File file, int size, int index, String title) {        
        load(file);
        check(file, size, index, title);
    }
    
    void check(File file, int size, int index, String title) {
        assertEquals("entries: size is wrong", size, history.getEntries().size());
        assertEquals("currentIndex: index is wrong", index, history.getCurrentIndex());
        assertEquals("entries: url is wrong", file.toURI().toString(), history.getEntries().get(index).getUrl());
        assertEquals("entries: title is wrong", title, history.getEntries().get(index).getTitle());
    }
    
    void ensureValueChanged(AtomicBoolean value, String errMsg) {
        if (!value.compareAndSet(true, false)) {
            fail(errMsg);
        }
    }
    
    ChangeListener newDateListener() {
        return new ChangeListener<Date>() {
            long startTime = System.currentTimeMillis();
            
            public void changed(ObservableValue<? extends Date> observable, Date oldValue, Date newValue) {
                long curTime = System.currentTimeMillis();
                
                if (newValue.before(oldValue) ||
                    newValue.getTime() < startTime ||
                    newValue.getTime() > curTime)
                {
                    System.out.println("oldValue=" + oldValue.getTime() +
                                       ", newValue=" + newValue.getTime() +
                                       ", startTime=" + startTime +
                                       ", curTime=" + curTime);
                    
                    fail("entries: date is wrong");
                }
                observable.removeListener(this);
                dateChanged.set(true);
            }
        };

    }
}
