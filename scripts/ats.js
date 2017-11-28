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
		$(expand_sign_element).text('+');
		$(expand_sign_element).nextAll('ul[level='+ul_level+']').toggle();
	} 
	else {
		$(expand_sign_element).text('-');
		$(expand_sign_element).nextAll('ul[level='+ul_level+']').toggle();
	}
}
    
function show_tooltip_for_expand_element(expand_sign_element) {
	if ( $(expand_sign_element).text() == '-' ) {
		$(expand_sign_element).attr('title','Collapse');
    }
    else {
    	$(expand_sign_element).attr('title','Expand');
    }    
}
