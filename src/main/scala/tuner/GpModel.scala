package tuner

import net.liftweb.json.JsonParser._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

import org.apache.commons.math.analysis.DifferentiableMultivariateRealFunction
import org.apache.commons.math.analysis.MultivariateRealFunction
import org.apache.commons.math.analysis.MultivariateVectorialFunction
import org.apache.commons.math.optimization.GoalType
import org.apache.commons.math.optimization.direct.MultiDirectional
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer
import org.apache.commons.math.optimization.general.NonLinearConjugateGradientOptimizer
import org.apache.commons.math.optimization.general.ConjugateGradientFormula

import numberz.Matrix
import numberz.Vector

import org.rosuda.JRI.RList

import tuner.util.Util

case class GpSpecification(
  responseDim:String,
  dimNames:List[String],
  thetas:List[Double],
  alphas:List[Double],
  mean:Double,
  sigma2:Double,
  designMatrix:List[List[Double]],
  responses:List[Double],
  invCorMtx:List[List[Double]]
)

object GpModel {
  def fromJson(json:GpSpecification) = {
    new GpModel(numberz.Vector(json.thetas), numberz.Vector(json.alphas), 
                json.mean, json.sigma2,
                numberz.Matrix(json.designMatrix),
                numberz.Vector(json.responses),
                numberz.Matrix(json.invCorMtx),
                json.dimNames, json.responseDim, Config.errorField)
  }
}

