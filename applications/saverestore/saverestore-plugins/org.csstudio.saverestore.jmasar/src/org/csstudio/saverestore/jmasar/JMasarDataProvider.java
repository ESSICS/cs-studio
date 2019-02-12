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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.csstudio.saverestore.BrowserPresentationType;
import org.csstudio.saverestore.CompletionNotifier;
import org.csstudio.saverestore.DataProvider;
import org.csstudio.saverestore.DataProviderException;
import org.csstudio.saverestore.SaveRestoreService;
import org.csstudio.saverestore.SearchCriterion;
import org.csstudio.saverestore.UnsupportedActionException;
import org.csstudio.saverestore.data.BaseLevel;
import org.csstudio.saverestore.data.Branch;
import org.csstudio.saverestore.data.SaveSet;
import org.csstudio.saverestore.data.SaveSetData;
import org.csstudio.saverestore.data.SnapshotEntry;
import org.csstudio.saverestore.data.VSnapshot;
import org.csstudio.saverestore.data.tree.FolderTreeNode;
import org.csstudio.saverestore.data.tree.TreeNode;
import org.csstudio.saverestore.data.tree.TreeNodeType;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;
import se.esss.ics.masar.model.SnapshotItem;

public class JMasarDataProvider implements DataProvider {

	public static final String ID = "org.csstudio.saverestore.jmasar.dataprovider";

	private JMasarClient jmasarClient;

	public JMasarDataProvider() {
		jmasarClient = new JMasarClient();

		Activator.getInstance().getPreferenceStore().addPropertyChangeListener(e -> {
			String property = e.getProperty();
			if (Activator.PREF_URL.equals(property)) {
				SaveRestoreService.getInstance().reselectDataProvider();
			}
		});
	}

	@Override
	public void initialise() throws DataProviderException {
		jmasarClient.setJMasarServiceUrl(Activator.getInstance().getJMasarServiceUrl());
	}

	@Override
	public void addCompletionNotifier(CompletionNotifier notifier) {
	}

	@Override
	public void removeCompletionNotifier(CompletionNotifier notifier) {
	}

	@Override
	public Branch[] getBranches() throws DataProviderException {
		return null;
	}

	@Override
	public BaseLevel[] getBaseLevels(Branch branch) throws DataProviderException {
		return null;
	}

	@Override
	public SaveSet[] getSaveSets(Optional<BaseLevel> baseLevel, Branch branch) throws DataProviderException {
		return null;
	}

	@Override
	public org.csstudio.saverestore.data.Snapshot[] getSnapshots(SaveSet set, boolean loadAll,
			Optional<org.csstudio.saverestore.data.Snapshot> fromThisOneBack) throws DataProviderException {
		return null;
	}

	@Override
	public org.csstudio.saverestore.data.Snapshot[] findSnapshots(String expression, Branch branch,
			List<SearchCriterion> criteria, Optional<Date> start, Optional<Date> end)
			throws DataProviderException, UnsupportedActionException {
		return null;
	}

	@Override
	public boolean isTaggingSupported() {
		return false;
	}

	@Override
	public boolean isSaveSetSavingSupported() {
		return true;
	}

	@Override
	public VSnapshot getSnapshotContent(org.csstudio.saverestore.data.Snapshot snapshot) throws DataProviderException {
		try {
			Snapshot snapshotData = jmasarClient.getSnapshot(Integer.parseInt(snapshot.getSnapshotId()));
			List<SnapshotItem> snapshotItems = snapshotData.getSnapshotItems();

			List<SnapshotEntry> snapshotEntries = snapshotItems.stream()
					.map(i -> DataConverter.fromJMasarSnapshotItem(i)).collect(Collectors.toList());

			VSnapshot vSnapshot = new VSnapshot(snapshot, snapshotEntries,
					Instant.ofEpochMilli(snapshotData.getCreated().getTime()));

			return vSnapshot;

		} catch (NumberFormatException e) {
			throw new DataProviderException("Snapshot id \"" + snapshot.getSnapshotId() + "\" is not an integer.");
		}
	}

	@Override
	public SaveSetData getSaveSetContent(SaveSet set) throws DataProviderException {

		Config config = jmasarClient.getConfiguration(Integer.parseInt(set.getSaveSetId()));

		return DataConverter.fromJMasarConfig(set, config);
	}

	@Override
	public boolean reinitialise() throws DataProviderException {
		return false;
	}

