import os
import subprocess
import sys
import unittest
from typing import cast

from hfc_thrift import xsdutils, rdfproxy
from hfc_thrift.rdfproxy import RdfProxy, RdfSet, classfactory, logger


class UtilsTestCase(unittest.TestCase):
    def test_isXsd(self):
        self.assertTrue(xsdutils.isXsd('"hi"^^<http://www.w3.org/2001/XMLSchema#string>'))
        self.assertTrue(xsdutils.isXsd('"hi"^^<xsd:string>'))
        self.assertFalse(xsdutils.isXsd('no-xsd'))

    def test_splitOwlUri(self):
        self.assertEqual(xsdutils.splitOwlUri('<http://www.w3.org/2001/XMLSchema#string>'),
                         ('http://www.w3.org/2001/XMLSchema', 'string'))
        self.assertEqual(xsdutils.splitOwlUri('<xsd:int>'), ('xsd', 'int'))
        self.assertRaises(ValueError, xsdutils.splitOwlUri, 'no-uri')

    def test_extractTypeAndValue(self):
        self.assertEqual(xsdutils.extractTypeAndValue('"1"^^<xsd:int>'), ('int', '1'))
        self.assertEqual(xsdutils.extractTypeAndValue('"1.0"^^<xsd:double>'), ('double', '1.0'))
        self.assertEqual(xsdutils.extractTypeAndValue('"ttt"^^<xsd:string>'), ('string', 'ttt'))
        with self.assertRaisesRegex(ValueError, 'no-xsd is not a valid xsd string'):
            xsdutils.extractTypeAndValue('no-xsd')
        with self.assertRaisesRegex(ValueError, 'uri no-xsd is not a valid owl uri'):
            xsdutils.extractTypeAndValue('ttt^^no-xsd')

    def test_xsd2py(self):
        self.assertEqual(xsdutils.xsd2python('"1"^^<xsd:int>'), 1)
        self.assertEqual(xsdutils.xsd2python('"1.0"^^<xsd:double>'), 1.0)
        self.assertEqual(xsdutils.xsd2python('"ttt"^^<xsd:string>'), 'ttt')
        with self.assertRaisesRegex(ValueError, 'no-xsd is not a valid xsd string'):
            xsdutils.xsd2python('no-xsd')
        with self.assertRaisesRegex(ValueError, 'unsupported type notype'):
            xsdutils.xsd2python('"ttt"^^<xsd:notype>')

    def test_py2xsd(self):
        self.assertEqual(xsdutils.python2xsd(1), '"1"^^<xsd:int>')
        self.assertEqual(xsdutils.python2xsd(1.0), '"1.0"^^<xsd:double>')
        self.assertEqual(xsdutils.python2xsd('ttt'), '"ttt"^^<xsd:string>')
        with self.assertRaisesRegex(ValueError, "unsupported type <class 'NoneType'> of None"):
            xsdutils.python2xsd(None)


