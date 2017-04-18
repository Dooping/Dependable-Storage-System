package Datatypes

case class Tag(sn: Int, id: String) extends Ordered[Tag]{
  def compare(that: Tag) = {
    (this.sn) - (that.sn) match {
      case 0 => that.id.compare(this.id)
      case other => other
    }
  }
  
  override def toString: String =
    s"($sn, $id)"
}
case class APIWrite(nonce: Long, key: String, clientId: String, v: Entry)

case class Read(nonce: Long, key: String)
case class ReadTag(nonce: Long, key: String)
case class Write(tag: Tag, v: Entry, sig: String, nonce: Long, key: String)
case class ReadResult(tag: Tag, v: Entry, sig: String, nonce: Long, key: String)
case class ReadTagResult(tag: Tag, sig: String, nonce: Long)
case class WriteResult(nonce: Long)
case class Ack(nonce: Long)

case class CrashReplica()
case class SetByzantine(chance: Int)