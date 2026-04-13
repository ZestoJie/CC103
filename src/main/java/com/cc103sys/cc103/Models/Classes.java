package com.cc103sys.cc103.Models;

public class Classes {

    private final int id;
    private final String className;
    private Integer ownerId;

    public Classes(int id, String className) {
        this.id = id;
        this.className = className;
    }

    public Classes(int id, String className, Integer ownerId) {
        this.id = id;
        this.className = className;
        this.ownerId = ownerId;
    }

    public int getId() { return id; }
    public String getClassName() { return className; }
    public Integer getOwnerId() { return ownerId; }

    @Override
    public String toString() {
        return className;
    }
}