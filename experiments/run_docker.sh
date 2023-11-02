#!/bin/bash

docker run -d -it \
  --name apr4vul \
  -v /root/repair_results:/results \
  -e JAVA_ARGS="-Xmx4g -Xms1g -XX:MaxPermSize=512m" \
  -e THREADS="1" \
  -e TOOL_TIMEOUT="240" \
  vulrepair/apr4vul
