package com.devonfw.ide.sonarqube.common.impl.check.layer;

import org.sonar.check.Priority;
import org.sonar.check.Rule;

import com.devonfw.ide.sonarqube.common.api.JavaType;
import com.devonfw.ide.sonarqube.common.impl.check.DevonArchitectureCheck;
import com.devonfw.ide.sonarqube.common.impl.check.DevonArchitectureImportCheck;

/**
 * {@link DevonArchitectureCheck} verifying that code from service layer does not depend on dataaccess layer.
 */
@Rule(key = "L8", name = "devonfw Layer Service-Dataaccess Check", //
    priority = Priority.CRITICAL, tags = { "architecture-violation", "devonfw", "layer" })
public class DevonArchitectureLayerService2DataaccessCheck extends DevonArchitectureImportCheck {

  @Override
  protected String checkDependency(JavaType source, JavaType target) {

    if (source.isLayerService() && target.isLayerDataAccess()) {
      if (target.toString().equals("com.devonfw.module.jpa.dataaccess.api.RevisionMetadata")) {
        return null; // specific exclusion for unclean packaging
      }
      return "Code from service layer shall not depend on dataaccess layer. ('" + source.getComponent() + "."
          + source.getLayer() + "' is dependent on '" + target.getComponent() + "." + target.getLayer() + "')";
    }
    return null;
  }

}