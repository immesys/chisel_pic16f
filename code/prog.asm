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

reset_vec:
  goto b
  movlw 0x02
  movlw 0x03
  movlw 0x04
  movlw 0x05
  movlw 0x06
  movlw 0x07
  movlw 0x08
  movlw 0x09
  movlw 0x10
  movlw 0x11
  movlw 0x12
  movlw 0x13
  movlw 0x14
  movlw 0x15
  movlw 0x16
b:
  movlw 0x17
  movlw 0x18
  movlw 0x19
