package tuner.gui.util

import java.awt.Graphics2D
import javax.media.opengl.{GL,GL2,DebugGL2,GL2GL3,GL2ES1}
import javax.media.opengl.fixedfunc.GLMatrixFunc

import tuner.gui.P5Panel

import com.jogamp.opengl.util.awt.TextRenderer

import scala.collection.mutable.ListBuffer

object TextAlign {
  sealed trait TextVAlign
  case object Top extends TextVAlign
  case object Middle extends TextVAlign
  case object Bottom extends TextVAlign

  sealed trait TextHAlign
  case object Left extends TextHAlign
  case object Center extends TextHAlign
  case object Right extends TextHAlign
}

object FontLib {

  import TextAlign._

  val horizStrings = new ListBuffer[(String,Int,Int)]
  val vertStrings = new ListBuffer[(String,Int,Int)]

  def textWidth(g:Graphics2D, str:String) = {
    val metrics = g.getFontMetrics(g.getFont)
    metrics.stringWidth(str)
  }

  def begin(renderer:TextRenderer) = {
    horizStrings.clear
    vertStrings.clear
  }

  def end(gl2:GL2, renderer:TextRenderer, screenW:Int, screenH:Int) = {
    renderer.beginRendering(screenW, screenH)

    horizStrings.foreach {case (str,x,y) => renderer.draw(str, x, y)}
    renderer.flush

    // vertical strings are more complex
    vertStrings.foreach {case (str,x,y) => 
      gl2.glMatrixMode(GLMatrixFunc.GL_MODELVIEW)
      gl2.glPushMatrix
      gl2.glLoadIdentity
      gl2.glTranslatef(x, y, 0)
      gl2.glRotatef(90, 0, 0, 1)

      renderer.draw(str, 0, 0)
      renderer.flush
      gl2.glPopMatrix
    }

    renderer.endRendering
  }

  def drawString(g:Graphics2D, str:String, x:Int, y:Int, 
                 hAlign:TextHAlign, vAlign:TextVAlign) = {
    val metrics = g.getFontMetrics(g.getFont)
    val height = metrics.getAscent
    val width = metrics.stringWidth(str)

    val xx = hAlign match {
      case Left   => x
      case Center => x - (width / 2)
      case Right  => x - width
    }
    val yy = vAlign match {
      case Top    => y + height
      case Middle => y + (height / 2)
      case Bottom => y 
    }

    g.drawString(str, xx, yy)
  }

  def drawString(renderer:TextRenderer, str:String, x:Int, y:Int,
                 hAlign:TextHAlign, vAlign:TextVAlign,
                 screenW:Int, screenH:Int) = {

    val stringBounds = renderer.getFont.getStringBounds(
      str, renderer.getFontRenderContext)
    val (width, height) = (stringBounds.getWidth, stringBounds.getHeight)

    val xx = hAlign match {
      case Left   => x
      case Center => x - (width / 2)
      case Right  => x - width
    }
    val yy = vAlign match {
      case Top    => y + height
      case Middle => y + (height / 2)
      case Bottom => y 
    }

    horizStrings.append((str, xx.toInt, screenH-yy.toInt))
  }

  def drawVString(g:Graphics2D, str:String, x:Int, y:Int,
                  hAlign:TextHAlign, vAlign:TextVAlign) = {

    // Need to compute these before rotation
    val metrics = g.getFontMetrics(g.getFont)
    val height = metrics.getAscent
    val width = metrics.stringWidth(str)

    val xx = hAlign match {
      case Left   => x + height
      case Center => x + (height / 2)
      case Right  => x
    }
    val yy = vAlign match {
      case Top    => y
      case Middle => y + (width / 2)
      case Bottom => y + width
    }

    val oldFont = g.getFont
    val rotFont = oldFont.deriveFont(
      java.awt.geom.AffineTransform.getRotateInstance(-3.1415926 / 2.0))
    g.setFont(rotFont)
    
    g.drawString(str, xx, yy)

    g.setFont(oldFont)
  }

  def drawVString(gl2:GL2, renderer:TextRenderer, str:String, 
                  x:Int, y:Int,
                  hAlign:TextHAlign, vAlign:TextVAlign,
                  screenW:Int, screenH:Int) = {

    // Need to compute these before rotation
    val stringBounds = renderer.getFont.getStringBounds(
      str, renderer.getFontRenderContext)
    val (width, height) = (stringBounds.getWidth, stringBounds.getHeight)

    val xx = hAlign match {
      case Left   => x + height
      case Center => x + (height / 2)
      case Right  => x
    }
    val yy = vAlign match {
      case Top    => y
      case Middle => y + (width / 2)
      case Bottom => y + width
    }

    vertStrings.append((str, xx.toInt, (screenH-yy).toInt))
  }

}

