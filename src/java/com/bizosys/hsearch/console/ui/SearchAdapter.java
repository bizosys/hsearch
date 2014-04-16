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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryPrefixComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.RowFilter;

import com.bizosys.hsearch.byteutils.SortedBytesBitset;
import com.bizosys.hsearch.byteutils.SortedBytesBitsetCompressed;
import com.bizosys.hsearch.federate.BitSetOrSet;
import com.bizosys.hsearch.federate.BitSetWrapper;
import com.bizosys.hsearch.hbase.HBaseFacade;
import com.bizosys.hsearch.hbase.HTableWrapper;
import com.bizosys.hsearch.kv.FacetCount;
import com.bizosys.hsearch.kv.IEnricher;
import com.bizosys.hsearch.kv.KVRowI;
import com.bizosys.hsearch.kv.Searcher;
import com.bizosys.hsearch.kv.impl.FieldMapping;
import com.bizosys.hsearch.kv.impl.FieldMapping.Field;

public class SearchAdapter  {

	public static String TABLE_NAME = "unknown";
	
	public FieldMapping fm = null;
	public ConcurrentHashMap<String, KVRowI> voInstances = new ConcurrentHashMap<String, KVRowI>();

	protected final void setUp(String schemaXmlString) throws Exception {
		fm = new FieldMapping();
		fm.parseXMLString(schemaXmlString);
		TABLE_NAME = fm.tableName;
	}

	protected final void tearDown() throws Exception {
		//ShutdownCleanup.getInstance().manualSh();
	}
	
	public final Set<String> getPartitionKeys() throws Exception {
		Searcher searcher = new Searcher(fm.tableName, fm);
		return searcher.getMergeIds();
	}
	
	public final SearchResult doSearch(String mergeId, String selectFields, 
			boolean checkForAllWords, String whereFilter, 
			String sortFields, String facetFields, int offset, int pageSize) throws Exception {

		SearcherPlugin plugin = new SearcherPlugin();
		Searcher searcher = new Searcher(fm.tableName, fm);
		searcher.setPlugin(plugin);
		if( offset >= 0) searcher.setPage(offset, pageSize);
		searcher.setCheckForAllWords(checkForAllWords);

		IEnricher enricher = null;
		long start = System.currentTimeMillis();
		searcher.search(mergeId, selectFields, whereFilter, facetFields, sortFields, createVO(fm.voClass), enricher);
		long end = System.currentTimeMillis();

		SearchResult result = new SearchResult(searcher.getResult(), plugin.facetResult); 
		result.found = plugin.totalRecords;
		result.responseTime = end -start;
		return result;
	}	
	
	
	public List<String> getAllTabFields() throws Exception {
		List<String> tabs = new ArrayList<String>();
		for (String name : fm.nameWithField.keySet()) {

			if ( fm.nameWithField.get(name).isMergedKey) continue;
			
			if ( fm.nameWithField.get(name).isRepeatable) tabs.add(name);
			else if ( fm.nameWithField.get(name).isAnalyzed) tabs.add(name);
		}
		return tabs;
	}
	
	
	public Map<Object,FacetCount> listTabCounts(String mergeId, String filterQuery, String fieldName) throws Exception {
		
		try {
			Field fld = fm.nameWithField.get(fieldName);
			
			boolean process = false;
			if ( fm.nameWithField.get(fieldName).isMergedKey) process = false;
			else {
				if ( fm.nameWithField.get(fieldName).isRepeatable) process = true;
				if ( fm.nameWithField.get(fieldName).isAnalyzed) process = true;
			}
			if ( ! process ) return null;
			
			if ( fld.isAnalyzed ) {
				return listIndexedWords(mergeId, filterQuery, fieldName);
			} else {
				return getFacetFields(mergeId,filterQuery,fieldName);
			}			
		} catch (Exception e) {
			System.err.println("SearchAdapter:listTabCounts() : Error in processing schema for field " + fieldName);
			e.printStackTrace();
		}
		return null;
	}	
	
