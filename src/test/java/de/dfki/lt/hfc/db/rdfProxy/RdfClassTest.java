/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.lt.hfc.db.rdfProxy;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.*;

import de.dfki.lt.hfc.db.QueryResult;
import de.dfki.lt.hfc.db.server.HfcDbServer;
import de.dfki.lt.hfc.db.server.TestUtils;

/**
 *
 * @author Christophe Biwer, christophe.biwer@dfki.de
 */
public class RdfClassTest {

  private static HfcDbServer server;

  private RdfProxy _proxy;

  @BeforeClass
  public static void startServer() {
    server = TestUtils.startServer(TestUtils.SERVER_PORT);
  }

  @AfterClass
  public static void shutdownServer() {
    TestUtils.shutdownServer(server);
  }

  @Before
  public void setUp() {
    _proxy = TestUtils.setupProxy();
  }

  @Test
  public void testFetchRdfClass() {
    String uri = "<dom:Hobby>";
    String expResult = "<dom:Hobby>";
    RdfClass result = _proxy.getClass(uri);
    assertNotNull(result);
    assertEquals(expResult, result.toString());
  }

  @Test
  public void testGetRdfClass1() {
    String name = "Hobby";
    String expResult = "<dom:Hobby>";
    RdfClass result = _proxy.fetchClass(name);
    assertEquals(expResult, result._uri);
    // doing twice to check if in _resolvedClasses of RdfProxy
    _proxy.getClass(name);
  }

  @Test
  public void testGetRdfClass2() {
    String name = "Schlaraffenland";
    RdfClass result = _proxy.getClass(name);
    assertNull(result);
  }

  @Test
  public void testHierarchy1() {
    RdfClass instance = _proxy.getClass("<dom:Gender>");

    RdfClass expectedSupClass1 = _proxy.getClass("<dom:PALDomain>");
    RdfClass expectedSubClass1 = _proxy.getClass("<dom:Male>");
    RdfClass expectedSubClass2 = _proxy.getClass("<dom:Female>");
    RdfClass expectedTrue = _proxy.getClass("<dom:Gender>");

    assertTrue(expectedSupClass1._uri,
        instance.isSubclassOf(expectedSupClass1));
    assertTrue(expectedSubClass1._uri,
        instance.isSuperclassOf(expectedSubClass1));
    assertTrue(expectedSubClass2._uri,
        instance.isSuperclassOf(expectedSubClass2));
    assertTrue(expectedTrue._uri,
        instance.isSuperclassOf(expectedTrue));
    assertTrue(expectedTrue._uri,
        instance.isSuperclassOf(expectedTrue));
  }

  @Test
  public void testHierarchy2() {
    RdfClass instance = _proxy.getClass("<dom:Male>");

    RdfClass expectedSupClass1 = _proxy.getClass("<dom:Gender>");
    RdfClass expectedSupClass2 = _proxy.getClass("<dom:PALDomain>");

    assertTrue(expectedSupClass2._uri,
        instance.isSubclassOf(expectedSupClass2));
    assertTrue(expectedSupClass1._uri,
        instance.isSubclassOf(expectedSupClass1));
  }

  @Test
  public void testGetNewInstance1() {
    String namespace = "dom:";
    RdfClass instance = _proxy.getClass("<dom:Child>");
    Rdf result = instance.getNewInstance(namespace);
    assertNotNull(result);
    QueryResult qresult = _proxy.selectQuery(
        "select ?t ?time where {} <rdf:type> ?t ?time "
        , result.getURI());
    assertEquals(1, qresult.getTable().getRowsSize());
    RdfClass instClass = _proxy.getClass(qresult.getTable().getRows().get(0).get(0));
    assertEquals(instance, instClass);
  }

  @Test
  public void testProperties() {
    String[] result = {
      "<dom:forename>",
      "<dom:hasGender>",
      "<dom:image>",
      "<dom:isLocatedAt>",
      "<dom:remark>",
      "<dom:surname>",
      "<upper:worksOn>",
      "<upper:hasAbility>",
      "<upper:involvedIn>",
      "<upper:isAwareOf>",
      "<upper:name>",
      "<upper:worksFor>",
      "<upper:worksIn>",
    };
    RdfClass instance = _proxy.getClass("<dom:Father>");
    for (String s : result) {
      assertTrue(s, instance.getPropertyType(s) > 0);
    }
  }

