package de.dfki.lt.hfc.db.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;

import de.dfki.lt.hfc.db.QueryResult;
import de.dfki.lt.hfc.db.remote.QueryException;

public class QueryWindow extends JFrame {
  private static final long serialVersionUID = 1L;

  private static String DEFAULT_QUERY = "select ?s ?o where ?s <rdf:type> ?o ?_";

  public static int DEFAULT_FONT_SIZE = 18;

  public JTable table;

  public JTextField queryInput;

  private List<Listener<String>> _listeners = new ArrayList<>();

  private RecentDialog hisDialog;
  public JLabel _statusbar;

  public void register(Listener<String> l) {
    _listeners.add(l);
  }

  private class QueryResultModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private QueryResult _qr;

    public QueryResultModel(QueryResult qr) { _qr = qr;}

    @Override
    public int getRowCount() { return _qr.getTable().getRowsSize(); }

    @Override
    public int getColumnCount() { return _qr.getVariablesSize(); }

    @Override
    public String getColumnName(int column) {
      return _qr.getVariables().get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return _qr.getTable().getRows().get(rowIndex).get(columnIndex);
    }

    @Override
    public boolean isCellEditable(int row, int col) { return false; }

    public void sortByColumn(final int col) {
      Collections.sort(_qr.table.rows, new Comparator<List<String>>() {
        @Override
        public int compare(List<String> o1, List<String> o2) {
          return o1.get(col).compareTo(o2.get(col));
        }});
      this.fireTableStructureChanged();
    }

    public void saveToFile(Path path) {
      try {
        _qr.saveToFile(path);
      } catch (IOException ex) {
        _statusbar.setText("Error saving to file: "+ex.getMessage());
      }
    }

    public void saveToGraph(Path path) {
      try {
        _qr.saveToGraph(path, true);
      } catch (IOException ex) {
        _statusbar.setText("Error saving graph: "+ex.getMessage());
      }
    }
  };

  private void showMsg(String msg) {
    _statusbar.setText(msg);
    _statusbar.setForeground(new Color(8, 135, 81));
  }

  private void showError(String msg) {
    _statusbar.setText(msg);
    _statusbar.setForeground(new Color(222, 41, 38));
  }

  public static void interactive (final Queryable client) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final QueryWindow qw = new QueryWindow();
        qw.register(new Listener<String>() {
          @Override
          public void listen(String query) {
            try {
              // put input in history
              QueryResult qr = client.query(query);
              qw.addToHistory(query);
              qw.setContent(qr, query);
              qw.showMsg(qr.table.getRowsSize() + " results have been found.");
            } catch (QueryException ex) {
              qw.showError(ex.getWhy());
            } catch (de.dfki.lt.hfc.db.QueryException ex) {
              qw.showError(ex.getWhy());
            } catch (RuntimeException rex) {
              if (rex.getCause() instanceof QueryException) {
                qw.showError(((QueryException)rex.getCause()).getWhy());
              } else {
                rex.printStackTrace();
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
      }
    });
  }
  public void setContent(final QueryResult res, String query) throws IOException {
    if (res == null) return;
    AbstractTableModel atm = new QueryResultModel(res);
    table.setModel(atm);
    atm.fireTableStructureChanged();
    if (atm.getRowCount() > 0 && atm.getColumnCount() > 0) {
      Rectangle charBounds =
          table.getFont().getStringBounds("MOgjQf",
              ((Graphics2D) this.getGraphics()).getFontRenderContext()).getBounds();
      table.setRowHeight(charBounds.height + 2);
    }
    table.repaint();
  }

  public void addToHistory(String query) throws IOException {
    // put input in history
    hisDialog.addToHistory(query);
  }

  private static JButton getIconButton(String name) {
    URL url = QueryWindow.class.getClassLoader()
        .getResource("icons/" + name + ".png");
    Icon icon = new ImageIcon(url);
    JButton button = new JButton(icon);
    button.setPreferredSize(new Dimension(32,32));
    button.setBorderPainted(false);
    button.setBorder(null);
    //button.setFocusable(false);
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setContentAreaFilled(false);
    return button;
  }

  private void saveTableToFile(QueryResultModel qr) {
    // open file save dialog
    Path p = new SaveFileDialog(this).save();
    // if not canceled, save table
    if (p != null) {
      qr.saveToFile(p);
    }
  }

  private void saveTableToGraph(QueryResultModel qr) {
    // open file save dialog
    Path p = new SaveFileDialog(this).save();
    // if not canceled, save table
    if (p != null) {
      qr.saveToGraph(p);
    }
  }

  private JTable getResultTable() {
    final JTable table = new JTable();
    // listener to sort columns
    table.getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int col = table.columnAtPoint(e.getPoint());
        // String name = table.getColumnName(col);
        QueryResultModel qrm = (QueryResultModel)table.getModel();
        qrm.sortByColumn(col);
      }
    });
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        int eventX = event.getX();
        int eventY = event.getY();
        if (event.getButton() == MouseEvent.BUTTON3) {
          JPopupMenu pop = new JPopupMenu();
          // save to file menu item
          JMenuItem saveToFile = new JMenuItem("Save to File");
          saveToFile.addActionListener(
              (e) -> QueryWindow.this.saveTableToFile(
                  ((QueryResultModel)table.getModel())));
          pop.add(saveToFile);
          // save to graph item
          JMenuItem saveToGraph = new JMenuItem("Save to Graph");
          saveToGraph.addActionListener(
              (e) -> QueryWindow.this.saveTableToGraph(
                  ((QueryResultModel)table.getModel())));
          pop.add(saveToGraph);
          saveToGraph.setEnabled(
              3 == ((QueryResultModel)table.getModel()).getColumnCount());
          pop.show(table, eventX, eventY);
        }
      }
    });
    return table;
  }

  private JTextField createQueryInput(ActionListener execute, String query) {
    JTextField queryInput = new JTextField();
    queryInput.setText(query);
    queryInput.addActionListener(execute);
    return queryInput;
  }

  private static void setDefaultFont(int size) {
    FontUIResource font = new FontUIResource("'DejaVu Sans Mono", Font.PLAIN, size);
    for (Map.Entry<Object, Object> e :
      UIManager.getLookAndFeelDefaults().entrySet()) {
      try {
        String key = (String) e.getKey();
        if (key.endsWith(".font")) {
          UIManager.put(key, font);
        }
        // If you want to list them all.
        // System.out.println(e.getKey() + " " + e.getValue());
      } catch (ClassCastException ex) { /* so what. */ }
    }
  }


  public QueryWindow() {
    super("HFC interactive");
    // use native windowing system to position new frames
    this.setLocationByPlatform(true);
    // set preferred size
    this.setPreferredSize(new Dimension(800, 300));
    setDefaultFont(DEFAULT_FONT_SIZE);

    final QueryWindow mainFrame = this;

    // set handler for closing operations
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // this.addWindowListener(new Terminator());
    // create content panel and add it to the frame
    JPanel contentPane = new JPanel(new BorderLayout());
    this.setContentPane(contentPane);

    table = getResultTable();
    JScrollPane jsp = new JScrollPane(table);
    contentPane.add(jsp, BorderLayout.CENTER);

    ActionListener execute = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        synchronized(queryInput) {
          String input = queryInput.getText().replaceAll("\\\\", " ");
          input = input.replaceAll("\"\\s*\\+\\s*\"", "");
          queryInput.setText(input);
          for (Listener<String> l : _listeners) { l.listen(input); }
        }
      }
    };

