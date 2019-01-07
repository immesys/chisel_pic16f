psect   init,class=CODE,delta=2
psect   end_init,class=CODE,delta=2
psect   powerup,class=CODE,delta=2
psect   cinit,class=CODE,delta=2
psect   config,class=CONFIG,delta=2,noexec
psect   idloc,class=IDLOC,delta=2,noexec
psect   eeprom_data,class=EEDATA,delta=2,space=3,noexec
psect   strings,class=CODE,delta=2,reloc=256
psect   intentry,class=CODE,delta=2
psect   reset_vec,class=CODE,delta=2
psect   functab,class=ENTRY,delta=2
psect 	reset_vec

; SFR Addresses
bsr equ 08h
fsr0h equ 05h
fsr0l equ 04h
fsr1h equ 07h
fsr1l equ 06h
indf equ 00h
indf0 equ 00h
indf1 equ 01h
intcon equ 0Bh
pc equ 02h
pcl equ 02h
pclath equ 0Ah
status equ 03h
wreg equ 09h

; variables
A equ 20h
ValueReg equ 21h
ExpectReg equ 22h
TestReg equ 23h
B equ 24h

; Used for unit tests. Checks that W is equal to
; the given value
expect MACRO vv
  movwf ValueReg
  movlw vv
  movwf ExpectReg
  incf TestReg, f
  clrf TestReg
  movf ValueReg, w
endm

reset_vec:
  ;test increment
  clrw
  incf wreg, w
  expect 1

  ;test addwf
  movwf A
  incf A, f
  addwf A, w
  expect 3

  ;test clrf
  clrf A
  movf A, w
  expect 0

  ;test carry flag set
  movlw 200
  movwf A
  addwf A, w
  movf status, w
  expect 1 ; carry set

  ;test carry flag not changed
  movlw 255
  incf wreg, w
  movf status, w
  expect 5 ;101 incf doesn't change carry

  ;clear status
  clrf status
  movf status, w
  expect 0

  ;test zero flag + carry
  movlw 255
  incf wreg, w
  movf status, w
  expect 4 ;100 incf doesn't set carry

  ;test zero flag no carry
  movlw 1
  decf wreg, w
  movf status, w
  expect 4 ;100

  ; test rrf
  movlw 85
  movwf A
  rrf A, w
  expect 42 ;
  rrf wreg, w
  expect 149 ; carry bit still set, rotate back in

  ; test rlf
  movlw 3
  movwf status ; set carry bit
  rlf A, w
  expect 171 ; rotate carry bit in

  ; test comf
  comf wreg, w
  expect 85

  ; test shift no rotate
  movlw 3
  movwf status
  lslf A, w
  expect 170

  ; test right shift no rotate
  clrf status
  lsrf A, w
  expect 42
  movwf status, w
  expect 0

  ; test SUBWF
  movlw 84
  subwf A, w
  expect 1
  movwf status, w
  expect 1 ; no borrow

  ; test SUBWF
  movlw 86
  subwf A, w
  expect 255
  movwf status, w
  expect 0 ; borrow

  ; test SUBWFB
  clrf status ; clear BORROW
  movlw 10
  subwfb A, w
  expect 74 ; 85-10-1 (borrow)

  ; test SUBWFB no borrow
  movlw 3
  movwf status
  movlw 10
  subwfb A, w
  expect 75 ; 85-10

  ; test ADDWFC
  clrf status ; clear carry
  movlw 10
  addwfc A, w
  expect 95

  ; check A is still 85
  movf A, w
  expect 85

  ; test ADDWFC with carry
  movlw 1
  movwf status
  movlw 10
  addwfc A, w
  expect 96

  ; test swapf
  movlw 0x35
  swapf wreg, w
  expect 0x53

  ; test swapf register
  movlw 0x45
  movwf B
  swapf B, f
  clrf wreg
  movf B, w
  expect 0x54

  ; test pcl write
  movlw 0x30
  movwf B
  movlw low(testpclwrite)
  movwf pcl
  ; this should be skipped
  movlw 0x35
  movwf B
