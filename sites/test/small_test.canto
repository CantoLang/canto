/-- little test --/

site small_test {
    response index [| <h6>small test</h6> |]

    concurrent_test {
    
        launch {
            {+
                new_thread;
            +}
        }
        
        if (running) {
          
            "counter is running.<br>tick: ";
          
            tick;
       
        } else { 
            "launching concurrent instantiation.<br>";
              
            launch;
            
            if (launch) {
                "error: ";
                launch;
            }           
       
        }
    }

    concurrent_test_2 {
    
        launch {
            new_thread;++
        }
        
        if (running) {
          
            "counter is running.<br>tick: ";
          
            tick;
       
        } else { 
            "launching concurrent instantiation.<br>";
              
            launch;
            
            if (launch) {
                "error: ";
                launch;
            }           
       
        }
    }

    boolean running(boolean flag) = flag
    int tick(int n) = n
    
    dynamic new_thread {
        eval(running(: true :));
        for int i from 0 to 10 {
            log("^ tick " + i);
            eval(tick(: i :));
            
            sleep(1000);
        
        }
    
        eval(running(: false :));
    }
    
    cache_test {
    
        a(x) = x
        
        a("A");
        a;
        a("B");
        a;
        a(: "C" :);
        a;        
    
    }
    
    keep_by_test {
        test_array[] = [ "x", "x", "x" ]
        
        keep by ix0 in test_array: val1(x) = x
        
        int ix0 = 0
        int ix1 = 1

        /-- it needs to be a dynamic instantiation because val1 already has a cached
         -- value thanks to the keep directive. --/
        val1(: "E" :);

        test_array[0];
    }
        
    keep_by_2 {
        cache_array[] = [ "X", "X", "X" ]
        keep by ix0 in cache_array: val1(x) = x
        keep by ix1 in cache_array: val2(x) = x
        ix0 = "0"
        int ix1 = 1
        
        eval(val1(: "E" :));
        eval(val2(: "F" :));
        
        cache_array[0];
        cache_array[1];
    }


    nested_keep_test {
        keep: a_container {
            keep in container: a(x) = x
            a("A");
        }

        a_container;    
        a_container.a;
    }
}