package com.joestelmach.zipper.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a concatenated group of JavaScript or CSS assets
 * 
 * @author Joe Stelmach
 */
public class AssetGroup {
  private String name;
  private List<String> includes;
  private boolean gzip;

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public List<String> getIncludes() {
    return includes == null ? new ArrayList<String>() : includes;
  }
  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public boolean getGzip() {
    return gzip;
  }
  public void setGzip(boolean gzip) {
    this.gzip = gzip;
  }
}
