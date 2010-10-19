package com.joestelmach;

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

  public List<String> getIncludes() {
    return includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }
  
  public boolean getGzip() {
    return gzip;
  }
}
