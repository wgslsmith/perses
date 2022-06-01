// Based on https://github.com/gpuweb/gpuweb/blob/a8e20cf4b10982b5d505d9a3a7f30995523c0297/wgsl/index.bs

grammar WGSL;

fragment EOL: '\r\n' | '\n';

WHITESPACE: [ \t\n\r]+ -> skip;

// TODO: Block comments
COMMENT: '//' .*? EOL -> skip;

// Literals

BOOL_LITERAL: 'false' | 'true';
INT_LITERAL: ('0' [xX] [0-9a-fA-F]+ | '0' | [1-9][0-9]*) [iu]?;

// Type-defining keywords

ARRAY: 'array';
BOOL: 'bool';
FLOAT32: 'f32';
INT32: 'i32';
MAT2X2: 'mat2x2';
MAT2X3: 'mat2x3';
MAT2X4: 'mat2x4';
MAT3X2: 'mat3x2';
MAT3X3: 'mat3x3';
MAT3X4: 'mat3x4';
MAT4X2: 'mat4x2';
MAT4X3: 'mat4x3';
MAT4X4: 'mat4x4';
STRUCT: 'struct';
UINT32: 'u32';
VEC2: 'vec2';
VEC3: 'vec3';
VEC4: 'vec4';

// Other keywords

BREAK: 'break';
CASE: 'case';
CONTINUE: 'continue';
CONTINUING: 'continuing';
DEFAULT: 'default';
ELSE: 'else';
FALLTHROUGH: 'fallthrough';
FN: 'fn';
FOR: 'for';
FUNCTION: 'function';
IF: 'if';
LET: 'let';
LOOP: 'loop';
OVERRIDE: 'override';
PRIVATE: 'private';
READ: 'read';
READ_WRITE: 'read_write';
RETURN: 'return';
STORAGE: 'storage';
SWITCH: 'switch';
TYPE: 'type';
UNIFORM: 'uniform';
VAR: 'var';
WHILE: 'while';
WORKGROUP: 'workgroup';
WRITE: 'write';

// Syntactic tokens

AND: '&';
AND_AND: '&&';
ARROW: '->';
ATTR: '@';
FORWARD_SLASH: '/';
BANG: '!';
BRACKET_LEFT: '[';
BRACKET_RIGHT: ']';
BRACE_LEFT: '{';
BRACE_RIGHT: '}';
COLON: ':';
COMMA: ',';
EQUAL: '=';
EQUAL_EQUAL: '==';
NOT_EQUAL: '!=';
GREATER_THAN: '>';
GREATER_THAN_EQUAL: '>=';
LESS_THAN: '<';
LESS_THAN_EQUAL: '<=';
MODULO: '%';
MINUS: '-';
MINUS_MINUS: '--';
PERIOD: '.';
PLUS: '+';
PLUS_PLUS: '++';
OR: '|';
OR_OR: '||';
PAREN_LEFT: '(';
PAREN_RIGHT: ')';
SEMICOLON: ';';
STAR: '*';
TILDE: '~';
UNDERSCORE: '_';
XOR: '^';
PLUS_EQUAL: '+=';
MINUS_EQUAL: '-=';
TIMES_EQUAL: '*=';
DIVISION_EQUAL: '/=';
MODULO_EQUAL: '%=';
AND_EQUAL: '&=';
OR_EQUAL: '|=';
XOR_EQUAL: '^=';

IDENT: [_\p{XID_Start}] [\p{XID_Continue}]+ | [\p{XID_Start}];

// Literals

int_literal: INT_LITERAL;
bool_literal: BOOL_LITERAL;
const_literal: int_literal | bool_literal;

// Attributes

attribute: ATTR IDENT PAREN_LEFT (literal_or_ident COMMA)* literal_or_ident COMMA? PAREN_RIGHT
         | BRACKET_LEFT BRACKET_LEFT (IDENT (PAREN_LEFT (literal_or_ident COMMA)* literal_or_ident COMMA? PAREN_RIGHT)? COMMA)* IDENT (PAREN_LEFT (literal_or_ident COMMA)* literal_or_ident COMMA? PAREN_RIGHT)? BRACKET_RIGHT BRACKET_RIGHT
         | ATTR IDENT;

