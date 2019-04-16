/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.buck.cli;

import com.facebook.nailgun.NGContext;
import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;

/**
 * The abstract entry point of Buck commands for both {@link MainWithoutNailgun} and {@link
 * MainWithNailgun}
 */
abstract class AbstractMain {

  protected final PrintStream stdOut;
  protected final PrintStream stdErr;
  protected final InputStream stdIn;

  protected final ImmutableMap<String, String> clientEnvironment;

  private final Optional<NGContext> optionalNGContext; // TODO(bobyf): remove this dependency.

  protected AbstractMain(
      PrintStream stdOut,
      PrintStream stdErr,
      InputStream stdIn,
      ImmutableMap<String, String> clientEnvironment,
      Optional<NGContext> ngContext) {
    this.stdOut = stdOut;
    this.stdErr = stdErr;
    this.stdIn = stdIn;

    this.clientEnvironment = clientEnvironment;
    this.optionalNGContext = ngContext;
  }

  /**
   * @return an initialized {@link MainRunner} for running the buck command, with all the base state
   *     setup.
   */
  protected MainRunner prepareMainRunner() {
    return new MainRunner(stdOut, stdErr, stdIn, clientEnvironment, optionalNGContext);
  }
}