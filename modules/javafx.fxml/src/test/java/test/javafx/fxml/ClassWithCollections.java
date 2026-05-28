package test.javafx.fxml;

import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassWithCollections {

    private String id;

    private List<String> list = new ArrayList<>();
    private Set<String> set = new HashSet<>();
    private Map<String, Object> map = new HashMap<>();

    private ObservableList<String> observableList = FXCollections.observableArrayList();
    private ObservableSet<String> observableSet = FXCollections.observableSet();
    private ObservableMap<String, Object> observableMap = FXCollections.observableHashMap();

    private float[] ratios = new float[]{};
    private String[] names = new String[]{};

    public ClassWithCollections() {

    }

    public ClassWithCollections(@NamedArg("id") String id) {
        this.id = id;
    }

    public List<String> getList() {
        return list;
    }

    public Set<String> getSet() {
        return set;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public ObservableList<String> getObservableList() {
        return observableList;
    }

    public ObservableSet<String> getObservableSet() {
        return observableSet;
    }

    public ObservableMap<String, Object> getObservableMap() {
        return observableMap;
    }

    public float[] getRatios() {
        return Arrays.copyOf(ratios, ratios.length);
    }

    public void setRatios(float[] ratios) {
        this.ratios = Arrays.copyOf(ratios, ratios.length);
    }

    public String[] getNames() {
        return Arrays.copyOf(names, names.length);
    }

    public void setNames(String[] names) {
        this.names = Arrays.copyOf(names, names.length);
    }

}
