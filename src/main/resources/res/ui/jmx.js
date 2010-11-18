/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var mbeanTable = false;

function renderData( eventData, filter )  {
	$('.statline').html(eventData.status); // FIXME:

	// append table view
	mbeanBody.empty();
	
	domainDetailsRow = mbeansTemplate.clone();
	var td = domainDetailsRow.find('td:eq(0)');
	var domainTree = td.find('ul');
    for ( var idx in eventData.data ) {
    	if ( currentDomain == null || !drawDetails || currentDomain == eventData.data[idx].domain) {
    		eventData.selected = idx;
            entry( eventData, filter, domainTree );
        }
    }

    domainDetailsRow.appendTo(mbeanBody);
	
	$("#domainTree").treeview({
		animated: "fast",
		collapsed: true,
		unique: true
	});
    
    initStaticWidgets();
    
    mbeansTemplate.trigger('update').trigger('applyWidgets');
}


function entry( /* Object */ eventData, filter, domainTree ) {
	var matches = !(filter && typeof filter.test == 'function') ? true : filter.test(dataEntry.domain);
	
	if (matches) entryInternal( eventData, domainTree );
	
}


function entryInternal( /* Object */ eventData, domainTree ) {
	
	var mbean = eventData.data[eventData.selected];
	
    var domain = mbean.domain;
	
	$("<li><span class='folder'><strong>"+domain+"</strong></span><ul>" + 
		drawMBeans(eventData) +
 		"</ul></li>").appendTo(domainTree);

}

function drawMBeans(eventData) {
	
	var mbeans = eventData.data[eventData.selected].mbeans;
	
	var mbeansList = "";
	
	var subStructure = {};
	
	if (mbeans.length > 0) {
		//first build up the sub tree structure
		$.each(mbeans, function(index, bean){
			var mbean = bean.mbean;
			var subBeans = mbean.split(",");			
			if (subStructure[subBeans[0]] == undefined){
				subStructure[subBeans[0]] = []
			} 
			subStructure[subBeans[0]].push(subBeans[1]);

		});
		
		var count = 0;
		
		// now build the tree structure with attributes and operations
		$.each( subStructure, function(name, value){
			var beans = name.split("=");
			mbeansList += "<li><span class='"+beans[0]+"'><strong>"+beans[1]+"</strong></span><ul>";
			$.each( value, function(index, val){
				eventData.data[eventData.selected].count = count;
				if (val != undefined ) {
					var valBeans = val.split("=");
					mbeansList += "<li><span class='"+valBeans[0]+"'><strong>"+valBeans[1]+"</strong></span><ul>";
					mbeansList += drawAttributes(eventData);
				 	mbeansList += drawOperations(eventData);
				 	mbeansList += "</ul></li>";
				} else {
					mbeansList += drawAttributes(eventData);
					mbeansList += drawOperations(eventData);
				}
				count++;
			});
			mbeansList += "</ul></li>";
		});


	}
	return mbeansList;
}

function drawAttributes(eventData) {
	
	var mbeanCount = eventData.data[eventData.selected].count;
	var attributes = eventData.data[eventData.selected].mbeans[mbeanCount].attributes;
	
	var domain = eventData.data[eventData.selected].domain;
	var mbean = eventData.data[eventData.selected].mbeans[mbeanCount].mbean
	
	var attributeList = "";
	
	if (attributes.length > 0) {
		attributeList = "<li><span class='attribute'><strong>Attributes</strong></span><ul>";
		for ( var idx in attributes ) {
			var attributeProps = attributes[idx].split(":");
			var isWritable = String(attributeProps[1].split("=")[1]);
			if ("true"==isWritable)
				attributeList += "<li><span class='file'><a type='attribute' href='javascript:showAttributeDetail(\""+domain+"\",\""+mbean+"\",\""+attributeProps[0]+"\");'>"+attributeProps[0]+"</a></span></li>";
			else
				attributeList += "<li><span class='file-readonly'><a type='attribute' href='javascript:showAttributeDetail(\""+domain+"\",\""+mbean+"\",\""+attributeProps[0]+"\");'>"+attributeProps[0]+"</a></span></li>";
		}
		attributeList += "</ul></li>";
	}
	
	return attributeList;
}

function drawOperations(eventData) {
	
	var mbeanCount = eventData.data[eventData.selected].count;
	var operations = eventData.data[eventData.selected].mbeans[mbeanCount].operations;

	var domain = eventData.data[eventData.selected].domain;
	var mbean = eventData.data[eventData.selected].mbeans[mbeanCount].mbean

	var operationsList = "";
	
	if (operations.length > 0) {
		operationsList = "<li><span class='operations'><strong>Operations</strong></span><ul>";
		for (var idx in operations) {
			operationsList += "<li><span class='operation'><a type='attribute' href='javascript:showOperationDetail(\""+domain+"\",\""+mbean+"\",\""+operations[idx]+"\"'>"+operations[idx]+"</a></span></li>";
		}
		operationsList += "</ul></li>";
	}	
	return operationsList;
}

function loadData() {
    $.get(pluginRoot + "/.json", null, renderData, "json"); 
}

function showAttributeDetail(domain, mbean, attribute) {
	$.get(pluginRoot+"/"+domain+".json?mbean="+mbean+"&attribute="+attribute, null, renderAttributeDetail, "json"); 
}

function renderAttributeDetail(attributeDetail) {
	var detail = $('#detail');
	
	var outdetail = "<li><span >"+attributeDetail.attributeName+": "+attributeDetail.attributeValue+"</span></li>";
	
	$(outdetail).appendTo(detail);
}

$(document).ready(function(){
	$('#reload').click(function() {
		$.get(pluginRoot + '/.json', null, renderData, 'json');
	}).click();
	
	
	mbeanOpError = $('#mbeanOpError').dialog({
		autoOpen: false,
		modal   : true,
		width   : '80%'
	});
	mbeanOpError.parent().addClass('ui-state-error');
	
	
	// filter
	$('.filterApply').click(function() {
		if ($(this).hasClass('ui-state-disabled')) return;
		var el = $(this).parent().find('input.filter');
		var filter = el.length && el.val() ? new RegExp(el.val()) : false;
		renderData(lastMBeanData, filter);
	});
	$('.filterForm').submit(function() {
		$(this).find('.filterApply').click();
		return false;
	});
	$('.filterClear').click(function() {
		if ($(this).hasClass('ui-state-disabled')) return;
		$('input.filter').val('');
		loadData();
	});
	$('.filterLDAP').click(function() {
		if ($(this).hasClass('ui-state-disabled')) return;
		var el = $(this).parent().find('input.filter');
		var filter = el.val();
		if (filter) $.get(pluginRoot + '/.json', { 'filter' : filter }, renderData, 'json');
		return false;
	});
	
	// check for cookie
	mbeanTable = $('#mbeanTable').tablesorter({
		headers: {
			0: { sorter:"digit" },
			5: { sorter: false }
		},
		textExtraction:mixedLinksExtraction
	}).bind("sortEnd", function() {
		var t = mbeanTable.eq(0).attr("config");
		if (t.sortList) $.cookies.set("webconsolembeanlist", t.sortList);
	});
	
	mbeanBody  = mbeanTable.find('tbody');
	mbeansTemplate = mbeanBody.find('tr').clone();
	
	$('#clear').click(function(){
		$('#detail').empty();
	}).click();

	
	renderData(lastMBeanData);
});
	
	
