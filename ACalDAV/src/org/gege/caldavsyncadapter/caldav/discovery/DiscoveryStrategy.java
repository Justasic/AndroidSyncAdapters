/**
 * Copyright (c) 2012-2013, Gerald Garcia
 *
 * This file is part of Andoid Caldav Sync Adapter Free.
 *
 * Andoid Caldav Sync Adapter Free is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or at your option any later version.
 *
 * Andoid Caldav Sync Adapter Free is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Andoid Caldav Sync Adapter Free.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.gege.caldavsyncadapter.caldav.discovery;

import java.net.URI;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.gege.caldavsyncadapter.caldav.http.HttpPropFind;
import org.gege.caldavsyncadapter.caldav.http.HttpReport;

/**
 * This interface deals with the preparation of requests.
 * Some hosts/server implementations have their peculiarities
 * that an implementation can challenge.
 */
public interface DiscoveryStrategy {

	/**
	 * method is used to determine a matching strategy implementation
	 *
	 * An implementation shall only return true, if it supports host specific
	 * features.
	 *
	 * @param targetHost the host
	 * @return true if this strategy instance supports the given host
	 */
	boolean supportsTargetHost(HttpHost targetHost);

	/**
	 * create a strategy specific version of a PROPFIND request
	 *
	 * @param uri the target uri
	 * @param data the data payload
	 * @param depth the "Depth" header value
	 * @param targetHost the target host, used for "Host" header
	 * @return the prepared request
	 */
	HttpPropFind createPropFindRequest(URI uri, String data, int depth, HttpHost targetHost);

	/**
	 * create a strategy specific version of a Report request
	 *
	 * @param uri the target uri
	 * @param data the data payload
	 * @param depth the "Depth" header value
	 * @param targetHost the target host, used for "Host" header
	 * @return the prepared request
	 */
	HttpReport createReportRequest(URI uri, String data, int depth, HttpHost targetHost);

	/**
	 * create a strategy specific version of a DELETE request
	 *
	 * @param uri the target uri
	 * @param targetHost the target host, used for "Host" header
	 * @return the prepared request
	 */
	HttpDelete createDeleteRequest(URI uri, HttpHost targetHost);

	/**
	 * create a strategy specific version of a PUT request
	 *
	 * @param uri the target uri
	 * @param data the data payload
	 * @param depth the "Depth" header value
	 * @param targetHost the target host, used for "Host" header
	 * @return the prepared request
	 */
	HttpPut createPutRequest(URI uri, String data, int depth, HttpHost targetHost);
}
