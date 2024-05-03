package de.dfki.lt.hfc.db.client;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import de.dfki.lt.hfc.WrongFormatException;
import de.dfki.lt.hfc.db.QueryResult;
import de.dfki.lt.hfc.db.ResultSet;
import de.dfki.lt.hfc.db.Table;
import de.dfki.lt.hfc.db.TupleException;
import de.dfki.lt.hfc.db.rdfProxy.DbClient;
import de.dfki.lt.hfc.db.remote.HfcDbService;
import de.dfki.lt.hfc.db.remote.QueryException;
import de.dfki.lt.hfc.db.rpc.RPCFactory;
import de.dfki.lt.hfc.db.server.ClientAdapter;
import de.dfki.lt.hfc.db.ui.Listener;
import de.dfki.lt.hfc.db.ui.QueryWindow;
import de.dfki.lt.hfc.types.XsdAnySimpleType;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/** An example client, to demonstrate how to create your own client */
public class HfcDbClient {

  private static final int SERVER_PORT = 9090;

  private static final Logger logger = LoggerFactory.getLogger(HfcDbClient.class);

  //private Namespace _ns;
  private HfcDbService.Client remoteClient;

  public DbClient _client;

  public HfcDbClient() {
    //_ns = new Namespace();
  }

  public void init(String host, int port) throws TTransportException {
    //_client = RPCFactory.createSyncDiscoverClient(
    //    HfcDbService.Client.class, "HfcDb");
    _client = new ClientAdapter(remoteClient = RPCFactory.createSyncClient(
        HfcDbService.Client.class, host, port));
  }

  public void shutdown() {
    // TODO how?
  }

  public QueryResult query(String query) throws TException {
    return _client.selectQuery(query);
  }

  public void initServer(String config) throws TException {
    remoteClient.init(config);
  }

  /* If that should make sense, the namespace has to be promoted to the
   * server
   *
  public void addNamespace(String shortForm, String longForm) {
    _ns.putForm(shortForm, longForm);
  }

  public void loadNamespaces(File namespaceFile)
      throws WrongFormatException, IOException {
    _ns.readNamespaces(Files.newBufferedReader(
        namespaceFile.toPath(), Charset.forName("utf-8")));
  }

  /*
  private static final Pattern tokens = Pattern.compile(
      "<[^>]*>|_\\S*|\"(?:[^\\\"]|\\\\.)*\"(?:\\^\\^<[^>]*>|@[a-z]*)");

  public static List<String> splitRow0(String line) {
    Matcher m = tokens.matcher(line);
    List<String> newRow = new ArrayList<String>();
    while (m.find()) {
      newRow.add(m.group());
    }
    return newRow;
  }
  */

  public static List<String> splitRow(String line) {
    int state = 0;
    int pos = 0;
    List<String> result = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    while (pos < line.length() && state != 8) {
      char c = line.charAt(pos);
      switch (state) {
      case 0:
        if (! Character.isSpaceChar(c)) {
          // token start, must be one of < _ "
          switch (c) {
          case '<': sb.append(c); state = 1; break;
          case '_': sb.append(c); state = 2; break;
          case '"': sb.append(c); state = 3; break;
          case '.': state = 8; break; // end processing
          default:
            logger.error("Wrong token in line: {} at pos {}", line, pos);
            return result;
          }
        }
        break;
      case 1:  // URI
        if (Character.isSpaceChar(c)) {
          logger.error("Wrong token in line: {} at pos {}", line, pos);
          return result;
        }
        sb.append(c);
        if (c == '>') {
          result.add(sb.toString()); sb = new StringBuilder(); state = 0;
          break;
        }
        break;
      case 2: // blank node
        if (Character.isSpaceChar(c)) {
          result.add(sb.toString()); sb = new StringBuilder(); state = 0;
        } else {
          sb.append(c);
        }
        break;
      case 3: // String, or XSD atom, part 1
        switch (c) {
        case '\\': sb.append(c); sb.append(line.charAt(++pos)); break;
        case '"': sb.append(c); state = 4; break;
        default: sb.append(c); break;
        }
        break;
      case 4: // String, or XSD atom, part 2
        switch (c) {
        case '^': sb.append(c); state = 5; break;
        case '@': sb.append(c); state = 6; break;
        default:
          logger.error("Wrong token in line: {} at pos {}", line, pos);
          return result;
        }
        break;
      case 5: // xsd atom, next ^ required
        if (c == '^') {
          sb.append(c); state = 7;
        } else {
          logger.error("Wrong token in line: {} at pos {}", line, pos);
          return result;
        }
        break;
      case 6: // language tag: [a-z]+
        if (c >= 'a' && c <= 'z') {
          sb.append(c);
        } else {
          result.add(sb.toString()); sb = new StringBuilder(); state = 0;
        }
        break;
      case 7: // xsd atom, URI required
        if (c != '<') {
          logger.error("Wrong token in line: {} at pos {}", line, pos);
          return result;
        }
        sb.append(c);
        state = 1;
        break;
      }
      ++pos;
    }
    if (sb.length() > 0) {
      result.add(sb.toString());
    }
    return result;
  }



