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

  
