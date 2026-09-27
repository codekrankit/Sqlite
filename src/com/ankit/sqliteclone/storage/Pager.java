package com.ankit.sqliteclone.storage;

import com.ankit.sqliteclone.constants.Constants;

import java.io.*;

public class Pager {
    private final RandomAccessFile file;
    private final Page[] pages;
    private int numPages;
    private final String filename;
    private boolean closed = false;

    public Pager(String fileName) throws IOException {
        this.file = new RandomAccessFile(fileName, "rw");
        this.pages = new Page[Constants.TABLE_MAX_PAGES];
        this.numPages = (int)(file.length() / Constants.PAGE_SIZE);
        this.filename = fileName;
    }
    public int loadRowCount() throws IOException {
        String metadataFile = filename+ ".meta";
        File file = new File(metadataFile);
        if (!file.exists()){
            return 0;
        }
        try (DataInputStream input = new DataInputStream(new FileInputStream(file))){
            return input.readInt();
        }
    }

    public void saveRowCount(int rowCount) throws IOException {
        String metadataFile = filename+ ".meta";
        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(metadataFile))){
            output.writeInt(rowCount);
        }
    }

    public Page getPage(int pageNumber)throws IOException{
        if (pageNumber >= Constants.TABLE_MAX_PAGES) {
            throw new IllegalArgumentException(
                    "Page number out of bounds: " + pageNumber +
                            ", max: " + Constants.TABLE_MAX_PAGES
            );
        }

        if(pages[pageNumber] == null){
            Page page = new Page();
            if( pageNumber < numPages){
                long offset = (long)(pageNumber * Constants.PAGE_SIZE);
                file.seek(offset);
                file.readFully(page.getData());
            } else {
                // New page -> increase page count
                numPages = pageNumber + 1;
            }
            pages[pageNumber] = page;
        }
        return pages[pageNumber];
    }

    public int getUnusedPageNumber(){
        return numPages;
    }

    public void flush(int pageNumber) throws IOException{
        if (pages[pageNumber] == null){
            throw new IllegalStateException("Tried to flush null page");
        }

        long offset = (long)pageNumber * Constants.PAGE_SIZE;

        file.seek(offset);
        file.write(pages[pageNumber].getData());
        if(pageNumber>=numPages){
            numPages = pageNumber+1;
        }
    }

    public void close() throws IOException{
        if(closed){
            return;
        }
        for(int i=0; i<Constants.TABLE_MAX_PAGES; i++){
            if(pages[i] != null){
                flush(i);
            }
        }
        file.close();
        closed = true;
    }
    public int getNumPages(){
        return numPages;
    }

    public Page getUnusedPage() throws IOException{
        int pageNumber = numPages;
        return getPage(pageNumber);
    }
}