class RdfProxyTestCase(unittest.TestCase):
    proc: subprocess.Popen

    @classmethod
    def setUpClass(cls) -> None:
        if sys.platform.startswith('linux'):
            # Linux specific procedures

            os.chdir("../../..")
            # start hfc server process
            cls.proc = subprocess.Popen(["/usr/bin/java",
                                         "-Dlogback.configurationFile=./logback.xml",
                                         "-jar", "target/hfc-server.jar",
                                         "-p", "7777", "src/test/data/test.yml"],
                                        encoding="UTF-8",
                                        stdout=subprocess.PIPE)
            for line in (cls.proc.stdout or []):
                if "Starting" in line:
                    logger.info("HFC Server started successfully")
                    break
        else:
            logger.warning('Make sure HFC server is running on port 7777 using test.yml configuration')

        # don't use default port: PAL hfc service uses it.
        RdfProxy.init_rdfproxy('localhost', 7777,
                               {'<tml:Event>': 'TmlEvent',
                                '<upper:Entity>': 'UpperEntity',
                                '<dial:Correction>': 'DialCorrection',
                                '<tml:Timeline>': 'TmlTimeline',
                                '<dom:Food>': 'DomFood',
                                '<tml:Doctor>': 'Doctor'  # trigger warning
                                })

    @classmethod
    def tearDownClass(cls) -> None:
        RdfProxy.shutdown_rdfproxy()

        if sys.platform.startswith('linux'):
            # stop hfc server process
            cls.proc.terminate()

    def setUp(self):
        if not sys.platform.startswith('linux'):
            rdfproxy.hfc.init("src/test/data/test.yml")

    def test_init_rdfproxy(self):
        # init_rdfproxy is called in test class setup, verify its effects here
        self.assertEqual(len(RdfProxy._RdfProxy__rdf2py), 223)  # type: ignore
        self.assertEqual(len(RdfProxy._RdfProxy__py2rdf), 223)  # type: ignore
        self.assertEqual(RdfProxy.namespace, 'dom:')

    def test_preload_classes(self):
        # pre-defined class mappings have been given at test class setup, test them here
        self.assertEqual(RdfProxy._RdfProxy__rdf2py['<tml:Timeline>'], 'TmlTimeline')  # type: ignore
        self.assertEqual(RdfProxy._RdfProxy__rdf2py['<upper:Entity>'], 'UpperEntity')  # type: ignore
        self.assertEqual(RdfProxy._RdfProxy__py2rdf['TmlTimeline'], '<tml:Timeline>')  # type: ignore
        self.assertEqual(RdfProxy._RdfProxy__py2rdf['UpperEntity'], '<upper:Entity>')  # type: ignore

    def test_classfactory(self):
        clazz = classfactory('foo')
        self.assertTrue(isinstance(clazz, type))
        self.assertTrue(issubclass(clazz, RdfProxy))
        self.assertEqual(str(clazz), "<class 'hfc_thrift.rdfproxy.foo'>")

    def test_getClass(self):
        clazz1 = RdfProxy.getClass('<dom:Brother>')
        self.assertTrue(isinstance(clazz1, type))
        self.assertTrue(issubclass(clazz1, RdfProxy))
        self.assertEqual(str(clazz1), "<class 'hfc_thrift.rdfproxy.Brother'>")
        self.assertEqual(clazz1._RdfProxy__clazzuri, '<dom:Brother>')  # type: ignore
        self.assertEqual(len(clazz1._RdfProxy__propertyType), 17)  # type: ignore
        self.assertEqual(len(clazz1._RdfProxy__propertyRange), 17)  # type: ignore
        self.assertEqual(len(clazz1._RdfProxy__propertyBaseToFull), 17)  # type: ignore
        clazz2 = RdfProxy.getClass('<dom:Brother>')
        self.assertIs(clazz1, clazz2)

    def test_getObject(self):
        bro = RdfProxy.getObject("Brother")
        self.assertEqual(RdfProxy._RdfProxy__uri2pyclass[RdfProxy._RdfProxy__py2rdf['Brother']],  # type: ignore
                         type(bro))

    def test_objectcreation(self):
        newchild = RdfProxy.createProxy("<dom:Child>", "<dom:child_22>")
        self.assertEqual(type(newchild).__name__, "Child")
        self.assertEqual(str(type(newchild)), "<class 'hfc_thrift.rdfproxy.Child'>")
        self.assertEqual(newchild.uri, "<dom:child_22>")
        # now test the properties
        self.assertTrue(newchild.isFunctional("<dom:hasIntimacyLevel>"))
        newchild.hasIntimacyLevel = 0.7

    def test_rdf2pyobj(self):
        self.assertEqual(RdfProxy.rdf2pyobj('"1"^^<xsd:int>'), 1)
        self.assertEqual(RdfProxy.rdf2pyobj('"1.0"^^<xsd:double>'), 1.0)
        self.assertEqual(RdfProxy.rdf2pyobj('"ttt"^^<xsd:string>'), 'ttt')
        child = RdfProxy.getObject("Child")
        # child is now in cache and can be retrieved via its uri
        self.assertTrue(RdfProxy._RdfProxy__uri2pyobject[child.uri] is child)  # type: ignore
        # delete from cache and force refresh
        del RdfProxy._RdfProxy__uri2pyobject[child.uri]  # type: ignore
        newchild = RdfProxy.rdf2pyobj(child.uri)  # type: ignore
        # a new instance is created
        self.assertFalse(newchild is child)  # type: ignore

    def test_python2rdf(self):
        self.assertEqual(RdfProxy.python2rdf('hi'), '"hi"^^<xsd:string>')
        self.assertEqual(RdfProxy.python2rdf(7), '"7"^^<xsd:int>')
        self.assertEqual(RdfProxy.python2rdf(7.7), '"7.7"^^<xsd:double>')
        with self.assertRaisesRegex(ValueError, "unsupported type <class 'NoneType'> of None"):
            RdfProxy.python2rdf(None)
        with self.assertRaisesRegex(ValueError, "unsupported type <class 'NoneType'> of None"):
            RdfProxy.python2rdf([None])
        newchild = RdfProxy.createProxy("<dom:Child>", "<dom:child_23>")
        self.assertEqual(RdfProxy.python2rdf(newchild), "<dom:child_23>")
        self.assertEqual(RdfProxy.python2rdf([newchild]), ["<dom:child_23>"])
        self.assertEqual(RdfProxy.python2rdf([[newchild]]), [["<dom:child_23>"]])

    def test_relationalprop(self):
        newchild = RdfProxy.createProxy("<dom:Child>", "<dom:child_23>")
        self.assertEqual(RdfProxy.python2rdf(newchild), "<dom:child_23>")
        with self.assertRaisesRegex(ValueError, "unsupported type <class 'NoneType'> of None"):
            newchild.age = None
        newchild.age = 7
        self.assertEqual(newchild.age, 7)
        bro1 = RdfProxy.createProxy("<dom:Brother>", "<dom:bro_23>")
        bro2 = RdfProxy.createProxy("<dom:Brother>", "<dom:bro_24>")
        # TODO: This causes an erorr in line 179, as the value returned from HFC is <owl:Nothing>
        # newchild.hasBrother = []
        # no brothers assigned yet
        self.assertEqual(len(cast(RdfSet, newchild.hasBrother)), 0)
        bros = RdfProxy.selectQuery(f"select ?b where {newchild.uri} <dom:hasBrother> ?b ?_")
        self.assertEqual(0, len(cast(RdfSet, bros)))
        # assign brothers:
        newchild.hasBrother = [bro1, bro2]
        self.assertEqual(2, len(cast(RdfSet, newchild.hasBrother)))
        bros = RdfProxy.selectQuery(f"select ?b where {newchild.uri} <dom:hasBrother> ?b ?_")
        self.assertEqual(2, len(cast(RdfSet, bros)))
        self.assertTrue(bro1 in bros and bro2 in bros)
        bro3 = RdfProxy.getObject("Brother")
        cast(RdfSet, newchild.hasBrother).add(bro3)
        self.assertEqual(3, len(cast(RdfSet, newchild.hasBrother)))
        cast(RdfSet, newchild.hasBrother).discard(bro1)
        self.assertEqual(2, len(cast(RdfSet, newchild.hasBrother)))
        self.assertTrue(bro2 in newchild.hasBrother and bro3 in newchild.hasBrother)


if __name__ == '__main__':
    unittest.main()
