"""
This is a singleton, it manages the data structures for HFC<-->Python mapping
and connection to HFC, and contains some globally valid helper functions
"""


# xsd -> atomic value
def isXsd(s: str) -> bool:
    index = s.rfind('^^')
    if index == -1:
        return False
    s = s[index + 2:]
    return s.startswith('<xsd:') or s.startswith('<http://www.w3.org/2001/XMLSchema#')


def splitOwlUri(uri: str) -> tuple[str, str]:
    pos = uri.rfind('#')
    if pos == -1:
        pos = uri.rfind(':')
    if pos == -1:
        raise ValueError(f'uri {uri} is not a valid owl uri')
    return uri[1:pos], uri[pos + 1:-1]


def extractTypeAndValue(xsdstring: str) -> tuple[str, str]:
    index = xsdstring.rfind('^')
    if index == -1:
        raise ValueError(f'{xsdstring} is not a valid xsd string')
    value = xsdstring[1:index - 2]
    xsdstring = xsdstring[index + 1:]
    splits = splitOwlUri(xsdstring)
    return splits[1], value


# atomic value --> xsd string
def xsd2python(xsdstring: str) -> int | str | float:
    # TODO: check which python type corresponds to the value and convert
    name, value = extractTypeAndValue(xsdstring)
    if name == "int" or name == "long":
        return int(value)
    elif name == "string":
        return value
    elif name == "double":
        return float(value)
    elif name == "boolean":
        return bool(value)
    raise ValueError(f'unsupported type {name}')


xsdclassdict = {
    int: "int",
    str: "string",
    float: "double",
    bool: "boolean"
    # datetime: "date"
}

INT_MAX_VALUE = 2147483647
INT_MIN_VALUE = -2147483648

def python2xsd(py_val) -> str:
    if type(py_val) in xsdclassdict:
        xsdtype = xsdclassdict[type(py_val)]
        if xsdtype == "int" and \
            (py_val > INT_MAX_VALUE or py_val < INT_MIN_VALUE):
            xsdtype = "long"
        return f'"{str(py_val)}"^^<xsd:{xsdtype}>'
    raise ValueError(f'unsupported type {type(py_val)} of {py_val}')
