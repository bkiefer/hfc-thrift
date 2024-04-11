"""
This is a singleton, it manages the data structures for HFC<-->Python mapping
and connection to HFC, and contains some globally valid helper functions
"""


# xsd -> atomic value
def isXsd(s: str) -> bool:
    # TODO: check correct prefixes
    return s.startswith('<xsd:') or s.startswith('<http://www.w3.org/2001/XMLSchema#')


def extractTypeAndValue(xsdstring: str):
    index = xsdstring.rfind('^')
    if not index:
        return None
    value = xsdstring[1:index - 2]
    xsdstring = xsdstring[index + 1:]
    ns, name = splitOwlUri(xsdstring)
    return name, value


# atomic value --> xsd string
def xsd2python(xsdstring: str):
    # TODO: check which python type corresponds to the value and convert
    name, value = extractTypeAndValue(xsdstring)
    if name == "int":
        return int(value)
    elif name == "string":
        return value
    elif name == "double":
        return float(value)


xsdclassdict = {
    int: "int",
    str: "string",
    float: "double",
    # datetime: "date"
}


def python2xsd(py_val):
    xsd = xsdclassdict[type(py_val)]
    if xsd:
        return '"' + str(py_val) + '"^^<xsd:' + xsd + '>'
    return None


def splitOwlUri(uri: str) -> (str, str):
    pos = uri.rfind('#')
    if pos == -1:
        pos = uri.rfind(':')
    return uri[1:pos], uri[pos + 1:-1]
