package tuner

import scala.collection.Iterable
import scala.collection.immutable.HashMap
import scala.collection.immutable.HashSet
import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ArrayBuffer

import com.typesafe.scalalogging.slf4j.LazyLogging

import tuner.util.FileReader

object Table extends LazyLogging {
  type Tuple = Map[String,Float]
  type Filter = Tuple => Tuple

  def fromCsv(filename:String) = {
    logger.info("reading " + filename + "...")
    val tbl = new Table

    val file = FileReader.read(filename)

    // First line is the header
    // Try and detect the separation character
    try {
      var header:List[String] = Nil
      var delim = ","
      while(header.length <= 1 && file.hasNext) {
        val rawHeader = file.next
        delim =
          if(rawHeader.split(",").toList.length > 1) ","
          else                                       "\t"
        header = rawHeader.split(delim).toList map {
          _.replaceAll("(^\"|\"$)", "")
        }
      }

      file.foreach((line) => {
        val splitLine = line.split(delim) map {_.toFloat}
        tbl.addRow(header.zip(splitLine))
      })
    } catch {
      case nse:java.util.NoSuchElementException =>
        logger.warn("can't find header")
    }
    tbl
  }

  def fromLists(fieldNames:List[String], data:List[List[Float]]) = {
    val tbl = new Table
    data.foreach {dr =>
      tbl.addRow(fieldNames.zip(dr))
    }
    tbl
  }

  // Some fun filters
  def fieldFilter(rmFields:List[String]) : Filter = {
    {tpl => tpl.filterKeys {k => rmFields.indexOf(k) == -1}}
  }

  def rangeFilter(ranges:List[(String, (Float, Float))]) : Filter = {
    val mins:Map[String,Float] = ranges.map({r=>(r._1,r._2._1)}).toMap
    val maxes:Map[String,Float] = ranges.map({r=>(r._1,r._2._2)}).toMap

    {tpl =>
      val minOk = mins.forall(mn => {tpl.getOrElse(mn._1, mn._2 + 1) >= mn._2})
      val maxOk = maxes.forall(mx => {tpl.getOrElse(mx._1, mx._2 - 1) <= mx._2})

      logger.debug("mn: " + minOk + " mx: " + maxOk)
      if(minOk && maxOk)
        tpl
      else
        null
    }
  }

  def notSubsetFilter(tbl:Table) : Filter = {
    {tpl =>
      var out:Table.Tuple = null
      for(r <- 0 until tbl.numRows) {
        val tpl2 = tbl.tuple(r)
        if(out == null &&
           (tpl.forall {case (fld,v) => tpl2.get(fld).forall(x=>x == v)})) {
          out = tpl
        }
      }
      out
    }
  }
}

class Table extends LazyLogging {
  val data:ArrayBuffer[Table.Tuple] = new ArrayBuffer

  def addRow(values:List[(String, Float)]) = {
    data += values.toMap
  }

  def removeRow(row:Int) = data.remove(row)

  def clear = data.clear

  def isEmpty = data.isEmpty

  def min(col:String) : Float = {
    data.map({_.get(col)}).flatten.min
  }

  def max(col:String) : Float = {
    data.map({_.get(col)}).flatten.max
  }

  def fieldNames : List[String] = {
    data.foldLeft(Set[String]())({(st:Set[String],tpl:Table.Tuple) => st ++ tpl.keys.toSet}).toList
  }

  def numRows : Int = data.size
  def numFields : Int = fieldNames.size

  def equals(other:Table) = {
    // we finish quick if tables aren't same dimensions
    if(numRows != other.numRows || numFields != other.numFields) {
      false
    } else {
      data == other.data
    }
  }

  def values(col:String) : Seq[Float] = {
    data.map({_.get(col)}).flatten.sorted
  }

  def to2dMatrix(rowField:String, colField:String, valField:String) : Grid2D = {
    // First collect all the columns from the datastore
    val rowVals = values(rowField)
    val colVals = values(colField)
    val m = new Grid2D(rowVals toList, colVals toList)

    // Now populate the matrix
    data.foreach(v => {
      // Only set the value if we have information at that point
      (v.get(rowField), v.get(colField), v.get(valField)) match {
        case (Some(rowV), Some(colV), Some(value)) =>
          m.set(rowVals.takeWhile({_ < rowV}).size,
                colVals.takeWhile({_ < colV}).size,
                value)
        case _ =>
      }
    })

    m
  }

  def iterator = data.iterator

  def columnValue(colName:String, row:Int) : Option[Float] = {
    data(row).get(colName)
  }

  def setColumnValue(colName:String, row:Int, value:Float) = {
    data(row) = data(row) + (colName -> value)
  }

  def tuple(row:Int) : Table.Tuple = {
    data(row) + (Config.rowField -> row)
  }

  def map[A](f:Table.Tuple=>A) = data.map(f)

  // Adds all rows of t to this table
  def merge(t:Table) = {
    for(r <- 0 until t.numRows)
      addRow(t.tuple(r).toList)
  }

  def filter(f:Table.Filter) : Table = {
    val outTbl = new Table
    for(r <- 0 until numRows) {
      val tpl = f(tuple(r))
      if(tpl != null)
        outTbl.addRow(tpl.toList)
    }
    outTbl
  }

  def subsample(startRow:Int, numRows:Int) : Table = {
    val t = new Table
    // silently return less than numRows if we have less
    val extractRows = math.min(numRows, this.numRows)
    for(r <- startRow until (startRow+extractRows)) {
      t.addRow(tuple(r).toList)
    }
    t
  }

  def toRanges : DimRanges = toRanges(Nil)

  def toRanges(filterFields:List[String]) : DimRanges = {
    val fns = if(filterFields == Nil) {
      fieldNames
    } else {
      fieldNames.filterNot({fn => filterFields.contains(fn)})
    }
    val ranges = fns.map({fn => (fn, (min(fn), max(fn)))}).toMap
    new DimRanges(ranges)
  }

  override def toString : String = if(numRows == 0) {
    "(empty table)"
  } else {
    val header = fieldNames.reduceLeft(_ + " " + _)
    val rows = data.map {row =>
      fieldNames.map {fn => row(fn).toString} reduceLeft(_ + " " + _)
    }
    header + "\n" + rows.reduceLeft(_ + "\n" + _)
  }

  def toCsv(filename:String) = {
    logger.info("writing " + filename + "...")
    val file = new java.io.FileWriter(filename)

    val header = if(numRows > 0) {
      val row0 = tuple(0)
      //val (hdr, _) = row0.unzip
      val hdr = row0.keys.filter({x => x != "rowNum"}).toList
      // write out the header
      file.write(hdr.mkString(",") + "\n")
      hdr
    } else {
      Nil
    }

    for(r <- 0 until numRows) {
      val tpl = tuple(r)
      val vals = header.map(tpl(_)).map(_.toString)
      file.write(vals.mkString(",") + "\n")
    }

    file.close
  }

}
