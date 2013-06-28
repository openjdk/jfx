package com.sun.javafx.scene.control.test;

public class Data {

    protected String name;
    protected long id;
    protected static long last_id = 0;

    public Data(String name) {
        this.name = name;
        id = last_id++;
    }

    public String getData() {
        return name;
    }

    @Override public String toString() {
        return name;
    }

    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (Data.class.isInstance(obj.getClass())) {
            return name.contentEquals(((Data) obj).name);
        }
        return super.equals(obj);
    }

    @Override public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}