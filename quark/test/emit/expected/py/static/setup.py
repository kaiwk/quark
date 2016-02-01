# Setup file for package static

from setuptools import setup

setup(name="static",
      version="0.0.1",
      install_requires=["datawire-quark-core==0.4.2", "builtin==0.0.1"],
      py_modules=['static'],
      packages=['static_md'])