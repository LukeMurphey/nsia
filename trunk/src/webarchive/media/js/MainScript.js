
function showHourglass( dialogText ){
	
	
	hourglassElement=document.getElementById("hourglass");
	hourglassElement.setAttribute("class", "hourglassVisible");

	/*for (i = 0; i {
		if (hourglassElement.attributes[i].name == "class")
		{
			hourglassElement.attributes[i].value = "hourglassVisible";
		}
	} */

	//document.getElementById("hourglass").style.display = 'block';

	//image1 = new Image();
	//image1.src = "Loading.gif";
	
	if( dialogText ){
		hourglassTextElement=document.getElementById("hourglassText");

		if (document.all) //if IE 4+
			hourglassTextElement.innerText = dialogText;
		else if (document.getElementById) //else if NS6+
			hourglassTextElement.innerHTML = dialogText;
	}
}
