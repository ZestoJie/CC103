package com.cc103sys.cc103.Models;

public class ClassItem {
    private int classId;
    private String className;

    public ClassItem(int classId, String className) {
        this.classId = classId;
        this.className = className;
    }

    public int getClassId() {
        return classId;
    }

    @Override
    public String toString() {
        return className;
    }
}