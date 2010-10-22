package com.joestelmach.zipper.plugin;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
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
  
  private List<String> _jsFileNames;
  private List<String> _cssFileNames;
  private JSOptimizerClosure _jsOptimizer;
  private CSSMinifierYUI _cssMinifier;
  private Configuration _configuration;
  
  private static final String JS_EXTENSION = ".js";
  private static final String CSS_EXTENSION = ".css";
  private static final String JS_MIN_EXTENSION = "-min.js";
  private static final String CSS_MIN_EXTENSION = "-min.css";
  
  /**
   * The maven project.
   * @parameter expression="${project}"
   * @readonly
   * @required
   */
  private MavenProject _project;
  
  /**
   * 
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    configure();
    lintCheckJs();
    optimizeJs();
    minifyCss();
    group();
  }
  
  /**
   * @throws MojoFailureException 
   * 
   */
  private void configure() throws MojoFailureException {
    try {
      _configuration = new PropertiesConfiguration("zipper.properties");
      
    } catch (ConfigurationException e) {
      throw new MojoFailureException("Could not find zipper.properties in your classpath");
    }
    
    ConfigurationUtils.dump(_configuration, System.out);
    
    // find all the files we'll be working with
    _jsFileNames = findFileNames("**/*.." + JS_EXTENSION);
    _cssFileNames = findFileNames("**/*" + CSS_EXTENSION);
  }
  
  /**
   * 
   * @throws MojoFailureException
   * @throws MojoExecutionException
   */
  public void lintCheckJs() throws MojoFailureException, MojoExecutionException {
    boolean lintSkip = _configuration.getBoolean(OptionKey.LINT_SKIP.getValue(), true);
    if(lintSkip) return;
    
    // find the list of excluded lint files
    @SuppressWarnings("unchecked")
    List<String> excludedFiles = (List<String>) 
      _configuration.getList(OptionKey.LINT_EXCLUDES.getValue());
    for(String exclude:excludedFiles) {
      excludedFiles.addAll(findFileNames(exclude));
    }
    
    for(String fileName:_jsFileNames) {
      if(excludedFiles.contains(fileName)) continue;
      
      getLog().info("lint checking " + fileName.substring(fileName.lastIndexOf('/') + 1));
      BufferedReader reader = null;
      try {
          reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
          
          // add configured lint options
          JSLint lint = new JSLintBuilder().fromDefault();
            for(Entry<String,String>entry:getLintOptions().entrySet()) {
            try {
              Option option = Option.valueOf(entry.getKey().toUpperCase());
              lint.addOption(option, entry.getValue());
              getLog().info("added lint option " + entry.getKey() + ": " + entry.getValue());
              
            } catch(Exception e) {
              getLog().warn("invalid lint option: " + entry.getKey());
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
      _jsOptimizer = new JSOptimizerClosure();
    }
    
    try {
      for(String fileName:_jsFileNames) {
        getLog().info("optimizing " + fileName.substring(fileName.lastIndexOf('/') + 1));
        String optimizeOptions = _configuration.getString(OptionKey.JS_OPTIMIZE_OPTIONS.getValue());
        _jsOptimizer.optimize(fileName, getOptimizedName(fileName), optimizeOptions);
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
      _cssMinifier = new CSSMinifierYUI();
    }
    try {
      for(String fileName:_cssFileNames) {
        getLog().info("minifying " + fileName.substring(fileName.lastIndexOf('/') + 1));
        int lineBreak = _configuration.getInt(OptionKey.CSS_LINE_BREAK.getValue());
        _cssMinifier.minify(fileName, getOptimizedName(fileName), lineBreak);
      }
    } catch(IOException e) {
      throw new MojoFailureException(e.getMessage());
    }
  }
  
  /*
   * 
   */
  private void group() throws MojoExecutionException {
    String outputPath = _configuration.getString(OptionKey.OUTPUT_PATH.getValue());
    File outputDirectory = new File(_project.getBuild().getOutputDirectory() + "/" + outputPath);
    if(!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }
    
    processGroups(getGroups(OptionKey.JS_ASSET_PREFIX), 
        outputDirectory.getAbsolutePath(), JS_EXTENSION);
    
    processGroups(getGroups(OptionKey.CSS_ASSET_PREFIX), 
        outputDirectory.getAbsolutePath(), CSS_EXTENSION);
  }
  
  /**
   * Processes the given asset group, combining the optimized version of each 
   * included file into a new file with the group's name, stored in the given 
   * directory.
   * 
   * @param groups
   * @throws MojoExecutionException 
   */
  private void processGroups(List<? extends AssetGroup> groups, 
      String outputDirectory, String outputSuffix) throws MojoExecutionException {
    
    // for each group, we'll create a unique list of files that should
    // be included, and attempt to combine them with the configured name
    for(AssetGroup group:groups) {
      Set<String> includedOptimizedFiles = new LinkedHashSet<String>();
      getLog().info("building asset " + group.getName());
      
      for(String include:group.getIncludes()) {
        for(String fileName:findFileNames(include)) {
          if(!fileName.endsWith(JS_MIN_EXTENSION) && !fileName.endsWith(CSS_MIN_EXTENSION)) {
            includedOptimizedFiles.add(getOptimizedName(fileName));
          }
        }
      }
      
      String outputFileName = outputDirectory + "/" + group.getName() + outputSuffix;
      if(includedOptimizedFiles.size() > 0) {
        combineAssets(includedOptimizedFiles, outputFileName, group.getGzip());
      }
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
  private void processLintFailure(String message) throws MojoFailureException, MojoExecutionException {
    boolean lintFailOnWarning = 
      _configuration.getBoolean(OptionKey.LINT_FAIL_ON_WARNING.getValue(), false);
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
    String suffix = originalName.endsWith(CSS_EXTENSION) ? CSS_MIN_EXTENSION : JS_MIN_EXTENSION;
    int suffixLength = suffix.length() - 4;
    return originalName.substring(0, originalName.length() - suffixLength) + suffix;
  }
  
  /**
   * 
   * @return
   */
  private Map<String, String> getLintOptions() {
    Map<String, String> map = new HashMap<String, String>();
    @SuppressWarnings("unchecked")
    Iterator<String> iter = _configuration.getKeys(OptionKey.LINT_OPTION_PREFIX.getValue());
    while(iter.hasNext()) {
      String key = iter.next();
      map.put(key, _configuration.getString(key));
    }
    
    return map;
  }
  
  
  @SuppressWarnings("unchecked")
  private List<AssetGroup>getGroups(OptionKey prefix) {
    Iterator<String> iter = (Iterator<String>) _configuration.getKeys(prefix.getValue());
    List<AssetGroup>groups = new ArrayList<AssetGroup>();
    while(iter.hasNext()) {
      String key = iter.next();
      AssetGroup group = new AssetGroup();
      group.setName(key.substring(prefix.getValue().length()));
      group.setIncludes(_configuration.getList(key));
      group.setGzip(true);
      groups.add(group);
    }
    return groups;
  }
}