/-----------------------------------------------------------------------------
 --  Fun Core
 --  
 --  $Id: core.fun,v 1.24 2015/04/23 16:52:16 sthippo Exp $                                                             --
 --                                                                         --
 --  This file contains core definitions -- ones that are broadly useful,   --
 --  needed by many applications and therefore worth making accessible to   --
 --  all by default.  In many languages this takes the form of standard     --
 --  libraries.  The equivalent of a library in Fun is a site that        --
 --  you adopt.                                                             --
 --                                                                         --
 --  core is also a fun keyword, denoting the root of a site's            --
 --  definition tree.  For a site to access another site, the two sites     --
 --  must share the same definition tree.                                   -- 
 --                                                                         --
 --  core contains the following:                                           --
 --      -- language metainformation (version number)                       --
 --      -- global system properties, settings and configuration            -- 
 --      -- reflection api                                                  --
 --      -- database api                                                    --
 --      -- standard page definition                                        --
 --      -- user interface definitions (components, forms, layouts)         --
 --      -- DHTML scripts and styles                                        --
 --      -- Ajax support                                                    --
 --      -- interfaces to other languages                                   --
 --      -- string manipulation                                             --
 --      -- file i/o                                                        --
 --      -- math functions                                                  --
 --      -- convenience functions                                           --
 --                                                                         --
 --  Some of the above categories are in their own files, such as core_ui   --
 --  (containing user interface definitions) and core_js (containing        --
 --  JavaScript for DHTML and Ajax).                                        --
 --                                                                         --
 -----------------------------------------------------------------------------/

