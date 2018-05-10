/--
 --   config.fun
 --/


{
    /** The primary site. **/
    site_config main_site = test_config

    /** All the other sites that the server should make available.  It is not
     *  necessary to include the primary site in this list, but it is allowed.
     *  Including the primary site has no effect, but may be desired to allow
     *  quick switching by putting the list of all possible primary sites in
     *  all_sites, then merely editing one line (the definition of main_site)
     *  to point to one or another of the sites in all_sites.  The other sites
     *  are defined but not instantiated unless explicitly accessed.  This
     *  approach provides syntax checking combined with lazy instantiation,
     *  which translates to increased reliability combined with more efficient
     *  resource utilization.
     **/ 
    site_config[] all_sites = []
    
    site_config{} site_config_table = { for site_config sc in all_sites {= sc.name: sc =} }
    
    /** If true, the sites in all_sites share the main_site core instead of
     *  loading their own.
     **/
    boolean share_core = true
    
    /** Verbosity level for console output, can range from 0 (terse) to 2 (verbose) **/
    int verbosity = 1;

    /** Define the available external interfaces (address and port).  Not every site
     *  necessarily responds to every interface.  Individual sites may filter out 
     *  some of them.
     **/
    listen_to[] = [ "127.0.0.1:8888" ]

    /** Do not handle requests for files with the following extensions; let the server's
     *  default file handling logic handle them.
     **/ 
    ignore_extensions[] = [ "png", "jpg", "gif", "ico" ]

    /** Handle requests with the following extensions if they are defined, even if they
     *  are not declared as responses or pages (i.e., handle as if a '$' were prepended).
     **/ 
    always_handle_extensions[] = [ "css", "js" ]


    /--- configuration variables queried by the server ---/
    file_base = main_site.filepath
    boolean files_first = main_site.files_first
    funpath = main_site.funpath
    sitename = main_site.name
    sitepath = main_site.sitepath
    
    file_separator = "/"

    /--- config objects for available sites ---/

    /--- fun language test site ---/
    site_config test_config {
        name = "test"
        funpath = "./sites/test"
        filepath = "."
    }    

    /--- site organization test ---/
    site_config site_org_test_config {
        name = "site_org_test"

        funpath = "./sites/test/site_org_test"
    }

    /--- multisite test site 1 ---/ 
    site_config site1_config {
        name = "site_1"

        funpath = "./sites/test/site1/site_1.fun"
    }
    
    /--- multisite test site 2 ---/ 
    site_config site2_config {
        name = "site_2"

        funpath = "./sites/test/site2/site_2.fun"
    }
    
    /--- multisite test site 3 ---/ 
    site_config site3_config {
        name = "site_3"

        funpath = "./sites/test/site3/site_3.fun"
    }
    
}
