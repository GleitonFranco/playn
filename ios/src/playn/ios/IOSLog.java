/**
 * Copyright 2012 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package playn.ios;

import cli.System.Console;

import playn.core.Log;

class IOSLog implements Log
{
  // TODO: stack traces

  @Override
  public void debug(String msg) {
    Console.WriteLine("DEBUG: " + msg);
  }

  @Override
  public void debug(String msg, Throwable e) {
    debug(msg + ": " + e.getMessage());
  }

  @Override
  public void info(String msg) {
    Console.WriteLine(msg);
  }

  @Override
  public void info(String msg, Throwable e) {
    info(msg + ": " + e.getMessage());
  }

  @Override
  public void warn(String msg) {
    Console.WriteLine("WARN: " + msg);
  }

  @Override
  public void warn(String msg, Throwable e) {
    warn(msg + ": " + e.getMessage());
  }

  @Override
  public void error(String msg) {
    Console.WriteLine("ERROR: " + msg);
  }

  @Override
  public void error(String msg, Throwable e) {
    error(msg + ": " + e.getMessage());
  }
}
