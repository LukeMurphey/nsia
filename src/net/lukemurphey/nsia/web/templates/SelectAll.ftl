<script type="text/javascript" src="/media/js/jquery.shiftcheckbox.js"></script>
<script type="text/javascript">
    function selectAll(){ $('input[type=checkbox]').attr("checked", "true"); };
    function unselectAll(){ $('input[type=checkbox]').removeAttr("checked"); };
    function doCheck(){ 
        if( $('#selectall').attr("checked") ) {
          selectAll();
        }
        else{
          unselectAll();
        }
    };
    
    $(document).ready(function(){
        $('#selectall').click( doCheck );
        $('.selectable').shiftcheckbox();
    });
</script>