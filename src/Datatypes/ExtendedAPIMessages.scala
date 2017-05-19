package Datatypes

import java.math.BigInteger
import java.security.interfaces.RSAPublicKey;

case class SumAll(nonce: Long, pos: Int, encrypted: Boolean, nsquare: BigInteger)
case class SumMultAllResult(nonce: Long, res: BigInteger)
case class MultAll(nonce: Long, pos: Int, encrypted: Boolean, key: RSAPublicKey)