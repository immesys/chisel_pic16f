package toplevel

import chisel3._
import chisel3.util._
import gcd.GCD



//These are inputs to the ALU
class ControlSignals extends Bundle {
  val Operation = UInt(4.W)
  val Address = UInt(7.W)
  val Complement1 = Bool()
  val Complement2 = Bool()
  val Literal2 = UInt(8.W)
  val Source1 = UInt(2.W)
  val Source2 = UInt(2.W)
  val JumpResult = Bool()
  val SetZero = Bool()
  val SetCarry = Bool()
  val DestF = Bool()
  val WriteMem = Bool()
  val Push = Bool()
  val Pop = Bool()
  val SetPCAbs = Bool()
  val AddPCW = Bool()
  val AddPCNonzero = Bool()
  val AddPCZero = Bool()
  val PCAbsAddr = UInt(11.W)
  val AddPC = Bool()
  val PCAddAddr = SInt(10.W)
}

class Status extends Bundle {
  val Res = UInt(5.W)
  val Zero = Bool()
  val DigitCarry = Bool()
  val Carry = Bool()
}

class IDecode extends Module {
  val io = IO(new Bundle {
    val instruction   = Input(UInt(14.W))
    val signals = Output(new ControlSignals)
  })
  val ( aAdd :: aAddWithCarry :: aAnd :: aRightShift :: aArithRightShift ::
  aLeftShift :: aIdentity1 :: aIdentity2 ::
  aInclOr ::
  aRotateLeftCarry :: aRotateRightCarry ::
  aSwapNibbles2 ::
  aXor ::
  aBitTestSet :: aBitTestClear :: Nil ) = Enum(15)
  val srcBus :: srcLiteral :: srcW :: Nil = Enum(3)

  io.signals.Operation := aIdentity2
  io.signals.Complement1 := false.B
  io.signals.Complement2 := false.B
  io.signals.Source1 := srcW
  io.signals.Source2 := srcBus
  io.signals.JumpResult := false.B
  io.signals.SetZero := false.B
  io.signals.SetCarry := false.B
  io.signals.DestF := io.instruction(7) === 1.U
  io.signals.Address := io.instruction(6, 0)
  io.signals.Literal2 := 0.U
  io.signals.WriteMem := true.B
  io.signals.SetPCAbs := false.B
  io.signals.AddPCW := false.B
  io.signals.AddPCNonzero := false.B
  io.signals.AddPCZero := false.B
  io.signals.PCAbsAddr := 0.U
  io.signals.PCAddAddr := 0.S
  io.signals.AddPC := false.B
  io.signals.Pop := false.B
  io.signals.Push := false.B
  val wreg_addr = 9.U
  val bsr_addr = 8.U
  val pclh_addr = 10.U

