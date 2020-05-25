package com.evolveum.axiom.lang.api;

import java.util.Optional;

class SimpleItemValue<T> implements AxiomItemValue<T> {

    private final AxiomTypeDefinition type;
    private final T value;



    SimpleItemValue(AxiomTypeDefinition type, T value) {
        super();
        this.type = type;
        this.value = value;
    }

    @Override
    public Optional<AxiomTypeDefinition> type() {
        return Optional.ofNullable(type);
    }

    @Override
    public T get() {
        return value;
    }

}