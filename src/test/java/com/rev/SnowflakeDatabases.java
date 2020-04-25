package com.rev;

import net.snowflake.ingest.SimpleIngestManager;
import net.snowflake.ingest.connection.HistoryResponse;
import net.snowflake.ingest.connection.IngestResponse;
import net.snowflake.ingest.utils.StagedFileWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SnowflakeDatabases {
  private final String TEST_FILE_NAME = "test1.csv";

  private String testFilePath = null;

  private String tableName = "";
  private String pipeName = "";
  private String stageName = "";

  /** Create test table and pipe */
  @Before
  public void beforeAll() throws Exception {

    // get test file path

    URL resource = TestUtils.class.getResource(TEST_FILE_NAME);
    testFilePath = resource.getFile();

    // create stage, pipe, and table
    Random rand = new Random();

    Long num = Math.abs(rand.nextLong());

    tableName = "ingest_sdk_test_table_" + num;

    pipeName = "ingest_sdk_test_pipe_" + num;

    stageName = "ingest_sdk_test_stage_" + num;

    TestUtils.executeQuery("create or replace table " + tableName + " (str string, num int)");

    TestUtils.executeQuery("create or replace stage " + stageName);

    TestUtils.executeQuery(
        "create or replace pipe "
            + pipeName
            + " as copy into "
            + tableName
            + " from @"
            + stageName);
  }

  /** Remove test table and pipe */
  @After
  public void afterAll() {
    TestUtils.executeQuery("drop pipe if exists " + pipeName);

    TestUtils.executeQuery("drop stage if exists " + stageName);

    TestUtils.executeQuery("drop table if exists " + tableName);
  }

  /** ingest test example ingest a simple file and check load history. */
  @Test
  public void testSimpleIngest() throws Exception {
    // put
    TestUtils.executeQuery("put file://" + testFilePath + " @" + stageName);

    // keeps track of whether we've loaded the file
    boolean loaded = false;

    // create ingest manager
    SimpleIngestManager manager = TestUtils.getManager(pipeName);

    // create a file wrapper
    StagedFileWrapper myFile = new StagedFileWrapper(TEST_FILE_NAME, null);

    // get an insert response after we submit
    IngestResponse insertResponse = manager.ingestFile(myFile, null);

    assertEquals("SUCCESS", insertResponse.getResponseCode());

    // create a new thread
    ExecutorService service = Executors.newSingleThreadExecutor();

    // fork off waiting for a load to the service
    Future<?> result =
        service.submit(
            () -> {
              String beginMark = null;

              while (true) {

                try {
                  Thread.sleep(5000);
                  HistoryResponse response = manager.getHistory(null, null, beginMark);

                  if (response != null && response.getNextBeginMark() != null) {
                    beginMark = response.getNextBeginMark();
                  }
                  if (response != null && response.files != null) {
                    for (HistoryResponse.FileEntry entry : response.files) {
                      // if we have a complete file that we've
                      // loaded with the same name..
                      String filename = entry.getPath();
                      if (entry.getPath() != null
                          && entry.isComplete()
                          && filename.equals(TEST_FILE_NAME)) {
                        return;
                      }
                    }
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    // try to wait until the future is done
    try {
      // wait up to 2 minutes to load
      result.get(2, TimeUnit.MINUTES);
      loaded = true;
    } finally {
      assertTrue(loaded);
    }
  }
}
