package com.ankit.sqliteclone.model;

import com.ankit.sqliteclone.constants.Constants;
import com.ankit.sqliteclone.enums.NodeType;
import com.ankit.sqliteclone.storage.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;

public class Table {
    private final Pager pager;
    private boolean closed = false;

    public Table(Pager pager){
        this.pager = pager;
        try{
            if (pager.getNumPages() == 0){
                Page rootPage = pager.getPage(0);
                NodeUtils.initializeLeafNode(rootPage);
                NodeUtils.setRoot(rootPage, true);
            }
        }catch (IOException e){
            throw new RuntimeException("Could not initialize table", e);
        }
    }

    public void leafNodeSplitAndInsert(int oldPageNumber, Page oldPage, Cursor cursor, Row row) throws IOException{
        int newPageNumber = pager.getUnusedPageNumber();
        Page newPage = pager.getPage(newPageNumber);
        NodeUtils.initializeLeafNode(newPage);

        int oldNextLeaf = NodeUtils.getLeafNodeNextLeaf(oldPage);
        NodeUtils.setLeafNodeNextLeaf(newPage, oldNextLeaf);

        NodeUtils.setLeafNodeNextLeaf(oldPage, newPageNumber);

        int oldNumCells = NodeUtils.getLeafNodeNumCells(oldPage);
        int oldMaxKey = NodeUtils.getNodeMaxKey(oldPage);

        int insertionIndex = cursor.getCellNumber();

        for(int i= oldNumCells; i>=0; i--){
            Page destinationPage;
            int destinationCell;
            if( i>= Constants.LEAF_NODE_LEFT_SPLIT_COUNT){
                destinationPage = newPage;
                destinationCell = i - Constants.LEAF_NODE_LEFT_SPLIT_COUNT;
            } else {
                destinationPage = oldPage;
                destinationCell = i;
            }
            if(i==insertionIndex){
                byte[] serializedRow = RowSerializer.serialize(row);
                NodeUtils.setLeafNodeKey(destinationPage, destinationCell, row.getId());
                NodeUtils.setLeafNodeValue(destinationPage,destinationCell, serializedRow);
            } else{
                int sourceIndex;
                if (i> insertionIndex){
                    sourceIndex = i - 1;
                }else{
                    sourceIndex = i;
                }

                int sourceOffset = NodeUtils.getLeafNodeCellOffset(sourceIndex);
                int destinationOffset = NodeUtils.getLeafNodeCellOffset(destinationCell);
                System.arraycopy(oldPage.getData(),sourceOffset,destinationPage.getData(),destinationOffset,Constants.LEAF_NODE_CELL_SIZE);
            }
        }
        NodeUtils.setLeafNodeNumCells(oldPage, Constants.LEAF_NODE_LEFT_SPLIT_COUNT);
        NodeUtils.setLeafNodeNumCells(newPage, Constants.LEAF_NODE_RIGHT_SPLIT_COUNT);
        int newMaxKey = NodeUtils.getNodeMaxKey(oldPage);
        if (NodeUtils.isRoot(oldPage)) {
            createNewRoot(newPageNumber);
        } else {
            int parentPageNumber = NodeUtils.getParent(oldPage);
            Page parentPage = pager.getPage(parentPageNumber);
            int rightChildPageNumber = NodeUtils.getInternalNodeRightChild(parentPage);
            if(rightChildPageNumber!=oldPageNumber){
                NodeUtils.updateInternalNodeKey(parentPage,oldMaxKey,newMaxKey);
            }
            internalNodeInsert(parentPageNumber,parentPage,newPageNumber);
        }
    }


