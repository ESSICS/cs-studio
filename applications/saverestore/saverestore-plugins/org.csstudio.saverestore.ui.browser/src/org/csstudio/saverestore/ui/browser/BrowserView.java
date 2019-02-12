/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.csstudio.saverestore.ui.browser;

import static org.csstudio.ui.fx.util.FXUtilities.setGridConstraints;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import org.csstudio.saverestore.BrowserPresentationType;
import org.csstudio.saverestore.DataProvider;
import org.csstudio.saverestore.DataProviderWrapper;
import org.csstudio.saverestore.SaveRestoreService;
import org.csstudio.saverestore.data.BaseLevel;
import org.csstudio.saverestore.data.Branch;
import org.csstudio.saverestore.data.SaveSet;
import org.csstudio.saverestore.data.Snapshot;
import org.csstudio.saverestore.data.tree.TreeNode;
import org.csstudio.saverestore.data.tree.TreeNodeType;
import org.csstudio.saverestore.ui.Activator;
import org.csstudio.saverestore.ui.Selector;
import org.csstudio.saverestore.ui.util.SnapshotDataFormat;
import org.csstudio.ui.fx.util.FXMessageDialog;
import org.csstudio.ui.fx.util.FXTaggingDialog;
import org.csstudio.ui.fx.util.UnfocusableButton;
import org.csstudio.ui.fx.util.UnfocusableToggleButton;
import org.eclipse.fx.ui.workbench3.FXViewPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 *
 * <code>BrowserView</code> is the view that provides the browsing facilities
 * for save and restore. The view consists of an accordion panel, composed of
 * three parts: isotopes, save sets and snapshots selector.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class BrowserView extends FXViewPart implements ISelectionProvider, IShellProvider {

	private static final String SETTINGS_SELECTED_BRANCH = "selectedBranch";
	private static final String SETTINGS_SELECTED_DATA_PROVIDER = "selectedDataProvider";
	private static final String SETTINGS_DEFAULT_BASE_LEVEL_BROWSER = "defaultBaseLevelBrowser";
	private static final String SETTINGS_BASE_LEVEL_FILTER_NOT_SELECTED = "baseLevelFilterNotSelected";

	public static final Image SAVE_SET_IMAGE = new Image(BrowserView.class.getResourceAsStream("/icons/txt.png"));
	public static final Image SNAPSHOT_IMAGE = new Image(BrowserView.class.getResourceAsStream("/icons/ksnapshot.png"));

	private static class SaveSetWrapper {

		final SaveSet set;
		final String[] path;

		SaveSetWrapper(SaveSet set, String name) {
			this.set = set;
			this.path = name == null ? set.getPath() : name.split("\\/");
		}

		boolean isFolder() {
			return set == null;
		}

		@Override
		public String toString() {
			String p = path[path.length - 1];
			if (p.toLowerCase(Locale.UK).endsWith(".bms") && p.length() > 4) {
				return p.substring(0, p.length() - 4);
			} else {
				return p;
			}
		}

	}

	private static class SaveSetTreeItem extends TreeItem<SaveSetWrapper> {
		SaveSetTreeItem(SaveSetWrapper set) {
			super(set, new ImageView(SAVE_SET_IMAGE));
		}

		SaveSetTreeItem(String name) {
			super(new SaveSetWrapper(null, name));
		}
	}

	private DefaultBaseLevelBrowser defaultBaseLevelBrowser;
	private BaseLevelBrowser<BaseLevel> baseLevelBrowser;
	private TreeView<SaveSetWrapper> saveSetsTree;
	private ListView<Snapshot> snapshotsList;
	private TreeViewBrowser treeView;
	private TitledPane snapshotsPane;
	private TitledPane baseLevelPane;
	private TitledPane saveSetsPane;
	private Button importButton;
	private Button newButton;
	private VBox mainPane;
	private VBox saveSetsVBoxPane;
	private VBox snapshotsVBoxPane;
	private VBox treeViewVBoxPane;

	private final Selector selector = new Selector(this);
	private final BrowserActionManager actionManager = new BrowserActionManager(selector, this);

	private boolean searchMode;

	private Menu contextMenu;
	private Action deleteTagAction;
	private Action deleteSaveSetAction;
	private Action editSaveSetAction;

	private final List<ISelectionChangedListener> selectionChangedListener = new CopyOnWriteArrayList<>();

	private PropertyChangeListener dpl = e -> Platform.runLater(() -> updateForDataProviderChange());

	private Scene scene;
	private boolean inSnapshotPane = false;
	private boolean inSaveSetsPane = false;
	private boolean inTreeViewBrowser = false;

	private MenuManager menu;

	/**
	 * @return the selector bound to this view
	 */
	public Selector getSelector() {
		return selector;
	}

	/**
	 * @return the action manager bound to this view
	 */
	public BrowserActionManager getActionManager() {
		return actionManager;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite)
	 */
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		getSite().setSelectionProvider(this);
	}

	@Override
	public void dispose() {
		SaveRestoreService.getInstance().removePropertyChangeListener(SaveRestoreService.SELECTED_DATA_PROVIDER, dpl);
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.fx.ui.workbench3.FXViewPart#createPartControl(org.eclipse.swt.
	 * widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		menu = new MenuManager();
		deleteTagAction = new DeleteTagAction();
		deleteSaveSetAction = new DeleteSaveSetAction();
		editSaveSetAction = new EditSaveSetAction();

		menu.addMenuListener(new ContextMenuListener());

		contextMenu = menu.createContextMenu(parent);
		parent.setMenu(contextMenu);

		getSite().registerContextMenu(menu, this);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.csstudio.saverestore.ui.help.browser");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.fx.ui.workbench3.FXViewPart#createFxScene()
	 */
	@Override
	protected Scene createFxScene() {

		ensureCurrentDataProvider();

		BorderPane main = new BorderPane();

		scene = new Scene(main);

		mainPane = new VBox();

		createUI();

		main.setCenter(mainPane);
		SaveRestoreService.getInstance().addPropertyChangeListener(SaveRestoreService.SELECTED_DATA_PROVIDER, dpl);

		return scene;
	}

	private void createUI() {

		mainPane.getChildren().clear();

		createCommonUIElements();
		createDataProviderSpecificUIElements();
	}

	/**
	 * Creates UI elements that are common for all {@link DataProvider}s. However,
	 * as switching between data providers affects the UI, the common UI components
	 * may already have been created.
	 */
	private void createCommonUIElements() {
		if (snapshotsVBoxPane == null) {
			snapshotsVBoxPane = new VBox();
			Node snapshots = createSnapshotsPane(scene);

			VBox.setVgrow(snapshots, Priority.ALWAYS);
			snapshotsVBoxPane.getChildren().addAll(snapshotsPane);
			VBox.setVgrow(snapshotsVBoxPane, Priority.ALWAYS);
		}
	}

	private void createDataProviderSpecificUIElements() {
		if (SaveRestoreService.getInstance().getSelectedDataProvider().getProvider()
				.preferredBrowserRepresentationType().equals(BrowserPresentationType.MULTIPLE_PANES)) {
			createBaseLevelBrowserUIElements();
		} else if (SaveRestoreService.getInstance().getSelectedDataProvider().getProvider()
				.preferredBrowserRepresentationType().equals(BrowserPresentationType.TREE_ONLY)) {
			createTreeViewBrowserUIElements();
		}
	}

	private void createBaseLevelBrowserUIElements() {
		if (saveSetsVBoxPane == null) {
			saveSetsVBoxPane = new VBox();
			Node saveSets = createSaveSetsPane(scene);
			VBox.setVgrow(saveSets, Priority.ALWAYS);
			saveSetsVBoxPane.getChildren().addAll(saveSetsPane);
			VBox.setVgrow(saveSetsVBoxPane, Priority.ALWAYS);
			saveSetsPane.setExpanded(true);
			initBaseLevelBrowser();
			setUpSetButtons(newButton, importButton, SaveRestoreService.getInstance().getSelectedDataProvider());
		}
		DataProviderWrapper dataProviderWrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
		if (dataProviderWrapper.getProvider().areBaseLevelsSupported()) {
			mainPane.getChildren().addAll(baseLevelPane, saveSetsVBoxPane, snapshotsVBoxPane);
		} else {
			mainPane.getChildren().addAll(saveSetsVBoxPane, snapshotsVBoxPane);
		}
	}

	private void createTreeViewBrowserUIElements() {
		if (treeViewVBoxPane == null) {
			treeViewVBoxPane = new VBox();

			treeView = new TreeViewBrowser(actionManager);

			setGridConstraints(treeView, true, true, Priority.ALWAYS, Priority.ALWAYS);

			VBox.setVgrow(treeView, Priority.ALWAYS);
			treeViewVBoxPane.getChildren().add(treeView);
			VBox.setVgrow(treeViewVBoxPane, Priority.ALWAYS);

			treeViewVBoxPane.setOnMouseEntered(me -> {
				inTreeViewBrowser = true;
			});

			treeViewVBoxPane.setOnMouseExited(me -> {
				inTreeViewBrowser = false;
			});

		}

		mainPane.getChildren().addAll(treeViewVBoxPane);
		treeView.loadInitialTreeData();
	}

	private TitledPane createBaseLevelsPane(BaseLevelBrowser<BaseLevel> browser) {
		BorderPane content = new BorderPane();
		baseLevelBrowser = browser;
		defaultBaseLevelBrowser = new DefaultBaseLevelBrowser(this.getSite());

		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		boolean useSpecialBrowser = !settings.getBoolean(SETTINGS_DEFAULT_BASE_LEVEL_BROWSER);
		if (baseLevelBrowser == null || !useSpecialBrowser) {
			content.setCenter(defaultBaseLevelBrowser.getFXContent());
		} else {
			content.setCenter(baseLevelBrowser.getFXContent());
		}
		if (browser == null) {
			browser = defaultBaseLevelBrowser;
		}
		baseLevelPane = new TitledPane(browser.getTitleFor(Optional.empty(), Optional.empty()), content);
		baseLevelPane.setMaxHeight(Double.MAX_VALUE);

		GridPane titleBox = new GridPane();
		titleBox.setHgap(5);
		Label titleText = new Label(browser.getTitleFor(Optional.empty(), Optional.empty()));
		titleText.textProperty().bind(baseLevelPane.textProperty());
		ToggleButton filterButton = new UnfocusableToggleButton("",
				new ImageView(new Image(BrowserView.class.getResourceAsStream("/icons/filter_ps.png"))));
		filterButton.setTooltip(new Tooltip("Disable non-existing"));
		filterButton.selectedProperty().addListener((a, o, n) -> {
			Activator.getDefault().getDialogSettings().put(SETTINGS_BASE_LEVEL_FILTER_NOT_SELECTED, !n);
			defaultBaseLevelBrowser.setShowOnlyAvailable(n);
			if (baseLevelBrowser != null) {
				baseLevelBrowser.setShowOnlyAvailable(n);
			}
		});

		setUpTitlePaneNode(titleText, true);
		setUpTitlePaneNode(filterButton, false);
		if (baseLevelBrowser == null) {
			titleBox.addRow(0, titleText, filterButton);
		} else {
			ToggleButton baseLevelPanelFilterButton = new UnfocusableToggleButton("",
					new ImageView(new Image(BrowserView.class.getResourceAsStream("/icons/Bookshelf16.gif"))));
			baseLevelPanelFilterButton
					.setTooltip(new Tooltip("Toggle between custom browser (" + baseLevelBrowser.getReadableName()
							+ ") and default browser (" + defaultBaseLevelBrowser.getReadableName() + ")"));
			baseLevelPanelFilterButton.selectedProperty().addListener((a, o, n) -> {
				Activator.getDefault().getDialogSettings().put(SETTINGS_DEFAULT_BASE_LEVEL_BROWSER, !n);
				if (n) {
					content.setCenter(baseLevelBrowser.getFXContent());
				} else {
					content.setCenter(defaultBaseLevelBrowser.getFXContent());
				}
			});
			setUpTitlePaneNode(baseLevelPanelFilterButton, false);
			titleBox.addRow(0, titleText, baseLevelPanelFilterButton, filterButton);
			baseLevelPanelFilterButton.selectedProperty().set(useSpecialBrowser);
		}
		titleBox.setMaxWidth(Double.MAX_VALUE);
		titleText.setMaxWidth(Double.MAX_VALUE);
		baseLevelPane.setGraphic(titleBox);
		baseLevelPane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		titleBox.prefWidthProperty().bind(scene.widthProperty().subtract(34));

		boolean selected = !settings.getBoolean(SETTINGS_BASE_LEVEL_FILTER_NOT_SELECTED);
		filterButton.setSelected(selected);
		defaultBaseLevelBrowser.setShowOnlyAvailable(selected);
		if (baseLevelBrowser != null) {
			baseLevelBrowser.setShowOnlyAvailable(selected);
		}

		return baseLevelPane;
	}

	private Node createSaveSetsPane(Scene scene) {
		GridPane content = new GridPane();

		saveSetsTree = new TreeView<>();

		saveSetsTree.getSelectionModel().selectedItemProperty().addListener((a, o, n) -> {
			selector.selectedSaveSetProperty().set(n == null || n.getValue().isFolder() ? null : n.getValue().set);
		});
		saveSetsTree.setShowRoot(false);

		setGridConstraints(saveSetsTree, true, true, Priority.ALWAYS, Priority.ALWAYS);
		content.add(saveSetsTree, 0, 0);

		saveSetsPane = new TitledPane("Save Sets", content);
		saveSetsPane.setMaxHeight(Double.MAX_VALUE);

		GridPane titleBox = new GridPane();
		titleBox.setHgap(5);
		Label titleText = new Label("Save Sets");
		titleText.textProperty().bind(saveSetsPane.textProperty());
		newButton = new UnfocusableButton("New");
		newButton.setTooltip(new Tooltip("Create a new Save Set"));
		Button editButton = new UnfocusableButton("Edit");
		editButton.setTooltip(new Tooltip("Edit selected Save Set"));
		importButton = new UnfocusableButton("Import");
		importButton.setTooltip(new Tooltip("Import Save Sets from another location"));
		Button openButton = new UnfocusableButton("Open");
		openButton.setTooltip(new Tooltip("Open selected Save Set in Snapshot Viewer"));
		openButton.disableProperty()
				.bind(selector.selectedSaveSetProperty().isNull().or(saveSetsPane.expandedProperty().not()));
		openButton.setOnAction(e -> actionManager.openSaveSet(selector.selectedSaveSetProperty().get()));
		editButton.disableProperty()
				.bind(selector.selectedSaveSetProperty().isNull().or(saveSetsPane.expandedProperty().not()));
		editButton.setOnAction(e -> {
			DataProviderWrapper wrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
			if (canExecute("Edit Save Set", wrapper.getName() + " data provider does not support editing of save sets.",
					wrapper.getProvider()::isSaveSetSavingSupported)) {
				actionManager.editSaveSet(selector.selectedSaveSetProperty().get());
			}
		});

		newButton.setOnAction(e -> {
			DataProviderWrapper wrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
			if (canExecute("New Save Set",
					wrapper.getName() + " data provider does not support creation of new save sets.",
					wrapper.getProvider()::isSaveSetSavingSupported)) {
				actionManager.newSaveSet();
			}
		});
		importButton.disableProperty()
				.bind(selector.selectedBaseLevelProperty().isNull().or(saveSetsPane.expandedProperty().not()));
		importButton.setOnAction(e -> {
			DataProviderWrapper wrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
			if (canExecute("Import Data", wrapper.getName() + " data provider does not support data importing.",
					wrapper.getProvider()::isImportSupported)) {
				new ImportDataDialog(BrowserView.this).openAndWait().ifPresent(actionManager::importFrom);
			}
		});

		setUpSetButtons(newButton, importButton, SaveRestoreService.getInstance().getSelectedDataProvider());

		setUpTitlePaneNode(titleText, true);
		setUpTitlePaneNode(newButton, false);
		setUpTitlePaneNode(importButton, false);
		setUpTitlePaneNode(editButton, false);
		setUpTitlePaneNode(openButton, false);
		titleBox.addRow(0, titleText, importButton, newButton, editButton, openButton);
		titleBox.setMaxWidth(Double.MAX_VALUE);
		titleText.setMaxWidth(Double.MAX_VALUE);
		saveSetsPane.setGraphic(titleBox);
		saveSetsPane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		titleBox.prefWidthProperty().bind(scene.widthProperty().subtract(34));

		saveSetsPane.expandedProperty()
				.addListener((a, o, n) -> VBox.setVgrow(saveSetsPane, n ? Priority.ALWAYS : Priority.NEVER));

		saveSetsPane.setOnMouseEntered(me -> {
			inSaveSetsPane = true;
		});

		saveSetsPane.setOnMouseExited(me -> {
			inSaveSetsPane = false;
		});

		return saveSetsPane;
	}

	private void setUpSetButtons(Button newButton, Button importButton, DataProviderWrapper dpw) {
		newButton.disableProperty().unbind();
		if (dpw == null) {
			importButton.setVisible(false);
			newButton.setDisable(true);
		} else {
			if (dpw.getProvider().areBaseLevelsSupported()) {
				importButton.setVisible(true);
				newButton.disableProperty()
						.bind(selector.selectedBaseLevelProperty().isNull().or(saveSetsPane.expandedProperty().not()));
			} else {
				importButton.setVisible(false);
				newButton.disableProperty().bind(saveSetsPane.expandedProperty().not());
			}
		}
	}

	private boolean canExecute(String title, String message, BooleanSupplier testingFunction) {
		if (!testingFunction.getAsBoolean()) {
			FXMessageDialog.openInformation(getSite().getShell(), title, message);
			return false;
		}
		return true;
	}

	private Node createSnapshotsPane(Scene scene) {
		BorderPane content = new BorderPane();
		snapshotsList = new ListView<>();
		snapshotsList.setCellFactory(e -> new ListCell<Snapshot>() {
			public void updateItem(Snapshot item, boolean empty) {
				super.updateItem(item, empty);
				getStyleClass().remove("tagged-cell");
				if (item == null || empty) {
					setTooltip(null);
					setText(null);
				} else {
					StringBuilder sb = new StringBuilder(300);
					sb.append(item.getComment());
					String message = item.getTagMessage().orElse(null);
					String tag = item.getTagName().orElse(null);
					if (tag != null) {
						sb.append("\n\n").append(tag).append('\n').append(message);
						getStyleClass().add("tagged-cell");
					}
					setTooltip(new Tooltip(sb.toString()));
					if (searchMode) {
						StringBuilder text = new StringBuilder(300);
						item.getSaveSet().getBaseLevel()
								.ifPresent(e -> text.append('[').append(e).append(']').append(' '));
						text.append(item.getSaveSet().getPathAsString()).append('\n');
						text.append(item.toString());
						setText(text.toString());
					} else {
						setText(item.toString());
					}
				}
			}
		});
		snapshotsList.getStylesheets().add(BrowserView.class.getResource("taggedCell.css").toExternalForm());

		snapshotsList.setOnDragDetected(e -> {
			Dragboard db = snapshotsList.startDragAndDrop(TransferMode.ANY);
			ClipboardContent cc = new ClipboardContent();
			Snapshot snapshot = snapshotsList.getSelectionModel().getSelectedItem();
			snapshot.getSaveSet().updateBaseLevel();
			cc.put(SnapshotDataFormat.INSTANCE, snapshot);
			db.setContent(cc);
			e.consume();
		});

		snapshotsList.setOnMouseClicked(e -> {
			Snapshot snapshot = snapshotsList.getSelectionModel().getSelectedItem();
			if (e.getClickCount() == 2 && snapshot != null) {
				actionManager.openSnapshot(snapshot);
			}
		});
		snapshotsList.selectionModelProperty().get().selectedItemProperty().addListener((a, o, n) -> {
			final SelectionChangedEvent e = new SelectionChangedEvent(BrowserView.this, getSelection());
			selectionChangedListener.forEach(l -> l.selectionChanged(e));
		});

		content.setCenter(snapshotsList);
		snapshotsPane = new TitledPane("Snapshots", content);
		snapshotsPane.setMaxHeight(Double.MAX_VALUE);

		GridPane titleBox = new GridPane();
		titleBox.setHgap(5);
		Label titleText = new Label("Snapshots");
		titleText.textProperty().bind(snapshotsPane.textProperty());
		Button tagButton = new UnfocusableButton("Tag");
		tagButton.setTooltip(new Tooltip("Tag selected snapshot"));
		tagButton.disableProperty().bind(snapshotsList.selectionModelProperty().get().selectedItemProperty().isNull()
				.or(snapshotsPane.expandedProperty().not()));
		tagButton.setOnAction(e -> {
			DataProviderWrapper wrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
			if (canExecute("Tag Snapshot", wrapper.getName() + " data provider does not support tagging.",
					wrapper.getProvider()::isTaggingSupported)) {
				final Snapshot snapshot = snapshotsList.getSelectionModel().getSelectedItem();
				final FXTaggingDialog dialog = new FXTaggingDialog(getSite().getShell());
				dialog.openAndWait().ifPresent(a -> actionManager.tagSnapshot(snapshot, a, dialog.getMessage()));
			}
		});
		Button openButton = new UnfocusableButton("Open");
		openButton.setTooltip(new Tooltip("Open selected snapshot in a new Snapshot Viewer"));
		openButton.disableProperty().bind(snapshotsList.selectionModelProperty().get().selectedItemProperty().isNull()
				.or(snapshotsPane.expandedProperty().not()));
		openButton.setOnAction(e -> actionManager.openSnapshot(snapshotsList.getSelectionModel().getSelectedItem()));

		Button compareButton = new UnfocusableButton("Compare");
		compareButton.setTooltip(new Tooltip("Open selected snapshot the active Snapshot Viewer"));
		compareButton.disableProperty().bind(snapshotsList.selectionModelProperty().get().selectedItemProperty()
				.isNull().or(snapshotsPane.expandedProperty().not()));
		compareButton
				.setOnAction(e -> actionManager.compareSnapshot(snapshotsList.getSelectionModel().getSelectedItem()));

		setUpTitlePaneNode(titleText, true);
		setUpTitlePaneNode(tagButton, false);
		setUpTitlePaneNode(openButton, false);
		setUpTitlePaneNode(compareButton, false);

		SaveRestoreService.getInstance().getPreferences().addPropertyChangeListener(e -> {
			if (SaveRestoreService.PREF_NUMBER_OF_SNAPSHOTS.equals(e.getProperty())) {
				setUpFetchButtons(titleBox, titleText, openButton, compareButton, tagButton);
			}
		});
		setUpFetchButtons(titleBox, titleText, openButton, compareButton, tagButton);

		titleBox.setMaxWidth(Double.MAX_VALUE);
		titleText.setMaxWidth(Double.MAX_VALUE);
		snapshotsPane.setGraphic(titleBox);
		snapshotsPane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		titleBox.prefWidthProperty().bind(scene.widthProperty().subtract(34));
		snapshotsPane.expandedProperty()
				.addListener((a, o, n) -> VBox.setVgrow(snapshotsPane, n ? Priority.ALWAYS : Priority.NEVER));

		snapshotsPane.setOnMouseEntered(me -> {
			inSnapshotPane = true;
		});

		snapshotsPane.setOnMouseExited(me -> {
			inSnapshotPane = false;
		});

		return snapshotsPane;
	}

	private void setUpFetchButtons(GridPane titleBox, Label titleText, Button openButton, Button compareButton,
			Button tagButton) {
		int num = SaveRestoreService.getInstance().getNumberOfSnapshots();
		titleBox.getChildren().clear();
		if (num > 0) {
			Button nextButton = new UnfocusableButton("",
					new ImageView(new Image(BrowserView.class.getResourceAsStream("/icons/1rightarrow.png"))));
			nextButton.setTooltip(new Tooltip("Load Next Batch"));
			nextButton.setOnAction(e -> selector.readSnapshots(false, false));
			nextButton.disableProperty()
					.bind(selector.selectedSaveSetProperty().isNull().or(selector.allSnapshotsLoadedProperty()));

			Button nextAllButton = new UnfocusableButton("",
					new ImageView(new Image(BrowserView.class.getResourceAsStream("/icons/2rightarrow.png"))));
			nextAllButton.setTooltip(new Tooltip("Load All"));
			nextAllButton.setOnAction(e -> selector.readSnapshots(false, true));
			nextAllButton.disableProperty()
					.bind(selector.selectedSaveSetProperty().isNull().or(selector.allSnapshotsLoadedProperty()));

			setUpTitlePaneNode(nextButton, false);
			setUpTitlePaneNode(nextAllButton, false);
			titleBox.addRow(0, titleText, nextButton, nextAllButton, openButton, compareButton, tagButton);
		} else {
			titleBox.addRow(0, titleText, openButton, compareButton, tagButton);
		}
	}

	private void setUpElementsPaneTitle() {
		BaseLevelBrowser<BaseLevel> br = defaultBaseLevelBrowser;
		if (baseLevelBrowser != null && baseLevelBrowser.getFXContent().getParent() != null) {
			br = baseLevelBrowser;
		}
		BaseLevel bl = selector.selectedBaseLevelProperty().get();
		if (bl == null) {
			baseLevelPane.setText(br.getTitleFor(Optional.empty(), Optional.empty()));
		} else {
			if (selector.isDefaultBranch()) {
				baseLevelPane.setText(br.getTitleFor(Optional.of(bl), Optional.empty()));
			} else {
				Branch branch = selector.selectedBranchProperty().get();
				baseLevelPane.setText(br.getTitleFor(Optional.of(bl), Optional.of(branch.getShortName())));
			}
		}
	}

	private void initBaseLevelBrowser() {

		Optional<BaseLevelBrowser<BaseLevel>> browser = new BaseLevelBrowserProvider().getBaseLevelBrowser();
		baseLevelPane = createBaseLevelsPane(browser.orElse(null));
		VBox.setVgrow(baseLevelPane, Priority.NEVER);
		baseLevelPane.setExpanded(true);

		selector.selectedBranchProperty().addListener((a, o, n) -> {
			if (selector.isDefaultBranch()) {
				Activator.getDefault().getDialogSettings().put(SETTINGS_SELECTED_BRANCH, (String) null);
			} else {
				Activator.getDefault().getDialogSettings().put(SETTINGS_SELECTED_BRANCH, n.getShortName());
			}
		});
		selector.selectedBaseLevelProperty().addListener((a, o, n) -> {
			setUpElementsPaneTitle();
			if (n == null) {
				saveSetsPane.setText("Save Sets");
			} else {
				saveSetsPane.setText("Save Sets for " + n.getPresentationName());
			}
		});
		if (baseLevelBrowser != null) {
			baseLevelBrowser.selectedBaseLevelProperty().addListener((a, o, n) -> {
				if (baseLevelBrowser.getFXContent().getParent() != null) {
					selector.selectedBaseLevelProperty().setValue(n);
				}
			});
		}
		defaultBaseLevelBrowser.selectedBaseLevelProperty().addListener((a, o, n) -> {
			if (defaultBaseLevelBrowser.getFXContent().getParent() != null) {
				selector.selectedBaseLevelProperty().setValue(n);
			}
		});
		selector.baseLevelsProperty().addListener((a, o, n) -> {
			try {
				defaultBaseLevelBrowser.availableBaseLevelsProperty().set(defaultBaseLevelBrowser.transform(n));
				defaultBaseLevelBrowser.selectedBaseLevelProperty()
						.setValue(selector.selectedBaseLevelProperty().get());
				if (baseLevelBrowser != null) {
					baseLevelBrowser.availableBaseLevelsProperty().set(baseLevelBrowser.transform(n));
					BaseLevel base = selector.selectedBaseLevelProperty().get();
					if (base == null) {
						baseLevelBrowser.selectedBaseLevelProperty().setValue(null);
					} else {
						List<BaseLevel> bl = baseLevelBrowser.transform(Arrays.asList(base));
						baseLevelBrowser.selectedBaseLevelProperty().setValue(bl.isEmpty() ? null : bl.get(0));
					}
				}
			} catch (RuntimeException e) {
				FXMessageDialog.openError(getSite().getShell(), "Base Level Error", e.getMessage());
			}
		});
		selector.selectedBranchProperty().addListener((a, o, n) -> setUpElementsPaneTitle());
		selector.selectedSaveSetProperty().addListener((a, o, n) -> {
			if (n == null) {
				snapshotsPane.setText("Snapshots");
			} else {
				snapshotsPane.setText("Snapshots of " + n.getName());
			}
		});
		selector.saveSetsProperty().addListener((a, o, saveSets) -> {
			TreeItem<SaveSetWrapper> root = new SaveSetTreeItem("Root");
			Map<String, TreeItem<SaveSetWrapper>> items = new HashMap<>();
			items.put("", root);
			for (SaveSet set : saveSets) {
				String folder = set.getFolder();
				TreeItem<SaveSetWrapper> parent = items.get(folder);
				if (parent == null) {
					parent = new SaveSetTreeItem(folder);
					items.put(folder, parent);

					String[] path = parent.getValue().path;
					TreeItem<SaveSetWrapper> currentChild = parent;
					for (int i = path.length - 1; i > -1; i--) {
						String m = makeStringFromParts(path, 0, i);
						TreeItem<SaveSetWrapper> ti = items.get(m);
						if (ti == null) {
							ti = new SaveSetTreeItem(m);
							items.put(m, ti);
							ti.getChildren().add(currentChild);
							currentChild = ti;
						} else {
							ti.getChildren().add(currentChild);
							break;
						}
					}
				}
				parent.getChildren().add(new SaveSetTreeItem(new SaveSetWrapper(set, null)));
			}
			saveSetsTree.setRoot(root);
			root.setExpanded(true);
		});
		selector.snapshotsProperty().addListener((a, o, n) -> {
			searchMode = false;
			snapshotsList.setItems(FXCollections.observableArrayList(n));
		});

		List<DataProviderWrapper> dpws = SaveRestoreService.getInstance().getDataProviders();
		if (!dpws.isEmpty()) {
			IDialogSettings settings = Activator.getDefault().getDialogSettings();
			String selectedDataProvider = settings.get(SETTINGS_SELECTED_DATA_PROVIDER);
			if (selectedDataProvider != null) {
				selector.setFirstTimeBranch(settings.get(SETTINGS_SELECTED_BRANCH));
			}
			DataProviderWrapper dpw = dpws.get(0);
			for (DataProviderWrapper w : dpws) {
				if (w.getId().equals(selectedDataProvider)) {
					dpw = w;
					break;
				}
			}
			SaveRestoreService.getInstance().setSelectedDataProvider(dpw);
		}
	}

	private void updateForDataProviderChange() {

		DataProviderWrapper dataProviderWrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		settings.put(SETTINGS_SELECTED_DATA_PROVIDER, dataProviderWrapper.getId());

		createUI();
	}

	private static String makeStringFromParts(String[] parts, int from, int to) {
		if (from == to)
			return "";
		StringBuilder sb = new StringBuilder(100);
		sb.append(parts[from]);
		for (int i = from + 1; i < to; i++) {
			sb.append('/').append(parts[i]);
		}
		return sb.toString();
	}

	private static void setUpTitlePaneNode(Region node, boolean isTitleText) {
		if (isTitleText) {
			setGridConstraints(node, true, false, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
		} else {
			setGridConstraints(node, false, false, HPos.RIGHT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
			node.setPadding(new Insets(3, 5, 3, 5));
		}
	}

	/**
	 * Sets the snapshot search results. The list of snapshots replaces the current
	 * list of snapshots and the expression is visible in the title of the snapshots
	 * pane. The base level and save sets panes are collapsed.
	 *
	 * @param expression the expression used for searching
	 * @param snapshots  the results
	 */
	void setSearchResults(String expression, List<Snapshot> snapshots) {
		if (baseLevelPane != null) {
			baseLevelPane.expandedProperty().set(false);
		}
		saveSetsPane.expandedProperty().set(false);
		snapshotsPane.expandedProperty().set(true);
		snapshotsPane.setText("Search results for '" + expression + "'");
		searchMode = true;
		snapshotsList.setItems(FXCollections.observableArrayList(snapshots));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.fx.ui.workbench3.FXViewPart#setFxFocus()
	 */
	@Override
	protected void setFxFocus() {
		// no focus
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.
	 * eclipse.jface.viewers. ISelectionChangedListener)
	 */
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListener.add(listener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(
	 * org.eclipse.jface.viewers. ISelectionChangedListener)
	 */
	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionChangedListener.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.
	 * viewers.ISelection)
	 */
	@Override
	public void setSelection(ISelection selection) {
		// nothing to select
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		if (!inSnapshotPane) {
			return new LazySnapshotStructuredSelection(null, null);
		}
		Snapshot selectedSnapshot = snapshotsList.selectionModelProperty().get().getSelectedItem();
		return selectedSnapshot == null ? new LazySnapshotStructuredSelection(null, null)
				: new LazySnapshotStructuredSelection(selectedSnapshot,
						SaveRestoreService.getInstance().getSelectedDataProvider().getProvider());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.window.IShellProvider#getShell()
	 */
	@Override
	public Shell getShell() {
		return getSite().getShell();
	}

	/**
	 * Determines the current {@link DataProvider} as the UI creation depends on it.
	 * This method assumes that there is at least one {@link DataProvider}
	 * available, exception is thrown if the list of {@link DataProvider}s is empty
	 * or null, see {@link SaveRestoreService#getDataProviders()}.
	 * 
	 * If a selection of {@link DataProvider} has not been made, the first available
	 * {@link DataProvider} is set as the current one.
	 * 
	 */
	private void ensureCurrentDataProvider() {
		SaveRestoreService saveRestoreService = SaveRestoreService.getInstance();

		if (saveRestoreService.getSelectedDataProvider() == null) {

			List<DataProviderWrapper> dataProviderWrappers = saveRestoreService.getDataProviders();

			IDialogSettings settings = Activator.getDefault().getDialogSettings();
			String selectedDataProvider = settings.get(SETTINGS_SELECTED_DATA_PROVIDER);
			if (selectedDataProvider == null) { // No data provider found in preferences
				saveRestoreService.setSelectedDataProvider(dataProviderWrappers.get(0));
			}

			// Set the data provider defined in the preferences
			for (DataProviderWrapper wrapper : dataProviderWrappers) {
				if (wrapper.getId().equals(selectedDataProvider)) {
					saveRestoreService.setSelectedDataProvider(wrapper);
					return;
				}
			}
		}
	}

	/**
	 * Implements a {@link IMenuListener} in order to provide logic to add/remove
	 * and enable/disable context menu items depending of the current UI context.
	 * 
	 * @author georgweiss Created 15 Jan 2019
	 */
	private class ContextMenuListener implements IMenuListener {

		@Override
		public void menuAboutToShow(IMenuManager menuManager) {

			// First remove all items in the context menu...
			menu.removeAll();

			// ...then determine what to add and optionally enable
			if (inSnapshotPane) {
				menu.add(deleteTagAction);
				Snapshot snapshot = (Snapshot) snapshotsList.getSelectionModel().getSelectedItem();
				deleteTagAction.setEnabled(snapshot != null && snapshot.getTagName().isPresent());
			}

			if (inSaveSetsPane) {
				menu.add(deleteSaveSetAction);
				SaveSetTreeItem item = (SaveSetTreeItem) saveSetsTree.getSelectionModel().getSelectedItem();
				deleteSaveSetAction.setEnabled(item != null && item.getValue() != null && item.getValue().set != null);
			}

			if (inTreeViewBrowser) {
				menu.add(deleteSaveSetAction);
				deleteSaveSetAction.setEnabled(treeView.getSelectedTreeNodeType() != null
						&& treeView.getSelectedTreeNodeType().equals(TreeNodeType.SAVESET));
				menu.add(editSaveSetAction);
				editSaveSetAction.setEnabled(treeView.getSelectedTreeNodeType() != null
						&& treeView.getSelectedTreeNodeType().equals(TreeNodeType.SAVESET));
			}
		}
	}

	private class DeleteSaveSetAction extends Action {

		public DeleteSaveSetAction() {
			super("Delete Save Set");
		}

		@Override
		public void run() {

			DataProviderWrapper wrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
			if (canExecute("Delete Save Set",
					wrapper.getName() + " data provider does not support deleting of save sets.",
					wrapper.getProvider()::isSaveSetSavingSupported)) {
				if (inSaveSetsPane) {
					SaveSetTreeItem item = (SaveSetTreeItem) saveSetsTree.getSelectionModel().getSelectedItem();
					if (FXMessageDialog.openQuestion(getSite().getShell(), "Delete Save Set",
							"Are you sure you want to delete save set '" + item.getValue().set.getPathAsString()
									+ "'?")) {
						actionManager.deleteSaveSet(item.getValue().set);
					}
				}
				else if(inTreeViewBrowser) {
					
				}
			}
		}
	}

	private class DeleteTagAction extends Action {

		public DeleteTagAction() {
			super("Remove Tag");
		}

		@Override
		public void run() {
			DataProviderWrapper wrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
			if (canExecute("Tag Snapshot", wrapper.getName() + " data provider does not support tagging.",
					wrapper.getProvider()::isTaggingSupported)) {
				Snapshot item = snapshotsList.getSelectionModel().getSelectedItem();
				if (FXMessageDialog.openQuestion(getSite().getShell(), "Remove Tag",
						"Are you sure you want to remove the tag '" + item.getTagName().get() + "' from snapshot '"
								+ item.getDate() + "'?")) {
					actionManager.deleteTag(item);
				}
			}
		}
	}

	private class EditSaveSetAction extends Action {

		public EditSaveSetAction() {
			super("Edit Save Set");
		}

		@Override
		public void run() {
			DataProviderWrapper wrapper = SaveRestoreService.getInstance().getSelectedDataProvider();
			if (canExecute("Edit Save Set", wrapper.getName() + " data provider does not support editing of save sets.",
					wrapper.getProvider()::isSaveSetSavingSupported)) {
				TreeItem<TreeNode> item = treeView.getSelectionModel().getSelectedItem();
				SaveSet saveSet = new SaveSet(new Branch(), Optional.empty(),
						new String[] { item.getValue().getName() },
						SaveRestoreService.getInstance().getSelectedDataProvider().getId());
				saveSet.setSaveSetId(Integer.toString(item.getValue().getId()));
				saveSet.setFullyQualifiedName("/config/" + item.getValue().getId());
				saveSet.setUserName(item.getValue().getUserName());
				actionManager.editSaveSet(saveSet);
			}
		}
	}
}
