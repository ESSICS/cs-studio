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

package org.csstudio.saverestore.data.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * @author georgweiss
 * Created 3 Jan 2019
 */
public class FolderTreeNode extends TreeNode{

	private List<TreeNode> children;

	public FolderTreeNode(int id, String name, TreeNodeType type) {
		super(id, name, type);
	}

	public List<TreeNode> getChildren() {
		if(children == null) {
			children = new ArrayList<>();
		}
		return children;
	}

	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}
}
