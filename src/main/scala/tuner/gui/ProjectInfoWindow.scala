package tuner.gui

import scala.swing.BorderPanel
import scala.swing.BoxPanel
import scala.swing.Button
import scala.swing.Label
import scala.swing.FlowPanel
import scala.swing.GridBagPanel
import scala.swing.MainFrame
import scala.swing.Orientation
import scala.swing.Swing
import scala.swing.TextField
import scala.swing.event.ButtonClicked

import tuner.Config
import tuner.Project
import tuner.Tuner

class ProjectInfoWindow(project:Project) extends MainFrame {

  title = "New Project"

  menuBar = MainMenu

  val projectNameField = new TextField
  val locationChooser = new FileChooser
  val scriptChooser = new FileChooser
  val nextButton = new Button("Next")
  val cancelButton = new Button("Cancel")

  val inputTable = new ControlTable(List("Name", "Lower", "Upper")) {
    def controlRow = List(
      new TextField,
      new TextField,
      new TextField
    )
  }

  val titlePanel = new BoxPanel(Orientation.Horizontal) {
    contents += Swing.HGlue
    contents += new Label("Step 1 of 2: Enter Initial Information")
    contents += Swing.HGlue
  }

  val contentPanel = new BoxPanel(Orientation.Vertical) {
    val projectInfoPanel = new GridBagPanel {
      layout(new Label("Project Name")) = (0,0)
      layout(new Label("Save Location")) = (0,1)

      layout(projectNameField) = (1,0)
      layout(locationChooser) = (1,1)

      border = Swing.TitledBorder(Swing.EmptyBorder(Config.borderSize),
                                  "Project Info")
    }

    val scriptPanel = new GridBagPanel {
      layout(new Label("Script")) = (0,0)
      layout(scriptChooser) = (1,0)

      border = Swing.TitledBorder(Swing.EmptyBorder(Config.borderSize),
                                  "Black Box Interface")
    }

    val inputsPanel = new FlowPanel {
      contents += inputTable

      border = Swing.TitledBorder(Swing.EmptyBorder(Config.borderSize),
                                  "Inputs")
    }

    contents += projectInfoPanel
    contents += scriptPanel
    contents += inputsPanel
  }

  val buttonPanel = new BoxPanel(Orientation.Horizontal) {
    contents += Swing.HGlue
    contents += cancelButton
    contents += nextButton
  }

  contents = new BorderPanel {
    layout(titlePanel) = BorderPanel.Position.North
    layout(contentPanel) = BorderPanel.Position.Center
    layout(buttonPanel) = BorderPanel.Position.South
  }

  // Set up the interactions
  listenTo(nextButton)
  listenTo(cancelButton)

  reactions += {
    case ButtonClicked(`nextButton`) =>
      val samplerWindow = new InitialSamplerWindow(project)
      close
      samplerWindow.visible = true
    case ButtonClicked(`cancelButton`) => 
      close
      Tuner.top
  }
}