  @Test
  public void testHasProperty() {
    RdfClass instance = _proxy.getClass("<dom:Child>");

    String property = "<dom:treats>";
    int expResult = 0;
    int result = instance.getPropertyType(property);
    assertEquals("fetchProperty Type - different domain",
        expResult, result);

    String property2 = "<dom:hasFather>";
    int expResult2 = RdfClass.FUNCTIONAL_PROPERTY | RdfClass.OBJECT_PROPERTY;
    int result2 = instance.getPropertyType(property2);
    assertEquals("fetchProperty Type - same domain (fun. & obj.)",
        expResult2, result2);

    String property3 = "<dom:isFather>";
    int expResult3 = 0;
    int result3 = instance.getPropertyType(property3);
    assertEquals("fetchProperty Type - property not contained",
        expResult3, result3);

    RdfClass instance2 = _proxy.getClass("<dom:Animate>");
    String property4 = "<dom:forename>";
    int expResult4 = RdfClass.FUNCTIONAL_PROPERTY | RdfClass.DATATYPE_PROPERTY;
    int result4 = instance2.getPropertyType(property4);
    assertEquals("hasProperty - property not contained",
        expResult4, result4);
  }

  /** Fetch property defined on dom:Child, works only if the owl
   *  rules are in place, since otherwise the statement
   *  "Child subclassOf Child" is missing
   */
  @Test
  public void testHasProperty2() {
    RdfClass instance = _proxy.getClass("<dom:Child>");

    String property5 = "<dom:hasTreatment>";
    int expResult5 = RdfClass.FUNCTIONAL_PROPERTY | RdfClass.OBJECT_PROPERTY;;
    int result5 = instance.getPropertyType(property5);
    assertEquals("fetchProperty Type - property not contained",
        expResult5, result5);

  }

  public void testGetNewInstance2() {
    String namespace = "doxxm:";
    RdfClass instance = _proxy.fetchClass("<dom:Child>");
    Rdf result = (instance.getNewInstance(namespace));
    QueryResult qresult = _proxy.selectQuery(
        "select ?t where {} <rdf:type> ?t ?time ",
        result.getURI());
    assertEquals(1, qresult.getTable().getRowsSize());
    assertEquals("<dom:Child>", qresult.getTable().getRows().get(0).get(0));
  }


  @Test
  public void testGetPropertytype1() {
    HashMap<String, String> resolv = new HashMap<>();
    resolv.put("Child", "<dom:Child>");
    _proxy.setBaseToUri(resolv);
    String classBaseName = "Child";
    RdfClass clazz = _proxy.fetchClass(classBaseName);
    List<String> elem = Arrays.asList(new String[] {"hasFather", "hasGender"});
    List<String> resultingList = clazz.getPropertyType(elem);
    String result = resultingList.get(resultingList.size()-1);
    String expected = "<dom:Gender>";
    assertEquals("get Gender", expected, result);
  }

  @Test
  public void testGetPropertytype2() {
    String classBaseName = "Father";
    HashMap<String, String> resolv = new HashMap<>();
    resolv.put("Father", "<dom:Father>");
    _proxy.setBaseToUri(resolv);
    RdfClass clazz = _proxy.fetchClass(classBaseName);
    List<String> elem = Arrays.asList(new String[] { "surname" });
    List<String> resultingList = clazz.getPropertyType(elem);
    String result = resultingList.get(resultingList.size()-1);
    String expected = "String";
    assertEquals("get surname", expected, result);
  }

  @Test
  public void testGetPropertytype3() {
    String classBaseName = "Father";
    HashMap<String, String> resolv = new HashMap<>();
    resolv.put("Father", "<dom:Father>");
    _proxy.setBaseToUri(resolv);
    RdfClass clazz = _proxy.fetchClass(classBaseName);
    List<String> elem = Arrays.asList(
        new String[] { "surname", "Gender", });

    assertNull("get surname", clazz.getPropertyType(elem));
  }

  @Test
  public void testGetPropertytype4() {
    String classBaseName = "Father";
    RdfClass clazz = _proxy.fetchClass(classBaseName);
    List<String> elem = Arrays.asList(
        new String[] { "Gender", "surname" });
    assertNull("get surname", clazz.getPropertyType(elem));
  }

  @Test
  public void testGetPropertytype5() {
    String classBaseName = "Quiz";
    HashMap<String, String> resolv = new HashMap<>();
    resolv.put("Quiz", "<dom:Quiz>");
    _proxy.setBaseToUri(resolv);
    RdfClass clazz = _proxy.fetchClass(classBaseName);
    List<String> elem = Arrays.asList(
        new String[] {"hasHistory", "questionId"});
    List<String> resultingList = clazz.getPropertyType(elem);
    String result = resultingList.get(resultingList.size()-1);

    String expected = "Integer";
    assertEquals(expected, result);
  }

