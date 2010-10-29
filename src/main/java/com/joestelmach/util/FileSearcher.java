package com.joestelmach.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Locates files using standard ant patterns
 * http://ant.apache.org/manual/dirtasks.html#patterns
 * 
 * @author Joe Stelmach
 */
public class FileSearcher {
  
  /**
   * Searches for files with the given pattern, starting in the given base directory,
   * and returns a list of absolute paths to matching files.
   *  
   * @param pattern
   * @param basePath
   * @return
   */
  public List<String> search(String pattern, String basePath) {
    File baseDir = new File(basePath);
    
    if(pattern.startsWith("/")) pattern = pattern.substring(1);
    
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(baseDir);
    scanner.setIncludes(new String[]{pattern});
    scanner.addDefaultExcludes();
    scanner.scan();
    
    List<String> fileNames = new ArrayList<String>();
    for(String fileName:scanner.getIncludedFiles() ) {
      fileNames.add(baseDir + "/" + fileName);
    }
    return fileNames;
  }
}
