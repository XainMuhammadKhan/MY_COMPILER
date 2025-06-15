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

    private static void reportError(String message) {
        Token current = peekToken();
        System.err.println("SYNTAX ERROR at line " + 
            (current != null ? current.line : "EOF") + ": " + message);
    }

    // <PS> → <import_statements> <classes> <main_class>
    public static boolean PS() {
        System.out.println("Entering <PS> section");
        
        // Parse optional import statements
        if (!import_statements()) {
            return false;
        }
        
        // Parse classes (can be empty)
        if (!classes()) {
            return false;
        }
        
        // Parse main class (required)
        if (!main_class()) {
            return false;
        }
        
        // Check for end of input
        Token tok = getCurrentToken();
        if (tok != null && !isEndOfInput(tok)) {
            reportError("End of file", tok);
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
        return true; // ε production
    }

    // <import_st> → import <qualified_name> <import_tail>
    public static boolean import_st() {
        System.out.println("Entering <import_st> section");
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("import")) {
            reportError("import", tok);
            return false;
        }
        
        if (!qualified_name()) {
            reportError("qualified name after import");
            return false;
        }
        
        if (!import_tail()) {
            reportError("';' or '.*'");
            return false;
        }
        
        return true;
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
                    } else {
                        reportError(";", tok);
                        return false;
                    }
                } else {
                    reportError("*", tok);
                    return false;
                }
            } else {
                reportError("';' or '.'", tok);
                return false;
            }
        }
        reportError("';' or '.'", null);
        return false;
    }

    // <qualified_name> → ID <qualified_name_tail>
    public static boolean qualified_name() {
        System.out.println("Entering <qualified_name> section");
        
        if (!Identifier(peekToken())) {
            reportError("identifier", peekToken());
            return false;
        }
        getNextToken(); // consume the identifier
        return qualified_name_tail();
    }

    // <qualified_name_tail> → ε | . ID <qualified_name_tail>
    public static boolean qualified_name_tail() {
        if (peekToken() != null && peekToken().value.equals(".")) {
            getNextToken(); // consume '.'
            if (!Identifier(peekToken())) {
                reportError("identifier after '.'", peekToken());
                return false;
            }
            getNextToken(); // consume identifier
            return qualified_name_tail();
        }
        return true; // ε production
    }

    // <classes> → <class> <classes> | ε
    public static boolean classes() {
        System.out.println("Entering <classes> section");
        
        while (peekToken() != null && First_class(peekToken()) && !isMainClass()) {
            if (!classDecl()) {
                return false;
            }
        }
        return true; // ε production
    }

    // Check if current position is start of main class
    private static boolean isMainClass() {
        int savedIndex = index;
        boolean hasMain = false;
        
        // Skip modifiers
        while (peekToken() != null && isModifier(peekToken())) {
            getNextToken();
        }
        
        // Check for class keyword
        if (peekToken() != null && peekToken().value.equals("class")) {
            getNextToken();
            if (Identifier(peekToken())) {
                getNextToken();
                // Skip inheritance
                if (peekToken() != null && peekToken().value.equals("extends")) {
                    getNextToken();
                    if (Identifier(peekToken())) {
                        getNextToken();
                    }
                }
                // Check if class body contains main method
                if (peekToken() != null && peekToken().value.equals("{")) {
                    hasMain = containsMainMethod();
                }
            }
        }
        
        index = savedIndex;
        return hasMain;
    }

    // Check if current class contains main method
    private static boolean containsMainMethod() {
        int savedIndex = index;
        boolean hasMain = false;
        
        if (peekToken() != null && peekToken().value.equals("{")) {
            getNextToken(); // consume '{'
            
            // Look for main method pattern
            while (peekToken() != null && !peekToken().value.equals("}")) {
                if (isMainMethodStart()) {
                    hasMain = true;
                    break;
                }
                getNextToken();
            }
        }
        
        index = savedIndex;
        return hasMain;
    }

    // <main_class> → <main_method>
    public static boolean main_class() {
        System.out.println("Entering <main_class> section");
        
        // Parse class structure containing main method
        if (!modifiers()) {
            return false;
        }
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("class")) {
            reportError("class", tok);
            return false;
        }
        
        if (!Identifier(peekToken())) {
            reportError("class name", peekToken());
            return false;
        }
        getNextToken(); // consume class name
        
        // Optional inheritance
        if (!inheritance()) {
            return false;
        }
        
        // Class body with main method
        tok = getNextToken();
        if (tok == null || !tok.value.equals("{")) {
            reportError("{", tok);
            return false;
        }
        
        if (!main_method()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("}")) {
            reportError("}", tok);
            return false;
        }
        
        return true;
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
            Token modifier = getNextToken();
            // Validate modifier combinations if needed
            if (!isValidModifier(modifier)) {
                reportError("Invalid modifier: " + modifier.value);
                return false;
            }
        }
        return true; // ε production
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

    // <class_body> → { <attributes> <class_body> | <constructors> <class_body> | <methods> <class_body> | ε }
    public static boolean class_body() {
        System.out.println("Entering <class_body> section");
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("{")) {
            reportError("{", tok);
            return false;
        }
        
        // Parse class members
        while (peekToken() != null && !peekToken().value.equals("}")) {
            if (!class_member()) {
                return false;
            }
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("}")) {
            reportError("}", tok);
            return false;
        }
        
        return true;
    }

    // Parse class members (attributes, constructors, methods)
    public static boolean class_member() {
        Token current = peekToken();
        if (current == null) {
            reportError("class member expected");
            return false;
        }

        // Try to parse different types of class members
        if (isAttribute()) {
            return attribute();
        } else if (isConstructor()) {
            return constructor();
        } else if (isMethod()) {
            return method();
        } else {
            reportError("Invalid class member", current);
            return false;
        }
    }

    // Check if current position is an attribute
    private static boolean isAttribute() {
        int savedIndex = index;
        boolean isAttr = false;
        
        // Skip modifiers
        while (peekToken() != null && isModifier(peekToken())) {
            getNextToken();
        }
        
        // Check for data type
        if (peekToken() != null && isDataType(peekToken())) {
            getNextToken();
            if (Identifier(peekToken())) {
                getNextToken();
                // Check for assignment or semicolon
                if (peekToken() != null && 
                    (peekToken().value.equals("=") || peekToken().value.equals(";"))) {
                    isAttr = true;
                }
            }
        }
        
        index = savedIndex;
        return isAttr;
    }

    // Check if current position is a constructor
    private static boolean isConstructor() {
        int savedIndex = index;
        boolean isCtor = false;
        String className = getCurrentClassName(); // This would need to be tracked
        
        // Skip modifiers
        while (peekToken() != null && isModifier(peekToken())) {
            getNextToken();
        }
        
        // Constructor has same name as class
        if (peekToken() != null && Identifier(peekToken()) && 
            peekToken().value.equals(className)) {
            getNextToken();
            if (peekToken() != null && peekToken().value.equals("(")) {
                isCtor = true;
            }
        }
        
        index = savedIndex;
        return isCtor;
    }

    // Check if current position is a method
    private static boolean isMethod() {
        int savedIndex = index;
        boolean isMethod = false;
        
        // Skip modifiers
        while (peekToken() != null && isModifier(peekToken())) {
            getNextToken();
        }
        
        // Check for return type
        if (peekToken() != null && (isDataType(peekToken()) || peekToken().value.equals("void"))) {
            getNextToken();
            if (Identifier(peekToken())) {
                getNextToken();
                if (peekToken() != null && peekToken().value.equals("(")) {
                    isMethod = true;
                }
            }
        }
        
        index = savedIndex;
        return isMethod;
    }

    // <attributes> → <modifiers> DT ID <exp>
    public static boolean attribute() {
        System.out.println("Entering <attribute> section");
        
        if (!modifiers()) {
            return false;
        }
        
        if (!dataType()) {
            reportError("data type expected");
            return false;
        }
        
        if (!Identifier(peekToken())) {
            reportError("attribute name expected", peekToken());
            return false;
        }
        getNextToken();
        
        // Optional initialization
        if (peekToken() != null && peekToken().value.equals("=")) {
            getNextToken();
            if (!expression()) {
                reportError("expression expected after '='");
                return false;
            }
        }
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals(";")) {
            reportError(";", tok);
            return false;
        }
        
        return true;
    }

    // <constructor> → <constructor_header> <method_body>
    public static boolean constructor() {
        System.out.println("Entering <constructor> section");
        
        if (!constructor_header()) {
            return false;
        }
        
        return method_body();
    }

    // <constructor_header> → <modifiers> ID(<parameters>)
    public static boolean constructor_header() {
        if (!modifiers()) {
            return false;
        }
        
        if (!Identifier(peekToken())) {
            reportError("constructor name expected", peekToken());
            return false;
        }
        getNextToken();
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("(")) {
            reportError("(", tok);
            return false;
        }
        
        if (!parameters()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(")")) {
            reportError(")", tok);
            return false;
        }
        
        return true;
    }

    // <method> → <method_header> <method_body>
    public static boolean method() {
        System.out.println("Entering <method> section");
        
        if (!method_header()) {
            return false;
        }
        
        return method_body();
    }

    // <method_header> → <modifiers> DT ID(<parameters>)
    public static boolean method_header() {
        if (!modifiers()) {
            return false;
        }
        
        // Return type (DT or void)
        if (peekToken() != null && peekToken().value.equals("void")) {
            getNextToken();
        } else if (!dataType()) {
            reportError("return type expected");
            return false;
        }
        
        if (!Identifier(peekToken())) {
            reportError("method name expected", peekToken());
            return false;
        }
        getNextToken();
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("(")) {
            reportError("(", tok);
            return false;
        }
        
        if (!parameters()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(")")) {
            reportError(")", tok);
            return false;
        }
        
        return true;
    }

    // <method_body> → { <MST> }
    public static boolean method_body() {
        System.out.println("Entering <method_body> section");
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals("{")) {
            reportError("{", tok);
            return false;
        }
        
        if (!MST()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("}")) {
            reportError("}", tok);
            return false;
        }
        
        return true;
    }

    // <parameters> → <parameter> <parameter'> | ε
    public static boolean parameters() {
        if (peekToken() != null && !peekToken().value.equals(")")) {
            if (!parameter()) {
                return false;
            }
            return parameter_prime();
        }
        return true; // ε production
    }

    // <parameter'> → ε | , <parameters>
    public static boolean parameter_prime() {
        if (peekToken() != null && peekToken().value.equals(",")) {
            getNextToken();
            return parameters();
        }
        return true; // ε production
    }

    // <parameter> → DT ID
    public static boolean parameter() {
        if (!dataType()) {
            reportError("parameter type expected");
            return false;
        }
        
        if (!Identifier(peekToken())) {
            reportError("parameter name expected", peekToken());
            return false;
        }
        getNextToken();
        
        return true;
    }

    // <MST> → <SST> <MST> | ε
    public static boolean MST() {
        while (peekToken() != null && !peekToken().value.equals("}") && First_SST(peekToken())) {
            if (!SST()) {
                return false;
            }
        }
        return true; // ε production
    }

    // <SST> → <exp>; | <TS>; | <return_st>; | <assign_st>; | <dec>; | <if_st> | <while_st> | <for_st>; | ObjCall
    public static boolean SST() {
    Token current = peekToken();
    System.out.println("Entering SST, current token: " + (current != null ? current : "null"));
    if (current == null) {
        reportError("statement expected");
        return false;
    }

    if (current.value.equals("if")) {
        return if_statement();
    } else if (current.value.equals("while")) {
        return while_statement();
    } else if (current.value.equals("for")) {
        return for_statement();
    } else if (current.value.equals("return")) {
        return return_statement();
    } else if (isDataType(current)) {
        System.out.println("Trying declaration for token: " + current);
        return declaration();
    } else if (Identifier(current)) {
        int savedIndex = index; // Save index for backtracking
        System.out.println("Trying assignment_or_call for token: " + current);
        if (assignment_or_call()) {
            return true;
        }
        index = savedIndex; // Backtrack to try expression
        System.out.println("Backtracked to index " + index + ", retrying expression");
    }

    // Try expression statement
    int savedIndex = index;
    System.out.println("Trying expression statement for token: " + current);
    if (expression()) {
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals(";")) {
            reportError(";", tok);
            index = savedIndex;
            return false;
        }
        System.out.println("Successfully parsed expression statement");
        return true;
    }
    index = savedIndex;

    reportError("invalid statement", current);
    return false;
}    // Simple implementations for statement types
    public static boolean if_statement() {
        Token tok = getNextToken(); // consume 'if'
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("(")) {
            reportError("(", tok);
            return false;
        }
        
        if (!condition()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(")")) {
            reportError(")", tok);
            return false;
        }
        
        if (!loop_body()) {
            return false;
        }
        
        // Optional else
        if (peekToken() != null && peekToken().value.equals("else")) {
            getNextToken();
            if (!loop_body()) {
                return false;
            }
        }
        
        return true;
    }

    public static boolean while_statement() {
        Token tok = getNextToken(); // consume 'while'
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("(")) {
            reportError("(", tok);
            return false;
        }
        
        if (!condition()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(")")) {
            reportError(")", tok);
            return false;
        }
        
        return loop_body();
    }

    public static boolean for_statement() {
        Token tok = getNextToken(); // consume 'for'
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("(")) {
            reportError("(", tok);
            return false;
        }
        
        // F1 - initialization
        if (!F1()) {
            return false;
        }
        
        // F2 - condition
        if (!F2()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(";")) {
            reportError(";", tok);
            return false;
        }
        
        // F3 - increment
        if (!F3()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(")")) {
            reportError(")", tok);
            return false;
        }
        
        return loop_body();
    }

    public static boolean return_statement() {
        getNextToken(); // consume 'return'
        
        // Optional expression
        if (peekToken() != null && !peekToken().value.equals(";")) {
            if (!expression()) {
                return false;
            }
        }
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals(";")) {
            reportError(";", tok);
            return false;
        }
        
        return true;
    }

    public static boolean declaration() {
        if (!dataType()) {
            return false;
        }
        
        if (!Identifier(peekToken())) {
            reportError("variable name expected", peekToken());
            return false;
        }
        getNextToken();
        
        // Optional initialization
        if (peekToken() != null && peekToken().value.equals("=")) {
            getNextToken();
            if (!expression()) {
                reportError("expression expected after '='");
                return false;
            }
        }
        
        Token tok = getNextToken();
        if (tok == null || !tok.value.equals(";")) {
            reportError(";", tok);
            return false;
        }
        
        return true;
    }

 public static boolean assignment_or_call() {
    System.out.println("Entering assignment_or_call, current token: " + peekToken());
    int savedIndex = index; // Save index for backtracking

    // Try parsing as a qualified name for a method call
    if (qualified_name()) {
        Token next = peekToken();
        System.out.println("After qualified_name, next token: " + next);
        if (next != null && next.value.equals("(")) {
            getNextToken(); // consume '('
            if (!args()) {
                System.out.println("args failed, backtracking to index " + savedIndex);
                index = savedIndex;
                return false;
            }
            next = getNextToken();
            if (next == null || !next.value.equals(")")) {
                reportError(")", next);
                index = savedIndex;
                return false;
            }
            next = getNextToken();
            if (next == null || !next.value.equals(";")) {
                reportError(";", next);
                index = savedIndex;
                return false;
            }
            System.out.println("Successfully parsed method call");
            return true;
        }
        System.out.println("No method call, backtracking to index " + savedIndex);
        index = savedIndex; // Backtrack if not a method call
    }

    // Try parsing as an assignment
    if (Identifier(peekToken())) {
        getNextToken(); // consume identifier
        Token next = peekToken();
        System.out.println("Trying assignment, next token: " + next);
        if (next != null && isAssignOperator(next)) {
            getNextToken(); // consume assignment operator
            if (!expression()) {
                reportError("expression expected after assignment operator");
                index = savedIndex;
                return false;
            }
            next = getNextToken();
            if (next == null || !next.value.equals(";")) {
                reportError(";", next);
                index = savedIndex;
                return false;
            }
            System.out.println("Successfully parsed assignment");
            return true;
        }
        System.out.println("No assignment, backtracking to index " + savedIndex);
        index = savedIndex; // Backtrack if not an assignment
    }

    System.out.println("Failed assignment_or_call, backtracking to index " + savedIndex);
    index = savedIndex;
    return false; // Let SST handle error reporting
}
    public static boolean condition() {
        // Simple condition parsing - can be expanded
        return expression();
    }

    public static boolean loop_body() {
        Token tok = peekToken();
        if (tok != null && tok.value.equals(";")) {
            getNextToken();
            return true;
        } else if (tok != null && tok.value.equals("{")) {
            getNextToken();
            if (!MST()) {
                return false;
            }
            tok = getNextToken();
            if (tok == null || !tok.value.equals("}")) {
                reportError("}", tok);
                return false;
            }
            return true;
        } else {
            return SST();
        }
    }

    public static boolean F1() {
        if (peekToken() != null && peekToken().value.equals(";")) {
            getNextToken();
            return true;
        }
        return declaration() || assignment_or_call();
    }

    public static boolean F2() {
        if (peekToken() != null && !peekToken().value.equals(";")) {
            return condition();
        }
        return true; // ε production
    }

    public static boolean F3() {
        if (peekToken() != null && !peekToken().value.equals(")")) {
            // Simple increment/decrement or assignment
            return expression();
        }
        return true; // ε production
    }

    public static boolean expression() {
        // Simplified expression parsing
        return OE();
    }

    public static boolean OE() {
        if (!AE()) {
            return false;
        }
        return OE_prime();
    }

    public static boolean OE_prime() {
        if (peekToken() != null && peekToken().value.equals("||")) {
            getNextToken();
            if (!AE()) {
                reportError("expression expected after '||'");
                return false;
            }
            return OE_prime();
        }
        return true; // ε production
    }

    public static boolean AE() {
        if (!RE2()) {
            return false;
        }
        return AE_prime();
    }

    public static boolean AE_prime() {
        if (peekToken() != null && peekToken().value.equals("&&")) {
            getNextToken();
            if (!RE2()) {
                reportError("expression expected after '&&'");
                return false;
            }
            return AE_prime();
        }
        return true; // ε production
    }

    public static boolean RE2() {
        if (!RE1()) {
            return false;
        }
        return RE2_prime();
    }

    public static boolean RE2_prime() {
        if (peekToken() != null && isRelationalOperator2(peekToken())) {
            getNextToken();
            if (!RE1()) {
                reportError("expression expected after relational operator");
                return false;
            }
            return RE2_prime();
        }
        return true; // ε production
    }

    public static boolean RE1() {
        if (!E()) {
            return false;
        }
        return RE1_prime();
    }

    public static boolean RE1_prime() {
        if (peekToken() != null && isRelationalOperator1(peekToken())) {
            getNextToken();
            if (!E()) {
                reportError("expression expected after relational operator");
                return false;
            }
            return RE1_prime();
        }
        return true; // ε production
    }

    public static boolean E() {
        if (!T()) {
            return false;
        }
        return E_prime();
    }

    public static boolean E_prime() {
        if (peekToken() != null && isPlusMinusOperator(peekToken())) {
            getNextToken();
            if (!T()) {
                reportError("expression expected after '+' or '-'");
                return false;
            }
            return E_prime();
        }
        return true; // ε production
    }

    public static boolean T() {
        if (!F()) {
            return false;
        }
        return T_prime();
    }

    public static boolean T_prime() {
        if (peekToken() != null && isMultDivModOperator(peekToken())) {
            getNextToken();
            if (!F()) {
                reportError("expression expected after '*', '/', or '%'");
                return false;
            }
            return T_prime();
        }
        return true; // ε production
    }

    public static boolean F() {
        Token tok = peekToken();
        if (tok == null) {
            reportError("expression expected");
            return false;
        }

        if (tok.value.equals("(")) {
            getNextToken();
            if (!OE()) {
                return false;
            }
            tok = getNextToken();
            if (tok == null || !tok.value.equals(")")) {
                reportError(")", tok);
                return false;
            }
            return true;
        } else if (tok.value.equals("-") || tok.value.equals("!")) {
            getNextToken();
            return F();
        } else {
            return primary();
        }
    }

