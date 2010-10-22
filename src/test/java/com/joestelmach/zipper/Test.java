package com.joestelmach.zipper;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.joestelmach.zipper.plugin.PackMojo;

public class Test {
  
  public static void main(String[] args) throws MojoExecutionException, MojoFailureException {
    new PackMojo().execute();
    
  }
}
