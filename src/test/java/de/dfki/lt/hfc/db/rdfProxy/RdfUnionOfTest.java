/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.lt.hfc.db.rdfProxy;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.*;

import de.dfki.lt.hfc.db.server.HfcDbServer;
import de.dfki.lt.hfc.db.server.TestUtils;

/**
 *
 */
public class RdfUnionOfTest {

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
  public void testStructureUnionOf() {
    RdfClass cl1 = _proxy.getClass("<tml:Food>");
    RdfClass cl2 = _proxy.getClass("<tml:OtherEvent>");
    RdfClass cl3 = _proxy.getClass("<tml:Glycemia>");
    List<RdfClass> sup1 = _proxy.getAllSuperClasses(cl1);
    List<RdfClass> sup2 = _proxy.getAllSuperClasses(cl2);
    List<RdfClass> sup3 = _proxy.getAllSuperClasses(cl3);
    sup1.retainAll(sup2);
    sup1.retainAll(sup3);
    sup1.remove(_proxy.getClass("<tml:Timeline>"));
    assertEquals(1, sup1.size());
  }

  @Test
  public void testInheritedPropertiesOfUnion() {
    RdfClass cl1 = _proxy.getClass("<tml:Food>");
    RdfClass cl2 = _proxy.getClass("<tml:OtherEvent>");
    String propName = "<tml:description>";
    assertNotNull(cl1.fetchProperty("description"));
    assertNotNull(cl2.fetchProperty("description"));
    assertEquals(propName, cl2.fetchProperty("description"));
    assertEquals(cl1.fetchProperty("description"),
        cl2.fetchProperty("description"));
    assertTrue(cl1.getPropertyRange(propName).contains("<xsd:string>"));
    assertEquals(1, cl1.getPropertyRange(propName).size());
  }

}