literal_or_ident: INT_LITERAL | IDENT;

// Types

array_type_decl: ARRAY LESS_THAN type_decl (COMMA element_count_expression)? GREATER_THAN;
element_count_expression: INT_LITERAL | IDENT;

struct_decl: STRUCT IDENT struct_body_decl SEMICOLON?;
struct_body_decl: BRACE_LEFT (struct_member (COMMA | SEMICOLON))* struct_member (COMMA | SEMICOLON)? BRACE_RIGHT;
struct_member: attribute* variable_ident_decl;

access_mode: READ | WRITE | READ_WRITE;
address_space: FUNCTION | PRIVATE | WORKGROUP | UNIFORM | STORAGE;

type_alias_decl: TYPE IDENT EQUAL type_decl;

type_decl: IDENT | type_decl_without_ident;

type_decl_without_ident: BOOL
                       | FLOAT32
                       | INT32
                       | UINT32
                       | vec_prefix LESS_THAN type_decl GREATER_THAN
                       | mat_prefix LESS_THAN type_decl GREATER_THAN
                       | array_type_decl;

vec_prefix: VEC2 | VEC3 | VEC4;
mat_prefix: MAT2X2
          | MAT2X3
          | MAT2X4
          | MAT3X2
          | MAT3X3
          | MAT3X4
          | MAT4X2
          | MAT4X3
          | MAT4X4;

// Variables

variable_statement: variable_decl
                  | variable_decl EQUAL expression
                  | LET (IDENT | variable_ident_decl) EQUAL expression;

variable_decl: VAR variable_qualifier? (IDENT | variable_ident_decl);
variable_ident_decl: IDENT COLON type_decl;
variable_qualifier: LESS_THAN address_space (COMMA access_mode)? GREATER_THAN;
global_variable_decl: attribute* variable_decl (EQUAL const_expression)?;
global_constant_decl: LET (IDENT | variable_ident_decl) EQUAL const_expression
                    | attribute* OVERRIDE (IDENT | variable_ident_decl) (EQUAL expression)?;
const_expression: type_decl PAREN_LEFT ((const_expression COMMA)* const_expression COMMA?)? PAREN_RIGHT
                | const_literal;

// Expressions

primary_expression: IDENT
          | callable_val argument_expression_list
          | const_literal
          | PAREN_LEFT expression PAREN_RIGHT;

callable_val: IDENT
            | type_decl_without_ident
            | vec_prefix
            | mat_prefix;

argument_expression_list: PAREN_LEFT ((expression COMMA)* expression COMMA?)? PAREN_RIGHT;

postfix_expression: BRACKET_LEFT expression BRACKET_RIGHT postfix_expression?
                  | PERIOD IDENT postfix_expression?;

unary_expression: singular_expression
                | MINUS unary_expression
                | BANG unary_expression
                | TILDE unary_expression
                | STAR unary_expression
                | AND unary_expression;

singular_expression: primary_expression postfix_expression?;

lhs_expression: (STAR | AND)* core_lhs_expression postfix_expression?;

core_lhs_expression: IDENT | PAREN_LEFT lhs_expression PAREN_RIGHT;

multiplicative_expression: unary_expression
                         | multiplicative_expression STAR unary_expression
                         | multiplicative_expression FORWARD_SLASH unary_expression
                         | multiplicative_expression MODULO unary_expression;

additive_expression: multiplicative_expression
                   | additive_expression PLUS multiplicative_expression
                   | additive_expression MINUS multiplicative_expression;

shift_expression: additive_expression
                | shift_expression GREATER_THAN GREATER_THAN additive_expression
                | shift_expression LESS_THAN LESS_THAN additive_expression;

