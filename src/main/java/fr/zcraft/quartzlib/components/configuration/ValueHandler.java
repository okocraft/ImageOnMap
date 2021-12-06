/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.zcraft.quartzlib.components.configuration;

import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

class ValueHandler<T> {
    private final Class<T> outputType;
    private final HashMap<Class<?>, Method> methods = new HashMap<>();

    public ValueHandler(Class<T> outputType) {
        this.outputType = outputType;
    }

    public void addHandler(Class<?> inputType, Method method) {
        if (methods.containsKey(inputType)) {
            throw new IllegalStateException(
                    "Value handler already registered for type " + outputType.getName() + " with input " + inputType);
        }

        methods.put(inputType, method);
    }

    public T handleValue(Object inputValue)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (inputValue == null) {
            return null;
        }

        Method handler = null;

        Class<?> inputType = Reflection.getClosestType(inputValue.getClass(), methods.keySet());
        if (inputType != null) {
            handler = methods.get(inputType);
        }
        if (handler == null) {
            if (!methods.containsKey(String.class)) {
                throw new UnsupportedOperationException(
                        "Unsupported input type '" + inputValue.getClass().getName() + "' for configuration type : "
                                + outputType.getName());
            } else {
                handler = methods.get(String.class);
                inputValue = inputValue.toString();
            }
        }
        return (T) handler.invoke(null, inputValue);
    }
}
