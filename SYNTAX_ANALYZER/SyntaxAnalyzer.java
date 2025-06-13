package SYNTAX_ANALYZER;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyntaxAnalyzer {
    private static List<Token> tokens = new ArrayList<>();
    private static int index = 0;

    // Token access methods
    private static Token getNextToken() {
        if (index < tokens.size()) {
            return tokens.get(index++);
        }
        return null;
    }

    private static Token getCurrentToken() {
        if (index < tokens.size()) {
            return tokens.get(index);
        }
        return null;
    }

    private static Token peekToken() {
        return getCurrentToken();
    }

    private static boolean pushBack(int n) {
        index = Math.max(0, index - n);
        return true;
    }

    private static void reset() {
        index = 0;
    }

    // Enhanced error reporting
    private static void reportError(String expected, Token found) {
        System.err.println("SYNTAX ERROR at line " + 
            (found != null ? found.line : "EOF") + 
            ": Expected '" + expected + "' but found '" + 
            (found != null ? found.value : "EOF") + "'");
    }

    // <PS> → <import_statements> <program_body>
    public static boolean PS() {
        System.out.println("Entering <PS> section");
        
        // Parse optional import statements
        if (!import_statements()) {
            return false;
        }
        
        // Parse the main program body (classes including main class)
        if (!program_body()) {
            return false;
        }
        
        // Check for end of input - accept both EOF and $ as end markers
        Token tok = getCurrentToken();
        System.out.println("DEBUG: After parsing, current token is: " + 
            (tok != null ? tok.value + " (" + tok.type + ")" : "null"));
        System.out.println("DEBUG: Current index: " + index + ", Total tokens: " + tokens.size());
        
        if (tok != null && !isEndOfInput(tok)) {
            System.err.println("SYNTAX ERROR: Unexpected token '" + tok.value + 
                "' at line " + tok.line + " after program end");
            return false;
        }
        
        System.out.println("Parsing Successful !!!");
        return true;
    }

    // <import_statements> → <import_st> <import_statements> | ε
    public static boolean import_statements() {
        System.out.println("Entering <import_statements> section");
        
        while (peekToken() != null && peekToken().value.equals("import")) {
            if (!import_st()) {
                return false;
            }
        }
        return true; // ε production - no imports is valid
    }

    // <import_st> → import <qualified_name> <import_tail>
    public static boolean import_st() {
        System.out.println("Entering <import_st> section");
        int startIndex = index;
        
        Token tok = getNextToken();
        if (tok != null && tok.value.equals("import")) {
            if (qualified_name()) {
                if (import_tail()) {
                    return true;
                }
            }
        }
        
        // Restore position if import parsing failed
        index = startIndex;
        return false;
    }

    // <import_tail> → ; | . * ;
    public static boolean import_tail() {
        Token tok = getNextToken();
        if (tok != null) {
            if (tok.value.equals(";")) {
                return true;
            } else if (tok.value.equals(".")) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("*")) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals(";")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // <qualified_name> → ID <qualified_name_tail>
    public static boolean qualified_name() {
        System.out.println("Entering <qualified_name> section");
        
        if (Identifier(peekToken())) {
            getNextToken(); // consume the identifier
            return qualified_name_tail();
        }
        return false;
    }

    // <qualified_name_tail> → ε | . ID <qualified_name_tail>
    public static boolean qualified_name_tail() {
        if (peekToken() != null && peekToken().value.equals(".")) {
            getNextToken(); // consume '.'
            if (Identifier(peekToken())) {
                getNextToken(); // consume identifier
                return qualified_name_tail();
            }
            return false;
        }
        return true; // ε production
    }

    // <program_body> → <class_list>
    public static boolean program_body() {
        System.out.println("Entering <program_body> section");
        return class_list();
    }

    // <class_list> → <class> <class_list> | ε
    public static boolean class_list() {
        System.out.println("Entering <class_list> section");
        
        while (peekToken() != null && First_class(peekToken())) {
            if (!classDecl()) {
                return false;
            }
        }
        return true; // ε production
    }

    // <class> → <class_header> <inheritance> <class_body>
    public static boolean classDecl() {
        System.out.println("Entering <class> section");
        
        if (!class_header()) {
            return false;
        }
        
        if (!inheritance()) {
            return false;
        }
        
        return class_body();
    }

    // <class_header> → <modifiers> class ID
    public static boolean class_header() {
        System.out.println("Entering <class_header> section");
        
        if (!modifiers()) {
            return false;
        }
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("class")) {
            reportError("class", tok);
            return false;
        }
        
        if (!Identifier(peekToken())) {
            reportError("class name (identifier)", peekToken());
            return false;
        }
        getNextToken(); // consume class name
        
        return true;
    }

    // <modifiers> → <modifier> <modifiers> | ε
    public static boolean modifiers() {
        while (peekToken() != null && isModifier(peekToken())) {
            getNextToken(); // consume modifier
        }
        return true; // ε production - no modifiers is valid
    }

    // <inheritance> → extends ID | ε
    public static boolean inheritance() {
        if (peekToken() != null && peekToken().value.equals("extends")) {
            getNextToken(); // consume 'extends'
            if (!Identifier(peekToken())) {
                reportError("class name after extends", peekToken());
                return false;
            }
            getNextToken(); // consume class name
        }
        return true; // ε production
    }

    // <class_body> → { <class_members> }
    public static boolean class_body() {
        System.out.println("Entering <class_body> section");
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("{")) {
            reportError("{", tok);
            return false;
        }
        
        if (!class_members()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("}")) {
            reportError("}", tok);
            return false;
        }
        
        System.out.println("DEBUG: Successfully parsed main method, current index: " + index);
        return true;
    }

    // <class_members> → <class_member> <class_members> | ε
    public static boolean class_members() {
        System.out.println("Entering <class_members> section");
        
        while (peekToken() != null && !peekToken().value.equals("}") && First_class_member(peekToken())) {
            System.out.println("DEBUG: Parsing class member, current token: " + peekToken().value);
            if (!class_member()) {
                return false;
            }
        }
        System.out.println("DEBUG: Exiting class_members, current token: " + 
            (peekToken() != null ? peekToken().value : "null"));
        return true; // ε production
    }

    // <class_member> → <main_method> | <method> | <attribute> | <constructor>
    public static boolean class_member() {
        // Try to parse main method first
        if (isMainMethodStart()) {
            return main_method();
        }
        
        // For now, we'll focus on main method parsing
        // Other members can be added later
        return false;
    }

    // Check if current position starts a main method
    private static boolean isMainMethodStart() {
        int savedIndex = index;
        boolean isMain = false;
        
        // Check for: public static void main
        if (peekToken() != null && peekToken().value.equals("public")) {
            getNextToken();
            if (peekToken() != null && peekToken().value.equals("static")) {
                getNextToken();
                if (peekToken() != null && peekToken().value.equals("void")) {
                    getNextToken();
                    if (peekToken() != null && peekToken().value.equals("main")) {
                        isMain = true;
                    }
                }
            }
        }
        
        // Restore position
        index = savedIndex;
        return isMain;
    }

    // <main_method> → public static void main ( String [] args ) { <method_body> }
    public static boolean main_method() {
        System.out.println("Entering <main_method> section");
        
        // Parse: public static void main
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("public")) {
            reportError("public", tok);
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("static")) {
            reportError("static", tok);
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("void")) {
            reportError("void", tok);
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("main")) {
            reportError("main", tok);
            return false;
        }
        
        // Parse: ( String [] args )
        tok = getNextToken();
        if (tok == null || !tok.value.equals("(")) {
            reportError("(", tok);
            return false;
        }
        
        // Handle both "String[] args" and "String args[]" formats
        tok = getNextToken();
        if (tok == null || !tok.value.equals("String")) {
            reportError("String", tok);
            return false;
        }
        
        // Check for [] before or after args
        tok = peekToken();
        if (tok != null && tok.value.equals("[")) {
            getNextToken(); // consume '['
            tok = getNextToken();
            if (tok == null || !tok.value.equals("]")) {
                reportError("]", tok);
                return false;
            }
            // Now expect args
            if (!Identifier(peekToken())) {
                reportError("parameter name", peekToken());
                return false;
            }
            getNextToken(); // consume args
        } else {
            // Expect args first, then []
            if (!Identifier(peekToken())) {
                reportError("parameter name", peekToken());
                return false;
            }
            getNextToken(); // consume args
            
            tok = getNextToken();
            if (tok == null || !tok.value.equals("[")) {
                reportError("[", tok);
                return false;
            }
            
            tok = getNextToken();
            if (tok == null || !tok.value.equals("]")) {
                reportError("]", tok);
                return false;
            }
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(")")) {
            reportError(")", tok);
            return false;
        }
        
        // Parse method body: { <statements> }
        tok = getNextToken();
        if (tok == null || !tok.value.equals("{")) {
            reportError("{", tok);
            return false;
        }
        
        if (!method_body()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("}")) {
            reportError("}", tok);
            return false;
        }
        
        return true;
    }

    // <method_body> → <statements>
    public static boolean method_body() {
        System.out.println("Entering <method_body> section");
        return statements();
    }

    // <statements> → <statement> <statements> | ε
    public static boolean statements() {
        // For now, just accept empty body (main method with no statements)
        // This can be expanded to handle actual statements
        return true; // ε production
    }

    // Helper method to check if token represents end of input
    private static boolean isEndOfInput(Token tok) {
        if (tok == null) return true;
        
        // Check both value and type for end markers
        return tok.value.equals("EOF") || 
               tok.value.equals("$") || 
               tok.type.equals("EOF");
    }

    // Helper methods
    public static boolean Identifier(Token tok) {
        return tok != null && tok.type.equals("Identifier");
    }

    public static boolean isModifier(Token tok) {
        return tok != null && (tok.value.equals("public") || tok.value.equals("private") || 
                              tok.value.equals("protected") || tok.value.equals("static") ||
                              tok.value.equals("final") || tok.value.equals("abstract"));
    }

    // First set checks
    public static boolean First_class(Token tok) {
        return tok != null && (isModifier(tok) || tok.value.equals("class"));
    }

    public static boolean First_class_member(Token tok) {
        if (tok == null) return false;
        
        // A class member can start with modifiers, or be a main method
        return isModifier(tok) || 
               tok.value.equals("public") || // for main method
               tok.type.equals("Identifier") || // for methods/attributes without explicit modifiers
               tok.value.equals("void") || // for methods
               tok.value.equals("int") || tok.value.equals("String") || // for attributes
               isMainMethodStart(); // check if it's the start of main method
    }

    // Main method with improved token parsing and error handling
    public static void main(String[] args) throws IOException {
        String fileName = args.length > 0 ? args[0] : "tokens.txt";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                line = line.trim();
                
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    if (line.startsWith("<") && line.endsWith(">")) {
                        line = line.substring(1, line.length() - 1);
                        String[] parts = line.split(",", 3); // Split into max 3 parts
                        
                        if (parts.length >= 3) {
                            String lineNum = parts[0].trim();
                            String type = parts[1].trim();
                            String value = parts[2].trim();
                            
                            Token t = new Token(value, type, lineNum);
                            tokens.add(t);
                            System.out.println("Loaded token: " + t.value + " (" + t.type + ") at line " + t.line);
                        } else {
                            System.err.println("Skipping malformed line " + lineCount + ": " + line);
                        }
                    } else {
                        System.err.println("Skipping invalid line format " + lineCount + ": " + line);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineCount + ": " + line + " (" + e.getMessage() + ")");
                }
            }
            
            // Add EOF token if not present (but don't duplicate end markers)
            if (tokens.isEmpty() || !isEndOfInput(tokens.get(tokens.size()-1))) {
                tokens.add(new Token("EOF", "EOF", "-1"));
            }
            
            System.out.println("\n=== Starting Syntax Analysis ===");
            System.out.println("Total tokens loaded: " + tokens.size());
            
            boolean result = PS();
            if (!result) {
                System.err.println("PARSING FAILED!");
            }
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + fileName + " (" + e.getMessage() + ")");
        }
    }

    // Token class
    public static class Token {
        public String value;
        public String type;
        public String line;

        public Token(String value, String type, String line) {
            this.value = value;
            this.type = type;
            this.line = line;
        }

        @Override
        public String toString() {
            return "Token{" + "value='" + value + '\'' + ", type='" + type + '\'' + ", line='" + line + '\'' + '}';
        }
    }
}