	public List<String> listDisplayFields(String mergeId) throws Exception {

		List<String> tabs = new ArrayList<String>();
		for (String name : fm.nameWithField.keySet()) {
			
			Field field = fm.nameWithField.get(name);
			if ( field.isMergedKey) continue;
			if ( field.isAnalyzed && ! field.isStored) continue;
			tabs.add(name + ":" + field.getDataType());
		}
		return tabs;
	}		
	
	private Map<Object, FacetCount> getFacetFields(String mergeId, String filterQuery, String fieldName) throws Exception {
		Searcher searcher = new Searcher(fm.tableName, fm);
		BitSetWrapper matchIds = null;
		if(null != filterQuery){
			BitSetOrSet matchIdsB = searcher.getIds(mergeId, filterQuery);
			if(null != matchIdsB)matchIds = matchIdsB.getDocumentSequences(); 
		}
		Map<String, Map<Object, FacetCount>> facetResult = searcher.createFacetCount(matchIds, mergeId, fieldName);
		return facetResult.get(fieldName);
	}
	
	private Map<Object, FacetCount> listIndexedWords(String mergeId, String filterQuery, String fieldName) throws Exception {
		
		Searcher searcher = new Searcher(fm.tableName, fm);
		BitSetOrSet foundIdsSets = ( null == filterQuery ) ? null : searcher.getIds(mergeId, filterQuery);
		BitSetWrapper foundIds = ( null == foundIdsSets ) ? null : foundIdsSets.getDocumentSequences();
		
		HBaseFacade facade = null;
		ResultScanner scanner = null;
		HTableWrapper table = null;

		try {
			facade = HBaseFacade.getInstance();
			table = facade.getTable(fm.tableName);
		
			Scan scan = new Scan();
			scan.setCacheBlocks(true);
			scan.setCaching(500);
			scan.setMaxVersions(1);

			String prefix = (mergeId + "_" + fieldName);
			int prefixT = prefix.length();
			Filter rowFilter = new RowFilter(CompareFilter.CompareOp.EQUAL,
				new BinaryPrefixComparator( prefix.getBytes()));

			scan.setFilter(rowFilter);
			scan = scan.addFamily(fm.familyName.getBytes());

			Map<Object, FacetCount> indexCount = new HashMap<Object, FacetCount>();

			Field fld = fm.nameWithField.get(fieldName);
			if ( ! (fld.isRepeatable || fld.isAnalyzed) ) {
				return indexCount;
			}
			boolean isCompressed = fld.isCompressed;
			
			scanner = table.getScanner(scan);
			
			for (Result r: scanner) {
				if ( null == r) continue;
				if ( r.isEmpty()) continue;
				
				String keyName = new String(r.getRow()).substring(prefixT);

				for (KeyValue kv : r.list()) {
					
					byte[] bits = kv.getValue();
					if ( null == bits) continue;
					int bitsT = bits.length;
					if ( 0 == bitsT ) continue;
					
					BitSetWrapper allIds = (isCompressed) ? 
							SortedBytesBitsetCompressed.getInstanceBitset().bytesToBitSet(bits, 0, bitsT)
							: SortedBytesBitset.getInstanceBitset().bytesToBitSet(bits, 0, bitsT);
					
					if ( null != foundIdsSets ) allIds.and(foundIds);
					int cardinality = allIds.cardinality();
					if ( cardinality > 0 ) indexCount.put(keyName, new FacetCount(cardinality));
				}				
			}
			return indexCount;	
			
		} finally {
			if ( null != scanner) scanner.close();
			if ( null != table ) facade.putTable(table);
		}				
	}
		
		
	public class SearchResult {
		
		public int found = 0;
		public long responseTime = 0;
		public Set<KVRowI> records = null;
		public Map<String, Map<Object, FacetCount>> facets = null;

		public SearchResult(Set<KVRowI> records,
				Map<String, Map<Object, FacetCount>> facets) {
			this.records = records;
			this.facets = facets;
		}
		
	}

	private KVRowI createVO(String voClass) throws InstantiationException,
	IllegalAccessException, ClassNotFoundException {
	
		KVRowI aBlankRow = null;
		if ( voInstances.contains(voClass) ) {
			aBlankRow = voInstances.get(voClass).create(); 
		} else {
			aBlankRow = (KVRowI) Class.forName(voClass).newInstance();
		}
		return aBlankRow;
	}
	

}