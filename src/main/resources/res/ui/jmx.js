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
    for ( var idx in eventData.data ) {
    	if ( currentDomain == null || !drawDetails || currentDomain == eventData.data[idx].domain) {
            entry( eventData.data[idx], filter );
        }
    }
    
    if ( drawDetails && eventData.data.length == 1 ) {
		$('.filterBox input, .filterBox button').addClass('ui-state-disabled');
        renderDetails(eventData.data[0]);    
    } else if ( currentDomain != null ) {
        var id = currentDomain;
        hideDetails(id);
        showDetails(id);
    }
    initStaticWidgets();
    
	mbeanTable.trigger('update').trigger('applyWidgets');
}


function entry( /* Object */ mbean, filter ) {
	var matches = !(filter && typeof filter.test == 'function') ? true : filter.test(dataEntry.domain);

	if (matches) entryInternal( mbean ) //.appendTo(mbeanBody);
}

function entryInternal( /* Object */ mbean ) {
    var domain = mbean.domain;

	/*
	var tr = mbeansTemplate.clone();

	tr.attr('id', 'entry'+domain);
	tr.find('td:eq(0)').text(domain);
	tr.find('.bIcon').attr('id', 'img'+domain).click(function() {showDetails(domain)});
	tr.find('.bName').html( drawDetails ? name : '<a href="' + pluginRoot + '/' + domain + '">' + domain + '</a>' );
	return tr;
	*/
	
	var branches = $("<li><span class='folder'>"+domain+"</span><ul class='treeview'>" + 
 		drawAttributes(mbean.attributes) +
 		drawOperations(mbean.operations) +
 		"</ul></li>").appendTo(mbeanBody);
 		
 	$(mbeanBody).treeview({
 		collapsed: true,
 		add: branches
 	});
	
}

function drawAttributes(attributes) {
	var attributeList = "";
	
	if (attributes.length > 0) {
		attributeList = "<li class='expandable'><div class='hitarea expandable-hitarea'></div><span><strong>Attributes</strong></span><ul>";
		for ( var idx in attributes ) {
			attributeList += "<li><span class='file'>"+attributes[idx]+"</span></li>";
		}
		attributeList += "</ul></li>"
	}
	
	return attributeList;
}

function drawOperations(operations) {
	var operationsList = "";
	
	if (operations.length > 0) {
		operationsList = "<li class='expandable'><div class='hitarea expandable-hitarea'></div><span><strong>Operations</strong></span><ul>";
		for (var idx in operations) {
			operationsList += "<li><span class='file'>"+operations[idx]+"</span></li>";
		}
		operationsList += "</ul></li>";
	}	
	return operationsList;
}

function showDetails( domain ) {
    currentDomain = domain;
    $.get(pluginRoot + "/" + domain + ".json", null, function(data) {
        renderDetails(data.data[0]);
    }, "json");
}

function hideDetails( domain ) {
    currentDomain = null;
    $("#img" + domain).each(function() {
        $("#mbeanInlineDetails" + domain).remove();
        $(this).
            removeClass('ui-icon-triangle-1-w').//left
            removeClass('ui-icon-triangle-1-s').//down
            addClass('ui-icon-triangle-1-e').//right
            attr("title", "Details").
            unbind('click').click(function() {showDetails(id)});
    });
}

function loadData() {
    $.get(pluginRoot + "/.json", null, renderData, "json"); 
}

function renderDetails( data ) {
    $("#entry" + data.domain + " > td").eq(1).append("<div id='mbeanInlineDetails"  + data.domain + "'/>");
    $("#img" + data.domain).each(function() {
        if ( drawDetails ) {
            var ref = window.location.pathname;
            ref = ref.substring(0, ref.lastIndexOf('/'));
            $(this).
                removeClass('ui-icon-triangle-1-e').//right
                removeClass('ui-icon-triangle-1-s').//down
                addClass('ui-icon-triangle-1-w').//left
                attr("title", "Back").
                unbind('click').click(function() {window.location = ref});
        } else {
            $(this).
                removeClass('ui-icon-triangle-1-w').//left
                removeClass('ui-icon-triangle-1-e').//right
                addClass('ui-icon-triangle-1-s').//down
                attr("title", "Hide Details").
                unbind('click').click(function() {hideDetails(data.id)});
        }
    });
    $("#mbeanInlineDetails" + data.domain).append("<table border='0'><tbody></tbody></table>");
    var attributes = data.attributes;
    for (var idx in attributes) {
        var prop = attributes[idx];
		//var key = i18n[prop.key] ? i18n[prop.key] : prop.key;
		
        var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + prop + "</td><td class='aligntop' style='border:0px none'>";
        /*          
        var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + key + "</td><td class='aligntop' style='border:0px none'>";
        if (prop.value) {
            if ( prop.key == 'Bundle Documentation' )  {
                txt = txt + "<a href='" + prop.value + "' target='_blank'>" + prop.value + "</a>";
            } else  {
                if ( $.isArray(prop.value) ) {
                    var i = 0;
                    for(var pi in prop.value) {
                        var value = prop.value[pi];
                        if (i > 0) { txt = txt + "<br/>"; }
                        txt = txt + value;
                        i++;
                    }
                } else {
                    txt = txt + prop.value;
                }
            }
        } else {
        */
            txt = txt + "\u00a0";
        //}
        txt = txt + "</td></tr>";
    	$("#mbeanInlineDetails" + data.domain + " > table > tbody").append(txt);
    }
    var oerations = data.oerations;
    for (var idx in operations) {
        var prop = operations[idx];
		//var key = i18n[prop.key] ? i18n[prop.key] : prop.key;

		var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + prop + "</td><td class='aligntop' style='border:0px none'>";
		/*
        var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + key + "</td><td class='aligntop' style='border:0px none'>";          
        if (prop.value) {
            if ( prop.key == 'Bundle Documentation' )  {
                txt = txt + "<a href='" + prop.value + "' target='_blank'>" + prop.value + "</a>";
            } else  {
                if ( $.isArray(prop.value) ) {
                    var i = 0;
                    for(var pi in prop.value) {
                        var value = prop.value[pi];
                        if (i > 0) { txt = txt + "<br/>"; }
                        txt = txt + value;
                        i++;
                    }
                } else {
                    txt = txt + prop.value;
                }
            }
        } else {
        */
            txt = txt + "\u00a0";
        //}
        txt = txt + "</td></tr>";
    	$("#mbeanInlineDetails" + data.domain + " > table > tbody").append(txt);
    }
}


$(document).ready(function(){
	//$('.reloadButton').click(loadData);
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
	
	$(mbeanBody).treeview();
	
	
	renderData(lastMBeanData);	
});

