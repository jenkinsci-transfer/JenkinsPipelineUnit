package com.lesfurets.jenkins.unit.declarative


class ParametersDeclaration {
    Binding binding

    void setParams(String key, Object defaultValue) {
        if (!binding.hasVariable("params")) {
            binding.setVariable("params", [:])
        }
        if (!binding.params.containsKey(key)) {
            binding.params[key] = defaultValue
        }
    }

    // dereference 'parameters closure
    def booleanParam(Map val) {
        this.setParams(val.name, val.defaultValue)
    }

    def choice(Map val) {
        this.setParams(val.name, val.choices[0])
    }

    def password(Map val) {
        this.setParams(val.name, val.defaultValue)
    }

    def string(Map val) {
        this.setParams(val.name, val.defaultValue)
    }

    def text(Map val) {
        this.setParams(val.name, val.defaultValue)
    }
}
