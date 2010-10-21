package com.joestelmach.zipper.plugin;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGroup {
  /**
   * @property
   */
  private String name;
  
  /**
   * @property
   */
  protected List<String> includes;
  
  /**
   * @property
   */
  protected List<String> excludes;
  
  /**
   * @property
   */
  protected boolean gzip;

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

  public List<String> getExcludes() {
    return excludes == null ? new ArrayList<String>() : excludes;
  }
  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }
  
  public boolean getGzip() {
    return gzip;
  }
  public void setGzip(boolean gzip) {
    this.gzip = gzip;
  }
}
