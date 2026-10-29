package br.gold;

import cn.nukkit.plugin.PluginFailure;

public final class PluginFailureTest {
    public static void main(String[] args) {
        PluginFailure.rethrowIfFatal(new NoClassDefFoundError("DependenciaOpcional"));
        PluginFailure.rethrowIfFatal(new AssertionError("Falha do plugin"));
        // Recursao infinita de um plugin e contida como erro do plugin.
        PluginFailure.rethrowIfFatal(new StackOverflowError("Recursao simulada"));
        try {
            PluginFailure.rethrowIfFatal(new RuntimeException(new OutOfMemoryError("Falha simulada, sem esgotar memoria")));
            throw new AssertionError("Falha fatal da JVM foi ocultada.");
        } catch (OutOfMemoryError esperado) { }
        try {
            PluginFailure.rethrowIfFatal(new ThreadDeath());
            throw new AssertionError("Encerramento da thread foi ocultado.");
        } catch (ThreadDeath esperado) { }
        System.out.println("PluginFailureTest: erros de plugin isolados e falhas fatais preservadas.");
    }
}
