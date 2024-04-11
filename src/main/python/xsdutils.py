"""
This is a singleton, it manages the data structures for HFC<-->Python mapping
and connection to HFC, and contains some globally valid helper functions
"""


# xsd -> atomic value
def isXsd(s: str) -> bool:
    # TODO: check correct prefixes
    return s.startswith('<xsd:') or s.startswith('<http://www.w3.org/2001/XMLSchema#')


def extractTypeAndValue(xsdstring: str) -> tuple[str, str] | None:
    index = xsdstring.rfind('^')
    if index == -1:
        return None
    value = xsdstring[1:index - 2]
    xsdstring = xsdstring[index + 1:]
    splits = splitOwlUri(xsdstring)
    if splits:
        return splits[1], value
    return None


# atomic value --> xsd string
def xsd2python(xsdstring: str) -> int | str | float | None:
    # TODO: check which python type corresponds to the value and convert
    pair = extractTypeAndValue(xsdstring)
    if not pair:
        return None
    name, value = pair
    if name == "int":
        return int(value)
    elif name == "string":
        return value
    elif name == "double":
        return float(value)
    return None


xsdclassdict = {
    int: "int",
    str: "string",
    float: "double",
    # datetime: "date"
}


def python2xsd(py_val) -> str | None:
    if type(py_val) in xsdclassdict:
        return f'"{str(py_val)}"^^<xsd:{xsdclassdict[type(py_val)]}>'
    return None


def splitOwlUri(uri: str) -> tuple[str, str] | None:
    pos = uri.rfind('#')
    if pos == -1:
        pos = uri.rfind(':')
    if pos == -1:
        return None
    return uri[1:pos], uri[pos + 1:-1]
