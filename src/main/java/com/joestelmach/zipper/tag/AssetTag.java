package com.joestelmach.zipper.tag;
import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.joestelmach.util.FileSearcher;
import com.joestelmach.zipper.plugin.ConfigKey;

public class AssetTag extends SimpleTagSupport {
  private String _type;
  private String _name;
  private String _media = "screen";
  private FileSearcher _searcher = new FileSearcher();
  
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
    
    String environment = getEnvironment();
    if(environment == null || environment.length() == 0 || environment.equals("development")) {
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
   * @return the running environment set either as a system
   * property or configured in zipper.properties
   */
  private String getEnvironment() {
    String key = ConfigKey.ENVIRONMENT.getKey();
    String environment =  System.getProperty(key);
    if(environment == null) environment = _configuration.getString(key);
    return environment;
  }
  
  /**
   * 
   * @throws IOException
   */
  private void writeProduction() throws IOException {
    // determine where the file is
    String baseDir = _configuration.getString(ConfigKey.OUTPUT_DIR.getKey(), "assets");
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
    
    PageContext context = (PageContext) getJspContext();
    
    // first find the absolute path to zipper's output directory
    // inside the webapp root
    String outputDir = _configuration.getString(ConfigKey.OUTPUT_DIR.getKey(), "assets");
    String baseDirPath = context.getServletContext().getRealPath("/") + "/" + outputDir;
    
    // now find the configured asset patterns
    @SuppressWarnings("unchecked")
    List<String> patterns = _configuration.getList(_type + ".asset." + _name);
    
    // and find all files that match each pattern, and generate
    // the appropriate html to include the asset on the page.
    JspWriter writer = getJspContext().getOut();
    for(String pattern:patterns) {
      for(String include:_searcher.search(pattern, baseDirPath)) {
        include = include.substring(baseDirPath.length());
        writer.write(_type.equals("css") ? getCssInclude(include) : getJsInclude(include));
        writer.write("\n");
      }
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
