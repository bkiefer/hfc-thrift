/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.lt.hfc.db.ui;

import java.io.File;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

/**
 *
 */
@SuppressWarnings("serial")
public class SaveFileDialog extends JFileChooser {

  private final JFrame parent;

  public SaveFileDialog(JFrame p) {
    parent = p;
    setCurrentDirectory(new File(System.getProperty("user.home")));
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    //addChoosableFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif", "bmp"));
  }

  public Path save() {
    int userSelection = showSaveDialog(parent);
    if (userSelection == JFileChooser.APPROVE_OPTION) {
      return getSelectedFile().toPath();
    }
    return null;
  }

}
