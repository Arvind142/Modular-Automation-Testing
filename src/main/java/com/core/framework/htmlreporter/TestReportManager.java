package com.core.framework.htmlreporter;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.core.framework.listener.Listener;
import com.core.framework.testLogs.TableLog;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import java.util.Properties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class TestReportManager {
    private static final ThreadLocal<ExtentTest> extentTestThreadLocal = new ThreadLocal<>();

    private static Reporter reporter;

    private static Map<String,Integer> listOfTCs;

    public static void initializeReporting(String outputFolder){
        reporter = Reporter.initializeReporting(outputFolder);
        listOfTCs = new ConcurrentHashMap<>();
    }

    public static void onTestStart(String testName,String description) {
        if(description!=null){
            description=description.replaceAll(" ", "&nbsp;");
        }
        onTestStart(testName,description,null);
    }

    public static void onTestStart(String testName,String description,String author) {
        onTestStart(testName,description,author, (String[]) null);
    }
    public static void onTestStart(String testName,String description,String author,String... category) {
        synchronized (TestReportManager.class){
            if(listOfTCs.containsKey(testName)){
                listOfTCs.replace(testName,listOfTCs.get(testName)+1);
                testName = testName+" [ Invocation: "+listOfTCs.get(testName)+"]";
            }
            else{
                listOfTCs.put(testName,1);
            }
        }
        extentTestThreadLocal.set(reporter.getExtentReport().createTest(testName,description));
        assignAuthor(author);
        assignCategory(category);
        assignDevice();
    }

    public static void checkAndAddParametersToReport(ITestResult result){
        if(result.getParameters().length!=0){
            extentTestThreadLocal.get().log(Status.INFO,MarkupHelper.createTable(new String[][]{
                    {"Parameters as follow"},
                    Listener.getParameter(result),
            }));
        }
    }

    public static void checkAndAddRetryReport(ITestResult result){
        if(result.wasRetried()){
            extentTestThreadLocal.get().log(Status.INFO,MarkupHelper.createLabel("TestCase will be retried", ExtentColor.AMBER));
        }
    }

    public static void setSystemVars(Properties pros){
        reporter.setSystemVars(pros);
    }

    public static String getReportingFolder(){
        return reporter.getReportingFolder();
    }
    public static void log(Status status,String message){
        extentTestThreadLocal.get().log(status,message);
    }

    public static <T> void log(String stepDescription, T expected, T actual, String evidence) {
        TableLog testLog = TableLog.log(stepDescription, expected, actual, evidence);
        extentTestThreadLocal.get().log(testLog.getLogStatus(), testLog.getEquivalent());
    }

    public static <T> void log(String stepDescription, T expected, T actual) {
        TableLog testLog = TableLog.log(stepDescription, expected, actual);
        log.trace(testLog.toString());
        extentTestThreadLocal.get().log(testLog.getLogStatus(), testLog.getEquivalent());
    }

    public static <T> void log(String stepDescription, T expected, T actual, WebDriver driver) {
        TableLog testLog = TableLog.log(stepDescription, expected, actual, takeScreenShotWebPage(driver,stepDescription));
        extentTestThreadLocal.get().log(testLog.getLogStatus(), testLog.getEquivalent());
    }

    public static void stopReporting(){
        reporter.stopReporting();
    }

    public static void assignAuthor(String author){
        if(author!=null){
            author=author.replaceAll(" ", "&nbsp;");
        }
        extentTestThreadLocal.get().assignAuthor(author==null?(System.getProperty("user.name")):author);
    }

    public static void assignDevice(){
        if (reporter.getDeviceDetails() != null) {
            extentTestThreadLocal.get().assignDevice(reporter.getDeviceDetails());
        }
    }

    public static void assignCategory(String... category){
        extentTestThreadLocal.get().assignCategory(category);
    }

    public static String takeScreenShotWebPage(WebDriver driver, String fileName) {
        return reporter.takeScreenShotWebPage(driver,fileName);
    }
}
