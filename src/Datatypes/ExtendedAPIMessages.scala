package Datatypes

import java.math.BigInteger
import java.security.interfaces.RSAPublicKey;
import scala.collection.mutable.Buffer

case class SumAll(nonce: Long, pos: Int, encrypted: Boolean, nsquare: BigInteger)
case class SumMultAllResult(nonce: Long, res: BigInteger)
case class MultAll(nonce: Long, pos: Int, encrypted: Boolean, key: RSAPublicKey)

case class EntrySet(nonce: Long, set: Buffer[Entry])
case class SearchEq(nonce: Long, pos: Int, value: String)
case class SearchNEq(nonce: Long, pos: Int, value: String)



