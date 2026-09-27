package com.ankit.sqliteclone.model;

import com.ankit.sqliteclone.enums.NodeType;
import com.ankit.sqliteclone.storage.Page;

public class Node {
    private final NodeType nodeType;
    private final Page page;

    public Node(NodeType nodeType, Page page) {
        this.nodeType = nodeType;
        this.page = page;
    }
    public NodeType getNodeType() {
        return nodeType;
    }
    public Page getPage() {
        return page;
    }
}
