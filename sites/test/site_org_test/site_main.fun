/--- test handling of multiple source files and varying adopted sites ---/

/-- main site source file --/

site site_org_test {

    page index {
    
        title = "site_org_test"
        
        [|
           <h3>{= title; =}</h3>
           <p>Tests handling of multiple source files and varying adopted sites.</p>
           <ul>
           <li><a href="{= good_test.type; =}">Good test</a></li>
           <li><a href="{= bad_test.type; =}">Bad test</a></li>
           </ul>
        |] 
    =]

    page good_test {

        title = "good_test"
        
        [|
           <h3>{= title; =}</h3>
           <p>This stuff should work.</p>
           <ul>
           <li>{= site_1_uses_lib_1; =}</li>
           <li>{= site_2_uses_lib_2; =}</li>
           </ul>
           
           <p><a href="index">Back.</a></p>
        |] 
        
        
    }
    
    page bad_test {
    
        title = "bad_test"
        
        [|
           <h3>{= title; =}</h3>
           <p>This stuff should fail.</p>
           <ul>
           <li>{= site_1_uses_lib_2; =} catch [| site_1_uses_lib_2 failed. |]</li>
           <li>{= site_2_uses_lib_1; =} catch [| site_2_uses_lib_1 failed. |]</li>
           </ul>
           
           <p><a href="index">Back.</a></p>
        |] 
    }
}