core {

    /--- version ---/
    version = this_server.version


    /--- constants for runtime options ---/

    /** Verbosity setting to minimize the information displayed on the console. **/
    int TERSE = 0

    /** Middle setting for verbosity; some but not all information is displayed on the console. **/
    int MODERATE = 1

    /** Verbosity setting to output all available information to the console. **/
    int VERBOSE = 2

    /--- wrapper for site configuration settings ---/
    
    /** A generic website configuration. **/
    site_config {
        name [?]

        funpath [?]
                
        boolean recursive = false;

        filepath = "."
        boolean files_first = false;
        
        /** If non-empty, respond only to the listed addresses.  The addresses are
         *  allowed, but are not automatically enabled; addresses must also appear in 
         *  the listen_to array.  If empty, respond to any address.
         **/
        respond_only_to[] = []  
        
    }
    
    /--- runtime system reflection and identification ---/
    
    /** Abstract Fun processor.  A Fun processor is capable of parsing Fun source
     *  code and creating fun_site objects.
     */
    fun_processor {
        name [?]
        version [?]
        props{} = {}
        
        /** Compile Fun source code and return a fun_domain object. **/
        fun_domain compile(funpath, boolean recursive, boolean autoload_core),
                            (fun_source_text, boolean autoload_core) [&]

        /** Returns the domain type.  The default for the primary domain is "site". **/
        domain_type [?]
    }


    fun_processor fun_server {

        /** The URL prefix that this server uses to filter requests.  This allows an
         *  HTTP server to dispatch requests among multiple Fun servers, using the
         *  first part of the URL to differentiate among them.
         *
         *  If null, this server accepts all requests.
         */
        base_url [/]

        /** The base directory on the local system where this server accesses data files.
         *  File names in requests are relative to this directory.
         */
        file_base [/]

        /** A table associating paths in the request to specific sites.  By default, a site's
         *  path is simply the site's name, except for the main site, whose path is an empty
         *  string.  If, however, there is a string in this table associated with a site's name,
         *  that string is used as the path for requests to that site.
         */
        site_paths{} = {}

        /** Returns true if the server was successfully started and has not yet
         *  been stopped.
         **/
        dynamic boolean is_running [/] 

        /** Requests data from the server. **/
        dynamic get(requestName, requestParams{}) [/]

        /-- support for launching of fun_servers --/

        /** Launches a new fun_server, initialized with the passed parameters, 
         *  unless a server with the passed name has been launched already by this
         *  server.  (To launch a new server even if it has already been launched,
         *  use relaunch_server.)
         **/
        dynamic fun_server launch_server(name, params{}) [/]
        
        /** Launches a new fun_server, initialized with the passed parameters, after
         *  stopping any previously launched server with the passed name.
         */
        dynamic fun_server relaunch_server(name, params{}) [/]
    
        /** Returns the fun_server with the specified name that was launched by this server, or null if
         *  no such fun_server exists.
         **/
        dynamic fun_server get_server(name) [/] 
    }
  
    fun_domain {
        fun_site{} sites = {}
        fun_domain{} domains = {}
        main_site [?]
        name [?]
        
        dynamic get(expr) [?]
        dynamic definition get_definition(expr) [?]
        dynamic get_instance(expr) [?]
        dynamic get_array(expr)[] = []
        dynamic get_table(expr){} = {}

        fun_context context [&]
        fun_domain child_domain(name),
                                 (name, src, boolean is_url),
                                 (name, path, filter, boolean recursive) [&]
    }

    /** A context for constructing objects **/
    fun_context {

        /** The name of the current site **/
        site_name [?]
         
        /-- functions to retrieve cached data, or instantiate if not cached --/

        /** Gets the cached value (if any) associated with the given name in this context. **/
        dynamic get(name), (definition d, arg[] args) [&]

        /** Modifies the cached value (if any) associated with the given name in this context. **/
        put(name, data) [&]

        /** Constructs the object with the given name in this context. **/
        construct(name),(name, arg[] args) [&]
        
        /** Returns the context associated with the container of the current context. **/
        fun_context container_context [&]
    }

    fun_domain complex_domain {
        fun_domain[] domains = []
    }

 
    /** A Fun statement; base class for the various types of statements. **/
    fun_node {
        fun_node[] children = []
    }

   /** Interface for statements that include names. **/
    named {
        name [/]
    }

    /** Interface for statements that define types. **/
    typed {
        named[] super_type = []
    }

    named param [/]
    param,typed typed_param [/]
    param[] param_list = []
    
    construction arg [/]

    /** Interface for owners of child constructions. **/
    construction_owner {
        construction[] constructions = []
    }

    parameterized {
        param_list[] param_lists = []
    }    

    /** Interface for constructions. **/
    construction {
        reference [/]
        
        arg[] args = []
        value(fun_context context) [?]
    }

    /** Interface for owners of child definitions. **/
    definition_owner {
        definition[] defs = [&]
        definition[] children_of_type(type_name) = [&]
        definition[] descendants_of_type(type_name) = [&]
    }
   
    fun_node,
    named,
    parameterized,
    construction_owner,
    definition_owner definition {
        full_name [&]
        definition ancestor_of_type(type_name) [&]

        /-- determine whether this is a collection and what kind it is --/
        boolean is_collection = (is_array || is_table)
        boolean is_array [&]
        boolean is_table [&]
        boolean is_a(type_name) [&]

        /-- instantiate this definition as an object, an array or a table --/
        dynamic instantiate(arg[] args) [&] 
        dynamic instantiate_array[] = []
        dynamic instantiate_table{} = {}
        fun_context owner_context [?]
    }

    definition fun_site {
        definition definition_of_type(type_name) [&]
    }

    /** object serialization **/

    deserializable(*) serializable(str),(field_names[], field_values[]) {
 
        dynamic serialize(child_names[]) {

            dynamic handle_def(definition d) {
                t{} = d.get_table

                if (d isa serializable) {
                    d.serialize;

                } else if (d.is_array) {
                    "[ ";
                    for item in d.get_array and int i from 0 {
                        if (i > 0) {
                            ", ";
                        }

                        if (item isa definition) {
                            handle_def(item);
                        } else {
                            "\"";
                            item;
                            "\"";
                        }
                    }
                    " ]";

                } else if (d.is_table) {
                    "{ ";
                    for k in t.keys and int i from 0 {
                        if (i > 0) {
                            ", ";
                        }
                        k;
                        if (t[k] isa definition) {
                            ".keep: ";
                            handle_def(t[k]);
                        } else if (t[k] isa number) {
                            t[k];                        
                        } else {
                            ": \"";
                            t[k];                        
                            "\"";
                        }
                    }
                    " }";

                } else {                    
                    "\"";
                    d.get;
                    "\"";
                }
            }
            

            /--- serialize this object ---/

            "{ ";
            with (child_names) {
                for c in child_names and int i from 0 {
                    if (i > 0) {
                        ", ";
                    }
                    c;
                    if (owner.defs[c] isa serializable) {
                        ".keep";
                    }
                    ": ";
                    handle_def(owner.defs[c]);
                }
            } else {
                for definition df in owner.keep_defs and int i from 0 {
                    if (i > 0) {
                        ", ";
                    }
                    df.name;
                    if (df isa serializable) {
                        ".keep";
                    }
                    ": ";
                    handle_def(df);
                }
            }
            " }\n";
        }
    }        
     
     
    deserializable(str),(field_names[], field_values[]) {
        if (str) {
            log("deserializing " + type + " from string \"" + str + "\"");    
            deserialized_obj{} = table.parse(str)
            for k in deserialized_obj.keys {
                here.put(k, deserialized_obj[k]);
            }
        } else if (field_names) {
            log("deserializing " + type + " from field names and values");
            for nm in field_names and val in field_values {
                log("  ....field name: " + nm + "  value: " + val); 
                here.put(nm, val);
            }
        } else with (str) {
            log("deserializing " + type + " from string, but string is null");    
        } else with (field_names) {
            log("deserializing " + type + " from field names, but field name array is null");
        } else {
            log("deserializing " + type + ", but no arguments passed");
        }
        sub;
    }


    /** A context stack.  **/
    fun_continuation {
        entry {
            definition defn [/]
            param[] params = []
            arg[] args = []
            entry prev [/]
        }

        entry root_entry [/]
        entry top_entry [/]
    }

    /--- general utility functions ---/

    /** Evaluates the passed reference silently and returns null. **/
    dynamic eval(x) {
        /--  if an implementation optimizes away empty conditionals then another
             mechanism to force evaluation will be needed here. --/
        if (x) [/]
    }

    /** Returns the maximum of two values. **/
    dynamic max(a, b) = (a >= b ? a : b)

    /** Returns the minimum of two values. **/
    dynamic min(a, b) = (a <= b ? a : b)

    /** If the first argument is non-null, return it, else return the second argument. **/
    dynamic either(a, b) = (a ? a : b)

    /** standard basic geometric types **/
    
    point(point p),
         (float fx, float fy),(int ix, int iy),
         (float fx, float fy, float fz),(int ix, int iy, int iz) {

        number x { with (p) { p.x; } else with (fx) { fx; } else { ix; } }
        number y { with (p) { p.y; } else with (fy) { fy; } else { iy; } }
        number z { with (p) { p.z; } else with (fz) { fz; } else with (iz) { iz; } }
        
        ( "(" + x + "," + y + "," + z + ")" );
    }
    
    dim(dim d),
       (float fwid, float fht),(int iwid, int iht),
       (float fwid, float fdpth, float fht),(int iwid, int idpth, int iht) {

        number width  { with (d) { p.width;  } else with (fwid)  { fwid;  } else { iwid; } }
        number height { with (d) { p.height; } else with (fht)   { fht;   } else { iht; } }
        number depth  { with (d) { p.depth;  } else with (fdpth) { fdpth; } else with (idpth) { idpth; } }
        
        ( "(w: " + width + "  h: " + height + "  d: " + depth + ")" );
    }
 
    rect(rect r),
        (float fx1, float fy1, float fx2, float fy2),
        (int ix1, int iy1, int ix2, int iy2) {
                
        number x1 { with (r) { r.x1; } else with (fx1) { min(fx1, fx2); } else { min(ix1, ix2); } }
        number y1 { with (r) { r.y1; } else with (fy1) { min(fy1, fy2); } else { min(iy1, iy2); } }
        number x2 { with (r) { r.x2; } else with (fx2) { max(fx1, fx2); } else { max(ix1, ix2); } }
        number y2 { with (r) { r.y2; } else with (fy2) { max(fy1, fy2); } else { max(iy1, iy2); } }

        number width = (x2 - x1);
        number height = (y2 - y1);
        
        is_inside(point pt) = (x1 <= pt.x && x2 >= pt.x && y1 <= pt.y && y2 >= pt.y)
    }

    cuboid(cuboid c),
          (float fx1, float fy1, float fz1, float fx2, float fy2, float fz2),
          (int ix1, int iy1, int iz1, int ix2, int iy2, int iz2) {
        number x1 { with (c) { c.x1; } else with (fx1) { min(fx1, fx2); } else { min(ix1, ix2); } }
        number y1 { with (c) { c.y1; } else with (fy1) { min(fy1, fy2); } else { min(iy1, iy2); } }
        number z1 { with (c) { c.z1; } else with (fz1) { min(fz1, fz2); } else { min(iz1, iz2); } }
        number x2 { with (c) { c.x2; } else with (fx2) { max(fx1, fx2); } else { max(ix1, ix2); } }
        number y2 { with (c) { c.y2; } else with (fy2) { max(fy1, fy2); } else { max(iy1, iy2); } }
        number z2 { with (c) { c.z2; } else with (fz2) { max(fz1, fz2); } else { max(iz1, iz2); } }

        number width = (x2 - x1);
        number depth = (y2 - y1);
        number height = (z2 - z1);

        is_inside(point pt) = (x1 <= pt.x && x2 >= pt.x && y1 <= pt.y && y2 >= pt.y && z1 <= pt.z && z2 >= pt.z)
    }
        
        


    /** Concatenates two strings, adding a file separator if necessary **/
    dynamic meld_paths(path1, path2) {
        path1;
        if (char_at(path1, strlen(path1) - 1) != file_sep && char_at(path2, 0) != file_sep) {
            file_sep;
        }
        trim_leading(path2, (string) file_sep);
    }


    /--------- platform-independent superclasses of platform-dependent types ---------/

    /** request describes an HTTP request. This is an optional parameter passed to
     *  pages when they are instantiated by the Fun server.
     */
    request {

        /** the URL for the request (not including the query string) */
        url [/]

        /** the raw path (URL minus server context and query string) **/
        path_info [/]
        
        /** the translated path **/
        translated_path [/]
        
        /** the unparsed query string */
        query [/]

        /** a table containing the HTTP headers */
        headers{} = {}

        /** a table containing the parameters, either parsed from the query
         *  string or extracted from the posted form data.
         */
        params{} = {}

        /** a table containing any cookies passed by the client. */
        cookies{} = {}

        /** the HTTP method used to sent this request, either "GET" or "POST" */
        method [?]

        /** redirects the response to this request to the indicated url (same as
         *  redirect statement)
         */
        redirectTo(url) [?]

    }

    session {
        id [?]

        attributes{} = {}

        long created = 0

        int accessed = 0

        int max_inactive = -1
    }

    response {
        int status = 200
    }
    
    response server_response {=
    
    =}

    /--------------- Runtime management ---------------/

    /** init is instantiated by the servlet at startup.  It is declared
     *  static to ensure that it is only evaluated once.  To re-initialize,
     *  call reinit.
     */
    static init {
        reinit;
    }


    dynamic reinit {
        log("Initializing core, Fun version " + version);
    }


    /--------------- Database definitions ---------------/

    static boolean db_enabled(driver, url, user, password) = database(driver, url, user, password).enabled

    /** The database interface definition.  The concrete database is defined in
     *  the platform-dependent core definition file (e.g. core_platform_java.fun).
     */
    database_interface {
        driver [/]
        url [/]
        user_name [/]
        password [/]
        
        /------- initialization -------/
        boolean init [?]
        boolean enabled [?]
        
        /------- Fun/database table mapping --------/
        string table(record, key){} = {}        

        /------- direct sql functions -------/
        db_row[] query(sql),(sql, values[]) = []
        result_set execute_query(sql, values[]) [?]
        int execute_update(sql, values[]) [?]
        int execute(sql) [?]
        execute_batch(sqls[]) [?]
    }


    /** The base database for sites to subclass.  The superclass, db_impl, is a concrete
     *  database implementation defined in the platform-dependent core definition file
     *  (e.g. core_platform_java.fun), and is a subclass of database_interface.
     */
    db_impl(driver, url, user, password) database(driver, url, user, password) {

        super;

        /--- initialize the database ---/
        if (init) {
            log("Database at " + url + " initialized for user " + user + ".");
        } else {
            log("Unable to initialize database at " + url + " for user " + user + ".");
        }
    }



    /-------------- basic types --------------/

    /** Base class for DOCTYPE definitions. */
    doctype [/]


    doctype html4strict [| <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
           "http://www.w3.org/TR/html4/strict.dtd">
    |]

    doctype html4loose [| <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
           "http://www.w3.org/TR/html4/loose.dtd">
    |]

    doctype html4frames [| <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN"
           "http://www.w3.org/TR/html4/frameset.dtd">
    |]

    doctype xhtmlstrict [| <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
           "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    |]

    doctype xhtmltrans [| <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    |]

    doctype xhtmlframes [| <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">
    |]

    doctype xhtml11 [| <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
        "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
    |]


    imagepath [/]

    image {
        if (imagepath) { imagepath; }
        sub;
    }

    color [/]


    /------- display constants for convenience -------/

    sp = " "
    urlsp = "%20";
    nbsp = "&nbsp;"
    amp = "&amp;"
    gt = "&gt;"
    lt = "&lt;"

    bslash = "\\"
    bar = "|"
    colon = ":"
    comma = ","
    dash = "-"
    dblquote [| " |]
    dot = "."
    eq = "="
    quote = "'"
    semicolon = ";"
    slash = "/"
    uscore = "_"


    hr = "<hr>"
    br = "<br>"
    para = "<p>"
    end_para = "</p>"

    char tab = (char) 9
    char newline = (char) 10
    char lf = (char) 10
    char cr = (char) 13
    

    /-- Convenience functions for building an HTML table.
     --
     -- They define a table by being interspersed with the contents of the table.
     -- start_table comes first, then the first item, then either next_column or next_row,
     -- then the next item, then next_column or next_row, and so forth until the last
     -- item, then finally end_table.
     --
     -- Example:
     --
     --     table_example {
     --
     --         start_table;
     --         [| Top left |]
     --         next_column;
     --         [| Top middle |]
     --         next_column;
     --         [| Top right |]
     --         next_row;
     --         [| Bottom left |]
     --         next_column;
     --         [| Bottom middle |]
     --         next_column;
     --         [| Bottom right |]
     --         end_table;
     --     }
     --/

    start_table(table_attribs, row_attribs, cell_attribs) [|
        <table {= table_attribs; =}><tr {= row_attribs; =}><td {= cell_attribs; =}>
    |]
    next_row(row_attribs, cell_attribs) [|
        </td></tr><tr {= row_attribs; =}><td {= cell_attribs; =}>
    |]
    next_column(cell_attribs) [|
        </td><td {= cell_attribs; =}>
    |]
    end_table [|
        </td></tr></table>
    |]

    /----------- session cache -------------/

    /** Table containing id values, used by next_id. **/
    int ids_in_session{} = { "next_id": -1 } 


    /** id generator **/
    dynamic int next_id {
        keep as this in ids_in_session: int n(int x) = x
        dynamic int incr = n + 1
        
        n(incr);
    }

    /**
     *  Standard base class for all pages.
     */
    response page(),(params{}),(request r),(request r, session s) {
      
      /---------------------------------------------/
      /------------ page initialization ------------/
      /---------------------------------------------/
      
        /** If a page needs to perform an initialization before construction of the
         *  page commences it should override this.
         **/
        page_init [/]

      /---------------------------------------------/
      /--------------- page metadata ---------------/
      /---------------------------------------------/

        /** The title of the page.  Appears in title bar. */
        title = owner.type
        
        /** Label for the page, suitable for a link or menu item pointing to this page. */
        label [/]

        /** The author of the page. */
        author [| me |]

        /** A short description of the page. */
        description [/]
        
        /** Optional value for expires meta tag **/
        expiration [| -1 |]
        
        viewport [| width=device-width, initial-scale=1 |]

        /** Indexing instructions for web crawlers **/
        robots = "all"

        meta[] = [ [| http-equiv="content-type" content="text/html; charset=iso-8859-1" |],
                   if (expiration)  {= [| http-equiv="expires" content="{= expiration; =}" |] =},
                   if (author)      {= [| name="author" content="{= author; =}"            |] =},
                   if (description) {= [| name="description" content="{= description; =}"  |] =},
                   if (viewport)    {= [| name="viewport" content="{= viewport; =}"        |] =},
                   if (robots)      {= [| name="robots" content="{= robots; =}"            |] =} ]

        links[] = []

      /---------------------------------------------/
      /-------------- page attributes --------------/
      /---------------------------------------------/

        dynamic page_name = owner.type

        /------- scripting options -------/
        boolean ajax_enabled = false
        boolean drag_enabled = false
        boolean debug_enabled = false
        boolean use_wait_cursor = false
        
        /------- style options -------/
        boolean auto_style = false
                
        /------- visual properties -------/
        color bgcolor [/]
        color linkcolor [/]
        color vlinkcolor [/]
        color alinkcolor [/]
        image bgimage [/]

        string[] additional_styles = owner.descendants_of_type("style")
        
        style {
            if (core_styles(options)) {
                core_styles(options);
            }
            
            if (auto_style) {
                for s in additional_styles {
                    if (s.full_name != this.full_name) {
                        s;
                        /-- log("...adding style " + s.full_name + " owned by " + s.owner.full_name); --/ 
                    }
                }
            }
        }

        boolean options{} = { "ajax": ajax_enabled,
                              "drag": drag_enabled,
                              "debug": debug_enabled,
                              "use_wait_cursor": use_wait_cursor }

        string[] additional_scripts = owner.descendants_of_type("script")

        dynamic core_script { 
            core_scripts(options, find_element_function, page_name);
            if (onload_script) {
                [| window.onload = function() { |]
                onload_script;
                [| } |]
            }
        }
        
        dynamic script {
            core_script; 
            
            for s in additional_scripts {
                if (s.full_name != this.full_name) {
                    s;
                    log("...adding script " + s.full_name + " owned by " + s.owner.full_name); 
                }
            }
            
        }
        
        onload_script [/]
        
        /** find_element_function encapsulates the javascript code for calls to
         *  document.getElementById, allowing pages to customize this behavior,
         *  for example to allow finding an element in another frame.
         **/
        find_element_function [|
            function findElement(id) {    
                return document.getElementById(id);
            }
        |]
        

      /---------------------------------------------/
      /-------------- page components --------------/
      /---------------------------------------------/

        doctype doctype_header = xhtmltrans

        html_header [|
            <head>
            <title>{= title; =}</title>
            {=
                for m in meta [|
                    <meta {= m; =} >
                |]
                
                for l in links [|
                    <link {= l; =} >
                |]

                if (style) [|
                    <style type="text/css" media="all">
                    {= style; =}
                    </style>
                |]
            =}
            </head>
        |]

        body_attribs = background;

        background {
            if (bgimage) {
                [| background="{= bgimage; =}" |]
                sp;
            }
            if (bgcolor) {
                [| bgcolor="{= bgcolor; =}" |]
                sp;
            }
            if (linkcolor) {
                [| link="{= linkcolor; =}" |]
                sp;
            }
            if (vlinkcolor) {
                [| vlink="{= vlinkcolor; =}" |]
                sp;
            }
            if (alinkcolor) {
                [| alink="{= alinkcolor; =}" |]
                sp;
            }
        }

        page_begin [|
            {= doctype_header; =}
            <html>
            {= html_header; =}
            <body {= body_attribs; =}>
        |]

        page_end [|
            {=
                if (script) [|
                    <script>
                    {= script; =}
                    </script>
                |]
            =}
            </body>
            </html>
        |]

      /---------------------------------------------/
      /------------- page construction -------------/
      /---------------------------------------------/

        eval(page_init);
        page_begin;
        sub;
        page_end;
    }

    /------------ standard test definitions -----------/

    /**
     *  Standard test result
     */

    
    test_result(test_name, boolean rslt, msgs[]) {
        keep: name = test_name
        keep: boolean result = rslt
        keep: messages[] = msgs
        
        eval(name);
        eval(result);
        eval(messages);
        
        this;
    }

    /**
     *  Standard test
     */

    dynamic test_base {

        name = owner.type

        /** every test has to define the expected output **/ 
        expected [?]
        
        /** accumulator for logging messages **/
        keep: string[] logged_messages(msgs[]) = msgs
        
        /** a test can log a message at any point **/
        dynamic test_log(msg) {
            eval(logged_messages(: logged_messages + msg :));
            log("test_log: " + msg);
        }

        /** a test can log an error message at any point **/
        dynamic error_log(msg) {
            err_msg = "<span style='color:red'>" + msg + "</span>"
            eval(logged_messages(: logged_messages + err_msg :));
            log("error_log: " + msg);
        }

        sub;    
    } 

    /**
     *  Standard test runner
     */

    test_runner {
    
        name = owner.type

        test_base[] tests = owner.children_of_type("test_base")

        /** accumulator for test results **/
        keep: test_result[] results(test_result[] rslts) = rslts 
        test_result newest_result(test_result tr) = tr

        dynamic run {
            eval(results(: :));
            for test_base t in tests {
                eval(newest_result(: run_test(t) :));
                eval(results(: results + newest_result :));
            }
        } 
        
        dynamic test_result run_test(test_base t) {
            log("Running " + t.name);
            if (t == t.expected) {
                log(" ...test " + t.name + " passed.");
                log("     .... logged_messages[0]: " + t.logged_messages[0]);
                test_result(: t.name, true, t.logged_messages :);
            } else {
                log(" ...test " + t.name + " failed.");
                log("     .... logged_messages[0]: " + t.logged_messages[0]);
                test_result(: t.name, false, t.logged_messages :);
            }
        }
    } 

    

    /--------------- error handling --------------/

    /**
     * Base class for errors.
     */
    
    error(int stat, msg, fun_context ctx),
         (int stat, msg),
         (msg, fun_context ctx),
         (msg) {
         
        int status = stat ?? stat : 200
        message = msg
        context = ctx
        
        this;
    }
         


    /**
     *  Standard error page.
     */

    public page error_page(request r),(error err) {
        title = [| Error |]
        color bgcolor = "#EEDDAA"

        int status = err ?? err.status : 500

        error_div(r);        
    }    
    
    public component error_div(request r),(error err) {
        
        error_message {
            with (err) { err.message; }
            else if (r.params["message"]) { r.params["message"]; }
            else if (r.query) { trim_leading(r.query, "message="); }
            else [/]     
       }

        [| <h2>Error constructing page:</h2><h3> |]

        if (error_message) {
            error_message;
        } else [|
            No additional information available.
        |]

        [| </h3><p><i>Fun version {= version; =}</i></p> |]
    }
}