// A gp model takes a sampling density and returns 
// a filename from which to read the sampled data
//type Model = Int => String
class GpModel(val thetas:numberz.Vector, val alphas:numberz.Vector, 
              val mean:Double, val sig2:Double, 
              val design:numberz.Matrix, val responses:numberz.Vector, 
              val rInverse:numberz.Matrix, 
              val dims:List[String], val respDim:String, val errDim:String) {

  def this(thetas:numberz.Vector, alphas:numberz.Vector, mean:Double, 
           design:numberz.Matrix, responses:numberz.Vector, 
           rInverse:numberz.Matrix, 
           dims:List[String], respDim:String, errDim:String) =
    this(thetas, alphas, mean, 
         (responses.map {_ - mean} dot (rInverse dot responses.map {_ - mean})) / responses.size,
         design, responses, rInverse, dims, respDim, errDim)

  def this(thetas:numberz.Vector, alphas:numberz.Vector, 
           design:numberz.Matrix, responses:numberz.Vector, 
           rInverse:numberz.Matrix, 
           dims:List[String], respDim:String, errDim:String) =
    this(thetas, alphas, 
         (Vector.ones(responses.size) dot (rInverse dot responses)) /
           (Vector.ones(responses.size) dot (rInverse dot Vector.ones(responses.size))),
         design, responses, rInverse, dims, respDim, errDim)

  def this(fm:RList, dims:List[String], respDim:String, errDim:String) =
    this(new Vector(fm.at("beta").asDoubleArray),
         new Vector(fm.at("a").asDoubleArray),
         fm.at("mu").asDouble,
         fm.at("sig2").asDouble,
         new Matrix(fm.at("X").asDoubleMatrix),
         new Vector(fm.at("Z").asDoubleArray),
         new Matrix(fm.at("invVarMatrix").asDoubleMatrix),
         dims, respDim, errDim)

  // Also precompute rInverse . (responses - mean)
  val corrResponses = rInverse dot (responses map {_ - mean})

  // Also precompute the square of the sum of the rows 
  // of the cholesky decomposition of rInverse
  val sqCholCols:Vector = {
    val (tmp, _) = (rInverse * sig2).chol
    val ttls = tmp.mapRows {row:Vector => row.sum}
    ttls * ttls
  }

  def toJson = {
    GpSpecification(
      respDim, dims, thetas.toList, alphas.toList, mean, sig2,
      design.toList, responses.toList, rInverse.toList)
  }

  def maxGain(range:DimRanges):Float = {
    var mx = Double.MinValue
    Sampler.lhc(range, Config.respHistogramSampleDensity, pt => {
      val (est, err) = runSample(pt)
      val expgain = calcExpectedGain(est, err)
      mx = math.max(mx, expgain)
    })
    mx.toFloat
  }

  // Store the most recently seen max value for the function
  def funcMax:Float = responses.max.toFloat
  def funcMin:Float = responses.min.toFloat

  def theta(dim:String) = thetas(dims.indexOf(dim))

  def sampleSlice(rowDim:(String,(Float,Float)), 
                  colDim:(String,(Float,Float)),
                  slices:List[(String,Float)], 
                  numSamples:Int)
      : ((String, Grid2D), (String, Grid2D), (String, Grid2D)) = {
    
    val arrSlice = Array.fill(dims.length)(0.0)
    val sliceMap = slices.toMap
    dims.zipWithIndex.foreach {case (fld, i) =>
      arrSlice(i) = sliceMap(fld)
    }
    val xDim = dims.indexOf(rowDim._1)
    val yDim = dims.indexOf(colDim._1)

    // generate one matrix from scratch and then copy the rest
    val startTime = System.currentTimeMillis
    val response = Sampler.regularSlice(rowDim, colDim, numSamples)
    val errors = new Grid2D(response.rowIds, response.colIds)
    val gains = new Grid2D(response.rowIds, response.colIds)

    response.rowIds.zipWithIndex.foreach {tmpx =>
      val (xval,x) = tmpx
      response.colIds.zipWithIndex.foreach {tmpy =>
        val (yval, y) = tmpy
        arrSlice(xDim) = xval
        arrSlice(yDim) = yval
        val (est, err) = estimatePoint(Vector(arrSlice))
        response.set(x, y, est.toFloat)
        errors.set(x, y, err.toFloat)
        val expgain = calcExpectedGain(est, err)
        if(!expgain.isNaN) {
          gains.set(x, y, expgain.toFloat)
        } else {
          gains.set(x, y, 0f)
        }
      }
    }

    val endTime = System.currentTimeMillis
    ((respDim, response), 
     (Config.errorField, errors), 
     (Config.gainField, gains))
  }

  def sampleTable(samples:Table) : Table = {
    val outTbl = new Table
    for(r <- 0 until samples.numRows) {
      val tpl = samples.tuple(r)
      val (est, err) = runSample(tpl.toList)
      outTbl.addRow((respDim, est.toFloat)::(errDim, err.toFloat)::tpl.toList)
    }
    outTbl
  }

  def runSample(pt:List[(String, Float)]) : (Double, Double) = {
    val mapx = pt.toMap
    val xx = dims.map({mapx.get(_)}).flatten.map({_.toDouble})
    val (est, err) = estimatePoint(Vector(xx))
    //curFuncMax = math.max(est.toFloat, curFuncMax)
    (est, err)
  }

  // Compute the correlation wrt each design point
  private def estimatePoint(point:Vector) : (Double,Double) = {
    def cf(x:Vector):Double = corrFunction(x, point)
    val ptCors = design.mapRows(cf _)
    val est = mean + sig2 * (ptCors dot corrResponses)
    val err = sig2 * (1 - sig2 * (ptCors dot (rInverse dot ptCors)))
    if(err < 0) (est, 0)
    else        (est, math.sqrt(err))
  }

  private def corrFunction(p1:Vector, p2:Vector) : Double = {
    var sum:Double = 0
    for(d <- 0 until p1.size) {
      sum += corrFunction(p1(d), p2(d), thetas(d), alphas(d))
    }
    math.exp(-sum)
  }

  private def corrFunction(x1:Double, x2:Double, 
                           theta:Double, alpha:Double) : Double = {
    theta * math.pow(math.abs(x1 - x2), alpha)
  }

  def gradient(pt:List[(String,Float)]) : List[(String,Float)] = {
    val Epsilon:Float = 1e-9.toFloat
    val outVals = Array.fill(pt.length)(0f)
    val (val1, _) = runSample(pt)
    for(d <- 0 until pt.length) {
      val p2 = pt.take(d) ++ List((pt(d)._1, pt(d)._2+Epsilon)) ++ pt.drop(d)
      val p3 = pt.take(d) ++ List((pt(d)._1, pt(d)._2-Epsilon)) ++ pt.drop(d)
      val (val2, _) = runSample(p2)
      val (val3, _) = runSample(p3)
      val g2 = (val1 - val2) / Epsilon
      val g3 = (val1 - val3) / Epsilon
      outVals(d) = if(math.abs(g2) > math.abs(g3)) g2.toFloat else g3.toFloat
    }
    pt.zip(outVals).map {case ((fld,_),v2) => (fld, v2)}
  }

  def calcExpectedGain(est:Double, stddev:Double) : Double = {
    // These will come in handy later
    def erf(v:Double) : Double = {
      val a = (8*(math.Pi - 3)) / (3*math.Pi*(4 - math.Pi))
      val tmp = (4 / math.Pi + a * v * v) / (1 + a * v * v)
      v/math.abs(v) * math.sqrt(1 - math.exp(-(v*v) * tmp))
    }
    def pdf(v:Double) : Double = 1/(2*math.Pi) * math.exp(-(v*v) / 2)
    def cdf(v:Double) : Double = 0.5 * (1 + erf(v / math.sqrt(2)))

    val curFuncMax = funcMax
    val t1 = (est - curFuncMax)
    val t2 = cdf((est - curFuncMax) / stddev)
    val t3 = stddev * pdf((est - curFuncMax) / stddev)

    math.abs(t1 * t2 + t3)
  }

}
