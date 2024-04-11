import sys
import unittest
import subprocess
import os

import xsdutils
from rdfproxy import RdfProxy

proc = None

class MyTestCase(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if sys.platform.startswith('linux'):
            # Linux specific procedures
            global proc
            os.chdir("../../..")
            # start hfc server process
            proc = subprocess.Popen(["/usr/bin/java",
                                     "-Dlogback.configurationFile=./logback.xml",
                                     "-jar", "target/hfc-server.jar",
                                     "-p", "7777", "src/test/data/test.yml"]
                                    , encoding="UTF-8", stdout=subprocess.PIPE)

            for line in proc.stdout:
                if "Starting" in line:
                    print("HFC Server started successfully", flush=True)
                    break
        else:
            print('Make sure HFC server is running on port 7777 using test.yml configuration')

        # don't use default port: PAL hfc service uses it.
        RdfProxy.init_rdfproxy('localhost', 7777)

    @classmethod
    def tearDownClass(cls) -> None:
        RdfProxy.shutdown_rdfproxy()

        if sys.platform.startswith('linux'):
            # stop hfc server process
            global proc
            proc.terminate()

    def test_isXsd(self):
        self.assertTrue(xsdutils.isXsd('<http://www.w3.org/2001/XMLSchema#string>'))
        self.assertTrue(xsdutils.isXsd('<xsd:string>'))
        self.assertFalse(xsdutils.isXsd('no-xsd'))

    def test_extractTypeAndValue(self):
        self.assertEqual(xsdutils.extractTypeAndValue('"1"^^<xsd:int>'), ('int', '1'))
        self.assertEqual(xsdutils.extractTypeAndValue('"1.0"^^<xsd:double>'), ('double', '1.0'))
        self.assertEqual(xsdutils.extractTypeAndValue('"ttt"^^<xsd:string>'), ('string', 'ttt'))
        self.assertIsNone(xsdutils.extractTypeAndValue('no-xsd'))
        self.assertIsNone(xsdutils.extractTypeAndValue('ttt^^no-xsd'))

    def test_xsd2py(self):
        self.assertEqual(xsdutils.xsd2python('"1"^^<xsd:int>'), 1)
        self.assertEqual(xsdutils.xsd2python('"1.0"^^<xsd:double>'), 1.0)
        self.assertEqual(xsdutils.xsd2python('"ttt"^^<xsd:string>'), 'ttt')
        self.assertIsNone(xsdutils.xsd2python('no-xsd'))
        self.assertIsNone(xsdutils.xsd2python('"ttt"^^<xsd:notype>'))

    def test_py2xsd(self):
        self.assertEqual(xsdutils.python2xsd(1), '"1"^^<xsd:int>')
        self.assertEqual(xsdutils.python2xsd(1.0), '"1.0"^^<xsd:double>')
        self.assertEqual(xsdutils.python2xsd('ttt'), '"ttt"^^<xsd:string>')
        self.assertIsNone(xsdutils.python2xsd(None))

    def test_splitOwlUri(self):
        self.assertEqual(xsdutils.splitOwlUri('<xsd:int>'), ('xsd', 'int'))
        self.assertEqual(xsdutils.splitOwlUri('<http://www.w3.org/2001/XMLSchema#string>'), ('http://www.w3.org/2001/XMLSchema', 'string'))
        self.assertIsNone(xsdutils.splitOwlUri('no-uri'))

    def test_classes_loaded(self):
        self.assertEqual(len(RdfProxy._RdfProxy__rdf2py), 223)

    def test_classcreation(self):
        clz = RdfProxy.getClass("Child", "<dom:Child>")
        self.assertTrue(isinstance(clz, type))

    def test_objectcreation(self):
        newchild = RdfProxy.createProxy("Child", "<dom:Child>", "<dom:child_22>")
        self.assertEqual(type(newchild).__name__, "Child")
        self.assertEqual(newchild.uri, "<dom:child_22>")
        # now test the properties
        self.assertTrue(newchild.isFunctional("<dom:hasIntimacyLevel>"))
        newchild.hasIntimacyLevel = 0.7

    def test_relationalprop(self):
        newchild = RdfProxy.createProxy("Child", "<dom:Child>", "<dom:child_22>")
        bro1 = RdfProxy.createProxy("Brother", "<dom:Brother>", "<dom:bro_23>")
        bro2 = RdfProxy.createProxy("Brother", "<dom:Brother>", "<dom:bro_24>")
        newchild.hasBrother = [bro1, bro2]
        self.assertEqual(2, len(newchild.hasBrother))
        bro3 = RdfProxy.getObject("Brother")
        newchild.hasBrother.add(bro3)
        self.assertEqual(3, len(newchild.hasBrother))
        newchild.hasBrother.remove(bro1)
        self.assertEqual(2, len(newchild.hasBrother))
        self.assertTrue(bro2 in newchild.hasBrother and bro3 in newchild.hasBrother)


if __name__ == '__main__':
    unittest.main()