  @Test
  public void testGetPropertytype6() {
    String classBaseName = "Child";
    HashMap<String, String> resolv = new HashMap<>();
    resolv.put("Child", "<dom:Child>");
    _proxy.setBaseToUri(resolv);
    RdfClass clazz = _proxy.fetchClass(classBaseName);
    List<String> elem = Arrays.asList(
        new String[] { "hasFather", "forename", "hasGender" });

    List<String> resultingList = clazz.getPropertyType(elem);

    String expected = null;
    assertEquals("get surname", expected, resultingList);
  }

  @Test
  public void testFetchMostSpecific() {
    String class1 = "<dom:Child>";
    String class2 = "<dom:Animate>";
    String class3 = "<dom:Father>";

    String result1 = _proxy.fetchMostSpecific(class1, class2);
    String expected1 = "<dom:Child>";
    assertEquals("most specific of " + class1 + " & " + class2,
            expected1, result1);

    String result1b = _proxy.fetchMostSpecific(class2, class1);
    String expected1b = "<dom:Child>";
    assertEquals("most specific of " + class2 + " & " + class1,
            expected1b, result1b);

    String result2 = _proxy.fetchMostSpecific(class1, class3);
    String expected2 = null;
    assertEquals("most specific of " + class1 + " & " + class3,
            expected2, result2);

  }

  @Test
  public void testEquivalent1() {
    String class1 = "<dom:Actor>";
    String class2 = "<sem:Actor>";
    String class3 = "<upper:Agent>";
    String class4 = "<dial:Agent>";

    RdfClass clazz1 = _proxy.getClass(class1);
    RdfClass clazz2 = _proxy.getClass(class2);
    RdfClass clazz3 = _proxy.getClass(class3);
    RdfClass clazz4 = _proxy.getClass(class4);
    assertNotNull(clazz1);
    assertNotNull(clazz2);
    assertNotNull(clazz3);
    assertNotNull(clazz4);
    assertEquals(clazz1, clazz2);
    assertEquals(clazz1, clazz3);
    assertEquals(clazz1, clazz4);
  }


  @Test public void testSubclass1() {
    String[] chain = {
        "<dial:DialogueAct>", "<dial:AggregateFunction>", "<dial:Accept>",
        "<dial:AcceptOffer>"
    };
    String[] c2 = { "<dial:DialogueAct>", "<dial:AggregateFunction>",
        "<dial:Decline>", "<dial:Correction>" };
    String revoke = "<dial:Correction>";

    for (String s : chain) {
      assertTrue(_proxy.isSubclassOf(s, chain[0]));
    }
    for (String s : c2) {
      assertTrue(_proxy.isSubclassOf(s, c2[0]));
    }
    assertTrue(_proxy.isSubclassOf(c2[3], revoke));
  }

  @Test
  public void testEquivalent2() {
    String class1 = "Actor";
    String class2 = "<sem:Actor>";
    String class3 = "Agent";
    String class4 = "<dial:Agent>";

    RdfClass clazz1 = _proxy.fetchClass(class1);
    RdfClass clazz2 = _proxy.getClass(class2);
    RdfClass clazz3 = _proxy.fetchClass(class3);
    RdfClass clazz4 = _proxy.getClass(class4);
    assertEquals(clazz1, clazz2);
    assertEquals(clazz1, clazz3);
    assertEquals(clazz1, clazz4);
  }


  @Test
  public void testEquivalentSubclass2() {
    String class1 = "<sem:Actor>";
    String class3 = "<upper:Agent>";
    String class4 = "<dom:Child>";

    RdfClass clazz1 = _proxy.getClass(class1);
    RdfClass clazz3 = _proxy.getClass(class3);
    RdfClass clazz4 = _proxy.getClass(class4);
    assertTrue(clazz4.isSubclassOf(clazz1));
    assertTrue(clazz4.isSubclassOf(clazz3));
    assertTrue(clazz1.isSuperclassOf(clazz4));
    assertTrue(clazz3.isSuperclassOf(clazz4));
  }

  @Test public void testSubclass2() {
    String[] sup = {
        "<dom:Actor>", "<dom:Actor>", "<sem:Entity>", "<sem:Actor>", "<upper:Entity>"
    };
    for (String s : sup) {
      assertTrue(s, _proxy.isSubclassOf("<dom:Child>", s));
    }
  }
  @Test public void testSubclass4() {
    assertTrue("1", _proxy.isSubclassOf("<dom:Child>", "<dom:Actor>"));
  }

  @Test public void testSubclass5() {
    assertTrue("1", _proxy.isSubclassOf("<dom:Child>", "<dom:Actor>"));
  }

  @Test public void testSubclass3() {
    assertTrue("1", _proxy.isSubclassOf("<dom:Child>", "<dom:Actor>"));
    assertTrue("2", _proxy.isSubclassOf("<dom:Child>", "<dom:Actor>"));
    assertTrue("3", _proxy.isSubclassOf("<dom:Child>", "<dom:Actor>"));
  }

}
