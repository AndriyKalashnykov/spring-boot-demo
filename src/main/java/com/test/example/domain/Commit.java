package com.test.example.domain;

public class Commit {

    private String id;
    private String message;
    private String branch;

    public Commit(String id, String message, String branch) {
        this.id = id;
        this.message = message;
        this.branch = branch;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

}
