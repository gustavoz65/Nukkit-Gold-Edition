package br.goldsuite.api;

import com.google.gson.Gson;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Estado limitado a 1 MiB, com alteracoes serializadas e publicacao apos persistir.
 * A funcao de alteracao so modifica a copia recebida, sem efeitos externos.
 * Todas as chamadas e conclusoes ocorrem na thread de criacao (principal).
 */
public final class PersistentState<T> {
    private final Gson gson = new Gson();
    private final Thread thread = Thread.currentThread();
    private final AsyncStore store;
    private final String namespace;
    private final Class<T> type;
    private final Consumer<T> validator;
    private CompletableFuture<Void> tail;
    private T state;
    private boolean closed;
    private int pending;

    public PersistentState(AsyncStore store, String namespace, Class<T> type, Supplier<T> defaults, Consumer<T> validator) {
        this.store = store; this.namespace = namespace; this.type = type; this.validator = validator;
        this.tail = store.get(namespace, "estado").thenAccept(value -> {
            checkThread();
            T loaded = value.isPresent() ? gson.fromJson(value.get(), type) : defaults.get();
            validator.accept(loaded);
            this.state = loaded;
        });
    }

    public CompletableFuture<Void> ready() { checkThread(); return tail; }
    public T snapshot() {
        checkThread();
        if (closed || state == null) { throw new IllegalStateException("Dados indisponiveis. Aguarde a inicializacao."); }
        return gson.fromJson(gson.toJson(state), type);
    }

    public CompletableFuture<Void> change(Consumer<T> mutation) {
        checkThread();
        if (closed || pending >= 128) { return failed(new IllegalStateException("Modulo encerrado ou fila de gravacao cheia.")); }
        pending++;
        CompletableFuture<Void> operation = tail.thenCompose(ignored -> {
            checkThread();
            T next = snapshot();
            mutation.accept(next);
            validator.accept(next);
            String encoded = gson.toJson(next);
            if (encoded.length() > 1024 * 1024) { throw new IllegalStateException("Limite de dados do modulo atingido."); }
            return store.put(namespace, "estado", encoded).thenRun(() -> { checkThread(); this.state = next; });
        });
        CompletableFuture<Void> completed = operation.whenComplete((unused, error) -> pending--);
        // Uma recusa de negocio nao bloqueia as proximas operacoes; falha de carga continua fechada.
        tail = completed.handle((unused, error) -> {
            if (state == null) { throw new IllegalStateException("Nao foi possivel carregar os dados.", error); }
            return null;
        });
        return completed;
    }

    public void close() { checkThread(); closed = true; }
    private void checkThread() { if (Thread.currentThread() != thread) { throw new IllegalStateException("Use a API na thread principal."); } }
    public static <V> CompletableFuture<V> failed(Throwable error) {
        CompletableFuture<V> future = new CompletableFuture<>(); future.completeExceptionally(error); return future;
    }
}
