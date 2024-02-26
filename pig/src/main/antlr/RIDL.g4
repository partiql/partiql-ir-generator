grammar RIDL;

document
  : include* definition+ EOF
  ;

include
  : '(' INCLUDE TEXT ')'
  ;

type
  : primitive
  | container
  | path
  ;

primitive
  : BOOL
  | INT32
  | INT64
  | FLOAT32
  | FLOAT64
  | DECIMAL
  | STRING
  | BYTES
  ;

container
  : list
  | map
  | set
  ;

list
  : '(' LIST type ')'
  ;

map
  : '(' MAP k=type v=type ')'
  ;

set
  : '(' SET type ')'
  ;

path
  : NAME                            #PathSymbol
  | '\'' NAME ('.' NAME)* '\''      #PathRelative
  | '\'' '.' NAME ('.' NAME)* '\''  #PathAbsolute
  ;

definition
  : product
  | sum
  | enum
  | fixed
  | unit
  ;

product
  : '(' PRODUCT NAME operand+ definition* ')'
  ;

operand
  : NAME COCO type
  ;

sum
  : '(' SUM NAME variant* ')'
  ;

variant
  : product
  | sum
  ;

enum
  : '(' ENUM NAME enumerators ')'
  ;

enumerators
  : '(' enumerator+ ')';

enumerator
  : LETTER+
  ;

fixed
  : '(' FIXED NAME int ')'
  ;

unit
  : '(' UNIT NAME ')'
  ;

int
  : DIGIT+
  ;

INCLUDE: 'include';
BOOL: 'bool';
INT32: 'int32';
INT64: 'int64';
FLOAT32: 'float32';
FLOAT64: 'float64';
DECIMAL: 'decimal';
STRING: 'string';
BYTES: 'bytes';
LIST: 'list';
MAP: 'map';
SET: 'set';
PRODUCT: 'product';
SUM: 'sum';
ENUM: 'enum';
FIXED: 'fixed';
UNIT: 'unit';

COCO: '::';
DIGIT: [0-9];
LETTER: [A-Z];

TEXT
  : '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"'
  ;

NAME
  : [a-z_]+
  ;

COMMENT_LINE
  : '//' ~[\r\n]* '\r'? '\n'? -> skip
  ;

COMMENT_BLOCK
  : '/*' .*? '*/' -> skip
  ;

WS
  : [ \r\n\t]+ -> skip
  ;

UNRECOGNIZED
  : .
  ;
