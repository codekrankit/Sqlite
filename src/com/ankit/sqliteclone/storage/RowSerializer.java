package com.ankit.sqliteclone.storage;

import com.ankit.sqliteclone.constants.Constants;
import com.ankit.sqliteclone.model.Row;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.ankit.sqliteclone.constants.Constants.*;

public class RowSerializer {
    public static byte[] serialize(Row row){
        ByteBuffer buffer = ByteBuffer.allocate(ROW_SIZE);

        // Store ID
        buffer.putInt(row.getId());

        // Store username
        putString(buffer, row.getUsername(), USERNAME_SIZE);

        // Store email
        putString(buffer, row.getEmail(), EMAIL_SIZE);

        return buffer.array();
    }

    private static void putString(ByteBuffer buffer, String value, int maxSize){
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        if(bytes.length > maxSize){
            throw new IllegalArgumentException("String too long");
        }
        buffer.put(bytes);
        for(int i=bytes.length; i<maxSize; i++){
            buffer.put((byte) 0);
        }
    }

    public static Row deserialize(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int id = buffer.getInt();
        String username = getString(buffer, USERNAME_SIZE);
        String email = getString(buffer, EMAIL_SIZE);
        return new Row(id, username, email);
    }

    private static String getString(ByteBuffer buffer, int size){
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        int length = 0;
        while(length < bytes.length && bytes[length] != 0){
            length++;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

}