public static boolean primary() {
    Token tok = peekToken();
    System.out.println("Entering primary, current token: " + tok);
    if (tok == null) {
        reportError("primary expression expected");
        return false;
    }

    if (Identifier(tok)) {
        int savedIndex = index; // Save index for backtracking
        if (qualified_name()) {
            Token next = peekToken();
            System.out.println("After qualified_name in primary, next token: " + next);
            if (next != null && next.value.equals("(")) {
                getNextToken(); // consume '('
                if (!args()) {
                    index = savedIndex;
                    return false;
                }
                tok = getNextToken();
                if (tok == null || !tok.value.equals(")")) {
                    reportError(")", tok);
                    index = savedIndex;
                    return false;
                }
            }
            return true; // Valid as variable access or method call
        }
        index = savedIndex;
        return false;
    } else if (isConstant(tok)) {
        getNextToken();
        System.out.println("Parsed constant: " + tok);
        return true;
    } else if (tok.value.equals("new")) {
        return constructor_call();
    } else {
        reportError("identifier, constant, or 'new'", tok);
        return false;
    }
}    public static boolean constructor_call() {
        Token tok = getNextToken(); // consume 'new'
        
        if (!Identifier(peekToken())) {
            reportError("class name after 'new'", peekToken());
            return false;
        }
        getNextToken();
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals("(")) {
            reportError("(", tok);
            return false;
        }
        
        if (!args()) {
            return false;
        }
        
        tok = getNextToken();
        if (tok == null || !tok.value.equals(")")) {
            reportError(")", tok);
            return false;
        }
        
        return true;
    }

    public static boolean args() {
        if (peekToken() != null && !peekToken().value.equals(")")) {
            if (!expression()) {
                return false;
            }
            return args_prime();
        }
        return true; // ε production
    }

    public static boolean args_prime() {
        if (peekToken() != null && peekToken().value.equals(",")) {
            getNextToken();
            return args();
        }
        return true; // ε production
    }

    public static boolean dataType() {
        Token tok = peekToken();
        if (tok != null && isDataType(tok)) {
            getNextToken();
            return true;
        }
        reportError("data type", tok);
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
        return method_body();
    }

    // Helper methods for token classification
    public static boolean Identifier(Token tok) {
        return tok != null && tok.type.equals("Identifier");
    }

    public static boolean isModifier(Token tok) {
        return tok != null && (tok.value.equals("public") || tok.value.equals("private") || 
                              tok.value.equals("protected") || tok.value.equals("static") ||
                              tok.value.equals("final") || tok.value.equals("abstract"));
    }

    public static boolean isValidModifier(Token tok) {
        // Add validation logic for modifier combinations if needed
        return isModifier(tok);
    }

    public static boolean isDataType(Token tok) {
        return tok != null && (tok.value.equals("int") || tok.value.equals("String") || 
                              tok.value.equals("boolean") || tok.value.equals("double") ||
                              tok.value.equals("float") || tok.value.equals("char") ||
                              tok.value.equals("byte") || tok.value.equals("short") ||
                              tok.value.equals("long")); // for custom types
    }

    public static boolean isConstant(Token tok) {
        return tok != null && (tok.type.equals("IntConstant") || tok.type.equals("StringConstant") ||
                              tok.type.equals("BoolConstant") || tok.type.equals("FloatConstant") ||
                              tok.value.equals("true") || tok.value.equals("false") ||
                              tok.value.matches("\\d+") || tok.value.matches("\".*\""));
    }

    public static boolean isAssignOperator(Token tok) {
        return tok != null && (tok.value.equals("=") || tok.value.equals("+=") || 
                              tok.value.equals("-=") || tok.value.equals("*=") ||
                              tok.value.equals("/=") || tok.value.equals("%="));
    }

    public static boolean isRelationalOperator1(Token tok) {
        return tok != null && (tok.value.equals("<") || tok.value.equals(">") ||
                              tok.value.equals("<=") || tok.value.equals(">="));
    }

    public static boolean isRelationalOperator2(Token tok) {
        return tok != null && (tok.value.equals("==") || tok.value.equals("!="));
    }

    public static boolean isPlusMinusOperator(Token tok) {
        return tok != null && (tok.value.equals("+") || tok.value.equals("-"));
    }

    public static boolean isMultDivModOperator(Token tok) {
        return tok != null && (tok.value.equals("*") || tok.value.equals("/") || tok.value.equals("%"));
    }

    // First set checks
    public static boolean First_class(Token tok) {
        return tok != null && (isModifier(tok) || tok.value.equals("class"));
    }

    public static boolean First_SST(Token tok) {
        if (tok == null) return false;
        
        return tok.value.equals("if") || tok.value.equals("while") || tok.value.equals("for") ||
               tok.value.equals("return") || tok.value.equals("try") || tok.value.equals("throw") ||
               isDataType(tok) || Identifier(tok) || tok.value.equals("{");
    }

    // Helper method to get current class name (would need proper implementation)
    private static String getCurrentClassName() {
        // This would need to be implemented to track the current class being parsed
        return "DefaultClass";
    }

    // Helper method to check if token represents end of input
    private static boolean isEndOfInput(Token tok) {
        if (tok == null) return true;
        
        return tok.value.equals("EOF") || 
               tok.value.equals("$") || 
               tok.type.equals("EOF");
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
            
            // Add EOF token if not present
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