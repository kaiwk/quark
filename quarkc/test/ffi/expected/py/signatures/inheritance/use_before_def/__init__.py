from quark_runtime import *

import quark.reflect
import quark_ffi_signatures_md


class Bar(object):
    def _init(self):
        pass
    def __init__(self): self._init()

    def go(self):
        pass

    def _getClass(self):
        return u"inheritance.use_before_def.Bar"

    def _getField(self, name):
        return None

    def _setField(self, name, value):
        pass
Bar.inheritance_use_before_def_Bar_ref = quark_ffi_signatures_md.Root.inheritance_use_before_def_Bar_md
class Foo(object):
    def _init(self):
        self.name = None

    def __init__(self): self._init()

    def _getClass(self):
        return u"inheritance.use_before_def.Foo"

    def _getField(self, name):
        if ((name) == (u"name")):
            return (self).name

        return None

    def _setField(self, name, value):
        if ((name) == (u"name")):
            (self).name = _cast(value, lambda: unicode)


Foo.inheritance_use_before_def_Foo_ref = quark_ffi_signatures_md.Root.inheritance_use_before_def_Foo_md
