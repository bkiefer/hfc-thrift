import unittest
import subprocess
import os

import xsdutils
from rdfproxy import RdfProxy

proc = None

class MyTestCase(unittest.TestCase):
    @staticmethod
    def setUpClass() -> None:
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
        # don't use default port: PAL hfc service uses it.
        RdfProxy.init_rdfproxy('localhost', 7777)

    @staticmethod
    def tearDownClass() -> None:
        global proc
        RdfProxy.shutdown_rdfproxy()
        # stop hfc server process
        proc.terminate()

    def test_classes_loaded(self):
        self.assertEqual(len(RdfProxy.__dict__["_RdfProxy__rdf2py"]), 223)

    def test_py2xsd(self):
        self.assertEqual('"1"^^<xsd:int>', xsdutils.python2xsd(1))
        self.assertEqual(1, xsdutils.xsd2python('"1"^^<xsd:int>'))

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