  //printf("instruction is %x\n", io.instruction)
  when (io.instruction(13,8) === "b0_00111".U) { //ADDWF
      //printf("decoded ADDWF\n")
      io.signals.Operation := aAdd
      io.signals.SetCarry := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b11_1101".U) { //ADDWFC
      print("decoded ADDWFC")
      io.signals.Operation := aAddWithCarry
      io.signals.SetCarry := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_0101".U) { //ANDWF
      print("decoded ANDWF\n")
      io.signals.Operation := aAnd
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b11_0111".U) { //ASRF
      print("decoded ASRF\n")
      io.signals.Operation := aArithRightShift
      io.signals.SetCarry := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b11_0101".U) { //LSLF
      //printf("decoded LSLF\n")
      io.signals.Operation := aLeftShift
      io.signals.SetCarry := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b11_0110".U) { //LSRF
      //printf("decoded LSRF\n")
      io.signals.Operation := aRightShift
      io.signals.SetCarry := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_0001".U) { //CLRF + CLRW
      //printf("decoded CLRF/W\n")
      io.signals.Operation := aIdentity2
      io.signals.Literal2 := 0.U
      io.signals.Source2 := srcLiteral
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_1001".U) { //COMF
      //printf("decoded COMF\n")
      io.signals.Operation := aIdentity2
      io.signals.Complement2 := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_0011".U) { //DECF
      //printf("decoded DECF\n")
      io.signals.Operation := aAdd
      io.signals.Complement2 := true.B
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := 1.U
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_1010".U) { //INCF
      //printf("decoded INCF\n")
      io.signals.Operation := aAdd
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := 1.U
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_0100".U) { //IORWF
      //printf("decoded IORWF\n")
      io.signals.Operation := aInclOr
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_1000".U) { //MOVF
      //printf("decoded MOVF\n")
      io.signals.Operation := aIdentity2
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,7) === "b00_0000_1".U) { //MOVWF
      //printf("decoded MOVWF\n")
      io.signals.Operation := aIdentity1
  }
  .elsewhen (io.instruction(13,8) === "b00_1101".U) { //RLF
      //printf("decoded RLF\n")
      io.signals.Operation := aRotateLeftCarry
      io.signals.SetCarry := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_1100".U) { //RRF
      //printf("decoded RRF\n")
      io.signals.Operation := aRotateRightCarry
      io.signals.SetCarry := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_0010".U) { //SUBWF
      //printf("decoded SUBWF\n")
      io.signals.Operation := aAdd
      io.signals.Complement1 := true.B
      io.signals.SetCarry := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b11_1011".U) { //SUBWFB
      //printf("decoded SUBWFB\n")
      io.signals.Operation := aAddWithCarry
      io.signals.Complement1 := true.B
      io.signals.SetCarry := true.B
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_1110".U) { //SWAPF
      //printf("decoded SWAPF\n")
      io.signals.Operation := aSwapNibbles2
  }
  .elsewhen (io.instruction(13,8) === "b00_0110".U) { //XORWF
      //printf("decoded XORWF\n")
      io.signals.Operation := aXor
      io.signals.SetZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_1011".U) { //DECFSZ
      //printf("decoded DECFSZ\n")
      io.signals.Operation := aAdd
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Complement2 := true.B
      io.signals.Literal2 := 1.U
      io.signals.PCAddAddr := 1.S
      io.signals.AddPCZero := true.B
  }
  .elsewhen (io.instruction(13,8) === "b00_1111".U) { //INCFSZ
      //printf("decoded INCFSZ\n")
      io.signals.Operation := aAdd
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := 1.U
      io.signals.PCAddAddr := 1.S
      io.signals.AddPCZero := true.B
  }
  .elsewhen (io.instruction(13,10) === "b01_00".U) { //BCF
      //printf("decoded BCF\n")
      var bitnum = io.instruction(9,7)
      var oh = UIntToOH(bitnum, 8)
      io.signals.Operation := aAnd
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := ~oh
      io.signals.DestF := true.B
  }
  .elsewhen (io.instruction(13,10) === "b01_01".U) { //BSF
      //printf("decoded BSF\n")
      var bitnum = io.instruction(9,7)
      var oh = UIntToOH(bitnum, 8)
      io.signals.Operation := aInclOr
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := oh
      io.signals.DestF := true.B
  }
  .elsewhen (io.instruction(13,10) === "b01_10".U) { //BTFSC
      //printf("decoded BTFSC\n")
      var bitnum = io.instruction(9,7)
      var oh = UIntToOH(bitnum, 8)
      io.signals.Operation := aAnd
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := oh
      io.signals.WriteMem := false.B
      io.signals.AddPCZero := true.B
      io.signals.PCAddAddr := 1.S
  }
  .elsewhen (io.instruction(13,10) === "b01_11".U) { //BTFSS
      //printf("decoded BTFSS\n")
      var bitnum = io.instruction(9,7)
      var oh = UIntToOH(bitnum, 8)
      io.signals.Operation := aAnd
      io.signals.Source1 := srcBus
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := oh
      io.signals.WriteMem := false.B
      io.signals.AddPCNonzero := true.B
      io.signals.PCAddAddr := 1.S
  }
  .elsewhen (io.instruction(13,8) === "b11_1110".U) { //ADDLW
      //printf("decoded ADDLW\n")
      io.signals.Operation := aAdd
      io.signals.SetZero := true.B
      io.signals.SetCarry := true.B
      io.signals.DestF := false.B
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
  }
  .elsewhen (io.instruction(13,8) === "b11_1001".U) { //ANDLW
      //printf("decoded ANDLW\n")
      io.signals.Operation := aAnd
      io.signals.SetZero := true.B
      io.signals.DestF := false.B
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
  }
  .elsewhen (io.instruction(13,8) === "b11_1000".U) { //IORLW
      //printf("decoded IORLW\n")
      io.signals.Operation := aInclOr
      io.signals.SetZero := true.B
      io.signals.DestF := false.B
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
  }
  .elsewhen (io.instruction(13,8) === "b00_0001".U) { //MOVLB
      //printf("decoded MOVLB\n")
      io.signals.Operation := aIdentity2
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
      io.signals.DestF := true.B
      io.signals.Address := bsr_addr
  }
  .elsewhen (io.instruction(13,8) === "b11_0001".U) { //MOVLP
      //printf("decoded MOVLP\n")
      io.signals.Operation := aIdentity2
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
      io.signals.DestF := true.B
      io.signals.Address := pclh_addr
  }
  .elsewhen (io.instruction(13,8) === "b11_0000".U) { //MOVLW
      //printf("decoded MOVLW\n")
      io.signals.Operation := aIdentity2
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
      io.signals.DestF := false.B
  }
  .elsewhen (io.instruction(13,8) === "b11_1100".U) { //SUBLW
      //printf("decoded SUBLW\n")
      io.signals.Operation := aAdd
      io.signals.Complement2 := true.B
      io.signals.SetZero := true.B
      io.signals.SetCarry := true.B
      io.signals.DestF := false.B
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
  }
  .elsewhen (io.instruction(13,8) === "b11_1010".U) { //XORLW
      //printf("decoded XORLW\n")
      io.signals.Operation := aXor
      io.signals.SetZero := true.B
      io.signals.DestF := false.B
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
  }
  .elsewhen (io.instruction(13,9) === "b11_001".U) { //BRA
      //printf("decoded BRA\n")
      val addr = io.instruction(8,0)
      io.signals.PCAddAddr := addr.asSInt
      io.signals.AddPC := true.B
      io.signals.WriteMem := false.B
  }
  .elsewhen (io.instruction === "b00_0000_0000_1011".U) { //BRW
      //printf("decoded BRW\n")
      io.signals.AddPCW := true.B
      io.signals.WriteMem := false.B
  }
  .elsewhen (io.instruction(13,11) === "b10_0".U) { //CALL
      //printf("decoded CALL\n")
      io.signals.PCAbsAddr := io.instruction(10,0)
      io.signals.SetPCAbs := true.B
      io.signals.Push := true.B
      io.signals.WriteMem := false.B
  }
    //CALLW
  .elsewhen (io.instruction(13,11) === "b10_1".U) { //GOTO
      //printf("decoded GOTO\n")
      io.signals.PCAbsAddr := io.instruction(10,0)
      io.signals.SetPCAbs := true.B
      io.signals.WriteMem := false.B
  }
  .elsewhen (io.instruction(13,8) === "b11_0100".U) { //RETLW
      //printf("decoded RETLW\n")
      io.signals.Pop := true.B
      io.signals.Operation := aIdentity2
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
      io.signals.DestF := false.B
  }
  .elsewhen (io.instruction === "b00_0000_0000_1000".U) { //RETURN
      //printf("decoded RETURN\n")
      io.signals.Pop := true.B
      io.signals.WriteMem := false.B
  }
    //-- TODO
    //ADDFSR
    //MOVIW
    //MOVWI
    //-- Probably never implement
    //CLRWDT
    //RETFIE
    //RESET
    //SLEEP
    //TRIS
  .otherwise { //NOP
    //Nop
    var inst = io.instruction
    var cv = io.instruction === "b00_0000_0000_1000".U
    io.signals.WriteMem := false.B
  }

}
