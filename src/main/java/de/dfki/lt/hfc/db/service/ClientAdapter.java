package de.dfki.lt.hfc.db.service;

import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.lt.hfc.db.QueryResult;
import de.dfki.lt.hfc.db.StreamingClient;
import de.dfki.lt.hfc.db.Table;
import de.dfki.lt.hfc.db.rdfProxy.DbClient;
import de.dfki.lt.hfc.db.remote.HfcDbService;
import de.dfki.lt.hfc.db.remote.PropInfo;

/** Turn a client that talks over the network into a HFC-only client */
public class ClientAdapter implements DbClient {
  private static Logger logger = LoggerFactory.getLogger(ClientAdapter.class);

  private HfcDbService.Client _client;

  public ClientAdapter(HfcDbService.Client client) {
    _client = client;
  }

  public static QueryResult get(de.dfki.lt.hfc.db.remote.QueryResult q) {
    return new QueryResult(q.getVariables(), q.getTable().getRows());
  }

  public static de.dfki.lt.hfc.db.remote.Table get(Table t) {
    return new de.dfki.lt.hfc.db.remote.Table(t.getRows());
  }

  @Override
  public String getNewId(String namespace, String clazzUri) {
    try {
      return _client.getNewId(namespace, clazzUri);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public QueryResult selectQuery(String query) {
    try {
      return get(_client.selectQuery(query));
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String getValue(String uri, String predicate) {
    try {
      return _client.getValue(uri, predicate);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public int setValue(String uri, String fieldName, String value) {
    try {
      return _client.setValue(uri, fieldName, value);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Set<String> getMultiValue(String uri, String property) {
    try {
      return _client.getMultiValue(uri, property);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public int addToMultiValue(String uri, String property, String val) {
    try {
      return _client.addToMultiValue(uri, property, val);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public int removeFromMultiValue(String uri, String property, String val) {
    try {
      return _client.removeFromMultiValue(uri, property, val);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public int setMultiValue(String uri, String property, Set<String> val) {
    try {
      return _client.setMultiValue(uri, property, val);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public boolean askQuery(String query) {
    try {
      return _client.askQuery(query);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public int insert(Table createTable) {
    try {
      return _client.insert(get(createTable));
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public int insertPlain(Table createTable) {
    try {
      return _client.insertPlain(get(createTable));
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public int insert(Table createTable, long timestamp) {
    try {
      return _client.insertTimed(get(createTable), timestamp);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getClassOf(String uri) {
    try {
      return _client.getClassOf(uri);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  public boolean isSubclassOf(String sup, String sub) {
    try {
      return _client.isSubclassOf(sup, sub);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Map<String, PropInfo> getAllProps(String classuri) {
    try {
      return _client.getAllProps(classuri);
    } catch (TException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void registerStreamingClient(StreamingClient c) {
    // TODO this should register a StreamingClient that also works remotely!
    logger.error("Can not register remote StreamingClient");
  }
}
