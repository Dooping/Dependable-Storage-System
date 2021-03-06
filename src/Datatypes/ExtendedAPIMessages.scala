package Datatypes

import java.math.BigInteger
import java.security.interfaces.RSAPublicKey;
import scala.collection.mutable.Buffer

case class SumAll(nonce: Long, pos: Int, encrypted: Boolean, nsquare: BigInteger)
case class SumMultAllResult(nonce: Long, res: BigInteger)
case class MultAll(nonce: Long, pos: Int, encrypted: Boolean, key: RSAPublicKey)

case class EntrySet(nonce: Long, set: java.util.List[String])

case class SearchEq(nonce: Long, pos: Int, value: String)
case class SearchNEq(nonce: Long, pos: Int, value: String)

case class SearchEntry(nonce: Long, value: Entry, encrypted: Boolean)
case class SearchEntryOr(nonce: Long, value: java.util.List[Entry], encrypted: Boolean)
case class SearchEntryAnd(nonce: Long, value: java.util.List[Entry], encrypted: Boolean)

case class OrderLS (nonce: Long, pos: Int)
case class OrderSL (nonce: Long, pos: Int)
case class SearchEqInt (nonce: Long, pos: Int, value: Long)
case class SearchGt (nonce: Long, pos: Int, value: Long)
case class SearchGtEq (nonce: Long, pos: Int, value: Long)
case class SearchLt (nonce: Long, pos: Int, value: Long)
case class SearchLtEq (nonce: Long, pos: Int, value: Long)
