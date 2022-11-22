package rocks.zipcode;

import java.util.ArrayList;
import java.util.Iterator;

public class Compiler {
    private Boolean DEBUG = false;

    /**
     * Main compile method.
     */
    public String compile(String input) {
        ArrayList<Token> tokens;
        
        try {
            // phase one: break input into list of tokens
            tokens = tokenizer(input);

            // phase two: create an abstract syntax tree (AST) from tokens.
            Ast ast = parser(tokens);
            
            // phase three: visit each node of AST and emit code 
            String output = codeGenerator(ast);
            
            return output; // is the Zee VM code as a string.

            } catch (Exception e) {
                System.err.println(";; Error in input: "+e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * tokenizer - takes a string of input, the source of the program
     * and produces program as a list of Tokens. This can also be referred to as
     * the `scanner` or the `lexer`.
     * @param input
     * @return ArrayList<Token>
     * @throws Exception
     */
    private ArrayList<Token> tokenizer(String input) throws Exception {
        int current = 0;
        // use current as index into input string.
        // notice that `ch` is NOT a `char`, it is a String.
        // this is just a design choice.


        ArrayList<Token> tokens = new ArrayList<>();

        while (current < input.length()) {

            String ch = String.valueOf(input.charAt(current));

            if (ch.equals("(")) {
                tokens.add(new Token(TokenType.paren, "("));
                current++;
                continue;
            }
            if (ch.equals(")")) {
                tokens.add(new Token(TokenType.thesis, ")"));
                current++;
                continue;
            }
            if (ch.isBlank()) {
                current++;
                continue;
            }
            if (ch.matches("[0-9]")) {
                String value = "";
                while (ch.matches("[0-9]")) {
                    value = value + ch;
                    current++;
                    ch = String.valueOf(input.charAt(current));
                }
                tokens.add(new Token(TokenType.number, value));
                continue;
            }
            if (ch.equals("\"")) {
                String value = "";
                while (!ch.equals("\"")) { // NB !
                    value = value + ch;
                    current++;
                    ch = String.valueOf(input.charAt(current));
                }
                ch = String.valueOf(input.charAt(current));
                tokens.add(new Token(TokenType.string, value));
                continue;
            }
            if (ch.matches("[a-zA-Z]")) {
                String value = "";
                while (ch.matches("[a-zA-Z]")) {
                    value = value + ch;
                    current++;
                    ch = String.valueOf(input.charAt(current));
                }
                tokens.add(new Token(TokenType.name, value));
                continue;
            }
            throw new Exception(";; Illegal character in input.");
        }

        if (DEBUG) printTokens(tokens);

        return tokens;
    }
    
    /**
     * parser - takes a list of Tokens and produces an Abstract Syntax Tree (AST) data
     * structure.
     * @param tokens
     * @return Ast tree
     * @throws Exception
     */
    private Ast parser(ArrayList<Token> tokens) throws Exception {
        Ast root = new Ast(AstType.program, null);
        Iterator<Token> tokenIterator = tokens.iterator();
        Token token = null;
        while (tokenIterator.hasNext()) {
            root.params.add(this.walk(token, tokenIterator));
        }
        return root;
    }

    // used to collect output code during code generation phase.
    private StringBuilder outputCode; 

    /**
     * codeGenerator - takes an Abstract Syntax Tree and produces assembly code for
     * the Zee virtual machine. Uses a `VISITOR` pattern as you traverse the tree. See
     * AstType for visitor routines, enter and exit for each AstType node type.
     * @param ast
     * @return String of output code
     */
    private String codeGenerator(Ast ast) {
        // For debugging, print out the AST received.
        if (DEBUG) dumpAST(ast);

        // using an instance varaable to capture emitted code.
        // this is vaguely distateful. WHY??
        outputCode = new StringBuilder();
        visitAndEmit(ast); // emitCode appends code to the outputCode
        return outputCode.toString();
    }

    /**
     * these are types of Tokens in the language
     */
    enum TokenType {
        paren,
        thesis,
        name,
        number,
        string
    }
    /**
     * Token of the language
     */
    class Token {
        public TokenType type;
        public String value;
        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    /**\
     * AstType are the nodes in the AST.
     * the two methods associated with each type are used to generate
     * code in the 3rd phase of the compiler.
     */
    enum AstType {
        program { // visitor methods
            public String enter(Ast node, Ast parent) {
                return ";; Begin program code\n\t\tSTART";
            }
            public String exit(Ast node, Ast parent) {
                return ";; exit program\n\t\tHALT";
            }
        },
        callexpression { // visitor methods
            public String enter(Ast node, Ast parent) {
                return "";
            }
            public String exit(Ast node, Ast parent) {
                return "\t\t" + node.value.toUpperCase();
            }
        },
        numberliteral { // visitor methods
            public String enter(Ast node, Ast parent) {
                return "";
            }
            public String exit(Ast node, Ast parent) {
                return "\t\tPUSH #"+ node.value;
            }
        },
        stringliteral { // visitor methods
            public String enter(Ast node, Ast parent) {
                return "";
            }
            public String exit(Ast node, Ast parent) {
                return "";
            }
        };

        abstract String enter(Ast node, Ast parent);
        abstract String exit(Ast node, Ast parent);
    }

    /**
     * Ast is an object within the Abstract Syntax Tree. Each one represents
     * a segment of the code in the program.
     */
    class Ast {
        public AstType type;
        public String value;
        public ArrayList<Ast> params;
        Ast(AstType type,
            String value) {
                this.type = type;
                this.value = value;
                this.params = new ArrayList<>();
            }
    }

    /**
     * walk - a routine that recursively assembles the AST based on each token 
     * in the tokenlist from the tokenizer.
     * @param token
     * @param tokens
     * @return
     * @throws Exception
     */
    private Ast walk(Token token, Iterator<Token> tokens) throws Exception {
        if (tokens.hasNext()) {
            token = tokens.next();
        } 

        if (token.type == TokenType.number) {
            return new Ast(AstType.numberliteral, token.value);
        }
        if (token.type == TokenType.string) {
            return new Ast(AstType.stringliteral, token.value);
        }
        if (token.type == TokenType.paren) {
            if (tokens.hasNext()) {
                token = tokens.next();
            }

            Ast node = new Ast(AstType.callexpression, token.value);

            while (token.type != TokenType.thesis) {
                Ast t = walk(token, tokens);
                if (t != null) {
                    node.params.add(t);
                } else break;
            }
            return node;
        }
        if (token.type == TokenType.thesis) {
            return null;
        }
        throw new Exception(";; UNKNOWN TOKEN..."+token.value);
    }

    /**
     * emitCode - attachs code passed in to a stringbuilder instance variable
     * in the compiler.
     * @param code
     */
    private void emitCode(String code) {
        //System.err.println(code);
        outputCode.append(code+"\n");
    }

    /**
     * traversal routines. careful here, this is how you traverse an AST
     * and generate code from it.
     * It is done recursively, which is one of the few places recursion is
     * generally accepted.
     * At each node, you invoke the AST's enter/exit routines to emit code.
     * 
     */

    private void visitAndEmit(Ast ast) {
        traverseNodeAndEmit(ast, null);
    }

    private void traverseList(ArrayList<Ast> list, Ast parent) {
        for (Ast child : list) {
            traverseNodeAndEmit(child, parent);
        }
    }

    private void traverseNodeAndEmit(Ast node, Ast parent) {
        emitCode(node.type.enter(node, parent));

        if (node.type == AstType.program) {
            traverseList(node.params, node);
        }
        if (node.type == AstType.callexpression) {
            traverseList(node.params, node);
        }
        if (node.type == AstType.numberliteral) ;
        if (node.type == AstType.stringliteral) ;

        emitCode(node.type.exit(node, parent));
    }

    /**
     * these routine each "dump", or print to std err,
     * the data structure so you can see what's what at each phase.
     * 
     */

    private void dumpAST(Ast ast) {
        System.err.println(";; BEGIN Ast Dump");
        printAST(ast);
        System.err.println(";; END Ast Dump");

    }
    private void printAST(Ast ast) {
        System.err.println(";; "+ast.type.toString()+": "+ast.value);
        for (Ast e : ast.params) {
            printAST(e);
        }
    }

    private void printTokens(ArrayList<Token> tts) {
        System.err.println(";; BEGIN Token List Dump");
        for (Token t : tts) {
            System.err.println(";; "+t.type+":"+t.value);
        }
        System.err.println(";; END Token List Dump");
    }

}
