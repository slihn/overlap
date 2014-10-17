
package overlap

import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.HashMap
import org.ejml.simple.SimpleMatrix

case class OverlapTriple(minOverlap: Double, crossLeft: Double, crossRight: Double)

object Overlap {
  def main(args: Array[String]) = {
    println("%s: Read Sample Data" format (Now().asISO))
    if (args.length == 1 && args(0) == "unit") unitTest2Funds()
    if (args.length == 1 && args(0) == "all") overlapForAllFunds()
    if (args.length == 1 && args(0) == "read") {
      val mt = OverlapDataMatrix.generate
      println("%s: mt size = %d" format (Now().asISO, mt.getNumElements))
    }
    println("%s: Done" format (Now().asISO))
  }

  def unitTest2Funds(debug: Boolean = false) = {
    val fundId1 = 178472
    val fundId2 = 216718

    val ovt = OverlapCalculator.get(fundId1, fundId2)

    // reference data, pre - calculated from another source
    val minOverlapRef: Double = 0.26660
    val crossLeftRef: Double  = 0.44561
    val crossRightRef: Double = 0.27654

    println("ovlp  %.5f vs %.5f for %d vs %d" format(minOverlapRef, ovt.minOverlap, fundId1, fundId2))
    println("left  %.5f vs %.5f for %d vs %d" format(crossLeftRef, ovt.crossLeft, fundId1, fundId2))
    println("right %.5f vs %.5f for %d vs %d" format(crossRightRef, ovt.crossRight, fundId1, fundId2))
    assert(("%.5f" format minOverlapRef) == ("%.5f" format ovt.minOverlap))
    assert(("%.5f" format crossLeftRef) == ("%.5f" format ovt.crossLeft))
    assert(("%.5f" format crossRightRef) == ("%.5f" format ovt.crossRight))
    println2("unit test pass")
  }

  def overlapForAllFunds(debug: Boolean = false) = {
    println2("Overlap start")

    val tmStart = millis()

    var cnt = 0
    var fundOverlap = HashMap.empty[Int, HashMap[Int, OverlapTriple]]
    for (fundId1 <- OverlapDataMatrix.fundList) {
      var fundOverlap2 = HashMap.empty[Int, OverlapTriple]
      fundOverlap += fundId1 -> fundOverlap2
      for (fundId2 <- OverlapDataMatrix.fundList if fundId2 > fundId1) {
        val ovt = OverlapCalculator.get(fundId1, fundId2)
        fundOverlap2 += fundId2 -> ovt
        cnt += 1
        if (debug || cnt % 1000 == 0 || cnt < 100) {
          println("ovlp %7d  %.5f %.5f %.5f for %d vs %d" format
            (cnt, ovt.minOverlap, ovt.crossLeft, ovt.crossRight, fundId1, fundId2))
        }
      }
    }
    val tmEnd = millis()
    println2("Overlap end, elapsed %d millis" format (tmEnd - tmStart))
    fundOverlap
  }
  def println2(s: String) = println("%s: %s" format (Now().asISO, s))
  def millis() = scala.compat.Platform.currentTime
}

case class SpFundInfo(fundId: Int, spFundId: Int, spSecurityList: ArrayBuffer[Int])

object OverlapDataMatrix {
  val (fundList, data) = OverlapData.readDataFile

  val securityList = data.map {
    _.securityKey
  }.toSet.toList

  val SecurityCnt = securityList.length

  val securityMap = securityList.zipWithIndex.map { case (securityKey, spSecurityId) =>
    securityKey -> spSecurityId
  }.toMap

  val fundMap: Map[Int, SpFundInfo] = fundList.zipWithIndex.map {
    case(fundId, spFundId) =>
      fundId -> SpFundInfo(fundId, spFundId, ArrayBuffer[Int]())
  }.toMap

  def generate = {
    val mt = new SimpleMatrix(fundList.length, SecurityCnt)
    for (ps <- data) {
      val fundId = ps.fundId
      val spFundId = fundMap.get(fundId).get.spFundId
      val spSecurityId = securityMap.get(ps.securityKey).get
      mt.set(spFundId, spSecurityId, ps.posSize)
      fundMap.get(fundId).get.spSecurityList += spSecurityId
    }
    println("%s: security cnt = %d" format (Now().asISO, SecurityCnt))
    mt
  }
}

object OverlapCalculator {
  val mt = OverlapDataMatrix.generate
  def get(fundId1: Int, fundId2: Int) = {
    // Overlap(i,j) = Sum_k(Min(S_i_k, S_j_k))
    // i,j: institutions, k: security id, S_i_k: position size
    var minOverlap: Double = 0.0
    var crossLeft: Double = 0.0
    var crossRight: Double = 0.0

    val data1 = OverlapDataMatrix.fundMap.get(fundId1).get
    val data2 = OverlapDataMatrix.fundMap.get(fundId2).get
    val i1: Int = data1.spFundId
    val s1 = data1.spSecurityList.toList
    val i2 = data2.spFundId
    val s2 = data2.spSecurityList.toList

    val su: Set[Int] = s1.toSet & s2.toSet  // This intersect is an important performance step
    for(s <- su) {
      val a = mt.get(i1, s)
      val b = mt.get(i2, s)
      if (a > 0.0 && b > 0.0) {
        minOverlap += min(a, b)
        crossLeft += a
        crossRight += b
      }
    }
    OverlapTriple(minOverlap, crossLeft, crossRight)
  }
  def min(a: Double, b: Double) = if(a < b) a else b
}
