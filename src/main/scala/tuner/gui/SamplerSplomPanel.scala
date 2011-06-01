package tuner.gui

import tuner.Config
import tuner.Project
import tuner.geom.Rectangle
import tuner.gui.util.FacetLayout
import tuner.gui.widgets.Axis
import tuner.gui.widgets.Scatterplot

class SamplerSplomPanel(project:Project)
  extends P5Panel(Config.samplerSplomDims._1, 
                  Config.samplerSplomDims._2, 
                  P5Panel.Java2D) {
  
  var splomBounds = Rectangle((0f,0f), (0f,0f))
  val sploms = project.inputFields.flatMap({fld1 =>
    project.inputFields.flatMap({fld2 =>
      if(fld1 < fld2) {
        Some(((fld1, fld2), new Scatterplot(Config.sampleDotColor)))
      } else {
        None
      }
    })
  }).toMap
  val xAxes:Map[String,Axis] = 
    project.inputFields.foldLeft(Map[String,Axis]()) {case (xa, fld) =>
      xa + (fld -> new Axis(Axis.HorizontalBottom))
    }
  val yAxes:Map[String,Axis] = 
    project.inputFields.foldLeft(Map[String,Axis]()) {case (ya,fld) =>
      ya + (fld -> new Axis(Axis.VerticalLeft))
    }

  override def setup = {
    noLoop
  }
  
  def redraw = loop

  def draw = {
    noLoop

    applet.background(Config.backgroundColor)

    // Make sure we have something to draw
    if(project.samples.numRows > 0) {
      // Compute all the sizes of things
      val totalSize = math.min(width, height) - 
                      Config.plotSpacing * 2 - 
                      Config.axisSize
      splomBounds = Rectangle((Config.plotSpacing+Config.axisSize, 
                               Config.plotSpacing), 
                              totalSize, totalSize)
      val (_, plotBounds) = 
        FacetLayout.plotBounds(splomBounds, project.inputFields)
      project.inputFields.foreach {xFld =>
        project.inputFields.foreach {yFld =>
          if(xFld < yFld) {
            val bound = plotBounds((xFld, yFld))
            val plot = sploms((xFld, yFld))
            plot.draw(this, bound.minX, bound.minY, bound.width, bound.height,
                      project.samples, xFld, yFld)
            if(xFld != project.inputFields.last) {
              xAxes(xFld).draw(this, bound.minX, splomBounds.maxY, 
                                     bound.width, Config.axisSize,
                                     xFld, ticks(xFld))
            }
            if(yFld != project.inputFields.head) {
              yAxes(yFld).draw(this, Config.plotSpacing, bound.minY, 
                                     Config.axisSize, bound.height, 
                                     yFld, ticks(yFld))
            }
          }
        }
      }
    }
  }

  private def ticks(fld:String) : List[Float] = {
    val (min, max) = project.inputs.range(fld)
    List(min, (min+max)/2, max)
  }

}

