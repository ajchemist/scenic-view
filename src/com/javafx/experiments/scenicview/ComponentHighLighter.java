package com.javafx.experiments.scenicview;

import javafx.geometry.*;
import javafx.scene.Group;
import javafx.scene.control.TitledPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;

public class ComponentHighLighter extends Group {

    public ComponentHighLighter(final NodeInfo node, final double width, final double height, final Bounds bounds, final Point2D start) {
        if(width == -1) {
            final Rectangle rect = new Rectangle();
            rect.setFill(Color.TRANSPARENT);
            rect.setStroke(Color.ORANGE);
            rect.setMouseTransparent(true);
            rect.setLayoutX(start.getX());
            rect.setLayoutY(start.getY());
            rect.setStrokeWidth(3);
            rect.setWidth(bounds.getMaxX() - bounds.getMinX());
            rect.setHeight(bounds.getMaxY() - bounds.getMinY());
            getChildren().add(rect);
        }
        else {
            final Rectangle base = new Rectangle(width, height);
            final Rectangle rect = new Rectangle();
            rect.setLayoutX(start.getX());
            rect.setLayoutY(start.getY());
            rect.setStrokeWidth(3);
            rect.setWidth(bounds.getMaxX() - bounds.getMinX());
            rect.setHeight(bounds.getMaxY() - bounds.getMinY());
            final Shape shape = Shape.subtract(base, rect);
            shape.setMouseTransparent(false);
            shape.setFill(Color.BLACK);
            shape.setOpacity(0.7);
            getChildren().add(shape);
            final TitledPane pane = new TitledPane();
            pane.setCollapsible(false);
            pane.setText(node.toString());
            pane.setPrefHeight(60);
            pane.setPrefWidth(100);
            final Text label = new Text();
            label.setText("x:"+DisplayUtils.format(start.getX())+" y:"+DisplayUtils.format(start.getY())+"\nw:"+DisplayUtils.format(rect.getWidth())+" h:"+DisplayUtils.format(rect.getHeight()));
            pane.setContent(label);
            pane.setLayoutX(start.getX()+(rect.getWidth()/2)-(pane.getPrefWidth()/2));
            if(pane.getLayoutX()<0) {
                pane.setLayoutX(0);
            }
            if(start.getY()-pane.getPrefHeight()>=0) {
                pane.setLayoutY(start.getY()-pane.getPrefHeight());
            }
            else if(start.getY()+rect.getHeight()+pane.getPrefHeight()<=height) {
                pane.setLayoutY(start.getY()+rect.getHeight());
            }
            else {
                pane.setLayoutY(0);
            }
            getChildren().add(pane);
        }
        setManaged(false);
        setId(ScenicView.SCENIC_VIEW_BASE_ID + "componentHighLighter");
    }
    
}
