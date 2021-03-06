package tuner.test.integration

import org.scalatest._
import org.scalatest.Matchers._

import scala.swing.Reactor
import scala.swing.event.Event

import tuner.Config
import tuner.project._
import tuner.Progress
import tuner.ProgressComplete
import tuner.Region
import tuner.Table
import tuner.ViewInfo

import tuner.test.Util._

class ProxyReactor(project:InProgress) extends Reactor {
  val events = new scala.collection.mutable.ArrayBuffer[Event]

  reactions += {
    case x => events += x
  }

  listenTo(project)
}

class RunningSamplesSpec extends WordSpec {

  val normalConfig = createConfig(resource(scriptPath("/sims/run_sim_noisy"), true))

  "A RunningSamples project" when {
    "running a normal project" must {
      val designSites = new Table
      val normalProj = new RunningSamples(normalConfig, resource("/sims"),
                                          testSamples, designSites)
      val reactor = new ProxyReactor(normalProj)
      val logger = new java.io.ByteArrayOutputStream
      normalProj.logStream = Some(logger)
      normalProj.start

      "send a Progress event with no progress when started" in {
        reactor.events should not be empty
        val e1 = reactor.events.head.asInstanceOf[Progress]
        e1.currentTime should be (0)
        e1.totalTime should be (testSamples.numRows)
        e1.ok should be (true)
      }
      "send a ProgressComplete event when finished" in {
        // One start, one finish, and one sampling finished message
        reactor.events.size should be >= (3)
        // Make sure it's the right type of object
        val e = reactor.events.last match {
          case ProgressComplete => ProgressComplete
          case _ => (fail)
        }
      }
      "write all stdout and stderr output to a log" in {
        logger.size should be > (0)
      }
      "send a Progress event with 100% progress when finished" in {
        val lastE = reactor.events(reactor.events.length-2).asInstanceOf[Progress]
        lastE.currentTime should equal (lastE.totalTime)
        lastE.ok should be (true)
      }
      "have all the new design sites written to the given table" in {
        designSites.numRows should equal (testSamples.numRows)
      }
    }
  }

  def testSamples : Table = {
    val tblData = List(
      List(("x1", 2.0f), ("x2", 0.2f), ("x3", 1.2f)),
      List(("x1", 2.1f), ("x2", 0.4f), ("x3", 0.3f)),
      List(("x1", 0.1f), ("x2", 0.9f), ("x3", 0.5f))
    )
    val tbl = new Table
    tblData.foreach {row => tbl.addRow(row)}
    tbl
  }

  def createConfig(script:String) : ProjConfig = ProjConfig(
    name="test",
    scriptPath=script,
    inputs=List("x1","x2","x3") map {InputSpecification(_, 0f, 1f)},
    outputs=List(),
    ignoreFields=List(),
    gpModels=List(),
    buildInBackground=false,
    currentVis=ViewInfo.Default,
    currentRegion=Region.Default,
    history=None,
    version=Some(Config.maxProjectVersion)
  )
}
