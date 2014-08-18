/*
 * Copyright (c) 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fxconnector.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;


import org.fxconnector.ConnectorUtils;
import org.fxconnector.helper.ChildrenGetter;

class SVRemoteNodeAdapter extends SVNodeImpl implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5972848763525174505L;
    private String id;
    private int nodeId;
    private boolean visible;
    private boolean mouseTransparent;
    private boolean focused;
    private List<SVNode> nodes;
    private SVRemoteNodeAdapter parent;

    public SVRemoteNodeAdapter() {
        super();
    }

    public SVRemoteNodeAdapter(final Node node, final boolean collapseControls, final boolean collapseContentControls) {
        this(node, collapseControls, collapseContentControls, true, null);
    }

    public SVRemoteNodeAdapter(final Node node, final boolean collapseControls, final boolean collapseContentControls, final boolean fillChildren, final SVRemoteNodeAdapter parent) {
        super(ConnectorUtils.nodeClass(node), node.getClass().getName());
        boolean mustBeExpanded = !(node instanceof Control) || !collapseControls;
        if (!mustBeExpanded && !collapseContentControls) {
            mustBeExpanded = node instanceof TabPane || node instanceof SplitPane || node instanceof ScrollPane || node instanceof Accordion || node instanceof TitledPane;
        }
        setExpanded(mustBeExpanded);
        this.id = node.getId();
        this.nodeId = ConnectorUtils.getNodeUniqueID(node);
        this.focused = node.isFocused();
        if (node.getParent() != null && parent == null) {
            this.parent = new SVRemoteNodeAdapter(node.getParent(), collapseControls, collapseContentControls, false, null);
        } else if (parent != null) {
            this.parent = parent;
        }
        /**
         * Check visibility and mouse transparency after calculating the parent
         */
        this.mouseTransparent = node.isMouseTransparent() || (this.parent != null && this.parent.isMouseTransparent());
        this.visible = node.isVisible() && (this.parent == null || this.parent.isVisible());

        /**
         * TODO This should be improved
         */
        if (fillChildren) {
            nodes = ChildrenGetter.getChildren(node)
                      .stream()
                      .map(childNode -> new SVRemoteNodeAdapter(childNode, collapseControls, collapseContentControls, true, this))
                      .collect(Collectors.toList());
        }
    }

    @Override public String getId() {
        return id;
    }

    @Override public String getExtendedId() {
        return ConnectorUtils.nodeDetail(this, true);
    }

    @Override public SVNode getParent() {
        return parent;
    }

    @Override public List<SVNode> getChildren() {
        return nodes;
    }

    @Override public boolean equals(final SVNode node) {
        if (node instanceof SVDummyNode) {
            return false;
        }
        return node != null && node.getNodeId() == getNodeId();
    }

    /**
     * This must be removed in the future
     */
    @Override public boolean equals(final Object node) {
        if (node instanceof SVNode) {
            return equals((SVNode) node);
        } else if (node instanceof Node) {
            return getNodeId() == ConnectorUtils.getNodeUniqueID((Node) node);
        }
        return false;
    }

    @Override @Deprecated public Node getImpl() {
        return null;
    }

    @Override public int getNodeId() {
        return nodeId;
    }

    @Override public boolean isVisible() {
        return visible;
    }

    @Override public boolean isMouseTransparent() {
        return mouseTransparent;
    }

    @Override public boolean isFocused() {
        // TODO Auto-generated method stub
        return focused;
    }

    @Override public boolean isRealNode() {
        return true;
    }

    @Override public String toString() {
        return ConnectorUtils.nodeDetail(this, showID);
    }

    @Override public int hashCode() {
        return nodeId;
    }

    @Override public NodeType getNodeType() {
        return NodeType.REMOTE_NODE;
    }

}
