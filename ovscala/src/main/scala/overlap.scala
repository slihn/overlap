
package overlap

import scala.collection.mutable.{Set => MutSet, ArrayBuffer}
import scala.collection.immutable.HashMap
import org.ejml.simple.SimpleMatrix
import no.uib.cipr.matrix.DenseMatrix

case class OverlapTriple(minOverlap: Double, crossLeft: Double, crossRight: Double)
case class OverlapConfig(useLarge: Boolean, matrixMode: Int)
// matrixMode: <=0: skip matrix lookup (0: including for loop; -1: excluding for loop; -2: excluding set join)
// matrixMode: 1: dense; 2: sparse

object Overlap {
  val conf = OverlapConfig(true, 1)
  def main(args: Array[String]) = {
    if (args.length == 1 && args(0) == "unit") unitTest2Funds(conf)
    if (args.length == 1 && args(0) == "all") overlapForAllFunds(conf)
    if (args.length == 1 && args(0) == "read") {
      //val (fundList, data) = OverlapData.readDataFile(conf.useLarge)
      //println2("data = %d" format data.length)
      val m = OverlapDataMatrix(conf)
      val mt = m.generate
      //println2("mt size = %d" format mt.getNumElements) // only good for ejml
    }
    println2("Done")
  }

  def unitTest2Funds(conf: OverlapConfig, debug: Boolean = false) = {
    val fundId1 = 178472
    val fundId2 = 216718

    val m = OverlapDataMatrix(conf)
    val calc = OverlapCalculator(m)
    val ovt = calc.get(fundId1, fundId2)

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
    println2("Unit test pass")
  }

  def overlapForAllFunds(conf: OverlapConfig, debug: Boolean = false) = {
    println2("Overlap start")
    val useLarge = conf.useLarge
    val cntMod = if (useLarge) 5000 else 1000

    val tmStart = millis()
    val m = OverlapDataMatrix(conf)
    val calc = OverlapCalculator(m)

    var cnt = 0
    var fundOverlap = HashMap.empty[Int, HashMap[Int, OverlapTriple]]
    for (fundId1 <- m.fundList) {
      var fundOverlap2 = HashMap.empty[Int, OverlapTriple]
      fundOverlap += fundId1 -> fundOverlap2
      for (fundId2 <- m.fundList if fundId2 > fundId1) {
        val ovt = calc.get(fundId1, fundId2)
        fundOverlap2 += fundId2 -> ovt
        cnt += 1
        if (debug || cnt % cntMod == 0 || cnt < 100) {
          val elapsed = if (cnt % 100000 == 0) "elapsed %d sec" format ((millis() - tmStart)/1000) else ""
          println("ovlp %7d  %.5f %.5f %.5f for %d vs %d %s" format
            (cnt, ovt.minOverlap, ovt.crossLeft, ovt.crossRight, fundId1, fundId2, elapsed))
        }
      }
    }
    val tmEnd = millis()
    println2("Overlap end, elapsed %d millis" format (tmEnd - tmStart))
    fundOverlap
  }
  def println2(s: String) = Now().println(s)
  def millis() = scala.compat.Platform.currentTime
}

case class SpFundInfo(fundId: Int, spFundId: Int, spSecurityList: ArrayBuffer[Int], spSecuritySet: MutSet[Int])

case class OverlapDataMatrix(conf: OverlapConfig) {
  val (fundList, data) = OverlapData.readDataFile(conf.useLarge)

  val securityList = data.map {
    _.securityKey
  }.toSet.toList

  val SecurityCnt: Int = securityList.length

  val securityMap = securityList.zipWithIndex.map { case (securityKey, spSecurityId) =>
    securityKey -> spSecurityId
  }.toMap

  val fundMap: Map[Int, SpFundInfo] = fundList.zipWithIndex.map {
    case(fundId, spFundId) =>
      fundId -> SpFundInfo(fundId, spFundId, ArrayBuffer[Int](), MutSet[Int]())
  }.toMap

  def generate = {
    // TODO: use conf.matrixMode to switch between dense and sparse
    //val mt = new SimpleMatrix(fundList.length, SecurityCnt) // EJML
    val mt = new DenseMatrix(fundList.length, SecurityCnt) // MTJ
    for (ps <- data) {
      val fundId = ps.fundId
      val spFundId = fundMap.get(fundId).get.spFundId
      val spSecurityId = securityMap.get(ps.securityKey).get
      mt.set(spFundId, spSecurityId, ps.posSize)
      fundMap.get(fundId).get.spSecurityList += spSecurityId
    }
    // Set data needs to be pre-calculated. Conversion from List to Set takes too much time later...
    for (fundId <- fundList) {
      fundMap.get(fundId).get.spSecuritySet ++= fundMap.get(fundId).get.spSecurityList.toList
    }
    Now().println("security cnt = %d" format SecurityCnt)
    mt
  }
}

case class OverlapCalculator(m: OverlapDataMatrix) {
  val matrixMode = m.conf.matrixMode
  val mt = m.generate

  def get(fundId1: Int, fundId2: Int): OverlapTriple = {
    // This is the core algorithm of the portfolio overlap
    // Overlap(i,j) = Sum_k(Min(S_i_k, S_j_k))
    // i,j: institutions, k: security id, S_i_k: position size
    var minOverlap: Double = 0.0
    var crossLeft: Double = 0.0
    var crossRight: Double = 0.0


    val data1 = m.fundMap.get(fundId1).get
    val data2 = m.fundMap.get(fundId2).get
    val i1 = data1.spFundId
    val s1 = data1.spSecuritySet
    val i2 = data2.spFundId
    val s2 = data2.spSecuritySet

    // excluding set join
    if (matrixMode == -2) return OverlapTriple(minOverlap, crossLeft, crossRight)

    val su = s1 & s2  // This intersect is an important performance step

    // exlcuding for loop
    if (matrixMode == -1) return OverlapTriple(minOverlap, crossLeft, crossRight)
    for(s <- su) {
      if (matrixMode > 0) {
        val a = mt.get(i1, s)
        val b = mt.get(i2, s)
        if (a > 0.0 && b > 0.0) {
          minOverlap += min(a, b)
          crossLeft += a
          crossRight += b
        }
      }
    }
    OverlapTriple(minOverlap, crossLeft, crossRight)
  }
  def min(a: Double, b: Double) = if(a < b) a else b
}
