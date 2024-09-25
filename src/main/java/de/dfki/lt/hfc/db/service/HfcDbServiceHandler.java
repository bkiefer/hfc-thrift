package de.dfki.lt.hfc.db.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.lt.hfc.db.HfcDbHandler;
import de.dfki.lt.hfc.db.StreamingClient;
import de.dfki.lt.hfc.db.rdfProxy.RdfClass;
import de.dfki.lt.hfc.db.rdfProxy.RdfProxy;
import de.dfki.lt.hfc.db.remote.HfcDbService;
import de.dfki.lt.hfc.db.remote.PropInfo;
import de.dfki.lt.hfc.db.remote.QueryException;
import de.dfki.lt.hfc.db.remote.QueryResult;
import de.dfki.lt.hfc.db.remote.Table;

/** SERVER-SIDE IMPLEMENTATION OF THE THRIFT API */
public class HfcDbServiceHandler implements HfcDbService.Iface {
  protected HfcDbHandler _h;
  protected RdfProxy _proxy;

  public HfcDbServiceHandler(String configPath) {
    init(configPath);
  }

  protected static Logger logger = LoggerFactory.getLogger(HfcDbServiceHandler.class);

  public static String toJSON(de.dfki.lt.hfc.db.QueryResult qa) {
    try {
      TMemoryBuffer transport = new TMemoryBuffer(100);
      TProtocol prot = new TSimpleJSONProtocol(transport);
      // new TJSONProtocol(transport);
      Table t = new Table(qa.getTable().getRows());
      QueryResult q = new QueryResult(qa.getVariables(), t);
      q.write(prot);
      return transport.toString(Charset.forName("UTF-8"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public HfcDbHandler getCore() {
    return _h;
  }

  /** Return a JSON representation of the query result */
  @Override
  public String query(String query) throws QueryException {
    String s = toJSON(_h.selectQuery(query));
    //s = s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    return s;
  }

  protected static Table get(de.dfki.lt.hfc.db.Table t) {
    return new Table(t.getRows());
  }

  protected static de.dfki.lt.hfc.db.Table get(Table t) {
    return new de.dfki.lt.hfc.db.Table(t.getRows());
  }

  protected static QueryResult get(de.dfki.lt.hfc.db.QueryResult q) {
    return new QueryResult(q.getVariables(), get(q.getTable()));
  }

  public void registerStreamingClient(StreamingClient client) {
    _h.registerStreamingClient(client);
  }

  public void dump(String name) {
    _h.dump(name);
  }

  @Override
  public void init(String configPath) {
    if (_h != null) {
      _h.shutdownNoExit();
    }
    _h = new HfcDbHandler(configPath);
    _proxy = new RdfProxy(_h);
  }

  @Override
  public int removeFromMultiValue(String uri, String property, String childUri) {
    return _h.removeFromMultiValue(uri, property, childUri);
  }


  @Override
  public int ping() throws TException {
    return _h.ping();
  }

  @Override
  public boolean addNamespace(String shortForm, String longForm) {
    return _h.addNamespace(shortForm, longForm);
  }


  /** Get a table via trift and call the core insertPlain method */
  @Override
  public int insertPlain(Table t) {
    return _h.insertPlain(get(t));
  }

  /** Call the core selectQuery method and return a thrift representation of
   *  the result
   * @throws QueryException 
   */
  @Override
  public QueryResult selectQuery(String query) throws QueryException {
    try {
      return get(_h.selectQuery(query));
    } catch (de.dfki.lt.hfc.db.QueryException qex) {
      throw new QueryException(qex.getMessage());
    }
  }


  @Override
  public boolean askQuery(String query) {
    return _h.askQuery(query);
  }

  /** Get a table via trift and call the core insert method */
  @Override
  public int insert(Table t) {
    return _h.insert(get(t));
  }


  @Override
  public Set<String> getMultiValue(String uri, String property) {
    return _h.getMultiValue(uri, property);
  }


  @Override
  public int addToMultiValue(String uri, String property, String value) {
    return _h.addToMultiValue(uri, property, value);
  }


  @Override
  public int setMultiValue(String uri, String property, Set<String> value) {
    return _h.setMultiValue(uri, property, value);
  }


  @Override
  public String getNewId(String nameSpace, String type) {
    return _h.getNewId(nameSpace, type);
  }


  @Override
  public int setValue(String uri, String predicate, String value) {
    return _h.setValue(uri, predicate, value);
  }


  @Override
  public String getValue(String uri, String predicate) {
    return _h.getValue(uri, predicate);
  }

  @Override
  public int insertTimed(Table t, long timestamp) {
    return _h.insert(get(t), timestamp);
  }

  @Override
  public Map<String, PropInfo> getAllProps(String classuri) {
    RdfClass clazz = _proxy.getClass(classuri);
    Map<String, PropInfo> result = new HashMap<>();
    for (String prop : clazz.getProperties()) {
      int type = clazz.getPropertyType(prop);
      List<String> ranges = new ArrayList<>(clazz.getPropertyRange(prop));
      result.put(prop, new PropInfo(type, ranges));
    }
    return result;
  }

  @Override
  public String getClassOf(String uri) {
    return _proxy.getMostSpecificClass(uri).toString();
  }

  @Override
  public void shutdown() throws TException {
    _h.shutdown();
  }
}