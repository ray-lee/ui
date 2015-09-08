/*
Copyright 2011 Museum of Moving Image

Licensed under the Educational Community License (ECL), Version 2.0. 
You may not use this file except in compliance with this License.

You may obtain a copy of the ECL 2.0 License at
https://source.collectionspace.org/collection-space/LICENSE.txt
*/

/*global cspace:true, jQuery, fluid*/
"use strict";

console.log("transport.js")

cspace = cspace || {};

(function ($, fluid) {

    // Basic component used to display transport specific
    // block within the record rendering/editing section.
    fluid.defaults("cspace.transport", {
        gradeNames: ["fluid.rendererComponent", "autoInit"],
        mergePolicy: {
            "rendererFnOptions.protoTree": "protoTree",
            "rendererOptions.applier": "applier",
            protoTree: "noexpand"
        },
        readOnly: false,
        selectors: {},
        strings: {},
        parentBundle: "{globalBundle}",
        rendererFnOptions: {
            cutpointGenerator: "cspace.transport.cutpointGenerator"
        },
        resources: {
            template: cspace.resourceSpecExpander({
                fetchClass: "fastTemplate",
                url: "%webapp/html/components/TransportTemplate.html",
                options: {
                    dataType: "html"
                }
            })
        },
        finalInitFunction: "cspace.transport.finalInit",
        renderOnInit: true
    });
    
    cspace.transport.finalInit = function (that) {
        // If record is read only, make sure everything is disabled.
        cspace.util.processReadOnly(that.container, that.options.readOnly);
        if (that.options.readOnly) {
            $("a", that.container).hide();
        }
    };

    cspace.transport.cutpointGenerator = function (selectors, options) {
        return cspace.renderUtils.cutpointsFromUISpec(options.protoTree);
    };
    
    fluid.fetchResources.primeCacheFromResources("cspace.transport");
    
})(jQuery, fluid);