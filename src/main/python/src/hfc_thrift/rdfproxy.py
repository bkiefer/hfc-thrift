import logging
from typing import ClassVar, Iterable, Union, Type, cast, Any

from thrift.transport.TTransport import TTransportException

from hfc_thrift.hfcclient import connect, HfcClient
from hfc_thrift.xsdutils import isXsd, xsd2python, python2xsd, splitOwlUri

# configure logger
logging.basicConfig(
    format="%(asctime)s: %(levelname)s: %(message)s",
    level=logging.INFO,
    force=True)
logger = logging.getLogger(__file__)
logger.setLevel(logging.INFO)

"""HFC client connector"""
hfc: HfcClient = None  # type: ignore


class RdfProxy:
    FUNCTIONAL_MASK: ClassVar[int] = 4
    OBJECTS_ARE_NAMED_INDIVIDUALS: ClassVar[bool] = True
    UNDEFINED_SLOTS_ARE_ERRORS = False

    UNBOUND = '__UNBOUND__'

    # namespace to create new instances in
    namespace: ClassVar[str]

    """to resolve name clashes because of namespaces in RDF, and to get the RDF
    class name from the python class name"""

    # for classes (and properties?):
    # python object <-> HFC Uris
    __rdf2py: ClassVar[dict[str, str]] = dict()
    __py2rdf: ClassVar[dict[str, str]] = dict()

    """All created RDF classes and objects in the python memory"""
    __uri2pyclass: ClassVar[dict[str, type['RdfProxy']]] = dict()
    __uri2pyobject: ClassVar[dict[str, 'RdfProxy']] = dict()

    @classmethod
    def newObject(cls, self, instance_uri=None):
        clazz = self.__class__
        if instance_uri == None:
            instance_uri = hfc.getNewId(cls.namespace, clazz.__clazzuri)
        super(RdfProxy, self).__setattr__('uri', instance_uri)
        RdfProxy.__uri2pyobject[instance_uri] = self

    @classmethod
    def getObject(cls, classname: str) -> 'RdfProxy':
        """ A factory method to create a new object in the RDF/python world
            from the class name"""
        class_uri = cls.__py2rdf[classname]
        if cls.OBJECTS_ARE_NAMED_INDIVIDUALS:
            pass
        # create a python clazz object using "introspection"
        return cls.getClass(class_uri)()

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
                return RdfProxy.createProxy(class_uri, uri)

    def get_rdf_as_pyobj(self, prop_uri: str) -> Union[int, str, float, 'RdfProxy']:
        """Turn arbitrary xsd value or uri into python object.
           If value is an uri, this may result in a recursive call
           (currently not)"""
        value = hfc.getValue(self.uri, prop_uri)
        return RdfProxy.rdf2pyobj(value)

    def get_rdf_as_rdfset(self, prop_uri: str) -> 'RdfSet':
        """Turn arbitrary xsd value or uri into python object.
           If value is an uri, this may result in a recursive call
           (currently not)"""
        value = hfc.getMultiValue(self.uri, prop_uri)
        # this must be represented as rdfset
        result = RdfSet(self, prop_uri, [RdfProxy.rdf2pyobj(val) for val in value])
        return result

    @staticmethod
    def python2rdf(object: Any) -> str | list[Any]:
        """Return a rdf representation of this object (a string)"""
        if isinstance(object, RdfProxy):
            return object.__getattribute__('uri')
        elif isinstance(object, list) or isinstance(object, set):
            it = iter(object)
            return [RdfProxy.python2rdf(val) for val in it]
        else:
            # now it better be an XSD compatible datatype
            return python2xsd(object)

    @staticmethod
    def subclass_of(sup, sub):
        return hfc.isSubclassOf(sup, sub)

    @classmethod
    def superclass_of(cls, sub):
        try:
            supuri = cls.__clazzuri
            suburi = sub.__clazzuri
            return cls.subclass_of(supuri, suburi)
        except:
            return None

    @classmethod
    def preload_classes(cls, classmapping: dict) -> None:
        for class_uri, clazz in classmapping.items():
            cls.__rdf2py[class_uri] = clazz
            cls.__py2rdf[clazz] = class_uri
        classes = hfc.selectQuery("select ?clz where ?clz <rdf:type> <owl:Class> ?_")
        for s in classes.table.rows:  # type: ignore
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
        # if hfc is set, no initialisation is needed
        if not hfc:
            hfc = connect(host, port)
            cls.namespace = ns
            cls.preload_classes(classmapping)
            cls.__uri2pyobject['<owl:Nothing>'] = RdfProxy.UNBOUND

    @classmethod
    def shutdown_rdfproxy(cls) -> None:
        global hfc
        hfc.disconnect()

    @classmethod
    def shutdown_server(cls) -> None:
        global hfc
        try:
            hfc.shutdown()
        except TTransportException:
            # ignore, the transport gets closed during shutdown
            pass


    @classmethod
    def preload_propertyInfo(cls, class_uri: str) -> None:
        cls.__propertyRange = dict()
        cls.__propertyType = dict()
        cls.__propertyBaseToFull = dict()
        # todo: do it
        propInfos = hfc.getAllProps(class_uri)
        for prop_uri, info in propInfos.items():
            _, name = splitOwlUri(prop_uri)
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
        """Get the python class for a specific class id"""
        if class_uri in cls.__uri2pyclass:
            return cls.__uri2pyclass[class_uri]
        else:
            # create a subclass of RdfProxy with name 'classname'
            classname = cls.__rdf2py[class_uri]
            clazz = classfactory(classname)
            clazz.__clazzuri = class_uri  # type: ignore
            clazz.preload_propertyInfo(class_uri)
            cls.__uri2pyclass[class_uri] = clazz
            assert issubclass(clazz, RdfProxy)
            return clazz

    @classmethod
    def getProxy(cls, instance_uri):
        """
        Get a python proxy object for an existing instance URI, None if the
        object is not in the database
        """
        try:
            clazz_uri = hfc.getClassOf(instance_uri)
            return cls.createProxy(clazz_uri, instance_uri)
        except:
            return None

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
        return clazz(instance_uri)

    @classmethod
    def askQuery(cls, query: str) -> bool:
        return hfc.askQuery(query)

    @classmethod
    def selectQuery(cls, query: str) -> list[Any]:
        query_result = hfc.selectQuery(query)
        table = []
        if not query_result.table or not query_result.table.rows or len(query_result.table.rows[0]) == 0:
            return []
        for table_row in query_result.table.rows:
            if len(table_row) > 1:
                row = []
                for elt in table_row:
                    row.append(cls.rdf2pyobj(elt))
            else:
                row = cls.rdf2pyobj(table_row[0])
            table.append(row)
        return table

    @classmethod
    def query(cls, query: str) -> list[Any]:
        query_result = hfc.selectQuery(query)
        table = []
        if not query_result.table or not query_result.table.rows or len(query_result.table.rows[0]) == 0:
            return []
        for table_row in query_result.table.rows:
            if len(table_row) > 1:
                row = []
                for elt in table_row:
                    row.append(elt)
            else:
                row = table_row[0]
            table.append(row)
        return table


    def isFunctional(self, prop_uri):
        return self.__class__.__propertyType[prop_uri] & RdfProxy.FUNCTIONAL_MASK

    def unbound(self, slot):
        """return true if the slot has no value in the DB"""

    def __slot_defined(self, slot):
        if slot not in self._RdfProxy__propertyBaseToFull:
            if RdfProxy.UNDEFINED_SLOTS_ARE_ERRORS:
                raise KeyError(f'property {slot} not defined for OWL class {self.__py2rdf[self.__class__.__name__]}')
            else:
                return False;
        return True

    def __getattr__(self, slot):
        # should we have an abstract method checking slot validity?
        if not self.__slot_defined(slot):
            return RdfProxy.UNBOUND
        rdfprop = self.__propertyBaseToFull[slot]
        if self.isFunctional(rdfprop):
            return self.get_rdf_as_pyobj(rdfprop)
        return self.get_rdf_as_rdfset(rdfprop)

    def __setattr__(self, slot, value):
        # should we have an abstract method checking slot/value validity?
        if self.__slot_defined(slot):
            rdfvalue = RdfProxy.python2rdf(value)
            rdfslot = self.__propertyBaseToFull[slot]
        else:
            rdfvalue = RdfProxy.python2rdf(value)
            rdfslot = "<" + RdfProxy.namespace + slot +">"
            self.__propertyBaseToFull[slot] = rdfslot
            self.__propertyType[rdfslot] = 1
        if self.isFunctional(rdfslot):
            hfc.setValue(self.uri, rdfslot, rdfvalue)
        else:
            hfc.setMultiValue(self.uri, rdfslot, rdfvalue)


class RdfSet():

    def __init__(self, subj: RdfProxy, pred_uri: str, pyobjlist: Iterable) -> None:
        self.__storage: set = set(pyobjlist)
        self.__subject: RdfProxy = subj
        self.__pred_uri: str = pred_uri

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

    def union(self, other: Iterable):
        for i in other:
            self.add(i)

    def intersection(self, other: Iterable):
        for i in other:
            self.remove(i)

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
        "__init__": lambda self, uri=None: self.newObject(self, uri)
    })
    return cast(Type[RdfProxy], newclass)
