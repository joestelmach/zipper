package com.joestelmach;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

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
  private JavascriptClosureOptimizer _jsOptimizer;
  private StylesheetYuiMinifier _cssMinifier;
  
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
  
  /**
   * @parameter default-value="false" 
   */
  private boolean bustCache;
  
  /**
   * @parameter
   */
  private List<JavascriptGroup> javascriptGroups; 
  
  /**
   * @parameter
   */
  private List<StylesheetGroup> stylesheetGroups; 
  
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
    group();
    if(bustCache) bustCache();
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
      _jsOptimizer = new JavascriptClosureOptimizer();
    }
    
    try {
      for(String fileName:_javascriptFileNames) {
        getLog().info("optimizing " + fileName.substring(fileName.lastIndexOf('/') + 1));
        _jsOptimizer.optimize(fileName, getOptimizedName(fileName), closureOptions);
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
      _cssMinifier = new StylesheetYuiMinifier();
    }
    try {
      for(String fileName:_cssFileNames) {
        getLog().info("minifying " + fileName.substring(fileName.lastIndexOf('/') + 1));
        _cssMinifier.minify(fileName, getOptimizedName(fileName));
      }
    } catch(IOException e) {
      throw new MojoFailureException(e.getMessage());
    }
  }
  
  /*
   * 
   */
  private void group() throws MojoExecutionException {
    String outputDirectory = _project.getBuild().getOutputDirectory();
    if(javascriptGroups != null) {
      processGroup(javascriptGroups, outputDirectory, ".js");
    }
    
    if(stylesheetGroups != null) {
      processGroup(stylesheetGroups, outputDirectory, ".css");
    }
  }
  
  /**
   * Processes the given asset group, combining the optimized version of each 
   * included file into a new file with the group's name, stored in the given 
   * directory.
   * 
   * @param groups
   * @throws MojoExecutionException 
   */
  private void processGroup(List<? extends AbstractGroup> groups, 
      String outputDirectory, String outputSuffix) throws MojoExecutionException {
    
    // for each group, we'll create a unique list of files that should
    // be included, and attempt to combine them with the configured name
    for(AbstractGroup group:groups) {
      Set<String> includedOptimizedFiles = new LinkedHashSet<String>();
      getLog().info("building asset " + group.getName());
      for(String include:group.getIncludes()) {
        for(String fileName:findFileNames(include)) {
          includedOptimizedFiles.add(getOptimizedName(fileName));
        }
      }
      
      String outputFileName = outputDirectory + "/" + group.getName() + outputSuffix;
      combineAssets(includedOptimizedFiles, outputFileName, group.getGzip());
    }
  }
  
  /**
   * Combines the given assets into a file with the given name
   * 
   * @param assets
   * @param outputFileName
   * @throws MojoExecutionException if an asset cannot be found, or an IOException occurs
   */
  private void combineAssets(Collection<String> assets, String outputFileName, boolean gzip) 
      throws MojoExecutionException {
    
    if(gzip) outputFileName += ".gz";
    
    File outputFile = new File(outputFileName);
    OutputStream output = null;
    boolean success = true;
    try {
      if(!outputFile.exists()) outputFile.createNewFile();
      output = new BufferedOutputStream(new FileOutputStream(outputFile));
      if(gzip) output = new GZIPOutputStream(output);
      
      for(String asset:assets) {
        File file = new File(asset);
        if(!file.exists()) {
          success = false;
          throw new MojoExecutionException("couldn't find file " + asset +
            ".  Please specify the path using the standard ant patterns: " +
            "http://ant.apache.org/manual/dirtasks.html#patterns");
        }
        else {
          getLog().info("adding file: " + asset);
          writeAssetToStream(asset, output);
        }
      }
        
    } catch (IOException e) {
      getLog().error("couldn't create asset file " + outputFileName, e);
      throw new MojoExecutionException("Something went wrong combining assets.", e);
      
    } finally {
      try {
        if(output != null) output.close();
        if(!success) outputFile.delete();
        
      } catch (IOException e) {
        getLog().error("couldn't close writer", e);
      }
    }
  }
  
  /**
   * 
   * @param assetFileName
   * @param writer
   */
  private void writeAssetToStream(String assetFileName, OutputStream output) throws IOException {
    InputStream input = null;
    try {
      input = new BufferedInputStream(new FileInputStream(new File(assetFileName)));
      while(input.available() > 0) {
        output.write(input.read());
      }
      
    } finally {
      if(input != null) input.close();
    }
  }
  
  /**
   * 
   */
  private void bustCache() {
    getLog().info("performing cache busting");
    
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
  
  /**
   * 
   * @param originalName
   * @return
   */
  private String getOptimizedName(String originalName) {
    String suffix = originalName.endsWith(".css") ? "-min.css" : "-min.js";
    int suffixLength = suffix.length() - 4;
    return originalName.substring(0, originalName.length() - suffixLength) + suffix;
  }
}