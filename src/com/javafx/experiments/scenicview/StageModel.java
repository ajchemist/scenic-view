package com.javafx.experiments.scenicview;

import java.util.*;

import javafx.beans.*;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.value.*;
import javafx.collections.ListChangeListener;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.*;

import com.javafx.experiments.scenicview.connector.*;
import com.javafx.experiments.scenicview.connector.event.AppEvent.SVEventType;
import com.javafx.experiments.scenicview.connector.event.*;
import com.javafx.experiments.scenicview.helper.StyleSheetRefresher;

public class StageModel {

    public static StageID STAGE_ID = new StageID(0, 0);

    private StyleSheetRefresher refresher;

    private Parent overlayParent;
    Parent target;
    Scene targetScene;
    public Window targetWindow;
    /**
     * Simplification for now, only a plain structure for now
     */
    final List<PopupWindow> popupWindows = new ArrayList<PopupWindow>();

    private final Rectangle boundsInParentRect;
    private final Rectangle layoutBoundsRect;
    private final Line baselineLine;
    private Rectangle componentSelector;
    private Node componentHighLighter;
    private RuleGrid grid;

    private Model2GUI model2gui;

    private final SubWindowChecker windowChecker;

    private final InvalidationListener targetScenePropListener;
    private final InvalidationListener targetWindowPropListener;
    private final InvalidationListener targetWindowSceneListener;

    private final InvalidationListener selectedNodePropListener;

    private final Map<Node, PropertyTracker> propertyTrackers = new HashMap<Node, PropertyTracker>();

    Configuration configuration = new Configuration();

    private final EventHandler<? super MouseEvent> sceneHoverListener = new EventHandler<MouseEvent>() {

        @Override public void handle(final MouseEvent ev) {
            highlightHovered(ev.getX(), ev.getY());
        }

    };
    

    /**
     * Listeners and EventHandlers
     */
    private final EventHandler<? super Event> traceEventHandler = new EventHandler<Event>() {

        @Override public void handle(final Event event) {
            if (configuration.isEventLogEnabled()) {
                model2gui.dispatchEvent(new EvLogEvent(getID(), new SVRealNodeAdapter((Node) event.getSource()), event.getEventType().toString(), ""));
            }
        }
    };
    
    private final ListChangeListener<Node> structureInvalidationListener;
    private final ChangeListener<Boolean> visibilityInvalidationListener;

    private Node previousHightLightedData;

    public StageModel(final Stage stage) {
        this(stage.getScene().getRoot());
    }

