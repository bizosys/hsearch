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

import java.util.Map;
import java.util.Set;

import com.bizosys.hsearch.federate.BitSetWrapper;
import com.bizosys.hsearch.federate.QueryPart;
import com.bizosys.hsearch.kv.FacetCount;
import com.bizosys.hsearch.kv.ISearcherPlugin;
import com.bizosys.hsearch.kv.KVRowI;

public class SearcherPlugin implements ISearcherPlugin {

	public Map<String, Map<Object, FacetCount>> facetResult = null;
	BitSetWrapper foundIds = null;
	public int totalRecords = 0;

	@Override
	public void onJoin(String mergeId, BitSetWrapper foundIds,
			Map<String, QueryPart> whereParts,
			Map<Integer, KVRowI> foundIdWithValueObjects) {
		
		this.foundIds = foundIds; 
		totalRecords = this.foundIds.cardinality();
	}

	@Override
	public void onFacets(String mergeId,
			Map<String, Map<Object, FacetCount>> facets) {
		
		this.facetResult = facets;

	}

	@Override
	public void beforeSelect(String mergeId, BitSetWrapper foundIds) {

	}

	@Override
	public void afterSelect(String mergeId, BitSetWrapper foundIds) {
		

	}

	@Override
	public void afterSelectOnSorted(String arg0, BitSetWrapper arg1) {
		
	}

	@Override
	public void beforeSelectOnSorted(String arg0, BitSetWrapper arg1) {
		
	}

	@Override
	public void afterSort(String arg0, Set<KVRowI> arg1) {
	}

	@Override
	public void beforeSort(String arg0, Set<KVRowI> arg1) {
		
	}

}