//    ComponentListener movsiz = new ComponentListener() {
//      @Override
//      public void componentResized(ComponentEvent e) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//      }
//
//      @Override
//      public void componentMoved(ComponentEvent e) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//      }
//
//      @Override
//      public void componentShown(ComponentEvent e) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//      }
//
//      @Override
//      public void componentHidden(ComponentEvent e) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//      }
//    };

    JPanel south = new JPanel();
    south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

    JPanel south_buttons = new JPanel();
    south_buttons.setLayout(new BoxLayout(south_buttons, BoxLayout.X_AXIS));

    JButton jb = getIconButton("help-about");
    jb.setToolTipText("show help");
    south_buttons.add(jb);

    jb = getIconButton("document-open-recent");
    jb.setToolTipText("show recent queries");
    south_buttons.add(jb);
    hisDialog = new RecentDialog(mainFrame,
    new Listener<String>() {
      @Override
      public void listen(String q) {
        queryInput.setText(q);
      }
    },
    execute);

    jb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hisDialog.changeVisibility();
      }});

    String lastQuery = hisDialog.getLastQuery();
    if (lastQuery == null) lastQuery = DEFAULT_QUERY;
    south_buttons.add(queryInput = createQueryInput(execute, lastQuery));

    jb = getIconButton("gtk-apply");
    jb.setToolTipText("execute query");
    south_buttons.add(jb);
    jb.addActionListener(execute);

    jb = getIconButton("gtk-clear");
    jb.setToolTipText("clear input field");
    south_buttons.add(jb);
    jb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        queryInput.setText("");
      }});

    south.add(south_buttons);

    JPanel south_status = new JPanel();
    south_status.setLayout(new FlowLayout(FlowLayout.LEFT));
    _statusbar = new JLabel("Welcome", SwingConstants.LEFT);
    _statusbar.setForeground(new Color(168, 168, 168));
    south_status.add(_statusbar);

    south.add(south_status);
    south.add(hisDialog);
    contentPane.add(south, BorderLayout.SOUTH);

    // display the frame
    this.pack();
    this.setLocationRelativeTo(null);
    this.setVisible(true);

  }

}