    public StageModel(final Parent target) {
        targetScenePropListener = new InvalidationListener() {
            @Override public void invalidated(final Observable value) {
                updateSceneDetails();
            }
        };

        targetWindowPropListener = new InvalidationListener() {
            @Override public void invalidated(final Observable value) {
                updateWindowDetails();
            }
        };
        targetWindowSceneListener = new InvalidationListener() {

            @Override public void invalidated(final Observable arg0) {
                if (targetScene.getRoot() == StageModel.this.target) {
                    setTarget(targetWindow.getScene().getRoot());
                    update();
                }
            }
        };
        selectedNodePropListener = new InvalidationListener() {
            @Override public void invalidated(final Observable arg0) {
                updateBoundsRects();
            }
        };

        visibilityInvalidationListener = new ChangeListener<Boolean>() {

            @Override public void changed(final ObservableValue<? extends Boolean> observable, final Boolean arg1, final Boolean newValue) {
                if (configuration.isAutoRefreshSceneGraph()) {
                    @SuppressWarnings("unchecked") final Node bean = (Node) ((Property<Boolean>) observable).getBean();
                    final boolean filteringActive = configuration.isVisibilityFilteringActive();
                    if (filteringActive && !newValue) {
                        removeNode(bean, false);
                    } else if (filteringActive && newValue) {
                        addNewNode(bean);
                    } else {
                        /**
                         * This should be improved ideally we use request a
                         * repaint for the TreeItem
                         */
                        removeNode(bean, false);
                        addNewNode(bean);
                    }
                    model2gui.dispatchEvent(new NodeCountEvent(getID(), DisplayUtils.getBranchCount(target)));
                }
            }
        };

        structureInvalidationListener = new ListChangeListener<Node>() {
            @Override public void onChanged(final Change<? extends Node> c) {
                if (configuration.isAutoRefreshSceneGraph()) {
                    while (c.next()) {
                        for (final Node dead : c.getRemoved()) {
                            final SVNode node = new SVRealNodeAdapter(dead);
                            model2gui.dispatchEvent(new EvLogEvent(getID(), node, EventLogPane.NODE_REMOVED, ""));
                            removeNode(dead, true);
                        }
                        for (final Node alive : c.getAddedSubList()) {
                            final SVNode node = new SVRealNodeAdapter(alive);
                            model2gui.dispatchEvent(new EvLogEvent(getID(), node, EventLogPane.NODE_ADDED, ""));
                            
                            addNewNode(alive);
                        }
                    }
                    model2gui.dispatchEvent(new NodeCountEvent(getID(), DisplayUtils.getBranchCount(target)));
                }
            }
        };

        windowChecker = new SubWindowChecker(this);
        windowChecker.start();
        boundsInParentRect = new Rectangle();
        boundsInParentRect.setId(ScenicView.SCENIC_VIEW_BASE_ID + "boundsInParentRect");
        boundsInParentRect.setFill(Color.YELLOW);
        boundsInParentRect.setOpacity(.5);
        boundsInParentRect.setManaged(false);
        boundsInParentRect.setMouseTransparent(true);
        layoutBoundsRect = new Rectangle();
        layoutBoundsRect.setId(ScenicView.SCENIC_VIEW_BASE_ID + "layoutBoundsRect");
        layoutBoundsRect.setFill(null);
        layoutBoundsRect.setStroke(Color.GREEN);
        layoutBoundsRect.setStrokeType(StrokeType.INSIDE);
        layoutBoundsRect.setOpacity(.8);
        layoutBoundsRect.getStrokeDashArray().addAll(3.0, 3.0);
        layoutBoundsRect.setStrokeWidth(1);
        layoutBoundsRect.setManaged(false);
        layoutBoundsRect.setMouseTransparent(true);
        baselineLine = new Line();
        baselineLine.setId(ScenicView.SCENIC_VIEW_BASE_ID + "baselineLine");
        baselineLine.setStroke(Color.RED);
        baselineLine.setOpacity(.75);
        baselineLine.setStrokeWidth(1);
        baselineLine.setManaged(false);
        this.target = target;
    }

    private void startRefresher() {
        refresher = new StyleSheetRefresher(targetScene);
    }

    private void updateBoundsRects(final Node selectedNode) {
        /**
         * By node layout bounds only on main scene not on popups
         */
        if (selectedNode != null && selectedNode.getScene() == targetScene) {
            updateRect(selectedNode, selectedNode.getBoundsInParent(), 0, 0, boundsInParentRect);
            updateRect(selectedNode, selectedNode.getLayoutBounds(), selectedNode.getLayoutX(), selectedNode.getLayoutY(), layoutBoundsRect);
            boundsInParentRect.setVisible(true);
            layoutBoundsRect.setVisible(true);
        } else {
            boundsInParentRect.setVisible(false);
            layoutBoundsRect.setVisible(false);
        }
    }

    public void close() {
        removeScenicViewComponents(target);
        if (targetScene != null) {
            targetScene.removeEventHandler(MouseEvent.MOUSE_MOVED, sceneHoverListener);
        }
        if (refresher != null)
            refresher.finish();
        if (windowChecker != null)
            windowChecker.finish();
    }

