package tuner.gui

import date.scala.swing.TimeField

import scala.swing.BorderPanel
import scala.swing.BoxPanel
import scala.swing.ComboBox
import scala.swing.TablePanel
import scala.swing.Label
import scala.swing.Orientation
import scala.swing.Swing
import scala.swing.TextField
import scala.swing.event.EditDone
import scala.swing.event.SelectionChanged
import scala.swing.event.ValueChanged

import tuner.Config
import tuner.Sampler

class SamplerPanel(project:tuner.project.Sampler) 
    extends BoxPanel(Orientation.Vertical) {
  
  val sampleNumField = new TextField
  val sampleTimeField = new TimeField
  val ttlRunTimeField = new TimeField
  val shapeSelector = new RegionShapeCombo
  val methodSelector = new ComboBox(List("LHS", "Random"))

  val controlPane = new TablePanel(2, 4) {
    // Labels in left column
    layout(new Label("Number of Samples")) = (0,0)
    layout(new Label("x Time per Sample")) = (0,1)
    layout(new Label("= Total Run Time")) = (0,2)
    //layout(new Label("Shape")) = (0,3)
    layout(new Label("Method")) = (0,3)

    // Fields in left column
    layout(sampleNumField) = (1,0)
    layout(sampleTimeField) = (1,1)
    layout(ttlRunTimeField) = (1,2)
    //layout(shapeSelector) = (1,3)
    layout(methodSelector) = (1,3)

    border = Swing.TitledBorder(border, "Sampling")
  }

  val splomPanel = new SamplerSplomPanel(project)

  contents += controlPane
  contents += splomPanel

  // Set up the events
  listenTo(sampleNumField)
  listenTo(sampleTimeField)
  listenTo(ttlRunTimeField)
  //listenTo(shapeSelector.selection)
  listenTo(methodSelector.selection)

  var lastSamples:Int = numSamples
  var lastSampleTime:Option[Long] = sampleTimeField.millis
  var lastTotalTime:Option[Long] = ttlRunTimeField.millis
  var lastSelection:String = methodString

  // Keep track of all the text field update times
  var sampleNumModTime:Long = System.currentTimeMillis
  var sampleTimeModTime:Long = System.currentTimeMillis
  var ttlTimeModTime:Long = System.currentTimeMillis

  var internalChange:Boolean = false

  reactions += {
    case SelectionChanged(`methodSelector`) => 
      handleSamplesChanged
    case ValueChanged(`sampleNumField`) =>
      handleSamplesChanged
      if(!internalChange) {
        sampleNumModTime = System.currentTimeMillis
        internalChange = true
        if(sampleTimeModTime > ttlTimeModTime)
          updateTotalTime
        else
          updateSampleTime
        internalChange = false
      }
    case ValueChanged(`sampleTimeField`) =>
      if(lastSampleTime != sampleTimeField.millis) {
        lastSampleTime = sampleTimeField.millis
        if(!internalChange) {
          sampleTimeModTime = System.currentTimeMillis
          internalChange = true
          if(sampleNumModTime > ttlTimeModTime)
          updateTotalTime
            else
            updateNumSamples
          internalChange = false
        }
      }
    case ValueChanged(`ttlRunTimeField`) => 
      if(lastTotalTime != ttlRunTimeField.millis) {
        lastTotalTime = ttlRunTimeField.millis
        if(!internalChange) {
          ttlTimeModTime = System.currentTimeMillis
          internalChange = true
          if(sampleNumModTime > sampleTimeModTime)
            updateSampleTime
          else
            updateNumSamples
          internalChange = false
        }
      }
  }

  def numSamples : Int = {
    try {
      sampleNumField.text.toInt
    } catch {
      case nfe:NumberFormatException => 0
    }
  }

  //def shape : String = shapeSelector.toString

  def methodString = methodSelector.selection.item

  def method : Sampler.Method = {
    //println(methodSelector.selection.item)
    methodSelector.selection.item match {
      case "LHS" => Sampler.lhc
      case "Random" => Sampler.random
    }
  }

  def saveSamples = {
    FileChooser.saveFile("Save Samples") foreach {filename =>
      project.newSamples.toCsv(filename)
    }
  }

  private def handleSamplesChanged = {
    // Only publish if something actually changed
    if(lastSamples != numSamples || lastSelection != methodString) {
      lastSamples = numSamples
      lastSelection = methodString
      publish(new ValueChanged(this))
    }
  }

  private def updateNumSamples = {
    (sampleTimeField.millis, ttlRunTimeField.millis) match {
      case (Some(st), Some(tt)) if(st > 0) => 
        sampleNumField.text = (tt/st).toInt.toString
      case _ =>
    }
  }

  private def updateSampleTime = {
    (ttlRunTimeField.millis, numSamples > 0) match {
      case (Some(tt), true) => sampleTimeField.millis = tt / numSamples
      case _ =>
    }
  }

  private def updateTotalTime = {
    (sampleTimeField.millis, numSamples > 0) match {
      case (Some(st), true) => ttlRunTimeField.millis = numSamples * st
      case _ =>
    }
  }

}

