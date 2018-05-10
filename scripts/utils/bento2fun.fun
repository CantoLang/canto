/--
 --  bento2fun.fun
 --
 --  Converts bento source files to fun source files
 --
 --  For each Bento file does the following:
 --
 --    * creates new Fun file
 --    * loads lines from Bento file
 --    * processes each line and writes to Fun file
 --    * keep track of entering and exiting data blocks
 --    * swap [= and =] with { and } outside of data blocks
 --    * swap [= and =] with {= and =} inside of data blocks
 --/
 
script bento2fun {
 
    public main(args[]) {
        bentosrc = args[1]
        if (args.count < 2) {
            "Usage: bento2fun <path>\n";
            exit(1, true);
        } else {
            "Called with path: ";
            bentosrc;
            newline;
        }
        walk(bentosrc);
        "Done.\n";
        exit(0);
    }
     
    dynamic walk(path) {
        files[] = dir_tree(path)
        
        for f in files {
            if (ends_with(f, ".bento")) {
                convert(f);
            }
        }
        console_println("done walking");
    }

    dynamic convert(bentofile) {
        funfile = ends_with(bentofile, ".bento") ? replace(bentofile, ".bento", ".fun") : (bentofile + ".fun") 
        bento_lines[] = lines_from_file(bentofile)
        
        keep: int data_levels = 0
        keep as data_levels: dynamic int add_to_data_levels(int n) = data_levels + n
        
        keep: fun_text = ""
        keep as fun_text: dynamic add_to_fun_text(text) = fun_text + text + newline

        static OPEN_CODE_0 = "[="
        static CLOSE_CODE_0 = "=]"
        static OPEN_CODE_1 = "{"
        static CLOSE_CODE_1 = "}"
        static OPEN_CODE_2 = "{="
        static CLOSE_CODE_2 = "=}"
        static OPEN_DATA_1 = "[/"
        static CLOSE_DATA_1 = "/]"
        static OPEN_DATA_2 = "[|"
        static CLOSE_DATA_2 = "|]"
        static NULL_BLOCK = "[/]"

        step_1A(str) = replace(str, OPEN_CODE_0, OPEN_CODE_1)
        step_1B(str) = replace(str, OPEN_CODE_0, OPEN_CODE_2)
        step_2A(str) = replace(str, CLOSE_CODE_0, CLOSE_CODE_1)
        step_2B(str) = replace(str, CLOSE_CODE_0, CLOSE_CODE_2)
        step_3(str) = replace(str, "bento", "fun")

        funcs_A[3] = [ step_1A, step_2A, step_3 ]          
        funcs_B[3] = [ step_1B, step_2B, step_3 ]          
        
        dynamic apply_funcs(funcs[], x), (funcs[], int n, x) {
            int last_func_ix = funcs.count - 1
            last_func = funcs[last_func_ix]
            int func_ix = n ?? n : 0
            func = funcs[func_ix]
            
            if (func_ix == last_func_ix) {
                last_func(x);
            } else if (func_ix < last_func_ix && func_ix >= 0) {
                apply_funcs(funcs, func_ix + 1, func(x));
            }
        }
       
        dynamic process_line(x) {
            int num_null_block = count_instances_of(x, NULL_BLOCK)
            int num_open_data = count_instances_of(x, OPEN_DATA_1) + count_instances_of(x, OPEN_DATA_2) - num_null_block
            int num_close_data = count_instances_of(x, CLOSE_DATA_1) + count_instances_of(x, CLOSE_DATA_2) - num_null_block

            eval(add_to_data_levels(num_open_data));
            
            if (data_levels > 0) {
                apply_funcs(funcs_B, x);
            } else {
                apply_funcs(funcs_A, x);
            }
            
            eval(add_to_data_levels(-num_close_data));
        }

        "Converting ";
        bentofile;
        newline;
        for line in bento_lines {
            eval(add_to_fun_text(process_line(line)));
        }

        if (!file(funfile).overwrite(fun_text)) {
            "  ...unable to write to ";
            srcfile;
            newline;
        }
    }
}
