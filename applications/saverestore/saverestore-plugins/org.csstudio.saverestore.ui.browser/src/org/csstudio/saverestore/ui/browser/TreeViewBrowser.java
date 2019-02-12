/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.csstudio.saverestore.ui.browser;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.csstudio.saverestore.DataProvider;
import org.csstudio.saverestore.SaveRestoreService;
import org.csstudio.saverestore.data.Branch;
import org.csstudio.saverestore.data.SaveSet;
import org.csstudio.saverestore.data.Snapshot;
import org.csstudio.saverestore.data.tree.FolderTreeNode;
import org.csstudio.saverestore.data.tree.SnapshotTreeNode;
import org.csstudio.saverestore.data.tree.TreeNode;
import org.csstudio.saverestore.data.tree.TreeNodeType;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Callback;

/**
 * @author georgweiss Created 9 Jan 2019
 */
public class TreeViewBrowser extends TreeView<TreeNode> {

	private static Executor UI_EXECUTOR = Platform::runLater;
	private static BiConsumer<String, Runnable> SERVICE_EXECUTOR = SaveRestoreService.getInstance()::execute;

	private EventHandler<TreeItem.TreeModificationEvent<TreeNode>> nodeExpandedHandler;

	private TreeNodeItem treeRootItem;

	private final BrowserActionManager actionManager;

	private TreeNodeType selectedTreeNodeType;

	private boolean isRightClick = false;

