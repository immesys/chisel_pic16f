

chisel:
	cd chisel && sbt run

build: chisel
	cp chisel/Toplevel.v icestorm/Toplevel.v
	cd icestorm && make

prog: build
	cd icestorm && make prog

code:
	cd code && /opt/microchip/xc8/v2.00/bin/xc8 --chip=16f19197 prog.asm --runtime=no_startup,-init,-clib
	./hextomem.py code/prog.hex code/prog.cmf > chisel/src/main/scala/toplevel/CompiledMem.scala

testv:
	cd chisel && sbt 'test:runMain toplevel.ToplevelMain --backend-name verilator --generate-vcd-output on'
test:
	cd chisel && sbt 'test:runMain toplevel.ToplevelMain --generate-vcd-output on'

.PHONY: test
.PHONY: chisel
.PHONY: build
.PHONY: prog
.PHONY: code
