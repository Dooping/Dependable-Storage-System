package Datatypes

case class Tag(sn: Int, id: String)
case class Read(nonce: Int, key: String)
case class ReadTag(nonce: Int, key: String)
case class Write(tag: Tag, v: Entry, sig: String, nonce: Int, key: String)
case class ReadResult(tag: Tag, v: Entry, sig: String, nonce: Int)
case class ReadTagResult(tag: Tag, sig: String, nonce: Int)
case class WriteResult(nonce: Int)