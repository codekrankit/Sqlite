package com.ankit.sqliteclone.statements;

import com.ankit.sqliteclone.enums.StatementType;
import com.ankit.sqliteclone.model.Row;

public class Statement {
    private final StatementType statementType;
    private Row rowToInsert;

    public Statement(StatementType statementType){
        this.statementType = statementType;
    }

    public StatementType getStatementType(){
        return statementType;
    }

    public Row getRowToInsert(){
        return rowToInsert;
    }

    public void setRowToInsert(Row rowToInsert){
        this.rowToInsert = rowToInsert;
    }

    @Override
    public String toString(){
        if (this.statementType.equals(StatementType.STATEMENT_INSERT)){
            return "This is where we would do an insert.\n";
        } else if(this.statementType.equals(StatementType.STATEMENT_SELECT)){
            return "This is where we would do a select.\n";
        }
        return "Undefined Statement";
    }
}
