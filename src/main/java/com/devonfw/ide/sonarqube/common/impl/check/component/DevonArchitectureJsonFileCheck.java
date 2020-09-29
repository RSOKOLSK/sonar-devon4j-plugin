package com.devonfw.ide.sonarqube.common.impl.check.component;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.json.api.tree.JsonTree;
import org.sonar.plugins.json.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;


@Rule(
    key = "ArchitectureJsonCheck",
    name = "Architecture json file check",
    priority = Priority.MINOR,
    tags = { "json", "architecture" })
@SqaleConstantRemediation("10min")
@ActivatedByDefault
public class DevonArchitectureJsonFileCheck extends DoubleDispatchVisitorCheck {

    @Override
    public void visitJson(JsonTree tree) {
        addFileIssue("JSON CHECK");
        super.visitJson(tree);
    }
}