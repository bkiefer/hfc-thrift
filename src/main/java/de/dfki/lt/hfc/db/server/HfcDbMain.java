package de.dfki.lt.hfc.db.server;

import java.io.IOException;
import java.util.List;

import org.apache.thrift.transport.TTransportException;

import de.dfki.lt.hfc.WrongFormatException;
import de.dfki.lt.hfc.db.remote.QueryException;
import de.dfki.lt.hfc.db.remote.QueryResult;
import de.dfki.lt.hfc.db.service.ClientAdapter;
import de.dfki.lt.hfc.db.ui.Listener;
import de.dfki.lt.hfc.db.ui.QueryWindow;
import java.awt.Color;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/** The main class to start a database server based on HFC */
public class HfcDbMain {
  protected static final int SERVER_PORT = 9090;
  protected static final int WEBSERVER_PORT = 9999;

  protected static void interactive (final HfcDbServer server) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final QueryWindow qw = new QueryWindow();
        qw.register(new Listener<String>() {
          @Override
          public void listen(String query) {
            try {
              QueryResult qr = server.selectQuery(query);
              qw.setContent(ClientAdapter.get(qr), query);
              qw._statusbar.setText(qr.table.getRowsSize()
                  + " results have been found.");
              qw._statusbar.setForeground(new Color(8, 135, 81));
            } catch (QueryException ex) {
              qw._statusbar.setText(ex.getWhy());
              qw._statusbar.setForeground(new Color(222, 41, 38));
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
      }
    });
  }


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
    OptionParser parser = new OptionParser("p:w:iW:");
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

    if (options.has("i")) interactive(server);

    if (options.has("W")) {
      server.getHandler().dump((String)options.valueOf("W"));
    }

  }
}
