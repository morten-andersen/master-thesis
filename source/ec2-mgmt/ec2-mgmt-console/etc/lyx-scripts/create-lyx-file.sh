#!/bin/bash

if [ $# -ne "1" ]
then
  echo 'No arg supplied'
  exit 1
fi

cp bilag_template.lyx "bilag_$1.lyx"

sed -i "s/\[name\]/$1/g" bilag_$1.lyx
sed -i "s/\[network-hops-before\]/$1-network-hops-before.pdf/g" bilag_$1.lyx
sed -i "s/\[network-rtt-before\]/$1-network-rtt-before.pdf/g" bilag_$1.lyx
sed -i "s/\[network-hops-after\]/$1-network-hops-after.pdf/g" bilag_$1.lyx
sed -i "s/\[network-rtt-after\]/$1-network-rtt-after.pdf/g" bilag_$1.lyx
sed -i 's/\\end_body//g' bilag_$1.lyx
sed -i 's/\\end_document//g' bilag_$1.lyx

echo -e'\\begin_layout Standard\n' >> bilag_$1.lyx

i=0
for f in $1-client-timings-*.pdf;
do
  if [ $(($i % 4)) == 0 ]
  then
    echo -e '\\begin_inset\nNewpage newpage\n\\end_inset\n' >> bilag_$1.lyx
    echo -e '\\begin_inset ERT status open\n' >> bilag_$1.lyx
    echo -e '\\begin_layout Plain Layout\n' >> bilag_$1.lyx
#    echo -e '\\backslash begin{figure}[htp]\n\\backslash addtocounter{figure}{-1}\n' >> bilag_$1.lyx
    echo -e '\\backslash begin{figure}[htp]\n' >> bilag_$1.lyx
    echo -e '\\end_layout\n' >> bilag_$1.lyx
  fi

  if [ $(($i % 2)) == 0 ]
  then
    echo -e '\\begin_layout Plain Layout\n' >> bilag_$1.lyx
    echo -e '\\backslash noindent\n\\backslash makebox\n[\\backslash textwidth]{%\n' >> bilag_$1.lyx
    echo -e '\\end_layout\n' >> bilag_$1.lyx
  fi

  echo -e '\\begin_layout Plain Layout\n' >> bilag_$1.lyx
  echo -e '\\backslash subfigure[]\n{\\backslash includegraphics[width=0.75\\backslash\ntextwidth]\n' >> bilag_$1.lyx
  echo -e '\\end_layout\n' >> bilag_$1.lyx
  echo -e '\\begin_layout Plain Layout\n' >> bilag_$1.lyx
  echo "{$f}}" >> bilag_$1.lyx
  echo -e '\\end_layout\n' >> bilag_$1.lyx

  i=$(($i + 1))

  if [ $(($i % 2)) == 0 ]
  then
    echo -e '\\begin_layout Plain Layout\n' >> bilag_$1.lyx
    echo '}' >> bilag_$1.lyx
    echo -e '\\end_layout\n' >> bilag_$1.lyx
  fi

  if [ $(($i % 4)) == 0 ]
  then
    echo -e '\\begin_layout Plain Layout\n' >> bilag_$1.lyx
    echo -e '\\backslash end{figure}\n' >> bilag_$1.lyx
    echo -e '\\end_layout\n' >> bilag_$1.lyx
    echo -e '\\end_inset\n' >> bilag_$1.lyx
  fi
done

echo -e '\\end_layout\n\\end_body\n\\end_document\n' >> bilag_$1.lyx
