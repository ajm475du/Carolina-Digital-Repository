require.config({
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'jquery.min',
		'jquery-ui' : 'jquery-ui.min',
		'qtip' : 'jquery.qtip.min',
		'jquery.preload': 'jquery.preload-1.0.8-unc',
		'thumbnail' : 'thumbnail',
		'adminCommon' : 'admin/adminCommon',
		'PID' : 'admin/src/PID',
		'MetadataObject' : 'admin/src/MetadataObject',
		'AjaxCallbackButton' : 'admin/src/AjaxCallbackButton',
		'PublishObjectButton' : 'admin/src/PublishObjectButton',
		'DeleteObjectButton' : 'admin/src/DeleteObjectButton',
		'ResultObject' : 'admin/src/ResultObject',
		'ResultObjectList' : 'admin/src/ResultObjectList',
		'BatchCallbackButton' : 'admin/src/BatchCallbackButton',
		'UnpublishBatchButton' : 'admin/src/UnpublishBatchButton',
		'PublishBatchButton' : 'admin/src/PublishBatchButton',
		'DeleteBatchButton' : 'admin/src/DeleteBatchButton',
		'EditAccessControlForm' : 'admin/src/EditAccessControlForm',
		'editable' : 'jqueryui-editable.min',
		'moment' : 'moment.min'
	},
	shim: {
		'jquery-ui' : {
			deps: ['jquery']
		},
		'jquery.preload' : ['jquery'],
		'thumbnail' : ['jquery'],
		'qtip' : ['jquery'],
		'adminCommon' : ['jquery'],
		'editable' : ['jquery']
	}
});

define('resultList', ['module', 'jquery', 'ResultObjectList', 'PublishBatchButton', 'UnpublishBatchButton', 
                      'DeleteBatchButton', 'EditAccessControlForm'], function(module, $, ResultObjectList) {
	$("#select_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", true);
		$(".browseitem").addClass("selected");
	});
	
	$("#deselect_all").click(function(){
		$(".browseitem input[type='checkbox']").prop("checked", false);
		$(".browseitem").removeClass("selected");
	});
	
	var resultObjectList = new ResultObjectList({'metadataObjects' : module.config().metadataObjects});
	
	
	$("#publish_selected").publishBatchButton({'resultObjectList' : resultObjectList});
	$("#unpublish_selected").unpublishBatchButton({'resultObjectList' : resultObjectList});
	$("#delete_selected").deleteBatchButton({'resultObjectList' : resultObjectList});
});