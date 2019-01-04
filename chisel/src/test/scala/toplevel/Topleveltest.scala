
package toplevel

import chisel3._

object ToplevelMain extends App {
  iotesters.Driver.execute(args, () => new Toplevel) {
    c => new ToplevelUnitTester(c)
  }
}

import java.io.File

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class ToplevelUnitTester(c: Toplevel) extends PeekPokeTester(c) {

  private val tl = c

  val flashmem = (new CompiledMem).CMem
/*  val flashmem = Map(
    0 -> "b00_0011_0_000_1001".U.litValue //incf w
    1 -> "b00_0011_0_000_1001".U.litValue //incf w
  )
  */
  def gstep(x:Int) = {
    for( a <- 1 to x){
      val addr = peek(tl.io.flash_read_addr)
      val res : BigInt = flashmem get addr.toInt match {
        case Some(result) => result
        case None => 0
      }
      Console.println(s"poking flash $addr->$res")
      poke(tl.io.flash_read_val, res)
      var rega = peekAt(tl.mem, 36)
      var regb = peekAt(tl.mem, 37)
      Console.println(s"ram a=$rega b=$regb")
      step(1)
    }
  }
  reset(10)
  gstep(6*10)
  expect(tl.io.w, 1)
}
