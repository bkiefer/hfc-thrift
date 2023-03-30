/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.lt.hfc.db.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Christophe Biwer, christophe.biwer@dfki.de
 */
public class RecentDialog extends JPanel {

  public static int MAX_HISTORY_LENGTH = 40;

  private Boolean _visible;
  private final JTable _table;
  private final Frame _owner;
  private final Listener<String> _listener;
  private final ActionListener _exe;

  public static Yaml yaml = new Yaml();

  public LinkedList<String> history;

  public RecentDialog(Frame owner, Listener<String> listener,
      ActionListener exe) {
    _listener = listener;
    _owner = owner;
    _exe = exe;
    this.setPreferredSize(new Dimension(300, 120));

    String homepath = System.getProperty("user.home");
    File file = new File(homepath + "/.hfc-history.yml");
    if (file.exists()) {
      try {
        ArrayList<String> temp = (ArrayList<String>)
            yaml.load(new FileInputStream(file));
        history = new LinkedList<>(temp);
      } catch (FileNotFoundException e) {
        // impossible to occur.
      }
    } else {
      System.out.println("No history found, creating a new one.");
      history = new LinkedList<>();
    }

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    JScrollPane jsp = new JScrollPane(_table = makeTable());
    _table.setTableHeader(null);
    this.add(jsp, BorderLayout.CENTER);

    this.add(jsp);
    setVisible(false);
    _visible = false;
  }


  public void changeVisibility() {
    if (_visible) {
      _visible = false;
      _owner.setSize(_owner.getWidth(), _owner.getHeight()-120);
      setVisible(false);
    }
    else {
      _visible = true;
      TableModel atm = _table.getModel();
      if (atm.getRowCount() > 0 && atm.getColumnCount() > 0) {
        Rectangle charBounds =
            _table.getFont().getStringBounds("MOgjQf",
                ((Graphics2D) this.getGraphics()).getFontRenderContext()).getBounds();
        _table.setRowHeight(charBounds.height + 2);
      }
      _owner.setSize(_owner.getWidth(), _owner.getHeight()+120);
      setVisible(true);
      repaint();
    }
  }

  public void addToHistory(String input) {
    if (history.contains(input)) {
      history.removeFirstOccurrence(input);
    }
    history.addFirst(input);
    if (history.size() > MAX_HISTORY_LENGTH) {
      history.removeLast();
    }
    ((AbstractTableModel) _table.getModel()).fireTableStructureChanged();
    try {
      String homepath = System.getProperty("user.home");
      FileWriter writer = new FileWriter((homepath + "/.hfc-history.yml"), false);
      yaml.dump(history, writer);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getLastQuery() {
    if (history.isEmpty()) return null;
    return history.getFirst();
  }

  private class RecentQueryModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private LinkedList<String> _d;

    public RecentQueryModel(LinkedList<String> qr) { _d = qr;}

    @Override
    public int getRowCount() { return _d.size(); }

    @Override
    public int getColumnCount() { return 1; }

    @Override
    public String getColumnName(int column) {
      return "None";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return _d.get(rowIndex);
    }
  }

  private JTable makeTable() {
    final JTable table = new JTable();
    // listener to sort columns
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        RecentQueryModel qrm = (RecentQueryModel)table.getModel();
        _listener.listen((String)qrm.getValueAt(row, 0));
        if (e.getClickCount() == 2) {
          ActionEvent f = new ActionEvent(e.getSource(), e.getID(), e.paramString());
          _exe.actionPerformed(f);
        }
      }
    });
    TableModel tm = new RecentQueryModel(history);
    table.setModel(tm);
    return table;
  }

}
