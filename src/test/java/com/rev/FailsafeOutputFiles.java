package com.rev;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.PrintStreamLogger;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.TestSuiteXmlParser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class FailsafeOutputFiles {

  protected static final Logger LOG = LoggerFactory.getLogger(FailsafeOutputFiles.class);

  private final ConsoleLogger consoleLogger = new PrintStreamLogger(new PrintStream(System.out));

  @Test
  public void canBeParsed() {
    TestSuiteXmlParser testSuiteXmlParser = new TestSuiteXmlParser(consoleLogger);
    List<ReportTestSuite> testSuites = null;
    try {
      testSuites = testSuiteXmlParser.parse("resources/failsafe-output.xml");
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (testSuites == null) {
      throw new RuntimeException("File was null!");
    }
    for (ReportTestSuite reportTestSuite : testSuites) {
      List<ReportTestCase> testCases = reportTestSuite.getTestCases();
      for (ReportTestCase reportTestCase : testCases) {
        LOG.info(
            "Test name ["
                + reportTestCase.getName()
                + "] Full test name ["
                + reportTestCase.getFullName()
                + "] Test time ["
                + reportTestCase.getTime()
                + "] Pass/Fail/Skip ["
                + getPassFailError(reportTestCase)
                + "].");
      }
    }
  }

  private String getPassFailError(ReportTestCase reportTestCase) {
    if (reportTestCase.isSuccessful()) {
      return "Pass";
    } else if (reportTestCase.hasError() || reportTestCase.hasFailure()) {
      return "Fail";
    } else if (reportTestCase.hasSkipped()) {
      return "Skip";
    } else {
      return "";
    }
  }
}
