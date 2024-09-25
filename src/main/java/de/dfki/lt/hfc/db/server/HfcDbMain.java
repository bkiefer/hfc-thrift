package de.dfki.lt.hfc.db.server;

import java.io.IOException;
import java.util.List;

import org.apache.thrift.transport.TTransportException;

import de.dfki.lt.hfc.WrongFormatException;
import de.dfki.lt.hfc.db.ui.QueryWindow;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/** The main class to start a database server based on HFC */
public class HfcDbMain {
  protected static final int SERVER_PORT = 9090;
  protected static final int WEBSERVER_PORT = 9999;

  private static void usage(String msg) {
    String[] usage = {
        "Usage: HfcDbServer [-p rpcportnumber] [-w webserviceport] <configFile>"
        + " [-i (interactive mode)]" };
    System.out.println();
    System.out.println(msg);
    System.out.println();
    for (String us : usage)
      System.out.println(us);
    System.exit(1);
  }

  public static void main(String[] args)
      throws TTransportException, IOException, WrongFormatException {
    OptionParser parser = new OptionParser("p:w:iW:f:");
    OptionSet options = null;
    try {
      options = parser.parse(args);
    } catch (OptionException ex) {
      usage("Error parsing options: " + ex.getLocalizedMessage());
      System.exit(1);
    }

    @SuppressWarnings("unchecked")
    List<String> nonOptionArgs = (List<String>) options.nonOptionArguments();
    int port = SERVER_PORT;
    if (options.has("p")) {
      try {
        port = Integer.parseInt((String) options.valueOf("p"));
      } catch (NumberFormatException nex) {
        usage("Argument of -p (port for rpc) must be a number");
      }
    }
    int webserviceport = WEBSERVER_PORT;
    if (options.has("w")) {
      try {
        webserviceport = Integer.parseInt((String) options.valueOf("w"));
      } catch (NumberFormatException nex) {
        usage("Argument of -w (port for webservice) must be a number");
      }
    }
    if (nonOptionArgs.isEmpty()) {
      usage("Configuration file missing.");
    }

    final HfcDbServer server = new HfcDbServer(nonOptionArgs.get(0));
    //server.readConfig(new File(nonOptionArgs.get(0)));
    server.runServer(port);

    //final HfcDbServer publicServer = server.runHttpService(webserviceport);;
    //publicServer.readConfig(new File(nonOptionArgs.get(0)));
    //server.runHttpService(webserviceport);

    // server.runHttpService(webserviceport);

    
    if (options.has("f")) 
      QueryWindow.DEFAULT_FONT_SIZE = Integer.parseInt((String)options.valueOf("f"));

    if (options.has("i")) QueryWindow.interactive(server);

    if (options.has("W")) {
      server.getHandler().dump((String)options.valueOf("W"));
    }

  }
}
