package com.sun.javafx.fxml.builder;

import com.sun.javafx.collections.TrackableObservableList;
import javafx.beans.NamedArg;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by msladecek on 22.7.14.
 */
public class ClassWithReadOnlyCollection {
    public double a;
    ObservableList<Integer> propertyList = new TrackableObservableList<Integer>() {
        @Override
        protected void onChanged(ListChangeListener.Change<Integer> c) {}
    };

    public ClassWithReadOnlyCollection() {

    }

    public ClassWithReadOnlyCollection(@NamedArg("a") double a) {
        this.a = a;
    }

    public List<Integer> getPropertyList() {
        return propertyList;
    }
}