  public int loadTuples(File tripleFile) throws IOException, TException {
    BufferedReader br = Files.newBufferedReader(
        tripleFile.toPath(), Charset.forName("utf-8"));
    return loadTuples(br);
  }

  private Table createTable(BufferedReader br) throws IOException {
    Table t = new Table();
    String line;
    while ((line = br.readLine()) != null) {
      List<String> newRow = splitRow(line);
      if (! newRow.isEmpty()) {
        t.addToRows(newRow);
      }
    }
    return t;
  }

  private int loadTuples(BufferedReader br) throws IOException, TException {
    int added = _client.insert(createTable(br));
    logger.info(added + " tuple(s) added");
    return added;
  }

  public int loadTuplesPlain(BufferedReader br) throws IOException, TException {
    int added = _client.insertPlain(createTable(br));
    logger.info(added + " tuple(s) added");
    return added;
  }



  /** TODO: HOW TO BUILD A COMPLEX NEW OBJECT, AND COMMIT IT IN ONE GO?
   *        DO WE REALLY NEED THIS FUNCTIONALITY?
   */
  public void addNewObject(){};

  /** Get all the latest values given entity uri and a predicate pred
   */
  private List<String> getAllValues(String uri, String pred)
        throws QueryException, TException {
    QueryResult r =
        _client.selectQuery("select ?o ?ts where "+ uri + " " + pred + " ?o ?ts "
            + "AGGREGATE ?val = LGetLatest2 ?o ?ts \"1\"^^<xsd:int>");
    return r.getTable().projectColumn(0);
  }

  /** returns a structure like the one you send me last time, with all available
   *  data
   * @throws TException
   * @throws QueryException
   * @throws WrongFormatException
   */
  private Map<String, Object> getRecursive(String uri,
      Map<String, Map<String, Object>> visited)
      throws QueryException, TException, WrongFormatException {
    if (visited.containsKey(uri))
        return visited.get(uri); // avoid cyclic calls

    Map<String, Object> result = new HashMap<String, Object>();
    result.put("id", uri);
    visited.put(uri, result);
    QueryResult r = _client.selectQuery("select ?p where "+ uri + " ?p ?o ?ts ");

    for(List<String> row : r.table.getRows()) {
      String pred = row.get(0);
      for(String value : getAllValues(uri, pred)) {
        Object o = null;
        switch (value.charAt(0)) {
        case '<': // URI
        case '_': // blank node
          o = getRecursive(value, visited);
          break;
        case '"': // simple type
          o = XsdAnySimpleType.getXsdObject(value).toJava();
          break;
        default:
          // Error: don't know what this is.
          break;
        }
        result.put(pred, o);
      }
    }
    return result;
  }

  /** returns for, e.g., a child id the complex embedded object containing
   *  structured info like preferences, or education
   *  (where it might return a list of IDs)
   */
  public Map<String, Object> getAllData(String uri)
      throws QueryException, TException, WrongFormatException {
    return getRecursive(uri, new HashMap<String, Map<String, Object>>());
  }


