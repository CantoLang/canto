Bento in a Nutshell
===================

##1. The Big Picture

The essence of a programming language, what might be called its DNA equivalent, is the grammar that defines it.  Most of this document will describe aspects of Bento's grammar and explain how it can be used to achieve useful results.  But DNA is not the organism.  A living creature is the product of the complex interaction of its DNA with its environment.  So too, understanding programs written in a language requires understanding not just the grammar but the larger context within which the language operates -- a greater whole encompassing both the program and the user as well as the mechanisms by which they interact.

Looking at this greater whole, most programming languages in wide use follow what might be called the command model of computing.  This model views a program as a command to be executed.  The user invokes the program by typing a command at the command line, or through some other system facility -- double-clicking an icon, selecting from a menu, etc.; the principle is the same.  The program then runs from start to finish, acquiring resources, perhaps soliciting input from the user, performing various tasks, generating some kind of output and eventually exiting.  The natural programming paradigm in this context is procedural: a program is a procedure, and the statements that make up the program are steps in the procedure.  If the program by chance obtains input or generates output, it does so as a side effect of some step or other in the procedural logic it executes.

Bento, in contrast, follows a web-style request/response model of computing.  This model views an application as a system of potential responses.  The user makes a request in some manner, such as typing a url into a browser; if the application has defined a response for this request, that response is retrieved and returned to the user.  Some parts of the response may already exist, some parts may need to be constructed, and all parts have to be woven together into a proper whole.  Some of the construction may involve the execution of complex logic, but the point of the code is to construct output.  Computation is a side effect.

Bento is declarative, which is a natural paradigm for describing output.  But Bento isn't a typical declarative language.  Its unique grammar allows it to express procedural logic, or, more precisely, declarative logic that can look and feel very procedural.  This dual personality allows Bento to handle both markup and scripting functions effectively and naturally.

But Bento's primary goal is not to follow any particular paradigm or model.  It's to be expressive.  Programming languages are human languages, written by humans, for humans.  (Computers, on the other hand, prefer machine language, which is not readily comprehensible to humans.)  Bento, like any ambitious programming language, is intended to allow programmers to craft application architectures, data structures, and implementations of business logic that are clear, concise and communicative to themselves and their fellow programmers.

##2. Running Bento

As described above, the runtime platform for Bento is a web server, specifically the Bento server, which is a standalone web server attached to a compiler and a runtime evaluation engine.  Alternatively, the Bento server can run as a servlet, which operates the same but piggybacks on an existing web server rather than providing its own web server.

The reference implementation of the Bento server is written in Java, so Bento also requires a standard Java runtime environment (JRE) to run.

A Bento application consists of one or more text files containing code written in Bento.  This code defines responses.  When the Bento server is launched, it compiles the source files into an internal representation, then listens for requests.  When the server receives a request, it constructs an appropriate response by evaluating the Bento code that defines it.

###2.1 Configuration

It's possible to configure Bento by means of parameters on the command line (if run as a standalone server) or in the web.xml file (if run as a servlet), but it's also possible and generally more convenient to configure Bento via a configuration file written in Bento (Bento happens to be a good configuration language).  At startup, Bento searches for a file called config.bento in the current directory.  If found, it looks in this file for configuration information such as the path to the source files for the application and the address and port to listen for requests on.

###2.2 Bento as a Service



##3. Blocks

Bento code is composed of two kinds of things: data, and instructions for generating data.  Data comes in data blocks, instructions come in code blocks.

###3.1 Data Blocks

A data block starts with ```[|``` and ends with ```|]```.  Example:
```
    [|
        <h1>Hello, world.</h1>
    |]
```

Leading and trailing white space is trimmed, so the above data block and the following data block are equivalent:
```
    [| <h1>Hello, world.</h1> |]
```

###3.2 Code Blocks

A code block starts with ```[=``` and ends with ```=]```.  Example:
```
    [=
        hello;
        goodbye;
    =]
```

###3.3 Nesting

Data can be embedded in code, and code can be embedded in data:
```
    [=
        [|
            <h1>Hello, [= name; =].</h1>
        |]
    =]
```

A data block may be empty, as may a code block:
```
    [| |]
    [= =]
```

There is special notation for an empty block (an empty block is empty of both code and data, so there is no need to distinguish beween the two):
```
    [/]
```

###3.4 Comments

Comments are blocks of text added for documentation or other purposes which are ignored by the Bento compiler.  Bento allows comments to be freely interspersed with Bento code.

There are two kinds of comments in Bento, documenting and nondocumenting.  Documenting comments are intended for usable commentary on the code that follows.  Automatic documentation generators should be able to construct documentation for Bento code by extracting the documenting comments.

Documenting comments are delimited by ```/*``` and ```*/```:
```
    /* This is a documenting comment. */
```

Nondocumenting comments are for text that is not suitable for documentation.  An example of such a comment would be one created by commenting out code -- a common programming practice that uses comments to hide code from the compiler without physically removing the code from the source file.

Nondocumenting comments are delimited by ```/--``` and ```--/```
```
    /-- This is a nondocumenting comment. --/
```

Comments may be nested as deeply as desired.  Documenting comments may be nested inside of nondocumenting comments, and vice versa, but nondocumenting comments render all embedded comments as nondocumenting as well, regardless of their delimiters.

Bento also supports single-line comments, which start with ```//``` and end at the next end-of-line character.  Single-line comments are nondocumenting.

##4. Constructions

Both data and code blocks do the same thing -- specify output.  A data block specifies output explictly.  A code block contains Bento statements that logically describe the output.  Such statements are called constructions.

