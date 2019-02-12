/*
 * Copyright (C) 2018 European Spallation Source ERIC.
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

package org.csstudio.saverestore.ui.browser.jmasar;

import static org.csstudio.ui.fx.util.FXUtilities.setGridConstraints;

import org.eclipse.fx.ui.workbench3.FXViewPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import se.esss.ics.masar.model.Folder;

public class JMasarBrowserView extends FXViewPart implements ISelectionProvider, IShellProvider {

	private TreeView<Folder> jmasarTree;
	private TitledPane baseLevelPane;
	//private VBox dataPane;
	private VBox mainPane;
	
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
	
	/*
     * (non-Javadoc)
     *
     * @see org.eclipse.fx.ui.workbench3.FXViewPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
    	super.createPartControl(parent);
    }

	@Override
	protected Scene createFxScene() {
		
		
		BorderPane main = new BorderPane();
		Scene scene = new Scene(main);

		jmasarTree = new TreeView<>();
		jmasarTree.setShowRoot(false);
		
		GridPane content = new GridPane();
		setGridConstraints(jmasarTree, true, true, Priority.ALWAYS, Priority.ALWAYS);
        content.add(jmasarTree, 0, 0);
        
        Node elements = createBaseLevelsPane(content, scene);

        //dataPane = new VBox();
        mainPane = new VBox();
        VBox.setVgrow(content, Priority.ALWAYS);
        
        //dataPane.getChildren().addAll(elements);
        mainPane.getChildren().addAll(elements);
        main.setCenter(mainPane);  

		return scene;
	}
	
	private Node createBaseLevelsPane(Node jmasarTreeContainer, Scene scene) {
        BorderPane content = new BorderPane();
        content.setCenter(jmasarTreeContainer);
      
        
        baseLevelPane = new TitledPane("MUSIGNY", content);
        baseLevelPane.setMaxHeight(Double.MAX_VALUE);

        GridPane titleBox = new GridPane();
        titleBox.setHgap(5);
        Label titleText = new Label("CORTON");
        titleText.textProperty().bind(baseLevelPane.textProperty());
        

        setUpTitlePaneNode(titleText, true);
       
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleText.setMaxWidth(Double.MAX_VALUE);
        baseLevelPane.setGraphic(titleBox);
        baseLevelPane.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        titleBox.prefWidthProperty().bind(scene.widthProperty().subtract(34));

      
        return baseLevelPane;
    }
	
	private void setUpTitlePaneNode(Region node, boolean isTitleText) {
        if (isTitleText) {
            setGridConstraints(node, true, false, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
        } else {
            setGridConstraints(node, false, false, HPos.RIGHT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
            node.setPadding(new Insets(3, 5, 3, 5));
        }
    }

	@Override
	public Shell getShell() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public ISelection getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSelection(ISelection arg0) {
		// TODO Auto-generated method stub

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

}
