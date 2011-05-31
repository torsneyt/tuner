package tuner.gui

import java.awt.Dimension
import javax.swing.JInternalFrame

import processing.core.PApplet
import processing.core.PConstants
import processing.core.PFont
import processing.core.PImage

import scala.swing.BoxPanel
import scala.swing.Component
import scala.swing.Orientation
import scala.swing.Swing

object P5Panel {
  sealed trait Renderer {def name : String}

  case object Java2D extends Renderer {val name = PConstants.JAVA2D}
  case object P2D extends Renderer {val name = PConstants.P2D}
  case object P3D extends Renderer {val name = PConstants.P3D}
  case object OpenGL extends Renderer {val name = PConstants.OPENGL}
  case object PDF extends Renderer {val name = PConstants.PDF}

  object BlendMode extends Enumeration {
    val Blend = Value(PConstants.BLEND)
    val Add = Value(PConstants.ADD)
    val Multiply = Value(PConstants.MULTIPLY)
  }

  object RectMode extends Enumeration {
    val Corner = Value(PConstants.CORNER)
    val Corners = Value(PConstants.CORNERS)
    val Center = Value(PConstants.CENTER)
    val Radius = Value(PConstants.RADIUS)
  }

  object EllipseMode extends Enumeration {
    val Corner = Value(PConstants.CORNER)
    val Corners = Value(PConstants.CORNERS)
    val Center = Value(PConstants.CENTER)
    val Radius = Value(PConstants.RADIUS)
  }

  object Shape extends Enumeration {
    val QuadStrip = Value(PConstants.QUAD_STRIP)
    val TriangleStrip = Value(PConstants.TRIANGLE_STRIP)
  }

  object ColorSpace extends Enumeration {
    val RGB = Value(PConstants.RGB)
    val HSB = Value(PConstants.HSB)
  }

  object TextHAlign extends Enumeration {
    val Left = Value(PConstants.LEFT)
    val Center = Value(PConstants.CENTER)
    val Right = Value(PConstants.RIGHT)
  }

  object TextVAlign extends Enumeration {
    val Top = Value(PConstants.TOP)
    val Center = Value(PConstants.CENTER)
    val Bottom = Value(PConstants.BOTTOM)
  }

  object MouseButton extends Enumeration {
    val Nothing = Value(0)
    val Left = Value(PConstants.LEFT)
    val Right = Value(PConstants.RIGHT)
    val Center = Value(PConstants.CENTER)
  }

  object KeyCode extends Enumeration {
    val None = Value(0)
    val Shift = Value(PConstants.SHIFT)
    val Alt = Value(PConstants.ALT)
    val Control = Value(PConstants.CONTROL)
    val Up = Value(PConstants.UP)
    val Down = Value(PConstants.DOWN)
    val Left = Value(PConstants.LEFT)
    val Right = Value(PConstants.RIGHT)
  }

  val HalfPi = PConstants.HALF_PI

  type Color = Int

  def map(value:Float, low1:Float, high1:Float, low2:Float, high2:Float) =
    PApplet.map(value, low1, high1, low2, high2)

  def constrain(value:Float, low:Float, high:Float) = 
    PApplet.constrain(value, low, high)

  def dist(x1:Float, y1:Float, x2:Float, y2:Float) =
    PApplet.dist(x1, y1, x2, y2)

  def norm(value:Float, low:Float, high:Float) = 
    PApplet.norm(value, low, high)
  
  def lerpColor(c1:Color, c2:Color, amt:Float, method:ColorSpace.Value) =
    PApplet.lerpColor(c1, c2, amt, method.id)
  
  def nfs(num:Float, left:Int, right:Int) = PApplet.nfs(num, left, right)
}

