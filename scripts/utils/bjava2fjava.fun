/--
 --  bjava2fjava.fun
 --
 --  Replaces headers of Java source files from the Bento version to
 --  to the Fun version
 --
 --  Walks the Java source tree and does the following for each file:
 --    
 --    * opens xxx.java and loads lines
 --    * if lines begin with new header, skip to next file
 --    * renames file from xxx.java to xxx.java.old
 --    * creates new file called xxx.java
 --    * writes new header to xxx.java
 --    * writes lines from old file to new file except for header lines
 --    * close file
 --/
 
script bjava2fjava {

    /** Header to replace old header with.  Assumes the second line will be replaced with the file name. **/
    static new_header[] = [ "/* Fun Compiler and Runtime Engine",
                            " *",
                            " *",
                            " * Copyright (c) 2017 by Fun Development",
                            " * All rights reserved.",
                            " */" ]
    
 
    public main(args[]) {
        src_path = args[1]
    
        if (args.count < 2) {
            "Usage: bjava2fjava <path>\n";
             exit(1, true);
        } else {
            "Called with path: ";
            src_path;
            newline;
        }
         
        walk(src_path);
        "Done.\n";
        exit(0);
    }
     
    dynamic walk(path) {
        files[] = dir_tree(path)
        
        for f in files {
            if (ends_with(f, ".java")) {
                convert(f);
            }
        }
    }
     

    dynamic boolean is_convertible(lines[]) = lines.count > 0
                                                 && !("Generated" in lines[0] || "generated" in lines[0])
                                                     && "/*" in lines[0] && "*/" in lines[8]    

    dynamic boolean show_convertible(lines_to_check[]) {
        lines[] = lines_to_check
        "    -- lines count: ";
        lines.count;
        newline;
        "    -- line 0: ";
        lines[0];
        newline;
        if (lines.count > 8) {
            "    -- line 8: ";
            lines[8];
            newline;
        }
    }
     
    dynamic convert(srcfile) {
        srcfilename = substring(srcfile, last_index_of(srcfile, "/") + 1)
        dynamic new_header_1 = " * " + srcfilename 
        lines[] = lines_from_file(srcfile)
        lines_after_header[] = [ for int i from 0 and line in lines { if (i > 8) { line } } ]
        dynamic header_lines[] = [ new_header[0], new_header_1, for int i from 2 to 6 { new_header[i] } ]
        converted_lines[] = header_lines + lines_after_header
        converted_text { for line in converted_lines { line; newline; } }

        if (is_convertible(lines)) {
            "Converting source file ";
            srcfile;
            newline;
            if (rename_file(srcfile, srcfile + ".old")) {
                if (!file(srcfile).overwrite(converted_text)) {
                   "  ...unable to convert ";
                   srcfile;
                   newline;
                }
            
            } else {
               "  ...unable to rename ";
               srcfile;
               ", skipping";
               newline;
            }


        } else { 
            "  ...skipping ";
            srcfile;
            newline;
        }
     }
 }
 
 
 
 