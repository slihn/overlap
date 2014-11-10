
package overlap_sparse

import scala.collection.mutable.{Set => MutSet, ArrayBuffer, Map => MutMap}
import org.ejml.simple.SimpleMatrix
import no.uib.cipr.matrix.DenseMatrix
import overlap._

case class FundPointer(start: Int, end: Int, len: Int)
case class Position(spSecurityId: Int, posSize: Double)
case class FundSecurities(securityList: ArrayBuffer[Position])

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
  val spFundMap: Map[Int, Int] = fundList.zipWithIndex.map {
    case(fundId, spFundId) => spFundId -> fundId
  }.toMap
  val fundPointerMap = MutMap[Int, FundPointer]()

  var matrixLen: Int = 0

  def generate = {
    val allFundSecurities = fundList.map {
      (fundId: Int) => fundId -> FundSecurities(ArrayBuffer[Position]())
    }.toMap
    for (ps <- data) {
      val fundId = ps.fundId
      val spSecurityId = securityMap.get(ps.securityKey).get
      val pos = Position(spSecurityId, ps.posSize)
      allFundSecurities.get(fundId).get.securityList += pos
      fundMap.get(fundId).get.spSecurityList += spSecurityId
    }
    matrixLen = 0
    for(spFundId <- 0 until fundList.length) {
      val fundId = spFundMap.get(spFundId).get
      val ds = allFundSecurities.get(fundId).get.securityList
      val start = matrixLen
      val len = ds.length
      val ptr = FundPointer(start, start+len, len)
      fundPointerMap += (spFundId -> ptr)
      matrixLen += len
    }

    // You can choose either EJML or MTJ to test their performance
    val mt = new SimpleMatrix(matrixLen, 2) // EJML
    //val mt = new DenseMatrix(matrixLen, 2) // MTJ

    Now().println("Sparse matrix created: %d" format matrixLen)

    for (spFundId <- 0 until fundList.length) {
      val fundId = spFundMap.get(spFundId).get
      val ptr = fundPointerMap.get(spFundId).get
      val ds = allFundSecurities.get(fundId).get.securityList.sortBy(_.spSecurityId)
      for (i <- 0 until ds.length) {
        val d = ds(i)
        val j = ptr.start + i
        if (j >= matrixLen) {
          println("Index too large: %d x %d vs %d" format (spFundId, d.spSecurityId, matrixLen))
          sys.error("MemoryError()")
        }
        mt.set(j,0, d.spSecurityId.toDouble)
        mt.set(j,1, d.posSize)
      }
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
    val i2 = data2.spFundId

    //println("calc funds: %d vs %d" format(fundId1, fundId2))

    val ptr1 = m.fundPointerMap.get(i1).get
    val ptr2 = m.fundPointerMap.get(i2).get

    // excluding set join
    if (matrixMode == -2) return OverlapTriple(minOverlap, crossLeft, crossRight)

    var p1 = ptr1.start
    var p2 = ptr2.start
    while (p1 < ptr1.end && p2 < ptr2.end) {
      if (matrixMode >= 0) {
        val d1 = Position(mt.get(p1, 0).toInt, mt.get(p1, 1))
        val d2 = Position(mt.get(p2, 0).toInt, mt.get(p2, 1))
        if (matrixMode <= 0) {
          if (d1.spSecurityId <= d2.spSecurityId) {  p1 += 1 }
          if (d1.spSecurityId >= d2.spSecurityId) {  p2 += 1 }
        } else {
          if (d1.spSecurityId == d2.spSecurityId) {
            val a = d1.posSize
            val b = d2.posSize
            // print "%d %d | %d = %.6f %.6f" %(i1, i2, d1.sp_security_id, a, b)
            if (a > 0.0 && b > 0.0) {
              minOverlap += min (a, b)
              crossLeft += a
              crossRight += b
            }
            p1 += 1
            p2 += 1
          } else if (d1.spSecurityId < d2.spSecurityId) {
            p1 += 1
          } else if (d1.spSecurityId > d2.spSecurityId) {
            p2 += 1
          } else {
            println("Index misaligned for sp_funds: %d x %d" format (i1, i2))
            sys.error("MemoryError()")
          }
        }
      } else {
        p1 += 1
        p2 += 1
      }
    }
    OverlapTriple(minOverlap, crossLeft, crossRight)
  }
  def min(a: Double, b: Double) = if(a < b) a else b
}
