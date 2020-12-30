/* Canto Compiler and Runtime Engine
 * 
 * CantoCompiler.java
 *
 * Copyright (c) 2018-2020 by cantolang.org
 * All rights reserved.
 */

package canto.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.Date;

import canto.compiler.visitor.IncompleteDefinitionVisitor;
import canto.lang.Core;
import canto.lang.Definition;
import canto.lang.Instantiation;
import canto.lang.Redirection;
import canto.lang.SiteLoader;
import canto.runtime.Context;
import canto.runtime.SiteBuilder;

/**
 * canto compiler.
 * 
 * @author Michael St. Hippolyte
 */

public class CantoCompiler {

    public static boolean verbose = true;

    public static void log( String str ) {
        if ( verbose ) {
            System.out.println( str );
        }
    }

    public static void main( String args[] ) {
        if ( args.length == 0 ) {
            showHelp();
        } else {
            CantoCompiler bc = new CantoCompiler();
            bc.compile( args );
        }
    }

    private static String durString( long time ) {
        if ( time > 500 ) {
            long sec = time / 1000;
            int hundredths = ( (int)( time - sec * 1000 ) + 5 ) / 10;
            return String.valueOf( sec ) + "." + String.valueOf( hundredths ) + " sec.";
        } else {
            return String.valueOf( time ) + " ms.";
        }
    }

    private static void showHelp() {
        System.out.println( "\nUsage:\n        java cantoc [options] sourcepath[:sourcepath...] [pagename]" );
        //System.out.println( "\n   or:\n        java -jar canto.jar [options] sourcepath[:sourcepath...]" );
        System.out.println( "\nwhere sourcepath is a file or directory pathname and pagename is" );
        System.out.println( "the name of the page to generate.  If pagename is omitted, all pages" );
        System.out.println( "defined in the source files are generated." );
        System.out.println( "\nThe following options are supported, in any combination:" );
        System.out.println( "\n   -a          Do not allow generate output for pages that" );
        System.out.println( "               have incomplete definitions." );
        System.out.println( "\n   -f filter   If sourcepath is a directory, load only the" );
        System.out.println( "               files that match the filter (default: *.can)" );
        System.out.println( "\n   -l logfile  Write logging information to logfile (default:" );
        System.out.println( "               write to console)" );
        System.out.println( "\n   -o dirname  Write output files to the dirname directory" );
        System.out.println( "               (default: write to current directory)" );
        System.out.println( "\n   -r          If sourcepath is a directory, recurse through" );
        System.out.println( "               subdirectories" );
        System.out.println( "\n   -v          Verbose output" );
        System.out.println( "\n   -?          Show this help screen" );
    }

    /**
     * The class and its extension points
     * 
     */
    public CantoCompiler() {
    }
    
    /**
     * isOutputAllowed
     * 
     * Checks if we need to generate output for a particular page.  The default convention is
     * to not allow output if the instance is abstract, since it does not have all the 
     * necessary information to be instantiated.
     * 
     * A secondary check, enabled with the "-a" flag, is to not allow this if the page 
     * has an incomplete Definition.  This just checks if there is a null Literal, and
     * does not allow output in there is such a Literal.
     * 
     * @param context
     * @param page
     * @param instance
     * @return
     */
    protected boolean disableIncompleteDefinition = false;
    protected boolean isOutputAllowed( Context context, Definition page, Instantiation instance ) {
        if ( instance.isAbstract( context ) ) {
            return false;
        }
        if ( disableIncompleteDefinition ) {
            IncompleteDefinitionVisitor idv = new IncompleteDefinitionVisitor();
            idv.traverse( page.getContents() );
            if ( idv.isIncomplete() ) {
                return false;
            }
        }
        
        
        return true;
    }

    /**
     * createOutputDirectory
     * 
     * Default convention is to use the current directory ".", unless the page
     * Definition overrides this by specifying a String constant
     * "directoryLocation"
     * 
     * @param page
     * @param instance
     * 
     * @return a File indicating the output directory, or null if there is no
     *         output associated with this page and instance.
     */
    protected String DEFAULT_OUTPUT_DIRECTORY = ".";

    protected File createOutputDirectory( Definition page, Instantiation instance ) {
        String directoryLocation = page.getStringConstant( "directoryLocation", DEFAULT_OUTPUT_DIRECTORY );
        return createOutDir( page.getOwner().getName() + File.separatorChar + directoryLocation );
    }

    /**
     * createFileName
     * 
     * Default convention is to use the page name with the default extension
     * ".html" current directory ".", unless the page Definition
     * 
     * @param page
     * @param instance
     * 
     * @return a String used for the file name, or null if there is no output
     *         associated with this page and instance
     */
    protected String DEFAULT_FILE_NAME_EXTENSION = "html";

    protected String createFileName( Definition page, Instantiation instance ) {
        String fileExtension = page.getStringConstant( "fileExtension", DEFAULT_FILE_NAME_EXTENSION );
        return page.getName() + "." + fileExtension;
    }

    File createOutDir( String outDirName ) {
        outDirName = outDirName.replace( '.', File.separatorChar );
        outDirName = outDirName.replace( '/', File.separatorChar );
        outDirName = outDirName.replace( '\\', File.separatorChar );
        File outDir = new File( outDirName );
        if ( !outDir.exists() ) {
            log( "Creating output directory " + outDir.getAbsolutePath() );
            outDir.mkdirs();
        }

        if ( !outDir.isDirectory() ) {
            log( "Output path " + outDir.getAbsolutePath() + " is not a directory." );
            return null;
        } else {
            return outDir;
        }
    }

