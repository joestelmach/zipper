package com.joestelmach.zipper.plugin;

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
 * A YUI CSS Minifier
 * 
 * @author Joe Stelmach
 */
public class CSSMinifierYUI {
  
  public void minify(String inputFileName, String outputFileName, int lineBreakPosition) throws IOException {
    // ensure that the output directory exists
    String outDir = outputFileName.substring(0, outputFileName.lastIndexOf('/'));
    new File(outDir).mkdirs();
    
    Reader in = new BufferedReader(new FileReader(new File(inputFileName)));
    CssCompressor compressor = new CssCompressor(in);
    Writer out = new BufferedWriter(new FileWriter(new File(outputFileName)));
    compressor.compress(out, lineBreakPosition);
    out.close();
    in.close();
  }
}
