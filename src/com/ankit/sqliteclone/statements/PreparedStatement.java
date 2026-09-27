package com.ankit.sqliteclone.statements;

import com.ankit.sqliteclone.enums.PrepareResult;

public class PreparedStatement {
    private PrepareResult prepareResult;
    private Statement statement;
    public PreparedStatement(PrepareResult prepareResult, Statement statement){
        this.prepareResult = prepareResult;
        this.statement = statement;
    }

    public PrepareResult getPrepareResult(){
        return prepareResult;
    }
    public Statement getStatement(){
        return statement;
    }
}
