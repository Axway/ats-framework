'use strict';

function onload() {
	var entry = document.getElementsByClassName('sidebar-entry-level-0-text-collapsed')[0];
	var parent = entry.parentNode;
	var parentLevel = parseInt( parent.getAttribute('level'));
	toggleChildren(parent, entry, parentLevel);
	loadPage(parent);
}

function isNotNullOrUndefined(element) {
	return element != null && element != undefined;
}

function showEntry(event,entry) {
	var divParent = entry.parentNode;
	if (entry.getAttribute('class').includes('-none')){
		loadPage(divParent);
		return;
	}
	var divParentLevel = parseInt( divParent.getAttribute('level'));
	if (event.clientX <= 15 + ( 10 * divParentLevel ) ) {  // if the ::before pseudo-element is clicked
		toggleChildren(divParent, entry, divParentLevel); 
	} else {
		loadPage(divParent); // if the actual p element is clicked
	}
}

function toggleChildren(divParent, entry, level) {
	if (divParent.getAttribute('state') == 'collapsed') {
		// we must expand only first order children
		divParent.setAttribute('state','expanded');
		entry.setAttribute('class',`sidebar-entry-level-${level}-text-${divParent.getAttribute('state')}`);
		showChildren(divParent, level+1);
	} else if(divParent.getAttribute('state') == 'expanded'){
		// we must hide, not collapse, all level of the children
		divParent.setAttribute('state','collapsed');
		entry.setAttribute('class',`sidebar-entry-level-${level}-text-${divParent.getAttribute('state')}`);
		hideAllChildren(divParent);
	} else {
		// no nothing
	}
}

function showChildren(divParent, level) {
	for (var child of divParent.children) {
		if (child.tagName == 'DIV' && parseInt(child.getAttribute('level')) == level) {
			var childId = child.getAttribute('id');
			var childTextEl = document.getElementById(childId+'-text');
			childTextEl.style.display = 'block';
		}
	}
}

function hideAllChildren(divParent) {
	for (var child of divParent.children) {
		if (child.tagName == 'DIV') {
			if(child.getAttribute('state') !== 'none') {
				child.setAttribute('state','collapsed');
			}
			var childId = child.getAttribute('id');
			var childTextEl = document.getElementById(childId+'-text');
			childTextEl.setAttribute('class',`sidebar-entry-level-${parseInt(child.getAttribute('level'))}-text-${child.getAttribute('state')}`);
			childTextEl.style.display = 'none';
			hideAllChildren(child);
		}
	}
}

function _toggleChildren(divParent, entry) {
	var level = divParent.getAttribute('level');
	if (divParent.getAttribute('state') == 'collapsed') {
		divParent.setAttribute('state','expanded');
		entry.setAttribute('class',`sidebar-entry-level-${level}-text-${divParent.getAttribute('state')}`);
	} else {
		divParent.setAttribute('state','collapsed');
		entry.setAttribute('class',`sidebar-entry-level-${level}-text-${divParent.getAttribute('state')}`);
	}
	var children = divParent.children;
	for (var child of children) {
		if (child.tagName == 'DIV') {
			var childId = child.getAttribute('id');
			var childTextEl = document.getElementById(childId+'-text');
			toggleSidebarEntry(childTextEl);
		}
	}
}

function toggleSidebarEntry(entry) {
	if(entry.style.display != 'block'){
		entry.style.display = 'block';
	} else {
		entry.style.display = 'none';
	}
}

function loadPage(entry) {
	var id = entry.getAttribute('id');
	if (!isNotNullOrUndefined(id)) {
		throw `Page entry not found for id '${id}'`;
	}
	
	entry.style.background = '#E9EAEA';
	for(var div of document.getElementsByClassName('sidebar')[0].getElementsByTagName('DIV')) {
		if (div !== entry) {
			div.style.background = '#ffffff';
		}
	}
	
	readFromFile(id);
}

function readFromFile(id) {
	var rawFile = new XMLHttpRequest();
	rawFile.open("GET", id, true);
	rawFile.onreadystatechange = function ()
    {
		if(rawFile.readyState === 4)
        {
			if(rawFile.status === 200 || rawFile.status == 0)
            {
				var text = rawFile.responseText;
                var doc = document.implementation.createHTMLDocument("tmp_"+id+"_html_doc");
				doc.documentElement.innerHTML = text;
				var new_element = doc.getElementById('content');
				new_element.id = 'main-content';
                var children = new_element.children;
                makeLinksLocal(children);
                // remove current #main_content element
                var currMainContentEl = document.getElementById('main-content');
                var parentEl = currMainContentEl.parentNode;
                parentEl.removeChild(currMainContentEl);
                // append new content
                parentEl.appendChild(new_element);
                
            }
        }
    }
    rawFile.send(null);
}

function makeLinksLocal(children){
	for(var child of children) {
		if (child.tagName == 'A') {
			var href = child.getAttribute('href');
			if(!href.startsWith('http:') && !href.startsWith('https:')) {
				child.setAttribute('href',`javascript:loadPageFromLink('${href}');`);
			}
		}
		makeLinksLocal(child.children);
	}
}

function loadPageFromLink(href) {
	readFromFile(href);
	document.getElementsByClassName('main_content')[0].scrollTop = 0;
	expandSidebarItem(href);
}

function expandSidebarItem(href) {
	var sidebarEl = document.getElementById(href+'-text');
	var sidebar = document.getElementsByClassName('sidebar')[0];
	var parent = sidebarEl.parentNode.parentNode;
	while(parent != document) {
		if (parent.getAttribute('state') == 'collapsed') {
			var pEl = document.getElementById(parent.getAttribute('id')+'-text');
			var event = {'clientX': 1};
			showEntry(event,pEl);
			event.clientX = 1;
			showEntry(event,pEl);
		}
		parent = parent.parentNode;
	}
	var event = {'clientX': 100000};
	showEntry(event,sidebarEl);
	event.clientX = 1;
	showEntry(event,sidebarEl);
}
