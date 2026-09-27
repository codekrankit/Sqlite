package com.ankit.sqliteclone.model;

import com.ankit.sqliteclone.storage.NodeUtils;
import com.ankit.sqliteclone.storage.Page;
import com.ankit.sqliteclone.storage.Pager;

import java.io.IOException;

public class Cursor {
    private final Pager pager;
    private int pageNumber;
    private int cellNumber;
    private boolean endOfTable;

    public Cursor(Pager pager, int pageNumber, int cellNumber, boolean endOfTable) {
        this.pager = pager;
        this.pageNumber = pageNumber;
        this.cellNumber = cellNumber;
        this.endOfTable = endOfTable;
    }
    public int getPageNumber() {
        return pageNumber;
    }
    public int getCellNumber() {
        return cellNumber;
    }
    public boolean isEndOfTable() {
        return endOfTable;
    }
    public void advance(){
        try{
            Page page = pager.getPage(pageNumber);
            cellNumber++;
            int numCells = NodeUtils.getLeafNodeNumCells(page);
            if (cellNumber == numCells) {
                int nextLeaf = NodeUtils.getLeafNodeNextLeaf(page);

                if (nextLeaf == 0){
                    endOfTable = true;
                } else {
                    pageNumber = nextLeaf;
                    cellNumber = 0;
                }
            }
        } catch (IOException e){
            throw new RuntimeException("Error advancing cursor", e);
        }
    }
    public void setEndOfTable(boolean endOfTable) {
        this.endOfTable = endOfTable;
    }
}
