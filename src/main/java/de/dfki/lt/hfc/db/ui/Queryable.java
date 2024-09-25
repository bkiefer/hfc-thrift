package de.dfki.lt.hfc.db.ui;

import org.apache.thrift.TException;

import de.dfki.lt.hfc.db.QueryResult;

public interface Queryable {
  QueryResult query(String s) throws TException;
}
