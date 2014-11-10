
package overlap

import scala.collection.mutable.{Set => MutSet, ArrayBuffer}
import scala.collection.immutable.HashMap

case class OverlapTriple(minOverlap: Double, crossLeft: Double, crossRight: Double)
case class OverlapConfig(useLarge: Boolean, matrixMode: Int)
// matrixMode: <=0: skip matrix lookup (0: including for loop; -1: excluding for loop; -2: excluding set join)
// matrixMode: 1: dense; 2: sparse

object Overlap {
  val conf = OverlapConfig(useLarge = true, matrixMode = 2)
  def main(args: Array[String]) = {
    if (args.length == 1 && args(0) == "unit") unitTest2Funds(conf)
    if (args.length == 1 && args(0) == "all") overlapForAllFunds(conf)
    if (args.length == 1 && args(0) == "read") {
      val m = overlap_dense.OverlapDataMatrix(conf)
      val mt = m.generate
      //println2("mt size = %d" format mt.getNumElements) // only good for ejml
    }
    println2("Done")
  }
  def getCalculator(conf: OverlapConfig) = if ( conf.matrixMode == 1 ) {
    val m = overlap_dense.OverlapDataMatrix(conf)
    overlap_dense.OverlapCalculator(m)
  } else {
    val m = overlap_sparse.OverlapDataMatrix(conf)
    overlap_sparse.OverlapCalculator(m)
  }

  def unitTest2Funds(conf: OverlapConfig, debug: Boolean = false) = {
    val fundId1 = 178472
    val fundId2 = 216718

    val calc = getCalculator(conf)
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
    val cntMod = if (useLarge) 20000 else 1000

    val tmStart = millis()
    val calc = getCalculator(conf)

    var cnt = 0
    var fundOverlap = HashMap.empty[Int, HashMap[Int, OverlapTriple]]
    for (fundId1 <- calc.fundList) {
      var fundOverlap2 = HashMap.empty[Int, OverlapTriple]
      fundOverlap += fundId1 -> fundOverlap2
      for (fundId2 <- calc.fundList if fundId2 > fundId1) {
        val ovt = calc.get(fundId1, fundId2)
        fundOverlap2 += fundId2 -> ovt
        cnt += 1
        if (debug || cnt % cntMod == 0 || cnt < 100) {
          val elapsed = if (cnt % 500000 == 0) "elapsed %d sec" format ((millis() - tmStart)/1000) else ""
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

abstract class AbstractOverlapCalculator {
  def get(fundId1: Int, fundId2: Int): OverlapTriple
  val fundList: List[Int]
}
