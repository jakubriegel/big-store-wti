package eu.jrie.put.wti.bigstore.hub.api.rest

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.PathMatcher
import akka.http.scaladsl.server.PathMatchers.NumberMatcher

class ModuloMatcher(
                     private val divisor: Long,
                     private val n: Long
                   ) extends NumberMatcher[Long](Long.MaxValue, 10) {

  override def apply(path: Uri.Path): PathMatcher.Matching[Tuple1[Long]] = {
    super.apply(path) match {
      case PathMatcher.Matched(pathRest, extractions) =>
        if (extractions._1 % divisor == n) PathMatcher.Matched(pathRest, extractions)
        else PathMatcher.Unmatched
      case PathMatcher.Unmatched => PathMatcher.Unmatched
    }
  }

  override def fromChar(c: Char): Long = fromDecimalChar(c)
}