    private void createNewRoot(int rightChildPageNumber) throws IOException{
        Page rootPage = pager.getPage(0);
        int leftChildPageNumber = pager.getUnusedPageNumber();
        Page leftChild = pager.getPage(leftChildPageNumber);
        NodeUtils.setParent(leftChild,0);
        Page rightChild = pager.getPage(rightChildPageNumber);
        NodeUtils.setParent(rightChild,0);


        System.arraycopy(rootPage.getData(),0,leftChild.getData(),0,Constants.PAGE_SIZE);
        NodeUtils.setRoot(leftChild, false);
        NodeUtils.initializeInternalNode(rootPage);
        NodeUtils.setRoot(rootPage, true);

        // Internal root has one key
        NodeUtils.setInternalNodeNumKeys(rootPage,1);
        NodeUtils.setInternalNodeChild(rootPage,0,leftChildPageNumber);

        int leftChildMaxKey = NodeUtils.getNodeMaxKey(leftChild);

        NodeUtils.setInternalNodeKey(rootPage,0, leftChildMaxKey);
        NodeUtils.setInternalNodeRightChild(rootPage,rightChildPageNumber);
    }

    public void insert(Row row){
        try {
            Cursor cursor = tableFind(row.getId());
            Page page = pager.getPage(cursor.getPageNumber());

            int numCells = NodeUtils.getLeafNodeNumCells(page);

            if (numCells >= Constants.LEAF_NODE_MAX_CELLS) {
                leafNodeSplitAndInsert(cursor.getPageNumber(), page, cursor, row);
                return;
            }


            int cellNumber = cursor.getCellNumber();

            if(cellNumber < numCells){
                int existingKey = NodeUtils.getLeafNodeKey(page, cellNumber);
                if (existingKey == row.getId()) {
                    throw new IllegalArgumentException("Duplicate key:"+ row.getId());
                }
            }

            for (int i=numCells;i>cellNumber;i--){
                int destinationOffset = NodeUtils.getLeafNodeCellOffset(i);
                int sourceOffset = NodeUtils.getLeafNodeCellOffset(i-1);
                System.arraycopy(page.getData(),sourceOffset,page.getData(),destinationOffset,Constants.LEAF_NODE_CELL_SIZE);
            }

            byte[] serializedRow = RowSerializer.serialize(row);

            NodeUtils.setLeafNodeKey(page,cellNumber,row.getId());
            NodeUtils.setLeafNodeValue(page,cellNumber,serializedRow);
            NodeUtils.setLeafNodeNumCells(page,numCells+1);
        } catch (IOException e){
            throw new RuntimeException("Error inserting row", e);
        }
    }

    public void selectAll(){
        Cursor cursor = tableStart();
        while(!cursor.isEndOfTable()) {
            Row row = cursorValue(cursor);
            System.out.println(
                    "(" +
                            row.getId() + ", " +
                            row.getUsername() + ", " +
                            row.getEmail() +
                            ")"
            );
            cursor.advance();
        }
        printTree();
    }

    public Cursor tableStart(){
        try{
            int pageNumber = 0;
            while(true){
                Page page = pager.getPage(pageNumber);
                NodeType nodeType = NodeUtils.getNodeType(page);
                if (nodeType == NodeType.LEAF){
                    int numCells = NodeUtils.getLeafNodeNumCells(page);

                    return new Cursor(
                            pager,
                            pageNumber,
                            0,
                            numCells == 0
                    );
                }
                pageNumber = NodeUtils.getInternalNodeChild(page,0);
            }

        } catch (IOException e){
            throw new RuntimeException("Error finding table start: ",e);
        }
    }

    public Row cursorValue(Cursor cursor){
        try{
            Page page = pager.getPage(cursor.getPageNumber());
            byte[] rowData = NodeUtils.getLeafNodeValue(page,cursor.getCellNumber());
            return RowSerializer.deserialize(rowData);
        }catch (IOException e){
            throw new RuntimeException("Error reading row", e);
        }
    }

    public void close(){
        if(closed){
            return;
        }
        try{
            pager.close();
            closed = true;
        } catch (IOException e){
            throw new RuntimeException("Error closing database", e);
        }
    }

