/-----------------------------------------------------------------------------
 --  core_sandbox.fun                                                     --
 --                                                                         --
 --  Sandbox for writing and running Fun code interactively.              --
 --                                                                         --
 --  Copyright (c) 2014 by fundev.org.  All rights reserved.              --
 --                                                                         --
 -----------------------------------------------------------------------------/

core {

    /---- switches ----/
    
    boolean allow_sandbox {
        boolean enabled = (site_name == "default" || this_domain.get("sandbox_enabled"));
    
        log("setting allow_sandbox to " + enabled);
        log("site_name = " + site_name);
        enabled;
        eval(sandbox_status(enabled ? "Sandbox enabled" : "Sandbox disabled")); 
    }

    /---- constants ----/

    SANDBOX_SITE = "~sandbox_site"
    SANDBOX_NAME = "`~sandbox`"

    /** current sandbox status **/
    sandbox_status(msg) = msg
 
    /---- source code storage and processing ----/

    /** raw sandbox contents **/ 
    sandbox_editor_contents(snippet) = snippet
    
    /** template to create a fun site out of the sandbox contents **/
    dynamic src_template(contents) {
    
        [`` site `~sandbox_site` { dynamic `~sandbox` { log("instantiating sandbox"); ``]
        
        contents;
        
        [`` } } ``]
    }
    
    /** templated sandbox contents **/
    dynamic sandbox_source = src_template(sandbox_editor_contents)

    /** create a fun domain from a string **/
    dynamic fun_domain domain_from_string(name, str) = this_domain.child_domain(name, "site", str, false)

    /** create the sandbox domain **/
    dynamic fun_domain sandbox_domain = domain_from_string(SANDBOX_SITE, sandbox_source)


    /---- the UI ----/

    /** sandbox page **/
    page(*) sandbox(params{}) {
        boolean auto_style = false
        boolean ajax_enabled = true
       
        links[] = [ [| href="sandbox.css" rel="stylesheet" type="text/css" |] ]
        
        css [|
            body {
                width: 100%;
                height: 100%;
                margin: 0;
                background: #eef4f0;
            }
            
            .source_editor {
                width: 100%;
                height: 100%;
                border: 0;
            }
            .source_editor textarea {
                width: 100%;
                height: 90%;
                border: 0;
            }
        |]
    
        script [|
            window.sandbox_frames = {
                source_frame: document.getElementById("sandbox_source_page"),
                output_frame: document.getElementById("sandbox_output_page")
            };
        |]

        if (allow_sandbox) {        

            if (params["contents"]) {
                eval(sandbox_editor_contents(: params["contents"] :));
            }

            sandbox_iframe(sandbox_source_page);
            sandbox_iframe(sandbox_output_page);
        } else {
            redirect index;
        }
    }
    
    
    dynamic sandbox_iframe(page p) [|
        <iframe id="{= p.type; =}" src="{= p.type; =}" style="width:100%;height:50%">
        </iframe>
    |]
    
    
    page(*) sandbox_source_page(params{}) {
        boolean ajax_enabled = true
        links[] = [ [| href="sandbox.css" rel="stylesheet" type="text/css" |] ]

        find_element_function [|
            function findElement(id) { 
                var element = document.getElementById(id);
                if (!element && window.parent) {
                    var sandbox_frames = window.parent.sandbox_frames;
                    if (sandbox_frames !== undefined) {
                        var output_frame_prefix = "sandbox_output_page.";
                        if (id.lastIndexOf(output_frame_prefix, 0) === 0) {
                            var local_id = id.substring(output_frame_prefix.length);                    
                            element = sandbox_frames.output_frame.contentWindow.document.getElementById(local_id);
                        }
                    }
                }
                return element;
            }
        |]


        source_code = params["sandbox_contents"] ? sandbox_editor_contents(: params["sandbox_contents"] :) : sandbox_editor_contents
    
        component source_editor(contents) {
            component_class = "source_editor"
            component status_box {
                sandbox_status;
            }
            component compile_panel {
                submit_button("compile_button", "Compile", target_id, "sandbox_contents");
                status_box;
            }
            
            log("  ------->  source_editor contents: " + contents);
        
            textarea("sandbox_contents", contents, 0, 25);
            compile_panel;
        }
        
        source_editor(source_code);
    }
    
    target_id = "/sandbox_output_page.sandbox_target"

    page(*) sandbox_output_page(params{}) {
        boolean ajax_enabled = true
        links[] = [ [| href="sandbox.css" rel="stylesheet" type="text/css" |] ]

        dynamic component sandbox_target(params{}) {
            log(" ------> passed to sandbox_target: " + params["sandbox_contents"]);
            if (params["sandbox_contents"]) {
                eval(sandbox_editor_contents(: params["sandbox_contents"] :));
            }
            log(" ---> editor contents: " + sandbox_editor_contents); 

            sandbox_domain.get(SANDBOX_NAME);
        }

        sandbox_target(params);
    }
}
