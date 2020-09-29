package com.devonfw.ide.sonarqube.common.impl.json;

import org.sonar.api.resources.AbstractLanguage;

public class DevonJSONLanguage extends AbstractLanguage {

  public static final String KEY = "json";

  public DevonJSONLanguage() {
    super(KEY, "JSON");
  }

  @Override
  public String[] getFileSuffixes() {
    return new String[] {"json"};
  }

}
