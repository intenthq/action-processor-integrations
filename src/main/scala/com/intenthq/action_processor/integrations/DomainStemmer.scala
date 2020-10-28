package com.intenthq.action_processor.integrations

import com.google.common.net.InternetDomainName

import scala.util.Try

object DomainStemmer {
  // using publicsuffix.org
  def apply(url: String): String = Try(InternetDomainName.from(url).topPrivateDomain().toString).getOrElse("")
}
