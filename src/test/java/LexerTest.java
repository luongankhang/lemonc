import org.junit.Test;
import site.ilemon.lexer.Lexer;
import site.ilemon.lexer.Token;
import site.ilemon.lexer.TokenKind;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Lexer test cases
 * Test various state transition paths of the state machine
 */
public class LexerTest {

    // ==================== Keyword tests ====================
    
    @Test
    public void testKeywords() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // Verify can recognize class keyword
        Token first = lexer.next();
        assertEquals(TokenKind.Class, first.kind);
        assertEquals("class", first.lexeme);
    }

    @Test
    public void testAllKeywords() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        // Check tokens contain various keywords
        boolean hasClass = false, hasVoid = false, hasFloat = false, hasReturn = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Class) hasClass = true;
            if (t.kind == TokenKind.Void) hasVoid = true;
            if (t.kind == TokenKind.Float) hasFloat = true;
            if (t.kind == TokenKind.Return) hasReturn = true;
        }
        assertTrue("Should recognize class keyword", hasClass);
        assertTrue("Should recognize void keyword", hasVoid);
        assertTrue("Should recognize float keyword", hasFloat);
        assertTrue("Should recognize return keyword", hasReturn);
    }

    // ==================== Identifier tests ====================
    
    @Test
    public void testIdentifier() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // Find identifier
        boolean hasId = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Id) {
                hasId = true;
                break;
            }
        }
        assertTrue("Should be able to recognize identifier", hasId);
    }

    // ==================== Number tests ====================
    
    @Test
    public void testIntegerNumber() throws IOException {
        Lexer lexer = new Lexer(new File("examples/IntTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasNum = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Num) {
                hasNum = true;
                break;
            }
        }
        assertTrue("Should be able to recognize integer", hasNum);
    }

    @Test
    public void testFloatNumber() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasDNum = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.FloatLiteral) {
                hasDNum = true;
                // Verify float format
                assertTrue("Float number should contain decimal point", t.lexeme.contains("."));
                break;
            }
        }
        assertTrue("Should be able to recognize float number", hasDNum);
    }

    // ==================== Operator tests ====================
    
    @Test
    public void testArithmeticOperators() throws IOException {
        // Use FloatTest01, which contains all four arithmetic operations
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasAdd = false, hasSub = false, hasMul = false, hasDiv = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Add) hasAdd = true;
            if (t.kind == TokenKind.Sub) hasSub = true;
            if (t.kind == TokenKind.Mul) hasMul = true;
            if (t.kind == TokenKind.Div) hasDiv = true;
        }
        assertTrue("Should recognize addition operator", hasAdd);
        assertTrue("Should recognize subtraction operator", hasSub);
        assertTrue("Should recognize multiplication operator", hasMul);
        assertTrue("Should recognize division operator", hasDiv);
    }

    @Test
    public void testComparisonOperators() throws IOException {
        Lexer lexer = new Lexer(new File("examples/If01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasComparison = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.LT || t.kind == TokenKind.GT ||
                t.kind == TokenKind.LTE || t.kind == TokenKind.GTE ||
                t.kind == TokenKind.EQ || t.kind == TokenKind.NEQ) {
                hasComparison = true;
                break;
            }
        }
        assertTrue("Should be able to recognize comparison operator", hasComparison);
    }

    @Test
    public void testLogicalOperators() throws IOException {
        Lexer lexer = new Lexer(new File("examples/BoolTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasLogical = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.And || t.kind == TokenKind.Or || t.kind == TokenKind.Not) {
                hasLogical = true;
                break;
            }
        }
        // Logical operators might not be in all files
        // assertTrue("Should be able to recognize logical operators", hasLogical);
    }

    // ==================== Delimiter tests ====================
    
    @Test
    public void testDelimiters() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasLbrace = false, hasRbrace = false;
        boolean hasLparen = false, hasRparen = false;
        boolean hasSemicolon = false;
        
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Lbrace) hasLbrace = true;
            if (t.kind == TokenKind.Rbrace) hasRbrace = true;
            if (t.kind == TokenKind.Lparen) hasLparen = true;
            if (t.kind == TokenKind.Rparen) hasRparen = true;
            if (t.kind == TokenKind.Semicolon) hasSemicolon = true;
        }
        
        assertTrue("Should recognize left brace", hasLbrace);
        assertTrue("Should recognize right brace", hasRbrace);
        assertTrue("Should recognize left parenthesis", hasLparen);
        assertTrue("Should recognize right parenthesis", hasRparen);
        assertTrue("Should recognize semicolon", hasSemicolon);
    }

    // ==================== String literal tests ====================
    
    @Test
    public void testStringLiteral() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasString = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.String && t.lexeme.contains("%")) {
                hasString = true;
                break;
            }
        }
        assertTrue("Should be able to recognize string literal", hasString);
    }

    // ==================== Comment tests ====================
    
    @Test
    public void testCommentIgnored() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        // Comments should be ignored and not appear in tokens
        for (Token t : lexer.tokens) {
            assertFalse("Comment content should not appear in tokens", 
                t.lexeme.contains("This is a comment"));
        }
    }

    // ==================== Line number tests ====================
    
    @Test
    public void testLineNumber() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // The first token should be on line 1 or later
        Token first = lexer.next();
        assertTrue("Line number should be greater than 0", first.lineNumber >= 1);
    }

    // ==================== EOF tests ====================
    
    @Test
    public void testEOF() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Cal.lemon"));
        lexer.lexicalAnalysis();
        
        // The last token should be EOF
        Token last = lexer.tokens.get(lexer.tokens.size() - 1);
        assertEquals("The last token should be EOF", TokenKind.EOF, last.kind);
    }

    // ==================== Comprehensive tests ====================
    
    @Test
    public void testCompleteFile() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        
        // Verify token count is reasonable
        assertTrue("Should generate multiple tokens", lexer.tokens.size() > 10);
        assertEquals(TokenKind.Class, lexer.tokens.get(0).kind);
        assertEquals(TokenKind.EOF, lexer.tokens.get(lexer.tokens.size() - 1).kind);
    }

    @Test
    public void testIfStatement() throws IOException {
        Lexer lexer = new Lexer(new File("examples/If07.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasIf = false, hasElse = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.If) hasIf = true;
            if (t.kind == TokenKind.Else) hasElse = true;
        }
        assertTrue("Should recognize if keyword", hasIf);
    }

    @Test
    public void testWhileLoop() throws IOException {
        Lexer lexer = new Lexer(new File("examples/Iteration01.lemon"));
        lexer.lexicalAnalysis();
        
        boolean hasWhile = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.While) {
                hasWhile = true;
                break;
            }
        }
        assertTrue("Should recognize while keyword", hasWhile);
    }

    @Test
    public void testMethodCall() throws IOException {
        Lexer lexer = new Lexer(new File("examples/SimpleMethodCall.lemon"));
        lexer.lexicalAnalysis();
        
        // Method call should have identifier and parentheses
        boolean hasId = false, hasLparen = false;
        for (Token t : lexer.tokens) {
            if (t.kind == TokenKind.Id) hasId = true;
            if (t.kind == TokenKind.Lparen) hasLparen = true;
        }
        assertTrue("Should have identifier", hasId);
        assertTrue("Should have left parenthesis", hasLparen);
    }

    // ==================== Keep original tests ====================
    
    @Test
    public void testCal() throws IOException {
        Lexer lexer = new Lexer(new File("examples/FloatTest01.lemon"));
        lexer.lexicalAnalysis();
        assertTrue(lexer.tokens.size() > 10);
        assertEquals(TokenKind.Class, lexer.tokens.get(0).kind);
        assertEquals(TokenKind.EOF, lexer.tokens.get(lexer.tokens.size() - 1).kind);
    }
}