relational_expression: shift_expression
                     | shift_expression LESS_THAN shift_expression
                     | shift_expression GREATER_THAN shift_expression
                     | shift_expression LESS_THAN_EQUAL shift_expression
                     | shift_expression GREATER_THAN_EQUAL shift_expression
                     | shift_expression EQUAL_EQUAL shift_expression
                     | shift_expression NOT_EQUAL shift_expression;

short_circuit_and_expression: relational_expression
                            | short_circuit_and_expression AND_AND relational_expression;

short_circuit_or_expression: relational_expression
                           | short_circuit_or_expression OR_OR relational_expression;

binary_or_expression: unary_expression
                    | binary_or_expression OR unary_expression;

binary_and_expression: unary_expression
                     | binary_and_expression AND unary_expression;

binary_xor_expression: unary_expression
                     | binary_xor_expression XOR unary_expression;

expression: relational_expression
          | short_circuit_or_expression OR_OR relational_expression
          | short_circuit_and_expression AND_AND relational_expression
          | binary_and_expression AND unary_expression
          | binary_or_expression OR unary_expression
          | binary_xor_expression XOR unary_expression;

// Statements

compound_statement: BRACE_LEFT statement* BRACE_RIGHT;

assignment_statement: lhs_expression (EQUAL | compound_assignment_operator) expression
                    | UNDERSCORE EQUAL expression;

compound_assignment_operator: PLUS_EQUAL
                            | MINUS_EQUAL
                            | TIMES_EQUAL
                            | DIVISION_EQUAL
                            | MODULO_EQUAL
                            | AND_EQUAL
                            | OR_EQUAL
                            | XOR_EQUAL;

increment_statement: lhs_expression PLUS_PLUS;
decrement_statement: lhs_expression MINUS_MINUS;

if_statement: IF expression compound_statement (ELSE else_statement)?;
else_statement: compound_statement | if_statement;

switch_statement: SWITCH expression BRACE_LEFT switch_body+ BRACE_RIGHT;
switch_body: CASE case_selectors COLON? case_compound_statement
           | DEFAULT COLON? case_compound_statement;
case_selectors: const_literal (COMMA const_literal)* COMMA?;
case_compound_statement: BRACE_LEFT statement* fallthrough_statement? BRACE_RIGHT;
fallthrough_statement: FALLTHROUGH SEMICOLON;

loop_statement: LOOP BRACE_LEFT statement* continuing_statement? BRACE_RIGHT;

for_statement: FOR PAREN_LEFT for_header PAREN_RIGHT compound_statement;
for_header: for_init? SEMICOLON expression? SEMICOLON for_update?;
for_init: variable_statement
        | increment_statement
        | decrement_statement
        | assignment_statement
        | func_call_statement;
for_update: increment_statement
          | decrement_statement
          | assignment_statement
          | func_call_statement;

while_statement: WHILE expression compound_statement;

break_statement: BREAK;
break_if_statement: BREAK IF expression SEMICOLON;
continue_statement: CONTINUE;
continuing_statement: CONTINUING continuing_compound_statement;
continuing_compound_statement: BRACE_LEFT statement* break_if_statement? BRACE_RIGHT;
return_statement: RETURN expression?;
func_call_statement: IDENT argument_expression_list;

statement: SEMICOLON
         | return_statement SEMICOLON
         | if_statement
         | switch_statement
         | loop_statement
         | for_statement
         | while_statement
         | func_call_statement SEMICOLON
         | variable_statement SEMICOLON
         | break_statement SEMICOLON
         | continue_statement SEMICOLON
         | assignment_statement SEMICOLON
         | compound_statement
         | increment_statement SEMICOLON
         | decrement_statement SEMICOLON;

// Functions

function_decl: attribute* function_header compound_statement;
function_header: FN IDENT PAREN_LEFT param_list? PAREN_RIGHT (ARROW attribute* type_decl)?;
param_list: (param COMMA)* param COMMA?;
param: attribute* variable_ident_decl;

// Program

translation_unit: global_decl* EOF;
global_decl: SEMICOLON
           | global_variable_decl SEMICOLON
           | global_constant_decl SEMICOLON
           | type_alias_decl SEMICOLON
           | struct_decl
           | function_decl;
