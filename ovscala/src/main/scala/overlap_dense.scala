
package overlap_dense

import scala.collection.mutable.{Set => MutSet, ArrayBuffer}
import org.ejml.simple.SimpleMatrix
import no.uib.cipr.matrix.DenseMatrix
import overlap._

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
    // TODO: this switch is a bit ugly, is there a better way to deal with this?
    //val mt = new SimpleMatrix(fundList.length, SecurityCnt) // EJML
    val mt = new DenseMatrix(fundList.length, SecurityCnt) // MTJ
    Now().println("Dense matrix created")
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

case class OverlapCalculator(m: OverlapDataMatrix) extends AbstractOverlapCalculator {
  val matrixMode = m.conf.matrixMode
  val mt = m.generate
  val fundList = m.fundList

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
