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

    private static boolean pushBack(int n) {
        index = Math.max(0, index - n);
        return true;
    }

    private static void reset() {
        index = 0;
    }

    private static Token getToken() {
        return getCurrentToken();
    }

    // <PS> → <import_st> <classes> <main_class>
    public static boolean PS(Token tok) {
        System.out.println("Entering <PS> section");
        int oldTokenCount = index;
        if (tok != null) {
            if (import_st(tok)) {
                tok = getNextToken();
                if (classes(tok)) {
                    tok = getNextToken();
                    if (main_class(tok)) {
                        System.out.println("Parsing Successful !!!");
                        return true;
                    }
                }
            }
            System.out.println("Failed at line: " + (getToken() != null ? getToken().line : "EOF") + " near >>> " + (getToken() != null ? getToken().value : "EOF"));
            return false;
        }
        return false;
    }

    // <import_st> → import <qualified_name> <import_tail>
    public static boolean import_st(Token tok) {
        System.out.println("Entering <import_st> section");
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("import")) {
            tok = getNextToken();
            if (qualified_name(tok)) {
                tok = getNextToken();
                if (import_tail(tok)) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <import_tail> → ; | . *
    public static boolean import_tail(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (tok.value.equals(";")) {
                return true;
            } else if (tok.value.equals(".")) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("*")) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <qualified_name> → ID <qualified_name_tail>
    public static boolean qualified_name(Token tok) {
        System.out.println("Entering <qualified_name> section");
        int oldTokenCount = index;
        if (tok != null && Identifier(tok)) {
            tok = getNextToken();
            if (qualified_name_tail(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <qualified_name_tail> → ε | . ID <qualified_name_tail>
    public static boolean qualified_name_tail(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals(".")) {
            tok = getNextToken();
            if (Identifier(tok)) {
                tok = getNextToken();
                if (qualified_name_tail(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <classname> → ID
    public static boolean classname(Token tok) {
        return Identifier(tok);
    }

    // <main_class> → <main_method>
    public static boolean main_class(Token tok) {
        return main_method(tok);
    }

    // <classes> → <class><classes> | ε
    public static boolean classes(Token tok) {
        int oldTokenCount = index;
        if (tok != null && First_class(tok)) {
            if (classDecl(tok)) {
                tok = getNextToken();
                if (classes(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <class> → <class_header><Inheritance><class_body>
    public static boolean classDecl(Token tok) {
        int oldTokenCount = index;
        if (class_header(tok)) {
            tok = getNextToken();
            if (Inheritance(tok)) {
                tok = getNextToken();
                if (class_body(tok)) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <class_header> → <Modifiers>Class ID
    public static boolean class_header(Token tok) {
        int oldTokenCount = index;
        if (Modifiers(tok)) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("class")) {
                tok = getNextToken();
                if (Identifier(tok)) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Inheritance> → extends ID | ε
    public static boolean Inheritance(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("extends")) {
            tok = getNextToken();
            if (Identifier(tok)) {
                return true;
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <class_body> → { | <Attributes> <class_body> | <constructors> <class_body> | <Methods> <class_body> | ε
    public static boolean class_body(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("{")) {
            tok = getNextToken();
            while (tok != null && First_class_body(tok)) {
                if (Attributes(tok) || constructors(tok) || Methods(tok)) {
                    tok = getNextToken();
                } else {
                    break;
                }
            }
            if (tok != null && tok.value.equals("}")) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Attributes> → <Modifiers>DT ID <Exp>
    public static boolean Attributes(Token tok) {
        int oldTokenCount = index;
        if (Modifiers(tok)) {
            tok = getNextToken();
            if (DataType(tok)) {
                tok = getNextToken();
                if (Identifier(tok)) {
                    tok = getNextToken();
                    if (Exp(tok)) {
                        return true;
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Modifiers> → <Access_Modifier> | static | final | abstract
    public static boolean Modifiers(Token tok) {
        int oldTokenCount = index;
        if (Access_Modifier(tok) || (tok != null && (tok.value.equals("static") || tok.value.equals("final") || tok.value.equals("abstract")))) {
            return true;
        }
        return true; // Modifiers can be empty in some cases
    }

    // <Access_Modifier> → public | private | protected
    public static boolean Access_Modifier(Token tok) {
        if (tok != null && (tok.value.equals("public") || tok.value.equals("private") || tok.value.equals("protected"))) {
            return true;
        }
        return false;
    }

    // <constructor> → <constructor_header><Method_Body>
    public static boolean constructors(Token tok) {
        int oldTokenCount = index;
        if (constructor_header(tok)) {
            tok = getNextToken();
            if (Method_body(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <constructor_header> → <Modifiers> ID(<Parameters>)
    public static boolean constructor_header(Token tok) {
        int oldTokenCount = index;
        if (Modifiers(tok)) {
            tok = getNextToken();
            if (Identifier(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("(")) {
                    tok = getNextToken();
                    if (Parameters(tok)) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals(")")) {
                            return true;
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Methods> → <Method><Methods> | ε
    public static boolean Methods(Token tok) {
        int oldTokenCount = index;
        if (tok != null && First_Method(tok)) {
            if (Method(tok)) {
                tok = getNextToken();
                if (Methods(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <Method> → <Method_header><Method_body>
    public static boolean Method(Token tok) {
        int oldTokenCount = index;
        if (Method_header(tok)) {
            tok = getNextToken();
            if (Method_body(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Method_header> → <Modifiers>DT ID(<Parameter>)
    public static boolean Method_header(Token tok) {
        int oldTokenCount = index;
        if (Modifiers(tok)) {
            tok = getNextToken();
            if (DataType(tok)) {
                tok = getNextToken();
                if (Identifier(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals("(")) {
                        tok = getNextToken();
                        if (Parameter(tok)) {
                            tok = getNextToken();
                            if (tok != null && tok.value.equals(")")) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Method_body> → {<MST>}
    public static boolean Method_body(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("{")) {
            tok = getNextToken();
            if (MST(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("}")) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Parameters> → <Parameter><Parameter’> | ε
    public static boolean Parameters(Token tok) {
        int oldTokenCount = index;
        if (tok != null && First_Parameter(tok)) {
            if (Parameter(tok)) {
                tok = getNextToken();
                if (ParameterPrime(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <Parameter’> → ε | <Parameters>
    public static boolean ParameterPrime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals(",")) {
            tok = getNextToken();
            if (Parameters(tok)) {
                return true;
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <Parameter> → DT ID
    public static boolean Parameter(Token tok) {
        int oldTokenCount = index;
        if (DataType(tok)) {
            tok = getNextToken();
            if (Identifier(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <MST> → <SST><MST> | ε
    public static boolean MST(Token tok) {
        int oldTokenCount = index;
        if (tok != null && First_SST(tok)) {
            if (SST(tok)) {
                tok = getNextToken();
                if (MST(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <SST> → <Exp>; | <TS>; | <ReturnSt>; | <assign_st>; | <Dec>; | <if_St> | <while_St> | <for_St> | ObjCall
    public static boolean SST(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (Exp(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(";")) {
                    return true;
                }
            } else if (TS(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(";")) {
                    return true;
                }
            } else if (ReturnSt(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(";")) {
                    return true;
                }
            } else if (assign_st(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(";")) {
                    return true;
                }
            } else if (Dec(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(";")) {
                    return true;
                }
            } else if (if_St(tok) || while_St(tok) || for_St(tok) || ObjCall(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Unary_Opr> → inc dec | NOT
    public static boolean Unary_Opr(Token tok) {
        if (tok != null && (tok.value.equals("++") || tok.value.equals("--") || tok.value.equals("!"))) {
            return true;
        }
        return false;
    }

    // <Binary_Opr> → PM | MDM | Comparison | Logical
    public static boolean Binary_Opr(Token tok) {
        if (tok != null && (tok.value.equals("+") || tok.value.equals("-") || tok.value.equals("*") ||
                            tok.value.equals("/") || tok.value.equals("%") || tok.value.equals("==") ||
                            tok.value.equals("!=") || tok.value.equals("<") || tok.value.equals(">") ||
                            tok.value.equals("<=") || tok.value.equals(">=") || tok.value.equals("&&") ||
                            tok.value.equals("||"))) {
            return true;
        }
        return false;
    }

    // <assign_st> → ID <Assign_Opr><Exp>
    public static boolean assign_st(Token tok) {
        int oldTokenCount = index;
        if (Identifier(tok)) {
            tok = getNextToken();
            if (Assign_Opr(tok)) {
                tok = getNextToken();
                if (Exp(tok)) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Assign_Opr> → = | += | -= | *= | /= | %=
    public static boolean Assign_Opr(Token tok) {
        if (tok != null && (tok.value.equals("=") || tok.value.equals("+=") || tok.value.equals("-=") ||
                            tok.value.equals("*=") || tok.value.equals("/=") || tok.value.equals("%="))) {
            return true;
        }
        return false;
    }

    // <Method_Call> → ID(<Args>)
    public static boolean Method_Call(Token tok) {
        int oldTokenCount = index;
        if (Identifier(tok)) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("(")) {
                tok = getNextToken();
                if (Args(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals(")")) {
                        return true;
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <constructor_call> → new ID (<Args>)
    public static boolean constructor_call(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("new")) {
            tok = getNextToken();
            if (Identifier(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("(")) {
                    tok = getNextToken();
                    if (Args(tok)) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals(")")) {
                            return true;
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Args> → <Exp><Args’> | ε
    public static boolean Args(Token tok) {
        int oldTokenCount = index;
        if (tok != null && First_Exp(tok)) {
            if (Exp(tok)) {
                tok = getNextToken();
                if (ArgsPrime(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <Args’> → ε | ,<Args>
    public static boolean ArgsPrime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals(",")) {
            tok = getNextToken();
            if (Args(tok)) {
                return true;
            }
        }
        return true; // ε production
    }

    // <TS> → <This_or_Super_or_ID><Args>
    public static boolean TS(Token tok) {
        int oldTokenCount = index;
        if (This_or_Super_or_ID(tok)) {
            tok = getNextToken();
            if (Args(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <This_or_Super_or_ID> → this | super | ID
    public static boolean This_or_Super_or_ID(Token tok) {
        if (tok != null && (tok.value.equals("this") || tok.value.equals("super") || Identifier(tok))) {
            return true;
        }
        return false;
    }

    // <Return_St> → return <Exp> | return this.
    public static boolean ReturnSt(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("return")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("this")) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(".")) {
                    return true;
                }
            } else if (Exp(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <main_method> → <m_m_header> {<m_m_body>}
    public static boolean main_method(Token tok) {
        int oldTokenCount = index;
        if (m_m_header(tok)) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("{")) {
                tok = getNextToken();
                if (m_m_body(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals("}")) {
                        return true;
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <m_m_body> → <MST>
    public static boolean m_m_body(Token tok) {
        return MST(tok);
    }

    // <m_m_header> → public static void main (String args[])
    public static boolean m_m_header(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("public")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("static")) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("void")) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals("main")) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals("(")) {
                            tok = getNextToken();
                            if (tok != null && tok.value.equals("String")) {
                                tok = getNextToken();
                                if (tok != null && tok.value.equals("args")) {
                                    tok = getNextToken();
                                    if (tok != null && tok.value.equals("[")) {
                                        tok = getNextToken();
                                        if (tok != null && tok.value.equals("]")) {
                                            tok = getNextToken();
                                            if (tok != null && tok.value.equals(")")) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <object_decl> → <obj_header> ;
    public static boolean object_decl(Token tok) {
        int oldTokenCount = index;
        if (obj_header(tok)) {
            tok = getNextToken();
            if (tok != null && tok.value.equals(";")) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <obj_header> → Type ID = <new_expr>
    public static boolean obj_header(Token tok) {
        int oldTokenCount = index;
        if (Type(tok)) {
            tok = getNextToken();
            if (Identifier(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("=")) {
                    tok = getNextToken();
                    if (new_expr(tok)) {
                        return true;
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <new_expr> → new Type (<arg_list_opt>)
    public static boolean new_expr(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("new")) {
            tok = getNextToken();
            if (Type(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("(")) {
                    tok = getNextToken();
                    if (arg_list_opt(tok)) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals(")")) {
                            return true;
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <arg_list_opt> → <arg_list> | ε
    public static boolean arg_list_opt(Token tok) {
        int oldTokenCount = index;
        if (tok != null && First_arg_list(tok)) {
            if (arg_list(tok)) {
                return true;
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <arg_list> → Expr <arg_list_tail>
    public static boolean arg_list(Token tok) {
        int oldTokenCount = index;
        if (Expr(tok)) {
            tok = getNextToken();
            if (arg_list_tail(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <arg_list_tail> → , <arg_list> | ε
    public static boolean arg_list_tail(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals(",")) {
            tok = getNextToken();
            if (arg_list(tok)) {
                return true;
            }
        }
        return true; // ε production
    }

    // Expr → ID <expr_tail> | <Const> | <new_expr>
    public static boolean Expr(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (Identifier(tok)) {
                tok = getNextToken();
                if (expr_tail(tok)) {
                    return true;
                }
            } else if (Const(tok)) {
                return true;
            } else if (new_expr(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <expr_tail> → ( <arg_list_opt> ) | ε
    public static boolean expr_tail(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("(")) {
            tok = getNextToken();
            if (arg_list_opt(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(")")) {
                    return true;
                }
            }
        }
        return true; // ε production
    }

    // <Type> → ID
    public static boolean Type(Token tok) {
        return Identifier(tok);
    }

    // <Const> → int_const | string_const | boolean_const
    public static boolean Const(Token tok) {
        if (tok != null && (tok.type.equals("int_const") || tok.type.equals("string_const") || tok.type.equals("boolean_const"))) {
            return true;
        }
        return false;
    }

    // <object_call> → <primary_expr> <access_chain>
    public static boolean ObjCall(Token tok) {
        int oldTokenCount = index;
        if (primary_expr(tok)) {
            tok = getNextToken();
            if (access_chain(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <primary_expr> → ID | this | super | <new_expr> | <Method_Call>
    public static boolean primary_expr(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (Identifier(tok) || tok.value.equals("this") || tok.value.equals("super")) {
                return true;
            } else if (new_expr(tok)) {
                return true;
            } else if (Method_Call(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <access_chain> → <access> <access_chain> | ε
    public static boolean access_chain(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals(".")) {
            if (access(tok)) {
                tok = getNextToken();
                if (access_chain(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <access> → . ID <access_tail>
    public static boolean access(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals(".")) {
            tok = getNextToken();
            if (Identifier(tok)) {
                tok = getNextToken();
                if (access_tail(tok)) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <access_tail> → ( <arg_list_opt> ) | ε
    public static boolean access_tail(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("(")) {
            tok = getNextToken();
            if (arg_list_opt(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(")")) {
                    return true;
                }
            }
        }
        return true; // ε production
    }

    // <Exp> → <OE>
    public static boolean Exp(Token tok) {
        return OE(tok);
    }

    // <OE> → <AE> <OE’>
    public static boolean OE(Token tok) {
        int oldTokenCount = index;
        if (AE(tok)) {
            tok = getNextToken();
            if (OEPrime(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <OE’> → OR <AE> <OE’> | ε
    public static boolean OEPrime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("||")) {
            tok = getNextToken();
            if (AE(tok)) {
                tok = getNextToken();
                if (OEPrime(tok)) {
                    return true;
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <AE> → <RE2> <AE’>
    public static boolean AE(Token tok) {
        int oldTokenCount = index;
        if (RE2(tok)) {
            tok = getNextToken();
            if (AEPrime(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <AE’> → AND <RE2> <AE’> | ε
    public static boolean AEPrime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("&&")) {
            tok = getNextToken();
            if (RE2(tok)) {
                tok = getNextToken();
                if (AEPrime(tok)) {
                    return true;
                }
            }
        }
        return true; // ε production
    }

    // <RE2> → <RE1> <RE2’>
    public static boolean RE2(Token tok) {
        int oldTokenCount = index;
        if (RE1(tok)) {
            tok = getNextToken();
            if (RE2Prime(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <RE2’> → RO2 <RE1> <RE2’> | ε
    public static boolean RE2Prime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && (tok.value.equals("==") || tok.value.equals("!="))) {
            tok = getNextToken();
            if (RE1(tok)) {
                tok = getNextToken();
                if (RE2Prime(tok)) {
                    return true;
                }
            }
        }
        return true; // ε production
    }

    // <RE1> → <E> <RE1’>
    public static boolean RE1(Token tok) {
        int oldTokenCount = index;
        if (E(tok)) {
            tok = getNextToken();
            if (RE1Prime(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <RE1’> → RO1 <E> <RE1’> | ε
    public static boolean RE1Prime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && (tok.value.equals("<") || tok.value.equals(">") || tok.value.equals("<=") || tok.value.equals(">="))) {
            tok = getNextToken();
            if (E(tok)) {
                tok = getNextToken();
                if (RE1Prime(tok)) {
                    return true;
                }
            }
        }
        return true; // ε production
    }

    // <E> → <T> <E’>
    public static boolean E(Token tok) {
        int oldTokenCount = index;
        if (T(tok)) {
            tok = getNextToken();
            if (EPrime(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <E’> → PM <T> <E’> | ε
    public static boolean EPrime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && (tok.value.equals("+") || tok.value.equals("-"))) {
            tok = getNextToken();
            if (T(tok)) {
                tok = getNextToken();
                if (EPrime(tok)) {
                    return true;
                }
            }
        }
        return true; // ε production
    }

    // <T> → <F> <T’>
    public static boolean T(Token tok) {
        int oldTokenCount = index;
        if (F(tok)) {
            tok = getNextToken();
            if (TPrime(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <T’> → MDM <F> <T’> | ε
    public static boolean TPrime(Token tok) {
        int oldTokenCount = index;
        if (tok != null && (tok.value.equals("*") || tok.value.equals("/") || tok.value.equals("%"))) {
            tok = getNextToken();
            if (F(tok)) {
                tok = getNextToken();
                if (TPrime(tok)) {
                    return true;
                }
            }
        }
        return true; // ε production
    }

    // <F> → <primary> | - <F> | NOT <F> | ( <OE> )
    public static boolean F(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (primary(tok)) {
                return true;
            } else if (tok.value.equals("-")) {
                tok = getNextToken();
                if (F(tok)) {
                    return true;
                }
            } else if (tok.value.equals("!")) {
                tok = getNextToken();
                if (F(tok)) {
                    return true;
                }
            } else if (tok.value.equals("(")) {
                tok = getNextToken();
                if (OE(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals(")")) {
                        return true;
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <primary> → ID | const | <Method_Call> | <constructor_call> | <assign_st>
    public static boolean primary(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (Identifier(tok)) {
                Token nextTok = getNextToken();
                if (nextTok != null && Assign_Opr(nextTok)) {
                    pushBack(1);
                    return assign_st(tok);
                } else if (nextTok != null && nextTok.value.equals("(")) {
                    pushBack(1);
                    return Method_Call(tok);
                }
                pushBack(1);
                return true; // ID alone
            } else if (Const(tok)) {
                return true;
            } else if (constructor_call(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <try> → try { <MST> } <catch_list>
    public static boolean try_stmt(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("try")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("{")) {
                tok = getNextToken();
                if (MST(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals("}")) {
                        tok = getNextToken();
                        if (catch_list(tok)) {
                            return true;
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <catch_list> → catch ( ID ) { <MST> } <catch_list_tail>
    public static boolean catch_list(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("catch")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("(")) {
                tok = getNextToken();
                if (Identifier(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals(")")) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals("{")) {
                            tok = getNextToken();
                            if (MST(tok)) {
                                tok = getNextToken();
                                if (tok != null && tok.value.equals("}")) {
                                    tok = getNextToken();
                                    if (catch_list_tail(tok)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <catch_list_tail> → catch ( ID ) { <MST> } <catch_list_tail> | ε
    public static boolean catch_list_tail(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("catch")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("(")) {
                tok = getNextToken();
                if (Identifier(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals(")")) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals("{")) {
                            tok = getNextToken();
                            if (MST(tok)) {
                                tok = getNextToken();
                                if (tok != null && tok.value.equals("}")) {
                                    tok = getNextToken();
                                    if (catch_list_tail(tok)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <throw> → throw <throw_options> ;
    public static boolean throw_stmt(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("throw")) {
            tok = getNextToken();
            if (throw_options(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(";")) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <throw_options> → ID | Const | new ID ( <param_list> )
    public static boolean throw_options(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (Identifier(tok)) {
                return true;
            } else if (Const(tok)) {
                return true;
            } else if (tok.value.equals("new")) {
                tok = getNextToken();
                if (Identifier(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals("(")) {
                        tok = getNextToken();
                        if (param_list(tok)) {
                            tok = getNextToken();
                            if (tok != null && tok.value.equals(")")) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <param_list> → <Parameters>
    public static boolean param_list(Token tok) {
        return Parameters(tok);
    }

    // <While_St> → while (<cond>)<loop_body>
    public static boolean while_St(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("while")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("(")) {
                tok = getNextToken();
                if (cond(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals(")")) {
                        tok = getNextToken();
                        if (loop_body(tok)) {
                            return true;
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <cond> → <Const_or_ID> | <Const_or_ID> <ROP> <Const_or_ID> | <Exp>
    public static boolean cond(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (Const_or_ID(tok)) {
                Token nextTok = getNextToken();
                if (nextTok != null && ROP(nextTok)) {
                    tok = getNextToken();
                    if (Const_or_ID(tok)) {
                        return true;
                    }
                }
                pushBack(1);
                return true;
            } else if (Exp(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <Const_or_ID> → ID | Const
    public static boolean Const_or_ID(Token tok) {
        return Identifier(tok) || Const(tok);
    }

    // <ROP> → RO1 | RO2
    public static boolean ROP(Token tok) {
        if (tok != null && (tok.value.equals("<") || tok.value.equals(">") || tok.value.equals("<=") ||
                            tok.value.equals(">=") || tok.value.equals("==") || tok.value.equals("!="))) {
            return true;
        }
        return false;
    }

    // <loop_body> → ; | <SST> | {<MST>}
    public static boolean loop_body(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (tok.value.equals(";")) {
                return true;
            } else if (SST(tok)) {
                return true;
            } else if (tok.value.equals("{")) {
                tok = getNextToken();
                if (MST(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals("}")) {
                        return true;
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <for_loop> → for (<F1><F2>;<F3>) <loop_body>
    public static boolean for_St(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("for")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("(")) {
                tok = getNextToken();
                if (F1(tok)) {
                    tok = getNextToken();
                    if (F2(tok)) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals(";")) {
                            tok = getNextToken();
                            if (F3(tok)) {
                                tok = getNextToken();
                                if (tok != null && tok.value.equals(")")) {
                                    tok = getNextToken();
                                    if (loop_body(tok)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <F1> → <dt_dec> | <assign_st> | ;
    public static boolean F1(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (dt_dec(tok)) {
                return true;
            } else if (assign_st(tok)) {
                return true;
            } else if (tok.value.equals(";")) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <F2> → <cond> | ε
    public static boolean F2(Token tok) {
        int oldTokenCount = index;
        if (tok != null && First_cond(tok)) {
            if (cond(tok)) {
                return true;
            }
            return pushBack(index - oldTokenCount);
        }
        return true; // ε production
    }

    // <F3> → <inc_dec> | <assign_st> | null
    public static boolean F3(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (inc_dec(tok)) {
                return true;
            } else if (assign_st(tok)) {
                return true;
            } else if (tok.value.equals("null")) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <inc_dec> → inc | dec
    public static boolean inc_dec(Token tok) {
        if (tok != null && (tok.value.equals("++") || tok.value.equals("--"))) {
            return true;
        }
        return false;
    }

    // <if> → if (<cond>) <loop_body> <else>
    public static boolean if_St(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("if")) {
            tok = getNextToken();
            if (tok != null && tok.value.equals("(")) {
                tok = getNextToken();
                if (cond(tok)) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals(")")) {
                        tok = getNextToken();
                        if (loop_body(tok)) {
                            tok = getNextToken();
                            if (else_stmt(tok)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <else> → else <loop_body> | null
    public static boolean else_stmt(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("else")) {
            tok = getNextToken();
            if (loop_body(tok)) {
                return true;
            }
        } else if (tok != null && tok.value.equals("null")) {
            return true;
        }
        return true; // ε production
    }

    // <array_dec> → <arr_type> ID [] = { <arr_const_or_id> };
    public static boolean array_dec(Token tok) {
        int oldTokenCount = index;
        if (arr_type(tok)) {
            tok = getNextToken();
            if (Identifier(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals("[")) {
                    tok = getNextToken();
                    if (tok != null && tok.value.equals("]")) {
                        tok = getNextToken();
                        if (tok != null && tok.value.equals("=")) {
                            tok = getNextToken();
                            if (tok != null && tok.value.equals("{")) {
                                tok = getNextToken();
                                if (arr_const_or_id(tok)) {
                                    tok = getNextToken();
                                    if (tok != null && tok.value.equals("}")) {
                                        tok = getNextToken();
                                        if (tok != null && tok.value.equals(";")) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <arr_type> → DT | ID
    public static boolean arr_type(Token tok) {
        return DataType(tok) || Identifier(tok);
    }

    // <arr_const_or_id> → ε | <Const_or_ID> | ID , | Const ,
    public static boolean arr_const_or_id(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (Const_or_ID(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(",")) {
                    return true;
                }
                pushBack(1);
                return true;
            }
        }
        return true; // ε production
    }

    // <dt_dec> → <var_init> <var_init_tail> ;
    public static boolean dt_dec(Token tok) {
        int oldTokenCount = index;
        if (var_init(tok)) {
            tok = getNextToken();
            if (var_init_tail(tok)) {
                tok = getNextToken();
                if (tok != null && tok.value.equals(";")) {
                    return true;
                }
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // <var_init> → = <Const_or_ID> | ε
    public static boolean var_init(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals("=")) {
            tok = getNextToken();
            if (Const_or_ID(tok)) {
                return true;
            }
        }
        return true; // ε production
    }

    // <var_init_tail> → , ID <var_init> <var_init_tail> | ε
    public static boolean var_init_tail(Token tok) {
        int oldTokenCount = index;
        if (tok != null && tok.value.equals(",")) {
            tok = getNextToken();
            if (Identifier(tok)) {
                tok = getNextToken();
                if (var_init(tok)) {
                    tok = getNextToken();
                    if (var_init_tail(tok)) {
                        return true;
                    }
                }
            }
        }
        return true; // ε production
    }

    // <Dec> → <dt_dec> | <array_dec> | <object_decl>
    public static boolean Dec(Token tok) {
        int oldTokenCount = index;
        if (tok != null) {
            if (dt_dec(tok)) {
                return true;
            } else if (array_dec(tok)) {
                return true;
            } else if (object_decl(tok)) {
                return true;
            }
        }
        return pushBack(index - oldTokenCount);
    }

    // Helper methods
    public static boolean Identifier(Token tok) {
        return tok != null && tok.type.equals("Identifier");
    }

    public static boolean DataType(Token tok) {
        if (tok != null && (tok.value.equals("int") || tok.value.equals("boolean") ||
                            tok.value.equals("char") || tok.value.equals("String") || Identifier(tok))) {
            return true;
        }
        return false;
    }

    // First set checks
    public static boolean First_class(Token tok) {
        return tok != null && (tok.value.equals("public") || tok.value.equals("private") ||
                               tok.value.equals("protected") || tok.value.equals("static") ||
                               tok.value.equals("final") || tok.value.equals("abstract") ||
                               tok.value.equals("class"));
    }

    public static boolean First_class_body(Token tok) {
        return tok != null && (tok.value.equals("{") || tok.value.equals("public") ||
                               tok.value.equals("private") || tok.value.equals("protected") ||
                               tok.value.equals("static") || tok.value.equals("final") ||
                               tok.value.equals("abstract") || DataType(tok) || Identifier(tok));
    }

    public static boolean First_Method(Token tok) {
        return tok != null && (tok.value.equals("public") || tok.value.equals("private") ||
                               tok.value.equals("protected") || tok.value.equals("static") ||
                               tok.value.equals("final") || tok.value.equals("abstract") || DataType(tok));
    }

    public static boolean First_Parameter(Token tok) {
        return DataType(tok);
    }

    public static boolean First_SST(Token tok) {
        return tok != null && (Identifier(tok) || tok.value.equals("++") || tok.value.equals("--") ||
                               tok.value.equals("!") || tok.value.equals("+") || tok.value.equals("-") ||
                               tok.value.equals("this") || tok.value.equals("super") || tok.value.equals("return") ||
                               tok.value.equals("if") || tok.value.equals("while") || tok.value.equals("for") ||
                               DataType(tok));
    }

    public static boolean First_Exp(Token tok) {
        return tok != null && (Identifier(tok) || Const(tok) || tok.value.equals("new") ||
                               tok.value.equals("-") || tok.value.equals("!") || tok.value.equals("("));
    }

    public static boolean First_arg_list(Token tok) {
        return First_Exp(tok);
    }

    public static boolean First_cond(Token tok) {
        return First_Exp(tok) || Const(tok);
    }

    // Main method to read tokens from a text file
    public static void main(String[] args) throws IOException {
        String fileName = args.length > 0 ? args[0] : "tokens.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    if (line.startsWith("<") && line.endsWith(">")) {
                        line = line.substring(1, line.length() - 1);
                        String[] parts = line.split(",");
                        if (parts.length >= 3) {
                            Token t = new Token(parts[2], parts[1], parts[0]);
                            tokens.add(t);
                            System.out.println("Loaded token: " + t.value + " (" + t.type + ")");
                        } else {
                            System.err.println("Skipping malformed line: " + line);
                        }
                    } else {
                        System.err.println("Skipping invalid line format: " + line);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line + " (" + e.getMessage() + ")");
                }
            }
            tokens.add(new Token("EOF", "EOF", "-1"));
            PS(getCurrentToken());
            reset();
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
    }
}