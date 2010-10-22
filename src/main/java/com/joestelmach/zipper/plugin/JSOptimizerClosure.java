package com.joestelmach.zipper.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class JSOptimizerClosure {
  public JSOptimizerClosure() {
    unpackCompiler();
  }
  
  /**
   * 
   * @param inputFileName
   * @param options 
   * @throws IOException
   * @throws InterruptedException
   */
  public void optimize(String inputFileName, String outputFileName, String options) throws IOException, InterruptedException {
    options = options != null ? options : "";
    String command = "java -jar /tmp/compiler.jar " + options + " --js " + inputFileName + 
      " --js_output_file " + outputFileName;
    Process proc = Runtime.getRuntime().exec(command);
    
    // print any errors to the console
    BufferedReader in = new BufferedReader(  
    new InputStreamReader(proc.getErrorStream()));  
    String line = null;  
    while ((line = in.readLine()) != null) {  
      System.out.println(line);  
    } 
    
    // wait for the process to finish
    proc.waitFor();
  }
  
  /**
   * Unfortunately, google has decided not to place their compiler in any
   * publicly available maven repository.  Since I want this plugin to 
   * have no class path or custom repository dependencies, I've decided
   * to bundle the compiler with the plugin and unpack it to the /tmp
   * directory for use during execution.
   * 
   * see: http://code.google.com/p/closure-compiler/issues/detail?id=37
   */
  private void unpackCompiler() {
    // write the closure compiler to the build dir
    InputStream in = new BufferedInputStream(ZipperMojo.class.getResourceAsStream("/compiler.jar"));
    try {
      File file = new File("/tmp/compiler.jar");
      //file.deleteOnExit();
      OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
      while(in.available() > 0) {
        output.write(in.read());
      }
      output.close();
      in.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
  

}
