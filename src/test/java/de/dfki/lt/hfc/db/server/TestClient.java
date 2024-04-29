package de.dfki.lt.hfc.db.server;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.dfki.lt.hfc.db.QueryException;
import de.dfki.lt.hfc.db.QueryResult;
import de.dfki.lt.hfc.db.Table;
import de.dfki.lt.hfc.db.TupleException;
import de.dfki.lt.hfc.db.client.HfcDbClient;
import de.dfki.lt.hfc.db.remote.HfcDbService;

public class TestClient {
  private static HfcDbServer server;

  private static HfcDbClient client;

  @BeforeClass
  public static void startServer() throws TTransportException {
    server = TestUtils.startServer(TestUtils.SERVER_PORT);

    client = new HfcDbClient();
    client.init("localhost", TestUtils.SERVER_PORT);
  }

  @AfterClass
  public static void shutdownServer() {
    server.shutdown();
  }

  @Test
  public void testInsert() throws TupleException, TException, InterruptedException {
    Table t = new Table();
    String[] row = {"<dom:child_2>", "<rdf:type>", "<dom:Child>"};
    t.addToRows(Arrays.asList(row));
    int added = client._client.insert(t);
    assertEquals(1, added);
    Thread.sleep(10);
    added = client._client.insert(t);
    assertEquals(1, added);
    assertEquals(2,
        client.query("select ?p ?o where <dom:child_2> ?p ?o ?time ")
        .getTable().getRowsSize());

  }

  @Test
  public void testGetMultiValue() throws TupleException, QueryException, TException
       {
    Set<String> result =
        client._client.getMultiValue("<rifca:Child_0>", "<edu:hasAccessTo>");
    assertEquals(2, result.size());
  }

  @Test
  public void testAddMultiValue() throws TupleException, TException {
    client._client.addToMultiValue("<dom:professional_1>",
        "<dom:treats>" , "<dom:child_0>");
    Set<String> result =
        client._client.getMultiValue("<dom:professional_1>", "<dom:treats>");
    assertEquals(1, result.size());
    client._client.addToMultiValue("<dom:professional_1>",
        "<dom:treats>" , "<dom:child_1>");
    result =
        client._client.getMultiValue("<dom:professional_1>", "<dom:treats>");
    assertEquals(2, result.size());
    client._client.addToMultiValue("<dom:professional_1>",
        "<dom:treats>" , "<dom:child_1>");
    result =
        client._client.getMultiValue("<dom:professional_1>", "<dom:treats>");
    assertEquals(2, result.size());
  }

  @Test
  public void testRemoveMultiValue() throws TupleException, TException {
    client._client.addToMultiValue("<dom:professional_2>",
        "<dom:treats>" , "<dom:child_0>");
    Set<String> result =
        client._client.getMultiValue("<dom:professional_2>", "<dom:treats>");
    assertEquals(1, result.size());
    client._client.addToMultiValue("<dom:professional_2>",
        "<dom:treats>" , "<dom:child_1>");
    result =
        client._client.getMultiValue("<dom:professional_2>", "<dom:treats>");
    assertEquals(2, result.size());
    client._client.addToMultiValue("<dom:professional_2>",
        "<dom:treats>" , "<dom:child_1>");
    result =
        client._client.getMultiValue("<dom:professional_2>", "<dom:treats>");
    assertEquals(2, result.size());
    client._client.removeFromMultiValue("<dom:professional_2>",
        "<dom:treats>" , "<dom:child_0>");
    result =
        client._client.getMultiValue("<dom:professional_2>", "<dom:treats>");
    assertEquals(1, result.size());
  }

  @Test
  public void testRemoveMultiValue2() throws TupleException, TException, InterruptedException {
    client._client.addToMultiValue("<dom:professional_3>",
        "<dom:treats>" , "<dom:child_0>");
    Set<String> result =
        client._client.getMultiValue("<dom:professional_3>", "<dom:treats>");
    assertEquals(1, result.size());
    Thread.sleep(10);
    client._client.removeFromMultiValue("<dom:professional_3>",
        "<dom:treats>" , "<dom:child_0>");
    result =
        client._client.getMultiValue("<dom:professional_3>", "<dom:treats>");
    assertEquals(0, result.size());
  }

