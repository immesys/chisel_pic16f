package toplevel

import chisel3._
import chisel3.util.Enum
import gcd.GCD

class Toplevel extends Module {
  val io = IO(new Bundle {
    val pins_left_in   = Input(UInt(13.W))
    val pins_left_out  = Output(UInt(13.W))
    val pins_left_en   = Output(UInt(13.W))
    val pins_right_in  = Input(UInt(8.W))
    val pins_right_out = Output(UInt(8.W))
    val pins_right_en  = Output(UInt(8.W))
  })


  io.pins_right_out := "hFF".U;
  io.pins_right_en := "h00".U;
  io.pins_left_en := "b1111111111111".U;
  io.pins_left_out := 0.U

  //values read from the fsr addresses
  var indf0 = Reg(UInt(8.W))
  var indf1 = Reg(UInt(8.W))

  var instruction = Reg(UInt(14.W))

  var pc = Reg(new Bundle {
    val pch = UInt(8.W)
    val pcl = UInt(8.W)
  })
  var status = Reg(UInt(8.W))
  var fsr0 = Reg(new Bundle{
    val fsr0h = UInt(8.W)
    val fsr0l = UInt(8.W)
  })
  var fsr1 = Reg(new Bundle{
    val fsr1h = UInt(8.W)
    val fsr1l = UInt(8.W)
  })
  val bsr = RegInit(0.U(8.W))
  val wreg = RegInit(0.U(8.W))
  val pclath = RegInit(0.U(8.W))
  val intcon = RegInit(0.U(8.W))

  val bus_value = Wire(UInt(16.W))


  //Memory mapping
  val raw_addr = Wire(UInt(16.W))
  val mapped_addr = Wire(UInt(16.W))
  val mapper = Module(new MemoryMapper)
  mapped_addr := mapper.io.mapped_addr
  mapper.io.raw_addr := raw_addr
  raw_addr := 0.U

  //Flash
  val flash = SyncReadMem(4096, UInt(14.W))
  val flash_read_val = flash.read(mapped_addr(12,0))

  //Flash testing
  val flash_write = Wire(Bool())
  flash_write := false.B
  val flash_write_addr = Wire(UInt(12.W))
  flash_write_addr := 0.U
  val flash_write_val = Wire(UInt(14.W))
  flash_write_val := 0.U

  //Bus memory mux
  val mem = SyncReadMem(4096, UInt(8.W))
  val sram_read_value = mem.read(mapped_addr(11,0))
  val bus_zero :: bus_sram :: Nil = Enum(2)
  val bus_in_sel = Wire(UInt(3.W))
  val bus_out_sel = Wire(UInt(3.W))
  val bus_write = Wire(Bool())
  bus_in_sel := bus_zero
  bus_out_sel := bus_zero
  bus_write := false.B
  val cycle = Reg(UInt(4.W))

  when (cycle === 4.U)
  {
    cycle := 0.U
  } .otherwise {
    cycle := cycle + 1.U
  }

  when (cycle === 0.U)
  {
//    raw_addr := pc.asUInt
//    val nextPC = pc.asUInt + 1.U
//    pc.pch := nextPC(15,8)
//    pc.pcl := nextPC(7,0)
//    bus_value := DontCare
//    addr := mapped_addr

    //PC -> rawaddr
    //mapped_addr -> addr reg
    //            -> flash read addr
  } .elsewhen (cycle === 1.U) {
    //flash value -> instruction register

  } .elsewhen (cycle === 2.U) {
    //instruction addr -> sram read addr


  } .elsewhen (cycle === 3.U)
  {
    //instruction operands -> alu registers
    //ALU

  }
  .elsewhen (cycle === 4.U)
  {
    //Writeback
    //alu writeback

  }

  //Flash memory

  when (flash_write)
  {
    flash.write(flash_write_addr, flash_write_val)
  }


  when (mapped_addr < "h2000".U && bus_write)
  {
    mem.write(mapped_addr(12,0), bus_value(7,0))
  }

  when (bus_in_sel === bus_sram)
  {
    when(mapped_addr < "h2000".U)
    {
      bus_value := sram_read_value
    } .elsewhen(mapped_addr === "h1C00".U)
    {
      //You can't set FSR to the indf address
      bus_value := 0.U
    } .elsewhen(mapped_addr === "h1C01".U)
    {
      //You can't set FSR to the indf address
      bus_value := 0.U
    } .elsewhen(mapped_addr === "h1C02".U)
    {
      bus_value := pc.pcl
    } .elsewhen(mapped_addr === "h1C03".U)
    {
      bus_value := status
    } .elsewhen(mapped_addr === "h1C04".U)
    {
      bus_value := fsr0.fsr0l
    } .elsewhen(mapped_addr === "h1C05".U)
    {
      bus_value := fsr0.fsr0h
    } .elsewhen(mapped_addr === "h1C06".U)
    {
      bus_value := fsr1.fsr1l
    } .elsewhen(mapped_addr === "h1C07".U)
    {
      bus_value := fsr1.fsr1h
    } .elsewhen(mapped_addr === "h1C08".U)
    {
      bus_value := bsr
    } .elsewhen(mapped_addr === "h1C09".U)
    {
      bus_value := wreg
    } .elsewhen(mapped_addr === "h1C0A".U)
    {
      bus_value := pclath
    } .elsewhen(mapped_addr === "h1C0B".U)
    {
      bus_value := intcon
    } .otherwise {
      bus_value := 0.U
    }
  } .otherwise {
    bus_value := 0.U
  }

  when(bus_out_sel === bus_sram && bus_write)
  {
    when(mapped_addr < "h2000".U)
    {
      mem.write(mapped_addr(11,0), bus_value)
    } .elsewhen(mapped_addr === "h1C00".U)
    {
      //Can't write to indf through fsr
    } .elsewhen(mapped_addr === "h1C01".U)
    {
      //Can't write to indf through fsr
    } .elsewhen(mapped_addr === "h1C02".U)
    {
      pc.pcl := bus_value
      pc.pch := pclath
    } .elsewhen(mapped_addr === "h1C03".U)
    {
      status := bus_value
    } .elsewhen(mapped_addr === "h1C04".U)
    {
      fsr0.fsr0l := bus_value
    } .elsewhen(mapped_addr === "h1C05".U)
    {
      fsr0.fsr0h := bus_value
    } .elsewhen(mapped_addr === "h1C06".U)
    {
      fsr1.fsr1l := bus_value
    } .elsewhen(mapped_addr === "h1C07".U)
    {
      fsr1.fsr1h := bus_value
    } .elsewhen(mapped_addr === "h1C08".U)
    {
      bsr := bus_value
    } .elsewhen(mapped_addr === "h1C09".U)
    {
      wreg := bus_value
    } .elsewhen(mapped_addr === "h1C0A".U)
    {
      pclath := bus_value
    } .elsewhen(mapped_addr === "h1C0B".U)
    {
      intcon := bus_value
    } .otherwise {
      //Nothing
    }
  }
}

object ToplevelDriver extends App {
  chisel3.Driver.execute(args, () => {
    new Toplevel
  })
}
