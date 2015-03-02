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

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.gege.caldavsyncadapter.caldav.http.HttpPropFind;
import org.gege.caldavsyncadapter.caldav.http.HttpReport;

public class DefaultDiscoveryStrategy implements DiscoveryStrategy {

	@Override
	public boolean supportsTargetHost(HttpHost targetHost) {
		return true;
	}

	@Override
	public HttpPropFind createPropFindRequest(URI uri, String data, int depth, HttpHost targetHost) {
        HttpPropFind request = new HttpPropFind();
        request.setURI(uri);
        try {
            request.setEntity(new StringEntity(data));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        }
        
        injectDefaultHeaders(request, depth, targetHost);
        return request;
	}

	@Override
	public HttpReport createReportRequest(URI uri, String data, int depth,
			HttpHost targetHost) {
        HttpReport request = new HttpReport();
        request.setURI(uri);
        try {
            request.setEntity(new StringEntity(data));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        }
        
        injectDefaultHeaders(request, depth, targetHost);
        return request;
	}
	
	private void injectDefaultHeaders(HttpUriRequest request, int depth, HttpHost targetHost) {
        
        //request.setHeader("Host", targetHost.getHostName());
        request.setHeader("Host",
                targetHost.getHostName() + ":" + String.valueOf(targetHost.getPort()));
        request.setHeader("Depth", Integer.toString(depth));
        request.setHeader("Content-Type", "application/xml;charset=\"UTF-8\"");
        //request.setHeader("Content-Type", "text/xml;charset=\"UTF-8\"");

	}

	@Override
	public HttpDelete createDeleteRequest(URI uri, HttpHost targetHost) {
        HttpDelete request = new HttpDelete();
        request.setURI(uri);
        injectDefaultHeaders(request, 0, targetHost);
        request.removeHeaders("Depth");
        return request;
	}

	@Override
	public HttpPut createPutRequest(URI uri, String data, int depth,
			HttpHost targetHost) {
        HttpPut request = new HttpPut();
        request.setURI(uri);
        injectDefaultHeaders(request, depth, targetHost);
        request.removeHeaders("Depth");
        try {
            request.setEntity(new StringEntity(data, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        }
        return request;
	}

}
