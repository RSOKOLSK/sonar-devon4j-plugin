package com.devonfw.ide.sonarqube.common.impl.json;

import com.devonfw.ide.sonarqube.common.impl.check.component.DevonArchitectureJsonFileCheck;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.typed.ActionParser;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.json.parser.JSONParserBuilder;
import org.sonar.json.visitors.CharsetAwareVisitor;
import org.sonar.json.visitors.JSONVisitorContext;
import org.sonar.json.visitors.SyntaxHighlighterVisitor;
import org.sonar.json.visitors.metrics.MetricsVisitor;
import org.sonar.plugins.json.api.CustomJSONRulesDefinition;
import org.sonar.plugins.json.api.JSONCheck;
import org.sonar.plugins.json.api.tree.JsonTree;
import org.sonar.plugins.json.api.tree.Tree;
import org.sonar.plugins.json.api.visitors.TreeVisitor;
import org.sonar.plugins.json.api.visitors.issue.Issue;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.api.AnalysisException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JSONSquidSensor implements Sensor {

  private static final Logger LOG = Loggers.get(JSONSquidSensor.class);

  private final FileSystem fileSystem;
  private final JSONChecks checks;
  private final ActionParser<Tree> parser;
  private final FilePredicate mainFilePredicate;
  private IssueSaver issueSaver;
  private RuleKey parsingErrorRuleKey = null;

  public JSONSquidSensor(FileSystem fileSystem, CheckFactory checkFactory) {
    this(fileSystem, checkFactory, null);
  }

  public JSONSquidSensor(FileSystem fileSystem, CheckFactory checkFactory, @Nullable CustomJSONRulesDefinition[] customRulesDefinition) {
    this.fileSystem = fileSystem;

    this.mainFilePredicate = fileSystem.predicates().and(
        fileSystem.predicates().hasType(InputFile.Type.MAIN),
        fileSystem.predicates().hasLanguage(DevonJSONLanguage.KEY));

    this.parser = JSONParserBuilder.createParser(fileSystem.encoding());

    this.checks = JSONChecks.createJSONCheck(checkFactory)
        .addChecks("json", ImmutableList.of(
            DevonArchitectureJsonFileCheck.class))
        .addCustomChecks(customRulesDefinition);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
        .onlyOnLanguage(DevonJSONLanguage.KEY)
        .name("JSON Squid Sensor")
        .onlyOnFileType(InputFile.Type.MAIN);
  }

  @Override
  public void execute(SensorContext sensorContext) {
    List<TreeVisitor> treeVisitors = Lists.newArrayList();
    treeVisitors.addAll(checks.visitorChecks());
    treeVisitors.add(new SyntaxHighlighterVisitor(sensorContext));
    treeVisitors.add(new MetricsVisitor(sensorContext));

    ProgressReport progressReport = new ProgressReport("Report about progress of JSON analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(Lists.newArrayList(fileSystem.files(mainFilePredicate)));

    issueSaver = new IssueSaver(sensorContext, checks);
    List<Issue> issues = new ArrayList<>();

    boolean success = false;
    try {
      for (InputFile inputFile : fileSystem.inputFiles(mainFilePredicate)) {
        LOG.debug("Analyzing " + inputFile.absolutePath() + "...");
        issues.addAll(analyzeFile(sensorContext, inputFile, treeVisitors));
        progressReport.nextFile();
      }
      saveSingleFileIssues(issues);
      success = true;
    } finally {
      stopProgressReport(progressReport, success);
    }
  }

  private List<Issue> analyzeFile(SensorContext sensorContext, InputFile inputFile, List<TreeVisitor> visitors) {
    try {
      JsonTree jsonTree = (JsonTree) parser.parse(new File(inputFile.absolutePath()));
      return scanFile(inputFile, jsonTree, visitors);

    } catch (RecognitionException e) {
      checkInterrupted(e);
      LOG.error("Unable to parse file: " + inputFile.absolutePath());
      LOG.error(e.getMessage());
      processRecognitionException(e, sensorContext, inputFile);

    } catch (Exception e) {
      checkInterrupted(e);
      throw new AnalysisException("Unable to analyse file: " + inputFile.absolutePath(), e);
    }
    return new ArrayList<>();
  }

  private List<Issue> scanFile(InputFile inputFile, JsonTree json, List<TreeVisitor> visitors) {
    JSONVisitorContext context = new JSONVisitorContext(json, inputFile.file());
    List<Issue> issues = new ArrayList<>();
    for (TreeVisitor visitor : visitors) {
      if (visitor instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) visitor).setCharset(fileSystem.encoding());
      }
      if (visitor instanceof JSONCheck) {
        issues.addAll(((JSONCheck) visitor).scanFile(context));
      } else {
        visitor.scanTree(context);
      }
    }
    return issues;
  }

  private void saveSingleFileIssues(List<Issue> issues) {
    for (Issue issue : issues) {
      issueSaver.saveIssue(issue);
    }
  }

  private void processRecognitionException(RecognitionException e, SensorContext sensorContext, InputFile inputFile) {
    if (parsingErrorRuleKey != null) {
      NewIssue newIssue = sensorContext.newIssue();

      NewIssueLocation primaryLocation = newIssue.newLocation()
          .message(e.getMessage())
          .on(inputFile)
          .at(inputFile.selectLine(e.getLine()));

      newIssue
          .forRule(parsingErrorRuleKey)
          .at(primaryLocation)
          .save();
    }
  }

  private static void stopProgressReport(ProgressReport progressReport, boolean success) {
    if (success) {
      progressReport.stop();
    } else {
      progressReport.cancel();
    }
  }

  private static void checkInterrupted(Exception e) {
    Throwable cause = Throwables.getRootCause(e);
    if (cause instanceof InterruptedException || cause instanceof InterruptedIOException) {
      throw new AnalysisException("Analysis cancelled", e);
    }
  }

}
