package toplevel

import chisel3._


/* A stack
 */
class Stack extends Module {
  val io = IO(new Bundle {
    val push_addr  = Input(UInt(15.W))
    val tos = Output(UInt(15.W))
    val push = Input(Bool())
    val pop = Input(Bool())
  })

  val stack = Mem(16, UInt(15.W))
  val tosptr = RegInit(0.U(4.W))

  io.tos := stack.read(tosptr)

  when (io.push) {
    stack.write(tosptr + 1.U, io.push_addr)
    tosptr := tosptr + 1.U
  } .elsewhen (io.pop) {
    tosptr := tosptr - 1.U
  }

}