  /** returns a new (unique) id (URI) for an object of type type
   * @throws TException
   */
  public String getNewId(String namespace, String type) throws TupleException {
    return _client.getNewId(namespace, type);
  }

  /** For a uri which represents a doctor, either returns a list of objects or IDs
   * @throws TException
   *
   * TODO: currently will not work as expected because (missing) <dom:treats>
   * is not a functional property, and adding new children requires re-adding
   * all current children
   */
  public Set<String> getTreatedChildren(String uri) throws QueryException, TupleException {
    return _client.getMultiValue(uri, "<dom:treats>");
  }

  /** Add new child id to given doctor
   * @throws TException
   */
  public int treatsNewChild(String doctorUri, String childUri) throws QueryException, TupleException {
    return _client.addToMultiValue(doctorUri, "<dom:treats>", childUri);
  }

  /** TODO: CANDIDATE FOR A CONVENIENCE LIBRARY FUNCTION */
  public List<String> getAll(String predicate, String value)
      throws QueryException, TException {
    QueryResult r =
        _client.selectQuery("select ?s ?ts where ?s "
            + predicate + " " + value + " ?ts "
            + "AGGREGATE ?ss = LGetLatest2 ?s ?ts \"1\"^^<xsd:int>");
    if (r.getTable().getRows().isEmpty())
      return Collections.emptyList();
    return r.getTable().getRows().get(0);
  }


  public List<String> getChildren() throws QueryException, TException {
    return getEvery("<rdf:type>", "<dom:Child>");
  }

  public List<String> getAllProfessionals() throws QueryException, TException {
    return getEvery("<rdf:type>", "<dom:Professional>");
  }

  public String createNewProessional() throws TException {
    return _client.getNewId("rifca:", "<dom:Professional>");
  }

  public String createNewChild() throws TException {
    return _client.getNewId("rifca:", "<dom:Child>");
  }

  /** TODO: CANDIDATE FOR A CONVENIENCE LIBRARY FUNCTION */
  public List<String> getEvery(String predicate, String value) throws TException {
    QueryResult r = _client.selectQuery("select ?s where ?s "
        + predicate + " " + value + " ?ts");
    return r.getTable().projectColumn(0);
  }


 /* ======================================================================
   * Initialisation: reading config, etc.
   *
   * Just some example main function. In principle, the QueryWindow could
   * be used.
   * ====================================================================== */

  public static File resolvePath(File baseFile, String dependentFileName) {
    File dependentFile = new File(dependentFileName);
    if (! dependentFile.isAbsolute()) {
      dependentFile = new File(baseFile.getParentFile(), dependentFileName);
    }
    return dependentFile;
  }

  @SuppressWarnings("unchecked")
  public void readConfig(File configFile) throws IOException, TException {
    Yaml yaml = new Yaml();
    Map<String, Object> confs = yaml.load(new FileInputStream(configFile));
    /*
    Map<String, String> namespaces = ((Map<String, String>)confs.get("namespaces"));
    if (namespaces != null) {
      for (Map.Entry<String, String> pair : namespaces.entrySet()) {
        _client.addNamespace(pair.getKey(), pair.getValue());
      }
    }
    */
    List<String> tuples = (List<String>)confs.get("tupleFiles");
    if (tuples != null) {
      for (String fileName : tuples) {
        loadTuples(resolvePath(configFile, fileName));
      }
    }
  }

  private static void usage(String msg) {
    String[] usage = { "Usage: HfcDbClient [-h[ost] host]"
        + " [-[p]<ort> portnumber] [-i<nteractive>] <configFile>" };
    System.out.println();
    System.out.println(msg);
    System.out.println();
    for (String us : usage)
      System.out.println(us);
    System.exit(1);
  }

