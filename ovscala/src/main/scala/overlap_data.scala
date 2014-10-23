
package overlap

import com.github.tototoshi.csv.CSVReader
import java.io.Reader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{HashSet => MutSet}

case class PosSizeRow (fundId: Int, securityKey: String, posSize: Double)

object OverlapData {
  val dataFileSmall = "../data/overlap_data_small.csv"
  val dataFileLarge = "../data/overlap_data.zip"

  val headerRef = List("fund_id", "security_key", "pos_size").toBuffer

  def readSmallSample: Reader = {
    import java.io.FileReader
    Now().println("Use small sample file")
    val reader = new FileReader(dataFileSmall)
    reader
  }

  def readLargeSample: Reader = {
    import java.util.zip.ZipFile
    import java.io.InputStreamReader

    Now().println("Use large sample file")
    val csvFileInZip = "overlap_data.csv"
    val zipFile = new ZipFile(dataFileLarge)
    val zipEntry = zipFile.getEntry(csvFileInZip)
    val dataIO = new InputStreamReader(zipFile.getInputStream(zipEntry))
    dataIO
  }

  def readDataFile(useLarge: Boolean): (List[Int], ArrayBuffer[PosSizeRow]) = {
    import scala.collection.JavaConversions._

    val reader = if(useLarge) readLargeSample else readSmallSample
    val rd = CSVFormat.EXCEL.withSkipHeaderRecord(false).parse(reader)

    val header = ArrayBuffer[String]()
    val data = ArrayBuffer[PosSizeRow]()
    val fundSet = MutSet[Int]()

    for(row: CSVRecord <- rd) {
      if (header.length == 0) {
        header ++= List(row.get(0), row.get(1), row.get(2))
        assert(header == headerRef)
      }
      else {
        val d = PosSizeRow(row.get(0).toInt, row.get(1), row.get(2).toDouble)
        data += d
        fundSet += d.fundId
      }
    }
    reader.close()
    val fundList = fundSet.toList.sorted
    println("%s: fund_list = %d rows = %d" format (Now().asISO, fundList.length, data.length))

    assert(fundList.contains(178472))
    assert(fundList.length == (if(useLarge) 3839 else 837))

    (fundList, data)
  }
}

