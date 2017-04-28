#/bin/bash
FILE=round${1}.tex
touch $FILE
for i in 0 1 2 3 4
do
   cat "\begin{center}\n" >> $FILE
   cat test_b${i}_results.tex >> $FILE
   cat "\textbf{Figure:} Caption" >> $FILE
   cat "\end{center}\n\n" >> $FILE
done

