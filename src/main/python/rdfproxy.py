import logging
from typing import Optional, Union, Type, cast, Any

import xsdutils
from hfcclient import connect, HfcClient
from xsdutils import isXsd, xsd2python, python2xsd, splitOwlUri

# configure logger
logging.basicConfig(
    format="%(asctime)s: %(levelname)s: %(message)s",
    level=logging.INFO,
    force=True)
logger = logging.getLogger(__file__)
logger.setLevel(logging.INFO)

"""HFC client connector"""
hfc: HfcClient


class RdfProxy:
    FUNCTIONAL_MASK: int = 4

    # namespace to create new instances in
    namespace: str

    """to resolve name clashes because of namespaces in RDF, and to get the RDF
    class name from the python class name"""

    # for classes (and properties?):
    # python object <-> HFC Uris
    __rdf2py: dict[str, str] = dict()
    __py2rdf: dict[str, str] = dict()

    """All created RDF classes and objects in the python memory"""
    __uri2pyclass: dict[str, type['RdfProxy']] = dict()
    __uri2pyobject: dict[str, 'RdfProxy'] = dict()

    @classmethod
    def getObject(cls, classname: str) -> 'RdfProxy':
        """ A factory method to create a new object in the RDF/python world"""
        class_uri = cls.__py2rdf[classname]
        instance_uri = hfc.getNewId(cls.namespace, class_uri)
        # create a python clazz object using "introspection"
        return cls.createProxy(class_uri, instance_uri)

    @classmethod
    def rdf2pyobj(cls, uri: str) -> Union[int, str, float, 'RdfProxy']:
        if isXsd(uri):
            return xsd2python(uri)
        else:
            # check if we have the object already in the cache
            if uri in RdfProxy.__uri2pyobject:
                return RdfProxy.__uri2pyobject[uri]
            else:
                # get class of an instance value from HFC
                class_uri = hfc.getClassOf(uri)
                proxy = RdfProxy.createProxy(RdfProxy.__rdf2py[class_uri], class_uri)
                RdfProxy.__uri2pyobject[uri] = proxy
                return proxy

    def get_rdf_as_pyobj(self, prop_uri: str):
        """Turn arbitrary xsd value or uri into python object.
           If value is an uri, this may result in a recursive call
           (currently not)"""
        value = hfc.getValue(self.uri, prop_uri)
        return RdfProxy.rdf2pyobj(value)

    def get_rdf_as_rdfset(self, prop_uri: str):
        """Turn arbitrary xsd value or uri into python object.
           If value is an uri, this may result in a recursive call
           (currently not)"""
        value = hfc.getMultiValue(self.uri, prop_uri)
        # this must be represented as rdfset
        result = RdfSet(self, prop_uri, [RdfProxy.rdf2pyobj(val) for val in value])
        return result

    @staticmethod
    def python2rdf(object) -> str | list[Any]:
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
        for class_uri, clazz in classmapping.items():
            cls.__rdf2py[class_uri] = clazz
            cls.__py2rdf[clazz] = class_uri
        classes = hfc.selectQuery("select ?clz where ?clz <rdf:type> <owl:Class> ?_")
        for s in classes.table.rows:
            class_uri = s[0]
            # has a name for the URI already been pre-set?
            if class_uri in cls.__rdf2py:
                continue
            _, classname = splitOwlUri(class_uri)
            if classname not in cls.__py2rdf:
                cls.__rdf2py[class_uri] = classname
                cls.__py2rdf[classname] = class_uri
            else:
                logger.warning(f'{cls.__py2rdf[classname]} already in class dict, {class_uri} ignored')

    @classmethod
    def init_rdfproxy(cls, host: str = 'localhost', port: int = 9090, classmapping: dict[str, str] = dict(),
                      ns: str = 'dom:') -> None:
        global hfc

        hfc = connect(host, port)
        cls.namespace = ns
        cls.preload_classes(classmapping)

    @classmethod
    def shutdown_rdfproxy(cls) -> None:
        hfc.disconnect()

    @classmethod
    def preload_propertyInfo(cls, class_uri: str) -> None:
        cls.__propertyRange = dict()
        cls.__propertyType = dict()
        cls.__propertyBaseToFull = dict()
        # todo: do it
        propInfos = hfc.getAllProps(class_uri)
        for prop_uri, info in propInfos.items():
            _, name = xsdutils.splitOwlUri(prop_uri)
            if name not in cls.__propertyBaseToFull:
                cls.__propertyBaseToFull[name] = prop_uri
                cls.__propertyType[prop_uri] = info.type
                range = info.ranges
                if len(range) == 1:
                    range = range[0]
                cls.__propertyRange[name] = range
            else:
                logger.warning(f'{cls.__propertyBaseToFull[name]} already in mapping, {prop_uri} ignored')

    @classmethod
    def getClass(cls, class_uri: str) -> type['RdfProxy']:
        if class_uri in cls.__uri2pyclass:
            return cls.__uri2pyclass[class_uri]
        else:
            # create a subclass of RdfProxy with name 'classname'
            classname = cls.__rdf2py[class_uri]
            clazz = classfactory(classname)
            clazz.__clazzuri = class_uri
            clazz.preload_propertyInfo(class_uri)
            cls.__uri2pyclass[class_uri] = clazz
            assert issubclass(clazz, RdfProxy)
            return clazz

    @classmethod
    def createProxy(cls, class_uri: str, instance_uri: str) -> 'RdfProxy':
        """
        create an object of the corresponding python class (a subclass of RdfProxy)
        class_uri: the uri of the RDF class the instance belongs to
        instance_uri: the uri of the instance
        """
        if instance_uri in cls.__uri2pyobject:
            return cls.__uri2pyobject[instance_uri]
        clazz = RdfProxy.getClass(class_uri)
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
        rdfvalue = RdfProxy.python2rdf(value)
        if not rdfvalue:
            logger.warning(f'{value} can not be converted to an RDF value')
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
        rdfobj = RdfProxy.python2rdf(pyobj)
        hfc.addToMultiValue(self.subjuri(), self.__pred_uri, rdfobj)

    def subjuri(self):
        return self.__subject.__getattribute__('uri')

    def add(self, pyobj):
        self.__storage.add(pyobj)
        self.__add_in_hfc(pyobj)

    def __remove_in_hfc(self, pyobj):
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


def classfactory(classname: str) -> type['RdfProxy']:
    newclass = type(classname, (RdfProxy,), {

        # constructor
        "__init__": RdfProxy.__init__,
    })
    return cast(Type[RdfProxy], newclass)
