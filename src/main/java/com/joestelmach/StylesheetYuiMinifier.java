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
public class StylesheetYuiMinifier {
  public StylesheetYuiMinifier() {}
  
  public void minify(String inputFileName, String outputFileName, int lineBreakPosition) throws IOException {
    Reader in = new BufferedReader(new FileReader(new File(inputFileName)));
    CssCompressor compressor = new CssCompressor(in);
    Writer out = new BufferedWriter(new FileWriter(new File(outputFileName)));
    compressor.compress(out, lineBreakPosition);
    out.close();
    in.close();
  }
}