###4.1 Basic Constructions

The simplest kind of construction consists of a name followed by a semicolon:
```
        hello;
```

The semicolon is the construction operator.  It constructs whatever immediately precedes it.  This may be a name as above or an explicit value, such as a literal string (delimited by double quotes), character (delimited by single quotes), number or boolean value (true or false):
```
    "Hello, World";
    'Z';
    23;
    true;
```

Names and values may be combined with various arithmetic, logical and string operators into expressions, delimited by parentheses:
```
    (x + 1)
    (is_enabled && is_authorized)
    ("Hello, " + "World")
```

Expressions, like names and values, may be contsructed using the construction operator:
```
    (start_tag + "Hello, World" + end_tag);
```

###4.2 Logical Constructions

Bento provides two kinds of special constructions for implementing logic, conditionals and loops.  The simplest conditional consists of an expression and a code or data block, which is evaluated only if the value of the expression, interpreted as a boolean value, is true:
```
    if (arriving) [= 
        hello;
    =]
```

A conditional may also provide an alternative block, which is evaluated if the test fails:
```
    if (arriving) [= 
        hello;
    =] else [=
        "So...";
    =]
```

Multiple conditionals may be strung together:
```
    if (arriving) [= 
        hello;
    =] else if (leaving) [=
        goodbye;
    =] else [=
        "So..."
    =]
```

Loops cause a code or data block to be evaluated some number of times.  The simplest kind of loop is used in conjuction with a collection, which is a Bento entity that groups together other entities (we will discuss exactly how collections are defined and what they can do in a later section):
```
    for x in x_list [=
        x;
    =]
```

Another kind of loop steps through a sequence of values:
```
    for int i from 0 to 3 [|
        <li>Line [= i; =]</li>
    |]
```

The body of the loop will be evaluated repeatedly with successive values of the loop parameter (i in the above example) until it reaches or exceeds the end value.  The loop body is not evaluated for the end value.  So the output from the above is
```
    <li>Line 0</li><li>Line 1</li><li>Line 2</li>
```

The default increment for this kind of loop is 1, but may be set to any value via the ```by``` keyword:
```
    for float x from 0 to 1.0 by 0.25
```

Loops may be nested; they may also be combined, using the "and" keyword:
```
    for x in x_list and y in y_list and int i from 0 [|
        <li>Point [= i; =]: ([= x; =], [= y; =])</li>
    |]
```

The above loop will repeat until either ```x_list``` or ```y_list``` runs out of members.

##5. Definitions

###5.1 Simple Definitions

When the construction operator is applied to a name, or to an expression which includes a name, Bento obtains the data associated with the name.  Such an association is called a definition.  A definition consists of a name and a scope, typically a code or data block, which contains the associated data, or code to generate the data.  Here is a simple definition using a data block:
```
    hello [|
        Hello, world.
    |]
```

Here is another simple definition, this time using a code block:
```
    greetings [=
        hello;
    =]
```

Here is an empty definition, using an empty block:
```
    say_nothing [/]
```

###5.2 Instantiation of Definitions

When a definition is instantiated, Bento constructs output by concatenating the constructions and blocks it contains, in the order in which they appear. So, if we have the following definitions:
```
    start_tag [| <h1> |]

    end_tag [| </h1> |]

    hello [=
        start_tag;
        [| Hello, world. |]
        end_tag;
    =]
```

then Bento handles the construction
```
    hello;
```

by concatenating the output of ```start_tag```, the data block ```[| Hello, world. |]``` and the output of ```end_tag```.  The result:
```
    <h1>Hello, world.</h1>
```

In Bento you always have more than one way to write an implementation, because you can always substitute code embedded in a data block for data emebedded in a code block (or vice versa).  Here is a code-in-data implementation of ```hello``` which achieves the exact same result as the data-in-code one above:
```
    hello [|
        [= start_tag; =]Hello, world.[= end_tag; =]
    |]
```

There is yet another way of writing a definition.  In the special case of a definition that contains exactly one construction, the definition may consist of a name, the definition operator ```=``` (equals sign), and the construction.  Example:
```
    start_tag = "<h1>"
```

This resembles the syntax for assignment in many languages.  And indeed it may appear to behave that way in many cases.  For example, consider the following bit of Bento code:
```
    hello = "Hello, world."

    hello;
```

Evaluating this, we get this output:
```
    Hello, world.
```

which is what we would expect from assigning a value to a variable and then outputting that variable.  But there is an important difference, not salient in this particular case but critical in many others.  A definition is not a construction, and doesn't do anything by its mere presence.  The statement that looks like an assignment is a definition, and the Bento server ignores it as it is constructing a response.  Until the Bento server gets to the following statement, which instantiates a ```hello```, there is no variable called ```hello``` containing the value "Hello, world."

Another way of describing this is to say that Bento is a lazy language -- nothing (ideally) is evaluated before it is needed.

###5.3 Instantiation Without Output

As noted earlier, a Bento program describes output, and computation is a side effect.  If computation is the purpose of a definition, however, the output may be superfluous or even inconvenient.  To accommodate such cases, Bento provides a built-in function, ```eval```, which instantiates its argument but throws away the output.

Example:
```
    a [| A |]
    b [| B |]
    c [| C |]

    a;
    eval(b);
    c;
```

which yields:
```
    AC
```

###5.4 Child Definitions

A definition can contain another definition, referred to as a child definition.  The following definition of greetings contains a child definition called ```hello```:
```
    greetings [=
        hello [| <h1>Hello, world.</h1> |]
    =]
```

