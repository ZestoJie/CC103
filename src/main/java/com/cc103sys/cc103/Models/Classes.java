package com.cc103sys.cc103.Models;

public class Classes {

    private int id;
    private String className;
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
    public void setId(int id) { this.id = id; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }

    @Override
    public String toString() {
        return className;
    }
}