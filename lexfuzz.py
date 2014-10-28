#! /usr/bin/env python
"""Prints a randomly generated string to stdout, and the corresponding
list of tokens (in the format --lextest produces) to stderr"""

from enum import Enum
import random
import re
import string
import sys

keywords = ["abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "double", "do", "else", "enum", "extends", "false", "finally", "final", "float", "for", "goto", "if", "implements", "import", "instanceof", "interface", "int", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throws", "throw", "transient", "true", "try", "void", "volatile", "while"]

operators = [':', '!=', '!', '(', ')', '*=', '*', '++', '+=', '+', ',', '-=', '--', '-', '.', '/=', '/', ':', ';', '<<=', '<<', '<=', '<', '==', '=', '>=', '>>=', '>>>=', '>>>', '>>', '>', '?', '%=', '%', '&=', '&&', '&', '[', ']', '^=', '^', '{', '}', '~', '|=', '||', '|']

needs_make_whitespace = [('!','='), ('*','='), ('+','+'), ('+','='), ('-','='), ('-','-'), ('/','='), ('<<','='), ('<','<'), ('<','='), ('=','='), ('>','='), ('>>','='), ('>>>','='), ('>','>>'), ('>>','>'), ('>','>'), ('%','='), ('&','='), ('&','&'), ('^','='), ('|','=')]

class Token(Enum):
    T_WHITESPACE = 0
    T_IDENT = 1
    T_INTEGER = 2
    T_KEYWORD = 3
    T_OPERATOR = 4

WHITESPACE_PROB = 50 # percent
STRING_LENGTH = 30
COMMENT_PROB = 5
COMMENT_LENGTH = 10
COMMENT_NESTING_PROB = 10 # percent

def _make_comment():
    length = random.randrange(COMMENT_LENGTH)
    c = "/*"
    while random.randrange(100) < COMMENT_NESTING_PROB:
        c += get_string(length, False) + "/*"
    c += get_string(length, False) + "*/"
    return c

def _make_whitespace(comments=True):
    wtsp = ''
    for _ in range(random.randrange(10) + 1):
        ws = ['\n','\r','\t',' ']
        ws_type = random.choice(ws)

        if comments and random.randrange(100) < COMMENT_PROB:
            ws_type = None  # None -> comment

        if ws_type:
            wtsp += ws_type
        else:
            wtsp += _make_comment()
    return wtsp

def _make_identifier():
    """We cheat by making every identifier include a keyword, which are non-overlapping
    Thus we can be sure not to accidentally generate a keyword"""
    ident = ''
    def letter():
        r = random.randrange(100)
        if r < 33:
            return random.choice(string.ascii_letters)
        if r < 66:
            return '_'
        return str(random.randrange(20))

    if random.getrandbits(1):
        ident += letter()
    kw = random.choice(keywords)
    ident += kw

    if random.getrandbits(1) or len(ident) == len(kw):
        ident += letter()
    return ident

def _make_integer():
    return str(random.getrandbits(32))

def _is_var(tok):
    ident = r'[a-zA-Z0-9_]'
    return re.match(ident, tok[0]) and re.match(ident, tok[-1])




def _pad_tokens(tokens, comments=True):
    """Some tokens need to be padded with whitespace, e.g. two consecutive keywords"""
    padded_tokens = []
    last_token = None
    for t_type, t in tokens:
        if last_token and _is_var(last_token) and _is_var(t):
            padded_tokens.append((Token.T_WHITESPACE, _make_whitespace(comments)))
        else:
            for tup in needs_make_whitespace:
                if last_token and last_token == tup[0] and t == tup[1]:
                    padded_tokens.append((Token.T_WHITESPACE, _make_whitespace(comments)))
        padded_tokens.append((t_type, t))

        last_token = t
        if random.randrange(100) < WHITESPACE_PROB:
            wtsp = _make_whitespace(comments)
            padded_tokens.append((Token.T_WHITESPACE, wtsp))
            last_token = wtsp
    return padded_tokens

def _random_token():
    r = random.randrange(100)
    if r < 30:
        return (Token.T_KEYWORD, random.choice(keywords))
    if r < 60:
        return (Token.T_OPERATOR, random.choice(operators))
    if r < 80:
        return (Token.T_INTEGER, _make_integer())
    return (Token.T_IDENT, _make_identifier())

def get_tokens(length):
    tokens = []
    for _ in range(length):
        tokens.append(_random_token())
    return tokens

def get_string(length, comments=True):
    return ''.join([t[1] for t in _pad_tokens(get_tokens(length), comments)])

def t_str(token):
    if token[0] == Token.T_IDENT:
        return "identifier " + token[1]
    if token[0] == Token.T_INTEGER:
        return "integer literal " + token[1]
    return token[1]

if __name__ == '__main__':
    length = STRING_LENGTH
    if len(sys.argv) > 1:
        length = int(sys.argv[1])

    tokens = get_tokens(length)
    print(''.join([t[1] for t in _pad_tokens(tokens)]), file=sys.stdout)
    print('\n'.join([t_str(t) for t in tokens]) + '\n', file=sys.stderr)