A child definition, though, is not a construction, and is ignored by Bento when the containing definition is instantiated.  So given the above definition, the construction
```
        greetings;
```

would yield no output because the definition of greetings contains no constructions.  But a definition can contain both child definitions and constructions.  For example:
```
    greetings [=
        hello [| <h1>Hello, world.</h1> |]

        hello;
    =]
```

In this case, when ```greetings``` is instantiated, you get
```
    <h1>Hello, world.</h1>
```

A definition may have any number of child definitions, of any type, and there may be more child definitions nested within those.  Example:
```
    building [=
        floor_1 [=
            apt_1A [=
                int bedrooms = 1
                float baths = 1.0
            =]
            apt_1B [= 
                int bedrooms = 2
                float baths = 1.5
            =]
        =]
        floor_2 [=
            apt_PH [= 
                int bedrooms = 3
                float baths = 2.5
            =]
        =]
    =]
```

You can reference a child of a definition using the dot operator.  Continuing on the preceding example: 
```
    show_bedrooms [|
        <h2>Bedrooms</h2>
        <p>Apt. 1A: [=

            building.floor_1.apt_1A.bedrooms;

        =] </p><p>Apt. 1B: [= 

            building.floor_1.apt_1B.bedrooms;

        =] </p><p>Apt. PH: [= 

            building.floor_2.apt_PH.bedrooms;

        =] </p>
    |]
```

##7. Namespace

Bento's namespace is somewhat unusual.  Programming languages typically have different namespaces for different categories of entities such as types, classes, functions, objects and variables.  In Bento, however, the boundaries between the categories are not so distinct.  A single entity may span multiple categories, acting as a type in some cases, a function in others and a variable in yet others, even though the entity is defined in only one place.  So Bento has just one namespace for all entities.

One advantage of this approach is that a single Bento statement can operate on multiple levels simultaneously, or operate on different levels in different circumstances.  An example of the former is the way every definition is also a type declaration.  An example of the latter is the way some constructions can generate new data in some circurmstances and retrieve stored data in others (see the chapter on State below).

Besides the namespace they reside in, the important characteristics of names in Bento are the lexical rules they follow and the rules for resolving them, which depends on the scope they are in. 

###7.1 Names

The lexical rules for names in Bento are fairly standard: names are case sensitive; they may include letters, digits, underscores and dollar signs; they may not start with a digit.  Keywords in the language such as ```for``` and ```if``` may not be used as names.

###7.2 Scopes

Every name must resolve to a definition.  The resolution process depends on the scope in which the name occurs.
 
Every definition creates a scope.  Nested definitions create nested scopes.  A name may only be defined once in an immediate scope (a scope not including its nested scopes).  Here again is an example from chapter 5:
```
    building [=
        floor_1 [=
            apt_1A [=
                int bedrooms = 1
                float baths = 1.0
            =]
            apt_1B [= 
                int bedrooms = 2
                float baths = 1.5
            =]
        =]
        floor_2 [=
            apt_PH [= 
                int bedrooms = 3
                float baths = 2.5
            =]
        =]
    =]
```

Note that the name ```baths``` is defined three times, but each definition is in a separate scope.  If ```baths``` had appeared twice in the same scope, a compile-time error would occur.

When Bento encounters a name and needs to find the corresponding definition, it searches its namespace by scope. It begins with the immediate scope and works its way up through the parent's scope, and the parent's parent's scope, until it runs out of scopes or a matching definition is found.

Consider the following definition:
```
    greetings [=
        name = "Moon"

        hello [=
            name = "World"

            [| <p>Hello, [= name; =].</p> |]
        =]

        good_night [=
            [| <p>Good night, [= name; =].</p> |]
        =]
        
        hello;
        good_night;
    =]
```

When Bento instantiates ```hello```, it looks in the scope of ```hello```'s definition for a definition of ```name``` and finds it, giving ```name``` the value "World".  But ```good_night``` has no definition for ```name``` in its immediate scope, so when Bento instantiates ```good_night``` it continues its search to the next scope outwards, the one created by the definition of ```greetings```.  There it finds ```name```, containing the value "Moon".  The result:
```
    <p>Hello, World.</p><p>Good night, Moon.</p>
```

Using the child operator (dot), it's possible to instantiate a definition that is not in an accessible scope as long as it has an ancestor that is in scope.  For example, in the following code ```good_night``` accesses ```name```, which is not directly accessible, by referencing it as the child of ```hello```, which is accessible:
```
    greetings [=

        hello [=
            name = "World"

            [| <p>Hello, [= name; =].</p> |]
        =]

        good_night [=
            [| <p>Good night, [= hello.name; =].</p> |]
        =]
        
        hello;
        good_night;
    =]
```

Such access from a wider scope can be controlled using an access modifier -- a keyword that precedes the definition and determines specifies its behavior in some regard.  One such modifier is ```local```, which specifies local access.  If a definition is declared to have local access, it is invisible to all wider scopes.  For example, if we change the definition of name in the preceding example to the following:
```
        hello [=
            local name = "World"
 
            [| <p>Hello, [= name; =].</p> |]
        =]
```

then instantiating ```hello``` will still work, but instantiating ```good_night``` will fail, because ```name``` is no longer visible beyond ```hello```.

###7.3 Sites

The outermost scope of an application is the site level, created by a site definition (a typed definition whose supertype is the built-in type ```site```).  Example:
```
    site hello_world_example [=

        hello [| Hello, world. |]

    =]
```

Site definitions have special properties that set them apart from other definitions:
* They need not have a parent definition -- all other definitions must have a parent
* They contain no constructions of their own, only child definitions
* They must be defined with a code block, not a data block
* They do not create a new type, so you cannot use the site name as the supertype in a typed definition
* They may be divided into multiple parts

