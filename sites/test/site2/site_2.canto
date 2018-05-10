/**
 *    site_2 test site.
 **/

site site_2 {

    /------------------ per-session initialization -----------------/

    session_init {
        s2_x(: "site_2 session_init called" :);
    }

    s2_x(x) = x

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

    color site2_bgcolor  = "#CCDD11"

	/----------- page layout components ---------------/

            
    /------------------ base page --------------------/

    page(r, s) basepage(request r, session s) {
        /-- metadata --/
        title [| Site 2 |]
        label [| Site 2 test site |]

        /-- appearance variables --/
        color bgcolor = site2_bgcolor
        
        
        /-- page components --/

        page[] destinations = [ site_1.index
                              , site_1.local_page
                              , site_1.s1_page
                              , index
                              , local_page
                              , s2_page
                              , site_3.index
                              , site_3.local_page
                              , site_3.s3_page
                              ]
        
        menuitem(page p) [|
            <div class="menu_item"><a href="{= urlprefix(p); p.type; =}">{= p.label; =}</a></div>
        |]

        content [?]

        menu {
            [| <table border="0" cellspacing="0" cellpadding="4" align="left" valign="top" height="100%"> |]
            for page p in destinations [|
                <tr><td>{ menuitem(p); }</td></tr>
            |]
            [| </table> |]
        }
        
        [| <table><tr><td> |]
        menu;
        [| </td><td> |]
        [| <p>s2_x = {= s2_x; =}</p> |]
        content;
        [| </td></tr></table> |]
        
    }

    /-------------- Site Pages ---------------/

    /** The front end of the application. **/    
    basepage(r, s) index(request r, session s) {
        title [| Site 2 |]
        label [| Site 2 Home |]

        content [|
            <p>This is the Site 2 home page.</p>
        |]
    }

    basepage(r, s) local_page(request r, session s) {
        title [| Site 2 Local Page|]
        label [| Site 2 local_page = {= owner.type; =} |]

        content [|
            <p>This is a Site 2 local page.</p>
        |]
    }

    basepage(r, s) s2_page(request r, session s) {
        title [| Site 2 Special Page|]
        label [| Site 2 s2_page = {= owner.type; =} |]

        content [|
            <p>This is the Site 2 special page.</p>
        |]
    }

}
