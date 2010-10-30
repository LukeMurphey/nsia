
function setRefreshRate(rate) {
    refreshRate = rate - 0;
}

function getRefreshRate() {
    return refreshRate;
}

setRefreshRate(60);

function onRateChange(selectBox) {
	if( selectBox.options[selectBox.selectedIndex].value == "Disable" ){
		stopCountDownTimeout();
		refreshRate = "Disable";
	}
	else
		setRefreshRate(selectBox.options[selectBox.selectedIndex].value);

	resetCountDown();
	//setCookie();
}

//configure refresh interval (in seconds)
var countDownTime = 0;

function resetCountDown() {
	countDownTime = getRefreshRate();
	if( isNaN( countDownTime ) || countDownTime == "Disable" ){
		stopCountDownTimeout();
	}
	else
		updateCountDownText(" in " + countDownTime + " seconds");
}

//configure width of displayed text, in px (applicable only in NS4)
resetCountDown();
var c_reloadwidth=200

function refreshView(){
	document.reloadForm.submit();
	//$("table.MainTable").load(location.href + '?isajax');
}

function countDown() {
	countDownTime--;
	if( isNaN(countDownTime) ){
		updateCountDownText(" disabled");
		return;
	}
	else if (countDownTime < 0 ) {
		//updateReloadFormValues(true);
		stopCountDownTimeout();
		refreshView();
		
		return;
        }

	updateCountDownText(" in " + countDownTime + " seconds");
	startCountDownTimeout();
}

function updateCountDownText(updateText) {
	var elem = getElem("countDownText");

	if(!elem) return;

	if (document.all) //if IE 4+
		elem.innerText = updateText;
        else if (document.getElementById) //else if NS6+
		elem.innerHTML = updateText;
	}

function getElem(elemId) {
	if (document.all) //if IE 4+
		return document.all[elemId];
	else if (document.getElementById) //else if NS6+
		return document.getElementById(elemId);
}

function showCountDownMsg() {
	var countDownMsg = getElem("countDownMsg");
	var pauseMsg = getElem("pausedMsg");

	if(!countDownMsg || !pauseMsg) return;

	countDownMsg.style.display = "inline";
	pauseMsg.style.display = "none";
}

function showPausedMsg() {
	var countDownMsg = getElem("countDownMsg");
	var pauseMsg = getElem("pausedMsg");

	if(!countDownMsg || !pauseMsg) return;

	countDownMsg.style.display = "none";
	pauseMsg.style.display = "inline";
}

var counterId = -1;

function startCountDownTimeout() {
	counterId = setTimeout("countDown()", 1000);
}

function stopCountDownTimeout() {
	clearTimeout(counterId);
	counterId = -1;
}

function isCountDownRunning() {
	return (counterId != -1);
}

function startit() {
	startCountDownTimeout();
}

var wasPaused = false;

function pauseTemporarily(){
	if( isCountDownRunning() ) {
		wasPaused = false;
		pauseCountdown();
	}
	else{
		wasPaused = true;
	}
}

function unpauseTemporarily(){
	if( wasPaused == false ){
		unpauseCountdown();
	}
}

function pauseCountdown(){
	var playImg = getElem("refresh_play");
	var pauseImg = getElem("refresh_pause");

	showPausedMsg();
	stopCountDownTimeout();

	pauseImg.style.display = "none";
	playImg.style.display = "inline";
}

function unpauseCountdown(){
	var playImg = getElem("refresh_play");
	var pauseImg = getElem("refresh_pause");

	showCountDownMsg();
	countDown();

	pauseImg.style.display = "inline";
	playImg.style.display = "none";
}

function onPlayPauseClick() {
	var playImg = getElem("refresh_play");
	var pauseImg = getElem("refresh_pause");

    if(isCountDownRunning()) {
		showPausedMsg();
		stopCountDownTimeout();

		pauseImg.style.display = "none";
		playImg.style.display = "inline";
	} else {
		showCountDownMsg();
		countDown();

		pauseImg.style.display = "inline";
		playImg.style.display = "none";
	}
}

function onReloadClick() {
	//updateReloadFormValues(true);
	stopCountDownTimeout();
	refreshView();
}
