# Snowman
a Very Simple compiler written in Java. produces code for the Zee virtual machine.

This is based on the-super-tiny-compiler written in javascript by @jamiebuilds (see [the-super-tiny-compiler](https://github.com/jamiebuilds/the-super-tiny-compiler))

Tranliterated into Java by @xt0fer for zip code wilmington lab on compiler phases and the ideas of scanning and parser into trees (well, ASTs).

It also has only three phases, not four like in the original. 

The first phase is the tokenizer, the second is the parser, and the  third is code generation.

Right now the `snowman language` is very simple.

It has two functions which add or subtract integers.
So the basic idea is 

Evaluating the expression `(add x y)` -> `x + y` 
Evaluating the expression `(subtract x y)` -> `x - y` 
Evaluating the expression `(print x)` -> it'll print x on std out. 



```
(add 2 (subtract 4 2)) 
// leaves 4 as result
```

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

Lines which start with `;;` are comments.

Generated from `Snowman` is used to demonstrate the `Zee Virtual Machine` (zeeVM). See https://github.com/xt0fer/ZeeVM


```
(add 1 1)
(add 45 (add (subtract (add 21 21) 13) 23))
```

are examples of other possible programs in this language.

## What you need to do

Once you have read the code and get an idea of what it does, you need to add the following concepts to it:

- now, you add `multiply` and `divide`
  - `(multiply x y)` -> `x * y`
  - `(divide x y)` -> `x / y` (integer result)
  - `(mod x y)` -> `x % y` (integer remainder or modulo)
- how about comments like "// comments..." (from // to EOL)

- simple comparisons `EQ` `NE` `ZE` `LT` and `GE`
- a `VAR` function for variables??

## Furture Lab Ideas

Below, where you see "VAR" think "let". Uppercase, lowercase should not matter.

### Variables
Integer values mapped to names.
```
;; declare, initialize vars
(VAR X 5) -> x = 5;
(VAR Y (add 5 8)) -> y = 5 + 8;
;; use vars
(add X Y) -> (x + y) -> 5 + 13 -> 18;
```

Could then
```
(VAR TRUE 1)
(VAR FALSE 0)
```

might also 

```
(LT 4 5) -> 1
(GE 4 5) -> 0
(GE 3 3) -> 1
(GE 5 4) -> 1
(EQ 5 5) -> 1
(NE 4 5) -> 1
(NE 3 3) -> 1
(EQ 1 x) -> isTrue
(EQ 0 x) -> isFalse
```

and also
```
(VAR TRUE 1)
(IF TRUE (VAR x (ADD 4 5)))
```
which might be
*if (true) { x = 4 + 5; }*

or perhaps 
```
(WHILE (LT X 10) (VAR X (SUBTRACT X 1)))
```
which might be thought of as
*while (x < 10) { x = x - 1; }*

and what about *lambda*, how would add it to this language?

```
(LAMBDA (X Y) (...))
(λ (X Y) (...))

(VAR F (LAMBDA (X) (ADD X + 123)))

(PRINT (F 456)) ;; which would print 579...
```

which then let the programmer create functions within the language.

## Late News from ZeeVM v1.2

Currently the ZeeVM (1.2) supports LABELs and LOAD/STORE of alpha-named variables.

```
        PUSH #9
        STORE X
        ;; and
        LOAD X
        PRINT
        ;; should display 9 in output.
```

and labels and jumps like this infinite loop.

```
        LABEL FOO
        ;; bunch of code
        JUMP FOO
```

*Gee?* so how do I avoid using JUMP (which is really a GOTO, isn't it?)?

Well, there is also conditional jump called `JMPZ FOO` which pops the stack and if the value there is equal to zero, the jump happens (jumping to where FOO is declared with a LABEL). Otherwise, the next instruction after the jump is done.

