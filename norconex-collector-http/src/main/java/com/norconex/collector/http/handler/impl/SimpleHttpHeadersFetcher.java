/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex HTTP Collector.
 * 
 * Norconex HTTP Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex HTTP Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex HTTP Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.http.handler.impl;

import java.io.Reader;
import java.io.Writer;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.http.HttpCollectorException;
import com.norconex.collector.http.handler.IHttpHeadersFetcher;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.map.Properties;

/**
 * Basic implementation of {@link IHttpHeadersFetcher}.  
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;httpHeadersFetcher 
 *      class="com.norconex.collector.http.handler.impl.SimpleHttpHeadersFetcher" &gt;
 *      &lt;validStatusCodes&gt;200&lt;/validStatusCodes&gt;
 *      &lt;headersPrefix&gt;(string to prefix headers)&lt;/headersPrefix&gt;
 *  &lt;/httpHeadersFetcher&gt;
 * </pre>
 * <p>
 * The "validStatusCodes" attribute expects a coma-separated list of HTTP
 * response code.
 * </p>
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
public class SimpleHttpHeadersFetcher 
        implements IHttpHeadersFetcher, IXMLConfigurable {

	private static final long serialVersionUID = 6526443843689019304L;
    private static final Logger LOG = LogManager.getLogger(
			SimpleHttpHeadersFetcher.class);
    public static final int[] DEFAULT_VALID_STATUS_CODES = new int[] {
        HttpStatus.SC_OK,
    };
	
    private int[] validStatusCodes;
    private String headersPrefix;

    public SimpleHttpHeadersFetcher() {
        this(DEFAULT_VALID_STATUS_CODES);
    }
    public SimpleHttpHeadersFetcher(int[] validStatusCodes) {
        super();
        this.validStatusCodes = validStatusCodes;
    }
	public int[] getValidStatusCodes() {
        return validStatusCodes;
    }
    public void setValidStatusCodes(int[] validStatusCodes) {
        this.validStatusCodes = validStatusCodes;
    }
	public String getHeadersPrefix() {
        return headersPrefix;
    }
    public void setHeadersPrefix(String headersPrefix) {
        this.headersPrefix = headersPrefix;
    }
    @Override
	public Properties fetchHTTPHeaders(HttpClient httpClient, String url) {
	    Properties metadata = new Properties();
	    HttpMethod method = null;
	    try {
	        method = new HeadMethod(url);
	        // Execute the method.
	        int statusCode = httpClient.executeMethod(method);
	        if (!ArrayUtils.contains(validStatusCodes, statusCode)) {
	            if (LOG.isDebugEnabled()) {
	                LOG.debug("Invalid HTTP status code ("
	                        + method.getStatusLine() + ") for URL: " + url);
	            }
	            return null;
	        }
	        
	        Header[] headers = method.getResponseHeaders();
	        for (int i = 0; i < headers.length; i++) {
	            Header header = headers[i];
	            String name = header.getName();
	            if (StringUtils.isNotBlank(headersPrefix)) {
	            	name = headersPrefix + name;
	            }
	            
	            metadata.addString(name, header.getValue());
//	            enhanceHTTPHeaders(metadata, header);
	        }
	        return metadata;
        } catch (Exception e) {
        	LOG.error("Cannot fetch document: " + url
        	        + " (" + e.getMessage() + ")", e);
        	throw new HttpCollectorException(e);
        } finally {
	        if (method != null) {
	            method.releaseConnection();
	        }
        }  
	}
	

//    private void enhanceHTTPHeaders(Metadata metadata, Header header) {
//        if ("Content-Type".equals(header.getName())) { //TODO appropriate??
//            String contentType = header.getValue();
//            String mimeType = contentType.replaceFirst("(.*?)(;.*)", "$1");
//            String charset = contentType.replaceFirst("(.*?)(; )(.*)", "$3");
//            charset = charset.replaceFirst("(charset=)(.*)", "$2");
//            metadata.addValue(
//                    HttpMetadata.DOC_MIMETYPE, mimeType);
//            metadata.addValue(
//                    HttpMetadata.DOC_CHARSET, charset);
//        }
//    }
    
    @Override
    public void loadFromXML(Reader in) {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
        String validCodes = xml.getString("validStatusCodes");
        int[] intCodes = DEFAULT_VALID_STATUS_CODES;
        if (StringUtils.isNotBlank(validCodes)) {
            String[] strCodes = validCodes.split(",");
            intCodes = new int[strCodes.length];
            for (int i = 0; i < strCodes.length; i++) {
                String code = strCodes[i];
                intCodes[i] = Integer.parseInt(code);
            }
        }
        setValidStatusCodes(intCodes);
        setHeadersPrefix(xml.getString("headersPrefix"));
    }
    @Override
    public void saveToXML(Writer out) {
        // TODO Implement me.
        System.err.println("saveToXML not implemented");
    }
}