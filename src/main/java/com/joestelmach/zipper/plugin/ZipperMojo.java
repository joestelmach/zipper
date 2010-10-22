package com.joestelmach.zipper.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
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
 * @goal zipper 
 * @phase process-resources
 */
public class ZipperMojo extends AbstractMojo {
  
  private List<String> _jsSourceFileNames;
  private List<String> _cssSourceFileNames;
  private JSOptimizerClosure _jsOptimizer;
  private CSSMinifierYUI _cssMinifier;
  private Configuration _configuration;
  private Map<String, List<String>> _finalManifest = new HashMap<String, List<String>>();
  
  private static final String PROP_FILE_NAME = "zipper.properties";
  private static final String JS_EXTENSION = ".js";
  private static final String CSS_EXTENSION = ".css";
  private static final String DEFAULT_OUTPUT_DIR = "assets";
  private static final String WORK_DIR = "zipper";
  
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
    jsLintCheck();
    jsOptimize();
    cssMinify();
    group();
    writeManifest();
  }
  
  /**
   * @throws MojoFailureException 
   * 
   */
  private void configure() throws MojoFailureException {
    List<String> paths = findFileNames("**/" + PROP_FILE_NAME, null, _project.getBasedir().getAbsolutePath());
    if(paths.size() == 0) {
      throw new MojoFailureException("Could not find your " + PROP_FILE_NAME + " file.  " +
      		"Please place it in src/main/resources");
    }
    
    try {
      String path = paths.get(0);
      getLog().info(PROP_FILE_NAME + " found: " + path);
      _configuration = new PropertiesConfiguration(paths.get(0));
      
    } catch (ConfigurationException e) {
      throw new MojoFailureException("Could not load your " + PROP_FILE_NAME + " file.");
    }
    
    // create our working dir
    File workDir = new File(getWorkDirPath());
    if(workDir.exists()) workDir.delete();
    workDir.mkdirs();
    
    // find all the files we'll be working with inside the configured
    // web root (relative to the project's base dir)
    _jsSourceFileNames = findFileNames("**/*" + JS_EXTENSION, null, getWebrootPath());
    _cssSourceFileNames = findFileNames("**/*" + CSS_EXTENSION, null, getWebrootPath());
  }
  /**
   * 
   * @throws MojoFailureException
   * @throws MojoExecutionException
   */
  public void jsLintCheck() throws MojoFailureException, MojoExecutionException {
    boolean lintSkip = _configuration.getBoolean(OptionKey.LINT_SKIP.getValue(), true);
    if(lintSkip) return;
    
    // find the list of excluded lint files
    @SuppressWarnings("unchecked")
    List<String> excludedPatterns = _configuration.getList(OptionKey.LINT_EXCLUDES.getValue());
    List<String> excludedFiles = new ArrayList<String>();
    
    for(String pattern:excludedPatterns) {
      excludedFiles.addAll(findFileNames(pattern, null, null));
    }
    
    for(String fileName:_jsSourceFileNames) {
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
  private void jsOptimize() throws MojoFailureException {
    if(_jsOptimizer == null) {
      _jsOptimizer = new JSOptimizerClosure();
    }
    
    // optimize each file from {webroot}/foo/bar.js to {outputdir}/foo/bar.js
    try {
      for(String fileName:_jsSourceFileNames) {
        getLog().info("optimizing " + fileName.substring(fileName.lastIndexOf('/') + 1));
        String optimizeOptions = _configuration.getString(OptionKey.JS_OPTIMIZE_OPTIONS.getValue(), "");
        _jsOptimizer.optimize(fileName, getOutputPathFromSourcePath(fileName), optimizeOptions);
      }
      
    } catch (Exception e) {
      throw new MojoFailureException(e.getMessage());
    }
  }
  
  /**
   * 
   */
  private void cssMinify() throws MojoFailureException {
    if(_cssMinifier == null) {
      _cssMinifier = new CSSMinifierYUI();
    }
    try {
      for(String fileName:_cssSourceFileNames) {
        getLog().info("minifying " + fileName.substring(fileName.lastIndexOf('/') + 1));
        int lineBreak = _configuration.getInt(OptionKey.CSS_LINE_BREAK.getValue(), -1);
        _cssMinifier.minify(fileName, getOutputPathFromSourcePath(fileName), lineBreak);
      }
    } catch(IOException e) {
      throw new MojoFailureException(e.getMessage());
    }
  }
  
  /*
   * 
   */
  private void group() throws MojoExecutionException {
    String outputPath = _project.getBuild().getOutputDirectory() + "/" + getOutputDir();
    
    File outputDirectory = new File(outputPath);
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
    
    // for each group, we'll create a list of files that should
    // be included, and attempt to combine them with the configured name
    for(AssetGroup group:groups) {
      List<String> includedOptimizedFiles = new ArrayList<String>();
      getLog().info("building " + outputSuffix + " asset " + group.getName());
      
      for(String include:group.getIncludes()) {
        for(String fileName:findFileNames(include, null, getWorkDirPath())) {
          includedOptimizedFiles.add(fileName);
        }
      }
      
      String outputFileName = outputDirectory + "/" + group.getName() + outputSuffix;
      if(includedOptimizedFiles.size() > 0) {
        combineAssets(includedOptimizedFiles, outputFileName, group.getGzip());
        _finalManifest.put("zipper.asset" + outputSuffix + "." + group.getName(), includedOptimizedFiles);
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
    
    File outputFile = new File(outputFileName);
    File gzipOutputFile = new File(outputFileName + ".gz");
    
    OutputStream output = null;
    GZIPOutputStream gzipOutput = null;
    boolean success = true;
    try {
      if(!outputFile.exists()) outputFile.createNewFile();
      output = new BufferedOutputStream(new FileOutputStream(outputFile));
      
      if(gzip) {
        if(!gzipOutputFile.exists()) gzipOutputFile.createNewFile();
        gzipOutput = new GZIPOutputStream(new BufferedOutputStream(
            new FileOutputStream(gzipOutputFile)));
      }
      
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
          writeAssetToStream(asset, gzipOutput);
        }
      }
        
    } catch (IOException e) {
      getLog().error("couldn't create asset file " + outputFileName, e);
      throw new MojoExecutionException("Something went wrong combining assets.", e);
      
    } finally {
      try {
        if(output != null) output.close();
        if(gzipOutput != null) gzipOutput.close();
        if(!success) {
          outputFile.delete();
          gzipOutputFile.delete();
        }
        
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
   * @param include
   * @return
   */
  private List<String> findFileNames(String include, String exclude, String basePath) {
    basePath =  basePath != null ? basePath : _project.getBuild().getOutputDirectory();
    File baseDir = new File(basePath);
    
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(baseDir);
    scanner.setIncludes(new String[]{include});
    if(exclude != null) scanner.setExcludes(new String[]{exclude});
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
   * @return
   */
  private Map<String, String> getLintOptions() {
    Map<String, String> map = new HashMap<String, String>();
    String prefix = OptionKey.LINT_OPTION_PREFIX.getValue();
    
    @SuppressWarnings("unchecked")
    Iterator<String> iter = _configuration.getKeys(prefix);
    while(iter.hasNext()) {
      String key = iter.next();
      map.put(key.substring(prefix.length() + 1), _configuration.getString(key));
    }
    
    return map;
  }
  
  
  @SuppressWarnings("unchecked")
  private List<AssetGroup>getGroups(OptionKey prefix) {
    Iterator<String> iter = (Iterator<String>) _configuration.getKeys(prefix.getValue());
    List<AssetGroup>groups = new ArrayList<AssetGroup>();
    String defaultAssetName = prefix.equals(OptionKey.JS_ASSET_PREFIX) ? "script" : "style";
    
    // if no groups were given for this asset prefix, we create a default group
    // consisting of all files of the prefix's type with an asset name of 'all'
    if(!iter.hasNext()) {
      AssetGroup group = new AssetGroup();
      group.setName(defaultAssetName);
      String includes = prefix.equals(OptionKey.JS_ASSET_PREFIX) ? "**/*.js" : "**/*.css";
      group.setIncludes(Arrays.asList(new String[]{includes}));
      group.setGzip(_configuration.getBoolean(OptionKey.GZIP.getValue(), true));
      groups.add(group);
    }
    else {
      while(iter.hasNext()) {
        String key = iter.next();
        AssetGroup group = new AssetGroup();
        String name = defaultAssetName;
        if(key.length() > prefix.getValue().length()) {
          name = key.substring(prefix.getValue().length() + 1);
        }
        group.setName(name);
        group.setIncludes(_configuration.getList(key));
        group.setGzip(_configuration.getBoolean(OptionKey.GZIP.getValue(), true));
        groups.add(group);
      }
    }
    return groups;
  }
  
  /**
   * 
   * @return
   */
  private String getOutputDir() {
    return _configuration.getString(OptionKey.OUTPUT_PATH.getValue(), DEFAULT_OUTPUT_DIR);
  }
  
  /**
   * 
   */
  private void writeManifest() {
    List<String> files = findFileNames("**/" + PROP_FILE_NAME, null, null);
    if(files.size() == 0) return;
    
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(files.get(0), true));
      out.write("\n\n# ***** Generated Zipper Manifest *****\n");
      
      for(Entry<String,List<String>> entry:_finalManifest.entrySet()) {
        for(String file:entry.getValue()) {
          String pathInBuildDir = file.substring(_project.getBuild().getOutputDirectory().length() + 1);
          
          
          out.write(entry.getKey() + " = " + pathInBuildDir + "\n");
        }
      }
      
    } catch (IOException e) {
      getLog().error("Could not write asset manifest", e);
      
    } finally {
      try {
        if(out != null) out.close();
        
      } catch (IOException e) {
        getLog().error("could not close stream", e);
      }
    }
  }
  
  /**
   * 
   * @return
   */
  private String getWebrootPath() {
    String basePath = _project.getBasedir().getAbsolutePath();
    String webroot = _configuration.getString(OptionKey.WEB_ROOT.getValue(), "src/main/webapp");
    return basePath + (webroot.startsWith("/") ? "" : "/") + webroot;
  }
  
  /**
   * 
   * @return the absolute path the directory used to store optimized
   * files while building
   */
  private String getWorkDirPath() {
    return _project.getBuild().getOutputDirectory() + "/" + WORK_DIR;
  }
  
  /**
   * 
   * @param sourcePath
   * @return
   */
  private String getOutputPathFromSourcePath(String sourcePath) {
    return getWorkDirPath() + "/" + sourcePath.substring(getWebrootPath().length() + 1);
  }
}