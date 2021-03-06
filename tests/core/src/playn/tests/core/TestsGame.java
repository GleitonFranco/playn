/**
 * Copyright 2011 The PlayN Authors
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
package playn.tests.core;

import playn.core.Color;
import playn.core.Game;
import playn.core.ImmediateLayer;
import playn.core.Pointer;
import playn.core.Surface;
import static playn.core.PlayN.*;

public class TestsGame implements Game {
  Test[] tests = new Test[] {
    new CanvasTest(),
    new ImmediateTest(),
    new ImageTypeTest(),
    new AlphaLayerTest(),
    new DepthTest(),
    new ClearBackgroundTest(),
    /*new YourTest(),*/
  };
  int currentTest;

  @Override
  public void init() {
    // display basic instructions
    log().info("Click or touch to go to the next test.");

    // add a listener for pointer (mouse, touch) input
    pointer().setListener(new Pointer.Adapter() {
      @Override
      public void onPointerStart(Pointer.Event event) {
        nextTest();
      }
    });

    currentTest = -1;
    nextTest();
  }

  Test currentTest() {
    return tests[currentTest];
  }

  void nextTest() {
    currentTest = (currentTest + 1) % tests.length;

    // setup root layer for next test
    graphics().rootLayer().clear();
    ImmediateLayer bg = graphics().createImmediateLayer(new ImmediateLayer.Renderer() {
      public void render(Surface surf) {
        surf.setFillColor(Color.rgb(255, 255, 255));
        surf.fillRect(0, 0, graphics().width(), graphics().height());
      }
    });
    bg.setDepth(Float.NEGATIVE_INFINITY); // render behind everything
    graphics().rootLayer().add(bg);

    log().info("Starting " + currentTest().getName());
    log().info(" Description: " + currentTest().getDescription());
    currentTest().init();
  }

  @Override
  public void paint(float alpha) {
    currentTest().paint(alpha);
  }

  @Override
  public void update(float delta) {
    currentTest().update(delta);
  }

  @Override
  public int updateRate() {
    return currentTest().updateRate();
  }
}
