package com.devonfw.ide.sonarqube.common.impl.json;

import com.devonfw.ide.sonarqube.common.impl.check.component.DevonArchitectureJsonFileCheck;
import com.google.common.collect.ImmutableList;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.squidbridge.annotations.AnnotationBasedProfileBuilder;

public class JSONProfile extends ProfileDefinition {

  private final RuleFinder ruleFinder;

  public static final String SONARQUBE_WAY_PROFILE_NAME = "SonarQube Way";

  public JSONProfile(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages messages) {
    AnnotationBasedProfileBuilder annotationBasedProfileBuilder = new AnnotationBasedProfileBuilder(ruleFinder);
    return annotationBasedProfileBuilder.build(
        "json",
        SONARQUBE_WAY_PROFILE_NAME,
        DevonJSONLanguage.KEY,
        ImmutableList.of(DevonArchitectureJsonFileCheck.class),
        messages);
  }

}
