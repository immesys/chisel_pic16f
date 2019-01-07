package toplevel

import chisel3._
import chisel3.util._
import gcd.GCD
import chisel3.util.experimental.loadMemoryFromFile

class Toplevel (testing: Boolean) extends Module {
  val io = IO(new Bundle {
    val ebus_in = Input(UInt(16.W))
    val ebus_out = Output(UInt(16.W))
    val ebus_en = Output(UInt(16.W))

    val ebus_alatch = Output(Bool())
    val ebus_read = Output(Bool())
    val ebus_write = Output(Bool())

    //val flash_read_addr = Output(UInt(15.W))
    //val flash_read_val = Input(UInt(14.W))
  })

  var ebus_en_b = Wire(Bool())

  when (ebus_en_b) {
    io.ebus_en := "hffff".U
  } .otherwise {
    io.ebus_en := 0.U
  }

  ebus_en_b := false.B
  io.ebus_out := 0.U
  io.ebus_alatch := false.B
  io.ebus_read := false.B
  io.ebus_write := false.B

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

  val changedFSR = Reg(UInt(1.W))
  val fsr0Shadow = Reg(UInt(16.W))
  val fsr0 = Reg(new Bundle{
    val fsr0h = UInt(8.W)
    val fsr0l = UInt(8.W)
  })
  val fsr1Shadow = Reg(UInt(16.W))
  val fsr1 = Reg(new Bundle{
    val fsr1h = UInt(8.W)
    val fsr1l = UInt(8.W)
  })
  val bsr = RegInit(0.U(8.W))
  val wreg = RegInit(0.U(8.W))
  val pclath = RegInit(0.U(7.W))
  val intcon = RegInit(0.U(8.W))

  val bus_value = Wire(UInt(16.W))
  //For saving the address across cycles
  //val addr = Reg(UInt(16.W))

  //Memory mapping
  val raw_addr = Wire(UInt(16.W))
  val mapped_addr = Wire(UInt(16.W))
  val mapper = Module(new MemoryMapper)
  mapped_addr := mapper.io.mapped_addr
  val mapped_addr_reg = RegNext(mapped_addr)
  mapper.io.raw_addr := raw_addr
  mapper.io.fsr0 := fsr0Shadow
  mapper.io.fsr1 := fsr1Shadow
  raw_addr := 7.U

  //Bus memory mux
  val mem = SyncReadMem(4096, UInt(8.W))
  var smem = Mem(4096, UInt(8.W))
  val stack = Module(new Stack)
  stack.io.push := false.B
  stack.io.pop := false.B
  stack.io.push_addr := 0.U
  val sram_read_value = mem.read(mapped_addr(11,0))
  val bus_zero :: bus_sram :: bus_alu :: bus_w :: Nil = Enum(4)
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