    private void removeScenicViewComponents(final Node target) {
        /**
         * We should any component associated with ScenicView on close
         */
        if (target instanceof Parent) {
            if (target instanceof Group) {
                final List<Node> nodes = ((Group) target).getChildren();
                for (final Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
                    final Node node = iterator.next();
                    if (node.getId() != null && node.getId().startsWith(ScenicView.SCENIC_VIEW_BASE_ID)) {
                        iterator.remove();
                    }
                }
            }
            if (target instanceof Pane) {
                final List<Node> nodes = ((Pane) target).getChildren();
                for (final Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
                    final Node node = iterator.next();
                    if (node.getId() != null && node.getId().startsWith(ScenicView.SCENIC_VIEW_BASE_ID)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void update() {
        updateListeners(target, true, false);
        model2gui.updateStageModel(this);
    }

    public void updateSceneDetails() {
        // hack, since we can't listen for a STAGE prop change on scene
        model2gui.dispatchEvent(new SceneDetailsEvent(getID(), DisplayUtils.getBranchCount(target), targetScene != null ? targetScene.getWidth() + " x " + targetScene.getHeight() : ""));
        if (targetScene != null && targetWindow == null) {
            setTargetWindow(targetScene.getWindow());
        }
    }

    private void setTargetWindow(final Window value) {
        if (targetWindow != null) {
            targetWindow.xProperty().removeListener(targetWindowPropListener);
            targetWindow.yProperty().removeListener(targetWindowPropListener);
            targetWindow.widthProperty().removeListener(targetWindowPropListener);
            targetWindow.heightProperty().removeListener(targetWindowPropListener);
            targetWindow.focusedProperty().removeListener(targetWindowPropListener);
            targetWindow.sceneProperty().removeListener(targetWindowSceneListener);
        }
        targetWindow = value;
        if (targetWindow != null) {
            targetWindow.xProperty().addListener(targetWindowPropListener);
            targetWindow.yProperty().addListener(targetWindowPropListener);
            targetWindow.widthProperty().addListener(targetWindowPropListener);
            targetWindow.heightProperty().addListener(targetWindowPropListener);
            targetWindow.focusedProperty().addListener(targetWindowPropListener);
            targetWindow.sceneProperty().addListener(targetWindowSceneListener);
        }
        updateWindowDetails();
    }

    private void updateWindowDetails() {
        if (targetWindow != null) {
            model2gui.dispatchEvent(new WindowDetailsEvent(getID(), targetWindow.getClass().getSimpleName(), DisplayUtils.boundsToString(targetWindow.getX(), targetWindow.getY(), targetWindow.getWidth(), targetWindow.getHeight()), targetWindow.isFocused(), canStylesheetsBeRefreshed()));
        } else {
            model2gui.dispatchEvent(new WindowDetailsEvent(getID(), null, "", false, false));
        }
    }

    void setTarget(final Parent value) {
        // Find parent we can use to hang bounds rectangles
        this.target = value;
        if (overlayParent != null) {
            removeFromNode(overlayParent, boundsInParentRect);
            removeFromNode(overlayParent, layoutBoundsRect);
            removeFromNode(overlayParent, baselineLine);
        }
        overlayParent = findFertileParent(value);
        if (overlayParent == null) {
            System.out.println("warning: could not find writable parent to add overlay nodes; overlays disabled.");
            /**
             * This should be improved
             */
            configuration.setShowBounds(false);
            updateBoundsRects();
            configuration.setShowBounds(true);
            configuration.setShowBaseline(false);
            updateBaseline();
            configuration.setShowBaseline(true);

        } else {
            addToNode(overlayParent, boundsInParentRect);
            addToNode(overlayParent, layoutBoundsRect);
            addToNode(overlayParent, baselineLine);
        }
        setTargetScene(target.getScene());
    }

    private void updateRect(final Node node, final Bounds bounds, final double tx, final double ty, final Rectangle rect) {
        final Bounds b = toSceneBounds(node, bounds, tx, ty);
        rect.setX(b.getMinX());
        rect.setY(b.getMinY());
        rect.setWidth(b.getMaxX() - b.getMinX());
        rect.setHeight(b.getMaxY() - b.getMinY());
    }

    private Bounds toSceneBounds(final Node node, final Bounds bounds, final double tx, final double ty) {
        final Parent parent = node.getParent();
        if (parent != null) {
            // need to translate position
            final Point2D pt = overlayParent.sceneToLocal(node.getParent().localToScene(bounds.getMinX(), bounds.getMinY()));
            return new BoundingBox(snapPosition(pt.getX()) + snapPosition(tx), snapPosition(pt.getY()) + snapPosition(ty), snapSize(bounds.getWidth()), snapSize(bounds.getHeight()));
        } else {
            // selected node is root
            return new BoundingBox(snapPosition(bounds.getMinX()) + snapPosition(tx) + 1, snapPosition(bounds.getMinY()) + snapPosition(ty) + 1, snapSize(bounds.getWidth()) - 2, snapSize(bounds.getHeight()) - 2);
        }
    }

    private double snapPosition(final double pos) {
        return pos;
    }

    private double snapSize(final double pos) {
        return pos;
    }

    private void addToNode(final Parent parent, final Node node) {
        if (parent instanceof Group) {
            ((Group) parent).getChildren().add(node);
        } else if (parent instanceof ScenicView) {
            ((ScenicView) parent).getChildren().add(node);
        } else { // instanceof Pane
            ((Pane) parent).getChildren().add(node);
        }
    }

    private void removeFromNode(final Parent parent, final Node node) {
        if (parent instanceof Group) {
            ((Group) parent).getChildren().remove(node);
        } else if (parent instanceof ScenicView) {
            ((ScenicView) parent).getChildren().remove(node);
        } else { // instanceof Pane
            ((Pane) parent).getChildren().remove(node);
        }
    }

    public void componentSelectOnClick(final boolean newValue) {
        if (newValue) {
            targetScene.addEventHandler(MouseEvent.MOUSE_MOVED, sceneHoverListener);
            final Rectangle rect = new Rectangle();
            rect.setFill(Color.TRANSPARENT);
            rect.setWidth(targetWindow.getWidth());
            rect.setHeight(targetWindow.getHeight());
            rect.setId(ScenicView.SCENIC_VIEW_BASE_ID + "componentSelectorRect");
            rect.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(final MouseEvent ev) {
                    model2gui.dispatchEvent(new NodeSelectedEvent(getID(), new SVRealNodeAdapter(getHoveredNode(ev.getX(), ev.getY()))));

                }
            });
            rect.setManaged(false);
            componentSelector = rect;
            addToNode(target, componentSelector);
            ((Stage) targetWindow).toFront();
        } else {
            targetScene.removeEventHandler(MouseEvent.MOUSE_MOVED, sceneHoverListener);
            if (componentHighLighter != null) {
                removeFromNode(target, componentHighLighter);
            }
            if (componentSelector != null)
                removeFromNode(target, componentSelector);
        }
    }

    private void showGrid(final boolean newValue, final int gap) {
        if (newValue) {
            grid = new RuleGrid(gap, targetScene.getWidth(), targetScene.getHeight());
            grid.setId(ScenicView.SCENIC_VIEW_BASE_ID + "ruler");
            grid.setManaged(false);
            addToNode(target, grid);
        } else {
            if (grid != null) {
                removeFromNode(target, grid);
                grid = null;
            }
        }
    }

    private Parent findFertileParent(final Parent p) {
        Parent fertile = (p instanceof Group || p instanceof Pane) ? p : null;
        if (fertile == null) {
            for (final Node child : p.getChildrenUnmodifiable()) {
                if (child instanceof Parent) {
                    fertile = findFertileParent((Parent) child);
                }
            }
        }
        return fertile; // could be null!
    }

    private void updateBaseline(final boolean show, final Point2D orig, final double width) {
        if (show) {
            final Point2D pt = overlayParent.sceneToLocal(orig);
            baselineLine.setStartX(pt.getX());
            baselineLine.setStartY(pt.getY());
            baselineLine.setEndX(pt.getX() + width);
            baselineLine.setEndY(pt.getY());
            baselineLine.setVisible(true);
        } else {
            baselineLine.setVisible(false);
        }
    }

    private void setTargetScene(final Scene value) {

        if (targetScene != null) {
            targetScene.widthProperty().removeListener(targetScenePropListener);
            targetScene.heightProperty().removeListener(targetScenePropListener);
        }
        targetScene = value;
        if (targetScene != null) {
            setTargetWindow(targetScene.getWindow());
            targetScene.widthProperty().addListener(targetScenePropListener);
            targetScene.heightProperty().addListener(targetScenePropListener);
            targetScene.setOnMouseMoved(new EventHandler<MouseEvent>() {

                @Override public void handle(final MouseEvent ev) {
                    model2gui.dispatchEvent(new MousePosEvent(STAGE_ID, (int) ev.getSceneX() + "x" + (int) ev.getSceneY()));
                }
            });
            final boolean canBeRefreshed = StyleSheetRefresher.canStylesBeRefreshed(targetScene);

            if (refresher == null || refresher.getScene() != value) {
                if (refresher != null)
                    refresher.finish();
                if (canBeRefreshed && configuration.isAutoRefreshStyles())
                    startRefresher();
            }
        }
        updateSceneDetails();
    }

    private void highlightHovered(final double x, final double y) {
        final Node nodeData = getHoveredNode(x, y);
        if (previousHightLightedData != nodeData) {
            previousHightLightedData = null;
            if (componentHighLighter != null) {
                removeFromNode(target, componentHighLighter);
            }
            if (nodeData != null) {
                // TODO Change this
                componentHighLighter = new ComponentHighLighter(new SVRealNodeAdapter(nodeData), targetWindow != null ? targetWindow.getWidth() : -1, targetWindow != null ? targetWindow.getHeight() : -1, toSceneBounds(nodeData, nodeData.getBoundsInParent(), 0, 0));
                addToNode(target, componentHighLighter);
            }
        }

    }

    private Node getHoveredNode(final double x, final double y) {
//        final List<TreeItem<SVNode>> infos = model2gui.getTreeItems();
//        for (int i = infos.size() - 1; i >= 0; i--) {
//            final SVNode info = infos.get(i).getValue();
//            /**
//             * Discard filtered nodes
//             */
//            if (!info.isInvalidForFilter()) {
//                final Point2D localPoint = info.getImpl().sceneToLocal(x, y);
//                if (info.getImpl().contains(localPoint)) {
//                    /**
//                     * Mouse Transparent nodes can be ignored
//                     */
//                    final boolean selectable = !model2gui.isIgnoreMouseTransparent() || !info.isMouseTransparent();
//                    if (selectable) {
//                        return infos.get(i);
//                    }
//                }
//            }
//        }
//        return null;

        return getHoveredNode(target, x, y);
    }
    
    private Node getHoveredNode(final Node target, final double x, final double y) {
        if(target.getId() != null && target.getId().startsWith(ScenicView.SCENIC_VIEW_BASE_ID)) return null;
        if(target instanceof Parent) {
            final List<Node> childrens = ((Parent)target).getChildrenUnmodifiable();
            for (int i = childrens.size() - 1; i >= 0; i--) {
                final Node node = childrens.get(i);
                final Node hovered = getHoveredNode(node, x, y);
                if(hovered != null) return hovered;
            }
        }
        final Point2D localPoint = target.sceneToLocal(x, y);
        if (target.contains(localPoint)) {
            if (!configuration.isIgnoreMouseTransparent() || !target.isMouseTransparent()) {
                return target;
            }
        }

        return null;
    }

    public void setModel2gui(final Model2GUI model2gui) {
        this.model2gui = model2gui;
        setTarget(target);
        update();
    }

    private boolean canStylesheetsBeRefreshed() {
        return StyleSheetRefresher.canStylesBeRefreshed(targetScene);
    }

    public void configurationUpdated(final Configuration configuration) {
        if (configuration.isShowBaseline() != this.configuration.isShowBaseline()) {
            this.configuration.setShowBaseline(configuration.isShowBaseline());
            updateBaseline();
        }
        if (configuration.isShowBounds() != this.configuration.isShowBounds()) {
            this.configuration.setShowBounds(configuration.isShowBounds());
            updateBoundsRects();
        }
        if(configuration.isShowRuler() != this.configuration.isShowRuler()) {
            showGrid(configuration.isShowRuler(), configuration.getRulerSeparation());
            this.configuration.setShowRuler(configuration.isShowRuler());
            this.configuration.setRulerSeparation(configuration.getRulerSeparation());
        }
        else if(configuration.getRulerSeparation()!= this.configuration.getRulerSeparation() && grid != null) {
            grid.updateSeparation(configuration.getRulerSeparation());
            this.configuration.setRulerSeparation(configuration.getRulerSeparation());
        }
        if(configuration.isAutoRefreshStyles() != this.configuration.isAutoRefreshStyles()) {
            this.configuration.setAutoRefreshStyles(configuration.isAutoRefreshStyles());
            if (this.configuration.isAutoRefreshStyles()) {
                startRefresher();
            } else {
                refresher.finish();
            }
        }
        if(configuration.isAutoRefreshSceneGraph() != this.configuration.isAutoRefreshSceneGraph()) {
            this.configuration.setAutoRefreshSceneGraph(configuration.isAutoRefreshSceneGraph());
            if (this.configuration.isAutoRefreshSceneGraph()) {
                update();
            }
        }
        this.configuration.setEventLogEnabled(configuration.isEventLogEnabled());
        this.configuration.setIgnoreMouseTransparent(configuration.isIgnoreMouseTransparent());
        this.configuration.setCollapseContentControls(configuration.isCollapseContentControls());
        this.configuration.setCollapseControls(configuration.isCollapseControls());
        this.configuration.setVisibilityFilteringActive(configuration.isVisibilityFilteringActive());
    }

    public void setSelectedNode(final SVNode svRealNodeAdapter) {
        final Node old = selectedNode;
        if (old != null) {
            old.boundsInParentProperty().removeListener(selectedNodePropListener);
            old.layoutBoundsProperty().removeListener(selectedNodePropListener);
        }
        if (svRealNodeAdapter != null) {
            this.selectedNode = svRealNodeAdapter.getImpl();
            if (selectedNode != null) {
                selectedNode.boundsInParentProperty().addListener(selectedNodePropListener);
                selectedNode.layoutBoundsProperty().addListener(selectedNodePropListener);
            }
        } else {
            selectedNode = null;
        }
        updateBoundsRects();
        updateBaseline();
    }

    private Node selectedNode;

    private void updateBaseline() {
        if (this.configuration.isShowBaseline() && selectedNode != null) {
            final double baseline = selectedNode.getBaselineOffset();
            final Bounds bounds = selectedNode.getLayoutBounds();
            updateBaseline(true, selectedNode.localToScene(bounds.getMinX(), bounds.getMinY() + baseline), bounds.getWidth());

        } else {
            updateBaseline(false, null, 0);
        }
    }

    private void updateBoundsRects() {
        if (this.configuration.isShowBounds()) {
            updateBoundsRects(selectedNode);
        } else {
            updateBoundsRects(null);
        }
    }

    void propertyTracker(final Node node, final boolean add) {
        PropertyTracker tracker = propertyTrackers.remove(node);
        if (tracker != null) {
            tracker.clear();
        }
        if (add && configuration.isEventLogEnabled()) {
            tracker = new PropertyTracker() {

                @Override protected void updateDetail(final String propertyName, @SuppressWarnings("rawtypes") final ObservableValue property) {
                    /**
                     * Remove the bean
                     */
                    model2gui.dispatchEvent(new EvLogEvent(STAGE_ID, new SVRealNodeAdapter(node), EventLogPane.PROPERTY_CHANGED, propertyName + "=" + property.getValue()));
                }
            };
            tracker.setTarget(node);
            propertyTrackers.put(node, tracker);
        }
    }

    public StageID getID() {
        return STAGE_ID;
    }

    public void placeStage(final Stage stage) {
        if (targetWindow != null) {
            stage.setX(targetWindow.getX() + targetWindow.getWidth());
            stage.setY(targetWindow.getY());
            try {
                // Prevents putting the stage out of the screen
                final Screen primary = Screen.getPrimary();
                if (primary != null) {
                    final Rectangle2D rect = primary.getVisualBounds();
                    if (stage.getX() + stage.getWidth() > rect.getMaxX()) {
                        stage.setX(rect.getMaxX() - stage.getWidth());
                    }
                    if (stage.getY() + stage.getHeight() > rect.getMaxY()) {
                        stage.setX(rect.getMaxY() - stage.getHeight());
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void addNewNode(final Node node) {
        updateListeners(node, true, false);
        boolean mustBeExpanded = !(node instanceof Control) || !configuration.isCollapseControls();
        if (!mustBeExpanded && !configuration.isCollapseContentControls()) {
            mustBeExpanded = node instanceof TabPane || node instanceof SplitPane || node instanceof ScrollPane || node instanceof Accordion || node instanceof TitledPane;
        }
        model2gui.dispatchEvent(new NodeAddRemoveEvent(SVEventType.NODE_ADDED, getID(), new SVRealNodeAdapter(node)));
    }
    
    private void removeNode(final Node node, final boolean removeVisibilityListener) {
        updateListeners(node, false, removeVisibilityListener);
        model2gui.dispatchEvent(new NodeAddRemoveEvent(SVEventType.NODE_REMOVED, getID(), new SVRealNodeAdapter(node)));
    }
    
    private void updateListeners(final Node node, final boolean add, final boolean removeVisibilityListener) {
        if(add) {
            if (node.getId() == null || !node.getId().startsWith(ScenicView.SCENIC_VIEW_BASE_ID)) {
                node.visibleProperty().removeListener(visibilityInvalidationListener);
                node.visibleProperty().addListener(visibilityInvalidationListener);
                propertyTracker(node, true);

                node.removeEventFilter(Event.ANY, traceEventHandler);
                if (configuration.isEventLogEnabled())
                    node.addEventFilter(Event.ANY, traceEventHandler);
                if (node instanceof Parent) {
                    ((Parent) node).getChildrenUnmodifiable().removeListener(structureInvalidationListener);
                    ((Parent) node).getChildrenUnmodifiable().addListener(structureInvalidationListener);
                    final List<Node> childrens = ((Parent) node).getChildrenUnmodifiable();
                    for (int i = 0; i < childrens.size(); i++) {
                        updateListeners(childrens.get(i), add, removeVisibilityListener);
                    }
                }
            }
        }
        else {
            if (node instanceof Parent) {
                ((Parent) node).getChildrenUnmodifiable().removeListener(structureInvalidationListener);
                final List<Node> childrens = ((Parent) node).getChildrenUnmodifiable();
                for (int i = 0; i < childrens.size(); i++) {
                    updateListeners(childrens.get(i), add, removeVisibilityListener);
                }
            }
            if (node != null && removeVisibilityListener) {
                node.visibleProperty().removeListener(visibilityInvalidationListener);
                propertyTracker(node, false);
                node.removeEventFilter(Event.ANY, traceEventHandler);
            }
        }
    }
}
