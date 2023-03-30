/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.lt.hfc.db.rdfProxy;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.*;

import de.dfki.lt.hfc.db.server.HfcDbServer;
import de.dfki.lt.hfc.db.server.TestUtils;

/**
 *
 * @author Christophe Biwer, christophe.biwer@dfki.de
 */
public class RdfTest {

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

  /**
   * Test of getRdf method, of class Rdf.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetRdf() {
    String child = "<rifca:Child_0>";
    Rdf testchild = _proxy.getRdf(child);
    String uri = testchild.getURI();
    String clazz = testchild.getClazz().toString(); // returns class uri
    // must be one of these
    String[] expected_results = {"<xai:Child>", "<dom:Child>", "<edu:Child>"};
    assertEquals(child, uri);
    assertTrue(Arrays.asList(expected_results).contains(clazz));
  }

  /**
   * Test of getRdf method, of class Rdf.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetRdf2() {
    Rdf testfather = _proxy.getRdf("<dom:father_66>");
    assertNull(testfather);
  }

  /**
   * Test of getRdf method, of class Rdf.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetRdf3() {
    Rdf testfather = _proxy.getRdf("<dom:christophe>");
    assertNull(testfather);
  }

  /**
   * Test of getValue method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testGetValue() {
    String predicate = "<dom:surname>";
    Rdf instance = _proxy.getRdf("<rifca:mother_1>");
    Set<Object> expResult = new HashSet<Object>() {
      {
        add("Truus");
      }
    };
    Set<Object> result = instance.getValue(predicate);
    assertEquals(expResult, result);
    assertFalse(instance.getClazz().isFunctionalProperty(predicate));
  }

  /**
   * Test of getSingleValue method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testGetSingleValue() {
    String predicate = "<dom:hasFather>";
    Rdf instance = _proxy.getRdf("<rifca:Child_0>");
    Object expResult = _proxy.getRdf("<rifca:father_2>");
    Object result = instance.getSingleValue(predicate);
    assertEquals(expResult, result);
    assertTrue(instance.getClazz().isFunctionalProperty(predicate));
  }

  /**
   * Test of getSingleValue method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testGetSingleValue2() {
    String predicate = "<dom:surname>";
    Rdf instance = _proxy.getRdf("<rifca:Child_0>");
    Set<Object> expResult = new HashSet<Object>() {
      {
        add("Jansen");
        add("Testi2");
      }
    };
    instance.add("<dom:forename>", "Testi2");
    Set<Object> result = instance.getValue(predicate);
    assertTrue(expResult.containsAll(result));
    assertFalse(instance.getClazz().isFunctionalProperty(predicate));
   }


  /**
   * Inexistent property: no value
   */
  @Test
  public void testGetSingleValue3() {
    String predicate = "<dom:middlename>";
    Rdf instance = _proxy.getRdf("<rifca:Child_0>");
    Object result = instance.getSingleValue(predicate);
    assertNull(result);
  }


  /**
   * Test of getClazz method, of class Rdf.
   * @throws org.apache.thrift.TException
   *
   * TODO: THIS CURRENTLY DOES NOT WORK BECAUSE RdfProxy ASSUMES THAT
   * ALL INSTANCES HAVE TIME ATTACHED, WHICH IS NOT TRUE FOR THIS URI
   * THIS SHOULD BE PUT BACK INTO PLACE ONCE WE SWITCH TO QUINTUPLES,
   * SINCE THEN, ALL TUPLES SHOULD BE TREATED ALIKE.
   *
  @Test
  public void testGetClazz() {
    Rdf instance = _proxy.getRdf("<dom:aquamarine>");
    RdfClass result = instance.getClazz();
    RdfClass expResult = _proxy.getRdfClass("<dom:Color>");
    assertEquals(expResult, result);
  }
  */

  @Test
  public void testGetClazzOfDomainInstance() {
    Rdf instance = _proxy.getRdf("<rifca:Child_0>");
    RdfClass result = instance.getClazz();
    RdfClass expResult = _proxy.getClass("<dom:Child>");
    assertEquals(expResult, result);
  }

  /**
   * Test of has method, of class Rdf.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testHas() {
    Rdf instance = _proxy.getRdf("<rifca:Child_0>");

    assertTrue(instance.has("<dom:hasFather>"));

    assertFalse(instance.has("<dom:bmi>"));
  }

  /**
   * Test of oneOf method, of class Rdf.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testOneOf() {
    Rdf instance = _proxy.getRdf("<rifca:mother_1>");

    String predicate = "<dom:forename>";

    Object[] value = {"Henk", "Anna", "Christophe", "Mier"};
    assertTrue(instance.oneOf(predicate, value));

    Object[] value2 = {"Anna", "Christophe"};
    assertFalse(instance.oneOf(predicate, value2));
  }

  /**
   * Test of put method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testAdd() {
    String predicate = "<dom:forename>";
    Object new_value = "Testi";
    Rdf instance = _proxy.getRdf("<rifca:Child_1>");
    instance.add(predicate, new_value);
    Object expResult = "Testi";
    Object result = instance.getSingleValue("<dom:forename>");
    assertEquals(expResult, result);
  }


   /**
   * Test of add method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testAddUnknownPredicate() {
    String predicate = "<dom:setUnknownPredicate>";
    Object new_value = "Testi";
    Rdf instance = _proxy.getRdf("<rifca:Child_1>");
    instance.add(predicate, new_value);
    Object expResult = "Testi";
    Set<Object> result = instance.getValue("<dom:setUnknownPredicate>");
    assertTrue(result.contains(expResult));
  }

  /**
   * Test of add method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testAddUnknownPredicate2() {
    String predicate = "<dom:forename>";
    Object new_value = "Testi";
    Rdf instance = _proxy.getRdf("<rifca:Child_8>");
    instance.add(predicate, new_value);
    Object expResult = "Testi";
    Set<Object> result = instance.getValue("<dom:forename>");
    assertTrue(result.contains(expResult));
  }

  /**
   * Test of wrong syntax : QueryException
   *
   * @throws java.lang.Exception TODO: CHECK WITH CHRISTIAN WHY THE EXCEPTION
   * DOES NOT COME THROUGH
   */
   @Test(expected=RuntimeException.class)
   public void testWrongQuery() {
     Rdf instance = _proxy.getRdf("<rifca:Child_0>");
     assertFalse(instance.has("dom:hasFather"));
     }

   /** Test of wrong syntax : QueryException
    */
   @Test(expected=RuntimeException.class)
   public void testWrongQuery2() {
     Rdf instance = _proxy.getRdf("<rifca:Child_0>");

     String predicate = "dom:hasFather";
     boolean expResult = true;
     Set<Object> result;
     result = instance.getValue(predicate);
     assertEquals("has - True", expResult, result);
   }

   /** Test setting/getting a pod xsd type */
   @Test
   public void testXsdBoolTreatment() {
     Rdf quiz = _proxy.getClass("<dom:Quiz>").getNewInstance("test:");
     assertNull(quiz.getSingleValue("<dom:quizbool>"));
     quiz.setValue("<dom:quizbool>", true);
     boolean result = (Boolean)quiz.getSingleValue("<dom:quizbool>");
     assertTrue(result);
     quiz.setValue("<dom:quizbool>", false);
     result = (Boolean)quiz.getSingleValue("<dom:quizbool>");
     assertFalse(result);
     quiz.clearValue("<dom:quizbool>");
     Object o = quiz.getSingleValue("<dom:quizbool>");
     assertNull(o);
   }

}
