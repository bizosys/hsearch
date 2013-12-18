package com.bizosys.hsearch.console.ui;

import com.bizosys.hsearch.kv.KVIndexer;

public class Indexing extends Thread{
	
	String[] params = null;
	KVIndexer indexer = null;
	
	public Indexing(String[] params){
		this.params = params;
		indexer = new KVIndexer();
	}
	
	@Override
	public void run() {
		try {
			indexer.execute(params);
		} catch (Exception e) {
			System.err.println(("Could not complete the job."));
		}
	}

	public static void main(String[] args) {
	}

}
