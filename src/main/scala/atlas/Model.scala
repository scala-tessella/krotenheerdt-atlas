package atlas

import scala.scalajs.js

/** Typed views of the data bundle's JSON (written by the atlas export of the uniform-tilings repository): the
  * parsed objects are read in place, never copied, since a class carries up to some two thousand cells.
  */
object Model:

  /** `index.json`: the metadata, the sequence and every class without its cells. */
  @js.native
  trait AtlasIndex extends js.Object:
    val version: String                 = js.native
    val meta: Meta                      = js.native
    val sequence: js.Array[SequenceRow] = js.native
    val classes: js.Array[ClassEntry]   = js.native

  @js.native
  trait Meta extends js.Object:
    /** The cell types by ordinal (the `k` of a cell): tet, cube, oct, …, P12. */
    val cells: js.Array[String] = js.native

    /** The full name of each cell type. */
    val cellNames: js.Dictionary[String] = js.native

    /** The species labels by species index. */
    val species: js.Dictionary[String] = js.native

  /** One value N_k of the sequence, with its status and composition. */
  @js.native
  trait SequenceRow extends js.Object:
    val k: Int         = js.native
    val n: Int         = js.native
    val status: String = js.native
    val note: String   = js.native

  /** A class as the index lists it. */
  @js.native
  trait ClassEntry extends js.Object:
    val id: String              = js.native
    val k: Int                  = js.native
    val name: String            = js.native
    val cat: String             = js.native
    val world: String           = js.native
    val cells: js.Array[String] = js.native
    val pair: String            = js.native
    val species: js.Array[Int]  = js.native

    /** The chambers of the minimal Delaney–Dress symbol; null for the uniform honeycombs. */
    val chambers: Int | Null = js.native
    val source: String       = js.native
    val key: String          = js.native
    val word: String         = js.native

  /** `classes/<id>.json`: the patch of one class. */
  @js.native
  trait ClassPatch extends js.Object:
    val id: String = js.native
    val k: Int     = js.native

    /** The species label of each vertex orbit. */
    val orbits: js.Array[String] = js.native
    val cells: js.Array[Cell]    = js.native

  /** A solid cell: its type ordinal, vertices, faces as vertex indices, and the orbit of each vertex. */
  @js.native
  trait Cell extends js.Object:
    val k: Int                        = js.native
    val v: js.Array[js.Array[Double]] = js.native
    val f: js.Array[js.Array[Int]]    = js.native
    val o: js.Array[Int]              = js.native

  /** The chambers of a class, if known. */
  def chambersOf(c: ClassEntry): Option[Int] = Option(c.chambers).map(_.asInstanceOf[Int])
