/**
 *    site_1 test site.
 **/

site site_1 {

    /------------------ per-session initialization -----------------/

    session_init {
        s1_x(: "site_1 session_init called" :);
        site_2.session_init;
        site_3.session_init;
    }

    s1_x(x) = x

    /------------------ site options -----------------/
    check_name = "body_attribs"
    verbosity = 2
    
    debug_info(info) {
        /-- comment this out to turn off display of debugging information --/
        info;
    }


    /------ configuration properties ------/
    urlprefix(page p) [| /{= if (p.site.type != sitename) { p.site.type; "/"; } =} |]  
	
	/------ display properties ------/

    color main_bgcolor  = "#CC11DD"

	/----------- page layout components ---------------/

            
    /------------------ base page --------------------/

    page(r, s) basepage(request r, session s) {
        /-- metadata --/
        title [| Site 1 |] 
        label [| Site 1 test site |]

        /-- appearance variables --/
        color bgcolor = main_bgcolor
        
        
        /-- page components --/

        page[] destinations = [ index
                              , local_page
                              , s1_page
                              , site_2.index
                              , site_2.local_page
                              , site_2.s2_page
                              , site_3.index
                              , site_3.local_page
                              , site_3.s3_page
                              ]

        menuitem(page p) [|
            <div class="menu_item"><a href="{= urlprefix(p); p.type; =}">{= p.label; =}</a></div>
        |]

        content [?]

        menu {
            [/ <table border="0" cellspacing="0" cellpadding="4" align="left" valign="top" height="100%"> /]
            for page p in destinations [/
                <tr><td>{= menuitem(p); =}</td></tr>
            /]
            [/ </table> /]
        }
        
        [/ <table><tr><td> /]
        menu;
        [/ </td><td> /]
        content;
        [/ </td></tr></table> /]
        
    }

    /-------------- Site Pages ---------------/

    /** The front end of the application. **/    
    basepage(r, s) index(request r, session s) {
        title [| Site 1 |]
        label [| Site 1 Home |]

        content [|
            <p>This is the Site 1 home page.</p>
        |]
    }

    basepage(r, s) local_page(request r, session s) {
        title [| Site 1 Local Page|]
        label [| Site 1 local_page = {= owner.type; =} |]

        content [|
            <p>This is a Site 1 local page.</p>
            
            <p>site configs:</p><ul>
            {=
                for site_config sc in all_sites [/
                    <li>site {= sc.name; =}:<ul>
                        <li>sitepath: {= sc.sitepath; =}</li>
                        <li>filepath: {= sc.filepath; =}</li>
                    </ul></li>
                |]
            =}
            </ul>
        |]
    }

    basepage(r, s) s1_page(request r, session s) {
        title [| Site 1 Special Page |]
        label [| Site 1 s1_page = {= owner.type; =} |]

        content [|
            <p>This is the Site 1 special page.</p>
        |]
    }

}
