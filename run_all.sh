#!/bin/bash
[ $# -eq 1 ] || { echo "Usage: run_all.sh <your compiler's run.sh>"; exit 1; }
fail=0
tot=0
for f in $(dirname $0)/run/*.mj
do
  echo $f
  tot=$((tot+1))
  ($1 $f 2>&1 && ./a.out | /usr/bin/diff - ${f%.mj}.check) || fail=$((yay+1))
done
echo "$fail/$tot failed"
