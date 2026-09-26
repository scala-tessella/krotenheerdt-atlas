package atlas

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalajs.dom

import Model.*

/** The data bundle, fetched from `data/<version>/` next to the application: the index once, each class file
  * on demand (the URLs carry the version, so the browser may cache them for good).
  */
object Data:

  /** The base URL of the bundle. */
  val base: String = s"data/${DataVersion.value}/"

  private def json[A <: js.Any](path: String): Future[A] =
    dom.fetch(base + path).toFuture.flatMap { r =>
      if !r.ok then Future.failed(new RuntimeException(s"$path: HTTP ${r.status}"))
      else r.json().toFuture.map(_.asInstanceOf[A])
    }

  lazy val index: Future[AtlasIndex] = json[AtlasIndex]("index.json")

  /** The planar tilings of the lifts, fetched once (for the gallery and the lift pages). */
  lazy val tilings: Future[js.Dictionary[TilingDrawing]] = json[js.Dictionary[TilingDrawing]]("tilings.json")

  /** The vertex stars of the species, by species index, fetched once. */
  lazy val stars: Future[js.Dictionary[StarDrawing]] = json[js.Dictionary[StarDrawing]]("stars.json")

  private val patches = collection.mutable.Map.empty[String, Future[ClassPatch]]

  /** The patch of a class, fetched once. */
  def patch(id: String): Future[ClassPatch] =
    patches.getOrElseUpdate(id, json[ClassPatch](s"classes/$id.json"))
