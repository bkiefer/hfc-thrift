import xsdutils
from hfcclient import connect
from xsdutils import isXsd, xsd2python, python2xsd, splitOwlUri

"""HFC client connector"""
hfc = None


class RdfProxy():
    FUNCTIONAL_MASK = 4

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
        global hfc

        owlclassuri = cls.__py2rdf[classname]
        uri = hfc.getNewId(cls.namespace, owlclassuri)
        # create a python clazz object using "introspection"
        return cls.createProxy(classname, owlclassuri, uri)

    @classmethod
    def rdf2pyobj(cls, uri: str):
        global hfc

        if isXsd(uri):
            return xsd2python(uri)
        else:
            # check if we have the object already in the cache
            if uri in RdfProxy.__uri2pyobject:
                return RdfProxy.__uri2pyobject[uri]
            else:
                # get class of an instance value from HFC
                owlclassuri = hfc.getClassOf(uri)
                proxy = RdfProxy.createProxy(RdfProxy.__rdf2py[owlclassuri], owlclassuri, uri)
                RdfProxy.__uri2pyobject[uri] = proxy
                return proxy

    def get_rdf_as_pyobj(self, prop_uri: str):
        """Turn arbitrary xsd value or uri into python object.
           If value is an uri, this may result in a recursive call
           (currently not)"""
        global hfc

        value = hfc.getValue(self.uri, prop_uri)
        return RdfProxy.rdf2pyobj(value)

    def get_rdf_as_rdfset(self, prop_uri: str):
        global hfc

        """Turn arbitrary xsd value or uri into python object.
           If value is an uri, this may result in a recursive call
           (currently not)"""
        value = hfc.getMultiValue(self.uri, prop_uri)
        # this must be represented as rdfset
        result = RdfSet(self, prop_uri, [RdfProxy.rdf2pyobj(val) for val in value])
        return result

    @staticmethod
    def python2rdf(object) -> str:
        """Return a rdf representation of this object (a string)"""
        if isinstance(object, RdfProxy):
            return object.__getattribute__('uri')
        else:
            try:
                it = iter(object)
                return [RdfProxy.python2rdf(val) for val in it]
            except TypeError as te:
                pass
            # now it better be an XSD compatible datatype
            return python2xsd(object)

    @classmethod
    def preload_classes(cls, classmapping: dict) -> None:
        global hfc

        for uri in classmapping:
            cls.__rdf2py[uri] = classmapping[uri]
            cls.__py2rdf[classmapping[uri]] = uri
        classes = hfc.selectQuery("select ?clz where ?clz <rdf:type> <owl:Class> ?_")
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
        global hfc

        hfc = connect(host, port)
        cls.namespace = ns
        cls.preload_classes(classmapping)

    @classmethod
    def shutdown_rdfproxy(cls):
        global hfc

        hfc.disconnect()

    @classmethod
    def preload_propertyInfo(cls, uri: str):
        global hfc

        cls.__propertyRange = dict()
        cls.__propertyType = dict()
        cls.__propertyBaseToFull = dict()
        # todo: do it
        propInfos = hfc.getAllProps(uri)
        for prop in propInfos:
            ns, name = xsdutils.splitOwlUri(prop)
            cls.__propertyBaseToFull[name] = prop
            cls.__propertyType[prop] = propInfos[prop].type
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
        if instance_uri in cls.__rdf2py:
            return cls.__rdf2py[instance_uri]
        clazz = RdfProxy.getClass(classname, class_uri)
        if clazz == None:
            return None
        pyobj = clazz(instance_uri)
        RdfProxy.__uri2pyobject[instance_uri] = pyobj
        return pyobj

    def __init__(self, uri=None):
        super(__class__, self).__setattr__("uri", uri)  # my uri

    def isFunctional(self, prop_uri):
        return self.__class__.__propertyType[prop_uri] & RdfProxy.FUNCTIONAL_MASK

    def __getattr__(self, slot):
        # should we have an abstract method checking slot validity?
        rdfprop = self._RdfProxy__propertyBaseToFull[slot]
        if self.isFunctional(rdfprop):
            return self.get_rdf_as_pyobj(rdfprop)
        return self.get_rdf_as_rdfset(rdfprop)

    def __setattr__(self, slot, value):
        # should we have an abstract method checking slot/value validity?
        global hfc

        rdfvalue = RdfProxy.python2rdf(value)
        if not rdfvalue:
            # Todo: logger.warn
            print('{} can not be converted to an RDF value'.format(value))
        rdfslot = self.__propertyBaseToFull[slot]
        if self.isFunctional(rdfslot):
            hfc.setValue(self.uri, rdfslot, rdfvalue)
        else:
            hfc.setMultiValue(self.uri, rdfslot, rdfvalue)


class RdfSet():

    def __init__(self, subj, pred_uri, pyobjlist):
        self.__storage = set(pyobjlist)
        self.__subject = subj
        self.__pred_uri = pred_uri

    def __add_in_hfc(self, pyobj):
        global hfc

        rdfobj = RdfProxy.python2rdf(pyobj)
        hfc.addToMultiValue(self.subjuri(), self.__pred_uri, rdfobj)

    def subjuri(self):
        return self.__subject.__getattribute__('uri')

    def add(self, pyobj):
        self.__storage.add(pyobj)
        self.__add_in_hfc(pyobj)

    def __remove_in_hfc(self, pyobj):
        global hfc

        rdfobj = RdfProxy.python2rdf(pyobj)
        hfc.removeFromMultiValue(self.subjuri(), self.__pred_uri, rdfobj)

    def remove(self, pyobj):
        self.__storage.remove(pyobj)
        self.__remove_in_hfc(pyobj)

    def discard(self, pyobj):
        if pyobj in self.__storage:
            self.remove(pyobj)

    def __len__(self):
        return len(self.__storage)

    def __next__(self):
        return next(self.__impl)

    def __iter__(self):
        self.__impl = iter(self.__storage)
        return self


def classfactory(classname):
    newclass = type(classname, (RdfProxy,), {

        # constructor
        "__init__": RdfProxy.__init__,
    })
    return newclass
