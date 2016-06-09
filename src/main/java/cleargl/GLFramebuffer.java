package cleargl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import coremem.types.NativeTypeEnum;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * <Description>
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
public class GLFramebuffer {
  protected int framebufferId[];
  protected List<GLTexture> backingTextures;
  protected List<GLTexture> depthBuffers;
  protected int width;
  protected int height;
  protected boolean initialized;

  public GLFramebuffer(GL4 gl, int width, int height) {
    framebufferId = new int[1];
    backingTextures = new ArrayList<>();
    depthBuffers = new ArrayList<>();
    this.width = width;
    this.height = height;

    gl.getGL().glGenFramebuffers(1, framebufferId, 0);

    initialized = true;
  }

  public void addFloatBuffer(GL4 gl, int channelDepth) {
    if(!initialized) {
      return;
    }

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, getId());

    backingTextures.add(new GLTexture(
            gl,
            NativeTypeEnum.Float,
            3,
            width, height, 1, true, 1, channelDepth
    ));

    gl.getGL().getGL4().glFramebufferTexture(GL.GL_FRAMEBUFFER,
            getCurrentFramebufferColorAttachment(),
            backingTextures.get(backingTextures.size()-1).getId(),
            0
    );

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
  }

  public void addFloatRGBBuffer(GL4 gl, int channelDepth) {
    if(!initialized) {
      return;
    }

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, getId());

    backingTextures.add(new GLTexture(
            gl,
            NativeTypeEnum.Float,
            3,
            width, height, 1, true, 1, channelDepth
    ));

    gl.getGL().getGL4().glFramebufferTexture(GL.GL_FRAMEBUFFER,
            getCurrentFramebufferColorAttachment(),
            backingTextures.get(backingTextures.size()-1).getId(),
            0
    );

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
  }

  public void addUnsignedByteRGBABuffer(GL4 gl, int channelDepth) {
    if(!initialized) {
      return;
    }

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, getId());

    backingTextures.add(new GLTexture(
            gl,
            NativeTypeEnum.UnsignedByte,
            4,
            width, height, 1, true, 1, channelDepth
    ));

    gl.getGL().getGL4().glFramebufferTexture(GL.GL_FRAMEBUFFER,
            getCurrentFramebufferColorAttachment(),
            backingTextures.get(backingTextures.size()-1).getId(),
            0
    );

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
  }

  public void addDepthBuffer(GL4 gl, int depth) {
    addDepthBuffer(gl, depth, 1);
  }

  public void addDepthBuffer(GL4 gl, int depth, int scale) {
    if(!initialized) {
      return;
    }

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, getId());

    depthBuffers.add(new GLTexture(
            gl,
            NativeTypeEnum.Float,
            -1,
            width/scale, height/scale, 1, true, 1, depth
    ));

    gl.getGL().getGL4().glFramebufferTexture(GL.GL_FRAMEBUFFER,
            GL.GL_DEPTH_ATTACHMENT,
            depthBuffers.get(depthBuffers.size()-1).getId(),
            0
    );

    gl.getGL().glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
  }

  public boolean checkDrawBuffers(GL4 gl) {
    if (!initialized) {
      return false;
    }

    gl.getGL4().glBindFramebuffer(GL.GL_FRAMEBUFFER, getId());
    int status = gl.getGL().getGL4().glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);

    if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
      System.err.println("Framebuffer " + framebufferId[0] + " is incomplete, " + Integer.toHexString(status));
      return false;
    }

    return true;
  }

  public void setDrawBuffers(GL4 gl) {

    int attachments[] = new int[backingTextures.size()];
    for(int i = 0; i < backingTextures.size(); i++) {
      attachments[i] = GL.GL_COLOR_ATTACHMENT0 + i;
    }

    gl.getGL().getGL4().glBindFramebuffer(GL.GL_FRAMEBUFFER, getId());
    gl.getGL().getGL4().glDrawBuffers(backingTextures.size(), IntBuffer.wrap(attachments));
  }

  public void bindTexturesToUnitsWithOffset(GL4 gl, int offset) {
    int colorUnits = 0;
    for(int i = 0; i < backingTextures.size(); i++) {
      gl.glActiveTexture(GL.GL_TEXTURE0 + offset + i);
      gl.glBindTexture(GL.GL_TEXTURE_2D, backingTextures.get(i).getId());
      colorUnits = i;
    }

    if(depthBuffers.size() > 0) {
      for (int i = 0; i < depthBuffers.size(); i++) {
        gl.glActiveTexture(GL.GL_TEXTURE0 + offset + i + colorUnits + 1);
        gl.glBindTexture(GL.GL_TEXTURE_2D, depthBuffers.get(i).getId());
      }
    }
  }

  public List<Integer> getTextureIds(GL4 gl) {
    ArrayList<Integer> list = new ArrayList<>();

    for(int i = 0; i < backingTextures.size(); i++) {
      list.add(backingTextures.get(i).getId());
    }

    return list;
  }

  public void revertToDefaultFramebuffer(GL4 gl) {
    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
  }

  public void resize(GL4 gl, int newWidth, int newHeight) {
    int oldIds[] = framebufferId.clone();

    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
    gl.glGenFramebuffers(1, framebufferId, 0);
    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, getId());

    List<GLTexture> newBackingTextures = new ArrayList<>();
    List<GLTexture> newDepthBuffers = new ArrayList<>();

    for(int i = 0; i < backingTextures.size(); i++) {
      GLTexture t = backingTextures.get(i);
      GLTexture newT = new GLTexture(gl,
              t.getNativeType(),
              t.getChannels(),
              newWidth, newHeight,
              1, true, 1, t.getBitsPerChannel()
      );

      newT.clear();
      newBackingTextures.add(newT);
      t.close();

      gl.getGL().getGL4().glFramebufferTexture(GL.GL_FRAMEBUFFER,
              getCurrentFramebufferColorAttachment(newBackingTextures),
              newBackingTextures.get(newBackingTextures.size()-1).getId(),
              0
      );
    }

    for(int i = 0; i < depthBuffers.size(); i++) {
      GLTexture t = depthBuffers.get(i);
      GLTexture newT = new GLTexture(gl,
              t.getNativeType(),
              -1,
              newWidth, newHeight,
              1, true, 1, t.getBitsPerChannel()
      );

      newDepthBuffers.add(newT);
      t.close();

      gl.getGL().getGL4().glFramebufferTexture(GL.GL_FRAMEBUFFER,
              GL.GL_DEPTH_ATTACHMENT,
              newDepthBuffers.get(newDepthBuffers.size()-1).getId(),
              0
      );
    }

    backingTextures.clear();
    depthBuffers.clear();

    backingTextures.addAll(newBackingTextures);
    depthBuffers.addAll(newDepthBuffers);

    width = newWidth;
    height = newHeight;

    gl.glDeleteFramebuffers(1, oldIds, 0);
    gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
  }

  @Override
  public String toString() {
    String info;
    if(!initialized) {
      info = "GLFramebuffer (not initialized)\n";
    } else {
      info = "GLFramebuffer " + framebufferId[0] + "\n|\n";

      for (GLTexture att : backingTextures) {
        info += String.format("+-\tColor Attachment %s, %dx%d/%d*%d, 0x%s\n", Integer.toHexString(att.getId()), att.getWidth(), att.getHeight(), att.getChannels(), att.getBitsPerChannel(), Integer.toHexString(att.getInternalFormat()));
      }

      info += "|\n";

      for (GLTexture att : depthBuffers) {
        info += String.format("+-\tDepth Attachment %s, %dx%d/%d*%d, 0x%s\n", Integer.toHexString(att.getId()), att.getWidth(), att.getHeight(), att.getChannels(), att.getBitsPerChannel(), Integer.toHexString(att.getInternalFormat()));
      }
    }
    return info;
  }

  public int getId() {
    if(initialized) {
      return framebufferId[0];
    } else {
      return -1;
    }
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  public int getBoundBufferNum() {
    return backingTextures.size()+depthBuffers.size();
  }

  private int getCurrentFramebufferColorAttachment() {
    return GL.GL_COLOR_ATTACHMENT0 + backingTextures.size() - 1;
  }

  private int getCurrentFramebufferColorAttachment(List<GLTexture> base) {
    return GL.GL_COLOR_ATTACHMENT0 + base.size() - 1;
  }
}