  public static void printResult(HfcDbClient client, String query, Formatter c)
      throws QueryException, TException{
    if (query.startsWith("addplain")) {
      try {
        client.loadTuplesPlain(
            new BufferedReader(new StringReader(query.substring(9).trim())));
      } catch (IOException ex) {
        throw new RuntimeException(ex); // will never happen
      }
    } else if (query.startsWith("add")) {
      try {
        client.loadTuples(
                new BufferedReader(new StringReader(query.substring(4).trim())));
      } catch (IOException ex) {
        throw new RuntimeException(ex); // will never happen
      }
    } else {
      QueryResult q = client.query(query);
      for (String v : q.variables) { c.format(" %s ", v); }
      c.format("\n========================================\n");
      for (List<Object> row : new ResultSet(q.table)) {
        for (Object val : row) { c.format(" %s ", val); }
        c.format("\n");
      }
    }
  }

  private static ArrayList<String> indents = new ArrayList<String>();
  private static String getIndent(int i) {
    while (i >= indents.size()) {
      String s = "";
      for (int j = 0 ; j < i; ++j) { s += "  "; }
      indents.add(s);
    }
    return indents.get(i);
  }


  private static void printMapRec(Map<String, Object> input, Set<String> visited, int indent) {
    String uri = (String)input.get("id");
    System.out.println(getIndent(indent) + uri + ": ");
    indent += 1;
    if (visited.contains(uri)) {
      System.out.println(getIndent(indent) + uri);
    } else {
      visited.add(uri);
      for (Map.Entry<String, Object> e : input.entrySet()) {
        if (e.getKey().equals("id")) continue;
        if (e.getValue() instanceof Map) {
          System.out.println(getIndent(indent) + e.getKey() + ": ");
          printMapRec((Map<String,Object>) e.getValue(), visited, indent + 1);
        } else {
          System.out.println(
              getIndent(indent) + e.getKey() + ": " + e.getValue());
        }
      }
    }
  }

  public static void printMap(Map<String, Object> input) {
    printMapRec(input, new HashSet<String>(), 0);
  }


