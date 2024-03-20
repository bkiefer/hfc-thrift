import unittest
import rdfbase
from rdfproxy import RdfProxy


class MyTestCase(unittest.TestCase):
    @staticmethod
    def setUpClass() -> None:
        rdfbase.connect('localhost', 9090)

    @staticmethod
    def tearDownClass() -> None:
        rdfbase.disconnect()

    def test_something(self):
        self.assertEqual(len(rdfbase.rdf2py), 223)  # add assertion here

    def test_py2xsd(self):
        self.assertEqual('"1"^^<xsd:int>', rdfbase.python2xsd(1))
        self.assertEqual(1, rdfbase.xsd2python('"1"^^<xsd:int>'))

    def test_classcreation(self):
        self.assertTrue(isinstance(RdfProxy.getClass("Child", "<dom:Child>"), type))

    def test_objectcreation(self):
        newchild = RdfProxy.createProxy("Child", "<dom:Child>", "<dom:child_22>")
        self.assertEqual(type(newchild).__name__, "Child")
        self.assertEqual(newchild.uri, "<dom:child_22>")


if __name__ == '__main__':
    # TODO: start hfc server process
    unittest.main()
    # TODO: Stop hfc server process
