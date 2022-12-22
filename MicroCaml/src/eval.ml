open MicroCamlTypes
open Utils

exception TypeError of string
exception DeclareError of string
exception DivByZeroError 

(* Provided functions - DO NOT MODIFY *)

(* Adds mapping [x:v] to environment [env] *)
let extend env x v = (x, ref v)::env

(* Returns [v] if [x:v] is a mapping in [env]; uses the
   most recent if multiple mappings for [x] are present *)
let rec lookup env x =
  match env with
  | [] -> raise (DeclareError ("Unbound variable " ^ x))
  | (var, value)::t -> if x = var then !value else lookup t x

(* Creates a placeholder mapping for [x] in [env]; needed
   for handling recursive definitions *)
let extend_tmp env x = (x, ref (Int 0))::env

(* Updates the (most recent) mapping in [env] for [x] to [v] *)
let rec update env x v =
  match env with
  | [] -> raise (DeclareError ("Unbound variable " ^ x))
  | (var, value)::t -> if x = var then (value := v) else update t x v
        
(* Part 1: Evaluating expressions *)

(* Evaluates MicroCaml expression [e] in environment [env],
   returning a value, or throwing an exception on error *)
let rec eval_expr env e =
	match e with
	| Value v -> v
	| ID i -> lookup env i
	| Not e1 -> let v1 = eval_expr env e1 in
				(match v1 with
				| Bool b -> Bool (not b)
				| _ -> raise (TypeError ("Expected type bool")))
	| Binop (op, e1, e2) -> (match op with
							| Add -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> Int (i1 + i2)
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| Sub -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> Int (i1 - i2)
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| Mult -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> Int (i1 * i2)
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| Div -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> if i2 <> 0 then Int (i1 / i2) else raise DivByZeroError
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| Greater -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> if i1 > i2 then Bool true else Bool false
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| Less -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> if i1 < i2 then Bool true else Bool false
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| GreaterEqual -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> if i1 >= i2 then Bool true else Bool false
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| LessEqual -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Int i2 -> if i1 <= i2 then Bool true else Bool false
									 			| _ -> raise (TypeError ("Expected type int")))
									 | _ -> raise (TypeError ("Expected type int")))
							| Concat -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | String i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| String i2 -> String (i1 ^ i2)
									 			| _ -> raise (TypeError ("Expected type string")))
									 | _ -> raise (TypeError ("Expected type string")))
							| Equal -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			 (match v2 with
									 			 | Int i2 -> if i1 = i2 then Bool true else Bool false
									 			 | _ -> raise (TypeError ("Cannot compare types")))
									 | Bool i1 -> let v2 = eval_expr env e2 in
									 			 (match v2 with
									 			 | Bool i2 -> if i1 = i2 then Bool true else Bool false
									 			 | _ -> raise (TypeError ("Cannot compare types")))
									 | String i1 -> let v2 = eval_expr env e2 in
									 			 (match v2 with
									 			 | String i2 -> if i1 = i2 then Bool true else Bool false
									 			 | _ -> raise (TypeError ("Cannot compare types")))
									 | _ -> raise (TypeError ("Cannot compare types")))
							| NotEqual -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Int i1 -> let v2 = eval_expr env e2 in
									 			 (match v2 with
									 			 | Int i2 -> if i1 <> i2 then Bool true else Bool false
									 			 | _ -> raise (TypeError ("Cannot compare types")))
									 | Bool i1 -> let v2 = eval_expr env e2 in
									 			 (match v2 with
									 			 | Bool i2 -> if i1 <> i2 then Bool true else Bool false
									 			 | _ -> raise (TypeError ("Cannot compare types")))
									 | String i1 -> let v2 = eval_expr env e2 in
									 			 (match v2 with
									 			 | String i2 -> if i1 <> i2 then Bool true else Bool false
									 			 | _ -> raise (TypeError ("Cannot compare types")))
									 | _ -> raise (TypeError ("Cannot compare types")))
							| Or -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Bool i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Bool i2 -> Bool (i1 || i2)
									 			| _ -> raise (TypeError ("Expected type bool")))
									 | _ -> raise (TypeError ("Expected type bool")))
							| And -> let v1 = eval_expr env e1 in
									 (match v1 with
									 | Bool i1 -> let v2 = eval_expr env e2 in
									 			(match v2 with
									 			| Bool i2 -> Bool (i1 && i2)
									 			| _ -> raise (TypeError ("Expected type bool")))
									 | _ -> raise (TypeError ("Expected type bool"))))
	| If (e1, e2, e3) -> let v1 = eval_expr env e1 in
						 (match v1 with
						 | Bool b -> if b then eval_expr env e2 else eval_expr env e3
						 | _ -> raise (TypeError ("Expected type bool in guard")))
	| Let (v, b, e1, e2) -> if b then
								let tmp_env = extend_tmp env v in
								let v1 = eval_expr tmp_env e1 in
								let () = update tmp_env v v1 in
								eval_expr tmp_env e2
							else
								let v1 = eval_expr env e1 in
								eval_expr (extend env v v1) e2
	| Fun (v, e1) -> Closure (env, v, e1)
	| FunctionCall (e1, e2) -> let v1 = eval_expr env e1 in
							   (match v1 with
							   | Closure (a, v, e3) -> let v2 = eval_expr env e2 in
							   						   eval_expr (extend a v v2) e3
							   | _ -> raise (TypeError ("Not a function")))
(* Part 2: Evaluating mutop directive *)

(* Evaluates MicroCaml mutop directive [m] in environment [env],
   returning a possibly updated environment paired with
   a value option; throws an exception on error *)
let eval_mutop env m =
	match m with
	| Def (var, e1) -> let tmp_env = extend_tmp env var in
					   let v = eval_expr tmp_env e1 in
					   let () = update tmp_env var v in
					   (tmp_env, Some v)
	| Expr e1 -> let v = eval_expr env e1 in
				 (env, Some v)
	| NoOp -> (env, None)




