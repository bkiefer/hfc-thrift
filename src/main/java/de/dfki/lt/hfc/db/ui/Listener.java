package de.dfki.lt.hfc.db.ui;

public interface Listener<T> {
  public void listen(T q);
}