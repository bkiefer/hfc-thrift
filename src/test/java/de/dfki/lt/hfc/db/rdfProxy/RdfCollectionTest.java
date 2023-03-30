package de.dfki.lt.hfc.db.rdfProxy;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import static org.junit.Assert.*;

import java.util.List;

import org.junit.*;

import de.dfki.lt.hfc.db.QueryResult;
import de.dfki.lt.hfc.db.server.HfcDbServer;
import de.dfki.lt.hfc.db.server.TestUtils;

public class RdfCollectionTest {

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

  /** test collection retrieval */
  @Test
  public void testGetCollection() {
    QueryResult r = _proxy.selectQuery(
        "select ?u where ?c <rdf:type> <owl:Class> ?_ & ?c <owl:unionOf> ?u ?_");
    assertFalse(r.getTable().getRows().isEmpty());
    // TODO: ACTIVATE WHEN FUNCTION IMPLEMENTED
    //RdfList l = _proxy.getCollection(r.table.getRows().get(0).get(0));
    //assertEquals(2, l.length());
  }
}
