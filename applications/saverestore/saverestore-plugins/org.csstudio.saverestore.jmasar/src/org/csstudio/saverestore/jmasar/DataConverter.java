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

import java.util.List;
import java.util.stream.Collectors;


import org.csstudio.saverestore.data.TreeViewNode;

import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;

public class DataConverter {

	public static TreeViewNode fromJMasarFolder(Folder folder) {
		
		TreeViewNode node = fromJMasarNode(null, folder);
		List<TreeViewNode> children =  folder.getChildNodes().stream().map(n -> fromJMasarNode(node, n))
			.collect(Collectors.toList());
		node.setLeaf(false);
		node.setChildren(children);
		return node;
	}
	
	public static TreeViewNode fromJMasarNode(TreeViewNode parent, Node node) {
		
		TreeViewNode treeViewNode = new TreeViewNode(node.getName());
		treeViewNode.setParent(parent);
		treeViewNode.setId(node.getId());
		treeViewNode.setLeaf(node.getNodeType().equals(NodeType.CONFIGURATION));
		return treeViewNode;
	}
}
