package br.goldsuite.auth;

public final class AuthTest {
    public static void main(String[] args) {
        char[] password = "Senha de teste com ação".toCharArray();
        String first = PasswordHasher.hash(password);
        String second = PasswordHasher.hash(password);
        if (first.equals(second) || !PasswordHasher.verify(password, first) || PasswordHasher.verify("Senha incorreta".toCharArray(), first)) {
            throw new AssertionError("Sal individual ou verificacao de senha falhou.");
        }
        if (PasswordHasher.verify(password, "hash-corrompido")
                || PasswordHasher.verify(password, first.replace("$600000$", "$2147483647$"))) {
            throw new AssertionError("Hash malformado ou custo excessivo aceito.");
        }
        AttemptWindow window = new AttemptWindow(0);
        for (int i = 0; i < 20; i++) { if (!window.acquire(0)) { throw new AssertionError("Tentativa valida recusada."); } }
        if (window.acquire(59_999_999_999L) || !window.acquire(60_000_000_000L)) {
            throw new AssertionError("Limite por IP nao respeitou a janela.");
        }
        System.out.println("GoldAuth: senha correta/incorreta, sal, hash malformado e limite por IP verificados.");
    }
}
