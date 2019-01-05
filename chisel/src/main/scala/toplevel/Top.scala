package toplevel

import chisel3._
import chisel3.util._
import gcd.GCD
import chisel3.util.experimental.loadMemoryFromFile

class Toplevel extends Module {
  val io = IO(new Bundle {
    val pins_left_in   = Input(UInt(13.W))
    val pins_left_out  = Output(UInt(13.W))
    val pins_left_en   = Output(UInt(13.W))
    val pins_right_in  = Input(UInt(8.W))
    val pins_right_out = Output(UInt(8.W))
    val pins_right_en  = Output(UInt(8.W))

    val flash_read_addr = Output(UInt(8.W))
    val flash_read_val = Input(UInt(14.W))
    val w = Output(UInt(8.W))
    val pcl = Output(UInt(8.W))
  })

  val x = (new CompiledMem).CMem

  io.pins_right_out := "hFF".U;
  io.pins_right_en := "h00".U;
  io.pins_left_en := "b1111111111111".U;
  io.pins_left_out := 0.U

  //values read from the fsr addresses
  var indf0 = Reg(UInt(8.W))
  var indf1 = Reg(UInt(8.W))

  var instruction = Reg(UInt(14.W))

  class PCBundle extends Bundle {
    val pch = UInt(7.W)
    val pcl = UInt(8.W)
  }
  val pc = RegInit(0.U.asTypeOf(new PCBundle))
  val status = Reg(UInt(8.W))
  val fsr0 = Reg(new Bundle{
    val fsr0h = UInt(8.W)
    val fsr0l = UInt(8.W)
  })
  val fsr1 = Reg(new Bundle{
    val fsr1h = UInt(8.W)
    val fsr1l = UInt(8.W)
  })
  val bsr = RegInit(0.U(8.W))
  val wreg = RegInit(0.U(8.W))
  val pclath = RegInit(0.U(8.W))
  val intcon = RegInit(0.U(8.W))

  io.w := wreg
  io.pcl := pc.pcl

  val bus_value = Wire(UInt(16.W))
  //For saving the address across cycles
  val addr = Reg(UInt(16.W))

  //Memory mapping
  val raw_addr = Wire(UInt(16.W))
  val mapped_addr = Wire(UInt(16.W))
  val mapper = Module(new MemoryMapper)
  mapped_addr := mapper.io.mapped_addr
  mapper.io.raw_addr := raw_addr
  raw_addr := 7.U

  //Flash
  //val flash = SyncReadMem(4096, UInt(14.W))
  //val flash_read_val = flash.read(mapped_addr(12,0))
  val flash_read_val = io.flash_read_val
  io.flash_read_addr := mapped_addr(12,0)

  //Flash testing
  val flash_write = Wire(Bool())
  flash_write := false.B
  val flash_write_addr = Wire(UInt(12.W))
  flash_write_addr := 0.U
  val flash_write_val = Wire(UInt(14.W))
  flash_write_val := 0.U

  //Bus memory mux
  val mem = Mem(4096, UInt(8.W))

  val sram_read_value = mem.read(mapped_addr(11,0))
  val bus_zero :: bus_sram :: bus_alu :: Nil = Enum(3)
  val bus_in_sel = Wire(UInt(3.W))
  val bus_out_sel = Wire(UInt(3.W))
  val bus_write = Wire(Bool())
  bus_in_sel := bus_zero
  bus_out_sel := bus_zero
  bus_write := false.B
  val cycle = Reg(UInt(4.W))
  val alu_res_reg = Reg(UInt(8.W))
  //Op decode
  val opdecode = Module(new IDecode)
  opdecode.io.instruction := instruction
  val signals = opdecode.io.signals

  //ALU
  val alu1 = Wire(UInt(8.W))
  val alu2 = Wire(UInt(8.W))
  val alu_res = Wire(UInt(8.W))
  val alu_pre_res = Wire(UInt(9.W))
  val alu_status_res = Wire(new Status)

  when (cycle === 4.U)
  {
    cycle := 0.U
  } .otherwise {
    cycle := cycle + 1.U
  }

