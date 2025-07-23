package com.danielpancake.cloudfrog.services.cache

/** A trait for a token cache
  */
trait TokenCache[F[_], K, V] {
  def get(key: K): F[Option[V]]
  def set(key: K, value: V): F[Unit]
  def del(key: K): F[Long]
}