  when (cycle === 0.U)
  {
    cycle := 1.U
    val nextPC = pc.asUInt + 1.U
    val curPC = pc.asUInt
    pc.pch := nextPC(14,8)
    pc.pcl := nextPC(7,0)
    io.ebus_out := Cat(1.U, pc.asUInt)
    ebus_en_b := true.B
    io.ebus_alatch := true.B

    //Perform FSR shadow mapping
    when (changedFSR === 0.U) {
      raw_addr := fsr0.asUInt
      fsr0Shadow := mapped_addr
    } .otherwise {
      raw_addr := fsr1.asUInt
      fsr1Shadow := mapped_addr
    }
    printf("cycle is 0, pc=%x (h%x ----------------- d%d)\n", curPC, curPC(14,0), curPC(14,0))
  } .elsewhen (cycle === 1.U) {
    //flash value -> instruction register
    io.ebus_read := true.B
    // register for subsequent cycles
    instruction := io.ebus_in
    // combinatorial for this cycle
    opdecode.io.instruction := io.ebus_in
    // send from idecode to addr mapping
    // raw_addr := Cat(bsr(5,0), signals.Address)
    printf("cy is 1, flashval = %b sigAddr is %x\n", io.ebus_in, signals.Address)
    printf("cy is 1, bsr=%x mapped_addr is %x\n", bsr, mapped_addr)

    when (!signals.SpecialINDF) {
      raw_addr := Cat(bsr(5,0), signals.Address)
    } .otherwise {
      val indf_addr = Wire(SInt(17.W))
      when (signals.FSRNum === 0.U) {
        indf_addr := (fsr0.asUInt).asSInt + signals.FSRPreAdd
        printf("XXX fsr addr=%x result=%x\n", fsr0.asUInt, indf_addr)
      } .otherwise {
        indf_addr := (fsr1.asUInt).asSInt + signals.FSRPreAdd
      }
      raw_addr := indf_addr.asUInt
      printf("raw_addr c3 is %x\n", raw_addr)
    }

    when (mapped_addr > "h6000".U) {
      //This is a flash/external address
      cycle := 2.U
    } .otherwise {
      cycle := 3.U
    }
  } .elsewhen (cycle === 2.U) {
    //reserved for flash/external indf
    cycle := 3.U
    ebus_en_b := true.B
    io.ebus_out := mapped_addr_reg
    io.ebus_alatch := true.B

    mapped_addr_reg := mapped_addr_reg
  } .elsewhen (cycle === 3.U) {
    cycle := 4.U
    io.ebus_read := true.B
    printf("cycle is 3, bus_value is %x alu2 is %x alu_res is %x\n", bus_value, alu2, alu_res)
    printf("alu op is %d\n", signals.Operation)

    bus_in_sel := bus_sram
    alu_res_reg := alu_res
    status := alu_status_res.asUInt

    //For testing, write ebus from smem here
    if (testing) {
      io.ebus_out := smem.read(mapped_addr_reg)
    }
  } .elsewhen (cycle === 4.U)
  {
    cycle := 0.U
  //  raw_addr := Cat(bsr(5,0), signals.Address)
    when (signals.WriteMem) {
      when (signals.SpecialINDF) {
        when (signals.SpecialINDF_ToW) {
          printf("sending alu res %x to w\n", alu_res_reg)
          wreg := alu_res_reg
        } .otherwise {
          bus_in_sel := bus_w
          bus_out_sel := bus_sram
          bus_write := true.B
        }
      } .elsewhen (signals.DestF) {
        bus_in_sel := bus_alu
        bus_out_sel := bus_sram
        bus_write := true.B
      } .otherwise {
        bus_out_sel := bus_zero
        bus_write := false.B
        wreg := alu_res_reg
      }
    }

    when (signals.SpecialINDF) {
      printf("performing special INDF writeback\n")
      when (signals.FSRNum === 0.U) {
        val curFSR = Wire(UInt(17.W))
        curFSR := fsr0.asUInt
        val nextFSR = curFSR.asSInt + signals.FSRPostAdd
        fsr0.fsr0l := nextFSR(7,0)
        fsr0.fsr0h := nextFSR(15,8)
        printf("new FSR0 value is %x\n", nextFSR)
      } .otherwise {
        val curFSR = Wire(UInt(17.W))
        curFSR := fsr1.asUInt
        val nextFSR = curFSR.asSInt + signals.FSRPostAdd
        fsr1.fsr1l := nextFSR(7,0)
        fsr1.fsr1h := nextFSR(15,8)
        printf("new FSR1 value is %x\n", nextFSR)
      }
    }

    when(signals.Push) {
      stack.io.push_addr := pc.asUInt
      stack.io.push := true.B
    }

    when(signals.Pop) {
      printf("asking stack to pop. Setting PC to %x\n", stack.io.tos)
      pc.pcl := stack.io.tos(7,0)
      pc.pch := stack.io.tos(14,8)
      stack.io.pop := true.B
    } .elsewhen (signals.SetPCAbs) {
      pc.pcl := signals.PCAbsAddr(7,0)
      pc.pch := Cat(pclath(6, 3), signals.PCAbsAddr(10,8))
    } .elsewhen (signals.AddPC ||
                (signals.AddPCZero && alu_res_reg === 0.U) ||
                (signals.AddPCNonzero && alu_res_reg =/= 0.U)) {
      val pcu = Wire(UInt(16.W))
      pcu := pc.asUInt
      val nextPC = (pcu.asSInt + signals.PCAddAddr).asUInt
      pc.pcl := nextPC(7,0)
      pc.pch := nextPC(14,8)
    } .elsewhen (signals.AddPCW) {
      val pcu = Wire(UInt(16.W))
      pcu := pc.asUInt
      val nextPC = (pcu.asSInt + wreg.asSInt).asUInt
      pc.pcl := nextPC(7,0)
      pc.pch := nextPC(14,8)
    }

    printf("cy4 s=%x sz=%x snz=%x res=%x\n", signals.AddPC, signals.AddPCZero, signals.AddPCNonzero, alu_res_reg)
    printf("cy4 rawaddr=%x mapped=%x\n", raw_addr, mapped_addr)
    printf("cy4, wreg=%x (%b) df=%b status=%b\n", wreg, wreg, signals.DestF, status)
  }

