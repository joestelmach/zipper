package com.joestelmach.zipper.plugin;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.googlecode.jslint4java.Issue;
import com.googlecode.jslint4java.JSLint;
import com.googlecode.jslint4java.JSLintBuilder;
import com.googlecode.jslint4java.JSLintResult;
import com.googlecode.jslint4java.Option;

/**
 * A JSLint linter
 * 
 * @author Joe Stelmach
 */
public class LinterJSLint {
  private Configuration _config;
  private Log _log;
  
  /**
   * 
   * @param config
   */
  LinterJSLint(Configuration config, Log log) {
    _config = config;
    _log = log;
  }
  
  /**
   * Runs the file at the given absolute path through the lint check
   * @param fileName the absolute path to the file to check
   * @throws MojoFailureException if a lint warning occurs and the given 
   *         configuration specifies to fail on lint warnings.
   */
  public void check(String fileName) throws MojoFailureException {
    _log.info("lint checking " + fileName.substring(fileName.lastIndexOf('/') + 1));
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
      
      // add configured lint options
      JSLint lint = new JSLintBuilder().fromDefault();
      for(Entry<String,String>entry:getLintOptions().entrySet()) {
        try {
          Option option = Option.valueOf(entry.getKey().toUpperCase());
          lint.addOption(option, entry.getValue());
          _log.info("added lint option " + entry.getKey() + ": " + entry.getValue());
          
        } catch(Exception e) {
          _log.warn("invalid lint option: " + entry.getKey());
        }
      }
      
      // run the linter, printing any issues found
      JSLintResult result = lint.lint(fileName, reader);
      List<Issue> issues = result.getIssues();
      for(Issue issue:issues) {
        _log.warn("line " + issue.getLine() + ": " + issue.getReason());
      }
      
      // if some issues were found, we decide what to do with the warning
      if(issues.size() > 0) {
        processLintWarning("The javascript lint check failed.  You can disable lint " +
          "checking by setting the 'lint.skip' option to true or defining a list of " +
          "files to exclude using the 'lint.exclude' option.  Alternatively, you can" + 
          "force the build to succeed when lint errors are present by setting the "   +
          "'lint.failonwarning' option to false.");
      }
        
    } catch (IOException e) {
      processLintWarning(e.getMessage());
      
    } finally {
      try { if(reader != null) reader.close(); } catch(IOException e) {
        _log.error("could not close js stream for file: " + fileName);
      }
    }
  }
  
  /**
   * @return a map of lint options found in the configuration
   */
  private Map<String, String> getLintOptions() {
    Map<String, String> map = new HashMap<String, String>();
    String prefix = ConfigKey.LINT_OPTION_PREFIX.getKey();
    
    @SuppressWarnings("unchecked")
    Iterator<String> iter = _config.getKeys(prefix);
    while(iter.hasNext()) {
      String key = iter.next();
      map.put(key.substring(prefix.length() + 1), _config.getString(key));
    }
    
    return map;
  }
  
  /**
   *  
   */
  private void processLintWarning(String message) throws MojoFailureException {
    boolean lintFailOnWarning = _config.getBoolean(ConfigKey.LINT_FAIL_ON_WARNING.getKey(), false);
    if(lintFailOnWarning) {
      throw new MojoFailureException(message);
    }
  }
}
