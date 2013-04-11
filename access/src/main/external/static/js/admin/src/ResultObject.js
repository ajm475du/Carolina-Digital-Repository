/*

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */
define([ 'jquery', 'jquery-ui', 'PID', 'MetadataObject', 'DeleteObjectButton',
		'PublishObjectButton', 'EditAccessControlForm', 'ModalLoadingOverlay'], function($, ui, PID, MetadataObject) {
	$.widget("cdr.resultObject", {
		options : {
			animateSpeed : 100,
			metadata : null,
			selected : false,
			selectable : true,
			selectCheckboxInitialState : false
		},

		_create : function() {
			if (this.options.metadata instanceof MetadataObject) {
				this.metadata = this.options.metadata;
			} else {
				this.metadata = new MetadataObject(this.options.metadata);
			}

			this.pid = this.metadata.pid;

			if (this.options.selected)
				this.select();

			var self = this;
			if (this.options.selectable) {
				this.checkbox = this.element.find("input[type='checkbox']");
				if (this.checkbox) {
					this.checkbox = $(this.checkbox[0]).click(function(event) {
						self.toggleSelect.apply(self);
						event.stopPropagation();
					}).prop("checked", self.options.selectCheckboxInitialState);
				}
				this.element.click($.proxy(self.toggleSelect, self)).find('a').click(function(event) {
					event.stopPropagation();
				});
			}
			this.initializeActionMenu();
		},
		
		initializeActionMenu : function() {
			var self = this;
			
			var actionMenu = $(".menu_box ul", this.element);//.clone();
			var menuIcon = $(".menu_box img", this.element);
			
			// Set up the dropdown menu
			menuIcon.qtip({
				content: actionMenu,
				position: {
					at: "bottom right",
					my: "top right"
				},
				style: {
					classes: 'qtip-light',
					tip: false
				},
				show: {
					event: 'click',
					delay: 0
				},
				hide: {
					delay: 2000,
					event: 'unfocus mouseleave click',
					fixed: true, // Make sure we can interact with the qTip by setting it as fixed
					effect: function(offset) {
						menuIcon.parent().css("background-color", "transparent");
						$(this).fadeOut(100);
					}
				},
				events: {
					render: function(event, api) {
						self.initializePublishLinks($(this));
						self.initializeDeleteLinks($(this));
					}
				}
			}).click(function(e){
				menuIcon.parent().css("background-color", "#7BAABF");
				e.stopPropagation();
			});
			
			this.element.modalLoadingOverlay({'text' : 'Working...', 'autoOpen' : false});
			
			actionMenu.children(".edit_access").click(function(){
				var dialog = $("<div class='containingDialog'><img src='/static/images/admin/loading-large.gif'/></div>");
				dialog.dialog({
					autoOpen: true,
					width: 500,
					height: 'auto',
					maxHeight: 800,
					minWidth: 500,
					modal: true,
					title: 'Access Control Settings',
					close: function() {
						dialog.remove();
					}
				});
				dialog.load("acl/" + self.pid.getPath(), function(responseText, textStatus, xmlHttpRequest){
					dialog.dialog('option', 'position', 'center');
				});
			});
		},

		initializePublishLinks : function(baseElement) {
			var links = baseElement.find(".publish_link");
			if (!links)
				return;

			var obj = this;
			$(links).publishObjectButton({
				pid : obj.pid,
				parentObject : obj,
				defaultPublish : $.inArray("Unpublished", this.metadata.data.status) == -1
			});
		},

		initializeDeleteLinks : function(baseElement) {
			var links = baseElement.find(".delete_link");
			if (!links)
				return;
			var obj = this;
			$(links).deleteObjectButton({
				pid : obj.pid,
				parentObject : obj
			});
		},

		disable : function() {
			this.options.disabled = true;
			this.element.css("cursor", "default");
			this.element.find(".ajaxCallbackButton").each(function(){
				$(this)[$(this).data("callbackButtonClass")].call($(this), "disable");
			});
		},

		enable : function() {
			this.options.disabled = false;
			this.element.css("cursor", "pointer");
			this.element.find(".ajaxCallbackButton").each(function(){
				$(this)[$(this).data("callbackButtonClass")].call($(this), "enable");
			});
		},
		
		isEnabled : function() {
			return !this.options.disabled;
		},

		toggleSelect : function() {
			if (this.element.hasClass("selected")) {
				this.unselect();
			} else {
				this.select();
			}
		},
		
		getPid : function () {
			return this.pid;
		},
		
		getMetadata : function () {
			return this.metadata;
		},

		select : function() {
			this.element.addClass("selected");
			if (this.checkbox) {
				this.checkbox.prop("checked", true);
			}
		},

		unselect : function() {
			this.element.removeClass("selected");
			if (this.checkbox) {
				this.checkbox.prop("checked", false);
			}
		},

		isSelected : function() {
			return this.element.hasClass("selected");
		},

		setState : function(state) {
			if ("idle" == state || "failed" == state) {
				this.enable();
				this.element.removeClass("followup working").addClass("idle");
				this.element.modalLoadingOverlay('hide');
				// this.element.switchClass("followup working", "idle", this.options.animateSpeed);
			} else if ("working" == state) {
				this.element.modalLoadingOverlay('show');
				this.disable();
				this.element.switchClass("idle followup", "working", this.options.animateSpeed);
			} else if ("followup" == state) {
				this.element.removeClass("idle").addClass("followup", this.options.animateSpeed);
			}
		},

		publish : function() {
			var obj = this;
			obj.metadata.publish();
			if ($.inArray("Parent Unpublished", obj.metadata.data.status) == -1) {
				obj.element.switchClass("unpublished", "published", obj.options.animateSpeed);
			}
			this.element.find(":cdr-publishObjectButton").publishObjectButton("publishedState");
		},

		unpublish : function() {
			var obj = this;
			obj.metadata.unpublish();
			if ($.inArray("Parent Unpublished", obj.metadata.data.status) == -1) {
				obj.element.switchClass("published", "unpublished", obj.options.animateSpeed);
			}
			this.element.find(":cdr-publishObjectButton").publishObjectButton("unpublishedState");
		},

		deleteObject : function() {
			var obj = this;
			obj.element.hide(obj.options.animateSpeed, function() {
				obj.element.remove();
				if (obj.options.resultObjectList) {
					for (var index in obj.options.resultObjectList) {
						obj.options.resultObjectList[index].removeResultObject(obj.pid.getPid());
					}
				}
			});
		},
		
		updateVersion : function(newVersion) {
			if (newVersion != this.metadata.data._version_) {
				this.metadata.data._version_ = newVersion;
				return true;
			}
			return false;
		},
		
		setStatusText : function(text) {
			this.element.modalLoadingOverlay('setText', text);
		}
	});
});