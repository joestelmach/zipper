package com.joestelmach.zipper.tag;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

public class AssetTag implements Tag {
  private PageContext _pageContext;
  private Tag _parent;

  public int doStartTag() throws JspException {
    try {
      String environment = System.getProperty("deployed.environment");
      _pageContext.getOut().print("env: " + environment);
      
    } catch (IOException e) {
      throw new JspException("Error:  IOException while writing to client" + e.getMessage());
    }
    
    return SKIP_BODY;
  }
  
  public int doEndTag() throws JspException {
    return SKIP_PAGE;
  }

  public Tag getParent() {
    return _parent;
  }

  public void release() {
    
  }

  public void setPageContext(PageContext pageContext) {
    _pageContext = pageContext;
  }

  public void setParent(Tag parent) {
    _parent = parent;
  }
}
