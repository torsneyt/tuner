package tuner.gui

import scala.actors.Actor
import scala.actors.Actor._
import scala.swing.BoxPanel
import scala.swing.Button
import scala.swing.CheckBox
import scala.swing.Frame
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.ProgressBar
import scala.swing.Window
import scala.swing.event.ButtonClicked

import tuner.Tuner
import tuner.project.InProgress

class SamplingProgressBar(project:InProgress) extends Frame {

  //modal = true
  //width = 800
  //height = 75

  val progressBar = new ProgressBar {
    min = 0
  }
  val alwaysBackgroundCheckbox = new CheckBox("Always Background") {
    selected = project.buildInBackground
  }
  val backgroundButton = new Button("Background")
  val stopButton = new Button("Stop")
  val progressLabel = new Label {
    text = "   "
  }

  listenTo(backgroundButton)
  listenTo(stopButton)

  contents = new BoxPanel(Orientation.Horizontal) {
    contents += new BoxPanel(Orientation.Vertical) {
      contents += progressBar
      contents += progressLabel
    }

    contents += alwaysBackgroundCheckbox
    contents += backgroundButton
    contents += stopButton
  }

  reactions += {
    case ButtonClicked(`backgroundButton`) =>
      project.buildInBackground = alwaysBackgroundCheckbox.selected
      project.save(project.savePath)
      Tuner.closeProject(project)
    case ButtonClicked(`stopButton`) => 
      project.sampleRunner match {
        case Some(sr) => 
          sr.stop
        case None     => // Nothing to stop!
      }
      Tuner.closeProject(project)
  }

  updateProgress

  private var runScanner = true
  private val scanner:Actor = actor {
    while(runScanner) {
      Thread.sleep(1237)
      updateProgress
    }
  }

  def updateProgress = {
    progressLabel.text = project.statusString

    val (cur, max) = project.runStatus

    if(max > 0) {
      progressBar.indeterminate = false
    } else {
      progressBar.indeterminate = true
      progressBar.max = max
      progressBar.value = cur
    }
    this.pack
  }
}

