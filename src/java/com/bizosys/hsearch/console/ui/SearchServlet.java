/*
* Copyright 2013 Bizosys Technologies Limited
*
* Licensed to the Bizosys Technologies Limited (Bizosys) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The Bizosys licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.bizosys.hsearch.console.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bizosys.hsearch.console.ui.SearchAdapter.SearchResult;
import com.bizosys.hsearch.idsearch.util.LruCache;
import com.bizosys.hsearch.kv.FacetCount;
import com.bizosys.hsearch.kv.Searcher;
import com.bizosys.hsearch.kv.impl.FieldMapping.Field;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneline.util.FileReaderUtil;
import com.oneline.util.StringUtils;

public class SearchServlet extends HttpServlet
{

	private static final Logger LOG = Logger.getLogger(SearchServlet.class);
	private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
	private static final boolean INFO_ENABLED = LOG.isInfoEnabled();
	
	protected static final long serialVersionUID = 2L;
	
	public static final String SCHEMA_PATH = "schema_index.xml";
	public static final String EMPTY = "";
	Configuration conf = new Configuration();
	public int noOfLines = 5;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		this.doProcess(req, res);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		this.doProcess(req, res);
	}
	
	private void doProcess(HttpServletRequest req, HttpServletResponse res) throws IOException
	{
		if ( DEBUG_ENABLED ) LOG.debug("Processing the request ....");
		PrintWriter out = null;
		String webappsClassDirPath = null;

		try
		{
			res.setContentType("text/html");
			res.setCharacterEncoding (req.getCharacterEncoding() );
			
			String sensorId = req.getParameter("service");
			String action = req.getParameter("action");
			String projectName = req.getParameter("project");
			
			sensorId = (null == sensorId) ? StringUtils.Empty : sensorId.trim();
			action = (null == action) ? StringUtils.Empty : action.trim();
			projectName = (null == projectName) ? StringUtils.Empty : projectName.trim();
			
			if ( INFO_ENABLED ) LOG.info(sensorId + " : " + action + ":" + projectName);
			
			if(sensorId.length() == 0 || action.length() == 0 || projectName.length() == 0) throw new RuntimeException("SendorId/Action/Project is missing.");
			
			
			URL webappsClassDirPathUrl = Thread.currentThread().getContextClassLoader().getResource("virgin.xml");
			if ( null == webappsClassDirPathUrl) throw new  IOException("Dummy file virgin.xml missing @ webapps classpath");

			webappsClassDirPath = new File(webappsClassDirPathUrl.toURI().getPath()).getParent();
			
			String schemaFileLoc = webappsClassDirPath + "/" + projectName + ".xml";
			
			
			out = res.getWriter();
			
			if (action.equals("tabflds")) {
				out.write(this.getTabNamesJson(projectName));
				
			} else if (action.equals("tabcount")) {
				actionTabcount(req, out, projectName);
				
			} else if (action.equals("displayflds")) {
				actionDisplayFields(req, out, projectName);
				
			} else if (action.equals("search")) {
				actionSearch(req, out, projectName);
				
			} else if (action.equals("fetch")) {
				actionFetch(req, out);
				
			} else if (action.equals("create")) {
				actionCreate(req, projectName, webappsClassDirPath, schemaFileLoc);
				
			} else if (action.equals("index")) {
				out.write(actionIndex(req, projectName, webappsClassDirPath, schemaFileLoc));
				
			} else if (action.equals("projects")) {
				out.write(this.getProjects(webappsClassDirPath));
				
			}
			else if (action.equals("partitionkeys")) {
				actionParitionKeys(out, projectName);
			}
			else if (action.equals("documenturlfld")) {
				actionDocumentUrlFld(out, projectName);
			}
			else if (action.equals("refresh")) {
				actionRefresh();
			}
		}
		
		catch (URISyntaxException e) {
			String msg = "Error in finding the classpath " + webappsClassDirPath + "\t" + e.getMessage();
			LOG.fatal(msg, e);
			res.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "CONTACT_ADMIN");
		}
		catch(Exception e)
		{
			LOG.fatal("Error in completing the request." + e.getMessage(), e);
			e.printStackTrace(System.err);
			res.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "CONTACT_ADMIN");
		}
		finally 
		{
			if(null != out)
			{
				out.flush();
				out.close();
			}
		}
	}
	
	public void actionRefresh() {
		LruCache.getInstance().clear();
		if(INFO_ENABLED) LOG.info("Lru Cache is cleared.");
	}

	public void actionDocumentUrlFld(PrintWriter out, String projectName) {
		SearchAdapter productSearchProxy = getProjectProxy(projectName);
		String sourceUrlFldName = "unknown";
		for (Field fld : productSearchProxy.fm.nameWithField.values()) {
			if ( fld.isSourceUrl ) {
				sourceUrlFldName = fld.name;
				break;
			}
		}
		out.write(sourceUrlFldName);
	}

	public void actionParitionKeys(PrintWriter out, String projectName)
			throws Exception {
		SearchAdapter productSearchProxy = getProjectProxy(projectName);
		Gson gson = new GsonBuilder().setVersion(1.0).create();
		out.write(gson.toJson(productSearchProxy.getPartitionKeys()));
	}

	public String actionIndex(HttpServletRequest req, String projectName, String webappsClassDirPath,
			String schemaFileLoc) throws IOException, InterruptedException,
			ClassNotFoundException {
		
		String isHeader = req.getParameter("header");
		String hdfsFilePath = req.getParameter("filepath");
		
		hdfsFilePath = (null == hdfsFilePath) ? StringUtils.Empty : hdfsFilePath.trim();
		
		if(hdfsFilePath.length() == 0) throw new RuntimeException("HDFS Filepath is missing.");
		
		String [] args = new String[]{ hdfsFilePath,   projectName + ".xml",isHeader};
		String trackingUrl = "";
		String mapredSiteLoc = webappsClassDirPath + "/" + "mapred-site.xml";
		File file = new File(mapredSiteLoc);
		DocumentBuilder dBuilder = null;
		Document doc = null;
		try {
			dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = dBuilder.parse(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		NodeList properties = doc.getElementsByTagName("property");
		String name = null;		
		for (int i = 0; i < properties.getLength(); i++) {
			Node aNode =  properties.item(i);
			if (aNode.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) aNode;
				name = e.getElementsByTagName("name").item(0).getTextContent();
				if(name.equalsIgnoreCase("mapreduce.jobtracker.http.address"))
					trackingUrl = e.getElementsByTagName("value").item(0).getTextContent().trim();
			}
		}
		
		if(trackingUrl.indexOf("http") == -1)
			trackingUrl = "http://" + trackingUrl;
		trackingUrl += "/jobtracker.jsp#running_jobs";
		
		Indexing indexer = new Indexing(args);
		indexer.setDaemon(true);
		indexer.start();
		
		return trackingUrl;
	}

	public void actionCreate(HttpServletRequest req, String projectName,
			String webappsClassDirPath, String schemaFileLoc)
			throws FileNotFoundException, UnsupportedEncodingException,
			IOException {
		String schemaXmlContent = req.getParameter("schema");
		schemaXmlContent = (null == schemaXmlContent) ? StringUtils.Empty : schemaXmlContent.trim();
		if(schemaXmlContent.length() == 0) throw new RuntimeException("Schema content is missing.");
		
		
		File schemaFile = new File(schemaFileLoc);
		PrintWriter writer = null;
		FileWriter fw = null;
		try
		{
			writer = new PrintWriter(schemaFile, "UTF-8");
			writer.write(schemaXmlContent);
			writer.flush();
			
			SetupServlet.create(projectName);
			
			File prjectFile = new File(webappsClassDirPath + "/projects.txt");
			fw = new FileWriter(prjectFile, true);
			fw.write(projectName);
			fw.write('\n');
		}
		finally
		{
			if(null != writer) writer.close();
			
			if(null != fw)
			{
				fw.flush(); 
				fw.close();
			}
		}
			
		if (schemaFile.exists()) 
		{
			
			FileSystem fs = null;
			FSDataOutputStream hdfsFile = null;
			try {
				fs = FileSystem.get(conf);
				Path schemaHdfsFilePath = new Path(schemaFile.getName());
				
				hdfsFile = fs.create(schemaHdfsFilePath, fs.exists(schemaHdfsFilePath));
				hdfsFile.write(FileReaderUtil.getBytes(new File(schemaFile.getAbsolutePath())));
			} catch (Exception ex) {
				throw new IOException("Unable to create @ hadoop Please check permission on dfs " + schemaFile.getName(), ex);
			} finally {
				if ( null != hdfsFile) hdfsFile.close();
				if ( null != fs) fs.close();
			}
			
		}
	}

	public void actionFetch(HttpServletRequest req, PrintWriter out)
			throws IOException {
		String hdfsDataFilePath = req.getParameter("filepath");
		String lines = req.getParameter("lines");
		hdfsDataFilePath = (null == hdfsDataFilePath) ? StringUtils.Empty : hdfsDataFilePath.trim();
		int readLineCount = (null == lines) ?  noOfLines : Integer.parseInt(lines);
		if(hdfsDataFilePath.length() == 0) throw new RuntimeException("HDFS Filepath is missing.");
		out.write(this.getFileData(hdfsDataFilePath, readLineCount));
	}

	public void actionSearch(HttpServletRequest req, PrintWriter out,
			String projectName) throws IOException {
		String mergeId = req.getParameter("mergeid");
		String selectFields = req.getParameter("select");
		String whereFilter = req.getParameter("filter");
		String sortFields = req.getParameter("sort");
		String facetFields = req.getParameter("facet");
		String offset = req.getParameter("offset");
		String pageSize = req.getParameter("pagesize");
		
		mergeId = (null == mergeId) ? StringUtils.Empty : mergeId.trim();
		selectFields = (null == selectFields) ? StringUtils.Empty : selectFields.trim();
		whereFilter = (null == whereFilter) ? StringUtils.Empty : whereFilter.trim();
		sortFields = (null == sortFields) ? StringUtils.Empty : sortFields.trim();
		facetFields = (null == facetFields) ? StringUtils.Empty : facetFields.trim();
		int offsetI = (null == offset) ? 0 : Integer.parseInt(offset);
		int pageSizeI = (null == pageSize) ? 10000 : Integer.parseInt(pageSize);
		
		boolean isSearchable = ! (mergeId.length() == 0 || selectFields.length() == 0 || whereFilter.length() == 0);
		if ( isSearchable ) {
			if ( INFO_ENABLED ) LOG.info(mergeId + " = " + selectFields + " = " + whereFilter + " = " + sortFields + " = " + facetFields);
			out.write(this.doSearch(projectName, mergeId, selectFields, whereFilter, sortFields, facetFields, offsetI, pageSizeI));
		}
	}

	public void actionDisplayFields(HttpServletRequest req, PrintWriter out,
			String projectName) throws Exception {
		String mergeId = req.getParameter("mergeid");
		mergeId = (null == mergeId) ? StringUtils.Empty : mergeId.trim();
		if(mergeId.length() == 0 ) throw new RuntimeException("mergied is missing.");
		String tabFlds = this.getDisplayFields(projectName, mergeId);
		out.write(tabFlds);
	}

	public void actionTabcount(HttpServletRequest req, PrintWriter out,
			String projectName) throws Exception {
		String mergeId = req.getParameter("mergeid");
		String fieldName = req.getParameter("fld");
		String whereFilter = req.getParameter("filter");
		whereFilter = ( null == whereFilter) ? null : ( whereFilter.trim().length() == 0 ) ? null : whereFilter;
		
		mergeId = (null == mergeId) ? StringUtils.Empty : mergeId.trim();
		fieldName = (null == fieldName) ? StringUtils.Empty : fieldName.trim();
		
		if(mergeId.length() == 0 || fieldName.length() ==0) throw new RuntimeException("mergied/fld are missing.");
		out.write(this.getTabCountJson(projectName, mergeId, whereFilter, fieldName));
	}
	
	
	public String getProjects(String webappsClassDirPath) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		File prjectFile = new File(webappsClassDirPath + "/projects.txt");
		List<String> projects = FileReaderUtil.toLines(prjectFile.getAbsolutePath());
		boolean first = true;
		for (String project : projects) {
			if(first) first = false;
			else
				sb.append('\n');
			
			sb.append(project);
		}
		return sb.toString();
			
	}
	
	private String getFileData(String path, int readLineCount) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		FileSystem fs = null;
		int lineNo = 1;
		try 
		{
			Path hadoopPath = new Path(path);
			fs = FileSystem.get(conf);
			if (fs.exists(hadoopPath)) 
			{
				br = new BufferedReader(new InputStreamReader(fs.open(hadoopPath)));
				String line = null;
				boolean first = true;
				while ((line = br.readLine()) != null) 
				{
					if(lineNo > readLineCount) break;
					if(first) first = false;
					else sb.append('\n');
					sb.append(line);
					lineNo++;
				}
			} 
		}
		catch (FileNotFoundException fex) 
		{
			System.err.println("Cannot read from path " + path);
			throw new IOException(fex);
		} 
		catch (Exception pex) {
			System.err.println("Error : " + path);
			throw new IOException(pex);
		}
		finally
		{
			if(null != br) try{ br.close();}catch(Exception e){}
			if(null != fs) try{ fs.close();}catch(Exception e){}
		}
		return sb.toString();
	}
	
	private String getFileData(String path) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		FileSystem fs = null;
		try 
		{
			Path hadoopPath = new Path(path);
			fs = FileSystem.get(conf);
			if (fs.exists(hadoopPath)) 
			{
				br = new BufferedReader(new InputStreamReader(fs.open(hadoopPath)));
				String line = null;
				boolean first = true;
				while ((line = br.readLine()) != null) 
				{
					if(first) first = false;
					else sb.append('\n');
					sb.append(line);
				}
			} 
		}
		catch (FileNotFoundException fex) 
		{
			System.err.println("Cannot read from path " + path);
			throw new IOException(fex);
		} 
		catch (Exception pex) {
			System.err.println("Error : " + path);
			throw new IOException(pex);
		}
		finally
		{
			if(null != br) try{ br.close();}catch(Exception e){}
			if(null != fs) try{ fs.close();}catch(Exception e){}
		}
		return sb.toString();
	}
	
	private String doSearch(String projectName, String mergeId,  String selectFields, String whereFilter, 
		String sortFields, String facetFields, int offset, int pageSize) throws IOException 
	{
		StringBuilder results = new StringBuilder(62238);
		try
		{
			SearchAdapter productSearchProxy = getProjectProxy(projectName);
			SearchResult  found = productSearchProxy.doSearch(mergeId, selectFields, true, whereFilter, sortFields, facetFields,  offset,  pageSize);
			
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setVersion(1.0).create();
			results.append("{\"offset\":").append(offset).append(",\"pagesize\":").append(pageSize);
			results.append(",\"count\":").append(found.found);
			results.append(",\"records\":");
			results.append(gson.toJson(found.records));
			results.append("}");
			return results.toString();
		}
		catch(Exception e)
		{
			LOG.error("Error in fetching search results. Please contact administrator " + e.getMessage());
			e.printStackTrace(System.err);
			throw new IOException("Project:" + projectName + "\tPartitionKey:" + mergeId+ "\tSelect:" + selectFields+ "\tWhere:" + whereFilter+ "\tSort:" + sortFields+ "\tFacet:" + 
					facetFields+ "\tOffset" + offset+ "\tLimit" + pageSize, e);
		}
	}

	private SearchAdapter getProjectProxy(String projectName) {
		SearchAdapter productSearchProxy = SetupServlet.projects.get(projectName);
		if ( null == productSearchProxy) {
			SetupServlet.create(projectName);
			productSearchProxy = SetupServlet.projects.get(projectName);
		}
		return productSearchProxy;
	}
	

	public String getTabNamesJson(String projectName) throws Exception
	{
		SearchAdapter productSearchProxy = getProjectProxy(projectName);
		List<Tab> tabNames = new ArrayList<Tab>();
		List<String> fieldNameList = productSearchProxy.getAllTabFields();
		for(String fieldName:fieldNameList)
			tabNames.add(new Tab(fieldName, ""));

		Gson gson = new GsonBuilder().setVersion(1.0).create();
		return gson.toJson(tabNames);
	}

	public String getTabCountJson(String projectName, String mergeId, String whereFilter, String fieldName) throws Exception
	{
		SearchAdapter productSearchProxy = getProjectProxy(projectName);
		List<TabDetail> tabDetailL = new ArrayList<TabDetail>();
		Map<Object, FacetCount> tabCountMap = productSearchProxy.listTabCounts(mergeId, whereFilter, fieldName);
		if(null == tabCountMap) return EMPTY; 
		for(Object key: tabCountMap.keySet()) {
			String tabName = key.toString();
			boolean isBlank = ( null == tabName) ? true : ( 0 == tabName.trim().length());
			if ( isBlank ) continue;
			tabDetailL.add(new TabDetail(key.toString(), tabCountMap.get(key).count));
		}
		
		Gson gson = new GsonBuilder().setVersion(1.0).create();
		return gson.toJson(tabDetailL);
	}

	public String getDisplayFields(String projectName, String mergeId) throws Exception
	{
		SearchAdapter productSearchProxy = getProjectProxy(projectName);
		List<Tab> displayFieldList = new ArrayList<Tab>();
		String [] nameAndType = null;
		for(String name: productSearchProxy.listDisplayFields(mergeId))
		{
			nameAndType = name.split(":");
			displayFieldList.add(new Tab(nameAndType[0], nameAndType[1]));
		}
		Gson gson = new GsonBuilder().setVersion(1.0).create();
		return gson.toJson(displayFieldList);
	}
	

	/**
	 * testing method 
	 * @param mergeId
	 * @throws Exception
	 */
	private void testSetup(String projectName, String mergeId) throws Exception
	{
		SearchAdapter productSearchProxy = getProjectProxy(projectName);
		productSearchProxy = new SearchAdapter();
		productSearchProxy.setUp("conf/results_index.xml");
		//SearchResult result = this.productSearchProxy.doSearch(mergeId, "productid", true, "colour:green", "category", "category");
		//System.out.println(this.doSearch(mergeId,  "domain", "title:bizosys", "", ""));
		
		System.out.println(this.getTabCountJson(projectName, mergeId, null, "title"));
		
		Searcher searcher = new Searcher("test", productSearchProxy.fm);
		//BitSetOrSet sets = searcher.getIds(mergeId, "description:bizosys");
		//System.out.println(sets.getDocumentSequences());
		//System.out.println(this.getDisplayFields(mergeId));
	}
	
	public static void main(String[] args) throws Exception {
		
		SearchAdapter productSearchProxy = new SearchServlet().getProjectProxy("app");
		System.out.println(productSearchProxy.listTabCounts("Bizosys", "Column1:1", "Column1").toString());
		
	}
}