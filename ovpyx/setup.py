__author__ = 'slihn'

import os
from distutils.core import setup
from Cython.Build import cythonize
from distutils.extension import Extension

setup(
    ext_modules = cythonize([
        Extension("cov", ["cov.pyx"]),
        Extension("cov_packed", ["cov_packed.pyx"], libraries=["calc"], library_dirs=[os.getcwd()])
    ])
)
