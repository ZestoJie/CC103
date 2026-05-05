package com.cc103sys.cc103.Models;

public class ClassItem {
    private final int classId;
    private final String className;

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