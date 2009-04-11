/*
 * CodePress regular expressions for syntax highlighting a list of URLs
 */
 
// List of URLs
/*
Language.syntax = [ 	{ input : /\b((http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?)\b/gi, output : '<u>$0</u>' } // URL
]

Language.syntax = [ 	{ input : /\b(https?:\/\/([-\w\.]+)+(:\d+)?(\/([\w/_\.]*(\?\S+)?)?)?)\b/gi, output : '<u>$1</u>' } // URL
]

Language.syntax = [ 	{ input : /\b(https?:\/\/[-a-zA-Z.]+\.[a-zA-Z]+(\:[0-9]+)?)\b/gi, output : '<u>$1</u>' } // URL
]

Language.syntax = [ 	{ input : /\b(https?:\/\/([-a-zA-Z0-9]+\.)+[a-zA-Z]+(\:[0-9]+)?)\b/gi, output : '<u>$1</u>' } // URL
]

Language.syntax = [ 	{ input : /\b(https?)\b/gi, output : '<u>$1</u>' } // URL
]
*/

/*Language.syntax = [ 	{ input : /\b(https?:\/\/([-a-zA-Z0-9]+\.)+[a-zA-Z]+(\:[0-9]+)?(\/([\w#!:.?+=&%@!\-\/])*)?)\b/gmi, output : '<u>$1</u>' } // URL
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
