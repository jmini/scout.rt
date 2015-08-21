package org.eclipse.scout.rt.ui.html.json.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.dnd.ResourceListTransferObject;
import org.eclipse.scout.commons.resource.BinaryResource;
import org.eclipse.scout.rt.client.ui.AbstractEventBuffer;
import org.eclipse.scout.rt.client.ui.IDNDSupport;
import org.eclipse.scout.rt.client.ui.MouseButton;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.IContextMenu;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeVisitor;
import org.eclipse.scout.rt.client.ui.basic.tree.IVirtualTreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeAdapter;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeListener;
import org.eclipse.scout.rt.ui.html.IUiSession;
import org.eclipse.scout.rt.ui.html.UiException;
import org.eclipse.scout.rt.ui.html.json.AbstractJsonPropertyObserver;
import org.eclipse.scout.rt.ui.html.json.IJsonAdapter;
import org.eclipse.scout.rt.ui.html.json.JsonEvent;
import org.eclipse.scout.rt.ui.html.json.JsonObjectUtility;
import org.eclipse.scout.rt.ui.html.json.JsonProperty;
import org.eclipse.scout.rt.ui.html.json.action.DisplayableActionFilter;
import org.eclipse.scout.rt.ui.html.json.menu.IJsonContextMenuOwner;
import org.eclipse.scout.rt.ui.html.json.menu.JsonContextMenu;
import org.eclipse.scout.rt.ui.html.res.BinaryResourceUrlUtility;
import org.eclipse.scout.rt.ui.html.res.IBinaryResourceConsumer;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonTree<TREE extends ITree> extends AbstractJsonPropertyObserver<TREE> implements IJsonContextMenuOwner, IBinaryResourceConsumer {

  public static final String EVENT_NODES_INSERTED = "nodesInserted";
  public static final String EVENT_NODES_UPDATED = "nodesUpdated";
  public static final String EVENT_NODES_DELETED = "nodesDeleted";
  public static final String EVENT_ALL_CHILD_NODES_DELETED = "allChildNodesDeleted";
  public static final String EVENT_NODES_SELECTED = "nodesSelected";
  public static final String EVENT_NODE_CLICKED = "nodeClicked";
  public static final String EVENT_NODE_ACTION = "nodeAction";
  public static final String EVENT_NODE_EXPANDED = "nodeExpanded";
  public static final String EVENT_NODE_CHANGED = "nodeChanged";
  public static final String EVENT_CHILD_NODE_ORDER_CHANGED = "childNodeOrderChanged";
  public static final String EVENT_NODES_CHECKED = "nodesChecked";
  public static final String EVENT_REQUEST_FOCUS = "requestFocus";
  public static final String EVENT_SCROLL_TO_SELECTION = "scrollToSelection";

  public static final String PROP_NODE_ID = "nodeId";
  public static final String PROP_NODE_IDS = "nodeIds";
  public static final String PROP_COMMON_PARENT_NODE_ID = "commonParentNodeId";
  public static final String PROP_NODE = "node";
  public static final String PROP_NODES = "nodes";
  public static final String PROP_EXPANDED = "expanded";
  public static final String PROP_SELECTED_NODES = "selectedNodes";

  private TreeListener m_treeListener;
  private final Map<String, ITreeNode> m_treeNodes;
  private final Map<ITreeNode, String> m_treeNodeIds;
  private final TreeEventFilter m_treeEventFilter;
  private final AbstractEventBuffer<TreeEvent> m_eventBuffer;

  public JsonTree(TREE model, IUiSession uiSession, String id, IJsonAdapter<?> parent) {
    super(model, uiSession, id, parent);
    m_treeNodes = new HashMap<>();
    m_treeNodeIds = new HashMap<>();
    m_treeEventFilter = new TreeEventFilter(getModel());
    m_eventBuffer = model.createEventBuffer();
  }

  @Override
  public String getObjectType() {
    return "Tree";
  }

  @Override
  protected void initJsonProperties(TREE model) {
    super.initJsonProperties(model);
    putJsonProperty(new JsonProperty<TREE>(ITree.PROP_TITLE, model) {
      @Override
      protected String modelValue() {
        return getModel().getTitle();
      }
    });
    putJsonProperty(new JsonProperty<TREE>(ITree.PROP_CHECKABLE, model) {
      @Override
      protected Boolean modelValue() {
        return getModel().isCheckable();
      }
    });
    putJsonProperty(new JsonProperty<TREE>(ITree.PROP_MULTI_CHECK, model) {
      @Override
      protected Boolean modelValue() {
        return getModel().isMultiCheck();
      }
    });
    putJsonProperty(new JsonProperty<TREE>(ITree.PROP_ENABLED, model) {
      @Override
      protected Boolean modelValue() {
        return getModel().isEnabled();
      }
    });
    putJsonProperty(new JsonProperty<TREE>(ITree.PROP_AUTO_CHECK_CHILDREN, model) {
      @Override
      protected Boolean modelValue() {
        return getModel().isAutoCheckChildNodes();
      }
    });
    putJsonProperty(new JsonProperty<ITree>(ITree.PROP_SCROLL_TO_SELECTION, model) {
      @Override
      protected Boolean modelValue() {
        return getModel().isScrollToSelection();
      }
    });
    putJsonProperty(new JsonProperty<ITree>(ITree.PROP_DROP_TYPE, model) {
      @Override
      protected Integer modelValue() {
        return getModel().getDropType();
      }
    });
    putJsonProperty(new JsonProperty<ITree>(ITree.PROP_DROP_MAXIMUM_SIZE, model) {
      @Override
      protected Long modelValue() {
        return getModel().getDropMaximumSize();
      }
    });
  }

  @Override
  protected void attachChildAdapters() {
    super.attachChildAdapters();
    attachAdapter(getModel().getContextMenu(), new DisplayableActionFilter<IMenu>());
    attachAdapters(getModel().getKeyStrokes());
    attachNodes(getTopLevelNodes(), true);
  }

  @Override
  protected void disposeChildAdapters() {
    super.disposeChildAdapters();
    disposeAllNodes();
  }

  @Override
  protected void attachModel() {
    super.attachModel();
    if (m_treeListener != null) {
      throw new IllegalStateException();
    }
    m_treeListener = new P_TreeListener();
    getModel().addUITreeListener(m_treeListener);
  }

  @Override
  protected void detachModel() {
    super.detachModel();
    if (m_treeListener == null) {
      throw new IllegalStateException();
    }
    getModel().removeTreeListener(m_treeListener);
    m_treeListener = null;
  }

  protected void attachNode(ITreeNode node, boolean attachChildren) {
  }

  protected void attachNodes(Collection<ITreeNode> nodes, boolean attachChildren) {
    for (ITreeNode node : nodes) {
      if (!isNodeAccepted(node)) {
        continue;
      }
      attachNode(node, attachChildren);
    }
  }

  /**
   * Removes all node mappings without querying the model.
   */
  protected void disposeAllNodes() {
    m_treeNodeIds.clear();
    m_treeNodes.clear();
  }

  protected void disposeNode(ITreeNode node, boolean disposeChildren) {
    if (disposeChildren) {
      disposeNodes(node.getChildNodes(), disposeChildren);
    }
    String nodeId = m_treeNodeIds.get(node);
    m_treeNodeIds.remove(node);
    m_treeNodes.remove(nodeId);
  }

  protected void disposeNodes(Collection<ITreeNode> nodes, boolean disposeChildren) {
    for (ITreeNode node : nodes) {
      disposeNode(node, disposeChildren);
    }
  }

  @Override
  public JSONObject toJson() {
    JSONObject json = super.toJson();
    JSONArray jsonNodes = new JSONArray();
    for (ITreeNode childNode : getTopLevelNodes()) {
      if (!isNodeAccepted(childNode)) {
        continue;
      }
      jsonNodes.put(treeNodeToJson(childNode));
    }
    putProperty(json, PROP_NODES, jsonNodes);
    putProperty(json, PROP_SELECTED_NODES, nodeIdsToJson(getModel().getSelectedNodes(), true, true));
    putContextMenu(json);
    putAdapterIdsProperty(json, "keyStrokes", getModel().getKeyStrokes());
    return json;
  }

  protected void putContextMenu(JSONObject json) {
    JsonContextMenu<IContextMenu> jsonContextMenu = getAdapter(getModel().getContextMenu());
    if (jsonContextMenu != null) {
      json.put(PROP_MENUS, jsonContextMenu.childActionsToJson());
    }
  }

  protected void handleModelTreeEvent(TreeEvent event) {
    event = m_treeEventFilter.filter(event);
    if (event == null) {
      return;
    }
    // Add event to buffer instead of handling it immediately. (This allows coalescing the events at JSON response level.)
    bufferModelEvent(event);
    registerAsBufferedEventsAdapter();
  }

  protected void bufferModelEvent(final TreeEvent event) {
    switch (event.getType()) {
      case TreeEvent.TYPE_NODE_FILTER_CHANGED: {
        // Convert the "filter changed" event to a NODES_DELETED and a NODES_INSERTED event. This prevents sending unnecessary
        // data to the UI. We convert the event before adding it to the event buffer to allow coalescing on UI-level.
        // NOTE: This may lead to a temporary inconsistent situation, where node events exist in the buffer after the
        // node itself is deleted. This is because the node is not really deleted from the model. However, when processing
        // the buffered events, the "wrong" events will be ignored and everything is fixed again.
        final List<ITreeNode> nodesToInsert = new ArrayList<>();
        final List<ITreeNode> nodesToDelete = new ArrayList<>();
        getModel().visitTree(new ITreeVisitor() {

          @Override
          public boolean visit(ITreeNode node) {
            if (isInvisibleRootNode(node)) {
              return true;
            }

            String existingNodeId = getNodeId(node);
            if (node.isFilterAccepted()) {
              if (existingNodeId == null) {
                // Node is not filtered but JsonTree does not know it yet --> handle as insertion event
                nodesToInsert.add(node);
              }
            }
            else if (!node.isRejectedByUser()) {
              if (existingNodeId != null) {
                // Node is filtered, but JsonTree has it in its list --> handle as deletion event
                nodesToDelete.add(node);
              }
            }
            return true;
          }
        });
        m_eventBuffer.add(new TreeEvent(event.getTree(), TreeEvent.TYPE_NODES_DELETED, nodesToDelete));
        m_eventBuffer.add(new TreeEvent(event.getTree(), TreeEvent.TYPE_NODES_INSERTED, nodesToInsert));

        break;
      }
      default: {
        m_eventBuffer.add(event);
      }
    }
  }

  @Override
  public void processBufferedEvents() {
    if (m_eventBuffer.isEmpty()) {
      return;
    }
    List<TreeEvent> coalescedEvents = m_eventBuffer.consumeAndCoalesceEvents();
    for (TreeEvent event : coalescedEvents) {
      processBufferedEvent(event);
    }
  }

  protected void processBufferedEvent(TreeEvent event) {
    switch (event.getType()) {
      case TreeEvent.TYPE_NODES_INSERTED:
        handleModelNodesInserted(event);
        break;
      case TreeEvent.TYPE_NODES_UPDATED:
        handleModelNodesUpdated(event);
        break;
      case TreeEvent.TYPE_NODES_DELETED:
        handleModelNodesDeleted(event);
        break;
      case TreeEvent.TYPE_ALL_CHILD_NODES_DELETED:
        handleModelAllChildNodesDeleted(event);
        break;
      case TreeEvent.TYPE_NODE_EXPANDED:
      case TreeEvent.TYPE_NODE_COLLAPSED:
        if (!isInvisibleRootNode(event.getNode())) { // Not necessary to send events for invisible root node
          handleModelNodeExpanded(event.getNode(), false);
        }
        break;
      case TreeEvent.TYPE_NODE_EXPANDED_RECURSIVE:
      case TreeEvent.TYPE_NODE_COLLAPSED_RECURSIVE:
        if (isInvisibleRootNode(event.getNode())) { // Send event for all child nodes
          for (ITreeNode childNode : event.getNode().getChildNodes()) {
            handleModelNodeExpanded(childNode, true);
          }
        }
        else {
          handleModelNodeExpanded(event.getNode(), true);
        }
        break;
      case TreeEvent.TYPE_NODES_SELECTED:
        handleModelNodesSelected(event.getNodes());
        break;
      case TreeEvent.TYPE_NODES_CHECKED:
        handleModelNodesChecked(event.getNodes());
        break;
      case TreeEvent.TYPE_NODE_CHANGED:
        handleModelNodeChanged(event.getNode());
        break;
      case TreeEvent.TYPE_NODE_FILTER_CHANGED:
        // See special handling in bufferModelEvent()
        throw new IllegalStateException("Unsupported event type: " + event);
      case TreeEvent.TYPE_CHILD_NODE_ORDER_CHANGED:
        handleModelChildNodeOrderChanged(event);
        break;
      case TreeEvent.TYPE_REQUEST_FOCUS:
        handleModelRequestFocus(event);
        break;
      case TreeEvent.TYPE_SCROLL_TO_SELECTION:
        handleModelScrollToSelection(event);
        break;
      default:
        handleModelOtherTreeEvent(event);
        break;
    }
    // TODO Tree | Events not yet implemented:
    // - TYPE_NODE_REQUEST_FOCUS
    // - TYPE_NODE_ENSURE_VISIBLE what is the difference to scroll_to_selection? delete in treeevent
    // - TYPE_NODES_DRAG_REQUEST
    // - TYPE_DRAG_FINISHED
    // - TYPE_NODE_DROP_ACTION, partly implemented with consumeBinaryResource(...)
    // - TYPE_NODE_DROP_TARGET_CHANGED
  }

  /**
   * Default impl. does nothing. Override this method to handle custom tree-events.
   */
  protected void handleModelOtherTreeEvent(TreeEvent event) {
  }

  protected void handleModelNodeExpanded(ITreeNode modelNode, boolean recursive) {
    if (!isNodeAccepted(modelNode)) {
      return;
    }
    String nodeId = getNodeId(modelNode);
    if (nodeId == null) { // Ignore nodes that are not yet sent to the UI (may happen due to asynchronous event processing)
      return;
    }
    JSONObject jsonEvent = JsonObjectUtility.newOrderedJSONObject();
    putProperty(jsonEvent, PROP_NODE_ID, nodeId);
    putProperty(jsonEvent, PROP_EXPANDED, modelNode.isExpanded());
    putProperty(jsonEvent, "recursive", recursive);
    addActionEvent(EVENT_NODE_EXPANDED, jsonEvent);
  }

  protected void handleModelNodesInserted(TreeEvent event) {
    JSONArray jsonNodes = new JSONArray();
    attachNodes(event.getNodes(), true);//FIXME CGU why not inside loop? attaching for rejected nodes?
    for (ITreeNode node : event.getNodes()) {
      if (!isNodeAccepted(node)) {
        continue;
      }
      jsonNodes.put(treeNodeToJson(node));
    }
    if (jsonNodes.length() == 0) {
      return;
    }
    JSONObject jsonEvent = JsonObjectUtility.newOrderedJSONObject();
    putProperty(jsonEvent, PROP_NODES, jsonNodes);
    putProperty(jsonEvent, PROP_COMMON_PARENT_NODE_ID, getOrCreateNodeId(event.getCommonParentNode()));
    addActionEvent(EVENT_NODES_INSERTED, jsonEvent);
  }

  protected void handleModelNodesUpdated(TreeEvent event) {
    JSONArray jsonNodes = new JSONArray();
    for (ITreeNode node : event.getNodes()) {
      if (!isNodeAccepted(node)) {
        continue;
      }
      String nodeId = getNodeId(node);
      if (nodeId == null) { // Ignore nodes that are not yet sent to the UI (may happen due to asynchronous event processing)
        continue;
      }
      JSONObject jsonNode = JsonObjectUtility.newOrderedJSONObject();
      putProperty(jsonNode, "id", nodeId);
      // Only send _some_ of the properties. Everything else (e.g. "checked", "expanded") will be handled with separate events.
      // --> See also: Tree.js/_onNodesUpdated()
      putProperty(jsonNode, "leaf", node.isLeaf());
      putProperty(jsonNode, "enabled", node.isEnabled());

      // Check for virtual nodes that were replaces with real nodes (this will have triggered an NODES_UPDATED event).
      // This would not really be necessary, as both nodes are considered "equal" (see implementation of VirtualTreeNode),
      // but some properties have to be updated in the UI, therefore we replace the nodes in our internal maps.
      ITreeNode cachedNode = getNode(nodeId);
      if (cachedNode instanceof IVirtualTreeNode && cachedNode != node) {
        m_treeNodeIds.put(node, nodeId);
        m_treeNodes.put(nodeId, node);
        putUpdatedPropertiesForResolvedNode(jsonNode, nodeId, node, (IVirtualTreeNode) cachedNode);
      }

      jsonNodes.put(jsonNode);
    }
    if (jsonNodes.length() == 0) {
      return;
    }
    JSONObject jsonEvent = JsonObjectUtility.newOrderedJSONObject();
    putProperty(jsonEvent, PROP_NODES, jsonNodes);
    putProperty(jsonEvent, PROP_COMMON_PARENT_NODE_ID, getNodeId(event.getCommonParentNode()));
    addActionEvent(EVENT_NODES_UPDATED, jsonEvent);
  }

  protected void handleModelNodesDeleted(TreeEvent event) {
    Collection<ITreeNode> nodes = event.getNodes();
    JSONObject jsonEvent = JsonObjectUtility.newOrderedJSONObject();
    putProperty(jsonEvent, PROP_COMMON_PARENT_NODE_ID, getNodeId(event.getCommonParentNode()));
    // Small optimization: If no nodes remain, just
    // send "all" instead of every single nodeId. (However, the nodes must be disposed individually.)
    // Caveat: This can only be optimized when no nodes were inserted again in the same "tree changing" scope.
    if (event.getCommonParentNode() != null && getFilteredNodeCount(event.getCommonParentNode()) == 0) {
      addActionEvent(EVENT_ALL_CHILD_NODES_DELETED, jsonEvent);
    }
    else {
      JSONArray jsonNodeIds = nodeIdsToJson(nodes, false, false);
      if (jsonNodeIds.length() > 0) {
        putProperty(jsonEvent, PROP_NODE_IDS, jsonNodeIds);
        addActionEvent(EVENT_NODES_DELETED, jsonEvent);
      }
    }
    disposeNodes(nodes, true);
  }

  protected void handleModelAllChildNodesDeleted(TreeEvent event) {
    JSONObject jsonEvent = JsonObjectUtility.newOrderedJSONObject();
    putProperty(jsonEvent, PROP_COMMON_PARENT_NODE_ID, getNodeId(event.getCommonParentNode()));
    addActionEvent(EVENT_ALL_CHILD_NODES_DELETED, jsonEvent);
    // Read the removed nodes from the event, because they are no longer contained in the model
    disposeNodes(event.getChildNodes(), true);
  }

  protected void handleModelNodesSelected(Collection<ITreeNode> modelNodes) {
    JSONArray jsonNodeIds = nodeIdsToJson(modelNodes, true, false);
    JSONObject jsonEvent = new JSONObject();
    putProperty(jsonEvent, PROP_NODE_IDS, jsonNodeIds);
    addActionEvent(EVENT_NODES_SELECTED, jsonEvent);
  }

  protected void handleModelNodesChecked(Collection<ITreeNode> modelNodes) {
    JSONArray jsonNodes = new JSONArray();
    for (ITreeNode node : modelNodes) {
      if (!isNodeAccepted(node)) {
        continue;
      }
      String nodeId = getNodeId(node);
      if (nodeId == null) { // Ignore nodes that are not yet sent to the UI (may happen due to asynchronous event processing)
        continue;
      }
      JSONObject json = JsonObjectUtility.newOrderedJSONObject();
      putProperty(json, "id", nodeId);
      putProperty(json, "checked", node.isChecked());
      jsonNodes.put(json);
    }
    if (jsonNodes.length() == 0) {
      return;
    }
    JSONObject jsonEvent = new JSONObject();
    putProperty(jsonEvent, PROP_NODES, (jsonNodes));
    addActionEvent(EVENT_NODES_CHECKED, jsonEvent);
  }

  protected void handleModelNodeChanged(ITreeNode modelNode) {
    if (!isNodeAccepted(modelNode)) {
      return;
    }
    String nodeId = getNodeId(modelNode);
    if (nodeId == null) { // Ignore nodes that are not yet sent to the UI (may happen due to asynchronous event processing)
      return;
    }
    JSONObject jsonEvent = JsonObjectUtility.newOrderedJSONObject();
    putProperty(jsonEvent, PROP_NODE_ID, nodeId);
    putCellProperties(jsonEvent, modelNode.getCell());
    addActionEvent(EVENT_NODE_CHANGED, jsonEvent);
  }

  protected void handleModelChildNodeOrderChanged(TreeEvent event) {
    JSONObject jsonEvent = JsonObjectUtility.newOrderedJSONObject();
    jsonEvent.put("parentNodeId", getNodeId(event.getCommonParentNode()));
    boolean hasNodeIds = false;
    for (ITreeNode childNode : event.getChildNodes()) {
      if (!isNodeAccepted(childNode)) {
        continue;
      }
      String childNodeId = getNodeId(childNode);
      if (childNodeId == null) { // Ignore nodes that are not yet sent to the UI (may happen due to asynchronous event processing)
        continue;
      }
      jsonEvent.append("childNodeIds", childNodeId);
      hasNodeIds = true;
    }
    if (hasNodeIds) {
      addActionEvent(EVENT_CHILD_NODE_ORDER_CHANGED, jsonEvent);
    }
  }

  protected void handleModelRequestFocus(TreeEvent event) {
    addActionEvent(EVENT_REQUEST_FOCUS);
  }

  protected void handleModelScrollToSelection(TreeEvent event) {
    addActionEvent(EVENT_SCROLL_TO_SELECTION);
  }

  @Override
  public void handleModelContextMenuChanged(List<IJsonAdapter<?>> menuAdapters) {
    addPropertyChangeEvent(PROP_MENUS, JsonObjectUtility.adapterIdsToJson(menuAdapters));
  }

  @Override
  public void consumeBinaryResource(List<BinaryResource> binaryResources, Map<String, String> uploadProperties) {
    if ((getModel().getDropType() & IDNDSupport.TYPE_FILE_TRANSFER) == IDNDSupport.TYPE_FILE_TRANSFER) {
      ResourceListTransferObject transferObject = new ResourceListTransferObject(binaryResources);
      ITreeNode node = null;
      if (uploadProperties != null && uploadProperties.containsKey("nodeId")) {
        String nodeId = uploadProperties.get("nodeId");
        if (!StringUtility.isNullOrEmpty(nodeId)) {
          node = getTreeNodeForNodeId(nodeId);
        }
      }
      getModel().getUIFacade().fireNodeDropActionFromUI(node, transferObject);
    }
  }

  @Override
  public long getMaximumBinaryResourceUploadSize() {
    return getModel().getDropMaximumSize();
  }

  protected JSONArray nodeIdsToJson(Collection<ITreeNode> modelNodes, boolean autoCreateNodeId) {
    return nodeIdsToJson(modelNodes, true, autoCreateNodeId);
  }

  protected JSONArray nodeIdsToJson(Collection<ITreeNode> modelNodes, boolean checkNodeAccepted, boolean autoCreateNodeId) {
    JSONArray jsonNodeIds = new JSONArray();
    for (ITreeNode node : modelNodes) {
      if (checkNodeAccepted && !isNodeAccepted(node)) {
        continue;
      }
      String nodeId;
      if (autoCreateNodeId) {
        nodeId = getOrCreateNodeId(node);
      }
      else {
        nodeId = getNodeId(node);
        if (nodeId == null) { // Ignore nodes that are not yet sent to the UI (may happen due to asynchronous event processing)
          continue;
        }
      }
      // May be null if its the invisible root node
      if (nodeId != null) {
        jsonNodeIds.put(nodeId);
      }
    }
    return jsonNodeIds;
  }

  public String getOrCreateNodeId(ITreeNode node) {
    if (node == null) {
      return null;
    }
    if (isInvisibleRootNode(node)) {
      return null;
    }
    String id = m_treeNodeIds.get(node);
    if (id != null) {
      return id;
    }
    id = getUiSession().createUniqueIdFor(null);
    m_treeNodes.put(id, node);
    m_treeNodeIds.put(node, id);
    return id;
  }

  protected String getNodeId(ITreeNode node) {
    if (node == null) {
      return null;
    }
    if (isInvisibleRootNode(node)) {
      return null;
    }
    return m_treeNodeIds.get(node);
  }

  protected ITreeNode getNode(String nodeId) {
    if (nodeId == null) {
      return null;
    }
    return m_treeNodes.get(nodeId);
  }

  protected boolean isInvisibleRootNode(ITreeNode node) {
    if (!getModel().isRootNodeVisible()) {
      return (node == getModel().getRootNode());
    }
    return false;
  }

  protected List<ITreeNode> getTopLevelNodes() {
    ITreeNode rootNode = getModel().getRootNode();
    if (getModel().isRootNodeVisible()) {
      return CollectionUtility.arrayList(rootNode);
    }
    return rootNode.getChildNodes();
  }

  protected void putCellProperties(JSONObject json, ICell cell) {
    // We deliberately don't use JsonCell here, because most properties are not supported in a tree anyway
    json.put("text", cell.getText());
    json.put("iconId", BinaryResourceUrlUtility.createIconUrl(cell.getIconId()));
    json.put("cssClass", (cell.getCssClass()));
    json.put("tooltipText", cell.getTooltipText());
    json.put("foregroundColor", cell.getForegroundColor());
    json.put("backgroundColor", cell.getBackgroundColor());
    json.put("font", (cell.getFont() == null ? null : cell.getFont().toPattern()));
  }

  protected JSONObject treeNodeToJson(ITreeNode node) {
    JSONObject json = JsonObjectUtility.newOrderedJSONObject();
    putProperty(json, "id", getOrCreateNodeId(node));
    putProperty(json, "expanded", node.isExpanded());
    putProperty(json, "leaf", node.isLeaf());
    putProperty(json, "checked", node.isChecked());
    putProperty(json, "enabled", node.isEnabled());
    putProperty(json, "childNodeIndex", node.getChildNodeIndex());
    putCellProperties(json, node.getCell());
    JSONArray jsonChildNodes = new JSONArray();
    if (node.getChildNodeCount() > 0) {
      for (ITreeNode childNode : node.getChildNodes()) {
        if (!isNodeAccepted(childNode)) {
          continue;
        }
        jsonChildNodes.put(treeNodeToJson(childNode));
      }
    }
    putProperty(json, "childNodes", jsonChildNodes);
    JsonObjectUtility.filterDefaultValues(json, "TreeNode");
    return json;
  }

  public ITreeNode getTreeNodeForNodeId(String nodeId) {
    ITreeNode node = m_treeNodes.get(nodeId);
    if (node == null) {
      throw new UiException("No node found for id " + nodeId);
    }
    return node;
  }

  public List<ITreeNode> extractTreeNodes(JSONObject json) {
    return jsonToTreeNodes(json.getJSONArray(PROP_NODE_IDS));
  }

  public List<ITreeNode> jsonToTreeNodes(JSONArray nodeIds) {
    List<ITreeNode> nodes = new ArrayList<>(nodeIds.length());
    for (int i = 0; i < nodeIds.length(); i++) {
      ITreeNode node = getNode(nodeIds.getString(i));
      if (node != null) {
        nodes.add(node);
      }
    }
    return nodes;
  }

  @Override
  public void handleUiEvent(JsonEvent event) {
    if (EVENT_NODE_CLICKED.equals(event.getType())) {
      handleUiNodeClicked(event);
    }
    else if (EVENT_NODE_ACTION.equals(event.getType())) {
      handleUiNodeAction(event);
    }
    else if (EVENT_NODES_SELECTED.equals(event.getType())) {
      handleUiNodesSelected(event);
    }
    else if (EVENT_NODE_EXPANDED.equals(event.getType())) {
      handleUiNodeExpanded(event);
    }
    else if (EVENT_NODES_CHECKED.equals(event.getType())) {
      handleUiNodesChecked(event);
    }
    else {
      super.handleUiEvent(event);
    }
  }

  protected void handleUiNodesChecked(JsonEvent event) {
    CheckedInfo treeNodesChecked = jsonToCheckedInfo(event.getData());
    addTreeEventFilterCondition(TreeEvent.TYPE_NODES_CHECKED, treeNodesChecked.getAllNodes());
    if (treeNodesChecked.getCheckedNodes().size() > 0) {
      getModel().getUIFacade().setNodesCheckedFromUI(treeNodesChecked.getCheckedNodes(), true);
    }
    if (treeNodesChecked.getUncheckedNodes().size() > 0) {
      getModel().getUIFacade().setNodesCheckedFromUI(treeNodesChecked.getUncheckedNodes(), false);
    }
  }

  protected void handleUiNodeClicked(JsonEvent event) {
    final ITreeNode node = getTreeNodeForNodeId(event.getData().getString(PROP_NODE_ID));
    getModel().getUIFacade().fireNodeClickFromUI(node, MouseButton.Left);
  }

  protected void handleUiNodeAction(JsonEvent event) {
    final ITreeNode node = getTreeNodeForNodeId(event.getData().getString(PROP_NODE_ID));
    getModel().getUIFacade().fireNodeActionFromUI(node);
  }

  protected void handleUiNodesSelected(JsonEvent event) {
    final List<ITreeNode> nodes = extractTreeNodes(event.getData());
    addTreeEventFilterCondition(TreeEvent.TYPE_NODES_SELECTED, nodes);
    getModel().getUIFacade().setNodesSelectedFromUI(nodes);
  }

  protected void handleUiNodeExpanded(JsonEvent event) {
    ITreeNode node = getTreeNodeForNodeId(event.getData().getString(PROP_NODE_ID));
    boolean expanded = event.getData().getBoolean(PROP_EXPANDED);
    int eventType = expanded ? TreeEvent.TYPE_NODE_EXPANDED : TreeEvent.TYPE_NODE_COLLAPSED;
    addTreeEventFilterCondition(eventType, CollectionUtility.arrayList(node));
    getModel().getUIFacade().setNodeExpandedFromUI(node, expanded);
  }

  /**
   * Ignore deleted or filtered nodes, because for the UI, they don't exist
   */
  protected boolean isNodeAccepted(ITreeNode node) {
    if (node.isStatusDeleted()) {
      return false;
    }
    if (!node.isFilterAccepted()) {
      // Accept if rejected by user row filter because gui is and should be aware of that row
      return node.isRejectedByUser();
    }
    return true;
  }

  /**
   * @return the filtered node count excluding nodes filtered by the user
   */
  protected int getFilteredNodeCount(ITreeNode parentNode) {
    if (getModel().getNodeFilters().size() == 0) {
      return parentNode.getChildNodeCount();
    }
    int filteredNodeCount = 0;
    for (ITreeNode node : parentNode.getChildNodes()) {
      if (node.isFilterAccepted() || node.isRejectedByUser()) {
        filteredNodeCount++;
      }
    }
    return filteredNodeCount;
  }

  protected AbstractEventBuffer<TreeEvent> eventBuffer() {
    return m_eventBuffer;
  }

  protected final TreeEventFilter getTreeEventFilter() {
    return m_treeEventFilter;
  }

  protected void addTreeEventFilterCondition(int type, List<ITreeNode> nodes) {
    m_treeEventFilter.addCondition(new TreeEventFilterCondition(type, nodes));
  }

  @Override
  public void cleanUpEventFilters() {
    super.cleanUpEventFilters();
    m_treeEventFilter.removeAllConditions();
  }

  protected CheckedInfo jsonToCheckedInfo(JSONObject data) {
    JSONArray jsonNodes = data.optJSONArray("nodes");
    CheckedInfo checkInfo = new CheckedInfo();
    for (int i = 0; i < jsonNodes.length(); i++) {
      JSONObject jsonObject = jsonNodes.optJSONObject(i);
      ITreeNode row = m_treeNodes.get(jsonObject.getString("nodeId"));
      checkInfo.getAllNodes().add(row);
      if (jsonObject.optBoolean("checked")) {
        checkInfo.getCheckedNodes().add(row);
      }
      else {
        checkInfo.getUncheckedNodes().add(row);
      }
    }
    return checkInfo;
  }

  /**
   * Called by {@link #handleModelNodesUpdated(TreeEvent)} when it has detected that a virtual tree node was resolved by
   * a real node. Subclasses may override this method to put any updated properties to the JSON response.
   *
   * @param jsonNode
   *          {@link JSONObject} sent to the UI for the resolved node. Updated properties may be put in here.
   * @param nodeId
   *          The ID of the resolved node.
   * @param node
   *          The new, resolved node
   * @param cachedNode
   *          The old, virtual node
   */
  protected void putUpdatedPropertiesForResolvedNode(JSONObject jsonNode, String nodeId, ITreeNode node, IVirtualTreeNode virtualNode) {
  }

  protected static class CheckedInfo {
    private final List<ITreeNode> m_allNodes = new ArrayList<ITreeNode>();
    private final List<ITreeNode> m_checkedNodes = new ArrayList<ITreeNode>();
    private final List<ITreeNode> m_uncheckedNodes = new ArrayList<ITreeNode>();

    public CheckedInfo() {
    }

    public List<ITreeNode> getAllNodes() {
      return m_allNodes;
    }

    public List<ITreeNode> getCheckedNodes() {
      return m_checkedNodes;
    }

    public List<ITreeNode> getUncheckedNodes() {
      return m_uncheckedNodes;
    }
  }

  private class P_TreeListener extends TreeAdapter {

    @Override
    public void treeChanged(final TreeEvent e) {
      handleModelTreeEvent(e);
    }
  }
}
