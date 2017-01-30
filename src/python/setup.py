
# -*- coding: utf-8 -*-


"""setup.py: setuptools control."""


import re
from setuptools import setup


version = re.search(
    '^__version__\s*=\s*"(.*)"',
    open('fims/fims.py').read(),
    re.M
).group(1)


with open("README.rst", "rb") as f:
    long_descr = f.read().decode("utf-8")


setup(
    name = "fims-cmdline",
    packages = ["fims"],
    entry_points = {
        "console_scripts": ['fims = fims.fims:main']
    },
    version = version,
    description = "Python command line application for interacting with FIMS.",
    long_description = long_descr,
    author = "RJ Ewing",
    author_email = "ewing.rj@gmail.com",
    url = "https://github.com/biocodellc/biocode-fims-commons/"
)