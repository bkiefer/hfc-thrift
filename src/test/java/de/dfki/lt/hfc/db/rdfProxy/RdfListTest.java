/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.lt.hfc.db.rdfProxy;

import static org.junit.Assert.*;

import org.junit.*;

import de.dfki.lt.hfc.db.server.HfcDbServer;
import de.dfki.lt.hfc.db.server.TestUtils;

/**
 *
 * @author Christophe Biwer, christophe.biwer@dfki.de
 */
public class RdfListTest {

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
    _proxy.getHierarchy().addNewSingleton("<rdf:List>");
  }

  /** test list creation
   * @throws java.lang.Exception
   */
  @Test
  public void testCreateList() {
    RdfList newList = _proxy.newList("pal:");
    assertEquals(_proxy.getClass(RdfProxy.RDF_LIST),
        _proxy.getRdf(newList.getHeadUri()).getClazz());
    assertTrue(newList.isEmpty());
    _proxy.clearCache();
    RdfList existing = _proxy.getList(_proxy.getRdf(newList.getHeadUri()));
    assertFalse(newList == existing);
    assertEquals(newList.getHeadUri(), existing.getHeadUri());
    assertTrue(existing.isEmpty());
  }

  /** Test adding to a rdf list
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testAddToList() {
    RdfList newList = _proxy.newList("pal:");
    assertEquals(_proxy.getClass(RdfProxy.RDF_LIST),
        _proxy.getRdf(newList.getHeadUri()).getClazz());
    assertTrue(newList.isEmpty());
    newList.add("test");
    assertEquals(1, newList.length());
    // does not do what i want since it uses the existing list
    _proxy.clearCache();
    RdfList existing = _proxy.getList(_proxy.getRdf(newList.getHeadUri()));
    assertEquals(newList.getHeadUri(), existing.getHeadUri());
    assertEquals(1, existing.length());
    assertEquals("test", existing.get(0));
  }

  /** Test of get element from list
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetElement() {
    // TODO
    RdfList newList = _proxy.newList("pal:");
    for (int i = 0; i < 10; ++i) {
      newList.add(i);
    }
    _proxy.clearCache();
    // does not do what i want since it uses the existing list
    RdfList existing = _proxy.getList(_proxy.getRdf(newList.getHeadUri()));
    assertEquals(10, existing.length());
    for (int i = 0; i < 10; ++i) {
      assertEquals(i, existing.get(i));
    }
  }

  /**
   * Test of getValue method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testRemoveFromList() {
    // TODO
    //assertEquals(expResult, result);
    //assertFalse(instance.getClazz().isFunctionalProperty(predicate));
  }

  /**
   * Test of getSingleValue method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testPeekFirstLast() {
    RdfList newList = _proxy.newList("pal:");
    assertNull(newList.peekFirst());
    assertNull(newList.peekLast());
    String listUri = newList.getHeadUri();
    _proxy.clearCache();
    // does not do what i want since it uses the existing list
    RdfList existing = _proxy.getList(_proxy.getRdf(listUri));
    assertNull(existing.peekFirst());
    assertNull(existing.peekLast());
    existing.add("test");
    assertEquals("test", existing.peekFirst());
    assertEquals("test", existing.peekLast());
    _proxy.clearCache();
    existing = _proxy.getList(_proxy.getRdf(listUri));
    assertEquals("test", existing.peekFirst());
    assertEquals("test", existing.peekLast());
    existing.add("test2");
    _proxy.clearCache();
    existing = _proxy.getList(_proxy.getRdf(listUri));
    assertEquals("test", existing.peekFirst());
    assertEquals("test2", existing.peekLast());
  }

  /**
   * Test of getSingleValue method, of class Rdf.
   * @throws java.lang.Exception
   */
  @Test
  public void testToString() {
    RdfList newList = _proxy.newList("pal:");
    int from = newList.getHeadUri().length();
    assertEquals("[]", newList.toString().substring(from));
    newList.add(1);
    assertEquals("[1]", newList.toString().substring(from));
    newList.add(2);
    assertEquals("[1, 2]", newList.toString().substring(from));
  }

  /** Test no list */
  @Test
  public void testNoList() {
    Rdf child = _proxy.getClass("<dom:Child>").getNewInstance("pal:");
    assertNull(_proxy.getList(child));
  }

}
