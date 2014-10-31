#!/bin/bash

# rm cov.so cov_packed.so

gcc -Wall -shared -o libcalc.so -fPIC calc.c

python setup.py build_ext --inplace && \
python overlap.py $*