The ability to divide a site into parts allows a site to be defined across multiple source files.  The Bento compiler concatenates together all the code blocks from site definitions of a given name, resulting in the equivalent of one big code block.

Normally the outermost definition in a Bento source file is a site definition.  If the source file does not begin with an explicit site definition, its contents are presumed to belong to a special unnamed site called the default site.

##7. Types

Bento has the notion of type, which is a named category of data.  But types are optional.  A definition may be either typed or untyped.  A typed definition asserts that the data resulting from its instantiation belongs to a particular category.  An untyped definition makes no such assertion.

###7.1 Primitive Types

The definitions shown up to now, consisting of a name and an implementation, are all untyped.  A typed definition has an additional component, a type name, which precedes the definition name:
```
    string hello [|
        Hello, world.
    |]
```

This definition declares the type to be ```string```, which is a primitive type.  Primitive types are types that are built into the language and are not explicitly defined in any Bento code.  Primitive types include standard types commonly found in programming languages, with the meanings a programmer would expect:
```
    boolean
    byte
    char
    float
    int
    string
```

These may be used in definitions of any format:
```
    boolean flag1 = true

    boolean flag2 [= 
        false;
    =]

    char a = 'A'

    char b [| B |]

    int thirteen = 13

    int fourteen = thirteen + 1

    float fourteen_point_zero = fourteen
```

As the final example above illustrates, the data in a typed definition does not have to itself be of the specified type.  If it is not, Bento will convert it to the specified type upon instantiation, and if it cannot be converted, Bento will output the null value for that type.  The null value for a string is an empty string; for a boolean, false; for a character, the NUL character; and for numeric types, zero.

When Bento instantiates a definition that contains multiple constructions, constructions other than strings are converted to strings before they are concatenated to form output.  In this sense, the string type is the most fundamental.  In fact, it may generally be omitted since untyped definitions and strings both ultimately produce text.  In the end, everything is a string.

###7.2 User-Defined Types

In addition to the built-in types, Bento supports user-defined types.  It is very easy to create a type in Bento.  In fact it is virtually impossible not to create a type, since every definition creates a new type, which may be referenced wherever a type is expected.  This is true even for definitions that are themselves empty or untyped or both -- an untyped definition does not use a type, but it creates one, which another defition can use.  For example:
```
    message [/]

    message hello [| Hello! |]
```

A typed definition, of course, itself creates a new type  Such a sequence of types and subtypes can continue indefinitely:
```
    message [/]

    message hello [| Hello! |]

    hello french_hello [| Bonjour! |]
```

The type created by a typed definition is referred to as a subtype of the original type.  The original type, in turn, is referred to as a supertype of the new type.  In the above example, ```hello``` is a subtype of ```message```, and a supertype of ```french_hello```.  Subtype and supertype relationships are transitive, so ```french_hello``` is a subtype of ```message``` as well as ```hello```, and ```message``` is a supertype of ```french_hello``` in addition to ```hello```.

###7.3 Detecting Types

Bento provides the ```isa``` operator (pronounced "is a") to test whether an entity belongs to a type, i.e., was defined as being of that type or a subtype.  An entity is also considered to belong to its own type, i.e. the type created by its own definition.  Given the code in the preceding example, the following three expressions are all true:
```
    (hello isa message)
    (french_hello isa message)
    (hello isa hello)
```

But these two expressions are false:
```
    (hello isa french_hello)
    (message isa int)
```

The keyword ```type``` makes it possible to query the current type, or the type associated with a name.  In the simplest case, type returns the name of the definition being instantiated.  For example:
```
    hello [=
        type;
    =]

    hello;
```

produces this:
```
    hello
```

But in some cases it may be a subtype:
```
    hello [=
        type;
    =]

    hello greeting [/]

    greeting;
```

produces
```
   greeting
```

because even though ```type``` is referenced in the definition of ```hello```, the definition being instantiated is ```greeting```, which is a subtype of ```hello```.

###7.4 Multiple Supertypes

A definition may have multiple supertypes, delimited by commas.  Example:
```
    hello, french french_hello [| Bonjour! |]
```

In this example, french_hello is a subtype of both hello and french.  Therefore, both of the following expressions are true:
```
    (french_hello isa hello)
    (french_hello isa french)
```


##8. Parameters

A definition can be made more general by allowing some of the information it uses to vary from instantiation to instantiation.  Bento supports this through parameters. 

###8.1 Basics of Parameters

A definition may have zero or more parameters.  Parameters are specified in parentheses immediately following the definition's name and before the definition's implementation.  Any of these parameters can be referenced inside the definition, as if they were definitions.  In fact, parameters are a special kind of definition, one in which only the name and optionally the type are provided with the definition, while the implementation is provided when the definition is instantiated.  Such an implementation is called an argument, and is typically a name, value or expression.

Here is a definition with a single typed parameter:
```
    htag(int level) [|
        <h[= level; =]>
    |]
```

The following example shows a definition with a single parameter and a corresponding instantiation with a single argument:
```
    greetings [=
        hello(nm) [=
            [| <h1>Hello, [= nm; =]</h1> |]
        =]

        hello("World");
    =]
```

In the above, ```hello``` is defined with a single untyped parameter called ```nm```.  ```hello``` is then instantiated with the a single argument, the string "World".  When Bento encounters ```nm``` in the implementation of ```hello```, it identifies ```nm``` as a parameter, and matches the parameter ```nm``` to the argument "World".  The result in this case is therefore the same as if we had written the following instead:
```
    greetings [=
        hello [=
            nm = "World"

            [| <h1>Hello, [= nm; =]</h1> |]
        =]

        hello;
    =]
```

