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

package org.csstudio.saverestore.jmasar;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.csstudio.saverestore.CompletionNotifier;
import org.csstudio.saverestore.DataProvider;
import org.csstudio.saverestore.DataProviderException;
import org.csstudio.saverestore.SearchCriterion;
import org.csstudio.saverestore.UnsupportedActionException;
import org.csstudio.saverestore.data.BaseLevel;
import org.csstudio.saverestore.data.Branch;
import org.csstudio.saverestore.data.SaveSet;
import org.csstudio.saverestore.data.SaveSetData;
import org.csstudio.saverestore.data.TreeViewNode;
import org.csstudio.saverestore.data.VSnapshot;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;

public class JMasarDataProvider implements DataProvider {

	public static final String ID = "org.csstudio.saverestore.jmasar.dataprovider";

	private JMasarClient jmasarClient;

	public JMasarDataProvider() {
		jmasarClient = new JMasarClient();
	}

	@Override
	public void initialise() throws DataProviderException {
	}

	@Override
	public void addCompletionNotifier(CompletionNotifier notifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeCompletionNotifier(CompletionNotifier notifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public Branch[] getBranches() throws DataProviderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BaseLevel[] getBaseLevels(Branch branch) throws DataProviderException {
		return null;
	}

	@Override
	public SaveSet[] getSaveSets(Optional<BaseLevel> baseLevel, Branch branch) throws DataProviderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.csstudio.saverestore.data.Snapshot[] getSnapshots(SaveSet set, boolean loadAll,
			Optional<org.csstudio.saverestore.data.Snapshot> fromThisOneBack) throws DataProviderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.csstudio.saverestore.data.Snapshot[] findSnapshots(String expression, Branch branch,
			List<SearchCriterion> criteria, Optional<Date> start, Optional<Date> end)
			throws DataProviderException, UnsupportedActionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VSnapshot getSnapshotContent(org.csstudio.saverestore.data.Snapshot snapshot) throws DataProviderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SaveSetData getSaveSetContent(SaveSet set) throws DataProviderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean reinitialise() throws DataProviderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean synchronise() throws DataProviderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Branch createNewBranch(Branch originalBranch, String newBranchName)
			throws DataProviderException, UnsupportedActionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SaveSetData saveSaveSet(SaveSetData set, String comment)
			throws DataProviderException, UnsupportedActionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteSaveSet(SaveSet set, String comment) throws DataProviderException, UnsupportedActionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public VSnapshot saveSnapshot(VSnapshot data, String comment) throws DataProviderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public org.csstudio.saverestore.data.Snapshot tagSnapshot(org.csstudio.saverestore.data.Snapshot snapshot,
			Optional<String> tagName, Optional<String> tagMessage)
			throws DataProviderException, UnsupportedActionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean importData(SaveSet source, Branch toBranch, Optional<BaseLevel> toBaseLevel, ImportType type)
			throws DataProviderException, UnsupportedActionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<SearchCriterion> getSupportedSearchCriteria() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * The concept of (git) branch does not make sense in JMasar.
	 * 
	 * return Always <code>false</code>
	 */
	@Override
	public boolean areBranchesSupported() {
		return false;
	}

	@Override
	public boolean mimicksFileSystemStructure() {
		return true;
	}

	@Override
	public TreeViewNode getTreeRootNode() {
		Folder root = jmasarClient.getRoot();
		Config c = Config.builder().name("config").build();

		TreeViewNode rootNode = DataConverter.fromJMasarFolder(root);

		TreeViewNode t = DataConverter.fromJMasarNode(rootNode.getChildren().get(2), c);

		rootNode.getChildren().get(2).getChildren().add(t);

		return rootNode;
	}

	@Override
	public List<TreeViewNode> getChildNodes(TreeViewNode parentNode) {

		List<Node> childNodes = jmasarClient.getChildNodes(parentNode.getId());
		return childNodes.stream().map(n -> DataConverter.fromJMasarNode(parentNode, n)).collect(Collectors.toList());

	}
}
