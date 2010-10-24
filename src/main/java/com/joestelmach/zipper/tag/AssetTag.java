package com.joestelmach.zipper.tag;
import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.joestelmach.zipper.plugin.OptionKey;

public class AssetTag extends SimpleTagSupport {
  private String _type;
  private String _name;
  private String _media = "screen";
  
  private static Configuration _configuration;
  static {
    try {
      _configuration = new PropertiesConfiguration("zipper.properties");
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * 
   */
  public void doTag() throws JspException, IOException {
    if(!_type.equals("css") && !_type.equals("js")) return;
    String environment = System.getProperty("environment");
    if(environment.length() == 0 || environment.equals("development")) {
      writeDevelopment();
    }
    else {
      writeProduction();
    }
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
  
  /**
   * 
   * @param media
   */
  public void setMedia(String media) {
    _media = media;
  }
  
  /**
   * 
   * @throws IOException
   */
  private void writeProduction() throws IOException {
    // determine where the file is
    String baseDir = _configuration.getString(OptionKey.OUTPUT_DIR.getValue(), "assets");
    _name = "/" + baseDir + "/" + _name;
    
    JspWriter writer = getJspContext().getOut();
    String html = _type.equals("css") ? getCssInclude(_name + ".css") : getJsInclude(_name + ".js");
    writer.write(html + "\n");
  }
  
  /**
   * 
   * @throws IOException
   */
  private void writeDevelopment() throws IOException {
    JspWriter writer = getJspContext().getOut();
    @SuppressWarnings("unchecked")
    List<String> includes = _configuration.getList(_type + ".asset." + _name);
    for(String include:includes) {
      String html = _type.equals("css") ? getCssInclude(include) : getJsInclude(include);
      writer.write(html + "\n");
    }
  }
  
  /**
   * 
   * @param file
   * @return
   */
  private String getCssInclude(String file) {
    return "<link rel=\"stylesheet\" type=\"text/css\" media=\"" + _media + "\" href=\"" + file + "\" />";
  }
  
  /**
   * 
   * @param file
   * @return
   */
  private String getJsInclude(String file) {
    return "<script type=\"text/javascript\" src=\"" + file  + "\"></script>";
  }
}