But this works only because in the initial version we instantiated ```hello``` just once.  The following variation would not be able to be rewritten quite so simply:
```
    greetings [=
        hello(nm) [=
            [| <h1>Hello, [= nm; =]</h1> |]
        =]

        hello("World");
    =]
```

Just like other definitions, the name of a parameter must be unique in the scope in which it is defined.  Also, just like other definitions, a parameter is visible to a scope beneath the one in which it is defined, as long as the same name has not been redefined in that scope (or an intervening scope, if it is more deeply nested).  So the following is valid:
```
    greetings(nm) [=
        hello [=
            [| <h1>Hello, [= nm; =]</h1> |]
        =]

        hello;
    =]
```

Multiple parameters are allowed; they are specified in a list, separated by commas:
```
    hello(nm, int level) [|
        <h[= level; =]>Hello, [= nm; =]</h[= level; =]>
    |] 
```

It is legal to omit some or all arguments from an instantiation.  In such a case, the parameter corresponding to the missing argument will evaluate as the null value for its base type (zero for a numeric type, false for a boolean, the NUL character (ASCII 0) for a char, or an empty string for a string or untyped parameter).


Arguments may be supplied to supertypes if the supertype's own definition includes parameters:
```
    hello("World", 1) hello_world [/]
```

Those arguments may include the subtype's parameters:
```
    hello(nm, level) hello_someone(nm, int level) [/]
```

The purpose of this will be discussed in the next chapter.

##8.2 Overloading
  
Bento supports overloading of definitions.  An overloaded definition is one that may be instantiated in more than one way, with differing numbers or types of parameters.  In other languages that support overloading of functions or operators, each overloaded version has its own distinct implementation.  In Bento, however, the different versions are defined using multiple parameter sets with a single implementation.

Overloaded parameter sets are specified in a list, separated by commas.  Each parameter set must differ from the others in either the number or the type of the parameters.  A parameter may appear in more than one parameter set, but it must have the same type in each.  For example:
```
    hello(nm), (nm, int level) [=
         if (level > 0) [| <h[= level; =]> |]
         else           [| <p> |]

         [| Hello, [= nm; =] |]

         if (level > 0) [| </h[= level; =]> |]
         else           [| </p> |]
    |]
```

When an overloaded definition is instantiated, Bento selects the parameter set that most closely matches the arguments specified in the instantiation, and constructs the definition utilizing the arguments as the values for the selected parameter set.  Other parameters receive the null value for their base type.

This presents a problem, however.  If a parameter that is not in every parameter list is null, it's impossible to tell why merely by examining its value  -- it might not be in the selected parameter set, or it might have been passed a null value.  So Bento provides the ```with``` statement, which is a special kind of conditional that tests whether a parameter is in the selected parameter set.

Example: 
```
    which_param(int x), (float y), (z) [=
        with (x) [|
            <p>Called with an int</p>
        |] else with (y) [|
            <p>Called with a float</p>
        |] else [|
            <p>Called with something else</p>
        |]
    =]
```

##9. Inheritance

A typed definition in Bento may make use of the type's definition.  This is called subclassing or inheritance.  The type's definition is called the superdefinition; the definition that uses the type is called the subdefinition.

Bento supports four kinds of inheritance: implementation inheritance, interface inheritance, override inheritance and lateral inheritance.

###9.1 Implementation Inheritance 

With implementation inheritance, the subdefinition incorporates the superdefinition's constructions into its own implementation.

The simplest case of implementation inheritance occurs when a typed definition does not provide its own implementation.  In such a case, the subdefinition simply assumes the superdefinition's implementation.  For example, given the following:
```
    hello [| Hello, World. |]

    hello hello_world [/]

    hello_world;
```

the output would be
```
    Hello, World.
```

Even without supplying its own implementation, a definition can customize the superdefinition's implementation by supplying the supertype with arguments:
```
    hello(nm) [| Hello, [= nm; =]. |]

    hello("World") hello_world [/]
```

with the same output as the preceding example.

Another simple case occurs when the subdefinition provides its own implementation in place of the superdefinition's implementation -- i.e., when the implementation is not inherited:
```
    hello [| Hello, World. |]

    hello another_hello [| How are you, World? |]

    another_hello;
```

This time the output would be
```
    How are you, World?
```
 
The more complex cases occur when the subdefinition extends the superdefinition's implementation rather than replacing it.  There are two such cases, which may be called outside-in and inside-out inheritance.

Outside-in inheritance is analogous to the approach followed by many object-oriented languages.  With this approach, the subdefinition explicitly references the superdefinition.  In Bento, this is accomplished via the built-in name ```super```.  Example:
```
    hello [| Hello, World |]

    hello another_hello [|
         [= super; =].  How are you?
    |]

    another_hello;
```

yielding the following output:
```
    Hello, World.  How are you?
```

Unlike in some languages, super can occur anywhere in the subdefinition's implementation, even multiple times, or in a conditional or a loop:
```
    hello [| Hello, World. |]

    hello two_worlds_meet [=
        [| Two worlds meet on the street.  " |]
        for int i from 0 to 2 [=
            super;
            [| "  " |]
        =]
        [| "  They continue on their way. |]
    =]
      
    two_worlds_meet;
```

The output of the above:
```
    Two worlds meet on the street.  "Hello, World."  "Hello, World."  They continue on their way.
```

