package com.joestelmach;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import com.googlecode.jslint4java.Issue;
import com.googlecode.jslint4java.JSLint;
import com.googlecode.jslint4java.JSLintBuilder;
import com.googlecode.jslint4java.JSLintResult;
import com.googlecode.jslint4java.Option;

/**
 * @author Joe Stelmach
 * 
 * @goal pack 
 * @execute phase="process-resources"
 * @phase process-resources
 */
public class PackMojo extends AbstractMojo {
  
  private List<String> _javascriptFileNames;
  private List<String> _cssFileNames;
  private JSOptimizer _jsOptimizer;
  private CssMinifier _cssMinifier;
  
  /**
   * The maven project.
   * @parameter expression="${project}"
   * @readonly
   * @required
   */
  private MavenProject _project;
  
  /***** Configuration Parameters *****/
  
  /**
   * @parameter default-value="false"
   */
  private boolean lintSkip;
  
  /**
   * @parameter default-value="true"
   */
  private boolean lintFailOnWarning;
  
  /**
   * @parameter
   */
  private List<String> lintExcludes;
  
  /**
   * @parameter
   */
  private Map<String, String>lintOptions;
  
  /**
   * @parameter default-value=""
   */
  private String closureOptions;
  
  /************************************/
  
  /**
   * 
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    // first we find the files we'll be working with
    _javascriptFileNames = findFileNames("**/*.js");
    _cssFileNames = findFileNames("**/*.css");
    
    if(!lintSkip) lintCheckJs();
    optimizeJs();
    minifyCss();
  }
  
  /**
   * 
   * @throws MojoFailureException
   * @throws MojoExecutionException
   */
  public void lintCheckJs() throws MojoFailureException, MojoExecutionException {
    // find the list of excluded lint files
    List<String> excludedFiles = new ArrayList<String>();
    if(lintExcludes != null) {
      for(String exclude:lintExcludes) {
        excludedFiles.addAll(findFileNames(exclude));
      }
    }
    
    for(String fileName:_javascriptFileNames) {
      if(excludedFiles.contains(fileName)) continue;
      
      getLog().info("lint checking " + fileName.substring(fileName.lastIndexOf('/') + 1));
      BufferedReader reader = null;
      try {
          reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
          
          // add configured lint options
          JSLint lint = new JSLintBuilder().fromDefault();
          if(lintOptions != null) {
            for(Entry<String,String>entry:lintOptions.entrySet()) {
              try {
                Option option = Option.valueOf(entry.getKey().toUpperCase());
                lint.addOption(option, entry.getValue());
                getLog().info("added lint option " + entry.getKey() + ": " + entry.getValue());
              } catch(Exception e) {
                getLog().warn("invalid lint option: " + entry.getKey());
              }
            }
          }
          
          JSLintResult result = lint.lint(fileName, reader);
          List<Issue> issues = result.getIssues();
          for(Issue issue:issues) {
            getLog().warn("line " + issue.getLine() + ": " + issue.getReason());
          }
          if(issues.size() > 0) {
            processLintFailure("The javascript lint check failed.  You can disable lint " +
              "checking by setting the 'lintSkip' option to true.  Alternatively, you can force " +
              "the build to succeed when lint errors are present by setting the 'lintFailOnWarning' " +
              "option to false.");
          }
          
      } catch (IOException e) {
        processLintFailure(e.getMessage());
        
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch(IOException e) {
            getLog().error("could not close js stream for file: " + fileName);
          }
        }
      }
    }
  }
  
  /**
   * 
   * @throws MojoFailureException
   */
  private void optimizeJs() throws MojoFailureException {
    if(_jsOptimizer == null) {
      _jsOptimizer = new JSOptimizer();
    }
    
    try {
      for(String fileName:_javascriptFileNames) {
        getLog().info("optimizing " + fileName.substring(fileName.lastIndexOf('/') + 1));
        _jsOptimizer.optimize(fileName, closureOptions);
      }
    } catch (Exception e) {
      throw new MojoFailureException(e.getMessage());
    }
  }
  
  /**
   * 
   */
  private void minifyCss() throws MojoFailureException {
    if(_cssMinifier == null) {
      _cssMinifier = new CssMinifier();
    }
    try {
      for(String fileName:_cssFileNames) {
        getLog().info("minifying " + fileName.substring(fileName.lastIndexOf('/') + 1));
        _cssMinifier.minify(fileName);
      }
    } catch(IOException e) {
      throw new MojoFailureException(e.getMessage());
    }
  }
  
  /**
   * 
   */
  private void processLintFailure(String message) throws MojoFailureException, MojoExecutionException {
    if(lintFailOnWarning) {
      throw new MojoFailureException(message);
    }
  }
  
  /**
   * 
   * @param expression
   * @return
   */
  private List<String> findFileNames(String expression) {
    File baseDir = new File(_project.getBuild().getOutputDirectory());
    
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(baseDir);
    scanner.setIncludes(new String[]{expression});
    scanner.addDefaultExcludes();
    scanner.scan();
    
    List<String> fileNames = new ArrayList<String>();
    for(String fileName:scanner.getIncludedFiles() ) {
      fileNames.add(baseDir + "/" + fileName);
    }
    return fileNames;
  }
  
  public static void main(String[] args) throws Exception {
 
    
  }
}