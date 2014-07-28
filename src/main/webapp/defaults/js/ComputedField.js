/*
Copyright 2011 Museum of Moving Image

Licensed under the Educational Community License (ECL), Version 2.0. 
You may not use this file except in compliance with this License.

You may obtain a copy of the ECL 2.0 License at
https://source.collectionspace.org/collection-space/LICENSE.txt
 */

/*global jQuery, fluid, cspace:true*/
"use strict";

cspace = cspace || {};

(function ($, fluid) {
    fluid.log("ComputedField.js loaded");
    
    fluid.defaults("cspace.computedField", {
        gradeNames: ["fluid.viewComponent", "autoInit"],
        preInitFunction: "cspace.computedField.preInit",
        postInitFunction: "cspace.computedField.postInit",
        finalInitFunction: "cspace.computedField.finalInit",
        invokers: {
            lookupMessage: "cspace.util.lookupMessage",
            validate: {
                funcName: "cspace.computedField.validate",
                args: ["{computedField}", "{arguments}.0", "{messageBar}", "{arguments}.1"]
            },
            showMessage: {
                funcName: "cspace.computedField.showMessage",
                args: ["{messageBar}", "{arguments}.0"]
            },
            clearMessage: {
                funcName: "cspace.computedField.clearMessage",
                args: "{messageBar}"
            },
            bindModelEvents: {
                funcName: "cspace.computedField.bindModelEvents",
                args: "{computedField}"
            },
            resolveElPath: {
                funcName: "cspace.computedField.resolveElPath",
                args: ["{computedField}", "{arguments}.0"]
            },
            refresh: {
                funcName: "cspace.computedField.refresh",
                args: ["{computedField}", "{arguments}.0"]
            },
            calculateFieldValue: {
                funcName: "cspace.computedField.calculateFieldValue",
                args: "{computedField}"
            },
            getArgListenerNamespace: {
                funcName: "cspace.computedField.getArgListenerNamespace",
                args: ["{computedField}", "{arguments}.0"]
            },
            getFieldListenerNamespace: {
                funcName: "cspace.computedField.getFieldListenerNamespace",
                args: "{computedField}"
            },
            updateLinkState: {
                funcName: "cspace.computedField.updateLinkState",
                args: "{computedField}"
            }
        },
        events: {
			refreshNewRepeatable: null,
            removeAllListeners: null,
            removeApplierListeners: null,
            onSubmit: null
        },
        listeners: {
            removeAllListeners: {
                listener: "{computedField}.removeAllListeners"
            },
            removeApplierListeners: {
                listener: "{computedField}.removeApplierListeners"
            }
        },

        // The root EL path of this field in the model. This will be non-empty if this is a repeating field.
        root: "",

        // The EL path of this field, relative to the root.
        elPath: "",

        // The name of the calculation function used to compute the field value.
        func: "cspace.computedField.joinArgs",

        // Arguments to the calculation function. These are specified as EL paths.
        // See cspace.computedField.resolveElPath for details on how these paths are resolved in the model.
        args: [],

        // The datatype used for validation.
        type: "string",

        // The delay in ms to wait after a key is pressed before validating the field entry.
        delay: 500
    });
       
    cspace.computedField.preInit = function (that) {
        that.applierListenerNamespaces = [];

        that.removeApplierListeners = function () {
            fluid.each(that.applierListenerNamespaces, function(namespace) {            
                that.applier.modelChanged.removeListener(namespace);
            });
        };

        that.removeAllListeners = function() {
            that.removeApplierListeners();
            that.events.onSubmit.removeListener(that.id);
			that.events.refreshNewRepeatable.removeListener(that.id);
        };
        
        that.refreshValue = function(silent) {
            cspace.computedField.refresh(that, silent);
        };
    };

    cspace.computedField.postInit = function (that) {
        that.container.keyup(function () {
            clearTimeout(that.outFirer);
            that.outFirer = setTimeout(function () {
                that.clearMessage();
                var value = that.container.val();
                that.validate(value, that.invalidNumberMessage);
            }, that.options.delay);
        });
    };

    cspace.computedField.finalInit = function (that) {
        that.labelText = "";

        if (that.options.label) {
            that.labelText = that.lookupMessage(that.options.label) + ": ";
        }

        that.invalidNumberMessage = fluid.stringTemplate(that.lookupMessage("invalidNumber"), {
            label: that.labelText
        });

        that.invalidCalculationMessage = fluid.stringTemplate(that.lookupMessage("errorCalculating"), {
            label: that.labelText,
            status: that.lookupMessage("invalidCalculatedNumber")
        });

        if (that.options.readOnly) {
            that.container.attr("disabled", true);
        }
        
        that.fullElPath = cspace.util.composeSegments(that.options.root, that.options.elPath);
        that.fullArgElPaths = [];

        fluid.each(that.options.args, function(argElPath) {
            that.fullArgElPaths.push(that.resolveElPath(argElPath));
        });

        that.events.onSubmit.addListener(function() {
			// Refresh the field value when the record is saved. I'm not sure if this is always
			// a good idea, but it would help in cases where a computed field has the wrong value,
			// through an import or database update, or for some other reason. This would correct
			// the value without the user having to modify any of the linked fields.
			// This should only be done if the computed field is read-only; otherwise, it would
			// overwrite any manually entered override. 
			
			if (that.options.readOnly) {
				that.refreshValue();
			}
		}, that.id);
		
		that.events.refreshNewRepeatable.addListener(function() {
			// If this is the last instance in a repeatable, then it was just created.
			// Refresh the value, in case empty inputs result in a non-empty output.

			var elPath = that.fullElPath;
			var parts = elPath.split('.');
			
			// Find the index of the repeat instance. This is the last el path component
			// that is numeric.
			
			var repeatIndex = -1;
			var repeatElPath = '';
			
			for (var i=parts.length-1; i>=0; i--) {
				var part = parts[i];
				
				if (part.match(/^\d+$/)) {
					repeatIndex = parseInt(part);
					repeatElPath = parts.slice(0, i).join('.');
					
					break;
				}
			}
			
			if (repeatIndex >= 0) {
				// Check if the index of the repeat instance makes it the last
				// instance, which means it's the one that was just added.
				// Refresh the value.
				
				var repeatModel = fluid.get(that.model, repeatElPath);
				
				if (Array.isArray(repeatModel)) {
					if (repeatIndex == repeatModel.length - 1) {
						that.refreshValue(true);
					}
				}
			}
		}, that.id);

        that.bindModelEvents();
		
		that.container.addClass("cs-computedField");
		that.updateLinkState();
		
		// If this is a new record, refresh the computed field immediately. This is necessary because
		// empty inputs to the computation function could produce a non-empty output.
		
		if (!that.model.csid) {
			that.refreshValue(true);
		}
    };

    /*
     * Registers listeners for changes in the model.
     * When the value at an EL path specified as an argument to the calculation function is changed, recompute the field value, and update the model.
     * When the value of the field is updated in the model, update it in the view.
     */
    cspace.computedField.bindModelEvents = function (that) {
        fluid.each(that.fullArgElPaths, function(fullArgElPath) {
            var namespace = that.getArgListenerNamespace(fullArgElPath);
            
            that.applier.modelChanged.addListener(fullArgElPath, function(model, oldModel, changeRequests) {
				// Test the path of the change.
				
				// If it's empty, the model is being completely replaced after a save, and there's no
				// need to recompute the field values. For fields that are not read-only, and were
				// overridden, recomputing would overwrite the overridden value that was just saved.
				
				// If it's a parent of the root of this field's el path, a repeating field or field 
				// group is being replaced because of an addition or deletion. There is also no need
				// to recompute. Recomputing would re-add the field or group being deleted, undermining
				// the deletion.
				
				var changeRequest = changeRequests[0];
				var path = changeRequest.path;
				
				if (path != "" && that.options.root.substring(0, path.length + 1) != (path + ".")) {
                	that.refresh();
				}
            }, namespace);

            that.applierListenerNamespaces.push(namespace);
        });

		if (!that.options.readOnly) {
	        var namespace = that.getFieldListenerNamespace();
        
	        that.applier.modelChanged.addListener(that.fullElPath, function(model, oldModel, changeRequests) {
				that.updateLinkState();
	        }, namespace);
        
	        that.applierListenerNamespaces.push(namespace);
		}
    };

    cspace.computedField.updateLinkState = function (that) {
		var value = fluid.get(that.model, that.fullElPath) || "";
		var calculatedValue = that.calculateFieldValue();
		
		if (value != calculatedValue) {
			that.container.addClass("overridden");
		}
		else {
			that.container.removeClass("overridden");
		}
    }
	
    /*
     * Updates the field value, showing an error message if necessary.
     * If silent is true, the update will not result in the page being considered modified
     * if the user attempts to navigate away without saving. This is useful for initializing
     * computed fields when records are created, or new repeating field instances are created.
     */
    cspace.computedField.refresh = function (that, silent) {
        that.clearMessage();

        var newValue;

        try {
            newValue = that.calculateFieldValue();
        }
        catch (error) {
            var message = fluid.stringTemplate(that.lookupMessage("errorCalculating"), {
                label: that.labelText,
                status: error.message
            });
        
            that.showMessage(message);
            return;
        }
    
        if (!that.validate(newValue, that.invalidCalculationMessage)) {
            return;
        }
    
        /*
         * If the new value is empty, and the target field is undefined, don't do anything.
         * By setting the value, the change detector would think that the page is modified,
         * because the field went from undefined to empty string; but to the user, they're the
         * same thing. This prevents an apparently spurious confirmation dialog, if the user
         * attempts to navigate away without saving.
         */
        var hasDefinedTarget = typeof(fluid.get(that.model, that.fullElPath)) !== 'undefined';
		
        if (newValue == "" && !hasDefinedTarget) {
			return;
		}
		
        that.container.val(newValue);
		
		if (silent) {
	        that.applier.fireChangeRequest({
	            path: that.fullElPath,
	            value: newValue,
	            silent: true
	        });
		}
		else {
			that.container.change();
		}
    }

    /*
     * Calculates the field value, applying the configured calculation function to the values
     * of the argument EL paths in the model.
     */
    cspace.computedField.calculateFieldValue = function (that) {
        var args = [];

        fluid.each(that.fullArgElPaths, function(fullArgElPath) {
            args.push(fluid.get(that.model, fullArgElPath));
        });

        return fluid.invoke(that.options.func, args);
    };

    /*
     * Resolve the given EL path. If the EL path starts with the document root, it is simply returned.
     * Otherwise, the argument EL path is composed with this field's root EL path.
     * TODO: Make this function smarter about finding argument elPaths that are outside of this field's root.
     * Returns the full EL path.
     */
    cspace.computedField.resolveElPath = function (that, elPath) {
        var documentRoot = "fields";
        var resolvedPath;
        
        if (elPath.substr(0, documentRoot.length + 1) == (documentRoot + ".")) {
            // The argument El path starts with the document root.
            
            resolvedPath = elPath;
        }
        else {
            var root = that.options.root;
            
            if (!root) {
                // This field has an undefined/null/empty root EL path.
                // Use the document root.
                
                root = documentRoot;
            }
            
            resolvedPath = cspace.util.composeSegments(root, elPath);
        }
        
        return resolvedPath;
    };
    
    /*
     * Returns the passed arguments as a comma-separated string. This is useful as a default calculation function.
     */
    cspace.computedField.joinArgs = function () {
        // The arguments to a javascript function are stored in the variable named arguments, which is
        // Array-like, but not an Array. Convert it to an Array using Array.prototype.slice.
        var args = Array.prototype.slice.call(arguments);
        return (args.join(", "));
    };

    /*
     * Returns a unique namespace name for a given argument EL path.
     */
    cspace.computedField.getArgListenerNamespace = function (that, elPath) {
        return  (elPath + ":" + that.fullElPath);
    };

    /*
     * Returns a unique namespace name for this component's field.
     */
    cspace.computedField.getFieldListenerNamespace = function (that) {
        return  ("elPath-" + that.id);
    };

    cspace.computedField.validate = function (that, value, messageBar, message) {
        var valid = true;
        
        if (that.options.type && that.options.type != "string") {
            valid = cspace.util.validate(value, that.options.type, messageBar, message);            
        }
        
        return valid;
    }

    cspace.computedField.showMessage = function (messageBar, message) {
        messageBar.show(message, null, true);
    }

    cspace.computedField.clearMessage = function (messageBar) {
        messageBar.hide();
    };
})(jQuery, fluid);