This is called outside-in inheritance because the implementation of the definition being instantiated (the subdefinition) wraps the implementation of its superdefinition.  With inside-out inheritance, the superdefinition's implementation wraps the subdefinition's.  This is accomplished using the ```sub``` built-in name.  ```sub``` appears in the superdefinition, and indicates where within the superdefinition's implementation the implementation provided by the subdefinition appears.  Example:
```
    message [=
        [| <h1> |]
        sub;
        [| </h1> |]
    =]

    message hello [| Hello, World |]

    hello;
```

Resulting output:
```
    <h1>Hello, World</h1>
```

Inside-out inheritance turns out to be very useful because for many classes of data items, the unchanging parts are on the outside, while the parts that vary from item to item are on the inside.  For example, it is common for the web pages on a site to have the same header and footer but varying content between the two.

Both outside-in and inside-out inheritance may be extended any number of levels.  Example:
```
    message [=
        sub;
    =]

    message header_message [=
        [| <h1> |]
        sub;
        [| </h1> |]
    =]

    header_message hello [|
        Hello, [= sub; =]
    |]

    hello hello_world [| World |]

    hello_world;
```

with the same result as the previous example.

Bento does not allow mixing inside-out and outside-in inheritance.  If a definition contains a reference to ```sub```, its descendants (subdefinitions, subdefinitions of those subdefinitions, and so on) cannot contain a reference to ```super```; and if a definition contains a reference to ```super```, its ancestors (superdefinition, its superdefinition, ans so on) cannot contain a reference to ```sub```.  It is always legal to have no reference to either.

Neither ```sub``` nor ```super``` takes arguments.  But arguments may be passed to a superdefinition by adding them to the supertype, as described in the previous chapter.  Here is an example that also uses ```super```:
```
    hello(nm) [| Hello, [= nm; =]. |]

    hello("World") hello_world [=
        [| <h1> |]
        super;
        [| </h1> |]
    =]
        
    hello_world;
```

which outputs the following:
```
    <h1>Hello, World.</h1>
```

###9.2 Interface Inheritance

An interface is the set of all child definitions of a definition.  With interface inheritance, a definition implicitly includes all the child definitions of all its ancestors.

If a definition contains a child definition with the same name as a child definition in an ancestor, it takes precedence over the ancestor's child definition.  This is called overriding.

Example:
```
    greetings [=
        hello = "Hello. "
        goodbye = "Bye."
    =]

    greetings night_greetings [=
        goodbye = "Good night."
    =]
    
    night_greetings.hello;
    night_greetings.goodbye;
```

The first definition creates a type called ```greetings```, whose interface consists of two definitions, ```hello``` and ```goodbye```.  The second definition is a subdefinition of ```greetings``` called ```night_greetings```, which overrides just one of these, the definition for ```goodbye```.  Because of interface inheritance, the non-overriden child definition in ```greetings``` (```hello```) is also effectively a child of ```night_greetings```, making ```night_greetings.hello``` a valid reference.  The result of the two instantiations above is the following:
```
    Hello. Good night.
```

###9.3 Override Inheritance

With override inheritance, a child definition may incorporate the implementation of the definition it overrides into its own implementation.  This is accomplished by the special supertype ```this```, which indicates that the supertype is the type being overridden, which in turn makes the overridden type available via the ```super``` keyword, just like any supertype.

Example:
```
    greetings [=
        hello = "Hello."
    =]
    
    greetings bold_greetings [=
        this hello [=
            [| <b> |]
            super;
            [| </b> |]
        =]
    =]

    bold_greetings.hello;
```

which produces:
```
    <b>Hello.</b>
```

###9.5 Lateral Inheritance

Bento supports a kind of inheritance which works outside the standard implementation inheritance chain defined by the ```super``` or ```sub``` keywords.


###9.4 Multiple Inheritance

As noted earlier, a definition may have multiple supertypes.  In such a case a definition may inherit from more than one supertype, which is commonly known as multiple inheritance.  But whether and exactly how this works depends on the kind of inheritance.

For implementation inheritance, the system selects the first elegible definition in the list of superdefinitions to be the superdefinition for implementation purposes.  A definition is elegible if it has a parameter list that matches the arguments supplied at runtime, and has an inheritance keyword that is compatible with the definition being instantiated.

For interface inheritance, all child definitions of all superdefinitions are available.  When resolving a reference to a child in a superdefinition, Bento looks first at the implementation superdefinition (the one selected by the logic for implementation inheritance); if it has a child of the given name, it is selected.  If it does not, then Bento searches the superdefinitions in the order they are listed.

In the case of lateral inheritance, all lateral superdefinitions (ones containing the ```next``` keyword) are implemented, in the order they appear.

The single exception to multiple inheritance is override inheritance, which allows only a single superdefinition, the keyword ```this```.


##10. Collections

A collection is a definition which associates a name with multiple values which may be referenced individually using an index.  Bento supports two kinds of collections, arrays and tables.


###10.1 Arrays

In an array, the values exist in a sequence, and individual values are referenced using an integer index, indicating the zero-based position of the value in the sequence.  Arrays and array indexes are denoted by ```[``` and ```]``` (square brackets).  Example:
```
    elements[] = [ "Earth", "Fire", "Wind" ]

    elements[0];
    elements[2];
    " and ";
    elements[1];
```

which yields the following:
```
    Earth Wind and Fire
```

The values in an array may be any valid instantiation or expression.  Array indexes, as well, may be any valid instantiation or expression.  Example:
```
    MINUTE = "minute"
    HOUR = "hour"

    time_units[] = [ MINUTE, ("half " + HOUR), HOUR ]
    
    for int i from 0 to 3 [=
        time_units[2 - i];
        "... ";
    =]
```

