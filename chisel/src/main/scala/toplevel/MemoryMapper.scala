package toplevel

import chisel3._
import chisel3.util._

/* A combinatorial memory address rewriter
 */
class MemoryMapper extends Module {
  val io = IO(new Bundle {
    val raw_addr  = Input(UInt(16.W))
    val mapped_addr = Output(UInt(16.W))
    val fsr0 = Input(UInt(16.W))
    val fsr1 = Input(UInt(16.W))
  })

  val bank_addr = io.raw_addr(6,0)
  val bank_sel = io.raw_addr(11,7)

  when (io.raw_addr < "h2000".U) {
    when (bank_addr === "h00".U) { //indf0
      io.mapped_addr := io.fsr0
    } .elsewhen (bank_addr === "h01".U) { //indf1
      io.mapped_addr := io.fsr1
    } .elsewhen (bank_addr < "h0C".U) { //other core registers
      //Core registers
      io.mapped_addr := Cat("h1C".U , 0.U(1.W), bank_addr)
    } .elsewhen (bank_addr < "h70".U) {
      //Memory
      //-16.u is +16 (shared) -32 (core registers + SFR)
      io.mapped_addr := (bank_sel * 80.U) - 16.U + bank_addr
    } .otherwise {
      //Common registers
      io.mapped_addr := bank_addr - "h70".U
    }
  }
  .elsewhen (io.raw_addr < "h7FFF".U)
  { //Linear data memory. Common 16B are not mapped
    io.mapped_addr := io.raw_addr - 16.U
  }
  .otherwise
  {
    //Flash (>8000)
    io.mapped_addr := io.raw_addr
  }
}
