package com.joestelmach;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.yahoo.platform.yui.compressor.CssCompressor;

/**
 * 
 * @author Joe Stelmach
 *
 */
public class CssMinifier {
  public CssMinifier() {}
  
  public void minify(String fileName) throws IOException {
    Reader in = new BufferedReader(new FileReader(new File(fileName)));
    CssCompressor compressor = new CssCompressor(in);
    String outputFileName = fileName.substring(0, fileName.length() - 4) + "-min.css";
    Writer out = new BufferedWriter(new FileWriter(new File(outputFileName)));
    compressor.compress(out, 0);
    out.close();
    in.close();
  }
}