    void compile( String args[] ) {

        long startTime = System.currentTimeMillis();

        String logFileName = null;
        String inFilter = "*.canto";

        String cantoPath = null;
        String pageName = null;
        boolean recursive = false;
        boolean multiThreaded = false;
        boolean autoLoadCore = true;

        System.out.println( "\ncantoc compiler for Canto\nCopyright (c) 2018-2020 by cantolang.org\n" );
        for ( int i = 0; i < args.length; i++ ) {
            if ( args[ i ].charAt( 0 ) == '-' && args[ i ].length() > 1 ) {
                switch ( args[ i ].charAt( 1 ) ) {
                case 'a':
                    disableIncompleteDefinition = true;
                    break;
                case 'c':
                    autoLoadCore = false;
                    break;
                case 'f':
                    if ( i < args.length - 1 ) {
                        i++;
                        inFilter = args[ i ];
                    }
                    break;
                case 'l':
                    if ( i < args.length - 1 ) {
                        i++;
                        logFileName = args[ i ];
                    }
                    break;
                case 'm':
                    multiThreaded = true;
                    break;
                case 'o':
                    if ( i < args.length - 1 ) {
                        i++;
                        DEFAULT_OUTPUT_DIRECTORY = args[ i ];
                    }
                    break;
                case 'r':
                    recursive = true;
                    break;
                case 'v':
                    SiteBuilder.verbosity = SiteBuilder.VERBOSE;
                    break;
                case '?':
                    showHelp();
                    return;

                }

            } else if ( cantoPath == null ) {
                cantoPath = args[ i ];

            } else if ( pageName == null ) {
                pageName = args[ i ];
            }
        }

        if ( cantoPath == null ) {
            System.out.println( "No path specified, exiting." );
            return;
        }

        if ( logFileName != null ) {
            try {
                PrintStream log = new PrintStream( new FileOutputStream( logFileName, true ) );
                System.setOut( log );
                Date now = new Date();
                System.out.println( "\n=========== begin logging " + now.toString() + " ============" );
            } catch ( Exception e ) {
                System.out.println( "Unable to log to file " + logFileName + ": " + e.toString() );
            }
        }

        Core core = new Core();

        SiteLoader.LoadOptions options = SiteLoader.getDefaultLoadOptions();
        options.multiThreaded = multiThreaded;
        options.autoLoadCore = autoLoadCore;
        options.configurable = false;
        options.allowUnresolvedInstances = false;

        SiteLoader loader = new SiteLoader(core, "", cantoPath, inFilter, recursive, options);
        loader.load();
        long parseTime = System.currentTimeMillis() - startTime;

        // all done; log results
        Object[] sources = loader.getSources();
        Exception[] exceptions = loader.getExceptions();

        for ( int i = 0; i < sources.length; i++ ) {
            Object source = sources[ i ];
            String name = ( source instanceof File ? ( (File)source ).getAbsolutePath() : source.toString() );
            Exception e = exceptions[ i ];
            if ( e != null ) {
                log( "Unable to load " + name + ": " + e );
                e.printStackTrace();
            } else {
                log( name + " loaded successfully." );
            }

        }

        Definition[] pages = core.getDefinitions( "page" );

        int numPages = pages.length;
        if ( numPages > 0 ) {
            // now spit out pages
            int count = 0; // count of pages successfully written
            try {

                Context context = new Context( core );
                for ( int i = 0; i < numPages; i++ ) {

                    if ( pageName != null && !pageName.equals( pages[ i ].getName() ) ) {
                        continue;
                    }

                    Definition page = pages[ i ];
                    Instantiation instance = new Instantiation( page );
                    if ( !isOutputAllowed( context, page, instance )) {
                        log( page.getFullName() + " does not generate output; skipping" );
                        continue;
                    }
                    String pageText = null;
                    try {
                        pageText = instance.getText( context );
                    } catch ( Redirection r ) {
                        log( "Page " + pageName + " redirects to " + r.getLocation() + "; skipping" );
                        continue;
                    }
                    if ( pageText != null && pageText.length() > 0 ) {
                        File outDir = createOutputDirectory( page, instance );
                        if ( outDir != null ) {
                            String fileName = createFileName( page, instance );
                            if ( fileName != null ) {
                                log( "Page " + count + " is " + fileName );
                                File pageFile = new File( outDir, fileName );
                                if ( !pageFile.exists() ) {
                                    File parent = pageFile.getParentFile();
                                    if ( !parent.exists() ) {
                                        if ( !parent.mkdirs() ) {
                                            log( "Unable to create parent directory " + parent.getAbsolutePath() );
                                            continue;
                                        }
                                    }
                                    pageFile.createNewFile();
                                }

                                if ( !pageFile.canWrite() ) {
                                    log( "Unable to write to file " + pageFile.getAbsolutePath() );
                                    continue;
                                }

                                log( "Writing " + pageFile.getAbsolutePath() + "..." );
                                FileWriter writer = new FileWriter( pageFile );
                                writer.write( pageText );
                                writer.close();
                                count++;
                            }
                        }

                    } else {
                        log( "Page " + page.getFullName() + " has no content, skipping." );
                    }
                }
                log( "Done." );

                long totalTime = System.currentTimeMillis() - startTime;
                long perPageTime = ( count > 0 ? ( totalTime / count ) : 0L );
                log( count + " page" + ( count == 1 ? "" : "s" ) + " generated in " + durString( totalTime ) + " ("
                        + durString( perPageTime ) + " per page)" );
                log( "(parse time " + durString( parseTime ) + ")" );

            } catch ( Exception e ) {
                log( "Exception generating pages: " + e );
                e.printStackTrace();
            } catch ( Redirection r ) {
                log( "Redirection thrown creating context: " + r );
            }
        } else {
            log( "No pages in input." );
        }
    }

}
