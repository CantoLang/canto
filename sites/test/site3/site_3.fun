/**
 *    site_3 test site.
 **/

site site_3 {

    /------------------ per-session initialization -----------------/

    session_init {
        s3_x(: "site_3 session_init called" :);
    }

    s3_x(x) = x

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
        title [| Site 3 |]
        label [| Site 3 test site |]

        /-- appearance variables --/
        color bgcolor = main_bgcolor
        
        
        /-- page components --/


        page[] destinations = [ site_1.index
                              , site_1.local_page
                              , site_1.s1_page
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
            [| <table border="0" cellspacing="0" cellpadding="4" align="left" valign="top" height="100%"> |]
            for page p in destinations [|
                <tr><td>{= menuitem(p); =}</td></tr>
            |]
            [| </table> |]
        }
        
        [| <table><tr><td> |]
        menu;
        [| </td><td> |]
        content;
        [| </td></tr></table> |]
        
    }

    /-------------- Site Pages ---------------/

    /** The front end of the application. **/    
    basepage(r, s) index(request r, session s) {
        title [| Site 3 |]
        label [| Site 3 Home |]

        content [|
            <p>This is the Site 3 home page.</p>
        |]
    }

    basepage(r, s) local_page(request r, session s) {
        title [| Site 3 Local Page|]
        label [| Site 3 local_page = {= owner.type; =} |]

        content [|
            <p>This is a Site 3 local page.</p>
        |]
    }

    basepage(r, s) s3_page(request r, session s) {
        title [| Site 3 Special Page|]
        label [| Site 3 s3_page = {= owner.type; =} |]

        content [|
            <p>This is the Site 3 special page.</p>
        |]
    }

}