	public TreeViewBrowser(BrowserActionManager actionManager) {

		this.actionManager = actionManager;

		nodeExpandedHandler = new EventHandler<TreeItem.TreeModificationEvent<TreeNode>>() {
			@Override
			public void handle(TreeModificationEvent<TreeNode> event) {
				expandTreeNode(event);
			}
		};

		this.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				TreeItem<TreeNode> item = getSelectionModel().getSelectedItem();
				if(item != null) {
					selectedTreeNodeType = item.getValue().getType();
					if (mouseEvent.getClickCount() == 2) {
						nodeSelected(getSelectionModel().getSelectedItem());
					}
				}
			}
		});

		this.setCellFactory(new Callback<TreeView<TreeNode>, TreeCell<TreeNode>>() {
			@Override
			public TreeCell<TreeNode> call(TreeView<TreeNode> p) {
				return new SnapshotTreeCell();
			}
		});

	}

	/**
	 * Loads the data for the tree root as provided (persisted) by the current
	 * {@link DataProvider}. This should be called when the UI is set up and when
	 * the {@link DataProvider} is changed or reconfigured, e.g. when specifying a
	 * different service URL.
	 */
	public void loadInitialTreeData() {

		if (treeRootItem != null) {
			treeRootItem.removeEventHandler(TreeItem.branchExpandedEvent(), nodeExpandedHandler);
		}

		SERVICE_EXECUTOR.accept("Load tree browser data", () -> {
			TreeNode treeRoot = SaveRestoreService.getInstance().getSelectedDataProvider().getProvider()
					.getTreeRootNode();

			treeRootItem = new TreeNodeItem(treeRoot);

			for (TreeNode childNode : ((FolderTreeNode) treeRoot).getChildren()) {
				treeRootItem.getChildren().add(new TreeNodeItem(childNode));
			}

			treeRootItem.setExpanded(true);
			treeRootItem.addEventHandler(TreeItem.branchExpandedEvent(), nodeExpandedHandler);

			UI_EXECUTOR.execute(() -> super.setRoot(treeRootItem));
		});
	}

	/**
	 * Handles expansion of a tree node. Queries the {@link DataProvider} service
	 * for child nodes of the node associated with the event.
	 * 
	 * @param event The event triggered by an expansion of a tree node.
	 */
	private void expandTreeNode(TreeModificationEvent<TreeNode> event) {
		SERVICE_EXECUTOR.accept("Expand tree node", () -> {
			DataProvider dataProvider = SaveRestoreService.getInstance().getSelectedDataProvider().getProvider();

			TreeItem<TreeNode> targetItem = event.getTreeItem();
			targetItem.getChildren().clear();
			List<TreeNodeItem> childItems = dataProvider.getChildNodes((FolderTreeNode) targetItem.getValue()).stream()
					.map(i -> new TreeNodeItem(i)).collect(Collectors.toList());
			UI_EXECUTOR.execute(() -> targetItem.getChildren().addAll(childItems));
		});
	}

	/**
	 * 
	 * Handles selection of a node. The action taken depends on the node type found
	 * in {@link TreeNode#type}.
	 * 
	 * @param newValue Tree node associated with the selection
	 */
	private void nodeSelected(TreeItem<TreeNode> newValue) {

		switch (selectedTreeNodeType) {

		case SNAPSHOT:
			SERVICE_EXECUTOR.accept("Snapshot node selected", () -> {

				SaveSet saveSet = new SaveSet(new Branch(), Optional.empty(),
						new String[] { newValue.getValue().getName() },
						SaveRestoreService.getInstance().getSelectedDataProvider().getId());
				SnapshotTreeNode snapshotTreeNode = (SnapshotTreeNode) newValue.getValue();
				Snapshot snapshot = new Snapshot(saveSet,
						Instant.ofEpochMilli(snapshotTreeNode.getLastModified().getTime()),
						snapshotTreeNode.getComment(), snapshotTreeNode.getUserName());
				snapshot.setSnapshotId(Integer.toString(newValue.getValue().getId()));
				actionManager.openSnapshot(snapshot);
			});
			break;
		case SAVESET:
			SERVICE_EXECUTOR.accept("SaveSet node selected", () -> {
				SaveSet saveSet = new SaveSet(new Branch(), Optional.empty(),
						new String[] { newValue.getValue().getName() },
						SaveRestoreService.getInstance().getSelectedDataProvider().getId());
				saveSet.setSaveSetId(Integer.toString(newValue.getValue().getId()));
				saveSet.setFullyQualifiedName("/config/" + newValue.getValue().getId());
				saveSet.setLastModified(newValue.getValue().getLastModified());
				saveSet.setUserName(newValue.getValue().getUserName());
				actionManager.openSaveSet(saveSet);
			});
			break;
		case FOLDER:
		default:
		}
	}

	/**
	 * Subclass of {@link TreeItem} using {@link TreeNode} (and subclasses) to hold
	 * business data.
	 * 
	 * @author georgweiss Created 3 Jan 2019
	 */
	private class TreeNodeItem extends TreeItem<TreeNode> {

		TreeNode treeNode;

		TreeNodeItem(TreeNode treeNode) {
			super(treeNode);
			this.treeNode = treeNode;
		}

		@Override
		public boolean isLeaf() {
			return treeNode.isLeaf();
		}
	}

	/**
	 * Cell renderer for a tree node item. It uses icons for save set and snapshot
	 * nodes, and also adds date and user name to snapshot nodes.
	 * 
	 * @author georgweiss Created 11 Jan 2019
	 */
	private class SnapshotTreeCell extends TreeCell<TreeNode> {

		private HBox saveSetBox = new HBox();
		private HBox snapshotBox = new HBox();
		private VBox snapshotLabels = new VBox();
		private Label folderNameLabel = new Label();
		private Label saveSetNameLabel = new Label();
		private Label snapshotNameLabel = new Label();
		private Label snapshotMetaDataLabel = new Label();
		private ImageView saveSetIcon = new ImageView(BrowserView.SAVE_SET_IMAGE);
		private ImageView snapshotIcon = new ImageView(BrowserView.SNAPSHOT_IMAGE);

		public SnapshotTreeCell() {

			saveSetBox.getChildren().addAll(saveSetIcon, saveSetNameLabel);
			snapshotLabels.getChildren().addAll(snapshotNameLabel, snapshotMetaDataLabel);
			snapshotBox.getChildren().addAll(snapshotIcon, snapshotLabels);
			snapshotMetaDataLabel.setFont(Font.font(Font.getDefault().getSize() - 3));
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}

		@Override
		public void updateItem(TreeNode treeNode, boolean empty) {
			super.updateItem(treeNode, empty);
			setGraphic(null);
			if (treeNode == null) {
				return;
			}

			if (treeNode.getType().equals(TreeNodeType.SNAPSHOT)) {
				snapshotNameLabel.setText(treeNode.getName());
				snapshotMetaDataLabel.setText(treeNode.getLastModified() + " (" + treeNode.getUserName() + ")");
				setGraphic(snapshotBox);
				setTooltip(new Tooltip("Double click to open snapshot"));
			} else if (treeNode.getType().equals(TreeNodeType.SAVESET)) {
				saveSetNameLabel.setText(treeNode.getName());
				setGraphic(saveSetBox);
				setTooltip(new Tooltip("Double click to open saveset"));
			} else {
				folderNameLabel.setText(treeNode.getName());
				setGraphic(folderNameLabel);
			}
		}
	}

	/**
	 * 
	 * @return The type of tree node selected, see {@link TreeNodeType}
	 */
	public TreeNodeType getSelectedTreeNodeType() {
		return selectedTreeNodeType;
	}
}