    public Cursor leafNodeFind(int pageNumber, Page page, int key){
        int numCells = NodeUtils.getLeafNodeNumCells(page);
        int minIndex = 0;
        int maxIndex = numCells;
        while (minIndex != maxIndex){
            int index = minIndex + (maxIndex-minIndex) / 2;
            int keyAtIndex = NodeUtils.getLeafNodeKey(page, index);
            if (keyAtIndex == key){
                return new Cursor(
                        pager,
                        pageNumber,
                        index,
                        false
                );
            } else if ( key < keyAtIndex){
                maxIndex = index;
            } else {
                minIndex = index + 1;
            }
        }

        return new Cursor(
                pager,
                pageNumber,
                minIndex,
                false
        );
    }
    public Cursor internalNodeFind(Page page, int key) throws  IOException {
        int numKeys = NodeUtils.getInternalNodeNumKeys(page);
        int childIndex = 0;
        while(childIndex < numKeys){
            int keyToRight = NodeUtils.getInternalNodeKey(page, childIndex);

            if (key<= keyToRight){
                break;
            }
            childIndex++;
        }
        int childPageNumber;
        if(childIndex == numKeys){
            childPageNumber = NodeUtils.getInternalNodeRightChild(page);
        } else {
            childPageNumber = NodeUtils.getInternalNodeChild(page, childIndex);
        }
        Page childPage = pager.getPage(childPageNumber);

        NodeType childType = NodeUtils.getNodeType(childPage);

        if(childType == NodeType.LEAF){
            return leafNodeFind(childPageNumber, childPage, key);
        }

        if(childType == NodeType.INTERNAL){
            return internalNodeFind(childPage, key);
        }

        throw new IllegalStateException("Unknown child node type");

    }
    public Cursor tableFind(int key){
        try{
            Page rootPage = pager.getPage(0);
            NodeType nodeType = NodeUtils.getNodeType(rootPage);
            if (nodeType == NodeType.LEAF){
                return leafNodeFind(0, rootPage, key);
            } else if (nodeType == NodeType.INTERNAL){
                return internalNodeFind(rootPage,key);
            }
            throw new IllegalStateException("Unknown node type");
        } catch (IOException e){
            throw new RuntimeException("Error finding key", e);
        }
    }

    public void internalNodeInsert(int parentPageNumber, Page parentPage, int childPageNumber) throws IOException{
        int numKeys = NodeUtils.getInternalNodeNumKeys(parentPage);
        if (numKeys >= Constants.INTERNAL_NODE_MAX_CELLS){
            internalNodeSplitAndInsert(parentPageNumber, parentPage, childPageNumber);
            return;
        }
        Page childPage = pager.getPage(childPageNumber);
        int childMaxKey = NodeUtils.getNodeMaxKey(childPage);

        int index = 0;

        while(index<numKeys){
            int key = NodeUtils.getInternalNodeKey(parentPage, index);

            if(childMaxKey < key){
                break;
            }

            index++;
        }

        int rightChildPageNumber = NodeUtils.getInternalNodeRightChild(parentPage);

        if (index == numKeys){
            Page rightChild = pager.getPage(rightChildPageNumber);
            int rightChildMaxKey = NodeUtils.getNodeMaxKey(rightChild);
            NodeUtils.setInternalNodeChild(parentPage, numKeys, rightChildPageNumber);
            NodeUtils.setInternalNodeKey(parentPage, numKeys, rightChildMaxKey);
            NodeUtils.setInternalNodeRightChild(parentPage, childPageNumber);
        } else {
            for (int i = numKeys; i > index; i--){
                int destinationOffset = NodeUtils.getInternalNodeCellOffset(i);
                int sourceOffset = NodeUtils.getInternalNodeCellOffset(i-1);

                System.arraycopy(parentPage.getData(),sourceOffset,parentPage.getData(),destinationOffset, Constants.INTERNAL_NODE_CELL_SIZE);
            }
            NodeUtils.setInternalNodeChild(parentPage, index, childPageNumber);
            NodeUtils.setInternalNodeKey(parentPage, index, childMaxKey);
        }

        NodeUtils.setInternalNodeNumKeys(parentPage, numKeys+1);
        NodeUtils.setParent(childPage,parentPageNumber);

    }

