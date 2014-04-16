	var projectsGrid;
	var sampleDataGrid;
	var propertyGrid;

	var hdfsFilePath = "";
	var projectName = "";
	var projects ;
	
	var records;
	var firstRow;
	var headerNames;
	var curPropertyName;
	var columnsProperties;
	var properties = ["Field type", "Index", "Store", "Is Repeatable", "Is PartitionKey", "Partition Position", "Cache", "Compress", "Analyze", "Analyzer", "Skip Null", "Default Value"];
	var defaultColProperties = ["String", "true", "true", "false", "false", "", "false", "false", "false", "", "true", "-1"];
	var selectedColProperty;
	var selectedSeparatorIndex;

	$(function () {
		defaultLoad();
	});
	
	function defaultLoad()
	{
		loadProjects();
		//createProject();
	}
	
	function loadProjects() {

		var xmlData = {
			service: "service",
			action: "projects",
			project: "dummy"
		};

		var projects = [];
		$.ajax({
			type: "POST",
			async: false,
			url: "process.xml",
			data: xmlData,
			cache:false,
			dataType: "text",
			beforeSend:function onBeforeSend(){$("#loader").css('display','block');},
			complete:function onComplete(){$("#loader").css('display','none');},
			success: function onSuccess(data){	
				if(null == data)
				{
					alert("Unable to get projects");
					return;
				}
				projects = data.split('\n');
			},
			error: function onError(data){
				alert("Error in getting projects data.");
			}
		});  
	}	
	
	function goToByScroll(id) {
		var pos = $(id).offset().top - 60;
		$('html,body').animate({
			scrollTop : pos
		}, 1000);
	}

	function loadSampleData() 
	{
		projectName = $('#txtProjectName').val();
		hdfsFilePath = $('#txtHdfsPath').val();
		
		if(projectName == "") {
			alert("Project name is missing. Please enter a valid java class name as project name."); 
			return;
		}
		
		if( hdfsFilePath == "")
		{
			alert("Datasource file path is missing. Please enter a valid HDFS data path. (eg. /stage/dummy.tsv)"); 
			return;
		} else {
			if( hdfsFilePath.indexOf("hdfs://") == 0 )
			{
				alert("Mention just the path. Don't prefix  protocol. "); 
				return;		
			}
		}

		if(!isAlphanumeric(projectName))
		{
			alert("Project name should be Alpha Numberic.");
			return;
		}
		
		if(contains(projects,projectName))
		{
			alert("Project already exists with this name. Please chose different name");
			return;
		}
		document.getElementById('showDataDiv').style.display = "block";
		$("input[name=delim][value=" + 1 + "]").attr('checked', 'checked');

		//process.xml?service=service&action=fetch&filepath=' + hdfsFilePath + '&project=' + projectName,
		var xmlData = {
			service: "service",
			action: "fetch",
			filepath: hdfsFilePath,
			project: projectName
		};
		
		
		$.ajax({
			type: "POST",
			async: false,
			url: "process.xml",
			data: xmlData,
			cache:false,
			dataType: "text",
			beforeSend:function onBeforeSend(){$("#loader").css('display','block');},
			complete:function onComplete(){$("#loader").css('display','none');},
			success: function onSuccess(data){
				if( null == data)
				{
					alert("Unable to fetch the sample data.");
					return;
				}
				records = data.split('\n');
				firstRow = records[0];
				goToByScroll("#showDataDiv");
			},
			error: function onError(data){
				alert(data);
			}
		});  

		selectedSeparatorIndex = parseInt(jQuery( 'input[name=delim]:checked' ).val());
		document.getElementById("otherdelim").disabled = true;
		$('#otherdelim').val("");
		var separator = getSeparator(selectedSeparatorIndex);
		var cols = firstRow.split(separator);
		recreateSampleDataGrid(cols, false, separator);
	}	

	function recreateSampleDataGrid(cols, isFirstRowHeader, separtor)
	{
		var columns = [];
		columnsProperties = [];
		var colPrefix = "column";

		headerNames = [];
		for(var colIndex in cols)
		{
			var colObj;
			var colName; 
			if(!isFirstRowHeader)
				colName = colPrefix + colIndex ;
			else
				colName = cols[colIndex];

			colName = colName.toLowerCase();
			
			colName = colName.replace(/\W/g, '');
			colName = colName.replace(/_/g, '');
			colName = colName.replace(/|/g, '');
			
			if ( colName.trim().length == 0 ) colName = "blank" + colIndex;
			var firstChar = colName.charAt(0);
			if ( firstChar == '0' || firstChar == '1' || firstChar == '2' || firstChar == '3' || firstChar == '4' || firstChar == '5' || 
                firstChar == '6' || firstChar == '7' || firstChar == '8' || firstChar == '9') colName = "d" + colName;


			headerNames[colIndex] = colName;
			colObj = { id: colName , name: colName, field: colName, width: 230}
			columns[colIndex] = colObj;

			var i = 0;
			var columnPropertyObj = [];
			columnPropertyObj['name'] = colName;
			columnPropertyObj['sourcename'] = colName;
			columnPropertyObj['sourcesequence'] = colIndex;
			columnPropertyObj['type'] = $("#txtFldType option:selected").text();
			columnPropertyObj['indexed'] =  $('#txtIndex').attr('checked') ? "true" : "false";
			columnPropertyObj['stored'] = $('#txtStore').attr('checked') ? "true" : "false";
			columnPropertyObj['repeatable'] =  $('#txtRepeatable').attr('checked') ? "true" : "false";
			columnPropertyObj['mergekey'] =  $('#txtPartitionKey').attr('checked') ? "true" : "false";
			columnPropertyObj['mergeposition'] = $('#txtPartitionKey').attr('checked') ?  $('#txtPartitionPos').val(): "";
			columnPropertyObj['cache'] =  $('#txtCache').attr('checked') ? "true" : "false";
			columnPropertyObj['compress'] =  $('#txtCompress').attr('checked') ? "true" : "false";
			columnPropertyObj['analyzed'] =  $('#txtAnalyze').attr('checked') ? "true" : "false";
			columnPropertyObj['analyzer'] =  $('#txtAnalyze').attr('checked') ? $('#txtAnalyzer').val() : "";
			if ($('#txtAnalyze').attr('checked') )
			{
				columnPropertyObj['biword'] = $('#txtBiWord').attr('checked') ? "true" : "false";
				columnPropertyObj['triword'] = $('#txtTriWord').attr('checked') ? "true" : "false";
				columnPropertyObj['repeatable'] = "true";
			}
			else
			{
				columnPropertyObj['biword'] = "false";
				columnPropertyObj['triword'] = "false";
			}
			columnPropertyObj['skipNull'] =  $('#txtSkipNull').attr('checked') ? "true" : "false";
			columnPropertyObj['defaultValue'] =  $('#txtDefaultValue').val();
			columnsProperties[colName] = columnPropertyObj;
		}
		
		var options = {
			editable: false,
			enableCellNavigation: true,
			asyncEditorLoading: false,
			autoEdit: false,
			html:true,
			forceFitColumns: false,
			rowHeight:32
		};

		var data = [];
		var skipFirstRow = isFirstRowHeader;
		var i = 0;
		for(var recordI in records)
		{
			if(skipFirstRow)
			{
				skipFirstRow = false;
				continue;
			}
			var d = (data[i++] = {});
			var recordCols = records[recordI].split(separtor);
			for (var colI in recordCols) if (columns[colI]) d[columns[colI].id] = recordCols[colI];
		}
		sampleDataGrid = new Slick.Grid("#grdSampleData", data, columns, options);

		sampleDataGrid.onHeaderClick.subscribe(function(e,args){
			var column = args.column;
			$("#propertyWindowDiv").modal();
			$('#propertyModalTitle').html("Properties - " + column.name);
			selectedColProperty = column.name;
			var selectedColProObj = columnsProperties[column.name];
			var i = 0;
			var colProperties = [];
			colProperties[i++] = selectedColProObj['type'] ;
			colProperties[i++] = selectedColProObj['indexed'];
			colProperties[i++] = selectedColProObj['stored'];
			colProperties[i++] = selectedColProObj['repeatable'];
			colProperties[i++] = selectedColProObj['mergekey'];
			colProperties[i++] = selectedColProObj['mergeposition'];
			colProperties[i++] = selectedColProObj['cache'];
			colProperties[i++] = selectedColProObj['compress'];
			colProperties[i++] = selectedColProObj['analyzed'];
			colProperties[i++] = selectedColProObj['analyzer'];
			colProperties[i++] = selectedColProObj['biword'];
			colProperties[i++] = selectedColProObj['triword'];
			colProperties[i++] = selectedColProObj['skipNull'];
			colProperties[i++] = selectedColProObj['defaultValue'];
			loadProperties(colProperties);
		});
	}

	function loadProperties(propertyValues)
	{
		var i = 0;
		$( "#txtFldType").val(propertyValues[i++]);
		(propertyValues[i++] == "false" ) ? $('#txtIndex').removeAttr('checked') : $('#txtIndex').attr('checked', 'checked');
		(propertyValues[i++] == "false" ) ? $('#txtStore').removeAttr('checked') : $('#txtStore').attr('checked', 'checked');
		(propertyValues[i++] == "false" ) ? $('#txtRepeatable').removeAttr('checked') : $('#txtRepeatable').attr('checked', 'checked');
		(propertyValues[i++] == "false" ) ? $('#txtPartitionKey').removeAttr('checked') : $('#txtPartitionKey').attr('checked', 'checked');
		$('#txtPartitionPos').val(propertyValues[i++]);
		if($('#txtPartitionKey').attr('checked'))
		{
			$('#txtPartitionPos').removeAttr('disabled');
		}
		else
		{
			$('#txtPartitionPos').attr('disabled', 'disabled');
		}
		(propertyValues[i++] == "false" ) ? $('#txtCache').removeAttr('checked') : $('#txtCache').attr('checked', 'checked');
		(propertyValues[i++] == "false" ) ? $('#txtCompress').removeAttr('checked') : $('#txtCompress').attr('checked', 'checked');
		(propertyValues[i++] == "false" ) ? $('#txtAnalyze').removeAttr('checked') : $('#txtAnalyze').attr('checked', 'checked');
		if($('#txtAnalyze').attr('checked')) 
		{
			$('#txtAnalyzer').removeAttr('disabled');
			$('#txtAnalyzer').val(propertyValues[i++]);
			$('#txtBiWord').removeAttr('disabled');
			$('#txtTriWord').removeAttr('disabled');
		}
		else
		{
			$('#txtAnalyzer').attr('disabled', 'disabled');
			$('#txtBiWord').attr('disabled', 'disabled');
			$('#txtTriWord').attr('disabled', 'disabled');
			i++;
		}
		(propertyValues[i++] == "false" ) ? $('#txtBiWord').removeAttr('checked') :	$('#txtBiWord').attr('checked', 'checked');
		(propertyValues[i++] == "false" ) ? $('#txtTriWord').removeAttr('checked') :	$('#txtTriWord').attr('checked', 'checked');
		(propertyValues[i++] == "false" ) ? $('#txtSkipNull').removeAttr('checked') : $('#txtSkipNull').attr('checked', 'checked');
		$('#txtDefaultValue').val(propertyValues[i++]);
	}
	
	function saveProperties()
	{
		if(selectedColProperty == null  || selectedColProperty === undefined || !selectedColProperty.length)
		{
			alert("Unable to save the properties");
			return;
		}
		var columnPropertyObj = columnsProperties[selectedColProperty];
		columnPropertyObj['type'] = $( "#txtFldType option:selected").text();
		columnPropertyObj['indexed'] =  $('#txtIndex').attr('checked') ? "true" : "false";
		columnPropertyObj['stored'] = $('#txtStore').attr('checked') ? "true" : "false";
		columnPropertyObj['repeatable'] =  $('#txtRepeatable').attr('checked') ? "true" : "false";
		columnPropertyObj['mergekey'] =  $('#txtPartitionKey').attr('checked') ? "true" : "false";
		if( $('#txtPartitionKey').attr('checked'))
		{
			var partitionKeyPosition = $('#txtPartitionPos').val();
			if(partitionKeyPosition == "" || !isNumber(partitionKeyPosition))
			{
				alert("Please enter the valid partition position.");
				return;
			}
			columnPropertyObj['mergeposition'] = parseInt(partitionKeyPosition);
		}
		else
		{
			columnPropertyObj['mergeposition'] = "";
		}
		
		columnPropertyObj['cache'] =  $('#txtCache').attr('checked') ? "true" : "false";
		columnPropertyObj['compress'] =  $('#txtCompress').attr('checked') ? "true" : "false";
		columnPropertyObj['analyzed'] =  $('#txtAnalyze').attr('checked') ? "true" : "false";
		columnPropertyObj['analyzer'] =  $('#txtAnalyze').attr('checked') ? $( "#txtAnalyzer option:selected").text() : "";
		columnPropertyObj['biword'] = $('#txtAnalyze').attr('checked') ?  ($('#txtBiWord').attr('checked') ? "true" : "false") : "false";
		columnPropertyObj['triword'] = $('#txtAnalyze').attr('checked') ?  ($('#txtTriWord').attr('checked') ? "true" : "false") : "false";
		columnPropertyObj['skipNull'] =  $('#txtSkipNull').attr('checked') ? "true" : "false";
		columnPropertyObj['defaultValue'] =  $('#txtDefaultValue').val();
		columnsProperties[selectedColProperty] = columnPropertyObj;
		$("#propertyWindowDiv").modal('hide');
	}

	function repaintGrid()
	{
		selectedSeparatorIndex = parseInt(jQuery( 'input[name=delim]:checked' ).val());
		var separator = "";
		if ( selectedSeparatorIndex == 5)
		{
			document.getElementById("otherdelim").disabled = false;
			separator = $('#otherdelim').val();
		} else {
			document.getElementById("otherdelim").disabled = true;
			separator = getSeparator(selectedSeparatorIndex);
			$('#otherdelim').val("");
        }
		
		if ( separator.length == 0 ) {
			return;
		}
		var cols = firstRow.split(separator);
		
		var isHeader = $('#isfirstheader').attr('checked');
		recreateSampleDataGrid(cols, isHeader, separator);
	}
	
	function repaintGridCustomDelim()
	{
		var customSeperator = $('#otherdelim').val();
		if(customSeperator == "")
		{			
			alert("Please enter custom delimiter value");
			return;
		}
		var cols = firstRow.split(customSeperator);
		var isHeader = $('#isfirstheader').attr('checked');
		recreateSampleDataGrid(cols, isHeader, customSeperator);
	}

	function buildSchemaXml() {
	    var sep;
	    if (selectedSeparatorIndex == 1)
	        sep = '\\t';
	    else
	        sep = getSeparator(selectedSeparatorIndex);


	    var isAppend = $('#incrementalindex').attr('checked') ? "true" : "false";

	    var schemaXml;
	    schemaXml = '<schema tableName="' + projectName + '" familyName="1" fieldSeparator="' + sep + '" voClass="' + projectName + '" append="' + isAppend + '">\n';
	    schemaXml = schemaXml + '	<fields>\n';
	    var isMergeKey = false;

	    var seqNo = columnsI;
	    for (var columnsI in columnsProperties) {
	        if (seqNo < parseInt(columnsI)) seqNo = parseInt(columnsI);
	        schemaXml = schemaXml + '		<field ';
	        for (var property in columnsProperties[columnsI]) {
	            schemaXml = schemaXml + ' ' + property + '="' + columnsProperties[columnsI][property] + '"';
	            if (property == "mergeposition") {
	                var propVal = columnsProperties[columnsI][property];
	                if (propVal.length > 0) {
	                    if (parseInt(propVal) >= 0) {
	                        isMergeKey = true;
	                    } else {
	                        alert("Error: Merge position has to be a positive integer, Found " + propVal);
	                        return;
	                    }
	                }
	            }
	        }
	        schemaXml = schemaXml + ' />' + '\n';
	    }

	    if (!isMergeKey) {
	        //alert("Expecting a partion key. Click on column header to set indexing properties.");
	    }

	    schemaXml = schemaXml + '	</fields>\n</schema>';
	    return schemaXml;
	}
	
	function indexData() {
	    startJob(buildSchemaXml());
	}	
	
	function indexDataWithSchemaUpload() {
		$("#uploadWindowDiv").modal();
		$("#xmlcontent").val(buildSchemaXml());
	}
	
	function startJob(schemaXml) {
		//process.xml?service=service&action=create&project=' + projectName + '&schema=' + schemaXml
		var xmlData = {
			service: "service",
			action: "create",
			project: projectName,
			schema: schemaXml
		}
		$.ajax({
			type: "POST",
			async: false,
			url: "process.xml",
			data: xmlData,
			cache:false,
			dataType: "text",
			success: function onSucess(data) {
				//'setup.xml?action=compilevo&project=' + projectName + '&schema=' + schemaXml,
				xmlData = {
					service: "service",
					action: "compilevo",
					project: projectName,
					schema: schemaXml
				}
				$.ajax({
					type: "POST",
					async: false,
					url: "setup.xml",
					data: xmlData,
					cache:false,
					dataType: "text",
					success: onCompileSuccess,
					error: onCompileError
				});
			},  
			error: function onError(data){
				alert("Error in creating project");
			}
		});  	
	}
	
	function onCompileSuccess(data)
	{
		var isHeader = $('#isfirstheader').attr('checked') ? "true" : "false";
		//'process.xml?service=service&action=index&project=' + projectName + '&header=' + isHeader + '&filepath=' + hdfsFilePath,
		var xmlData = {
			service: "service",
			action: "index",
			project: projectName,
			header: isHeader,
			filepath: hdfsFilePath
		};
		$.ajax({
			type: "POST",
			async: false,
			url: "process.xml",
			data: xmlData,
			cache:false,
			dataType: "text",
			success: onIndexStarted,
			error: onIndexError
		});  
	}

	function onCompileError(data)
	{
		alert("Error in compile VO class. " + data);
		return;
	}
	
	function onIndexStarted(data)
	{
		window.location.href = "jobStatus.html?returnUrl=" + data + "&project=" + projectName;
	}
	
	function onIndexError(data)
	{
		alert("Error in completing the data indexing");
		return;
	}
	
	//  ============ General Utils =================
	
	function isAlphanumeric(str) {
		var re = /^[0-9a-zA-Z]+$/;
		if (str.match(re)) {
			return true;
		}
		return false;
	}

	function isNumber(str) {
		var re = /^\d+$/;
		if (str.match(re)) {
			return true;
		}
		return false;
	}


	function getSeparator(index)
	{
		var separator;
		switch(index)
		  {
			case 1: separator = '\t';
				break
				
			case 2: separator = ',';
			break
			
			case 3: separator = ';';
			break
			
			case 4: separator = ' ';
			break
			
			case 5: separator = $('#otherdelim').val();
		  }
		return separator;
	}

	
	function contains(array, elem, casesensitive)
	{
		if(casesensitive){
			for(var e in array){
				if(array[e] === elem) return true;
			}
		}
		else{
			for(var e in array){
				if(array[e].toLowerCase() == elem.toLowerCase()) return true;
			}
		}
		return false;
	}
	
	function showModalProperty(){
		$("#modalProperty").modal();
	}
	
	// =============== Grid Utils ===================
	function requiredFieldValidator(value) {
		if (value == null || value == undefined || !value.length) {
		  return {valid: false, msg: "This is a required field"};
		} else {
		  return {valid: true, msg: null};
		}
	}
	
	function DynamicSelectCellEditor(args) 
	{
		return new Slick.Editors.SelectCellEditor(args);
	}

	function setFields(setType, propertyName) {
	    curPropertyName = propertyName;
	    $("#divFieldSetterHeader").html(setType);


	    var setHtml = new Array();
	    var posId = '';
	    
	    for (var headerIndex in headerNames) {
	        var checkedId = 'divFieldSetterChk' + headerIndex;
	        setHtml.push('<input type="checkbox" name="');
	        setHtml.push(checkedId);
	        setHtml.push('" id="');
	        setHtml.push(checkedId);
	        setHtml.push('" style="width:20px;"');

	        var columnPropertyObj = columnsProperties[headerNames[headerIndex]];

	        if (curPropertyName == "String" || curPropertyName == "int" || curPropertyName == "float" || curPropertyName == "double" || curPropertyName == "boolean" || curPropertyName == "short" || curPropertyName == "byte") {
	            if (columnPropertyObj["type"] == curPropertyName) setHtml.push(' checked');
	        } else if (curPropertyName == "stdAnalyzer") {
	            if (columnPropertyObj["analyzer"] == "org.apache.lucene.analysis.standard.StandardAnalyzer") setHtml.push(' checked');
	        } else if(curPropertyName == "partition"){
	        	if(columnPropertyObj["mergekey"] == "true") setHtml.push(' checked');
	        }else {
	            if (columnPropertyObj[curPropertyName] == "true") setHtml.push(' checked');
	        }

	        setHtml.push('>');
	        setHtml.push(headerNames[headerIndex]);

	        setHtml.push('</input>');
	        if(curPropertyName == "partition"){
	        	posId =  "partitionPos" + headerIndex;
	        	setHtml.push('&nbsp;&nbsp;&nbsp;<input id="')
	        	setHtml.push(posId);
	        	setHtml.push('" name="');
	        	setHtml.push(posId);
	        	setHtml.push('" style="height: 22px;width: 28px;padding: 1px;text-align: center;" type="text"/>');	
	        }
	        setHtml.push('<br />');
	    }
	    $("#divFieldSetterChecked").html(setHtml.join(""));

	    $("#divFieldSetter").modal();
	}

	function applyFieldSettings() {
	    for (var headerIndex in headerNames) {
	        var checkedId = '#divFieldSetterChk' + headerIndex;
	        var columnPropertyObj = columnsProperties[headerNames[headerIndex]];
	        if ($(checkedId).attr('checked')) {
	            if (curPropertyName == "String" || curPropertyName == "int" || curPropertyName == "float" || curPropertyName == "double" || curPropertyName == "boolean" || curPropertyName == "short" || curPropertyName == "byte") {
	                columnPropertyObj["type"] = curPropertyName;
	            } else if (curPropertyName == "stdAnalyzer") {
	                columnPropertyObj["analyzed"] = "true";
	                columnPropertyObj["biword"] = "true";
	                columnPropertyObj["triword"] = "true";
	                columnPropertyObj["repeatable"] = "true";
	                columnPropertyObj["analyzer"] = "org.apache.lucene.analysis.standard.StandardAnalyzer";
	            } else if(curPropertyName == "partition"){
	            	var posId =  "partitionPos" + headerIndex;
	            	var partitionPos = $("#" + posId).val();
                    if (parseInt(partitionPos) >= 0) {
                    	columnPropertyObj["mergekey"] = "true";
                    	columnPropertyObj["mergeposition"] = partitionPos;
                    } else {
                        alert("Error: Merge position has to be a positive integer, Found " + partitionPos);
                        return;
                    }
	            }else {
	                columnPropertyObj[curPropertyName] = "true";
	            }
	        } else {
	        	if (curPropertyName == "String" || curPropertyName == "int" || curPropertyName == "float" || curPropertyName == "double" || curPropertyName == "boolean" || curPropertyName == "short" || curPropertyName == "byte") {
	                continue;
	            }else if (curPropertyName == "stdAnalyzer") {
	                columnPropertyObj["analyzed"] = "false";
	                columnPropertyObj["biword"] = "false";
	                columnPropertyObj["triword"] = "false";
	                columnPropertyObj["analyzer"] = "";	                
	            } else if(curPropertyName == "stored") {
		                columnPropertyObj[curPropertyName] = "false";
		                columnPropertyObj["indexed"] = "false";
	            } else if(curPropertyName == "partition"){
                	columnPropertyObj["mergekey"] = "false";
                	columnPropertyObj["mergeposition"] = "";	            	
	            }else {
	                columnPropertyObj[curPropertyName] = "false";
	            }
	        }
	    }
	    $("#divFieldSetter").modal('hide');
	}

	function applyFieldAll(btnObj) {

	    var checkAll = false;
	    if (btnObj.value == "Select All") {
	        checkAll = true;
	        btnObj.value = "Deselect All";
	    }  else {
	        checkAll = false;
	        btnObj.value = "Select All";
	    }

	    for (var headerIndex in headerNames) {
	        var checkedId = '#divFieldSetterChk' + headerIndex;
	        var columnPropertyObj = columnsProperties[headerNames[headerIndex]];
	        if (checkAll) ($(checkedId).attr('checked', 'checked'));
	        else $(checkedId).removeAttr('checked');
	    }
	}
	
	function analyzerChanged(){
		var isAnalyzed = $('#txtAnalyze').attr('checked');
		if(isAnalyzed){
			$('#txtAnalyzer').removeAttr('disabled');
			$('#txtBiWord').removeAttr('disabled');
			$('#txtTriWord').removeAttr('disabled');
		} else {
			$('#txtAnalyzer').attr('disabled', 'disabled');
			$('#txtBiWord').attr('disabled', 'disabled');
			$('#txtTriWord').attr('disabled', 'disabled');
		}
	}