package com.cc103sys.cc103.Models;

public class Classes {

    private int id;
    private String className;

    public Classes(int id, String className) {
        this.id = id;
        this.className = className;
    }

    public int getId() { return id; }
    public String getClassName() { return className; }

    @Override
    public String toString() {
        return className;
    }
}