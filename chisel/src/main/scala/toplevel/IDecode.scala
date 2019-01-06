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

  val wreg_addr = 9.U

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
  .elsewhen (io.instruction(13,8) === "b00_0000".U) { //MOVWF / NOP
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
    //DECFSZ
    //INCFSZ
    //BCF
    //BSF
    //BTFSC
    //BTFSS

    //ADDLW
    //ANDLW
    //IORLW
    //MOVLB
    //MOVLP
  .elsewhen (io.instruction(13,8) === "b11_0000".U) { //MOVLW
      //printf("decoded MOVLW\n")
      io.signals.Operation := aIdentity2
      io.signals.Source2 := srcLiteral
      io.signals.Literal2 := io.instruction(7,0)
      io.signals.DestF := true.B
      io.signals.Address := wreg_addr
  }
    //SUBLW
    //XORLW

    //BRA
    //BRW
    //CALL
    //CALLW
    //GOTO
    //RETFIE
    //RETLW
    //RETURN

    //CLRWDT

    //RESET
    //SLEEP
    //TRIS

    //ADDFSR
    //MOVIW
    //MOVWI
  .otherwise { //NOP
    //Nop
    io.signals.WriteMem := false.B
  }

}
