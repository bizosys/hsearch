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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.log4j.Logger;

import com.bizosys.hsearch.idsearch.admin.ColGenerator;
import com.bizosys.hsearch.kv.impl.FieldMapping;
import com.oneline.util.StringUtils;

public class SetupServlet extends HttpServlet
{

	public static boolean isSetup = false;
	
	private static final Logger LOG = Logger.getLogger(SetupServlet.class);
	private static String OS = System.getProperty("os.name").toLowerCase();	
	static Map<String, SearchAdapter> projects = new HashMap<String, SearchAdapter>();
	
	
	protected static final long serialVersionUID = 2L;
	@Override
	public void init(ServletConfig config) throws ServletException 
	{
	}
	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		doProcess(req, res);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		doProcess(req, res);
	}
	
	private void doProcess(HttpServletRequest req, HttpServletResponse res) throws IOException
	{
		/**
		 * Check if the hbase-site is there or not. If it is not there, put it. 
		 */
		String action = req.getParameter("action");
		if ( null == action) {
			Object actionO = req.getAttribute("action");
			if ( null == actionO) action = "check";
			else action = actionO.toString();
		}
		
		URL classPath = Thread.currentThread().getContextClassLoader().getResource("virgin.xml");
		String actualPath;
		try {
			actualPath = classPath.toURI().getPath();
		} catch (URISyntaxException e) {
			throw new IOException("Error in finding the classpath");
		}
		
		if ( action.equals("check")) 
		{
			check(req, res);
		} 
		else if ( action.equals("hadoopsetup")) 
		{ 
			String hbaseRootDir = req.getParameter("hbaserootdir");
			String hbaseZookeeper = req.getParameter("hbasezookeeper");
			String fsDefaultName = req.getParameter("fsdefaultname");
			String jobAddr = req.getParameter("jobTrackerAddr");
			String jobHttpAddr = req.getParameter("jobHttpAddr");
			String jobHostname = req.getParameter("jobHostName");

			String mrVersion = req.getParameter("mrVersion");
			boolean ismrv1 = mrVersion.equalsIgnoreCase("mrv1"); 

			hbaseRootDir = (null == hbaseRootDir) ? StringUtils.Empty : hbaseRootDir.trim();
			hbaseZookeeper = (null == hbaseZookeeper) ? StringUtils.Empty : hbaseZookeeper.trim();
			fsDefaultName = (null == fsDefaultName) ? StringUtils.Empty : fsDefaultName.trim();
			jobAddr = (null == jobAddr) ? StringUtils.Empty : jobAddr.trim();
			jobHttpAddr = (null == jobHttpAddr) ? StringUtils.Empty : jobHttpAddr.trim();
			jobHostname = (null == jobHostname) ? StringUtils.Empty : jobHostname.trim();

			
			String coreSiteContent = this.getFileContent("core-site-base");
			String hbaseSiteContent = this.getFileContent("hbase-site-base");

			coreSiteContent = (null == coreSiteContent) ? StringUtils.Empty : coreSiteContent.trim();
			hbaseSiteContent = (null == hbaseSiteContent) ? StringUtils.Empty : hbaseSiteContent.trim();
			coreSiteContent = coreSiteContent.replaceAll("__FSDEFAULT__", fsDefaultName);
			hbaseSiteContent = hbaseSiteContent.replaceAll("__HBASEROOTDIR__", hbaseRootDir);
			hbaseSiteContent = hbaseSiteContent.replaceAll("__HBASEZOOKEEPER__", hbaseZookeeper);

			String mapredSiteContent = null;
			String yarnSiteContent = null;
			
			if(ismrv1){
				mapredSiteContent = this.getFileContent("mr1-base");
				mapredSiteContent = (null == mapredSiteContent) ? StringUtils.Empty : mapredSiteContent.trim();
				mapredSiteContent = mapredSiteContent.replaceAll("__JOBTRACKER__", jobAddr);
				mapredSiteContent = mapredSiteContent.replaceAll("__JOBURL__", jobHttpAddr);				
			} else {
				mapredSiteContent = this.getFileContent("mr2-base");
				mapredSiteContent = (null == mapredSiteContent) ? StringUtils.Empty : mapredSiteContent.trim();
				mapredSiteContent = mapredSiteContent.replaceAll("__JOBURL__", jobHttpAddr);
				
				yarnSiteContent = this.getFileContent("mr2yarn-base");
				yarnSiteContent = (null == yarnSiteContent) ? StringUtils.Empty : yarnSiteContent.trim();
				yarnSiteContent = yarnSiteContent.replaceAll("__JOBHOSTNAME__", jobHostname);
				yarnSiteContent = yarnSiteContent.replaceAll("__JOBADDR__", jobAddr);
				yarnSiteContent = yarnSiteContent.replaceAll("__JOBURL__", jobHttpAddr);
			}
			
			
			File virginFile = new File(actualPath);
			if ( virginFile.exists()) 
			{
				File hbaseSiteFile = new File(virginFile.getParent() + "/" + "hbase-site.xml");
				File coreSiteFile = new File(virginFile.getParent() + "/" + "core-site.xml");
				
				PrintWriter writer = null;
				try
				{
					writer = new PrintWriter(hbaseSiteFile, "UTF-8");
					writer.write(hbaseSiteContent);
					
				} finally {
					if(null != writer){try{writer.flush();writer.close();}catch(Exception e){};}
				}

				try{
					writer = new PrintWriter(coreSiteFile, "UTF-8");
					writer.write(coreSiteContent);
					
				}finally {
					if(null != writer){try{writer.flush();writer.close();}catch(Exception e){};}
				}
				
				if(ismrv1){
					File mapredSiteFile = new File(virginFile.getParent() + "/" + "mapred-site.xml");
					try{	
						writer = new PrintWriter(mapredSiteFile, "UTF-8");
						writer.write(mapredSiteContent);
					}finally{
						if(null != writer){try{writer.flush();writer.close();}catch(Exception e){};}
					}					
				} else {
					File mapredSiteFile = new File(virginFile.getParent() + "/" + "mapred-site.xml");
					try{	
						writer = new PrintWriter(mapredSiteFile, "UTF-8");
						writer.write(mapredSiteContent);
					}finally{
						if(null != writer){try{writer.flush();writer.close();}catch(Exception e){};}
					}
					File yarnSiteFile = new File(virginFile.getParent() + "/" + "yarn-site.xml");
					try{	
						writer = new PrintWriter(yarnSiteFile, "UTF-8");
						writer.write(yarnSiteContent);
					}finally{
						if(null != writer){try{writer.flush();writer.close();}catch(Exception e){};}
					}
				}
			}
			else
				throw new IOException("Unable to find the classpath. Please contact administrator");
		} 
		else if ( action.equals("compilevo")) 
		{ 
			String projectName = req.getParameter("project");
			projectName = (null == projectName) ? StringUtils.Empty : projectName.trim();
			if(projectName.length() == 0) throw new IOException("Project name is missing");
			
			File virginFile = new File(actualPath);
			if(virginFile.exists())
			{
				File shemaFile = new File(virginFile.getParent() + "/" + projectName + ".xml");
				String valueObjectPath = shemaFile.getParentFile().getAbsolutePath() + "/" ;  
				String valueObjectClassName = projectName;
				FieldMapping fm = null;
				try 
				{
					fm = FieldMapping.getInstance(shemaFile.getAbsolutePath());
				} 
				catch (Exception ex) 
				{
					throw new IOException(ex);
				}
				
				ColGenerator.createVO(fm, valueObjectPath, valueObjectClassName);
				String[] compileArgs = new String[5];
				int index = 0;
				compileArgs[index++] = "-cp";
				
				String libraryPath = System.getProperty("java.class.path");	
				String libFolder = shemaFile.getParentFile().getParentFile().getAbsolutePath() + "/lib";
				File libPath = new File(libFolder);
				
				char separator = ( OS.indexOf("win") >= 0 ) ? ';' : ':' ;
				for ( String file : libPath.list()) {
					libraryPath = libraryPath + separator + libFolder + "/" + file;
				}
				
				compileArgs[index++] = libraryPath;
				
				compileArgs[index++] = "-d";
				compileArgs[index++] = valueObjectPath;
				compileArgs[index++] = valueObjectPath + valueObjectClassName + ".java";
				
				JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
				compiler.run(null, null, null, compileArgs);
			}
		}
	}
	
	public static void check(HttpServletRequest req, HttpServletResponse res) throws IOException 
	{
		URL hbaseSiteUrl = Thread.currentThread().getContextClassLoader().getResource("hbase-site.xml");
		URL coreSiteUrl = Thread.currentThread().getContextClassLoader().getResource("core-site.xml");
		if ( null == hbaseSiteUrl || null == coreSiteUrl ) {
			res.sendRedirect("setup.html");
			isSetup = false;
			return;
		}
		isSetup = true;
		res.sendRedirect("projects.html");
	}
	
	public static void create(String projectName) 
	{
		SearchAdapter productSearchProxy = new SearchAdapter();
		try 
		{
			InputStream is =  Thread.currentThread().getContextClassLoader().getResourceAsStream(projectName + ".xml");

			BufferedReader bis = new BufferedReader(new InputStreamReader(is));
			StringBuilder builder = new StringBuilder(4096);
			try 
			{
	            for(String line=bis.readLine(); line!=null; line=bis.readLine()) {
	                builder.append(line);
	                builder.append('\n');
	            }
	        } 
			catch (IOException ignore) {}
			finally 
			{
				if ( null != bis) bis.close();
			}
	        String xmlText = builder.toString(); 
			productSearchProxy.setUp(xmlText);
			
			projects.put(projectName, productSearchProxy);
			
			LOG.debug("Fld mapping total size :" + productSearchProxy.fm.nameWithField.size());
			
		} 
		catch (Exception e) 
		{
			LOG.error("Error in setting up the PrdouctSearchProxy." + e.getMessage());
			e.printStackTrace(System.err);
		}
				
	}
	
	public String getFileContent(String fileName)
	{
		String content = null;
		try 
		{
			InputStream is =  Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName + ".xml");
			StringBuilder builder = new StringBuilder(4096);
			BufferedReader bis = new BufferedReader(new InputStreamReader(is));
			try 
			{
	            for(String line=bis.readLine(); line!=null; line=bis.readLine()) {
	                builder.append(line);
	                builder.append('\n');
	            }
	            content = builder.toString();
	        } 
			catch (IOException ignore) {}
			finally 
			{
				if ( null != bis) bis.close();
			}
		} 
		catch (Exception e) 
		{
			LOG.error("Error in getting file content" + e.getMessage());
			e.printStackTrace(System.err);
		}
		 return content; 
	}
}