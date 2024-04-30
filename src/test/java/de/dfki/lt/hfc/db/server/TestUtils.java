package de.dfki.lt.hfc.db.server;

import java.io.File;

import de.dfki.lt.hfc.db.HfcDbHandler;
import de.dfki.lt.hfc.db.client.HfcDbClient;
import de.dfki.lt.hfc.db.rdfProxy.RdfProxy;
import de.dfki.lt.hfc.db.remote.HfcDbService;
import de.dfki.lt.hfc.db.remote.HfcDbService.Client;
import de.dfki.lt.hfc.db.rpc.RPCFactory;

public class TestUtils {

  public static final String RESOURCE_DIR = "src/test/data/";

  public static final int SERVER_PORT = 8994;

  public static HfcDbServer startServer(int port) {
    return startServer(port, RESOURCE_DIR + "test.yml", RESOURCE_DIR + "rifca.yml");
  }

  public static HfcDbServer startServer(int port, String ontoConf, String dataConf) {
    try {
      HfcDbServer server = new HfcDbServer(ontoConf);
      server.runServer(port);
      // this client is used only to inject user data, not for the tests!
      HfcDbClient client = new HfcDbClient();
      client.init("localhost", port);
      if (dataConf != null)
        client.readConfig(new File(dataConf));
      return server;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void shutdownServer(HfcDbServer server) {
    server.shutdown();
  }

  public static RdfProxy setupProxy() {
    try {
      Client client = RPCFactory.createSyncClient(HfcDbService.Client.class,
          "localhost", SERVER_PORT);
      RdfProxy _proxy = new RdfProxy(new ClientAdapter(client));
      return _proxy;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static HfcDbService.Client setupPrivateClient() {
    return RPCFactory.createSyncClient(HfcDbService.Client.class, "localhost", SERVER_PORT);
  }

  public static HfcDbHandler setupLocalHandler() {
    return new HfcDbHandler(RESOURCE_DIR + "test.yml");
  }

}
