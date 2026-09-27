package com.ankit.sqliteclone.storage;

import com.ankit.sqliteclone.constants.Constants;

public class Page {
    private final byte[] data;
    public Page() {
        this.data = new byte[Constants.PAGE_SIZE];
    }

    public byte[] getData() {
        return data;
    }
}