abstract class P5Panel (
    _width:Int, _height:Int, 
    renderer:P5Panel.Renderer=P5Panel.Java2D) 
    extends BoxPanel(Orientation.Vertical) {
  
  import P5Panel._

  val applet = {
    new PApplet {
      override def setup = {
        size(_width, _height, renderer.name)
        P5Panel.this.setup
      }

      override def draw = {
        P5Panel.this.draw
      }

      override def mouseClicked = {
        P5Panel.this.mouseClicked(mouseX, mouseY, MouseButton(mouseButton))
      }

      override def mouseDragged = {
        P5Panel.this.mouseDragged(pmouseX, pmouseY, 
                                  mouseX, mouseY,
                                  MouseButton(mouseButton))
      }

      override def mouseMoved = {
        P5Panel.this.mouseMoved(pmouseX, pmouseY, 
                                mouseX, mouseY,
                                MouseButton(mouseButton))
      }

      override def mousePressed = {
        P5Panel.this.mousePressed(mouseX, mouseY, MouseButton(mouseButton))
      }

    }
  }

  // From http://forum.processing.org/topic/papplet-in-jscrollpane
  // set up a lightweight container for P5 and strip the UI
  val sketchFrame = new Component {
    override lazy val peer = new JInternalFrame with SuperMixin
    val ui = peer.getUI.asInstanceOf[javax.swing.plaf.basic.BasicInternalFrameUI]
    peer.putClientProperty("titlePane", ui.getNorthPane)
    peer.putClientProperty("border", peer.getBorder)
    ui.setNorthPane(null)
    peer.setBorder(null)
    // Set up P5's container frame
    peer.add(applet)
    peer.setSize(_width, _height)
    peer.setPreferredSize(new Dimension(_width, _height))
    peer.setMaximumSize(new Dimension(_width, _height))
    peer.setResizable(false)
    peer.setVisible(true)
    peer.pack
  }

  /*
  layout(sketchFrame) = BorderPanel.Position.Center
  layout(Swing.VGlue) = BorderPanel.Position.North
  layout(Swing.VGlue) = BorderPanel.Position.South
  layout(Swing.HGlue) = BorderPanel.Position.East
  layout(Swing.HGlue) = BorderPanel.Position.West
  */
  contents += Swing.VGlue
  contents += new BoxPanel(Orientation.Horizontal) {
    contents += Swing.HGlue
    contents += sketchFrame
    contents += Swing.HGlue
  }
  contents += Swing.VGlue

  applet.init

  private var loadedFonts = Map[(String,Int),PFont]()

  def setup = {}
  def draw

  def loop = applet.loop
  def noLoop = applet.noLoop

  def createGraphics(xSize:Int, ySize:Int, renderer:P5Panel.Renderer) =
    applet.createGraphics(xSize, ySize, renderer.name)

  def mouseClicked(mouseX:Int, mouseY:Int, button:MouseButton.Value) = {}
  def mouseDragged(prevMouseX:Int, prevMouseY:Int, 
                   mouseX:Int, mouseY:Int,
                   mouseButton:MouseButton.Value) = {}
  def mouseMoved(prevMouseX:Int, prevMouseY:Int, 
                 mouseX:Int, mouseY:Int,
                 button:MouseButton.Value) = {}
  def mousePressed(mouseX:Int, mouseY:Int, button:MouseButton.Value) = {}

  def mousePos : (Int,Int) = (applet.mouseX, applet.mouseY)
  def mouseDown = applet.mousePressed
  def mouseButton = MouseButton(applet.mouseButton)

  def keyCode = KeyCode(applet.keyCode)

  def ellipseMode(mode:EllipseMode.Value) = applet.ellipseMode(mode.id)
  def rectMode(mode:RectMode.Value) = applet.rectMode(mode.id)

  def text(stringdata:String, x:Float, y:Float) = {
    applet.text(stringdata, x, y)
  }
  def textFont(path:String, size:Int) = {
    applet.textFont(font(path, size))
  }
  def textAlign(halign:TextHAlign.Value) = applet.textAlign(halign.id)
  def textAlign(halign:TextHAlign.Value, valign:TextVAlign.Value) = 
    applet.textAlign(halign.id, valign.id)
  def textWidth(text:String) = applet.textWidth(text)

  def height = applet.height
  def width = applet.width

  def pushMatrix = applet.pushMatrix
  def popMatrix = applet.popMatrix
  def translate(x:Float, y:Float) = applet.translate(x, y)
  def translate(x:Float, y:Float, z:Float) = applet.translate(x, y, z)
  def rotate(radians:Float) = applet.rotate(radians)

  def blend(srcImg:PImage, x:Int, y:Int, width:Int, height:Int, 
                           dx:Int, dy:Int, dwidth:Int, dheight:Int, 
                           mode:BlendMode.Value) = {
    applet.blend(srcImg, x, y, width, height, dx, dy, dwidth, dheight, mode.id)
  }
  def stroke(color:Color) = applet.stroke(color)
  def noStroke = applet.noStroke

  def fill(color:Color) = applet.fill(color)
  def fill(grey:Int, alpha:Float) = applet.fill(grey, alpha)
  def noFill = applet.noFill

  def beginShape(shape:Shape.Value) = applet.beginShape(shape.id)
  def endShape = applet.endShape
  def vertex(x:Float, y:Float) = applet.vertex(x, y)

  def line(x1:Float, y1:Float, x2:Float, y2:Float) = 
    applet.line(x1, y1, x2, y2)

  def triangle(x1:Float, y1:Float, x2:Float, y2:Float, x3:Float, y3:Float) =
    applet.triangle(x1, y1, x2, y2, x3, y3)
  
  def rect(x1:Float, y1:Float, x2:Float, y2:Float) = 
    applet.rect(x1, y1, x2, y2)

  def ellipse(x1:Float, y1:Float, x2:Float, y2:Float) = 
    applet.ellipse(x1, y1, x2, y2)

  def font(path:String, size:Int) : PFont = {
    loadedFonts.getOrElse((path, size), {
      println("loading font `" + path + "' (" + size + ")")
      val f = applet.createFont(path, size)
      loadedFonts += (path, size) -> f
      f
    })
  }
}

