package com.joestelmach.zipper.tag;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class AssetTag extends SimpleTagSupport {
  
  private static Configuration _configuration;
  static {
    try {
      _configuration = new PropertiesConfiguration("zipper.properties");
      
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
  }
  
  public void doTag() throws JspException, IOException {
    getJspContext().getOut().write("Hello, world.");
  }
}
