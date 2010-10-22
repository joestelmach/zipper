package com.joestelmach.zipper.tag;
import java.io.File;
import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;

public class AssetTag extends SimpleTagSupport {
  private String _type;
  private String _name;
  
  private static Configuration _configuration;
  static {
      try {
		_configuration = new PropertiesConfiguration("zipper.properties");
	} catch (ConfigurationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      ConfigurationUtils.dump(_configuration, System.out);
      
  }
  
  /**
   * 
   */
  public void doTag() throws JspException, IOException {
    getJspContext().getOut().write(_type + " " + _name);
    // first we find the asset in the configuration
    
  }
  
  /**
   * 
   * @param type
   */
  public void setType(String type) {
    _type = type;
  }
  
  /**
   * 
   * @param name
   */
  public void setName(String name) {
    _name = name;
  }
}
