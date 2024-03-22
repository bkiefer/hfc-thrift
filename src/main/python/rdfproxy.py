import xsdutils
from xsdutils import isXsd, xsd2python, python2xsd, splitOwlUri
from hfcclient import connect

class RdfProxy():
  FUNCTIONAL_MASK = 4

  """HFC client connector"""
  __hfc = None

  # namespace to create new instances in
  namespace = None

  """to resolve name clashes because of namespaces in RDF, and to get the RDF
  class name from the python class name"""

  # for classes (and properties?):
  # python object <-> HFC Uris
  __rdf2py = dict()
  __py2rdf = dict()

  """All created RDF objects in the python memory"""
  __uri2pyobject = dict()

  @classmethod
  def getObject(cls, classname):
    """ A factory method to create a new object in the RDF/python world"""

    owlclassuri = cls.__py2rdf[classname]
    uri = cls.__hfc.getNewId(cls.namespace, owlclassuri)
    # create a python clazz object using "introspection"
    return cls.createProxy(classname, owlclassuri, uri)

  @classmethod
  def rdf2python(cls, value: str):
    """Turn arbitrary xsd value or uri into python object.
       If value is an uri, this may result in a recursive call
       (currently not)"""

    # todo: not sure if list is the right type, maybe Collection?
    if isinstance(value, list):
      # todo this must map onto an RdfSet
      return [cls.rdf2python(val) for val in value]
    else:
      if isXsd(value):
        return xsd2python(value)
      else:
        # check if we have the object already in the cache
        if value in cls.__uri2pyobject:
          return cls.__uri2pyobject[value]
        else:
          # get class of an instance value from HFC
          owlclassuri = cls.__hfc.getClassOf(value)
          return cls.createProxy(cls.__rdf2py[owlclassuri], owlclassuri, value)

  @staticmethod
  def python2rdf(object) -> str:
    """Return a rdf representation of this object (a string)"""
    if isinstance(object, RdfProxy):
      return object.__getattr__('uri')
    else:
      # now it better be an XSD compatible datatype
      return python2xsd(object)

  @classmethod
  def preload_classes(cls, classmapping: dict) -> None:

    for uri in classmapping:
      cls.__rdf2py[uri] = classmapping[uri]
      cls.__py2rdf[classmapping[uri]] = uri
    classes = cls.__hfc.selectQuery("select ?clz where ?clz <rdf:type> <owl:Class> ?_")
    for s in classes.table.rows:
      uri = s[0]
      ns, name = splitOwlUri(uri)
      if uri not in classmapping:
        if uri not in cls.__rdf2py:
          cls.__rdf2py[uri] = name
          cls.__py2rdf[name] = uri
        else:
          # Todo: logger.warn
          print('{} already in class dict, second URI {}, ignored'.format(name, uri))

  @classmethod
  def init_rdfproxy(cls, host='localhost', port=9090, classmapping=dict(), ns='dom:') -> None:
    cls.__hfc = connect(host, port)
    cls.namespace = ns
    cls.preload_classes(classmapping)

  @classmethod
  def shutdown_rdfproxy(cls):
    cls.__hfc.disconnect()

  @classmethod
  def preload_propertyInfo(cls, uri: str):
    cls.__propertyRange = dict()
    cls.__propertyType = dict()
    cls.__propertyBaseToFull = dict()
    # todo: do it
    propInfos = cls.__hfc.getAllProps(uri)
    for prop in propInfos:
      ns, name = xsdutils.splitOwlUri(prop)
      cls.__propertyBaseToFull[name] = prop
      cls.__propertyType[name] = propInfos[prop].type
      range = propInfos[prop].ranges
      if len(range) == 1:
        range = range[0]
      cls.__propertyRange[name] = range

  @classmethod
  def getClass(cls, classname, uri):
    if classname not in globals():
      # create a subclass of RdfProxy with name 'classname'
      clazz = classfactory(classname)
      clazz.__clazzuri = uri
      clazz.preload_propertyInfo(uri)
    else:
      clazz = globals()[classname]
    if issubclass(clazz, RdfProxy):
      return clazz
    return None


  @classmethod
  def createProxy(cls, classname, class_uri, instance_uri):
    """
    create an object of the corresponding python class (a subclass of RdfProxy)
    classname: the name of the python class
    class_uri: the uri of the RDF class the instance belongs to
    instance_uri: the uri of the instance
    """
    clazz = RdfProxy.getClass(classname, class_uri)
    if clazz == None:
        return None
    pyobj = clazz(instance_uri)
    RdfProxy.__uri2pyobject[instance_uri] = pyobj
    return pyobj


  def __init__(self, uri=None):
    super(__class__, self).__setattr__("uri", uri)   # my uri


  def isFunctional(self, prop_uri):
    return self.__class__.__propertyType[prop_uri] & RdfProxy.FUNCTIONAL_MASK


  def __getattr__(self, slot):
    # should we have an abstract method checking slot validity?
    rdfprop = RdfProxy.__py2rdf[slot]
    if self.isFunctional(rdfprop):
      value = RdfProxy.__hfc.getValue(self.uri, rdfprop)
    else:
      value = RdfProxy.__hfc.getMultiValue(self.uri, rdfprop)
    return RdfProxy.rdf2python(value)


  def __setattr__(self, slot, value):
    # should we have an abstract method checking slot/value validity?

    rdfvalue = RdfProxy.python2rdf(value)
    if not rdfvalue:
        # Todo: logger.warn
        print('{} can not be converted to an RDF value'.format(value))
    rdfslot = self.__propertyBaseToFull[slot]
    if self.isFunctional(slot):
      RdfProxy.__hfc.setValue(self.uri, rdfslot, rdfvalue)
    else:
      RdfProxy.__hfc.setMultiValue(self.uri, rdfslot, rdfvalue)


def classfactory(classname):
  newclass = type(classname, (RdfProxy,), {

    # constructor
    "__init__": RdfProxy.__init__,
  })
  return newclass
