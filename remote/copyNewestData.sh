
currentNode="JohnDoe@Nexus"

if [ "$#" -gt 0 ]; then
    currentNode=$1
fi

pattern="????????"

if [ "$#" -gt 1 ]; then
    pattern=$2
fi

fileTOcopy=$(ssh -XY ${currentNode} 'ls -t '${pattern} | head -1)

scp -p ${currentNode}:${fileTOcopy} tempData.dat

ssh -XY ${currentNode} 'date -r '${fileTOcopy}
