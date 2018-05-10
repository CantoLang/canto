/-----------------------------------------------------------------------------
 --  core_ui.fun                                                          --
 --                                                                         --
 --  Core user interface definitions for Fun.                             --
 --                                                                         --
 --  Copyright (c) 2006-2012 by fundev.org.  All rights reserved.         --
 --                                                                         --
 -----------------------------------------------------------------------------/

core {

    /** Construct standard styles, given a configuration table. **/
    core_styles(options{}) {
       
        if (options["drag"]) {
            vlog("dragging enabled, adding handle styles");
            seldeco.style;
        }
        
     
    }


    /----------- components --------------/

    component {
        color bgcolor [/]
        int width = -1
        int height = -1
        align [/]
        valign [/]
        style [/]
        boolean disabled = false

        /** The outer tag defining the component.  By default, it is div. **/
        outer_tag = "div"

        /** A component's id is used by css and javascript to identify a component.  The default
         *  value is to use the component's type name.  This may be overridden.  For a component 
         *  to be scriptable, id must have a unique (in the current DOM document) non-null value.
         **/
        dynamic id = owner.type;
              
        /** A component's class is is used by css and javascript to identify a category of 
         *  components.  The default value is "component".  This may be overridden.
         **/
        component_class = "component"
        
        /** If resubmit_delay is overridden with a nonzero value, this component will automatically
         *  resubmit a refresh request to the server every resubmit_delay milliseconds.
         **/
        int resubmit_delay [/]

        /** For additional attributes in the component's outer tag, add elements to this array.  They should be of the
         *  form attrib="value"
         **/
        tag_attribs[] = []

        [| <{= outer_tag; =} class="{= component_class; =}" |]
        sp;
        if (id) [| id="{= id; =}" |]
        sp;
        if (style || bgcolor) {
            [| style=" |]
            if (bgcolor) [| background-color: {= bgcolor; =}; |]
            style;
            [| " |]
        }
        if (tag_attribs) {
            for a in tag_attribs {
                if (a) {
                    sp;
                    a;
                }
            }
        }
        [| > |]
        sub; 
        [| </{= outer_tag; =}> |]   
    }

    
    component, draggable drag_handle {
        int left = 0
        int top = 0
    
        style [|
            #{= owner.id; =} {
                position: absolute;
                left: {= left; =}px; top: {= top; =}px;
                width: 7px; height: 7px; 
                background-color: #000000;
                opacity: 1.0;
            }
         |]
         
         purpose = "resize"
         direction [?]      
        
         script {
             dx_filter_script;
             dy_filter_script;
         } 
         
         dx_filter_script [| 
             {= owner.id; =}.dx_filter = function(dx) {
                                             return dx;
                                         }
         |]  
         
         dy_filter_script [|
             {= owner.id; =}.dx_filter = function(dx) {
                                             return dx;
                                         }
         |]
    }

    component, draggable, resizable seldeco(),(component target) {
        id = "selection_decoration"
        int x { with (target) { (target.x - 4); } else { 0; } }
        int y { with (target) { (target.y - 4); } else { 0; } }
        int width { with (target) { (target.width + 8); } else { 7; } }
        int height { with (target) { (target.height + 8); } else { 7; } }
           
        style {
            [|
                #{= id; =} {
                    position: absolute;
                    left: {= x; =}px; top: {= y; =}px;
                    width: {= width; =}px; height: {= height; =}px; 
                }
            |]

            /** add handle styles **/
            nw_handle.style;
            n_handle.style;
            ne_handle.style;
            w_handle.style;
            e_handle.style;
            sw_handle.style;
            s_handle.style;
            se_handle.style;
        }
        
        script {
            /** add handle scripts **/
            nw_handle.script;
            n_handle.script;
            ne_handle.script;
            w_handle.script;
            e_handle.script;
            sw_handle.script;
            s_handle.script;
            se_handle.script;
        }
        
        drag_handle nw_handle {
            int left = 0
            int top = 0
            direction = "nw"
            dynamic style = drag_handle.style
        }
    
        drag_handle n_handle {
            int left = (width - 7) / 2
            int top = 0
            direction = "n"
            style = drag_handle.style
        }

        drag_handle ne_handle {
            int left = width - 7
            int top = 0
            direction = "ne"
            style = drag_handle.style
        }
    
        drag_handle w_handle {
            int left = 0
            int top = (height - 7) / 2
            direction = "w"
            style = drag_handle.style
        }
    
        drag_handle e_handle {
            int left = width - 7
            int top = (height - 7) / 2
            direction = "e"
            style = drag_handle.style
        }

        drag_handle sw_handle {
            int left = 0
            int top = height - 7
            direction = "sw"
            style = drag_handle.style
        }
    
        drag_handle s_handle {
            int left = (width - 7) / 2
            int top = height - 7
            direction = "s"
            style = drag_handle.style
        }

        drag_handle se_handle {
            int left = width - 7
            int top = height - 7
            direction = "se"
            style = drag_handle.style
        }
        
        nw_handle;
        n_handle;
        ne_handle;
        w_handle;
        e_handle;
        sw_handle;
        s_handle;
        se_handle;
    }
    
    
    component default_component(contents) {
        contents;
    }

    clickable {
        /** The onclick event occurs when the pointing device button is clicked over an element. **/
        onclick [/]

        /** The ondblclick event occurs when the pointing device button is double clicked over an element. **/
        ondblclick [/]

        /** The onmousedown event occurs when the pointing device button is pressed over an element. **/
        onmousedown [/]

        /** The onmouseup event occurs when the pointing device button is released over an element. **/
        onmouseup [/]

        /** The onmouseover event occurs when the pointing device is moved onto an element. **/
        onmouseover [/]

        /** The onmouseout event occurs when the pointing device is moved away from an element. **/
        onmouseout [/]
    }
    
    selectable {
        /** The onmousedown event occurs when the pointing device button is pressed over an element. **/
        onmousedown [/]
    }
    
    draggable {
        /** The onmousemove event occurs when the pointing device is moved while it is over an element. **/
        onmousemove [/]
    }

    resizable {
        /** The onmousemove event occurs when the pointing device is moved while it is over an element. **/
        onmousemove [/]
    }

    selectable focusable {
        /** The onfocus event occurs when an element receives focus either by the pointing device or by tabbing 
         *  navigation. This attribute may be used with the following elements: A, AREA, LABEL, INPUT, SELECT, 
         *  TEXTAREA, and BUTTON.
         **/
        onfocus [/]

        /** The onblur event occurs when an element loses focus either by the pointing device or by tabbing 
         *  navigation. It may be used with the same elements as onfocus.
         **/
        onblur [/]
    }
    
    keyable {
        /** The onkeypress event occurs when a key is pressed and released over an element. **/
        onkeypress [/]

        /** The onkeydown event occurs when a key is pressed down over an element. **/
        onkeydown [/]

        /** The onkeyup event occurs when a key is released over an element. **/
        onkeyup [/]
    }
    
    editable {
        /** The onselect event occurs when a user selects some text in a text field. This attribute may be used 
         *  with the INPUT and TEXTAREA elements.
         **/
        onselect [/]

        /** The onchange event occurs when a control loses the input focus and its value has been modified since 
         *  gaining focus. This attribute applies to the following elements: INPUT, SELECT, and TEXTAREA.
         **/
        onchange [/]
    }


    /----------- layout control --------------/

    layout {
        table_props [/]

        dynamic cell_props(p), (component c) {
            with (c) {
                if (c.width >= 0) [/ width="{= c.width; =}" |]
                if (c.height >= 0) [/ height="{= c.height; =}" |]
                if (c.bgcolor) [/ bgcolor="{= c.bgcolor; =}" |]
                if (c.align) [/ align="{= c.align; =}" |]
                if (c.valign) [/ valign="{= c.valign; =}" |]
            } else {
                /-- cell_props called on non-component; does nothing --/
            }
        }

        dynamic style_props(p), (component c) {
            with (c) {
                if (c.style) {
                    c.style;
                } else {
                    if (c.width >= 0) [/ width: {= c.width; =}px; |]
                    if (c.height >= 0) [/ height: {= c.height; =}px; |]
                    if (c.bgcolor) [/ background-color: {= c.bgcolor; =}; |]
                    if (c.align) [/ text-align: {= c.align; =}; |]
                    if (c.valign) [/ vertical-align: {= c.valign; =}; |]
                }
            } else {
                /-- cell_props called on non-component; does nothing --/
            }
        }
    }


    dynamic layout border_layout(c), (n, c, s), (n, w, c, e, s), (x[]), (comp_table{}) {

        dynamic center_if_not_aligned(p), (component c) {
            with (c) { if (!c.align) [/ align="center" |] }
            else [/ align="center" |]         
		}            

        dynamic north(panel) [|
            <table{= table_props; =}>{=
                if (panel) [|
                    <tr><td colspan="3"{= cell_props(panel); center_if_not_aligned(panel); =}>{= panel; =}</td></tr>
                |]
            =}
        |]
        dynamic west(panel) [|
            <tr><td{= cell_props(panel); =}>{= panel; =}</td>
        |]
        dynamic center(panel) [|
            <td{= cell_props(panel); =}>{= panel; =}</td>
        |]
        dynamic east(panel) {
            if (panel) [|
                <td{= cell_props(panel); =}>{= panel; =}</td>
            |]
            [| </tr>  |]
        }
        dynamic south(panel) {
            if (panel) [|
                <tr><td colspan="3"{= cell_props(panel); center_if_not_aligned(panel); =}>{= panel; =}</td></tr>
            |]
            [| </table> |]
        }

        with (x) {
            if (x.count < 2) {
                north;
                center(x[0]);
                south;
            } else if (x.count < 4) {
                north(x[0]);
                center(x[1]);
                if (x.count == 3) {
                    south(x[2]);
                } else {
                    south;
                }
            } else {
                north(x[0]);
                west(x[1]);
                center(x[2]);
                if (x.count == 4) {
                    south(x[3]);
                } else {
                    east(x[3]);
                    south(x[4]);
                }
            }
        } else with (comp_table) {
            north(comp_table["north"]);
            west(comp_table["west"]);
            center(comp_table["center"]);
            east(comp_table["east"]);
            south(comp_table["south"]);
            
        } else {
            north(n);
            west(w);
            center(c);
            east(e);
            south(s);
        }
    }


    dynamic border_layout(n, w, c, e, s) compass_layout(nw, n, ne, w, c, e, sw, s, se), (comp_table{}) {
        dynamic northwest(panel) {
            [| <table> |]
            west(panel);
        }
        dynamic north(panel) = center(panel)
        dynamic northeast(panel) = east(panel)
        dynamic southwest(panel) = west(panel)
        dynamic south(panel) = center(panel)
        dynamic southeast(panel) {
            east(panel);
            [| </table> |]
        }

        with (comp_table) {
            northwest(comp_table["nw"]);
            north(comp_table["n"]);
            northeast(comp_table["ne"]);
            west(comp_table["w"]);
            center(comp_table["c"]);
            east(comp_table["e"]);
            southwest(comp_table["sw"]);
            south(comp_table["s"]);
            southeast(comp_table["se"]);

        } else {
            northwest(nw);
            north(n);
            northeast(ne);
            west(w);
            center(c);
            east(e);
            southwest(sw);
            south(s);
            southeast(se);
        }
    }


    dynamic layout stage_layout(c), (w, c, e), (w, n, c, s, e), (x[]), (comp_table{}) {
        dynamic west(panel) [|
            <td rowspan="3" {= cell_props(panel); =}>{= panel; =}</td>
        |]
        dynamic north(panel) [|
            <td {= cell_props(panel); =}>{= panel; =}</td>
        |]
        dynamic center(panel) [|
            <td {= cell_props(panel); =}>{= panel; =}</td>
        |]
        dynamic south(panel) [|
            <td {= cell_props(panel); =}>{= panel; =}</td>
        |]
        dynamic east(panel) [|
            <td rowspan= "3" {= cell_props(panel); =}>{= panel; =}</td>
        |]
        [| <table {= table_props; =} ><tr> |]
        with (x) {
            if (x.count < 2) {
                center(x[0]);
            } else if (x.count < 4) {
                west(x[0]);
                center(x[1]);
                if (x.count == 3) {
                    east(x[2]);
                }
            } else {
                west(x[0]);
                north(x[1]);
                if (x.count > 4) {
                    east(x[4]);
                }
                [| </tr><tr> |]
                center(x[2]);
                [| </tr><tr> |]
                south(x[3]);
            }

        } else with (comp_table) {
            west(comp_table["west"]);
            north(comp_table["north"]);
            east(comp_table["east"]);
            [| </tr><tr> |]
            center(comp_table["center"]);
            [| </tr><tr> |]
            south(comp_table["south"]);

        } else {
            west(w);
            north(n);
            east(e);
            [| </tr><tr> |]
            center(c);
            [| </tr><tr> |]
            south(s);
        }
        [| </tr></table> |]
    }


    dynamic border_layout(*) css_border_layout(c), (n, c, s), (n, w, c, e, s), (x[]), (comp_table{}) {

        dynamic north(panel) [|
            <div style="{= style_props(panel); =}">
            {= panel; =}
            </div>
        |]
        dynamic west(panel) [|
            <div style="float: left; {= style_props(panel); =}">
            {= panel; =}
            </div>
        |]
        dynamic center(panel) [|
            <div style="{= style_props(panel); =}">
            {= panel; =}
            </div>
        |]
        dynamic east(panel) [|
            <div style="float: right; {= style_props(panel); =}">
            {= panel; =}
            </div>
        |]
        dynamic south(panel) [|
            <div style="clear: both; {= style_props(panel); =}">
            {= panel; =}
            </div>
        |]
    }


    /--------------- components --------------/

    standard_control {
        dynamic id [/]
    }


    dynamic standard_control hidden(name, value) [|
        <input type="hidden" id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" value="{= value; =}" />
    |]


    dynamic standard_control textarea(name, value, int cols, int rows, req_id),
                                     (name, value, int cols, int rows),
                                     (name, value, req_id),
                                     (name, value) [|
        <textarea id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" {= 
            if (cols) [| cols="{= cols; =}" /]
            if (rows) [| rows="{= rows; =}" /] 
            with (req_id) [| onchange='requestComponent("{= req_id; =}", this.name, this.value)' |]
        =}>{= value; =}</textarea>
    |]


    dynamic standard_control textedit(name, value, int size, int maxlength, req_id),
                                     (name, value, int size, int maxlength),
                                     (name, value, int size, boolean disabled),
                                     (name, value, int size),
                                     (name, value) [|
        <input type="text" id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" value="{= value; =}" {=
            with (size) [| size="{= size; =}" |] with (maxlength) [/ maxlength="{= maxlength; =}" |]
            with (req_id) [/ onchange='requestComponent("{= req_id; =}", this.name, this.value)' |]
            if (disabled) [/ disabled |]
        =}>
    |]


    dynamic standard_control autosubmit_textedit(form_name, name, value, int size, int maxlength),
                                                (form_name, name, value, int size),
                                                (form_name, name, value) [|
        <input type="text" id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" value="{= value; =}" {=
            with (size) [| size="{= size; =}" |] with (maxlength) [/ maxlength="{= maxlength; =}" |]
        =} onchange="document.forms['{= form_name; =}'].submit()" />
    |]


    dynamic standard_control scripted_textedit(name, value, int size, int maxlength, onchange_script),
                                              (name, value, int size, int maxlength, onchange_script),
                                              (name, value, int size, onchange_script),
                                              (name, value, onchange_script) [|
        <input type="text" id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" value="{= value; =}" {=
            with (size) [| size="{= size; =}" |] with (maxlength) [/ maxlength="{= maxlength; =}" |]
        =} onchange='{= onchange_script; =}' oninput='{= onchange_script; =}'>
    |]


    dynamic standard_control request_textedit(name, value, int size, req_id),
                                             (name, value, int size, component req_component),
                                             (name, value, req_id),
                                             (name, value, component req_component) {
   
        [| <input type="text" id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" value="{= value; =}" |]
        with (size) [| size="{= size; =}" |]
        with (req_id) [/ onchange="requestComponent('{= req_id; =}', this.name, this.value)" |]
        else with (req_component) [/ onchange="requestComponent('{= req_component.id; =}', this.name, this.value)" |]
        [| /> |]
    }


    dynamic standard_control passwordedit(name, value, int size, int maxlength),
                                         (name, value, int size) [|
        <input type="password" id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" value="{= value; =}" {=
            with (size) [| size="{= size; =}" |] with (maxlength) [| maxlength="{= maxlength; =}" |]
        =}>
    |]


    dynamic standard_control autosubmit_passwordedit(form_name, name, value, int size, int maxlength),
                                                    (form_name, name, value, int size),
                                                    (form_name, name, value) [|
        <input type="password" id="{= (id ? id : name); =}" class="{= owner.type; =}" name="{= name; =}" value="{= value; =}" {=
            with (size) [| size="{= size; =}" |] with (maxlength) [/ maxlength="{= maxlength; =}" |]
        =} onchange="document.forms['{= form_name; =}'].submit()" />
    |]


    dynamic standard_control button(name, value, component req_component, onclick_script),
                                   (name, value, req_id, onclick_script),
                                   (name, value, component req_component),
                                   (name, value, req_id),
                                   (name, value) {

        [| <input name="{= name; =}" value="{= value; =}" id="{= name; =}_button" class="{= owner.type; =}" |]
        with (onclick_script)     [/ type="button" onclick='{= onclick_script; =}' |]
        else with (req_id)        [/ type="button" onclick="refreshComponent('{= req_id; =}')" |]
        else with (req_component) [/ type="button" onclick="refreshComponent('{= req_component.id; =}')" |]
        else                      [/ type="submit" |]
        [| /> |]
    }


    dynamic standard_control request_button(name, value, onclick_script),
                                           (name, value, req_id, param, paramval),
                                           (name, value, component req_component, param, paramval),
                                           (name, value, element_id, component req_component, param, paramval) {

        [| <input type="button" name="{= name; =}" value="{= value; =}" id="{= name; =}_button" class="{= owner.type; =}" |]
        with (onclick_script) [/ onclick='{= onclick_script; =}' |]
        else with (element_id) [/ onclick="requestComponentByName('{= element_id; =}', '{= req_component.name; =}', '{= param; =}', '{= paramval; =}')" |]
        else with (req_id) [/ onclick="requestComponent('{= req_id; =}', '{= param; =}', '{= paramval; =}')" |]
        else with (req_component) [/ onclick="requestComponent('{= req_component.id; =}', '{= param; =}', '{= paramval; =}')" |]
        [| /> |]
    }


    dynamic standard_control query_button(name, value, element_id, component req_component, params[]),
                                         (name, value, element_id, req_id, params[]) {

        [| <input type='button' name='{= name; =}' value='{= value; =}' id='{= name; =}_button' class=''{= owner.type; =}' onclick=' |]

        with (req_component) [| queryComponentByName("{= element_id; =}", "{= req_component.name; =}", {= params; =}, null, null)'/> |]
        else                 [| queryComponentByName("{= element_id; =}", "{= req_id; =}", {= params; =}, null, null)'/> |]
    }


    dynamic standard_control submit_button(name, value, component req_component, field_ids[], params{}),
                                          (name, value, req_id, field_ids[], params{}),
                                          (name, value, component req_component, field_ids[]),
                                          (name, value, req_id, field_ids[]),
                                          (name, value, component req_component, field_id),
                                          (name, value, req_id, field_id),
                                          (name, value, req_id),
                                          (name, value) {

        submit_script(id) {
            query_params{} = params

            if (field_id) [|
                requestComponent("{= id; =}", "{= field_id; =}", document.getElementById("{= field_id; =}").value);
            |] else with (field_ids) [|
                var params = {};
                {=
                    with (params) { 
                        for k in query_params.keys [|
                            params["{= k; =}"] = "{= query_params[k]; =}";
                        |]
                    }
                =}
                {= for f in field_ids [|
                params["{= f; =}"] = encodeURIComponent(document.getElementById("{= f; =}").value);
                |] =}  
                queryComponent("{= id; =}", params);
            |] else [|
                refreshComponent("{= id; =}");
            |]
            [|
                return true;
            |]
        }

        [| <input type="submit" name="{= name;=}" value="{= value; =}" class="{= owner.type; =}" id="{= name; =}_button" {=
        with (req_id)             [| onclick='{= submit_script(req_id); =}' {= log("req_id " + req_id); =} |]
        else with (req_component) [| onclick='{= submit_script(req_component.id); =}' {= log("req_component.id " + req_component.id); =} |]
        =}/> |]
    }


    dynamic standard_control disabled_button(name, value) [|
        <input type="submit" name="{= name; =}" value="{= value; =}" class="{= owner.type; =}" id="{= name; =}_button" disabled />
    |]


    dynamic standard_control checkbox(name, label, boolean checked),
                                     (name, label, boolean checked, value),
                                     (name, label, boolean checked, value, onclick_script)  [|
        <input type="checkbox" name="{= name; =}" class="{= owner.type; =}" id="{= name; =}_checkbox" {=
            with (value) [| value="{= value; =}" |]
            if (checked) [/ checked |]
            with (onclick_script) [/ onclick='{= onclick_script; =}' |]
        =} >{= label; =}</input>
    |]


    dynamic standard_control radiobutton(groupname, label, boolean checked, value),
                                        (groupname, label, boolean checked, value, onclick_script) [|
        <input type="radio" name="{= groupname; =}" value="{= value; =}"{=
            if (value) [/ id="{= groupname; =}_{= value; =}_radiobutton" class="{= groupname; =}_radiobutton" |]
            if (checked) [/ checked |]
            with (onclick_script) [/ onclick='{= onclick_script; =}' |]
        =} >{= label; =}</input>
    |]


    dynamic standard_control dropdown(id, name, option[] options, onchange_script),
                                     (name, option[] options, onchange_script),
                                     (name, option[] options, boolean disabled),
                                     (name, option[] options) [|
        <select name="{= name; =}" class="{= owner.type; =}" id="{= (id ? id : name + "_dropdown"); =}"{=
            with (onchange_script) [/ onchange='{= onchange_script; =}' |] 
            if (disabled) [/ disabled |] 
            [| > |]
            for option o in options {
                o;
            }
        =}</select>
    |]


    dynamic option(name, value, boolean selected) [|
        <option value="{= value; =}" {= if (selected) [| selected="selected" |] =}>{= name; =}</option>    
    |]


    dynamic form(action),(page action_page) {
        /** The onsubmit event occurs when a form is submitted. It only applies to the FORM element. **/
        onsubmit [/]

        /** The onreset event occurs when a form is reset. It only applies to the FORM element. **/
        onreset [/]

        [| <form name="|] type; [|" action=" |]
        with (action) { action; }
        else { action_page.type; }
        [| " method="POST |]
        if (onsubmit) [| " onsubmit="{= onsubmit; =} |]
        if (onreset) [| " onreset="{= onreset; =} |]
        [| "> |]
        sub;
        [| </form> |]
    }
}