  when (bus_in_sel === bus_sram)
  {
    when(mapped_addr_reg < "h1000".U)
    {
      bus_value := sram_read_value
    } .elsewhen(mapped_addr_reg === "h1C00".U)
    {
      //You can't set FSR to the indf address
      bus_value := 0.U
    } .elsewhen(mapped_addr_reg === "h1C01".U)
    {
      //You can't set FSR to the indf address
      bus_value := 0.U
    } .elsewhen(mapped_addr_reg === "h1C02".U)
    {
      bus_value := pc.pcl
    } .elsewhen(mapped_addr_reg === "h1C03".U)
    {
      bus_value := status
    } .elsewhen(mapped_addr_reg === "h1C04".U)
    {
      bus_value := fsr0.fsr0l
    } .elsewhen(mapped_addr_reg === "h1C05".U)
    {
      bus_value := fsr0.fsr0h
    } .elsewhen(mapped_addr_reg === "h1C06".U)
    {
      bus_value := fsr1.fsr1l
    } .elsewhen(mapped_addr_reg === "h1C07".U)
    {
      bus_value := fsr1.fsr1h
    } .elsewhen(mapped_addr_reg === "h1C08".U)
    {
      bus_value := bsr
    } .elsewhen(mapped_addr_reg === "h1C09".U)
    {
      bus_value := wreg
    } .elsewhen(mapped_addr_reg === "h1C0A".U)
    {
      bus_value := pclath
    } .elsewhen(mapped_addr_reg === "h1C0B".U)
    {
      bus_value := intcon
    } .elsewhen(mapped_addr_reg >= "h6000".U)
    {
      //Flash read / external read
      bus_value := io.ebus_in(7,0)
    } .otherwise {
      bus_value := 0.U
    }
  } .elsewhen (bus_in_sel === bus_alu) {
    bus_value := alu_res_reg
  } .elsewhen (bus_in_sel === bus_w) {
    bus_value := wreg
  } .otherwise {
    bus_value := 0.U
  }

  when(bus_out_sel === bus_sram && bus_write)
  {
    when(mapped_addr_reg < "h1000".U)
    {
      printf("writing to memory addr=%x val=%x\n", mapped_addr_reg(11,0), bus_value)
      mem.write(mapped_addr_reg(11,0), bus_value)
      if (testing) {
        smem.write(mapped_addr_reg(11,0), bus_value)
      }
    } .elsewhen(mapped_addr_reg === "h1C00".U)
    {
      //Can't write to indf through fsr
    } .elsewhen(mapped_addr_reg === "h1C01".U)
    {
      //Can't write to indf through fsr
    } .elsewhen(mapped_addr_reg === "h1C02".U)
    {
      printf("writing to pcl=%x and pclh=%x\n", bus_value, pclath)
      pc.pcl := bus_value
      pc.pch := pclath
    } .elsewhen(mapped_addr_reg === "h1C03".U)
    {
      status := bus_value
    } .elsewhen(mapped_addr_reg === "h1C04".U)
    {
      fsr0.fsr0l := bus_value
      changedFSR := 0.U
    } .elsewhen(mapped_addr_reg === "h1C05".U)
    {
      fsr0.fsr0h := bus_value
      changedFSR := 0.U
    } .elsewhen(mapped_addr_reg === "h1C06".U)
    {
      fsr1.fsr1l := bus_value
      changedFSR := 1.U
    } .elsewhen(mapped_addr_reg === "h1C07".U)
    {
      fsr1.fsr1h := bus_value
      changedFSR := 1.U
    } .elsewhen(mapped_addr_reg === "h1C08".U)
    {
      bsr := bus_value
    } .elsewhen(mapped_addr_reg === "h1C09".U)
    {
      wreg := bus_value
    } .elsewhen(mapped_addr_reg === "h1C0A".U)
    {
      pclath := bus_value
    } .elsewhen(mapped_addr_reg === "h1C0B".U)
    {
      intcon := bus_value
    }  .elsewhen(mapped_addr_reg >= "h2000".U)
    {
      // External write
      io.ebus_out := bus_value
      ebus_en_b := true.B
      io.ebus_write := true.B
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

  val carryBorrow = Wire(UInt(8.W))

  when (signals.Complement2 ^ signals.Complement1) {
    when (status(0) === 0.U) {
      carryBorrow := "hFF".U
    } .otherwise {
      carryBorrow := 0.U
    }
  } .otherwise {
    when (status(0) === 0.U) {
      carryBorrow := 0.U
    } .otherwise {
      carryBorrow := 1.U
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
    printf("alu_res is %x\n", alu_res)
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
      alu_pre_res := (alu1 +& alu2) + carryBorrow
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
    new Toplevel(false)
  })
}
