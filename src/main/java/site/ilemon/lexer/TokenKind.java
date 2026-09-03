package site.ilemon.lexer;

public enum TokenKind {

		/*** End of file identifier ****/
        EOF,

		/*** Keyword ****/
		Class,
		Main,
		Void,
		String,
		Int,
		Float,
		Double,
		Bool,
		Byte,
		Long,
		While,
		For,
		True,
		False,
		If,
		Else,
		Printf,
		Return,
		Break,
		Continue,

		/*** Arithmetic operator ****/
		Add,
		Sub,
		Mul,
		Div,
		Mod,

		/*** Comparison operator ****/
		LT,			// <
		GT,			// >
		LTE,		// <=
		GTE,		// >=
		EQ,			// ==
		NEQ,		// !=

		/*** Relational operator ****/
		And,		// &&
		Or,			// ||
		Not,		// !

		/*** Delimiter ****/
		DoubleQuotation,	// "
		Lbrace,				// {
		Rbrace,				// }
		Lparen,				// (
		Rparen,				// )
		Lbracket,			// [
		Rbracket,			// ]
		Semicolon,			// ;
		Comma,				// ,
		Dot,				// .

		Id,
		Num,
		FloatLiteral,
		Assign,
		Unknown, PrintLine,

	}
