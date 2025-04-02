package LEXER;

public class TokenDefinition {
    public final TokenType type;
    public final String pattern;

    public TokenDefinition(TokenType type, String pattern) {
        this.type = type;
        this.pattern = pattern;
    }
}
