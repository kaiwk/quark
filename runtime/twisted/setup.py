# Copyright 2015 datawire. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
from setuptools import setup

metadata = {}
with open(os.path.join(os.path.dirname(__file__), "quark_twisted_runtime.py")) as fp:
    for line in fp:
        if line.startswith("__version__ = "):
            exec(line, metadata)
            break

setup(name="datawire-quark-twisted",
      version=metadata["__version__"],
      description="Twisted integration for Python code generated by Quark",
      author="datawire.io",
      author_email="hello@datawire.io",
      url="http://datawire.github.io/quark",
      license="Apache License, Version 2.0",
      install_requires=["autobahn[twisted]==0.10.9", "txaio==1.1", "service_identity==14.0.0"],
      py_modules=["quark_twisted_runtime"]
)
