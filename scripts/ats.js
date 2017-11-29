function toggle_ul_elements() {	
	// collapse all elements
	$('#content').find('p.expand_sign').click();
	
	// expand up until element with text equal to the document.referer is found
	$('#content').find('a').each(function(idx, itm){
		if ( document.referrer.includes( $(itm).text() ) ) {
			$(itm).siblings('p.expand_sign').click();
			var ul_parent =  $(itm).parent().parent();
			$(ul_parent).parents('ul > li').children('p.expand_sign').click();
		}
	});
}
                
function expand_or_collapse_ul_element(expand_sign_element) {
	var collapse = $(expand_sign_element).text() == "-";
	var ul_level = parseInt($(expand_sign_element).parent().parent().attr('level')) + 1;
	if (collapse) {
		if ( $(expand_sign_element).text() == '-' ) {
			$(expand_sign_element).text('+');
		}
		$(expand_sign_element).nextAll('ul[level='+ul_level+']').toggle();
	}
	else {
		if ( $(expand_sign_element).text() == '+' ) {
			$(expand_sign_element).text('-');
		}
		$(expand_sign_element).nextAll('ul[level='+ul_level+']').toggle();
	}
}
    
function show_tooltip_for_expand_element(expand_sign_element) {
	if ( $(expand_sign_element).text() == '-' ) {
		$(expand_sign_element).attr('title','Collapse');
    }
    else if ( $(expand_sign_element).text() == '+' ) {
    	$(expand_sign_element).attr('title','Expand');
    }
}

function expand_all() {
	$('#content').find('p.expand_sign').each(function(idx,itm){
		if ( $(itm).text() == '+' ) {
			expand_or_collapse_ul_element(itm);
		}
	});
}


function collapse_all() {
	$('#content').find('p.expand_sign').each(function(idx,itm){
		if ( $(itm).text() == '-' ) {
			expand_or_collapse_ul_element(itm); // close all
			if ( $(itm).parent().parent().attr('level') == 0 ) {
				expand_or_collapse_ul_element(itm); // open only level one elements
			}
		}
	});
}