Result:
``` 
    hour... half hour... minute...
```

Arrays may be typed.  In such case the array indicator may go either with the type or the name.  The following two integer array definitions are equivalent:
```
    int lengths[] = [ 60, 60, 24, 7 ]

    int[] lengths = [ 60, 60, 24, 7 ]
```

###10.2 Tables

In a table, the values exist as a set of key-value pairs, and individual values are referenced using a string index, also called a key.  In other languages, the equivalent of a Bento table is sometimes called an associative array, map or hashtable.  Tables and table indexes are denoted by ```{``` and ```}``` (curly braces).  Example:
```
    heavenly_bodies{} = { "Earth": "planet", "Luna": "moon", "Sol": "star" }

    heavenly_bodies{"Sol"};
    "dust and ";
    heavenly_bodies{"Luna"};
    "beams";
```

which generates
```
    stardust and moonbeams
```

Like arrays, tables may be typed, and the table indicator may go either with the type or the name.  In typed tables, the type applies only to the value part of the name-value pairs.  The keys are always strings.  Example:
```
    int{} lengths = { "minute": 60, "hour": 60, "day": 24, "week": 7 }
```

###10.3 Dynamically Generated Collections

Bento allows collections to be wholly or partially generated by logic rather than having to separately specify each element.  This ability, called array comprehension in some languages, is accomplished via special forms of logical constructions.



##11. State

Most computation requires modifying and reading state information, whether it be intermediate results, user input, retrieved data, loop indexes, flags, etc.  Bento supports the handling of state information, but in a unique way.


###11.1 Caching

Traditional programming languages manage state using variables and assignment operators.  The model underlying the traditional approach is based on memory: variables represent chunks of memory, generally implemented as relative locations within a larger block (the heap or stack) but conceptually fixed; assignment operators represent the modification of the contents of such memory locations.

Bento follows a different approach.  There are no variables or assignment operators.  Instead, Bento manages state by caching results in a controlled manner.  According to this model, when a definition is instantiated, by default the results are maintained for possible reuse by further constructions in the same scope that reference the same definition.  Example:
```
    say_hello [=
        hello(nm) [| Hello, [= nm; =]. |]

        hello("world");
        "  I repeat, ";
        hello;
    =]

    say_hello;
```

resulting in 
```
     Hello, world.  I repeat, Hello, world.
```

In the above example, ```hello``` is referenced by two constructions in the same scope.  When Bento encounters the first, it looks up the definition for ```hello`` and instantiates it.  It also saves the generated data in a local cache associated with the current scope.  When Bento encounters the second construction of ```hello```, it retrieves the cached data rather than regenerating it.  In this way, child definitions can act in effect as local variables for the parent's implementation.

However, this may not always be desirable.  For example, suppose the programmer in the above example wishes for ```hello``` to behave differently if no name is passed to it:
```
    say_hello [=
        hello(nm) [=
            if (nm) [| Hello, [= nm; =]. |]
            else    [| Hello! |]
        =]

        hello("world");
        "  I repeat, ";
        hello;
    =]

    say_hello;
```

This time caching may work contrary to the programmer's purpose.  If the programmer is expecting the second construction of hello to be handled by the new code, she will be disappointed, because the second construction will retrieve the cached result of the first construction.  Caching may be avoided, however, by adding a durability modifier to the definition.  The modifier in this case is ```dynamic```, and is used as follows:
```
    say_hello [=
        dynamic hello(nm) [=
            if (nm) [| Hello, [= nm; =]. |]
            else    [| Hello! |]
        =]

        hello("world");
        "  I repeat, ";
        hello;
    =]

    say_hello;
```

The ```dynamic``` modifier instructs Bento to regenerate the output every time, rather than looking for cached output.  So, with the modifier in place, the result will be:
```
     Hello, world.  I repeat, Hello!
```

In general, if you want a definition to behave more like a traditional function, you should use the ```dynamic``` modifier, while if you want it to behave more like a variable, you should avoid the ```dynamic``` modifier and allow Bento to cache the value.

###11.2 Dynamic Instantiation

Bento allows you to combine caching and dynamic behaviors, specifying dynamic behavior only for selected instantiations of a definition.  This is accomplished by using ```(:``` and ```:)``` instead of ```(``` and ```)``` for the argument list.   In the following example, we have removed the dynamic modifier from ```hello```, and instead used the dynamic instantiation form where we want dynamic behavior.
```
    say_hello [=
        hello(nm) [=
            if (nm) [| Hello, [= nm; =]. |]
            else    [| Hello! |]
        =]

        hello("world");
        "  I repeat, ";
        hello;
        "  Let me add, ";
        hello(: "moon" :);
        "  In short, ";
        hello(::);
    =]

    say_hello;
```

resulting in:
```
     Hello, world.  I repeat, Hello, world.  Let me add, Hello, moon.  In short, Hello!
```

By default, cached values are cleared when a scope is exited.  Consider the following:
```
    say_hello [=

        hello(nm) [| Hello, [= nm; =]. |]

        hello("world");
        "  ";
        hello(:"moon":);
        "  ";
    =]

    say_hello;
    say_hello;
```

which yields:
```
    Hello, world.  Hello, moon.  Hello, world.  Hello, moon.
