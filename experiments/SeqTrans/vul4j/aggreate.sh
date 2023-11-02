#!/bin/bash
find . -name "abstract.txt" -print0 | xargs -0 ls | sort -V | xargs cat > src-test.txt
