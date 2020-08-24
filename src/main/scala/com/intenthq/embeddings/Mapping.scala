package com.intenthq.embeddings

trait Mapping[K, V, F[_]] {
  def get(key: K): F[Option[V]]
}
