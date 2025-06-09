package LEXER;

/**
 * Represents a single token with type, value, and line number.
 */
public class Token {
    private final TokenType type;
    private final String value;
    private final int line;

    /**
     * Constructs a Token with the given type, value, and line number.
     * @param type The token type.
     * @param value The token's lexeme.
     * @param line The line number where the token appears.
     */
    public Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    /**
     * Gets the token type.
     * @return The token type.
     */
    public TokenType getType() {
        return type;
    }

    /**
     * Gets the token's value.
     * @return The token's lexeme.
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the line number.
     * @return The line number.
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns a string representation of the token in the format <line>\t<type>\t<value>.
     * @return The string representation.
     */
    @Override
    public String toString() {
        return String.format("%d\t%s\t%s", line, Lexer.tokenTypeToString(type), value);
    }
}