	<table>
		<tr>
			<td colspan="2"><span class="Text_3">Automatic Refresh</span></td>
		</tr>
		<tr>
			<td>
				<img style="display: none" id="refresh_play" src="/media/img/16_Play" />
				<img id="refresh_pause" src="/media/img/16_Pause" />
			</td>
			<td>
				Updated: <span id="lastRefreshed">--- <noscript>(JavaScript disabled)</noscript></span>
			</td>
		</tr>
	</table>

	<script type="text/javascript">
        var wasPaused = false;
        var counterId = -1;
    	
        function unpauseCountdown(){
            $('#refresh_play').hide();
            $('#refresh_pause').show();
            countDown();
        }
        
        function pauseCountdown(){
            $('#refresh_play').show();
            $('#refresh_pause').hide();
            stopCountDownTimeout();
        }
    	
        function stopCountDownTimeout() {
            clearTimeout(counterId);
            counterId = -1;
        }

        function unpauseTemporarily(){
            if( wasPaused == false ){
                unpauseCountdown();
            }
        }
    	
        function pauseTemporarily(){
            if( isCountDownRunning() ) {
                wasPaused = false;
                pauseCountdown();
            }
            else{
                wasPaused = true;
            }
        }

        function isCountDownRunning() {
            return (counterId != -1);
        }

        function ajaxCallback(responseText, textStatus, XMLHttpRequest) {
            if( XMLHttpRequest.status != 200 ){
                window.location.href=window.location.href
            }
        }

        function getURL(){
        
            if( getArgument("${refresh_url}", "isajax") != null ){
                return "${refresh_url}";
            }
            else if( hasArgs("${refresh_url}") ){
                return "${refresh_url}" + '&isajax';
            }
            else{
                return "${refresh_url}" + '?isajax';
            }
        }

        function getArgument( url, name )
        {
            name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
            var regexS = "[\\?&]"+name+"=([^&#]*)";
            var regex = new RegExp( regexS );
            var results = regex.exec( url );
          
            if( results == null )
                return null;
            else
                return results[1];
        }
        
        function hasArgs( url ){
            if( url.search(/.*[?].*/i) > -1 ){
                return true;
            }
            else{
                return false;
            }
        }

        function refreshView(){
            <#if refresh_url??>
            $("table.MainTable").load( getURL(), {}, ajaxCallback);
            </#if>
        }

        function countDown(){
            counterId = setTimeout("refreshView()", 30000);
        }
		
        function pad(number, length) {

            var str = '' + number;
            
            while (str.length < length) {
                str = '0' + str;
            }
            
            return str;
		}
        
        function updateDateTime(){
            var now = new Date();
            var hour = now.getHours();
            
            var ap = "AM";
            if (hour > 11) { ap = "PM";        }
            if (hour > 12) { hour = hour - 12; }
            if (hour == 0) { hour = 12;        }
			
            var t = hour + ":" + pad(now.getMinutes(),2) + ":" + pad(now.getSeconds(),2) + " " + ap;
            $('#lastRefreshed').text(t);
		}

        $(document).ready(function() {
            $('#refresh_play').click( function() { unpauseCountdown(); } );
            $('#refresh_pause').click( function() { pauseCountdown(); } );
            <#if isajax?? && isajax>$('#lastRefreshed').addClass('refreshTextStart');</#if>
            updateDateTime();
            <#if isajax?? && isajax>$('#lastRefreshed').animate( {'backgroundColor' : '#FFFFF'}, 1000);</#if>
            countDown();
        });
    </script>

<#if refresh_url?? && (!isajax?? || !isajax)>
<noscript>
    <meta name='Refresh' http-equiv="Refresh" content="30;URL=${refresh_url}">
</noscript>
</#if>