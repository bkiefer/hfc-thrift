import rdfbase

class RdfProxy():
  FUNCTIONAL_MASK = 4

  __uri2pyobject = dict()

  @classmethod
  def preload_propertyInfo(cls):
      cls.__propertyRange = dict()
      cls.__propertyType = dict()
      cls.__propertyBaseToFull = dict()
      # clazz.preload_propertyInfo(); todo: do it

  @classmethod
  def getClass(cls, classname, uri):
    if classname not in globals():
      # create a subclass of RdfProxy with name 'classname'
      clazz = classfactory(classname)
      clazz.__clazzuri = uri
      clazz.preload_propertyInfo()
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


  @classmethod
  def new_instance(cls, instance_uri):
    return cls(instance_uri)
    cls.uri2pyobject[instance_uri] = pyobj


  def __init__(self, uri=None):
    super(__class__, self).__setattr__("uri", uri)   # my uri

  def getObject(classname):
    """ A factory method to create a new object in the RDF/python world"""

    owlclassuri = rdfbase.py2rdf[classname]
    uri = rdfbase.hfc.getNewId(rdfbase.namespace, owlclassuri)
    # create a python clazz object using "introspection"
    return RdfProxy.createProxy(classname, owlclassuri, uri)

  def rdf2python(value):
    """Turn arbitrary xsd value or uri into python object.
       If value is an uri, this may result in a recursive call
       (currently not)"""

    # todo: not sure if list is the right type, maybe Collection?
    if isinstance(value, list):
      # todo this must map onto an RdfSet
      return [rdfbase.rdf2python(val) for val in value]
    else:
      if rdfbase.isXsd(value):
        return rdfbase.xsd2python(value)
      else:
        # check if we have the object already in the cache
        if value in RdfProxy.__uri2pyobject:
          return RdfProxy.__uri2pyobject[value]
        else:
          # get class of an instance value from HFC
          owlclassuri = rdfbase.hfc.getClassOf(value)
          return RdfProxy.createProxy(rdfbase.rdf2py[owlclassuri], owlclassuri, value)

  def python2rdf(object):
    if isinstance(object, RdfProxy):
      return object.__getattr__('uri')
    else:
      # now it better be an XSD compatible datatype
      return rdfbase.python2xsd(object)


  def isFunctional(self, prop_uri):
    return self.__class__.__propertyType[prop_uri] & RdfProxy.FUNCTIONAL_MASK


  def __getattr__(self, slot):
    # should we have an abstract method checking slot validity?
    rdfprop = RdfProxy.py2rdf[slot]
    if self.isFunctional(rdfprop):
      value = rdfbase.hfc.getValue(self.uri, rdfprop)
    else:
      value = rdfbase.hfc.getMultiValue(self.uri, rdfprop)
    return rdfbase.rdf2python(value)


  def __setattr__(self, slot, value):
    # should we have an abstract method checking slot/value validity?
    rdfvalue = rdfbase.python2rdf(value)
    if not rdfvalue:
        # Todo: logger.warn
        print('{} can not be converted to an RDF value'.format(value))
    rdfslot = rdfbase.py2rdf[slot]
    if self.isFunctional(rdfslot):
      rdfbase.hfc.setValue(self.uri, rdfslot, rdfvalue)
    else:
      rdfbase.hfc.setMultiValue(self.uri, rdfslot, rdfvalue)


def classfactory(classname):
  newclass = type(classname, (RdfProxy,), {

    # constructor
    "__init__": RdfProxy.__init__,
  })
  return newclass
