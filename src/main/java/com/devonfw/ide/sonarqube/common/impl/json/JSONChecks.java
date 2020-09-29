package com.devonfw.ide.sonarqube.common.impl.json;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.json.api.CustomJSONRulesDefinition;
import org.sonar.plugins.json.api.JSONCheck;
import org.sonar.plugins.json.api.visitors.TreeVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JSONChecks {

  private final CheckFactory checkFactory;
  private Set<Checks<JSONCheck>> checksByRepository = Sets.newHashSet();

  private JSONChecks(CheckFactory checkFactory) {
    this.checkFactory = checkFactory;
  }

  public static JSONChecks createJSONCheck(CheckFactory checkFactory) {
    return new JSONChecks(checkFactory);
  }

  public JSONChecks addChecks(String repositoryKey, Iterable<Class> checkClass) {
    checksByRepository.add(checkFactory
        .<JSONCheck>create(repositoryKey)
        .addAnnotatedChecks(checkClass));

    return this;
  }

  public JSONChecks addCustomChecks(@Nullable CustomJSONRulesDefinition[] customRulesDefinitions) {
    if (customRulesDefinitions != null) {

      for (CustomJSONRulesDefinition rulesDefinition : customRulesDefinitions) {
        addChecks(rulesDefinition.repositoryKey(), Lists.newArrayList(rulesDefinition.checkClasses()));
      }
    }
    return this;
  }

  public List<JSONCheck> all() {
    List<JSONCheck> allVisitors = Lists.newArrayList();

    for (Checks<JSONCheck> checks : checksByRepository) {
      allVisitors.addAll(checks.all());
    }

    return allVisitors;
  }

  public List<TreeVisitor> visitorChecks() {
    List<TreeVisitor> checks = new ArrayList<>();
    for (JSONCheck check : all()) {
      if (check instanceof TreeVisitor) {
        checks.add((TreeVisitor) check);
      }
    }

    return checks;
  }

  @Nullable
  public RuleKey ruleKeyFor(JSONCheck check) {
    RuleKey ruleKey;

    for (Checks<JSONCheck> checks : checksByRepository) {
      ruleKey = checks.ruleKey(check);

      if (ruleKey != null) {
        return ruleKey;
      }
    }
    return null;
  }

}
