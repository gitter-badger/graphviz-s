package uk.co.turingatemyhamster.graphvizs
package exec

import io.Source
import sys.process.ProcessIO
import uk.co.turingatemyhamster.graphvizs.dsl.Graph
import java.io._

/**
 * Dot binary executor.
 *
 * Usually, you will use this through the exec package object, configured with the system-global dot binary location.
 * You will use this trait directly if you want to maintain multiple dot facades.
 *
 * @author Matthew Pocock
 */

trait Exec {

  /**
   * Location of the dot binary.
   *
   * @return a File pointing to the dot binary
   */
  def dotBinary: File

  /**
   * Run the input through dot and acquire the output.
   *
   * @param from        the input value
   * @param inputFrom   implicit handler to present `from` to the stdin of dot
   * @param outputTo    implicit handler to process the stdout of dot into a `To`
   * @tparam From       the input type
   * @tparam To         the output type
   * @return            a `To` representing the output of dot
   */
  def dot2dot[From, To](from: From)(implicit inputFrom: InputHandler[From], outputTo: OutputHandler[To]): To = {

    val app = DotApp(dotBinary, DotOpts(Some(DotLayout.dot), Option(DotFormat.dot)))

    val errHandler = OutputHandler.stringOutputHandler

    val io = new ProcessIO(inputFrom.handle(from), outputTo.handle, errHandler.handle, false)

    val proc = app.process run io
    proc.exitValue() match {
      case 0 => outputTo.value
      case x => sys.error("Dot exited with error code: " + x + " with output:\n" + errHandler.value)
    }
  }
}

trait InputHandler[A] {
  def handle(a: A)(input: OutputStream)
}

object InputHandler {

  implicit object StringInputHandler extends InputHandler[String] {
    def handle(a: String)(os: OutputStream) {
      val pw = new PrintWriter(os)
      pw write a
      pw.close()
    }
  }
  
  implicit object GraphInputHandler extends InputHandler[Graph] {
    def handle(g: Graph)(os: OutputStream) {
      val pw = new PrintWriter(os)
      dsl.renderGraph(g, pw)
      pw.close()
    }
  }
  
}

trait OutputHandler[A] {
  def handle(output: InputStream)
  def value: A
}

object OutputHandler {

  implicit def stringOutputHandler: OutputHandler[String] = new OutputHandler[String] {
    var value: String = null

    def handle(out: InputStream) {
      value = Source.fromInputStream(out).mkString
      out.close()
    }
  }

  implicit def graphOutputHandler: OutputHandler[Graph] = new OutputHandler[Graph] {
    var value: Graph = null

    def handle(out: InputStream) {
      value = dsl.parseAsGraph(new InputStreamReader(out))
    }
  }
}