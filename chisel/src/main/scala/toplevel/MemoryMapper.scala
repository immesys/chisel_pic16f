package toplevel

import chisel3._


/* A combinatorial memory address rewriter
 */
class MemoryMapper extends Module {
  val io = IO(new Bundle {
    val raw_addr  = Input(UInt(16.W))
    val mapped_addr = Output(UInt(16.W))
  })

  val bank_addr = io.raw_addr(7,0)
  val bank_sel = io.raw_addr(15,8)
  
  when (io.raw_addr < "h2000".U) {
    when (bank_addr < "h0C".U) {
      //Core registers
      io.mapped_addr := "h1C00".U | bank_addr
    } .elsewhen (bank_addr < "h70".U) {
      //Memory
      //4.u is +16 (shared) -12 (core registers)
      io.mapped_addr := (bank_sel * 80.U) + 4.U + bank_addr
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
