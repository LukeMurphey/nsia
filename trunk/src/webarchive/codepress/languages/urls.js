/*
 * CodePress regular expressions for syntax highlighting a list of URLs
 */
 
// List of URLs
/*
Language.syntax = [ 
]

Language.syntax = [ 
]

Language.syntax = [ 
]

Language.syntax = [ 
]

Language.syntax = [ 
]
*/

/*Language.syntax = [ 
]*/

Language.syntax = [ 
	{ input : /\b(https?:\/\/([-a-zA-Z0-9]+\.)+[a-zA-Z]+(\:[0-9]+)?[\w#!:.?+=&%@!\-\/\\;]*)\b/gi, output : '<u>$1</u>' } // URL
]
//[\w#!:.?+=%@!\-\/]*
//[\w#!:.?+=&%@!\-\/\\;]*

//(\/(.)*)?)

Language.snippets = []

Language.complete = []

Language.shortcuts = []