    private void printTree(
            int pageNumber,
            int indentation,
            BufferedWriter writer) throws IOException {

        Page page = pager.getPage(pageNumber);
        NodeType nodeType = NodeUtils.getNodeType(page);

        if (nodeType == NodeType.LEAF) {

            int numCells = NodeUtils.getLeafNodeNumCells(page);

            printIndent(indentation, writer);

            writer.write("- leaf (page " + pageNumber +
                    ", " + numCells + " cells)");
            writer.newLine();

            for (int i = 0; i < numCells; i++) {

                int key = NodeUtils.getLeafNodeKey(page, i);

                printIndent(indentation + 1, writer);

                writer.write("- key " + key);
                writer.newLine();
            }

        } else if (nodeType == NodeType.INTERNAL) {

            int numKeys = NodeUtils.getInternalNodeNumKeys(page);

            printIndent(indentation, writer);

            writer.write("- internal (page " + pageNumber +
                    ", " + numKeys + " keys)");
            writer.newLine();

            for (int i = 0; i < numKeys; i++) {

                int childPageNumber =
                        NodeUtils.getInternalNodeChild(page, i);

                int key =
                        NodeUtils.getInternalNodeKey(page, i);

                printIndent(indentation + 1, writer);

                writer.write("- key " + key);
                writer.newLine();

                printTree(childPageNumber, indentation + 2, writer);
            }

            int rightChildPageNumber =
                    NodeUtils.getInternalNodeRightChild(page);

            printIndent(indentation + 1, writer);

            writer.write("- right child:");
            writer.newLine();

            printTree(rightChildPageNumber, indentation + 2, writer);
        }
    }

    private void printIndent(
            int indentation,
            BufferedWriter writer) throws IOException {

        for (int i = 0; i < indentation; i++) {
            writer.write("  ");
        }
    }

