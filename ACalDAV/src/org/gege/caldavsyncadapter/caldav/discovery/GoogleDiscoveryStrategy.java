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
import org.apache.http.client.methods.HttpUriRequest;
import org.gege.caldavsyncadapter.caldav.http.HttpPropFind;
import org.gege.caldavsyncadapter.caldav.http.HttpReport;

public class GoogleDiscoveryStrategy extends DefaultDiscoveryStrategy
{

	@Override
	public boolean supportsTargetHost(HttpHost targetHost)
	{
		if (targetHost != null && targetHost.getHostName() != null)
		{
			return targetHost.getHostName().contains("google.com");
		}
		return false;
	}

	@Override
	public HttpPropFind createPropFindRequest(URI uri, String data, int depth, HttpHost targetHost)
	{
		HttpPropFind result = super.createPropFindRequest(uri, data, depth, targetHost);
		filterHostSpecifics(targetHost, result);
		return result;
	}

	@Override
	public HttpReport createReportRequest(URI uri, String data, int depth, HttpHost targetHost)
	{
		HttpReport result = super.createReportRequest(uri, data, depth, targetHost);
		filterHostSpecifics(targetHost, result);
		return result;
	}

	@Override
	public HttpDelete createDeleteRequest(URI uri, HttpHost targetHost)
	{
		HttpDelete result = super.createDeleteRequest(uri, targetHost);
		filterHostSpecifics(targetHost, result);
		return result;
	}

	@Override
	public HttpPut createPutRequest(URI uri, String data, int depth, HttpHost targetHost)
	{
		HttpPut result = super.createPutRequest(uri, data, depth, targetHost);
		filterHostSpecifics(targetHost, result);
		return result;
	}

	private HttpUriRequest filterHostSpecifics(HttpHost targetHost, HttpUriRequest result)
	{
		result.setHeader("Host", targetHost.getHostName());
		return result;
	}
}
