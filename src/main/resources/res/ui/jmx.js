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

/* displays a date in the user's local timezone */
function printDate(time) {
    var date = time ? new Date(time) : new Date();
    return date.toLocaleString();
}

function renderData( mbeanData )  {
	$('.statline').html(mbeanData.status); // FIXME:

	// append table view
	mbeanBody.empty();
    for ( var i in mbeanData.data ) entry( mbeanData.data[i] );
	mbeanTable.trigger('update').trigger('applyWidgets');
}


function entry( /* Object */ dataEntry ) {
    var domain = dataEntry.domain;

	/*
    var propE;
    if ( dataEntry.info ) {
    	propE = text(dataEntry.info);
    } else {
	    var bodyE = createElement('tbody');
	    for( var p in dataEntry.properties ) {
	    	bodyE.appendChild(tr(null, null, [ 
				td('propName', null, [text(p)]),
				td('propVal' , null, [text(dataEntry.properties[p])])
			]));
	    }
	    propE = createElement('table', 'propTable', null, [ bodyE ]);
    }
    
	*/

	$(tr( null, { id: 'entry' + dataEntry.domain }, [
		td( null, null, [ text( dataEntry.domain ) ] ),
		td( null, null, [  ] ),
		td( null, null, [  ] )
	])).appendTo(mbeanBody);
}

$(document).ready(function(){
	mbeanTable = $('#mbeanTable');
	mbeanBody  = mbeanTable.find('tbody');
	
	$('#reload').click(function() {
		$.get(pluginRoot + '/data.json', null, renderData, 'json');
	}).click();
});
