/--
 -- test the Fun script runner
 --/
 
script test_script {
 
    public main(args[]) {
        int num_args = args.count
         
        if (num_args) {
            "test script called with ";
            num_args;
            " arguments: ";
            args;
            newline;
        } else {
            "test script called with no arguments.\n";
        }
        exit(0);
    } 
}
 
 
 
 