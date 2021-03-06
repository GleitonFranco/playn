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
package playn.android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import android.util.Log;

import playn.core.InternalTransform;
import playn.core.gl.GL20;
import playn.core.gl.GLShader;

/**
 * Implements shaders for Android.
 */
public class AndroidGLShader implements GLShader
{
  static class Texture extends AndroidGLShader implements GLShader.Texture {
    private int uTexture, uAlpha, lastTex;
    private float lastAlpha;

    Texture(AndroidGLContext ctx) {
      super(ctx, TEX_FRAG_SHADER);
      uTexture = gl20.glGetUniformLocation(program, "u_Texture");
      uAlpha = gl20.glGetUniformLocation(program, "u_Alpha");
    }

    @Override
    public void flush() {
      gl20.glBindTexture(GL20.GL_TEXTURE_2D, lastTex);
      super.flush();
    }

    @Override
    public void prepare(Object texObj, float alpha) {
      ctx.checkGLError("textureShader.prepare start");
      if (super.prepare()) {
        gl20.glActiveTexture(GL20.GL_TEXTURE0);
        gl20.glUniform1i(uTexture, 0);
      }

      int tex = (Integer) texObj;
      if (tex == lastTex && alpha == lastAlpha)
        return;
      flush();

      gl20.glUniform1f(uAlpha, alpha);
      lastAlpha = alpha;
      lastTex = tex;
      ctx.checkGLError("textureShader.prepare end");
    }

  }

  static class Color extends AndroidGLShader implements GLShader.Color {
    private int uColor, uAlpha, lastColor;
    private FloatBuffer colors = FloatBuffer.allocate(4);
    private float lastAlpha;

    Color(AndroidGLContext ctx) {
      super(ctx, COLOR_FRAG_SHADER);
      uColor = gl20.glGetUniformLocation(program, "u_Color");
      uAlpha = gl20.glGetUniformLocation(program, "u_Alpha");
    }

    @Override
    public void prepare(int color, float alpha) {
      ctx.checkGLError("colorShader.prepare start");
      super.prepare();

      ctx.checkGLError("colorShader.prepare super called");

      if (color == lastColor && alpha == lastAlpha)
        return;
      flush();

      ctx.checkGLError("colorShader.prepare flushed");

      gl20.glUniform1f(uAlpha, alpha);
      lastAlpha = alpha;
      setColor(color);
      ctx.checkGLError("colorShader.prepare end");
    }

    private void setColor(int color) {
      float[] colorsArray = colors.array();
      colorsArray[3] = (float) ((color >> 24) & 0xff) / 255;
      colorsArray[0] = (float) ((color >> 16) & 0xff) / 255;
      colorsArray[1] = (float) ((color >> 8) & 0xff) / 255;
      colorsArray[2] = (float) ((color >> 0) & 0xff) / 255;
      // Still can't work out how to use glUniform4fv without generating a
      // glError, so passing the array through as individual floats
      gl20.glUniform4f(uColor, colorsArray[0], colorsArray[1], colorsArray[2], colorsArray[3]);

      lastColor = color;
    }
  }

  private static final int VERTEX_SIZE = 10; // 10 floats per vertex
  private static final int MAX_VERTS = 4;
  private static final int MAX_ELEMS = 6;
  private static final int FLOAT_SIZE_BYTES = 4;
  private static final int SHORT_SIZE_BYTES = 2;
  private static final int VERTEX_STRIDE = VERTEX_SIZE * FLOAT_SIZE_BYTES;

  protected final AndroidGLContext ctx;
  protected final AndroidGL20 gl20;
  protected final int program, uScreenSizeLoc, aMatrix, aTranslation, aPosition, aTexture;