  @Test
  public void testGetNewId() throws TException {
    String uri = client.getNewId("dom:", "<dom:Child>");
    assertNotNull(uri);
    QueryResult result =
        client.query("select ?t where " + uri + " <rdf:type> ?t ?time ");
    assertEquals(1, result.getTable().getRowsSize());
    assertTrue(result.getTable().getRows().get(0).get(0).endsWith(":Child>"));
  }

  @Test
  public void testGetValue() throws TException {
    assertEquals("\"Sem\"^^<xsd:string>", client._client.getValue(
        "<rifca:Child_8>", "<dom:forename>"));
  }

  @Test
  public void testSetValue() throws TException {
    assertEquals("\"Henk\"^^<xsd:string>", client._client.getValue(
        "<rifca:Child_0>", "<dom:forename>"));
    assertEquals(1, client._client.setValue("<rifca:Child_0>", "<dom:forename>",
        "\"John\"^^<xsd:string>"));
    assertEquals("\"John\"^^<xsd:string>", client._client.getValue(
        "<rifca:Child_0>", "<dom:forename>"));
  }

  /**
   * Test of getRdf method, of class RdfProxy.
   * @throws TException
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetRdf() throws TException {
    String child = "<rifca:Child_0>";
    String clazzuri = ((ClientAdapter)client._client).getClassOf(child);
    assertEquals("<dom:Child>", clazzuri);
  }

  /*
  @Test
  public void testInsertQuestionCounts()
      throws TException, InterruptedException, WrongFormatException {
    HashMap<String, Integer> goalToNr = new HashMap<>();
    goalToNr.put("G4", 7);
    goalToNr.put("D10", 5);

    QueryResult qr =
        client._client.selectQuery("select ?goalinstance ?nr where "
            + "?goalinstance <rdf:type> <goal:AnswerG4> ?t "
            + "& ?goalinstance <goal:numOfQuestions> ?nr ?t1");
    assertEquals(0, qr.table.getRowsSize());
    client._client.setAllQuestionCounts(goalToNr);
    qr =
        client._client.selectQuery("select distinct ?goalinstance ?nr where "
            + "?goalinstance <rdf:type> <goal:AnswerG4> ?t "
            + "& ?goalinstance <goal:numOfQuestions> ?nr ?t1");
    assertEquals(1, qr.table.getRowsSize());
    String s = qr.table.getRows().get(0).get(1);
    Object o = XsdAnySimpleType.getXsdObject(s).toJava();
    assertEquals(7, o);

    qr = client._client.selectQuery("select distinct ?goalinstance ?nr where "
        + "?goalinstance <rdf:type> <goal:AnswerD10> ?t "
        + " & ?goalinstance <goal:numOfQuestions> ?nr ?t1");
    assertEquals(1, qr.table.getRowsSize());
    s = qr.table.getRows().get(0).get(1);
    o = XsdAnySimpleType.getXsdObject(s).toJava();
    assertEquals(5, o);

    goalToNr.put("D10", 7);
    client._client.setAllQuestionCounts(goalToNr);

    qr = client._client.selectQuery("select distinct ?goalinstance ?nr where "
        + "?goalinstance <rdf:type> <goal:AnswerG4> ?t "
        + " & ?goalinstance <goal:numOfQuestions> ?nr ?t1");
    assertEquals(1, qr.table.getRowsSize());
    s = qr.table.getRows().get(0).get(1);
    o = XsdAnySimpleType.getXsdObject(s).toJava();
    assertEquals(7, o);

    qr = client._client.selectQuery("select distinct ?goalinstance ?nr ?t1 where "
        + "?goalinstance <rdf:type> <goal:AnswerD10> ?t "
        + " & ?goalinstance <goal:numOfQuestions> ?nr ?t1");
    assertEquals(2, qr.table.getRowsSize());
    qr.table.rows.sort(new Comparator<List<String>>() {
      @Override
      public int compare(List<String> o1, List<String> o2) {
        try {
          long t1 = (Long)XsdAnySimpleType.getXsdObject(o1.get(2)).toJava();
          long t2 = (Long)XsdAnySimpleType.getXsdObject(o2.get(2)).toJava();
          return (int)(t1 - t2);
        } catch (WrongFormatException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    s = qr.table.getRows().get(0).get(1);
    o = XsdAnySimpleType.getXsdObject(s).toJava();
    assertEquals(5, o);
    s = qr.table.getRows().get(1).get(1);
    o = XsdAnySimpleType.getXsdObject(s).toJava();
    assertEquals(7, o);
  }
*/
}
