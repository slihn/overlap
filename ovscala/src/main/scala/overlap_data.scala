
package overlap

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{HashSet => MutSet}

case class PosSizeRow (fundId: Int, securityKey: String, posSize: Double)

object OverlapData {
  val dataFile = "../data/overlap_data_small.csv"
  val headerRef = List("fund_id", "security_key", "pos_size").toBuffer

  def readDataFile: (List[Int], ArrayBuffer[PosSizeRow]) = {
    import com.github.tototoshi.csv._

    val reader = CSVReader.open(dataFile)
    val rd = reader.iterator

    val header = ArrayBuffer[String]()
    val data = ArrayBuffer[PosSizeRow]()
    val fundSet = MutSet[Int]()

    for(row <- rd) {
      if (header.length == 0) {
        header ++= row
        assert(header == headerRef)
      }
      else {
        val d = PosSizeRow(row(0).toInt, row(1), row(2).toDouble)
        data += d
        fundSet += d.fundId
      }
    }
    reader.close()
    val fundList = fundSet.toList.sorted
    println("%s: fund_list = %d rows = %d" format (Now().asISO, fundList.length, data.length))

    assert(fundList.contains(178472))
    assert(fundList.length == 837)

    (fundList, data)
  }
}

