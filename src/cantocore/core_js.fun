/-----------------------------------------------------------------------------
 --  core_js.fun                                                          --
 --                                                                         --
 --  Core JavaScript definitions for Fun.                                 --
 --                                                                         --
 --  Copyright (c) 2006-2017 by fundev.org.  All rights reserved.         --
 --                                                                         --
 -----------------------------------------------------------------------------/

core {

    /** base class for definitions that instantiate javascript. **/
    javascript [/]

    dynamic core_scripts(boolean{} options, find_element_function, page_name) {

        boolean use_wait_cursor = options["use_wait_cursor"]
        if (options["graphics"]) {
            vlog("**graphics enabled**");
            graphics_script;
        } else {
            vlog("**graphics disabled**");            
        }
        
        if (options["drag"]) {
            vlog("**dragging enabled**");
            drag_script;
        } else {
            vlog("**dragging disabled**");
        }
       
        if (options["ajax"]) {
            log("**ajax enabled**");
            ajax_script(page_name, find_element_function, use_wait_cursor);
        } else {
            log("**ajax disabled**");
        }

        if (options["animation"]) {
            vlog("**animation enabled**");
            animation_script(page_name, use_wait_cursor);
        } else {
            vlog("**animation disabled**");
        }

        if (options["debug"]) {
            vlog("**debugging enabled**");
            debug_script;
        } else {
            vlog("**debugging disabled**");
        }

    }

    /** Base class for cross-browser functions to handle events. **/
    javascript event_handler [|
        function {= type; =}(e) {
            if (!e) {
               e = window.event;
            }
            var targetElement = (e.target ? e.target : e.srcElement);
            {= sub; =}
            return false;
        }
    |]
    
    /** Inspired by Scott Andrew's code at http://www.scottandrew.com/weblog/articles/cbs-events **/
    javascript add_handler(event_type, event_handler eh) [|
        {=/** W3C support **/=}
        if (document.addEventListener) {
            document.addEventListener({= event_type; =}, {= eh.type; =}, false);
            return true;
        {=/** IE support **/=}
        } else if (document.attachEvent) {
            var r = document.attachEvent("on" + {= event_type; =}, {= eh.type; =});
            return r;
        } else {
            alert("Sorry, this web site won't work correctly in your browser.  Either JavaScript has been disabled, or your browser is too old, new or weird.");  
        }
    |]        
    
    event_handler select_handler [|
        selectedElement = targetElement;
        while (selectedElement != null && (selectedElement.id == null || !selectable[selectedElement.id])) {
            selectedElement = selectedElement.parentNode;
        }
        if (selectedElement != null) {    
            showSelected(selectedElement);
            dragStart(selectedElement, e);
        }
    |]                    

    javascript init_draggable {
        for d in site.descendants_of_type("draggable") {
            if (d.id) [|
                draggable["{= d.id; =}"] = true;
            |]
        }
    }

    javascript init_selectable { 
        [| var selectableElement; |]
        for s in site.descendants_of_type("selectable") {
            if (s.id) [|
                selectableElement = document.getElementById("{= s.id; =}");
                if (selectableElement != null) {
                    topz++;
                    selectable["{= s.id; =}"] = true;
                    selectableElement.style.zIndex = topz;
                }
            |]
        }
        [| selectable.length = topz; |]
    }
    
    javascript drag_script [|
        var eventModel;
        var selectedElement;
        var dragElement;
        var dragID;
        var xOffset;
        var yOffset;
        var dragPurpose;
        var topz = 0;
        var draggable = new Array();
        var selectable = new Array();
        var seldeco;
       
        // operations
        var MOVE = "move";
        var RESIZE = "resize";
        var CONNECT = "connect";
  
        // directions
        var NW = "nw"
        var N = "n"
        var NE = "ne"
        var W = "w"
        var C = "c"
        var E = "e"
        var SW = "sw"
        var S = "s"
        var SE = "se"

        {= select_handler; =}
       
        function showSelected(element) {
            moveToTop(element);
            seldecorate(element);
        }
        
        function moveToTop(element) {
            var z;
            
            z = parseInt(element.style.zIndex, 10);
            
            for (id in selectable)  {
                var el = document.getElementById(id);
                if (el != null) {
                   var oldz = parseInt(el.style.zIndex, 10);
                   if (oldz > z) {
                       el.style.zIndex = oldz - 1;
	               }
                }
            }
            element.style.zIndex = topz + 1;
        }

        function initSeldeco() {
            seldeco = document.getElementById("selection_decoration");
            seldeco.setpos = function(x, y, width, height) {
                width = width + 10;
                height = height + 10;
                this.style.left = (x - 5) + "px";
                this.style.top = (y - 5) + "px";
                this.style.width = width + "px";
                this.style.height = height + "px";
                this.style.zIndex = topz;

                var n_handle = document.getElementById("n_handle");
                n_handle.style.left = ((width - 7) / 2) + "px";

                var ne_handle = document.getElementById("ne_handle");
                ne_handle.style.left = (width - 7) + "px";

                var w_handle = document.getElementById("w_handle");
                w_handle.style.top = ((height - 7) / 2) + "px";

                var e_handle = document.getElementById("e_handle");
                e_handle.style.left = (width - 7) + "px";
                e_handle.style.top = ((height - 7) / 2) + "px";

                var sw_handle = document.getElementById("sw_handle");
                sw_handle.style.top = (height - 7) + "px";

                var s_handle = document.getElementById("s_handle");
                s_handle.style.left = ((width - 7) / 2) + "px";
                s_handle.style.top = (height - 7) + "px";

                var se_handle = document.getElementById("se_handle");
                se_handle.style.left = (width - 7) + "px";
                se_handle.style.top = (height - 7) + "px";

                this.style.visibility = "visible";
            }
        }

        function seldecorate(element) {
            if (!element) {
                return;
            }
            
            var x = parseInt(element.style.left);
            if (isNaN(x)) {
                x = element.offsetLeft;
            }
            var y = parseInt(element.style.top);
            if (isNaN(y)) {
                y = element.offsetTop;
            }
            
            var width = parseInt(element.style.width);
            if (isNaN(width) || width == 0) {
                width = element.offsetWidth;
            }
        
            var height = parseInt(element.style.height);
            if (isNaN(height) || height == 0) {
                height = element.offsetHeight;
            }
            seldeco.setpos(x, y, width, height);
        }
                
        function dragStart(element, e) {
            var x;
            var y;

            if (!element) {
                return true;
            }
            
            if (!e) {
                e = window.event;
            }
            
            dragElement = element;

            x = parseInt(dragElement.style.left);
            if (isNaN(x)) {
                x = dragElement.offsetLeft;
            }
            y = parseInt(dragElement.style.top);
            if (isNaN(y)) {
                y = dragElement.offsetTop;
            }

            xOffset = e.clientX - x;
            yOffset = e.clientY - y;

            if (eventModel == "W3C") {
                document.addEventListener("mousemove", dragMove, true);
                document.addEventListener("mouseup", dragStop, true);
                e.stopPropagation();
            } else {
                document.onmousemove = dragMove;
                document.onmouseup = dragStop;
                window.event.cancelBubble = true;
            }

            return false;
        }

        function dragMove(e) {
            if (eventModel == "IE") {
                e = window.event;
            }

            dragElement.style.left = (e.clientX - xOffset) + "px";
            dragElement.style.top = (e.clientY - yOffset) + "px";
            
            seldecorate(dragElement);

            if (eventModel == "W3C") {
                e.stopPropagation();
            } else {
                window.event.cancelBubble = true;
            }

            return false;
        }

        function dragStop(e) {
            if (eventModel == "IE") {
                e = window.event;
                document.onmousemove = null;
                document.onmouseup = null;
                window.event.cancelBubble = true;
            } else {
                document.removeEventListener("mouseup", dragStop, true);
                document.removeEventListener("mousemove", dragMove, true);
                e.stopPropagation();
            }
            return false;
        }

        function pageLoad(e) {
            eventModel = (e ? "W3C" : (window.event ? "IE" : "unknown"));
            {=
                init_selectable;
                init_draggable;
            =}

            if (eventModel == "IE") {
                document.onmousedown = select_handler;
            } else { /*W3C*/
                for (id in selectable)  {
                    var el = document.getElementById(id);
                    if (el != null && el.addEventListener) {
                        el.addEventListener("mousedown", select_handler, true);
                    }
                }
            }

            initSeldeco();
            return true;
        }
        window.onload = pageLoad;
    |]

    javascript ajax_script(page_name, find_element_function, boolean use_wait_cursor) [|
    
        {= find_element_function; =}
        
        function createXMLHttpRequest() {
            var xmlHttp = undefined;

            if (window.XMLHttpRequest) {
                xmlHttp = new XMLHttpRequest();

            } else if (window.ActiveXObject) {
                try {
                    xmlHttp = new ActiveXObject("Msxml2.XMLHTTP");
                } catch (e) {
                    try {
                        xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
                    } catch (ee) {
                        xmlHttp = undefined;
                    }
                }
            }
            return xmlHttp;
        }

        function refreshComponent(id) {
            var req = undefined;
            var req_id = id;

            if (id != undefined) {
                if (id.charAt(0) == '/') {
                    id = id.substring(1);
                    req_id = "$" + id;
                } else {
                    req_id = "${= page_name; =}." + id;
                }

                req = createXMLHttpRequest();
                if (req != undefined) {
                    req.onreadystatechange = getComponentResponder(req, id, null, null);
                    req.open("GET", req_id, true);
                    req.send("");
                }
                return false;
            } else {
                return true;
            }
        }

        function pollComponent(id, wait, perPollFunction) {
            var req = undefined;
            var req_id = id;

            if (id != undefined) {
                if (id.charAt(0) == '/') {
                    id = id.substring(1);
                    req_id = "$" + id;
                } else {
                    req_id = "${= page_name; =}." + id;
                }

                req = createXMLHttpRequest();
                if (req != undefined) {
                    req.onreadystatechange = getComponentResponder(req, id,
                        function() {
                            if (perPollFunction && perPollFunction !== undefined) {
                                perPollFunction();
                            }
                        
                            setTimeout(function() {
                                pollComponent(id, wait, perPollFunction); }, wait);
                        });
                    req.open("GET", req_id, true);
                    req.send("");
                }
                return false;
            } else {
                return true;
            }
        }

        function requestComponent(id, param, value) {
            var req = undefined;
            var req_id = id;
            var query = "";
            
            if (id != undefined) {
                if (id.charAt(0) == '/') {
                    id = id.substring(1);
                    req_id = "$" + id;
                } else {
                    req_id = "${= page_name; =}." + id;
                }

                req = createXMLHttpRequest();
                if (req != undefined) {
                    req.onreadystatechange = getComponentResponder(req, id, null, null);
                    if (param != undefined && value != undefined) {
                    	query = "?" + param + "=" + encodeURIComponent(value);
                    }
                    req.open("GET", req_id + query, true);
                    req.send("");
                }
                return false;
            } else {
                return true;
            }
        }

        function requestComponentByName(id, compName, param, value) {
            var req = undefined;
            var req_id = compName;
            var query = "";
            
            if (compName != undefined) {
                if (compName.charAt(0) == '/') {
                    compName = compName.substring(1);
                    req_id = "$" + compName;
                } else {
                    req_id = "${= page_name; =}." + compName;
                }

                req = createXMLHttpRequest();
                if (req != undefined) {
                    req.onreadystatechange = getComponentResponder(req, id, null, null);
                    if (param != undefined && value != undefined) {
                        query = "?" + param + "=" + encodeURIComponent(value);
                    }
                    req.open("GET", req_id + query, true);
                    req.send("");
                }
                return false;
            } else {
                return true;
            }
        }

        function queryComponent(id, params) {
            var req = undefined;
            var req_id = id;
            var query = "";
            var first = true;
            
            if (id != undefined) {
                if (id.charAt(0) == '/') {
                    id = id.substring(1);
                    req_id = "$" + id;
                } else {
                    req_id = "${= page_name; =}." + id;
                }
                req = createXMLHttpRequest();
                if (req != undefined) {
                    req.onreadystatechange = getComponentResponder(req, id, null, null);
                    req.open("POST", req_id, true);
                    req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                    for (var key in params) {
                        if (!first) {
                            query = query + "&";
                        } else {
                            first = false;
                        }
                        query = query + key + "=" + params[key];
                    }
                    req.send(query);
                }
                return false;
            } else {
                return true;
            }
        }

        function queryComponentByName(id, compName, params, callback, callbackParam) {
            var req = undefined;
            var req_id = compName;
            var query = "";
            var first = true;
            
            if (compName != undefined) {
                if (compName.charAt(0) == '/') {
                    compName = compName.substring(1);
                    req_id = "$" + compName;
                } else {
                    req_id = "${= page_name; =}." + compName;
                }

                req = createXMLHttpRequest();
                if (req != undefined) {
                    req.onreadystatechange = getComponentResponder(req, id, callback, callbackParam);
                    req.open("POST", req_id, true);
                    req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                    for (var key in params) {
                        if (!first) {
                            query = query + "&";
                        } else {
                            first = false;
                        }
                        query = query + key + "=" + params[key];
                    }
                    req.send(query);
                }
                return false;
            } else {
                return true;
            }
        }

        function getComponentResponder(req, id, successFunction, successParam) {
            {= if (use_wait_cursor) [| document.body.style.cursor = 'wait'; |] =}
            return function() {
                var element;
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        var responseText = req.responseText.trim();
                        if (responseText.length > 9 && responseText.substring(0, 9) === "<!DOCTYPE") {
                            document.open("text/html", "replace");
                            document.write(responseText);
                            document.close();
                        } else {
                            element = findElement(id);
                            if (element && element !== undefined) {
                                replaceElement(element, responseText);
                            }
                            if (successFunction && successFunction !== undefined) {
                                successFunction(successParam);
                            }
                        }
                    } else {
                        console.log("Error " + req.status + ": " + req.statusText);
                    }
                    {= if (use_wait_cursor) [| document.body.style.cursor = 'default'; |] =}
                }
            }
        }

        function informServer(id, callbackFunction, callbackParam, serverParam, serverValue) {
            var query = "";
            var req_id = id;
            var req = createXMLHttpRequest();
            if (req != undefined) {
                if (id.charAt(0) == '/') {
                    id = id.substring(1);
                    req_id = "$" + id;
                } else {
                    req_id = "${= page_name; =}." + id;
                }
                req.onreadystatechange = getCallbackResponder(req, callbackFunction, callbackParam);
                if (serverParam != undefined && serverValue != undefined) {
                    // use encodeURIComponent instead of escape because the
                    // latter doesn't correctly handle plus signs
                    var escapedValue = encodeURIComponent(serverValue);
                	query = "?" + serverParam + "=" + escapedValue;
                }
                req.open("GET", req_id + query, true);
                req.send("");
            }
            return false;
        }

        function postToServer(id, callbackFunction, callbackParam, serverParams) {
            var query = "";
            var first = true;
            var req_id = id;
            var req = createXMLHttpRequest();
            if (req != undefined) {
                if (id.charAt(0) == '/') {
                    id = id.substring(1);
                    req_id = "$" + id;
                } else {
                    req_id = "${= page_name; =}." + id;
                }
                req.onreadystatechange = getCallbackResponder(req, callbackFunction, callbackParam);
                req.open("POST", req_id, true);
                req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                for (var key in serverParams) {
                    if (!first) {
                        query = query + "&";
                    } else {
                        first = false;
                    }
                    query = query + key + "=" + serverParams[key];
                }
                req.send(query);
            }
            return false;
        }

        function getCallbackResponder(req, fn, id) {
            return function() {
                if (fn != null && fn != undefined) {
                    if (req.readyState == 4) {
                        if (req.status == 200) {
                            fn(id);
                        }
                    }
                }
            }
        }
         
        function replaceElement(element, responseText) {
             var holder = document.createElement("div");
             holder.innerHTML = responseText;
             var newElement = holder.firstChild;
             var parent = element.parentNode;
             if (parent && newElement && element) {
	             parent.replaceChild(newElement, element);
	         }
         }
    |]

    javascript graphics_script [|
        function getWindowHeight() {
            var windowHeight = 0;
            if (typeof(window.innerHeight) == 'number') {
                windowHeight = window.innerHeight;

            } else if (document.documentElement && document.documentElement.clientHeight) {
                windowHeight = document.documentElement.clientHeight;

            } else if (document.body && document.body.clientHeight) {
                windowHeight = document.body.clientHeight;
            }
            return windowHeight;
        } 
    |]

    debug_script [|

    |]
}
