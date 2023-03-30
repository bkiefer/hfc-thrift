package de.dfki.lt.hfc.db.server;

import org.apache.thrift.transport.TTransportException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.dfki.lt.hfc.db.remote.HfcDbService;
import de.dfki.lt.hfc.db.rpc.RPCFactory;
import de.dfki.lt.hfc.db.server.HfcDbServer;

public class TestAccessControl {

  private static HfcDbServer server;

  private static HfcDbService.Client client;

  private static final String TOKEN = "ThisIsMyToken";

  private static final String CHILD1 = "<rifca:Child_0>";
  private static final String CHILD2 = "<rifca:Child_8>";

  private static final String PROF1 = "<rifca:Professional9a9c3ce8_2>";
  private static final String ADMIN1 = "<rifca:administrator_0>";

  private static final String PARENT1 = "<rifca:Professional_xxx>";
  private static final String PARENT2 = "<rifca:Professional_xxx>";

  @BeforeClass
  public static void startServer() throws TTransportException {
    server = TestUtils.startServer(TestUtils.SERVER_PORT);

    client = RPCFactory.createSyncClient(HfcDbService.Client.class,
        "localhost", TestUtils.SERVER_PORT);
  }

  @AfterClass
  public static void shutdownServer() {
    server.shutdown();
  }


}