  private static void interactive (final HfcDbClient client) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final QueryWindow qw = new QueryWindow();
        qw.register(new Listener<String>() {
          @Override
          public void listen(String query) {
            try {
              // put input in history
              qw.addToHistory(query);
              QueryResult qr = client.query(query);
              qw.setContent(qr, query);
              qw._statusbar.setText(qr.table.getRowsSize() + " results have been found.");
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

  static String line = "\"<p style=\\\"margin-top: 0\\\">\n      this subclass of DialogueAct is intended to cross-classify dialogue acts \n      which are subclasses of GeneralPurposeFunction; the idea behind this is \n      to address uncertainty in the recognition of &quot;related&quot; dialogue acts \n      through a _single_ class;\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      currently, we suggest to have three subclasses of AggregateFunction for \n      which I list their subclasses below:\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      * Accept &gt; \n      {AcceptOffer,AcceptSuggestion,AcceptRequest,Confirm,CheckPositive,Agreement}\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      * Decline &gt; \n      {DeclineOffer,DeclineSuggestion,DeclineRequest,Disconfirm,CheckNegative,Disagreement}\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      * Revoke &gt; {Revocation,Correction,RevokeSuggestion}\n    </p>\"^^<http://www.w3.org/2001/XMLSchema#string>";
  static String line0 = "<http://www.dfki.de/lt/onto/common/dialogue.owl#AggregateFunction> <http://www.w3.org/2000/01/rdf-schema#comment> \"<p style=\\\"margin-top: 0\\\">\n      this subclass of DialogueAct is intended to cross-classify dialogue acts \n      which are subclasses of GeneralPurposeFunction; the idea behind this is \n      to address uncertainty in the recognition of &quot;related&quot; dialogue acts \n      through a _single_ class;\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      currently, we suggest to have three subclasses of AggregateFunction for \n      which I list their subclasses below:\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      * Accept &gt; \n      {AcceptOffer,AcceptSuggestion,AcceptRequest,Confirm,CheckPositive,Agreement}\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      * Decline &gt; \n      {DeclineOffer,DeclineSuggestion,DeclineRequest,Disconfirm,CheckNegative,Disagreement}\n    </p>\n    <p style=\\\"margin-top: 0\\\">\n      * Revoke &gt; {Revocation,Correction,RevokeSuggestion}\n    </p>\"^^<http://www.w3.org/2001/XMLSchema#string>";

  @SuppressWarnings("unchecked")
  public static void main(String[] args)
	    throws IOException, WrongFormatException, TException {
    OptionParser parser = new OptionParser("h:p:itcs");
    OptionSet options = null;
    String host = "localhost";
    int port = SERVER_PORT;

    try {
      options = parser.parse(args);
      if (options.has("p")) {
        port = Integer.parseInt((String) options.valueOf("p"));
      }
      if (options.has("h")) {
        host = (String) options.valueOf("h");
      }
    } catch (OptionException ex) {
      usage("Error parsing options: " + ex.getLocalizedMessage());
    } catch (NumberFormatException nex) {
      usage("Argument of -p (port) must be a number");
    }

    HfcDbClient client = new HfcDbClient();
    client.init(host, port);

    List<String> nonOptionArgs = (List<String>) options.nonOptionArguments();

	  if (! nonOptionArgs.isEmpty()) {
	    client.readConfig(new File(nonOptionArgs.get(0)));
	  }

	  Formatter f = new Formatter(System.out);
	  if (options.has("c")) {
	    Terminal terminal = TerminalBuilder.terminal();
	    LineReader r = LineReaderBuilder.builder().terminal(terminal).build();
	    String line = null, query = "";
	    while ((line = r.readLine()) != null) {
	      line = line.trim();
	      if (line.endsWith("\\")) {
	        query += line.substring(0, line.length() - 1) + " ";
	      } else {
	        query += line;
	        try {
	          printResult(client, query, f);
	        } catch (QueryException ex) {
	          System.out.println(ex.getMessage());
	        }
	        query = "";
	      }
	    }
	  } else if (options.has("i")) {
	    interactive(client);
    } else if (options.has("t")) {
      List<String> l = client.getChildren();
      for (String s : l) {
        System.out.println(s);
      }
    } else if (options.has("s")) {
      Random rnd = new Random(System.currentTimeMillis());
      while (true) {
        try {
          String uri = client.getNewId("pal:", "<dom:Closing>");
          System.err.println(uri);
          Thread.sleep(100 + rnd.nextInt(100));
          String type = client.getValue(uri, "<rdf:type>");
          System.err.println(type);
          Thread.sleep(100 + rnd.nextInt(100));
        }
        catch (InterruptedException ex) {
          break;
        }
        catch (Throwable t) {
          t.printStackTrace();
          break;
        }
      }
    } else {
	    /*
	    String[] queries = {
	        "select distinct ?c ?t where ?c <rdf:type> ?t",
	        "select distinct ?c where ?c <rdf:type> <dom:Child>",
	        "select distinct ?c ?f ?s where ?c <rdf:type> <dom:Child>"
	            + " & ?c <dom:forename> ?f & ?c <dom:surname> ?s",
	            "select ?s where ?s <owl:equivalentClass> <dom:Child>"
	    };

	    for (String query : queries) {
	      BufferedReader b = new BufferedReader(new StringReader(query));
	      String s = null;
	      while ((s = b.readLine()) != null) {
	        printResult(client, s, f);
	      }
	    }*/

	    List<String> children = client.getChildren();
	    for (String childUri : children) {
	      printMap(client.getAllData(childUri));
	      /*
	      System.out.println(childUri);
	    	List<String> names = client.getAllValues(childUri, "<dom:forename>");
	    	for (String name : names) {
	    	  System.out.println(childUri +": " + name);
	    	}
	    	*/
	    }
	    /* Does not work because it's in the initial database: no time stamps
      Set<String> comments = client._client.getMultiValue(
          "<goal:A2kFoodAndActivity>",
          "<rdfs:comment>");
      for(String s : comments) {System.out.println(s);}
	    */
	  }
	  f.close();
	}

	/*
	 * Returns a value of a instantiation. For example:
	 * getValue("<dom:child_0>", "<dom:forename>")
	 */
	public String getValue(String instantiation, String dataProperty) throws TException {
		return _client.getValue(instantiation, dataProperty);
	}

	public HfcDbService.Client getServiceClient() {
		return remoteClient;
	}
}
