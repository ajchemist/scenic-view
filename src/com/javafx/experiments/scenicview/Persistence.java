package com.javafx.experiments.scenicview;

import java.util.*;

import javafx.scene.control.*;
import javafx.stage.Stage;

import com.javafx.experiments.scenicview.utils.PropertiesUtils;

class Persistence {

    private static Properties properties;
    private static final Map<String, Object> persistentComponents = new HashMap<String, Object>();

    static void loadProperty(final String propertyName, final Object component, final Object defaultValue) {
        final String property = properties.getProperty(propertyName, defaultValue.toString());
        if (component instanceof CheckMenuItem) {
            ((CheckMenuItem) component).setSelected(Boolean.parseBoolean(property));
        }
        // We should think of a better way of doing this
        else if (component instanceof SplitPane) {
            // System.out.println("Loaded divider position position:"+property);
            ((SplitPane) component).setDividerPosition(0, Double.parseDouble(property));
        }
        // else if(component instanceof Control) {
        // if(propertyName.toLowerCase().indexOf("width")!=-1) {
        // ((Control) component).setPrefWidth(Double.parseDouble(property));
        // }
        // else {
        // ((Control) component).setPrefHeight(Double.parseDouble(property));
        // }
        // }
        else if (component instanceof Stage) {
            if (propertyName.toLowerCase().indexOf("width") != -1) {
                ((Stage) component).setWidth(Double.parseDouble(property));
            } else {
                ((Stage) component).setHeight(Double.parseDouble(property));
            }
        }
        persistentComponents.put(propertyName, component);
    }

    static void loadProperties() {
        properties = PropertiesUtils.loadProperties();
    }

    static void saveProperties(final Properties properties) {
        for (final Iterator<String> iterator = persistentComponents.keySet().iterator(); iterator.hasNext();) {
            final String propertyName = iterator.next();
            final Object component = persistentComponents.get(propertyName);
            if (component instanceof CheckMenuItem) {
                properties.put(propertyName, Boolean.toString(((CheckMenuItem) component).isSelected()));
            } else if (component instanceof SplitPane) {
                properties.put(propertyName, Double.toString(((SplitPane) component).getDividerPositions()[0]));
            }
            // else if(component instanceof Control) {
            // if(propertyName.toLowerCase().indexOf("width") != -1) {
            // properties.put(propertyName, Double.toString(((Control)
            // component).getWidth()));
            // }
            // else {
            // properties.put(propertyName, Double.toString(((Control)
            // component).getHeight()));
            // }
            // }
            else if (component instanceof Stage) {
                if (propertyName.toLowerCase().indexOf("width") != -1) {
                    properties.put(propertyName, Double.toString(((Stage) component).getWidth()));
                } else {
                    properties.put(propertyName, Double.toString(((Stage) component).getHeight()));
                }
            }
        }
        PropertiesUtils.saveProperties(properties);
    }

}
