package Datatypes

import java.math.BigInteger

case class SumAll(nonce: Long, pos: Int, encrypted: Boolean, nsquare: BigInteger)
case class SumAllResult(nonce: Long, res: BigInteger)