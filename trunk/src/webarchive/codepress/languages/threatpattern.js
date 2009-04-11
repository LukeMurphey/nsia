/*
 * CodePress regular expressions for ThreatSignature syntax highlighting
 */
 
// ThreatSignature
Language.syntax = [ 
	{ input : /\"(.*?)(\"|<br>|<\/P>)/g, output : '<s>"$1$2</s>' }, // strings double quote

	{ input : /\b(alert|block|eval|evaluate)\b/gi, output : '<b>$1</b>' }, // reserved words
	{ input : /\b(IfSet|URI|BasicEncoding|Severity|ContentType|Set|UnSet|ID|IfNotSet|Message|Reference|Regex|ByteTest|Byte|Offset|Within|NoCase|IgnoreCase|Version|String|Toggle)\b/gi, output : '<u>$1</u>' }, // special words
	{ input : /([^:]|^)\/\/(.*?)(<br|<\/P)/g, output : '$1<i>//$2</i>$3' }, // comments //
	{ input : /\/\*(.*?)\*\//g, output : '<i>/*$1*/</i>' } // comments /* */
]

Language.snippets = []

Language.complete = [
	{ input : '\'',output : '\'$0\'' },
	{ input : '"', output : '"$0"' },
	{ input : '(', output : '\($0\)' },
	{ input : '[', output : '\[$0\]' },
	{ input : '{', output : '{\n\t$0\n}' }		
]

Language.shortcuts = []
