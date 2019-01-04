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
B equ 21h

reset_vec:
  ;expect w=1
  incf wreg, w
  movwf A
  incf wreg, w
  incf wreg, w
  ;expect w = 4
  incf wreg, w
  movwf B
  ;expect w = 8
  addwf B, w