  when (cycle === 0.U)
  {
      raw_addr := Cat(1.U, pc.asUInt)
      val nextPC = pc.asUInt + 1.U
      pc.pch := nextPC(14,8)
      pc.pcl := nextPC(7,0)
      addr := raw_addr
      printf("cycle is 0, pc=%x (h%x ----------------- d%d)\n", raw_addr, mapped_addr, mapped_addr(11,0))
  } .elsewhen (cycle === 1.U) {
    //This might not be necessary, but it's needed for sim
    raw_addr := addr

    //flash value -> instruction register
    printf("cycle is 1, flashval = %b\n", flash_read_val)
    instruction := flash_read_val
  } .elsewhen (cycle === 2.U) {

    //instruction addr -> sram read addr
    printf("cycle is 2, sigAddr is %x\n", signals.Address)
  //  bus_in_sel := bus_sram
    raw_addr := signals.Address
  } .elsewhen (cycle === 3.U) {
    printf("cycle is 3, bus_value is %x alu2 is %x\n", bus_value, alu2)
    raw_addr := signals.Address
    bus_in_sel := bus_sram
    alu_res_reg := alu_res
    status := alu_status_res.asUInt
  } .elsewhen (cycle === 4.U)
  {
    raw_addr := signals.Address
    when (signals.DestF) {
      bus_in_sel := bus_alu
      bus_out_sel := bus_sram
      bus_write := true.B
    } .otherwise {
      bus_out_sel := bus_zero
      bus_write := false.B
      wreg := alu_res_reg
    }
    printf("cy4 rawaddr=%x mapped=%x\n", raw_addr, mapped_addr)
    printf("cycle is 4, wreg=%x (%b) df=%b status=%b\n", wreg, wreg, signals.DestF, status)
  }


//  when (mapped_addr < "h1000".U && bus_write)
//  {
//    mem.write(mapped_addr(11,0), bus_value(7,0))
//  }

  when (bus_in_sel === bus_sram)
  {
    when(mapped_addr < "h1000".U)
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
  } .elsewhen (bus_in_sel === bus_alu) {
    bus_value := alu_res_reg
  } .otherwise {
    bus_value := 0.U
  }

  when(bus_out_sel === bus_sram && bus_write)
  {
    when(mapped_addr < "h1000".U)
    {
      printf("writing to memory addr=%x val=%x\n", mapped_addr(11,0), bus_value)
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

  //ALU
  val ( aAdd :: aAddWithCarry :: aAnd :: aRightShift :: aArithRightShift ::
  aLeftShift :: aIdentity1 :: aIdentity2 ::
  aInclOr ::
  aRotateLeftCarry :: aRotateRightCarry ::
  aSwapNibbles2 ::
  aXor ::
  aBitTestSet :: aBitTestClear :: Nil ) = Enum(15)
  val srcBus :: srcLiteral :: srcW :: Nil = Enum(3)

  when (signals.Source1 === srcW)
  {
    when (signals.Complement1) {
      alu1 := (-(wreg.asSInt)).asUInt
    } .otherwise {
      alu1 := wreg
    }
  } .otherwise {
    when (signals.Complement1) {
      alu1 := (-(bus_value.asSInt)).asUInt
    } .otherwise {
      alu1 := bus_value
    }
  }


  when (signals.Source2 === srcBus) {
    when (signals.Complement2) {
      alu2 := (-(bus_value.asSInt)).asUInt
    } .otherwise {
      alu2 := bus_value
    }
  } .otherwise {
    when (signals.Complement2) {
      alu2 := (-(signals.Literal2.asSInt)).asUInt
    } .otherwise {
      alu2 := signals.Literal2
    }
  }


  alu_res := alu_pre_res
  when (signals.SetZero) {
    alu_status_res.Zero := alu_res === 0.U
  } .otherwise {
    alu_status_res.Zero := status(2)
  }
  when (signals.SetCarry) {
    alu_status_res.Carry := alu_pre_res(8) === 1.U
  } .otherwise {
    alu_status_res.Carry := status(0)
  }
  alu_status_res.Res := 0.U
  alu_status_res.DigitCarry := false.B
  alu_pre_res := 0.U
  switch (signals.Operation) {
    is (aAdd) {
      alu_pre_res := alu1 +& alu2
      printf("add alu1=%x alu2=%x res=%x\n", alu1, alu2, alu_pre_res)
    }
    is (aAddWithCarry) {
      alu_pre_res := (alu1 +& alu2) + status(0).asUInt
    }
    is (aAnd) {
      alu_pre_res := alu1 & alu2
    }
    is (aRightShift) {
      alu_pre_res := alu2 >> 1
    }
    is (aIdentity1) {
      alu_pre_res := alu1
    }
    is (aIdentity2) {
      alu_pre_res := alu2
    }
    is (aArithRightShift) {
      alu_pre_res := (alu2.asSInt >> 1).asUInt
    }
    is (aLeftShift) {
      alu_pre_res := alu2 << 1
    }
    is (aInclOr) {
      alu_pre_res := alu1 | alu2
    }
    is (aRotateLeftCarry) {
      alu_pre_res := Cat(alu2, status(0))
    }
    is (aRotateRightCarry) {
      alu_pre_res := Cat(alu2(0), status(0), alu2(7,1))
    }
    is (aSwapNibbles2) {
      alu_pre_res := Cat(alu2(3,0), alu2(7,4))
    }
    is (aXor) {
      alu_pre_res := alu1 ^ alu2
    }
    is (aBitTestSet) {
      alu_pre_res := (alu1(alu2(3,0)) === 1.U).asUInt
    }
    is (aBitTestClear) {
      alu_pre_res := (alu1(alu2(3,0)) === 0.U).asUInt
    }
  }
}

object ToplevelDriver extends App {
  chisel3.Driver.execute(args, () => {
    new Toplevel
  })
}
