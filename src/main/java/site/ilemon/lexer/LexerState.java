package site.ilemon.lexer;

/**
 * Lexer state enumeration
 * Defines all states of the DFA
 */
public enum LexerState {
    START,          // Initial state
    IN_ID,          // Reading identifier
    IN_NUM,         // Reading integer
    IN_FLOAT,       // Reading float (after decimal point)
    IN_STRING,      // Reading string
    IN_COMMENT,     // Reading single line comment
    IN_ASSIGN,      // Read =, maybe = or ==
    IN_LT,          // Read <, maybe < or <=
    IN_GT,          // Read >, maybe > or >=
    IN_NOT,         // Read !, maybe ! or !=
    IN_AND,         // Read &, expecting &&
    IN_OR,          // Read |, expecting ||
    IN_DIV,         // Read /, maybe / or //
    DONE,           // Completed a token
    ERROR           // Error state
}
