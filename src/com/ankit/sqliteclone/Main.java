package com.ankit.sqliteclone;
import com.ankit.sqliteclone.enums.MetaCommandResult;
import com.ankit.sqliteclone.enums.PrepareResult;
import com.ankit.sqliteclone.enums.StatementType;
import com.ankit.sqliteclone.model.Cursor;
import com.ankit.sqliteclone.model.Row;
import com.ankit.sqliteclone.model.Table;
import com.ankit.sqliteclone.statements.PreparedStatement;
import com.ankit.sqliteclone.statements.Statement;
import com.ankit.sqliteclone.storage.Pager;
import com.ankit.sqliteclone.storage.RowSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import static com.ankit.sqliteclone.enums.PrepareResult.PREPARE_SUCCESS;
import static com.ankit.sqliteclone.enums.PrepareResult.PREPARE_UNRECOGNIZED_STATEMENT;

public class Main{
    private final Table table;
    public Main(){
        try {
            Pager pager = new Pager("test.db");
            table = new Table(pager);
            Runtime.getRuntime().addShutdownHook(new Thread(this.table::close));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
//        try{
//            Pager pager = new Pager("test.db");
//            Table table = new Table(pager);
//            for (int i = 1; i <= 3700; i++) {
//                System.out.println("Inserting " + i);
//
//                table.insert(new Row(
//                        i,
//                        "user" + i,
//                        "user" + i + "@example.com"
//                ));
//            }
//
//            System.out.println("Insertion complete");
//            int[] testKeys = {1, 7, 8, 100, 500, 1000, 2000, 3000, 3500, 3600};
//
//            for (int key : testKeys) {
//                Cursor cursor = table.tableFind(key);
//                Row row = table.cursorValue(cursor);
//
//                System.out.println(
//                        "Found: " + row.getId() +
//                                " -> " + row.getEmail()
//                );
//            }

        // testing read of the saved db.
//            Pager pager = new Pager("test.db");
//            Table table = new Table(pager);
//
//            int[] testKeys = {
//                    1, 7, 8, 100,
//                    500, 1000, 2000,
//                    3000, 3500, 3600
//            };
//
//            for (int key : testKeys) {
//                Cursor cursor = table.tableFind(key);
//                Row row = table.cursorValue(cursor);
//
//                System.out.println(
//                        "Found: " + row.getId() +
//                                " -> " + row.getUsername()
//                );
//            }
//
//            table.close();
//        } catch (Exception e){
//            System.out.println(e.getMessage());
//        }

//        try{
//            Pager pager = new Pager("test.db");
//            Table table = new Table(pager);
//
//            Cursor cursor = table.tableStart();
//
//            int expectedId = 1;
//
//            while (!cursor.isEndOfTable()) {
//
//                Row row = table.cursorValue(cursor);
//
//                if (row.getId() != expectedId) {
//                    throw new RuntimeException(
//                            "Expected " + expectedId +
//                                    " but found " + row.getId()
//                    );
//                }
//
//                expectedId++;
//                cursor.advance();
//            }
//
//            System.out.println("Successfully scanned " + (expectedId - 1) + " rows.");
//
//            table.close();
//        }catch (Exception ex){
//            ex.printStackTrace();
//        }



        new Main().run();
    }

    public void run(){
        while(true){
            printPrompt();
            String input = readInput();
            Objects.requireNonNull(input);

            if(input.isEmpty()){
                continue;
            }
            if(input.charAt(0) == '.'){
                MetaCommandResult metaCommandResult = doMetaCommand(input);
                switch(metaCommandResult){
                    case META_COMMAND_SUCCESS:
                        continue;
                    case META_COMMAND_UNRECOGNIZED_COMMAND:
                        System.out.printf("Unrecognized command: '%s'%n", input);
                        continue;
                }
            }
            PreparedStatement preparedStatement = prepareStatement(input);
            switch (preparedStatement.getPrepareResult()){
                case PREPARE_SUCCESS:
                    break;
                case PREPARE_UNRECOGNIZED_STATEMENT:
                    System.out.printf("Unrecognized keyword at start of: '%s'%n", input);
                    continue;
                case PREPARE_SYNTAX_ERROR:
                    System.out.printf("Syntax error at start of: '%s'%n", input);
                    continue;
            }
            try {
                executeStatement(preparedStatement.getStatement());
                System.out.println("Executed.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Execution failed.");
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private MetaCommandResult doMetaCommand(String input){
        if (input.equals(".exit")){
            System.out.println("Exiting - Good bye.");
            table.close();
            System.exit(0);
        }
        return MetaCommandResult.META_COMMAND_UNRECOGNIZED_COMMAND;
    }

    private PreparedStatement prepareStatement(String input){
        String[] parts = input.trim().split("\\s+");
        if(parts[0].equalsIgnoreCase("insert")){
            if(parts.length != 4){
                return new PreparedStatement(
                        PrepareResult.PREPARE_SYNTAX_ERROR,
                        null
                );
            }

            try {

                int id = Integer.parseInt(parts[1]);
                String username = parts[2];
                String email = parts[3];

                Row row = new Row(id, username, email);
                byte[] data = RowSerializer.serialize(row);
                Row deserializedRow = RowSerializer.deserialize(data);
                System.out.println(deserializedRow.getId());
                System.out.println(deserializedRow.getUsername());
                System.out.println(deserializedRow.getEmail());
                Statement statement = new Statement(StatementType.STATEMENT_INSERT);

                statement.setRowToInsert(row);

                return new PreparedStatement(
                        PREPARE_SUCCESS,
                        statement
                );
            } catch (NumberFormatException e){
                return new PreparedStatement(
                        PrepareResult.PREPARE_SYNTAX_ERROR,
                        null
                );
            }

        }
        String preparedInput = input.toLowerCase();
        if(preparedInput.startsWith(StatementType.STATEMENT_INSERT.toString())){
            return new PreparedStatement(PREPARE_SUCCESS,new Statement(StatementType.STATEMENT_INSERT));
        }else if(preparedInput.startsWith(StatementType.STATEMENT_SELECT.toString())){
            return new PreparedStatement(PREPARE_SUCCESS,new Statement(StatementType.STATEMENT_SELECT));
        }
        return new PreparedStatement(PrepareResult.PREPARE_UNRECOGNIZED_STATEMENT, null);
    }

    private void executeStatement(Statement statement){
        if(statement.getStatementType() == StatementType.STATEMENT_INSERT){
            Row row = statement.getRowToInsert();
            table.insert(row);
        } else if(statement.getStatementType() == StatementType.STATEMENT_SELECT){
            table.selectAll();
        }
    }

    private void printPrompt(){
        System.out.print("homemadeDB > ");
    }

    private String readInput(){
        String inputLine = null;
        try{
            BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
            inputLine = is.readLine();
        }catch(IOException e){
            System.out.println("IOException: "+ e);
        }
        return inputLine;
    }
}