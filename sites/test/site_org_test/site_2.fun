/--- test handling of multiple source files and varying adopted sites ---/

/-- main site source file 2 --/

site site_org_test {

    adopt lib_2
    
    site_2_uses_lib_1 [| This is site_2 using lib_1: {= lib_1_statement; =} |] 

    site_2_uses_lib_2 [| This is site_2 using lib_2: {= lib_2_statement; =} |] 
}