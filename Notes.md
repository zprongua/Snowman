# Structural Notes on the Compiler

*these are adapted (well, copied/borrowed/stolen) from https://github.com/jamiebuilds/the-super-tiny-compiler/blob/master/the-super-tiny-compiler.js

THis compiler is very small and pretty simple. It shows some of the _big ideas_ used when describing the building of such tools.

We're going to compile some lisp-like function calls into a small VM's _language_ which a stack-centric evaluation of integers.


If you are not familiar with one or the other. I'll just give you a quick intro.

If we had two functions `add` and `subtract` they would be written like this:

```
                 LISP                      ZeeVM code

  2 + 2          (add 2 2)                 PUSH 2; PUSH 2; ADD
  4 - 2          (subtract 4 2)            PUSH 4; PUSH 2; SUBTRACT
  2 + (4 - 2)    (add 2 (subtract 4 2))    PUSH 2; PUSH 4; PUSH 2; SUBTRACT; ADD
```

Easy peezy right?

Well good, because this is exactly what we are going to compile. While this
is neither a complete LISP or C syntax, it will be enough of the syntax to
demonstrate many of the major pieces of a modern compiler.


Most compilers break down into three primary stages: Parsing, Transformation,
and Code Generation

1. *Lexical Analysis* takes the raw source code text and splits 
   it apart into things called tokens by a tokenizer (or lexer).

2. *Parsing* is taking raw code and turning it into a more abstract
   representation of the code.

3. *Code Generation* takes the transformed representation of the code and
   turns it into new code.
/


## Parsing & Lexing

Parsing typically gets broken down into two phases: Lexical Analysis and
Syntactic Analysis. So think of them as a combined thing, even though are describing
this compiler as having three phases.

1. *Tokenizing* (or Lexing) is taking the text of the source code and
   analyzing it into a data structure which identifies all the lexical
   pieces of the program's source code.


   Tokens are an array of tiny little objects that describe an isolated piece
   of the syntax. They could be numbers, labels, punctuation, operators, keywords,
   whatever.

2. *Syntactic Analysis* takes the tokens and reformats them into a
   representation that describes each part of the syntax and their relation
   to one another. This is known as an intermediate representation or
   Abstract Syntax Tree and is usually expressed as a tree data structure.

   An Abstract Syntax Tree, or AST for short, is a deeply nested object that
   represents code in a way that is both easy to work with and tells us a lot
   of information.

For the following syntax:
```
  (add 2 (subtract 4 2))
```
__Tokens__ might look something like this:

```
  [
    { type: 'paren',  value: '('        },
    { type: 'name',   value: 'add'      },
    { type: 'number', value: '2'        },
    { type: 'paren',  value: '('        },
    { type: 'name',   value: 'subtract' },
    { type: 'number', value: '4'        },
    { type: 'number', value: '2'        },
    { type: 'paren',  value: ')'        },
    { type: 'paren',  value: ')'        },
  ]
```
And an __Abstract Syntax Tree (AST)__ might look like this:

```
  {
    type: 'Program',
    body: [{
      type: 'CallExpression',
      name: 'add',
      params: [{
        type: 'NumberLiteral',
        value: '2',
      }, {
        type: 'CallExpression',
        name: 'subtract',
        params: [{
          type: 'NumberLiteral',
          value: '4',
        }, {
          type: 'NumberLiteral',
          value: '2',
        }]
      }]
    }]
  }
```

## Traversal

In order to navigate through all of these nodes in an AST, we need to be able to
_traverse_ through them. This traversal process goes to each node in the AST
depth-first.

  {
    type: 'Program',
    body: [{
      type: 'CallExpression',
      name: 'add',
      params: [{
        type: 'NumberLiteral',
        value: '2'
      }, {
        type: 'CallExpression',
        name: 'subtract',
        params: [{
          type: 'NumberLiteral',
          value: '4'
        }, {
          type: 'NumberLiteral',
          value: '2'
        }]
      }]
    }]
  }

So for the above AST we would go:

  1. Program - Starting at the top level of the AST
  2. CallExpression (add) - Moving to the first element of the Program's body
  3. NumberLiteral (2) - Moving to the first element of CallExpression's params
  4. CallExpression (subtract) - Moving to the second element of CallExpression's params
  5. NumberLiteral (4) - Moving to the first element of CallExpression's params
  6. NumberLiteral (2) - Moving to the second element of CallExpression's params

If we were manipulating this AST directly, instead of creating a separate AST,
we would likely introduce all sorts of abstractions here. But just visiting
each node in the tree is enough for what we're trying to do.

The reason I use the word "visiting" is because there is this pattern of how
to represent operations on elements of an object structure.

## Visitors

The basic idea here is that we are going to create a “visitor” object that
has methods that will accept different node types. This is an important code pattern, 
one used to traverse a data structure, perhaps doing something at each node 
(or not as needed).

It also givrs you the opportunity to something _before_ you traverse lower in the tree, or
_after_ you have traversed a node's subtree, and before you return the parent node. 
(Yes, this is tricky stuff.)

```
someNodeType { // visitor methods
    public String enter(Ast node, Ast parent) {
        // work *before* you traverse the subtree
        return "";
    }
    public String exit(Ast node, Ast parent) {
        // work *after* you traverse the subtree
        return "";
    }
}
```
When we traverse our AST, we will call the methods on this visitor whenever we
"enter" a node of a matching type.

In order to make this useful we will also pass the node and a reference to
the parent node.

```
abstract String enter(Ast node, Ast parentNode)
```
However, there also exists the possibility of calling things on "exit". Imagine
our tree structure from before in list form:

```
  - Program
    - CallExpression
      - NumberLiteral
      - CallExpression
        - NumberLiteral
        - NumberLiteral
```

As we traverse down, we're going to reach branches with dead ends. As we
finish each branch of the tree we "exit" it. So going down the tree we
"enter" each node, and going back up we "exit".

```
  -> Program (enter)
    -> CallExpression (enter)
      -> Number Literal (enter)
      <- Number Literal (exit)
      -> Call Expression (enter)
         -> Number Literal (enter)
         <- Number Literal (exit)
         -> Number Literal (enter)
         <- Number Literal (exit)
      <- CallExpression (exit)
    <- CallExpression (exit)
  <- Program (exit)
```


## Code Generation

The final phase of a compiler is code generation. Sometimes compilers will do
things that overlap with transformation, but for the most part code
generation just means take our AST and string-ify code back out.

Code generators work several different ways, some compilers will reuse the
tokens from earlier, others will have created a separate representation of
the code so that they can print nodes linearly, but from what I can tell most
will use the same AST we just created, which is what we’re going to focus on.

Effectively our code generator will know how to “print” all of the different
node types of the AST, and it will recursively call itself to print nested
nodes until everything is printed into one long string of code.

In this case, we will be generating code that the ZeeVM virtual machine
can execute to produce an answer. So a small piece of code like this:

```
(print (add 2 (subtract 4 2)))
// print 4 on std out.
```

produces a pseudo-assembly-code of 

```
;; Begin program code
                START
                PUSH #2
                PUSH #4
                PUSH #2
                SUBTRACT
                ADD
                PRINT
                HALT
```

Which is what the ZeeVM machine can take in and execute. It's a "lower level" language,
using a simple stack to hold operands, which can then evaluate to a result.

And that's it! That's all the different pieces of a compiler.

Now that isn’t to say every compiler looks exactly like I described here.
Compilers serve many different purposes, and they might need more steps than
I have detailed.

But now you should have a general high-level idea of what most compilers (like `javac`) look
like. It also helps then to realize what a virtual machine looks like, and that's the point of
ZeeVM.

