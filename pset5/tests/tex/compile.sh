#/bin/bash
touch round${1}.tex
for i in 0 1 2 3 4
do
   cat test_b${i}_results.tex >> round${1}.tex
done

