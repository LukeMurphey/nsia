<script type="text/javascript">
    <#-- A dialog to confirm an operation -->
    function openYesNoDialog(prompt, title, onOK){
        <#-- Populate the default arguments -->
        if( typeof(title) == 'undefined' ){
            title = "Alert";
        }
    
        if( typeof(okButtonText) == 'undefined' ){
            okButtonText = "OK";
        }
        
        if( typeof(height) == 'undefined' ){
            height = 140;
        }
        <#-- Show the dialog -->
        var $dialog = $('<div></div>')
            .html( '<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>' + prompt + "</p>")
            .dialog({
                autoOpen: true,
                title: title,
                height: height,
                modal: true,
                buttons: {
                    Ok: function() {
                        $( this ).dialog( "close" );
                        if( (typeof onOK) == 'function' ){
                            onOK();
                        }
                        else{
                            location.href = onOk;
                        }
                        return false;
                    },
                    Cancel: function() {
                        $( this ).dialog( "close" );
                        return false;
                    }
                }
            });
    }
    <#-- A dialog to confirm the delection -->
    function openDeleteConfirmDialog(prompt, title, onOK){
    
        if( typeof(title) == 'undefined' ){
            title = "Alert";
        }
    
        if( typeof(okButtonText) == 'undefined' ){
            okButtonText = "OK";
        }
        
        if( typeof(height) == 'undefined' ){
            height = 140;
        }
        <#-- Show the dialog -->
        var $dialog = $('<div></div>')
            .html( '<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>' + prompt + "</p>")
            .dialog({
                autoOpen: true,
                title: title,
                height: height,
                modal: true,
                buttons: {
                    Delete: function() {
                        $( this ).dialog( "close" );
                        if( (typeof onOK) == 'function' ){
                            onOK();
                        }
                        else{
                            location.href = onOK;
                        }
                        return false;
                    },
                    Cancel: function() {
                        $( this ).dialog( "close" );
                        return false;
                    }
                }
            });
     }
</script>