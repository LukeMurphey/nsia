

function trace( msg ){
  if( typeof( jsTrace ) != 'undefined' ){
    jsTrace.send( msg );
  }
}

var xmlHttp;
//The method below retrieves the updated status and modifies the 
function prepareAjax()
{
	// 1 -- Load the xmlhttp control
	
	try
	{
		// Firefox, Opera 8.0+, Safari
		xmlHttp=new XMLHttpRequest();
	}
	catch (e)
	{
		// Internet Explorer
		try
		{
			xmlHttp=new ActiveXObject("Msxml2.XMLHTTP");
		}
		catch (e)
		{
			try
			{
				xmlHttp=new ActiveXObject("Microsoft.XMLHTTP");
			}
			catch (e)
			{
				alert("Your browser does not support AJAX!");
				return false;
			}
		}
	}
	
	//alert("xmlHttp created");
	
	xmlHttp.onreadystatechange=function(){
		// 2 -- Get the current status
		if(xmlHttp != undefined && xmlHttp != null && xmlHttp.readyState==4)
		{
			
			var doc = xmlHttp.responseXML;
			
			if(xmlHttp.status == 200){ //Removed per: http://helpful.knobs-dials.com/index.php/%22Component_returned_failure_code:_0x80040111_(NS_ERROR_NOT_AVAILABLE)%22
				
				var progress = GetInnerText( doc.getElementsByTagName('Progress').item(0) );
				var state = GetInnerText( doc.getElementsByTagName('State').item(0) );
				var statusDesc = GetInnerText( doc.getElementsByTagName('StatusDescription').item(0) );
				
				// 3 -- Reset the progress bar
				if( progress > 0 ){
          trace( progress );
          document.getElementById('ProgressBar').style.width = ((363 * progress ) / 100)+"px";
		  document.getElementById('Description').innerHTML = statusDesc;
		  trace( statusDesc );
        }
        
        // 4 -- Refresh the entire page if the task is complete
        if( state == "STOPPED" ){
          //trace("Reloading to " + finalLocation);
          window.location.replace( finalLocation );
        }
				
			}
			
			setTimeout( "progressBarUpdate()", 3000);
		}
		
	}
	
	
}



function GetInnerText(node)
{
	return(node.textContent||node.innerText||node.text );
}

function progressBarUpdate(){
	//Initiate the Ajax command processor
	prepareAjax();
	xmlHttp.open("GET",ajaxsrc,true);
	xmlHttp.send(null);
	//setTimeout( "progressBarUpdate()", 3000);
}

// Call the progressBarUpdate method to update the progress bar every few seconds
progressBarUpdate();
//setTimeout( "progressBarUpdate()", 3000);