module Quark
def self.extending_primitives_md; MODULE_extending_primitives_md; end
module MODULE_extending_primitives_md
require 'builtin' # .../reflect
require_relative 'pkg' # 0 () ()

def self.pkg_C_event1_Method; CLASS_pkg_C_event1_Method; end
class CLASS_pkg_C_event1_Method < ::Quark.builtin.reflect.Method



    def initialize()
        
        super("builtin.void", "event1", DatawireQuarkCore::List.new([]))

        nil
    end




    def invoke(object, args)
        
        obj = object
        obj.event1()
        return nil

        nil
    end

    def _getClass()
        
        return nil

        nil
    end

    def _getField(name)
        
        return nil

        nil
    end

    def _setField(name, value)
        
        nil

        nil
    end

    def __init_fields__()
        

        nil
    end


end

def self.pkg_C; CLASS_pkg_C; end
class CLASS_pkg_C < ::Quark.builtin.reflect.QuarkClass
    extend DatawireQuarkCore::Static

    static singleton: -> { ::Quark.extending_primitives_md.pkg_C.new() }



    def initialize()
        
        super("pkg.C")
        (self).name = "pkg.C"
        (self).parameters = DatawireQuarkCore::List.new([])
        (self).fields = DatawireQuarkCore::List.new([])
        (self).methods = DatawireQuarkCore::List.new([::Quark.extending_primitives_md.pkg_C_event1_Method.new()])

        nil
    end




    def construct(args)
        
        return ::Quark.pkg.C.new()

        nil
    end

    def _getClass()
        
        return nil

        nil
    end

    def _getField(name)
        
        return nil

        nil
    end

    def _setField(name, value)
        
        nil

        nil
    end

    def __init_fields__()
        

        nil
    end


end
CLASS_pkg_C.unlazy_statics

def self.Root; CLASS_Root; end
class CLASS_Root < Object
    extend DatawireQuarkCore::Static

    static pkg_C_md: -> { ::Quark.extending_primitives_md.pkg_C.singleton }



    def initialize()
        self.__init_fields__

        nil
    end




    def _getClass()
        
        return nil

        nil
    end

    def _getField(name)
        
        return nil

        nil
    end

    def _setField(name, value)
        
        nil

        nil
    end

    def __init_fields__()
        

        nil
    end


end
CLASS_Root.unlazy_statics
end # module MODULE_extending_primitives_md
end # module Quark
