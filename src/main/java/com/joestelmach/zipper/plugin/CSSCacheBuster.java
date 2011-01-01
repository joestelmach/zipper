package com.joestelmach.zipper.plugin;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;


/**
 * Responsible for busting the cache of any url references
 * found inside css files
 * 
 * @author Joe Stelmach
 */
public class CSSCacheBuster {
  private static final Pattern PATTERN = Pattern.compile("(url\\([^\\)]*)(\\))");
  
  private Log _log;
  
  /**
   * @param log
   */
  public CSSCacheBuster(Log log) {
    _log = log;
  }
  
  /**
   * Busts the cache of all url references found in the given css file, 
   * over-writing the original. 
   * 
   * @param fileName The absolute path to the css file to bust
   */
  public void bustIt(String fileName) {
    File file = new File(fileName);
    if(!file.exists()) {
      _log.error("Couldn't find css file: " + file);
      return;
    }
    
    String css = readCss(file);
    Matcher matcher = PATTERN.matcher(css);
    while(matcher.find()) {
      if(matcher.groupCount() == 2) {
        String bustedCSS = matcher.replaceAll("$1?" + file.lastModified() + "$2");
        writeCss(bustedCSS, file);
      }
    }
  }
  
  /**
   * Reads the css from the given file into a string 
   * 
   * @param fileName
   * @return 
   */
  public String readCss(File file) {
    BufferedReader in = null;
    ByteArrayOutputStream bytesOut = null;
    OutputStream out = null;
    try {
      in = new BufferedReader(new FileReader(file));
      bytesOut = new ByteArrayOutputStream();
      out = new BufferedOutputStream(bytesOut);
      int c;
      while((c = in.read()) != -1) {
        out.write(c);
      }
      
    } catch(IOException e) {
      _log.error("Couldn't process css file: " + file, e);
      
    } finally {
      try {
        if(in != null) in.close();
        if(out != null) out.close();
        
      } catch(IOException e) {
        _log.error("Couldn't close stream", e);
      }
    }
    
    return new String(bytesOut.toByteArray());
  }
  
  /**
   * Writes the given css to the given file, over-writing
   * any existing content.
   * 
   * @param css
   * @param file
   */
  private void writeCss(String css, File file) {
    BufferedOutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(file));
      out.write(css.getBytes());
    
    } catch(IOException e) {
      _log.error("couldn't write css file " + file.getAbsolutePath());
      
    } finally {
      try { if(out != null) out.close(); } catch(IOException e) {
        _log.error("Couldn't close stream", e);
      }
    }
  }
}
