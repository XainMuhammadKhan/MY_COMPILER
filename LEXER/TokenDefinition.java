package LEXER;

import java.util.regex.Pattern;

/**
 * Defines a token type and its corresponding regex pattern.
 */
public class TokenDefinition {
    final TokenType type;
    final Pattern pattern;

    /**
     * Constructs a TokenDefinition with a type and regex pattern.
     * @param type The token type.
     * @param pattern The regex pattern (uncompiled).
     */
    public TokenDefinition(TokenType type, String pattern) {
        this.type = type;
        this.pattern = Pattern.compile("^" + pattern);
    }

    /**
     * Gets the token type.
     * @return The token type.
     */
    public TokenType getType() {
        return type;
    }

    /**
     * Gets the compiled regex pattern.
     * @return The compiled pattern.
     */
    public Pattern getPattern() {
        return pattern;
    }
}