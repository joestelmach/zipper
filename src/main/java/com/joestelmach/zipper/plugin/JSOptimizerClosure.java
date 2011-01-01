package com.joestelmach.zipper.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;

/**
 * A simple wrapper around the Google closure compiler.
 * 
 * @author Joe Stelmach
 */
public class JSOptimizerClosure {
  
  /**
   * 
   */
  public JSOptimizerClosure() {}
  
  /**
   * 
   * @param inputFileName
   * @param outputFileName
   * @param level
   * @throws IOException
   * @throws InterruptedException
   */
  public void optimize(String inputFileName, String outputFileName, CompilationLevel level) 
      throws IOException, InterruptedException {
    
    // ensure our output dir exists
    String outDir = outputFileName.substring(0, outputFileName.lastIndexOf('/'));
    new File(outDir).mkdirs();
    
    // read in the javascript source
    byte[] buffer = new byte[(int) new File(inputFileName).length()];
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFileName));
    in.read(buffer);
    in.close();
    String source = new String(buffer);
    
    // create our closure compiler
    Compiler compiler = new Compiler();
    
    // TODO allow closure options to be specified
    CompilerOptions options = new CompilerOptions();
    level.setOptionsForCompilationLevel(options);
    
    JSSourceFile sourceFile = JSSourceFile.fromCode(inputFileName, source.toString());
    
    // TODO allow externs.js file to be specified
    JSSourceFile externFile = JSSourceFile.fromCode("externs.js", "function alert(x) {}");
    
    compiler.compile(externFile, sourceFile, options);
    String result = compiler.toSource();
    
    // write the result to the output file
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File(outputFileName)));
    output.write(result.getBytes());
    output.close();
  }
}
