
package toplevel

import chisel3._


class TopLevelTestWrapper extends Module {
  val io = IO(new Bundle {
    val flash_addr = Output(UInt(15.W))
    val flash_value = Input(UInt(15.W))
  })

  val t = Module(new Toplevel(true))
  t.io.ebus_in := 0.U
//  val smem = t.smem

  val addr = Reg(UInt(16.W))
  io.flash_addr := addr(14,0)

  when (t.io.ebus_alatch) {
    addr := t.io.ebus_out
  }
  when (t.io.ebus_read && addr > "h8000".U) {
    t.io.ebus_in := io.flash_value
  }
}


object ToplevelMain extends App {
  iotesters.Driver.execute(args, () => new TopLevelTestWrapper) {
    c => new ToplevelUnitTester(c)
  }
}

import java.io.File

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class ToplevelUnitTester(c: TopLevelTestWrapper) extends PeekPokeTester(c) {

  private val tl = c

  val flashmem = (new CompiledMem).CMem

  var testsdone = 0

  def gstep(x:Int) = {
    for( a <- 1 to x){
      val addr = peek(tl.io.flash_addr)
      val res : BigInt = flashmem get addr.toInt match {
        case Some(result) => result
        case None => 0
      }
      Console.println(s"poking flash $addr->$res")
      poke(tl.io.flash_value, res)

      var rega = peekAt(tl.t.smem, 36)
      var valuereg = peekAt(tl.t.smem, 37)
      var expectreg = peekAt(tl.t.smem, 38)
      var testreg = peekAt(tl.t.smem, 39)
      Console.println(s"a=$rega v=$valuereg e=$expectreg t=$testreg")
      if (testreg > 0) {
        testsdone = testsdone + 1
        Console.println(s"PERFORMING TEST $expectreg == $valuereg")
        expect(valuereg == expectreg, s"expected ${expectreg} got ${valuereg}")
      }
      //Console.println(s"ram a=$rega b=$regb")
      step(1)
    }
  }
  reset(10)
  gstep(5*700)
  Console.println("==================================")
  Console.println(s"performed $testsdone expect tests")
}