	@Override
	public boolean synchronise() throws DataProviderException {
		return false;
	}

	@Override
	public Branch createNewBranch(Branch originalBranch, String newBranchName)
			throws DataProviderException, UnsupportedActionException {
		return null;
	}

	@Override
	public SaveSetData saveSaveSet(SaveSetData set, String comment)
			throws DataProviderException, UnsupportedActionException {
		Config config = DataConverter.toJMasarConfig(set);
		Config updatedConfig = jmasarClient.saveConfig(config);
		return DataConverter.fromJMasarConfig(set.getDescriptor(), updatedConfig);
	}

	@Override
	public boolean deleteSaveSet(SaveSet set, String comment) throws DataProviderException, UnsupportedActionException {
		return false;
	}

	@Override
	public VSnapshot saveSnapshot(VSnapshot data, String comment) throws DataProviderException {
		
		String userName = System.getProperty("user.name");
		String snapshotName = data.getTimestamp().toString();
		
		jmasarClient.commitSnapshot(data.getSnapshotId(), snapshotName, userName, comment);
		
		org.csstudio.saverestore.data.Snapshot snapshotData = 
				new org.csstudio.saverestore.data.Snapshot(data.getSaveSet(), data.getTimestamp(), comment, userName);
		
		VSnapshot snapshot = new VSnapshot(snapshotData, data.getEntries(), data.getTimestamp());
		
		return snapshot;
	}

	@Override
	public org.csstudio.saverestore.data.Snapshot tagSnapshot(org.csstudio.saverestore.data.Snapshot snapshot,
			Optional<String> tagName, Optional<String> tagMessage)
			throws DataProviderException, UnsupportedActionException {
		return null;
	}

	@Override
	public boolean importData(SaveSet source, Branch toBranch, Optional<BaseLevel> toBaseLevel, ImportType type)
			throws DataProviderException, UnsupportedActionException {
		return false;
	}

	@Override
	public List<SearchCriterion> getSupportedSearchCriteria() {
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
	public BrowserPresentationType preferredBrowserRepresentationType() {
		return BrowserPresentationType.TREE_ONLY;
	}

	@Override
	public TreeNode getTreeRootNode() {
		Folder root = jmasarClient.getRoot();
		return DataConverter.fromJMasarFolder(root);
	}

	@Override
	public List<TreeNode> getChildNodes(FolderTreeNode parentNode) {

		if (parentNode.getType().equals(TreeNodeType.SAVESET)) {
			List<Snapshot> snapshots = jmasarClient.getSnapshots(parentNode);
			return snapshots.stream().map(s -> DataConverter.jmasarSnapshot2TreeNode(s)).collect(Collectors.toList());
		} else {
			List<Node> childNodes = jmasarClient.getChildNodes(parentNode.getId());
			return childNodes.stream().map(n -> DataConverter.fromJMasarNode(n)).collect(Collectors.toList());
		}
	}

	@Override
	public List<TreeNode> getSnapshots(FolderTreeNode configurationNode) {
		List<Snapshot> snapshots = jmasarClient.getSnapshots(configurationNode);
		return snapshots.stream().map(s -> DataConverter.jmasarSnapshot2TreeNode(s)).collect(Collectors.toList());
	}
	
	@Override
	public boolean commentOnSaveMandatory() {
		return false;
	}
	
	@Override
	public boolean isTakingSnapshotsSupported() {
        return true;
    }
	
	@Override
	public VSnapshot takeSnapshot(SaveSet saveSet) throws DataProviderException {
        
		Snapshot jmasarSnapshot = jmasarClient.takeSnapshot(saveSet.getSaveSetId());
		
		org.csstudio.saverestore.data.Snapshot snapshot = 
				new org.csstudio.saverestore.data.Snapshot(saveSet, 
						Instant.ofEpochMilli(jmasarSnapshot.getCreated().getTime()), 
						null, 
						null);
		
		List<SnapshotEntry> snapshotEntries = 
				jmasarSnapshot.getSnapshotItems().stream().map(si -> DataConverter.fromJMasarSnapshotItem(si)).collect(Collectors.toList());
		
		VSnapshot vSnapshot = new VSnapshot(snapshot, snapshotEntries, Instant.ofEpochMilli(jmasarSnapshot.getCreated().getTime()));
		vSnapshot.setSnapshotId(Integer.toString(jmasarSnapshot.getId()));
		return vSnapshot;
    }
}