  protected final FloatBuffer vertexData = ByteBuffer.allocateDirect(
    VERTEX_STRIDE * MAX_VERTS).order(ByteOrder.nativeOrder()).asFloatBuffer();
  protected final ShortBuffer elementData = ByteBuffer.allocateDirect(
    MAX_ELEMS * SHORT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer();
  protected int vertexBuffer, elementBuffer, vertexOffset, elementOffset;

  protected AndroidGLShader(AndroidGLContext ctx, String fragShader) {
    this.ctx = ctx;
    this.gl20 = ctx.gl20;
    program = createProgram(VERTEX_SHADER, fragShader);

    // glGet*() calls are slow; determine locations once.
    uScreenSizeLoc = gl20.glGetUniformLocation(program, "u_ScreenSize");
    aMatrix = gl20.glGetAttribLocation(program, "a_Matrix");
    aTranslation = gl20.glGetAttribLocation(program, "a_Translation");
    aPosition = gl20.glGetAttribLocation(program, "a_Position");
    aTexture = gl20.glGetAttribLocation(program, "a_Texture");

    // Create the vertex and index buffers
    int[] buffers = new int[2];
    gl20.glGenBuffers(2, buffers, 0);
    vertexBuffer = buffers[0];
    elementBuffer = buffers[1];
  }

  protected boolean prepare() {
    if (ctx.useShader(this) && gl20.glIsProgram(program)) {
      gl20.glUseProgram(program);
      ctx.checkGLError("Shader.prepare useProgram");
      // Couldn't get glUniform2fv to work for whatever reason.
      gl20.glUniform2f(uScreenSizeLoc, ctx.fbufWidth, ctx.fbufHeight);

      ctx.checkGLError("Shader.prepare uScreenSizeLoc vector set to " +
                       ctx.fbufWidth + " " + ctx.fbufHeight);

      gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vertexBuffer);
      gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);

      ctx.checkGLError("Shader.prepare BindBuffer");

      gl20.glEnableVertexAttribArray(aMatrix);
      gl20.glEnableVertexAttribArray(aTranslation);
      gl20.glEnableVertexAttribArray(aPosition);
      if (aTexture != -1)
        gl20.glEnableVertexAttribArray(aTexture);

      ctx.checkGLError("Shader.prepare AttribArrays enabled");

      gl20.glVertexAttribPointer(aMatrix, 4, GL20.GL_FLOAT, false, VERTEX_STRIDE, 0);
      gl20.glVertexAttribPointer(aTranslation, 2, GL20.GL_FLOAT, false, VERTEX_STRIDE, 16);
      gl20.glVertexAttribPointer(aPosition, 2, GL20.GL_FLOAT, false, VERTEX_STRIDE, 24);
      if (aTexture != -1)
        gl20.glVertexAttribPointer(aTexture, 2, GL20.GL_FLOAT, false, VERTEX_STRIDE, 32);
      ctx.checkGLError("Shader.prepare AttribPointer");
      return true;
    }
    return false;
  }

  @Override
  public void flush() {
    if (vertexOffset == 0) {
      return;
    }
    ctx.checkGLError("Shader.flush");
    gl20.glBufferData(GL20.GL_ARRAY_BUFFER, vertexOffset * FLOAT_SIZE_BYTES, vertexData,
                      GL20.GL_STREAM_DRAW);
    gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, elementOffset * SHORT_SIZE_BYTES,
                      elementData, GL20.GL_STREAM_DRAW);
    ctx.checkGLError("Shader.flush BufferData");
    gl20.glDrawElements(GL20.GL_TRIANGLE_STRIP, elementOffset, GL20.GL_UNSIGNED_SHORT, 0);
    vertexOffset = elementOffset = 0;
    ctx.checkGLError("Shader.flush DrawElements");
  }

  @Override
  public int beginPrimitive(int vertexCount, int elemCount) {
    int vertIdx = vertexOffset / VERTEX_SIZE;
    if ((vertIdx + vertexCount > MAX_VERTS) || (elementOffset + elemCount > MAX_ELEMS)) {
      flush();
      return 0;
    }
    return vertIdx;
  }

  @Override
  public void buildVertex(InternalTransform local, float dx, float dy) {
    buildVertex(local, dx, dy, 0, 0);
  }

  @Override
  public void buildVertex(InternalTransform local, float dx, float dy, float sx, float sy) {
    vertexData.position(vertexOffset);
    vertexData.put(local.m00());
    vertexData.put(local.m01());
    vertexData.put(local.m10());
    vertexData.put(local.m11());
    vertexData.put(local.tx());
    vertexData.put(local.ty());
    vertexData.put(dx);
    vertexData.put(dy);
    vertexData.put(sx);
    vertexData.put(sy);
    vertexData.position(0);

    vertexOffset += VERTEX_SIZE;

  }

  @Override
  public void addElement(int index) {
    elementData.position(elementOffset);
    elementData.put((short) index);
    elementOffset++;
    elementData.position(0);
  }

  private int loadShader(int type, final String shaderSource) {
    int shader;

    // Create the shader object
    shader = gl20.glCreateShader(type);
    if (shader == 0)
      return 0;

    // Load the shader source
    gl20.glShaderSource(shader, shaderSource);

    // Compile the shader
    gl20.glCompileShader(shader);

    IntBuffer compiled = IntBuffer.allocate(1);
    gl20.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, compiled);

    if (compiled.array()[0] == 0) { // Same as gfx.GL_FALSE
      Log.e(this.getClass().getName(), "Could not compile shader " + type + ":");
      Log.e(this.getClass().getName(), gl20.glGetShaderInfoLog(shader));
      gl20.glDeleteShader(shader);
      shader = 0;
    }

    return shader;
  }

  // Creates program object, attaches shaders, and links into pipeline
  protected int createProgram(String vertexSource, String fragmentSource) {
    // Load the vertex and fragment shaders
    int vertexShader = loadShader(GL20.GL_VERTEX_SHADER, vertexSource);
    int fragmentShader = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
    // Create the program object
    int program = gl20.glCreateProgram();
    if (vertexShader == 0 || fragmentShader == 0 || program == 0)
      return 0;

    if (program != 0) {
      gl20.glAttachShader(program, vertexShader);
      ctx.checkGLError("createProgram Attaching vertex shader");
      gl20.glAttachShader(program, fragmentShader);
      ctx.checkGLError("createProgram Attaching fragment shader");
      gl20.glLinkProgram(program);
      IntBuffer linkStatus = IntBuffer.allocate(1);
      gl20.glGetProgramiv(program, GL20.GL_LINK_STATUS, linkStatus);
      if (linkStatus.array()[0] != GL20.GL_TRUE) {
        Log.e(this.getClass().getName(), "Could not link program: ");
        Log.e(this.getClass().getName(), gl20.glGetProgramInfoLog(program));
        gl20.glDeleteProgram(program);
        program = 0;
      }
    }
    return program;
  }
}
