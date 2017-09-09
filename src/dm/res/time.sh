#usage: sh time.sh shellscriptName

{ time -p sh $1; } 2>> time.$1