testpclwrite:
  movf B, w
  expect 0x30

  ; test nop works
  movlw 0x41
  nop
  expect 0x41

  ; test xor
  movlw 0xff
  xorwf A, w
  expect 170
  movf status, w
  expect 0
  movf A, w
  xorwf A, w
  expect 0
  movf status, w
  expect 4

  ; test addlw
  movlw 5
  addlw 13
  expect 18
  movf status, w
  expect 0
  movlw 18
  addlw 242
  expect 4
  movf status, w
  expect 1

  ; test iorlw
  movlw 37
  iorlw 7
  expect 39

  ; test andlw
  movlw 37
  andlw 15
  expect 5

  ; test sublw
  movlw 10
  sublw 11
  expect 255
  movf status, w
  expect 0 ; borrow

  ; test sublw no borrow
  movlw 10
  sublw 9
  expect 1
  movf status, w
  expect 1 ; no borrow

  ; clear carry
  clrf status

  ; test xorlw
  movlw 55
  xorlw 55
  expect 0
  movf status, w
  expect 4

  ; check xor no clear carry
  movlw 1
  movwf status

  ; test xorlw
  movlw 55
  xorlw 37
  expect 18
  movf status, w
  expect 1

  ; test movlb
  ; assuming this label is on second page
  movlw 0x56
  movwf B
  movlp high testmovlb
  movlw low testmovlb
  movwf pcl
  movlw 0x55
  movwf B
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
  nop
testmovlb:
  movf B, w
  expect 0x56

  ; test decfsz
  movlw 3
  movwf B
  decfsz B, f
  movlw 7
  expect 7

  ; test decfsz
  movlw 1
  movwf B
  decfsz B, f
  movlw 7
  expect 1

  ; test decfsz
  movlw 1
  movwf B
  decfsz B, w
  movlw 7
  expect 0

  ; test btfss
  movlw 8
  movwf B
  btfss B, 3
  movlw 15
  expect 8

  ; test btfss doesn't alter args
  movlw 11
  movwf B
  btfss B, 7
  nop
  expect 11
  movf B, w
  expect 11

  ; test btfss on W
  movlw 8
  btfss wreg, 1
  movlw 15
  expect 15

  ; test btfss on W
  movlw 8
  btfss wreg, 3
  movlw 15
  expect 8

  ; test btfsc on B
  movlw 3
  movwf B
  btfsc B, 0
  movlw 9
  expect 9

  ; test btfsc on B
  movlw 3
  movwf B
  btfsc B, 3
  movlw 9
  expect 3

  ; test btfsc on B
  movlw 3
  movwf B
  clrf wreg ; check we aren't testing wreg
  btfsc B, 0
  movlw 9
  expect 9

  ; test bsf
  clrf B
  bsf B, 3
  movf B, w
  expect 8

  ; test bcf
  movlw 7
  bcf wreg, 0
  expect 6

  ; test bra
  movlw 5
  movwf B
  bra testbra1
  movlw 6
  movlw 7
  movwf B
testbra3:
  movlw 27
  bra testbra2
testbra1:
  expect 5
  bra testbra3
testbra2:
  expect 27
  movf B, w
  expect 5

; test pagesel
  movlw 85
  movwf B ; about to pagesel
  pagesel faraway
  goto faraway & 0x7FF

; a function to call that lies before the next piece
org 0x500
setw:
  movlw 73
  return
  expect 1 ; should not hit


; stuff far away to test pagesel
org 0x1000
  movlw 4
  movwf B
faraway:
  movf B, w
  expect 85

 ; test call ret
 movlw 5
 pagesel setw
 call setw ; this should fail
 expect 73

 ; test brw
 movlw 2
 brw
 movlw 4
 movlw 4
 expect 2
 nop ; end of tests

 ; test retlw
 clrf wreg
 pagesel retlw94
 call retlw94 & 0x7FF
 expect 94

 ; end tests
 pagesel endtests
 goto endtests & 0x7FF

 ; some utility functions
org 0x1300
retlw94:
  retlw 94




org 0x1700
endtests:
  goto $ & 0x7FF
