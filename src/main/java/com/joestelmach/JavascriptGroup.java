package com.joestelmach;

import java.util.List;

public class JavascriptGroup {
  
  /**
   * @property
   */
  private String name;
  
  /**
   * @property
   */
  private List<String> includes;
  
  /**
   * @property
   */
  private List<String> excludes;

  public String getName() {
    return name;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }
}