    public void printTree() {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("tree.txt"))) {

            printTree(0, 0, writer);

        } catch (IOException e) {
            throw new RuntimeException("Error printing tree", e);
        }
    }

    private int getNodeMaxKey(int pageNumber) throws IOException{
        Page page = pager.getPage(pageNumber);

        NodeType nodeType = NodeUtils.getNodeType(page);
        if (nodeType == NodeType.LEAF) {
            return NodeUtils.getNodeMaxKey(page);
        }
        else if (nodeType == NodeType.INTERNAL) {
            int rightChild = NodeUtils.getInternalNodeRightChild(page);
            return getNodeMaxKey(rightChild);
        }

        throw new IllegalStateException("Unknown node type");
    }
    private void internalNodeSplitAndInsert(int oldPageNumber, Page oldPage, int childPageNumber) throws IOException{
        int oldMaxKey = getNodeMaxKey(oldPageNumber);
        int oldNumKeys = NodeUtils.getInternalNodeNumKeys(oldPage);
        int totalChildren = oldNumKeys + 2;

        int[] children = new int[totalChildren];

        for (int i=0; i<oldNumKeys; i++){
            children[i] = NodeUtils.getInternalNodeChild(oldPage, i);
        }
        children[oldNumKeys] = NodeUtils.getInternalNodeRightChild(oldPage);
        int newChildMaxKey = getNodeMaxKey(childPageNumber);
        int insertIndex = 0;
        while( insertIndex < totalChildren - 1){
            int existingChildMaxKey = getNodeMaxKey(children[insertIndex]);

            if ( newChildMaxKey < existingChildMaxKey ){
                break;
            }

            insertIndex++;
        }
        for (int i= totalChildren-1; i>insertIndex; i--){
            children[i] = children[i-1];
        }
        children[insertIndex] = childPageNumber;

        int leftChildCount = totalChildren / 2;
        int rightChildCount = totalChildren - leftChildCount;
        int leftPageNumber;
        Page leftPage;
        int rightPageNumber;
        Page rightPage;

        if (NodeUtils.isRoot(oldPage)){
            leftPageNumber = pager.getUnusedPageNumber();
            leftPage = pager.getPage(leftPageNumber);

            rightPageNumber = pager.getUnusedPageNumber();
            rightPage = pager.getPage(rightPageNumber);

            NodeUtils.initializeInternalNode(leftPage);
            NodeUtils.initializeInternalNode(rightPage);
            NodeUtils.setParent(leftPage,0);
            NodeUtils.setParent(rightPage,0);
            NodeUtils.setRoot(leftPage, false);
            NodeUtils.setRoot(rightPage, false);
        } else {
            leftPageNumber = oldPageNumber;
            leftPage = oldPage;

            rightPageNumber = pager.getUnusedPageNumber();
            rightPage = pager.getPage(rightPageNumber);

            NodeUtils.initializeInternalNode(rightPage);

            int parentPageNumber = NodeUtils.getParent(oldPage);

            NodeUtils.setParent(rightPage,parentPageNumber);
            NodeUtils.setRoot(rightPage, false);
        }

        setInternalNodeChildren(leftPage, children, 0, leftChildCount);

        setInternalNodeChildren(rightPage, children, leftChildCount, rightChildCount);

        for( int i=0;i< leftChildCount;i++){
            Page childPage = pager.getPage(children[i]);
            NodeUtils.setParent(childPage, leftPageNumber);
        }
        for (int i=leftChildCount;i< totalChildren;i++){
            Page childPage = pager.getPage(children[i]);
            NodeUtils.setParent(childPage, rightPageNumber);
        }
        if (NodeUtils.isRoot(oldPage)){
            Page rootPage = pager.getPage(0);
            NodeUtils.initializeInternalNode(rootPage);
            NodeUtils.setRoot(rootPage, true);
            NodeUtils.setInternalNodeNumKeys(rootPage, 1);
            NodeUtils.setInternalNodeChild(rootPage, 0, leftPageNumber);
            int leftMaxKey = getNodeMaxKey(leftPageNumber);
            NodeUtils.setInternalNodeKey(rootPage, 0, leftMaxKey);
            NodeUtils.setInternalNodeRightChild(rootPage, rightPageNumber);
        } else {
            int parentPageNumber = NodeUtils.getParent(oldPage);
            Page parentPage = pager.getPage(parentPageNumber);
            int newMaxKey = getNodeMaxKey(leftPageNumber);
            int parentRightChild = NodeUtils.getInternalNodeRightChild(parentPage);

            if (parentRightChild != oldPageNumber){
                NodeUtils.updateInternalNodeKey(
                        parentPage,
                        oldMaxKey,
                        newMaxKey
                );
            }
            internalNodeInsert(
                    parentPageNumber,
                    parentPage,
                    rightPageNumber
            );
        }
    }
    private void setInternalNodeChildren(Page page, int[] children, int start, int count) throws IOException{
        int numKeys = count -1;
        NodeUtils.initializeInternalNode(page);
        NodeUtils.setInternalNodeNumKeys(page, numKeys);
        for (int i=0;i<numKeys;i++){
            int childPageNumber = children[start + i];
            NodeUtils.setInternalNodeChild(page, i, childPageNumber);
            int childMaxKey = getNodeMaxKey(childPageNumber);
            NodeUtils.setInternalNodeKey(page, i, childMaxKey);
        }
        int rightChild = children[start+count -1];
        NodeUtils.setInternalNodeRightChild(page, rightChild);
    }

}
