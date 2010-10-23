package com.joestelmach.zipper.plugin;

import java.util.HashMap;
import java.util.Map;

public enum OptionKey {
  WEB_ROOT("webroot"),
  LINT_SKIP("lint.skip"),
  LINT_EXCLUDES("lint.exclude"),
  LINT_OPTION_PREFIX("lint.option"),
  LINT_FAIL_ON_WARNING("lint.failonwarning"),
  JS_OPTIMIZE_OPTIONS("js.optimize.options"),
  CSS_LINE_BREAK("css.line.break"),
  OUTPUT_DIR("output.dir"),
  JS_ASSET_PREFIX("js.asset"),
  CSS_ASSET_PREFIX("css.asset"),
  GZIP("gzip");
  
  private static final Map<String, OptionKey> VALUE_MAP;
  
  static {
    VALUE_MAP = new HashMap<String, OptionKey>();
    for(OptionKey key:OptionKey.values()) {
      VALUE_MAP.put(key.getValue(), key);
    }
  }
  
  private String _value;
  
  private OptionKey(String value) {
    _value = value;
  }
  
  public String getValue() {
    return _value;
  }
}
