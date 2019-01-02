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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;

public class JMasarClient {

	private Client client;
	private String jmasarServiceUrl;

	public JMasarClient() {

		jmasarServiceUrl = Activator.getInstance().getJMasarServiceUrl();
		DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
		defaultClientConfig.getClasses().add(JacksonJsonProvider.class);
		client = Client.create(defaultClientConfig);
		
	}

	public List<Node> getFoldersInRoot() {

		WebResource webResource = client.resource(jmasarServiceUrl + "/folder/" + Node.ROOT_NODE_ID);

		ClientResponse response = webResource.accept("application/json; charset=UTF-8").get(ClientResponse.class);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		Folder folder = response.getEntity(Folder.class);
		return folder.getChildNodes();

	}
	
	public Folder getRoot() {
		WebResource webResource = client.resource(jmasarServiceUrl + "/folder/" + Node.ROOT_NODE_ID);

		ClientResponse response = webResource.accept("application/json; charset=UTF-8").get(ClientResponse.class);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		return response.getEntity(Folder.class);
	}
	
	public List<Node> getChildNodes(int id){
		WebResource webResource = client.resource(jmasarServiceUrl + "/folder/" + id);
		
		ClientResponse response = webResource.accept("application/json; charset=UTF-8").get(ClientResponse.class);
		
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		
		Folder folder = response.getEntity(Folder.class);
		return folder.getChildNodes();
	}
	
	public List<Snapshot> getSnapshots(int id){
		WebResource webResource = client.resource(jmasarServiceUrl + "/config/" + id + "/snapshots");
		
		ClientResponse response = webResource.accept("application/json; charset=UTF-8").get(ClientResponse.class);
		
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		
		return response.getEntity(new GenericType<List<Snapshot>>(){});
	}
}
