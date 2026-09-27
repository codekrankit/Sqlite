package com.ankit.sqliteclone.storage;

import com.ankit.sqliteclone.constants.Constants;
import com.ankit.sqliteclone.enums.NodeType;

import java.nio.ByteBuffer;

public class NodeUtils {
    public static void initializeLeafNode(Page page){
        byte[] data = page.getData();
        data[Constants.NODE_TYPE_OFFSET] = (byte) NodeType.LEAF.ordinal();

        data[Constants.IS_ROOT_OFFSET] = 0;

        ByteBuffer.wrap(data).putInt(Constants.PARENT_POINTER_OFFSET,0);
        ByteBuffer.wrap(data).putInt(Constants.LEAF_NODE_NUM_CELLS_OFFSET,0);
        ByteBuffer.wrap(data).putInt(Constants.LEAF_NODE_NEXT_LEAF_OFFSET,0);
    }
    public static int getLeafNodeNextLeaf(Page page){
        return ByteBuffer.wrap(page.getData()).getInt(Constants.LEAF_NODE_NEXT_LEAF_OFFSET);
    }
    public static void setLeafNodeNextLeaf(Page page,int pageNumber){
        ByteBuffer.wrap(page.getData()).putInt(Constants.LEAF_NODE_NEXT_LEAF_OFFSET,pageNumber);
    }

    public static void setRoot(Page page, boolean isRoot){
        page.getData()[Constants.IS_ROOT_OFFSET] = (byte)(isRoot ? 1 : 0);
    }

    public static boolean isRoot(Page page){
        return page.getData()[Constants.IS_ROOT_OFFSET] == 1;
    }

    public static int getLeafNodeNumCells(Page page){
        return ByteBuffer.wrap(page.getData()).getInt(Constants.LEAF_NODE_NUM_CELLS_OFFSET);
    }

    public static void setLeafNodeNumCells(Page page, int numCells){
        ByteBuffer.wrap(page.getData()).putInt(Constants.LEAF_NODE_NUM_CELLS_OFFSET, numCells);
    }

    public static int getNodeMaxKey(Page page){
        int numCells = getLeafNodeNumCells(page);
        return getLeafNodeKey(page, numCells -1);
    }


    public static int getParent(Page page){
        return ByteBuffer.wrap(page.getData()).getInt(Constants.PARENT_POINTER_OFFSET);
    }

    public static void setParent(Page page,int parentPageNumber){
        ByteBuffer.wrap(page.getData()).putInt(Constants.PARENT_POINTER_OFFSET, parentPageNumber);
    }

    public static NodeType getNodeType(Page page){
        int ordinal = page.getData()[Constants.NODE_TYPE_OFFSET];
        return NodeType.values()[ordinal];
    }


    public static int getLeafNodeCellOffset(int cellNumber){
        return Constants.LEAF_NODE_HEADER_SIZE + (cellNumber * Constants.LEAF_NODE_CELL_SIZE);
    }

    public static int getLeafNodeKey(Page page, int cellNumber){
        int cellOffset = getLeafNodeCellOffset(cellNumber);
        int keyOffset = cellOffset + Constants.LEAF_NODE_KEY_OFFSET;

        return ByteBuffer.wrap(page.getData()).getInt(keyOffset);
    }

    public static void setLeafNodeKey(Page page, int cellNumber, int key){
        int cellOffset = getLeafNodeCellOffset(cellNumber);
        int keyOffset = cellOffset + Constants.LEAF_NODE_KEY_OFFSET;
        ByteBuffer.wrap(page.getData()).putInt(keyOffset, key);
    }

    public static void setLeafNodeValue(Page page, int cellNumber, byte[] serializedRow){
        int cellOffset = getLeafNodeCellOffset(cellNumber);
        int valueOffset = cellOffset + Constants.LEAF_NODE_VALUE_OFFSET;

        System.arraycopy(serializedRow, 0, page.getData(), valueOffset, Constants.ROW_SIZE);
    }

    public static byte[] getLeafNodeValue(Page page, int cellNumber){
        int cellOffset = getLeafNodeCellOffset(cellNumber);
        int valueOffset = cellOffset + Constants.LEAF_NODE_VALUE_OFFSET;

        byte[] rowData = new byte[Constants.ROW_SIZE];

        System.arraycopy(page.getData(), valueOffset, rowData, 0, Constants.ROW_SIZE);

        return rowData;
    }

    public static void initializeInternalNode(Page page){
        byte[] data = page.getData();
        data[Constants.NODE_TYPE_OFFSET] = (byte) NodeType.INTERNAL.ordinal();
        data[Constants.IS_ROOT_OFFSET] = 0;
        ByteBuffer.wrap(data).putInt(Constants.PARENT_POINTER_OFFSET,0);
        ByteBuffer.wrap(data).putInt(Constants.INTERNAL_NODE_NUM_KEYS_OFFSET,0);
        ByteBuffer.wrap(data).putInt(Constants.INTERNAL_NODE_RIGHT_CHILD_OFFSET,0);
    }

    public static int getInternalNodeNumKeys(Page page){
        return ByteBuffer.wrap(page.getData()).getInt(Constants.INTERNAL_NODE_NUM_KEYS_OFFSET);
    }

    public static void setInternalNodeNumKeys(Page page, int numKeys){
        ByteBuffer.wrap(page.getData()).putInt(Constants.INTERNAL_NODE_NUM_KEYS_OFFSET,numKeys);
    }
    public static int getInternalNodeRightChild(Page page){
        return ByteBuffer.wrap(page.getData()).getInt(Constants.INTERNAL_NODE_RIGHT_CHILD_OFFSET);
    }
    public static void setInternalNodeRightChild(Page page, int pageNumber){
        ByteBuffer.wrap(page.getData()).putInt(Constants.INTERNAL_NODE_RIGHT_CHILD_OFFSET,pageNumber);
    }
    public static int getInternalNodeCellOffset(int cellNumber){
        return Constants.INTERNAL_NODE_HEADER_SIZE + (cellNumber * Constants.INTERNAL_NODE_CELL_SIZE);
    }
    public static void setInternalNodeChild(Page page, int cellNumber, int childPageNumber){
        int offset = getInternalNodeCellOffset(cellNumber);
        ByteBuffer.wrap(page.getData()).putInt(offset,childPageNumber);
    }
    public static int getInternalNodeChild(Page page,int cellNumber){
        int offset = getInternalNodeCellOffset(cellNumber);

        return ByteBuffer.wrap(page.getData()).getInt(offset);
    }

    public static void setInternalNodeKey(Page page, int cellNumber, int key){
        int offset = getInternalNodeCellOffset(cellNumber) + Constants.INTERNAL_NODE_CHILD_SIZE;
        ByteBuffer.wrap(page.getData()).putInt(offset,key);
    }

    public static int getInternalNodeKey(
            Page page,
            int cellNumber
    ) {

        int offset =
                getInternalNodeCellOffset(cellNumber)
                        + Constants.INTERNAL_NODE_CHILD_SIZE;

        return ByteBuffer.wrap(page.getData()).getInt(offset);
    }

    public static void updateInternalNodeKey(Page page, int oldKey, int newKey){
        int numKeys = getInternalNodeNumKeys(page);
        for(int i=0;i<numKeys;i++){
            if(getInternalNodeKey(page,i) == oldKey){
                setInternalNodeKey(page,i,newKey);
                return;
            }
        }
        throw new IllegalStateException("Old key not found in internal node");
    }



}
