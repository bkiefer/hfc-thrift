from builtins import classmethod

from hfcclient import HfcClient

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
  value = xsdstring[1:index-2]
  xsdstring = xsdstring[index+1:]
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
  #datetime: "date"
}

def python2xsd(py_val):
  xsd = xsdclassdict[type(py_val)]
  if xsd:
    return '"' + str(py_val) + '"^^<xsd:' + xsd +'>'
  return None

def splitOwlUri(uri: str) -> (str, str):
  pos = uri.rfind('#')
  if pos == -1:
    pos = uri.rfind(':')
  return uri[1:pos], uri[pos+1:-1]

"""HFC client connector"""
hfc = None

"""to resolve name clashes because of namespaces in RDF, and to get the RDF
class name from the python class name"""

# for classes (and properties?):
# python object <-> HFC Uris
rdf2py = dict()
py2rdf = dict()

# for objects: HFC Uris -> Python object

# namespace to create new instances in
namespace = None

def preload_classes(classmapping: dict) -> None:
  global rdf2py, py2rdf, hfc
  for uri in classmapping:
    rdf2py[uri] = classmapping[uri]
    py2rdf[classmapping[uri]] = uri
  classes = hfc.selectQuery("select ?clz where ?clz <rdf:type> <owl:Class> ?_")
  for s in classes.table.rows:
    uri = s[0]
    ns, name = splitOwlUri(uri)
    if uri not in classmapping:
      if uri not in rdf2py:
        rdf2py[uri] = name
        py2rdf[name] = uri
      else:
        # Todo: logger.warn
        print('{} already in class dict, second URI {}, ignored'.format(name, uri))

def connect(host='localhost', port=9090, classmapping=dict(), ns='dom:') -> None:
  """
  classmapping is a map from class uri to (simple) name
  ns is the namespace where new instances are created
  """
  global hfc, namespace

  hfc = HfcClient(host, port)
  hfc.connect()
  namespace = ns
  preload_classes(classmapping)

def disconnect() -> None:
  global hfc
  hfc.disconnect()