```

Instantiating ```say_hello``` a second time returns the same output as the first time because there is no cached value of ```hello``` when the scope is re-entered.  If cached values were not cleared, then ```hello``` would still have the cached value "moon" on re-entry.

###11.3 The Identity Pattern

Bento's caching system makes it possible to do virtually anything in Bento that can be done with mutable variables in other languages.  Indeed, mutable variables are such a natural fit to certain kinds of operations that mimicking them in Bento is a reasonable thing to do.  Consider the problem of counting the members of a set with some testable property.  A natural solution is to employ a loop, a test and a counting variable:
```
    int count_with_some_property [=
        int num(int n) = n

        for x in some_array [=
            if (some_test(x)) [=
                eval(num(: num + 1 :));
             =]
        =]

        num;
    =]
``` 

The example above illustrates a common Bento pattern for storing values, the identity definition pattern, which may be written more generally as follows:
```
    vartype varname(vartype x) = x
```

This code mimics a variable named ```varname``` of type ```vartype```.  Like a variable, an identity definition associates a name with a value.  But unlike variables as usually implemented, this definition does not allocate any memory.  Rather, it associates a name with a passed value, and depends on caching to store the value.

###11.4 Session Caching

As explained in an earlier chapter, the outermost scope of an application is the site level scope, which is a scope created by a site definition.  Caching at this level has a special property: the cache persists through a session.  This means that if a top-level definition (i.e., one at the site level) is instantiated in a response, its value is available to subsequent responses.  Example:
```
    site hello_world_example [=

        hello(nm) [| Hello, [= nm; =]. |]

        first_response [=
            hello(world);
        =] 

        second_response [=
            hello;
        =]
    =]
```
    
Because ```hello``` is a top-level definition, it is cached in the session.  Assume ```first_response``` is instantiated as part of the response to the user's first request, and ```second_response``` is instantiated as part of the response to the user's second request.  Then, because of session caching, instantiating ```hello``` in ```second_response``` will retrieve the cached value generated by the instantiation of ```hello(world)``` in ```first_response```, namely:
```
    Hello, world. 
```

Thus, definitions at the site level act like session variables.

###11.5 Static and Global Definitions

If you do want the cached value preserved beyond its scope, you can achieve this by adding the ```static``` or ```global``` modifier to the definition.  ```global``` instructs Bento to use the cached value for all references in all scopes.  Here is the previous example, with hello modified to be global:
```
    say_hello [=

        global hello(nm) [| Hello, [= nm; =]. |]

        hello("world");
        "  ";
        hello(:"moon":);
        "  ";
    =]

    say_hello;
    say_hello;
```

This time the output is as follows:
```
    Hello, world.  Hello, moon.  Hello, moon.  Hello, moon.
```

Like ```global```, the ```static``` modifier makes the cached value available in all scopes.  In addition, ```static``` makes a definition immutable; Bento constructs the definition just once, and attempts to cache a different value for the definition via dynamic instantiation are ignored.


##12 Advanced State Management

###12.1 Object Caching

The default caching behavior in Bento is sufficient for many tasks, but for some purposes it's too limiting.  A prime example of this occurs when programming in an object-oriented style.  In object-oriented programming, an object often encapsulates a number of properties.  This is naturally expressed in Bento through child definitions -- the parent definition represents the object, and the child definitions represent the properties.  For example:
```
    greeting(nm, msg) [=
        message = msg
        name = nm

        message;
        ", ";
        name;
        "!";
    =]
```

This defines an object called ```greeting``` that has two properties, ```message``` and ```name```.  But ```message``` and ```name``` are definitions, not values.

Bento supports caching of child definitions this via keep directives.  A keep directive is an optional prefix to a definition that begins with the ```keep``` keyword.

The simplest form is just the keyword ```keep``` by itself.  When prefixed to a definition, Bento will cache the instantiated value whenever and wherever the parent is cached.  For example:
```
    hello(nm, lang) [=
        keep: name = nm 
        keep: language = lang

        if (language == "French") [| Bonjour |]
        else                      [| Hello |]

        ", ";
        name;
        "!";                
    =]

    hello h = hello("Jacques", "French");

    h;
    "<br>Name: ";
    h.name;
    "<br>Language: ";
    h.language;
```

which yields the following output:
```
    Bonjour, Jacques!
    Name: Jacques
    Language: French
```

The ```keep``` keyword supports various options to further customize and control caching; these are described in the following sections.

###12.2 Cache Exposure



###12.3 Cache Aliasing

Bento allows a program to explicitly specify the name with which a definition's results are cached.  , a f  ```keep as``` specifies the name by which a definition's results are cached (cache aliasing).  This allows

```keep by``` makes it possible to dynamically generate the name by which a definition's results are cached (cache by key).

Finally, ```keep in``` allows the programmer to specify the table in which a definitions's results are cached (cache exposure).

Exposing and controlling Bento's caching mechanism in this way forms the basis of a number of advanced techniques for managing state.  It also carries the potential of seamlessly integrating Bento's caching with external persistence mechanisms such as databases.


###11.8 Context

A scope always exists in a context.  This context is conceptually a stack, with each layer in the stack consisting of a scope, an argument-parameter mapping and a cache.  When Bento instantiates a definition, a new layer is pushed on to the stack.  When the instantiation is complete, the layer is popped off the stack.  In between, as instantiations nested within are evaluated, further layers are pushed and popped.  The values resulting from these instantiations are cached in the current layer, as explained above.

These contexts have two properties that are relevant to a Bento application.  One is that they are threadsafe; that is, Bento can construct many responses simultaneously, each one with its own context.  The second is that a context encapsulates the entire application state for a thread.

Bento supports direct access to provides a special keyword, ```here```, provides the mechanism for capturing a continuation; and another keyword, continue, resumes a continuation.



##12. Core Definitions

##13. External Definitions

