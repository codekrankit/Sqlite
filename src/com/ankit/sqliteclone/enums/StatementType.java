package com.ankit.sqliteclone.enums;

public enum StatementType {
    STATEMENT_INSERT("insert"),
    STATEMENT_SELECT("select")
    ;
    private final String text;
    StatementType(final String text){
        this.text = text;
    }
    @Override
    public String toString(){
        return text;
    }

}
