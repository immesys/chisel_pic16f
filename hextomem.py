#!/usr/bin/python

import sys
hexf = open(sys.argv[1]).readlines()
of = open("mem.scala", "w")

dat = {}
for l in hexf:
    ln = int(l[1:3],16)
    addr = int(l[3:7],16)
    type = int(l[7:9],16)
    if type != 0:
        continue
    content = l[9:9+2*ln]
    for i in range(0, ln/2):
        wi = addr/2 + i
        #print ("addr=%x i=%d wi=%d\n" %( addr, i, wi))
        dat[wi] = content[i*4 : (i+1)*4]

listf = open(sys.argv[2]).readlines()
dat2 = {}
# skip the other stuff
while not listf[0].startswith("%LINETAB"):
    listf = listf[1:]
listf = listf[2:]

lines = []

for l in listf:
    if l[0] == "#":
        break
    parts = l.split(">")
    fparts = parts[0].split(" ")
    codeaddr = int(fparts[0], 16)
    linenum = int(parts[1].split(":")[0])
    file = parts[1].split(":")[1].strip()
    lines.append((codeaddr, file, linenum))



files = {}
for l in lines:
    if l[1] not in files:
        lz = open(l[1]).readlines()
        files[l[1]] = lz


rlines = {}
for l in lines:
    linenum = l[2] - 1
    ln = files[l[1]][linenum].strip()
    if l[0] in rlines:
        rlines[l[0]].append(ln)
    else:
        rlines[l[0]] = [ln]
    #rlines.append((l[0], ln))

addrs = sorted(dat.keys())

print "package toplevel"
print "import chisel3._"
print "class CompiledMem {"
print "     val CMem = Map("
for i in addrs:
    if i in rlines:
        comments = rlines[i]
    else:
        comments = [" "]
    for k in range (0, len(comments)-1):
        print "        // %s" % comments[k]
    cma = ","
    if i == addrs[-1]:
        cma = " "
    print """        %d -> "h%s".U.litValue%s // %s""" % (i, dat[i][2:4]+dat[i][0:2], cma, comments[-1])
print "     )"
print "}"
