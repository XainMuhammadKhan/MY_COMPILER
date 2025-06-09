package LEXER;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexer that tokenizes input source code and saves tokens to a file.
 * Tokens are formatted as <line,type,value> for SyntaxAnalyzer compatibility.
 * Ignores whitespace, newline, and comment tokens in the output.
 */
public class Lexer {
    private final String input;
    private int currentPosition;
    private int lineNumber;
    private static final List<TokenDefinition> TOKEN_DEFINITIONS;

    static {
        TOKEN_DEFINITIONS = List.of(
            new TokenDefinition(TokenType.MULTI_LINE_COMMENT, "/\\*~[\\s\\S]*?~\\*/"),
            new TokenDefinition(TokenType.SINGLE_LINE_COMMENT, "#[^\\n]*"),
            new TokenDefinition(TokenType.KEYWORD, 
                "\\b(class|function|if|else|return|int|double|char|boolean|void|const|static|enum|break|continue|for|while|public|private|protected|interface|extends|implements|this|super|new|abstract|try|catch|finally|throw|final|true|false|string)\\b"),
            new TokenDefinition(TokenType.BOOLEAN_CONSTANT, "\\b(true|false)\\b"),
            new TokenDefinition(TokenType.OPERATOR, 
                "(\\<\\<\\=|\\>\\>\\=|\\<\\<|\\>\\>|\\<\\=\\>|\\<\\>|\\=\\>|\\+\\+|\\-\\-|\\=\\=|\\!\\=|\\<\\=|\\>\\=|\\&\\&|\\|\\||\\+|\\-|\\*|\\/|\\%|\\=|\\<|\\>|\\!)"),
            new TokenDefinition(TokenType.FLOAT_CONSTANT, "\\b[0-9]*\\.[0-9]+\\b"),
            new TokenDefinition(TokenType.INT_CONSTANT, "\\b[0-9]+\\b"),
            new TokenDefinition(TokenType.IDENTIFIER, "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b"),
            new TokenDefinition(TokenType.INVALID_IDENTIFIER, "\\b\\d+[a-zA-Z0-9_]*\\b"),
            new TokenDefinition(TokenType.STRING_CONSTANT, "\"([^\"\\\\\\n]|\\\\.)*\""),
            new TokenDefinition(TokenType.CHAR_CONSTANT, "'([^'\\\\\\n]|\\\\.)*'"),
            new TokenDefinition(TokenType.PUNCTUATION, "[(){}\\[\\];,.:@#]"),
            new TokenDefinition(TokenType.NEWLINE, "\\n"),
            new TokenDefinition(TokenType.WHITESPACE, "[ \\t\\r]+"),
            new TokenDefinition(TokenType.INVALID, ".")
        );
    }

    /**
     * Constructs a Lexer with the given input source code.
     * @param input The source code to tokenize.
     */
    public Lexer(String input) {
        this.input = input;
        this.currentPosition = 0;
        this.lineNumber = 1;
    }

    /**
     * Tokenizes the input and returns a list of tokens.
     * Excludes whitespace, newline, and comment tokens.
     * @return List of tokens.
     * @throws LexerException If tokenization fails.
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (currentPosition < input.length()) {
            int startLine = lineNumber;
            Token token = nextToken();
            if (token == null) {
                throw new LexerException("Failed to tokenize at line " + lineNumber);
            }
            if (token.getType() == TokenType.NEWLINE) {
                lineNumber++;
            }
            if (token.getType() != TokenType.WHITESPACE && 
                token.getType() != TokenType.NEWLINE && 
                token.getType() != TokenType.SINGLE_LINE_COMMENT && 
                token.getType() != TokenType.MULTI_LINE_COMMENT) {
                tokens.add(new Token(token.getType(), token.getValue(), startLine));
            }
        }
        return tokens;
    }

    /**
     * Retrieves the next token from the input.
     * @return The next token, or null if at end of input.
     */
    private Token nextToken() {
        if (currentPosition >= input.length()) {
            return null;
        }
        String remainingInput = input.substring(currentPosition);
        Token bestToken = null;
        int bestLength = 0;

        for (TokenDefinition definition : TOKEN_DEFINITIONS) {
            // Remove ^ anchor for MULTI_LINE_COMMENT to ensure full match
            String patternStr = definition.type == TokenType.MULTI_LINE_COMMENT ? 
                definition.pattern.pattern() : "^" + definition.pattern.pattern();
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(remainingInput);
            if (matcher.lookingAt()) { // Use lookingAt() to match at start
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

        currentPosition++;
        return new Token(TokenType.INVALID, remainingInput.substring(0, 1), lineNumber);
    }

    /**
     * Converts TokenType to a string compatible with SyntaxAnalyzer.
     * @param type The TokenType to convert.
     * @return The string representation of the type.
     */
    public static String tokenTypeToString(TokenType type) {
        switch (type) {
            case KEYWORD: return "Keyword";
            case IDENTIFIER: return "Identifier";
            case INVALID_IDENTIFIER: return "Invalid_Identifier";
            case PUNCTUATION: return "Punctuation";
            case OPERATOR: return "Operator";
            case STRING_CONSTANT: return "string_const";
            case CHAR_CONSTANT: return "char_const";
            case BOOLEAN_CONSTANT: return "boolean_const";
            case INT_CONSTANT: return "int_const";
            case FLOAT_CONSTANT: return "float_const";
            case MULTI_LINE_COMMENT: return "multi_line_comment";
            case SINGLE_LINE_COMMENT: return "single_line_comment";
            case END_MARKER: return "EOF";
            default: return type.toString();
        }
    }

    /**
     * Saves tokens to a file in the format <line,type,value>.
     * @param tokens The list of tokens to save.
     * @param filePath The output file path.
     * @throws IOException If file writing fails.
     */
    private static void saveTokensToFile(List<Token> tokens, String filePath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            for (Token token : tokens) {
                writer.write(String.format("<%d,%s,%s>", token.getLine(), tokenTypeToString(token.getType()), token.getValue()));
                writer.newLine();
            }
        }
    }

    /**
     * Main method to tokenize a file and save tokens to tokens.txt.
     * @param args Command "_line arguments (input file path).
     */
    public static void main(String[] args) {
        String inputFilePath = args.length > 0 ? args[0] : "input.txt";
        String outputFilePath = "tokens.txt";

        try {
            String fileContent = Files.readString(Paths.get(inputFilePath));
            Lexer lexer = new Lexer(fileContent);
            List<Token> tokens = lexer.tokenize();

            // Save tokens to file
            saveTokensToFile(tokens, outputFilePath);

            // Print tokens to console for verification
            for (Token token : tokens) {
                System.out.printf("<%d,%s,%s>%n", token.getLine(), tokenTypeToString(token.getType()), token.getValue());
            }

            System.out.println("Tokens successfully written to " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error reading/writing file: " + e.getMessage());
        } catch (LexerException e) {
            System.err.println("Lexer error: " + e.getMessage());
        }
    }
}

/**
 * Custom exception for lexer errors.
 */
class LexerException extends RuntimeException {
    public LexerException(String message) {
        super(message);
    }
}