
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
  //  printf("EBUS ADDR LATCH addr=%x\n", t.io.ebus_out)
    addr := t.io.ebus_out
  }
  when (t.io.ebus_read && addr > "h8000".U) {
    t.io.ebus_in := io.flash_value
  }
  when (t.io.ebus_write) {
    printf("EBUS WRITE addr=%x val=%x\n", addr, t.io.ebus_out)
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

      var valuereg = peekAt(tl.t.smem, 0)
      var expectreg = peekAt(tl.t.smem, 1)
      var testreg = peekAt(tl.t.smem, 2)
      if (testreg > 0) {
        testsdone = testsdone + 1
        Console.println(s"PERFORMING TEST $expectreg == $valuereg")
        expect(valuereg == expectreg, s"expected ${expectreg} got ${valuereg}")
      }
      step(1)
    }
  }
  reset(10)
  gstep(5*700)
  Console.println("==================================")
  Console.println(s"performed $testsdone expect tests")
}
