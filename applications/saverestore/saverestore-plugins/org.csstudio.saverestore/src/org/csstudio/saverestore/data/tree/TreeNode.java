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

package org.csstudio.saverestore.data.tree;

import java.util.Date;

/**
 * @author georgweiss
 * Created 7 Jan 2019
 */
public abstract class TreeNode {
	
	private int id;
	private String name;
	private String userName;
	private Date LastModified;

	private TreeNodeType type;
	
	public TreeNode(int id, String name, TreeNodeType type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public TreeNodeType getType() {
		return type;
	}
	public void setType(TreeNodeType type) {
		this.type = type;
	}
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public Date getLastModified() {
		return LastModified;
	}

	public void setLastModified(Date lastModified) {
		LastModified = lastModified;
	}

	public abstract boolean isLeaf();
	
	@Override
	public String toString() {
		return name;
	}

}


