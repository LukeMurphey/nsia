/*
 * CodePress regular expressions for a list of ports
 */
 
// ports list (UDP\162, TCP\80, etc.)
Language.syntax = [ 
	{ input : /\b((UDP)\\([0-9]+)([ ]*\-[ ]*([0-9]+))?)\b/gi, output : '<b>$1</b>' }, // reserved words
	{ input : /\b((TCP)\\([0-9]+)([ ]*\-[ ]*([0-9]+))?)\b/gi, output : '<u>$1</u>' } // special words
]

Language.snippets = []

Language.complete = []

Language.shortcuts = []
