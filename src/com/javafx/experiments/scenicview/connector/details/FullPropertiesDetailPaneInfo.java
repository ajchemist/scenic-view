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
package com.javafx.experiments.scenicview.connector.details;

import java.util.*;

import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import com.javafx.experiments.scenicview.connector.StageID;
import com.javafx.experiments.scenicview.connector.event.AppEventDispatcher;
import com.sun.javafx.css.StyleableProperty;

@SuppressWarnings("rawtypes")
public class FullPropertiesDetailPaneInfo extends DetailPaneInfo {

    boolean showCSSProperties = true;

    public FullPropertiesDetailPaneInfo(final AppEventDispatcher dispatcher, final StageID stageID) {
        super(dispatcher, stageID, DetailPaneType.FULL);
    }

    Map<String, ObservableValue> orderedProperties;
    Map<String, Detail> fullPropertiesDetails;
    Map<WritableValue, String> styles;

    @Override protected String getPaneName() {
        return "Full Properties Details";
    }

    @Override public Class<? extends Node> getTargetClass() {
        return null;
    }

    @Override public boolean targetMatches(final Object candidate) {
        return candidate != null;
    }

    @Override protected void createDetails() {
        // Nothing to do
    }

    @Override public void setTarget(final Object value) {
        if (doSetTarget(value)) {
            createPropertiesPanel();
        }

    }

    @SuppressWarnings({ "deprecation", "unchecked" }) private void createPropertiesPanel() {
        final Node node = (Node) getTarget();
        styles = new HashMap<WritableValue, String>();
        details.clear();
        if (node != null) {
            final List<StyleableProperty> list = node.impl_getStyleableProperties();
            for (final Iterator iterator = list.iterator(); iterator.hasNext();) {
                final StyleableProperty styleableProperty = (StyleableProperty) iterator.next();
                final WritableValue wvalue = styleableProperty.getWritableValue(node);
                styles.put(wvalue, styleableProperty.getProperty());
            }
        }

        orderedProperties = new TreeMap<String, ObservableValue>();
        fullPropertiesDetails = new HashMap<String, Detail>();
        final Map<ObservableValue, String> properties = tracker.getProperties();
        for (final Iterator<ObservableValue> iterator = properties.keySet().iterator(); iterator.hasNext();) {
            final ObservableValue type = iterator.next();
            orderedProperties.put(properties.get(type), type);
        }
        for (final Iterator<String> iterator = orderedProperties.keySet().iterator(); iterator.hasNext();) {
            final String type = iterator.next();
            final String style = styles.get(orderedProperties.get(type));
            if (style == null || !showCSSProperties) {
                fullPropertiesDetails.put(type, addDetail(type, type + ":"));
            } else {
                fullPropertiesDetails.put(type, addDetail(type, type + "(" + style + "):"));
            }
        }
        updateAllDetails();
    }

    @Override protected void updateAllDetails() {
        if (orderedProperties != null) {
            for (final Iterator<String> iterator = orderedProperties.keySet().iterator(); iterator.hasNext();) {
                updateDetail(iterator.next(), true);
            }
        }
        sendAllDetails();
    }

    @Override protected void updateDetail(final String propertyName) {
        updateDetail(propertyName, false);
    }

    @SuppressWarnings("unchecked") protected void updateDetail(final String propertyName, final boolean all) {
        final Detail detail = fullPropertiesDetails.get(propertyName);
        final ObservableValue observable = orderedProperties.get(propertyName);
        final Object value = observable.getValue();
        detail.setValue(value == null ? "----" : value.toString());
        if (observable instanceof Property) {
            if (observable.getValue() instanceof Enum) {
                detail.setEnumProperty((Property) observable, (Class<? extends Enum>) observable.getValue().getClass());
            } else if (!(observable instanceof ObjectProperty)) {
                detail.setSimpleProperty((Property) observable);
            } else if (observable.getValue() instanceof Color) {
                detail.setSimpleProperty((Property) observable);
            } else {
                detail.setSimpleProperty(null);
                detail.unavailableEdition(Detail.STATUS_NOT_SUPPORTED);
            }
        } else {
            detail.setSimpleProperty(null);
            if (observable instanceof ReadOnlyProperty) {
                detail.unavailableEdition(Detail.STATUS_READ_ONLY);
            } else {
                System.out.println("Strange Property:" + observable);
            }
        }
        if (!all)
            detail.updated();
    }

    @Override void setShowCSSProperties(final boolean show) {
        showCSSProperties = show;
        if (getTarget() != null) {
            createPropertiesPanel();
        }
    }

}
