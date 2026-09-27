package com.ankit.sqliteclone.model;

public class Row {
    private final int id;
    private final String email;
    private final String username;

    public Row(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
}
