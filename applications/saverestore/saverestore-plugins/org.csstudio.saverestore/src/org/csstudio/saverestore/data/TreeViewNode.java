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

package org.csstudio.saverestore.data;

import java.util.ArrayList;
import java.util.List;

public class TreeViewNode {

	private int id;
	private TreeViewNode parent;
	private List<TreeViewNode> children;
	private String name;
	private boolean isLeaf;
	
	public TreeViewNode(String name) {
		this.name = name;
	}
	
	public TreeViewNode getParent() {
		return parent;
	}

	public void setParent(TreeViewNode parent) {
		this.parent = parent;
	}

	public List<TreeViewNode> getChildren() {
		if(children == null) {
			children = new ArrayList<>();
		}
		return children;
	}

	public void setChildren(List<TreeViewNode> children) {
		this.children = children;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	

	

	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return name;
	}
}
