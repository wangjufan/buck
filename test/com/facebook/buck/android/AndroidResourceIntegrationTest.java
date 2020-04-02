/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.buck.android;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.core.model.BuildTargetFactory;
import com.facebook.buck.core.model.impl.BuildTargetPaths;
import com.facebook.buck.testutil.JsonMatcher;
import com.facebook.buck.testutil.ProcessResult;
import com.facebook.buck.testutil.TemporaryPaths;
import com.facebook.buck.testutil.integration.ProjectWorkspace;
import com.facebook.buck.testutil.integration.TestDataHelper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AndroidResourceIntegrationTest {

  @Rule public TemporaryPaths tmpFolder = new TemporaryPaths();

  private ProjectWorkspace workspace;

  @Before
  public void setUp() throws IOException {
    workspace =
        TestDataHelper.createProjectWorkspaceForScenario(this, "android_resource", tmpFolder);
    workspace.setUp();
  }

  @Test
  public void testOnlyUsesFirstOrderResources() throws IOException {
    ProcessResult result = workspace.runBuckBuild("//res1:res");
    result.assertFailure();
    assertTrue(result.getStderr().contains("The following resources were not found"));
    assertTrue(result.getStderr().contains("another_name"));
    workspace.replaceFileContents("res1/BUCK", "#EXTRA_DEP_HERE", "'//res3:res',");
    workspace.runBuckBuild("//res1:res").assertSuccess();
  }

  @Test
  public void testGeneratedResourceDirectory() throws IOException {
    // Verify we correctly build the R.txt file using a generated input resource directory.
    workspace.runBuckBuild("//generated_res:res").assertSuccess();
    String output =
        Splitter.on(' ')
            .trimResults()
            .splitToList(
                workspace
                    .runBuckCommand("targets", "--show-output", "//generated_res:res")
                    .assertSuccess()
                    .getStdout())
            .get(1);
    assertThat(
        Files.readAllLines(workspace.getPath(output).resolve("R.txt"), StandardCharsets.UTF_8),
        Matchers.contains("int string another_name 0x7f010002", "int string some_name 0x7f010001"));

    // Add a new item in the input and verify that the resource rule gets re-run.
    Files.createDirectory(workspace.getPath("generated_res/input_res/raw"));
    workspace.writeContentsToPath("", "generated_res/input_res/raw/empty.txt");
    workspace.runBuckBuild("//generated_res:res").assertSuccess();
    workspace.getBuildLog().assertTargetBuiltLocally("//generated_res:res");
  }

  @Test
  public void testAndroidResourceIndex() throws IOException {
    AssumeAndroidPlatform.get(workspace).assumeSdkIsAvailable();

    // Verify we correctly build the json file using a generated input resource directory.
    String buildTarget =
        "//generated_res:res#" + AndroidResourceDescription.ANDROID_RESOURCE_INDEX_FLAVOR;
    workspace.runBuckBuild(buildTarget);

    String resourceIndexJson =
        StandardCharsets.US_ASCII
            .decode(
                ByteBuffer.wrap(
                    Files.readAllBytes(
                        workspace
                            .getGenPath(BuildTargetFactory.newInstance(buildTarget), "__%s")
                            .resolve("resource_index.json"))))
            .toString();

    String resourceFilePath =
        BuildTargetPaths.getGenPath(
                workspace.getProjectFileSystem(),
                BuildTargetFactory.newInstance("//generated_res:gen-res"),
                "%s")
            .resolve("res/values/strings.xml")
            .toString();

    assertThat(
        resourceIndexJson,
        JsonMatcher.fromJsonObject(
            ImmutableList.of(
                ImmutableMap.of(
                    "type",
                    "STRING",
                    "name",
                    "another_name",
                    "lineNumber",
                    3,
                    "columnNumber",
                    30,
                    "resourceFilePath",
                    resourceFilePath),
                ImmutableMap.of(
                    "type",
                    "STRING",
                    "name",
                    "some_name",
                    "lineNumber",
                    2,
                    "columnNumber",
                    27,
                    "resourceFilePath",
                    resourceFilePath))));

    // Add a new item in the input and verify that the resource rule gets re-run.
    Files.createDirectory(workspace.getPath("generated_res/input_res/raw"));
    workspace.writeContentsToPath("", "generated_res/input_res/raw/empty.txt");
    workspace.runBuckBuild(buildTarget).assertSuccess();
    workspace.getBuildLog().assertTargetBuiltLocally(buildTarget);
  }

  @Test
  public void testResourcesAreNotIgnored() throws IOException {
    workspace.addBuckConfigLocalOption("project", "ignore", "buck-out/");
    Path output = workspace.buildAndReturnOutput("//res3:res");
    assertTrue(Files.isDirectory(output));
    Path rTxt = output.resolve("R.txt");
    assertTrue(Files.exists(rTxt));
    assertTrue(
        Joiner.on(System.lineSeparator()).join(Files.readAllLines(rTxt)).contains("some_name"));
    assertTrue(
        Joiner.on(System.lineSeparator()).join(Files.readAllLines(rTxt)).contains("another_name"));
  }
}
