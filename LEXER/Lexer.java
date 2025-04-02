package LEXER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private String input;
    private int currentPosition;
    private int lineNumber;

    private static final String KEYWORDS = "\\b(class|function|if|else|return|int|double|char|boolean|void|const|static|enum|break|continue|for|while|public|private|protected|interface|extends|implements|this|super|new|abstract|try|catch|finally|throw|final|true|false|string)\\b";
    private static final String IDENTIFIER = "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b";
    private static final String INVALID_IDENTIFIER = "\\b\\d+[a-zA-Z0-9_]*\\b";
    private static final String OPERATOR = "(\\<\\<\\=|\\>\\>\\=|\\<\\<|\\>\\>|\\<\\=\\>|\\<\\>|\\=\\>|\\+\\+|\\-\\-|\\=\\=|\\!\\=|\\<\\=|\\>\\=|\\&\\&|\\|\\||\\+|\\-|\\*|\\/|\\%|\\=|\\<|\\>|\\!)";
    private static final String PUNCTUATION = "[(){}\\[\\];,.:@#]";
    private static final String STRING_CONSTANT = "\"([^\"\\\\\n]|\\\\.)*\"";
    private static final String CHAR_CONSTANT = "'([^'\\\\\n]|\\\\.)*'";
    private static final String BOOLEAN_CONSTANT = "\\b(true|false)\\b";
    private static final String INT_CONSTANT = "\\b[0-9]+\\b";
    private static final String FLOAT_CONSTANT = "\\b[0-9]*\\.[0-9]+\\b";
    private static final String SINGLE_LINE_COMMENT = "#>.*$";
    private static final String MULTI_LINE_COMMENT = "/\\*~[\\s\\S]*?~\\*/";
    private static final String WHITESPACE = "[ \t\r]+";
    private static final String NEWLINE = "\n";
    private static final String INVALID = ".";

    private static final List<TokenDefinition> TOKEN_DEFINITIONS = List.of(
        new TokenDefinition(TokenType.SINGLE_LINE_COMMENT, SINGLE_LINE_COMMENT),
        new TokenDefinition(TokenType.MULTI_LINE_COMMENT, MULTI_LINE_COMMENT),
        new TokenDefinition(TokenType.KEYWORD, KEYWORDS),
        new TokenDefinition(TokenType.BOOLEAN_CONSTANT, BOOLEAN_CONSTANT),
        new TokenDefinition(TokenType.OPERATOR, OPERATOR),
        new TokenDefinition(TokenType.FLOAT_CONSTANT, FLOAT_CONSTANT),
        new TokenDefinition(TokenType.INT_CONSTANT, INT_CONSTANT),
        new TokenDefinition(TokenType.IDENTIFIER, IDENTIFIER),
        new TokenDefinition(TokenType.INVALID_IDENTIFIER, INVALID_IDENTIFIER),
        new TokenDefinition(TokenType.STRING_CONSTANT, STRING_CONSTANT),
        new TokenDefinition(TokenType.CHAR_CONSTANT, CHAR_CONSTANT),
        new TokenDefinition(TokenType.PUNCTUATION, PUNCTUATION),
        new TokenDefinition(TokenType.NEWLINE, NEWLINE),
        new TokenDefinition(TokenType.WHITESPACE, WHITESPACE),
        new TokenDefinition(TokenType.INVALID, INVALID)
    );

    public Lexer(String input) {
        this.input = input;
        this.currentPosition = 0;
        this.lineNumber = 1;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (currentPosition < input.length()) {
            int startPosition = currentPosition;
            int startLine = lineNumber;

            Token token = nextToken();
            if (token != null) {
                if (token.getType() == TokenType.NEWLINE) {
                    lineNumber++;
                }

                if (token.getType() != TokenType.WHITESPACE && token.getType() != TokenType.NEWLINE) {
                    token = new Token(token.getType(), token.getValue(), startLine);
                    tokens.add(token);
                }
            } else {
                throw new RuntimeException("Failed to tokenize at line " + lineNumber);
            }
        }

        return tokens;
    }

    private Token nextToken() {
        if (currentPosition >= input.length()) {
            return null;
        }

        String remainingInput = input.substring(currentPosition);
        Token bestToken = null;
        int bestLength = 0;

        for (TokenDefinition definition : TOKEN_DEFINITIONS) {
            Pattern pattern = Pattern.compile("^" + definition.pattern);
            Matcher matcher = pattern.matcher(remainingInput);

            if (matcher.find()) {
                String value = matcher.group();
                int length = value.length();
                
                if (length > bestLength) {
                    bestLength = length;
                    bestToken = new Token(definition.type, value, lineNumber);
                }
            }
        }

        if (bestToken != null) {
            currentPosition += bestLength;
            return bestToken;
        }

        // If no token is found, advance by one character
        currentPosition++;
        return new Token(TokenType.INVALID, remainingInput.substring(0, 1), lineNumber);
    }

    public static void main(String[] args) {
        String filePath = "C:/Users/Xain M-k/Desktop/ALL USEFULL THINGS/New folder/MY_COMPILER/MY_COMPILER/input.txt";

        try {
            String fileContent = Files.readString(Paths.get(filePath));
            Lexer lexer = new Lexer(fileContent);
            List<Token> tokens = lexer.tokenize();

            for (Token token : tokens) {
                System.out.println(token);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Lexer error: " + e.getMessage());
        }
    }
}
