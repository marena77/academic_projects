open TokenTypes

(* PASTE YOUR LEXER FROM P4A HERE *)

let re_id = Str.regexp "[a-zA-Z][a-zA-Z0-9]*"
let re_let = Str.regexp "let$"
let re_def = Str.regexp "def$"
let re_in = Str.regexp "in$"
let re_rec = Str.regexp "rec$"
let re_fun = Str.regexp "fun$"
let re_not = Str.regexp "not$"
let re_if = Str.regexp "if$"
let re_then = Str.regexp "then$"
let re_else = Str.regexp "else$"
let re_true = Str.regexp "true$"
let re_false = Str.regexp "false$"
let re_int_pos = Str.regexp "[0-9]+"
let re_int_neg = Str.regexp "(-[0-9]+)"
let re_wspace = Str.regexp "[ \n\t]+"
let re_lparen = Str.regexp "("
let re_rparen = Str.regexp ")"
let re_equal = Str.regexp "="
let re_nequal = Str.regexp "<>"
let re_greater = Str.regexp ">"
let re_less = Str.regexp "<"
let re_greateq = Str.regexp ">="
let re_lesseq = Str.regexp "<="
let re_or = Str.regexp "||"
let re_and = Str.regexp "&&"
let re_add = Str.regexp "+"
let re_sub = Str.regexp "-"
let re_mult = Str.regexp "*"
let re_div = Str.regexp "/"
let re_concat = Str.regexp "\\^"
let re_arrow = Str.regexp "->"
let re_dsemi = Str.regexp ";;"
let re_string = Str.regexp "\"[^\"]*\""

let tokenize input = 
	let rec tok pos s =
		if pos >= String.length s then
			[]
		else
			if (Str.string_match re_id s pos) then

				let token = Str.matched_string s in
				if Str.string_match re_let token 0 then
				(Tok_Let)::(tok (pos + 3) s) else
				if Str.string_match re_def token 0 then
				(Tok_Def)::(tok (pos + 3) s) else
				if Str.string_match re_in token 0 then
				(Tok_In)::(tok (pos + 2) s) else
				if Str.string_match re_rec token 0 then
				(Tok_Rec)::(tok (pos + 3) s) else
				if Str.string_match re_fun token 0 then
				(Tok_Fun)::(tok (pos + 3) s) else
				if Str.string_match re_not token 0 then
				(Tok_Not)::(tok (pos + 3) s) else
				if Str.string_match re_if token 0 then
				(Tok_If)::(tok (pos + 2) s) else
				if Str.string_match re_then token 0 then
				(Tok_Then)::(tok (pos + 4) s) else
				if Str.string_match re_else token 0 then
				(Tok_Else)::(tok (pos + 4) s) else
				if Str.string_match re_true token 0 then
				(Tok_Bool (true))::(tok (pos + 4) s) else
				if Str.string_match re_false token 0 then
				(Tok_Bool (false))::(tok (pos + 5) s) else
				(Tok_ID token)::(tok (pos + (String.length token)) s)

			else if (Str.string_match re_int_pos s pos) then
				let token = Str.matched_string s in
				(Tok_Int (int_of_string token))::(tok (pos + (String.length token)) s)
			else if (Str.string_match re_int_neg s pos) then
				let token = Str.matched_string s in
				(Tok_Int (int_of_string (Str.global_replace (Str.regexp "(\\|)") "" token)))::(tok (pos + (String.length token)) s)
			else if (Str.string_match re_lparen s pos) then
				(Tok_LParen)::(tok (pos + 1) s)
			else if (Str.string_match re_rparen s pos) then
				(Tok_RParen)::(tok (pos + 1) s)
			else if (Str.string_match re_arrow s pos) then
				(Tok_Arrow)::(tok (pos + 2) s)
			else if (Str.string_match re_equal s pos) then
				(Tok_Equal)::(tok (pos + 1) s)	
			else if (Str.string_match re_nequal s pos) then
				(Tok_NotEqual)::(tok (pos + 2) s)
			else if (Str.string_match re_greater s pos) then
				(Tok_Greater)::(tok (pos + 1) s)
			else if (Str.string_match re_less s pos) then
				(Tok_Less)::(tok (pos + 1) s)
			else if (Str.string_match re_greateq s pos) then
				(Tok_GreaterEqual)::(tok (pos + 2) s)
			else if (Str.string_match re_lesseq s pos) then
				(Tok_LessEqual)::(tok (pos + 2) s)	
			else if (Str.string_match re_or s pos) then
				(Tok_Or)::(tok (pos + 2) s)
			else if (Str.string_match re_and s pos) then
				(Tok_And)::(tok (pos + 2) s)
			else if (Str.string_match re_add s pos) then
				(Tok_Add)::(tok (pos + 1) s)
			else if (Str.string_match re_sub s pos) then
				(Tok_Sub)::(tok (pos + 1) s)
			else if (Str.string_match re_mult s pos) then
				(Tok_Mult)::(tok (pos + 1) s)
			else if (Str.string_match re_div s pos) then
				(Tok_Div)::(tok (pos + 1) s)
			else if (Str.string_match re_concat s pos) then
				(Tok_Concat)::(tok (pos + 1) s)	
			else if (Str.string_match re_dsemi s pos) then
				(Tok_DoubleSemi)::(tok (pos + 2) s)
			else if (Str.string_match re_string s pos) then
				let token = Str.matched_string s in
				(Tok_String (Str.global_replace (Str.regexp "\"") "" token))::(tok (pos + (String.length token)) s)	
	
			else if (Str.string_match re_wspace s pos) then
				let token = Str.matched_string s in
				tok (pos +(String.length token)) s
			else
				raise (InvalidInputException "bad input")
				
	in tok 0 input