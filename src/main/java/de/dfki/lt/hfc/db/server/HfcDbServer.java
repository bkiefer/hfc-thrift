package de.dfki.lt.hfc.db.server;

import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.lt.hfc.db.remote.HfcDbService;
import de.dfki.lt.hfc.db.remote.QueryResult;
import de.dfki.lt.hfc.db.rpc.RPCFactory;
import de.dfki.lt.hfc.db.service.HfcDbServiceHandler;

public class HfcDbServer {

  private static final Logger logger = LoggerFactory.getLogger(HfcDbServer.class);

  protected HfcDbServiceHandler _handler;

  private TServer _server;

  private Thread _serverThread;

  public HfcDbServer(String configPath) {
    _handler = new HfcDbServiceHandler(configPath);
  }

  protected HfcDbServer() { }

  public HfcDbServiceHandler getHandler() {
    return _handler;
  }

  public QueryResult selectQuery(String query) throws TException {
    return _handler.selectQuery(query);
  }

  protected TServer createServer(int port) {
    return RPCFactory.createSyncServer(HfcDbService.class, port, _handler);
  }

  public void runServer(int port) {
    _server = createServer(port);
    Runnable simple = new Runnable() {
      @Override
      public void run() {
        _server.serve();
        logger.info("Starting the simple server on port {} ...", port);
      }
    };
    _serverThread = new Thread(simple);
    _serverThread.setDaemon(false);
    _serverThread.start();
  }

  public void shutdown() {
    if (_server != null)
      _server.stop();
    try {
      if (_serverThread != null)
        _serverThread.join(10);
    } catch (InterruptedException e) {
    }
  }

}
