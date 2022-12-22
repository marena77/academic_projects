open MicroCamlTypes
open Utils
open TokenTypes

(* Provided functions - DO NOT MODIFY *)

(* Matches the next token in the list, throwing an error if it doesn't match the given token *)
let match_token (toks: token list) (tok: token) =
  match toks with
  | [] -> raise (InvalidInputException(string_of_token tok))
  | h::t when h = tok -> t
  | h::_ -> raise (InvalidInputException(
      Printf.sprintf "Expected %s from input %s, got %s"
        (string_of_token tok)
        (string_of_list string_of_token toks)
        (string_of_token h)))

(* Matches a sequence of tokens given as the second list in the order in which they appear, throwing an error if they don't match *)
let match_many (toks: token list) (to_match: token list) =
  List.fold_left match_token toks to_match

(* Return the next token in the token list as an option *)
let lookahead (toks: token list) = 
  match toks with
  | [] -> None
  | h::t -> Some h

(* Return the token at the nth index in the token list as an option*)
let rec lookahead_many (toks: token list) (n: int) = 
  match toks, n with
  | h::_, 0 -> Some h
  | _::t, n when n > 0 -> lookahead_many t (n-1)
  | _ -> None

(* Part 2: Parsing expressions *)

let rec parse_expr toks = 
	match (lookahead toks) with
	| Some Tok_Let -> parse_let toks
	| Some Tok_If -> parse_if toks
	| Some Tok_Fun -> parse_fun toks
	| _ -> parse_or toks

and parse_let toks =
	let toks2 = match_token toks Tok_Let in
	match (lookahead toks2) with
	| Some Tok_Rec -> let toks3 = match_token toks2 Tok_Rec in
		(match (lookahead toks3) with
		| Some Tok_ID i -> let toks4 = match_token toks3 (Tok_ID i) in
						   let toks5 = match_token toks4 Tok_Equal in
						   let (toks6, expr) = parse_expr toks5 in
						   let toks7 = match_token toks6 Tok_In in
						   let (toks8, expr2) = parse_expr toks7 in
						   (toks8, Let(i, true, expr, expr2))
		| _ -> raise (InvalidInputException "not recognized"))

	| Some Tok_ID i -> let toks4 = match_token toks2 (Tok_ID i) in
						   let toks5 = match_token toks4 Tok_Equal in
						   let (toks6, expr) = parse_expr toks5 in
						   let toks7 = match_token toks6 Tok_In in
						   let (toks8, expr2) = parse_expr toks7 in
						   (toks8, Let(i, false, expr, expr2))
	| _ -> raise (InvalidInputException "not recognized")

and parse_fun toks =
	let toks2 = match_token toks Tok_Fun in
	match (lookahead toks2) with
	| Some Tok_ID i -> let toks3 = match_token toks2 (Tok_ID i) in
					   let toks4 = match_token toks3 Tok_Arrow in
					   let (toks5, expr) = parse_expr toks4 in
					   (toks5, Fun((i), expr))
	| _ -> raise (InvalidInputException "not recognized")

and parse_if toks =
	let toks2 = match_token toks Tok_If in
	let (toks3, expr) = parse_expr toks2 in
	let toks4 = match_token toks3 Tok_Then in
	let (toks5, expr2) = parse_expr toks4 in
	let toks6 = match_token toks5 Tok_Else in
	let (toks7, expr3) = parse_expr toks6 in
	(toks7, If(expr, expr2, expr3))

and parse_or toks =
	let (toks2, expr) = parse_and toks in
	match (lookahead toks2) with
	| Some Tok_Or -> let toks3 = match_token toks2 Tok_Or in
					  let (toks4, expr2) = parse_or toks3 in
					  (toks4, Binop(Or, expr, expr2))
	| _ -> (toks2, expr)

and parse_and toks =
	let (toks2, expr) = parse_equal toks in
	match (lookahead toks2) with
	| Some Tok_And -> let toks3 = match_token toks2 Tok_And in
					  let (toks4, expr2) = parse_and toks3 in
					  (toks4, Binop(And, expr, expr2))
	| _ -> (toks2, expr)

and parse_equal toks = 
	let (toks2, expr) = parse_relate toks in
	match (lookahead toks2) with
	| Some Tok_Equal -> let toks3 = match_token toks2 Tok_Equal in
					  let (toks4, expr2) = parse_equal toks3 in
					  (toks4, Binop(Equal, expr, expr2))
	| Some Tok_NotEqual -> let toks3 = match_token toks2 Tok_NotEqual in
					  let (toks4, expr2) = parse_equal toks3 in
					  (toks4, Binop(NotEqual, expr, expr2))
	| _ -> (toks2, expr)

and parse_relate toks = 
	let (toks2, expr) = parse_add toks in
	match (lookahead toks2) with 
	| Some Tok_Greater -> let toks3 = match_token toks2 Tok_Greater in
						  let (toks4, expr2) = parse_relate toks3 in
						  (toks4, Binop(Greater, expr, expr2))
	| Some Tok_Less -> let toks3 = match_token toks2 Tok_Less in
					   let (toks4, expr2) = parse_relate toks3 in
					   (toks4, Binop(Less, expr, expr2))
	| Some Tok_GreaterEqual -> let toks3 = match_token toks2 Tok_GreaterEqual in
						  	   let (toks4, expr2) = parse_relate toks3 in
						  	   (toks4, Binop(GreaterEqual, expr, expr2))
	| Some Tok_LessEqual -> let toks3 = match_token toks2 Tok_LessEqual in
						    let (toks4, expr2) = parse_relate toks3 in
						    (toks4, Binop(LessEqual, expr, expr2))
	| _ -> (toks2, expr)

and parse_add toks =
	let (toks2, expr) = parse_mult toks in
	match (lookahead toks2) with
	| Some Tok_Add -> let toks3 = match_token toks2 Tok_Add in
					  let (toks4, expr2) = parse_add toks3 in
					  (toks4, Binop(Add, expr, expr2))
	| Some Tok_Sub -> let toks3 = match_token toks2 Tok_Sub in
					  let (toks4, expr2) = parse_add toks3 in
					  (toks4, Binop(Sub, expr, expr2))
	| _ -> (toks2, expr)

and parse_mult toks = 
	let (toks2, expr) = parse_concat toks in
	match (lookahead toks2) with
	| Some Tok_Mult -> let toks3 = match_token toks2 Tok_Mult in
					   let (toks4, expr2) = parse_mult toks3 in
					   (toks4, Binop(Mult, expr, expr2))
	| Some Tok_Div -> let toks3 = match_token toks2 Tok_Div in
					  let (toks4, expr2) = parse_mult toks3 in
					  (toks4, Binop(Div, expr, expr2))
	| _ -> (toks2, expr)

and parse_concat toks =
	let (toks2, expr) = parse_unary toks in
	match (lookahead toks2) with
	| Some Tok_Concat -> let toks3 = match_token toks2 Tok_Concat in
					let (toks4, expr2) = parse_concat toks3 in
					(toks4, Binop(Concat, expr, expr2))
	| _ -> (toks2, expr) 


and parse_unary toks =
	match lookahead toks with
	| Some Tok_Not -> let toks2 = match_token toks Tok_Not in
					  let (toks3, expr) = parse_unary toks2 in
					  (toks3, (Not (expr)))
	| _ -> parse_func_call toks

and parse_func_call toks = 
	let (toks2, expr) = parse_prim_expr toks in
	match (lookahead toks2) with
	| Some Tok_Int _ | Some Tok_Bool _ | Some Tok_String _ | Some Tok_ID _ | Some Tok_LParen -> 
	let (toks3, expr2) = parse_prim_expr toks2 in
	(toks3, (FunctionCall (expr, expr2)))
	| _ -> (toks2, expr)

and parse_prim_expr toks =
	match lookahead toks with
	| Some Tok_Int i -> let toks2 = match_token toks (Tok_Int i) in
				   (toks2, Value (Int i))
	| Some Tok_Bool b -> let toks2 = match_token toks (Tok_Bool b) in
					(toks2, Value (Bool b))
	| Some Tok_String s -> let toks2 = match_token toks (Tok_String s) in
					  (toks2, Value (String s))
	| Some Tok_ID s -> let toks2 = match_token toks (Tok_ID s) in
				  (toks2, ID s)
	| Some Tok_LParen -> let toks2 = match_token toks Tok_LParen in
					let (toks3, expr) = parse_expr toks2 in
					let toks4 = match_token toks3 Tok_RParen in
					(toks4, expr)
	| _ -> raise (InvalidInputException "not recognized")


(* Part 3: Parsing mutop *)

let rec parse_mutop toks = 
	match (lookahead toks) with
	| Some Tok_Def -> parse_def toks
	| Some Tok_DoubleSemi -> ([], NoOp)
	| _ -> parse_mexpr toks

and parse_def toks = 
	let toks2 = match_token toks Tok_Def in
	match (lookahead toks2) with
	| Some Tok_ID i -> let toks9 = match_token toks2 (Tok_ID i) in
					   let toks3 = match_token toks9 Tok_Equal in
					   let (toks4, expr) = parse_expr toks3 in
					   let toks5 = match_token toks4 Tok_DoubleSemi in
						(toks5, Def (i, expr))

	| _ -> raise (InvalidInputException "not recognized")

and parse_mexpr toks = 
	let (toks2, expr) = parse_expr toks in
	let toks3 = match_token toks2 Tok_DoubleSemi in
	(toks3, Expr(expr))