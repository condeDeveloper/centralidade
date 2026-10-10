package br.com.conde.centralidade;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao
 * quebra quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("definicao") || comando.equals("tudo")) definicao();
        if (comando.equals("enumeracao") || comando.equals("tudo")) enumeracao();
        if (comando.equals("vazamento") || comando.equals("tudo")) vazamento();
        if (comando.equals("convergencia") || comando.equals("tudo")) convergencia();
        if (comando.equals("haltere") || comando.equals("tudo")) haltere();
        if (comando.equals("custo") || comando.equals("tudo")) custo();
        if (comando.equals("centro") || comando.equals("tudo")) centro();
    }

    private static void definicao() {
        System.out.println();
        System.out.println("BRANDES CONTRA A DEFINICAO, e a iteracao contra o SISTEMA LINEAR.");
        System.out.println();
        System.out.printf("  %-28s %10s %9s %26s %24s %14s %20s%n",
                "grafo", "vertices", "arestas", "desvio da intermediacao", "desvio do PageRank",
                "mesmo centro", "soma do PageRank");

        for (var l : Medidas.contraAForcaBruta())
            System.out.printf(Locale.ROOT, "  %-28s %10d %9d %26.2e %24.2e %14s %20.10f%n",
                    l.grafo(), l.vertices(), l.arestas(), l.desvioDaIntermediacao(),
                    l.desvioDoPageRank(), l.mesmoCentro() ? "sim" : "NAO", l.somaDoPageRank());

        System.out.println();
        System.out.println("AS FORMAS COM RESPOSTA CONHECIDA, que dao para escrever sem rodar nada.");
        System.out.println();
        System.out.printf("  %-20s %-34s %12s %12s %8s%n", "grafo", "medida", "esperado", "obtido", "bate");

        for (var l : Medidas.asFormasConhecidas())
            System.out.printf("  %-20s %-34s %12s %12s %8s%n",
                    l.grafo(), l.medida(), l.esperado(), l.obtido(), l.bate() ? "sim" : "NAO");

        System.out.println();
    }

    private static void enumeracao() {
        System.out.println();
        System.out.println("AS TRES CONTAS contra a ENUMERACAO LITERAL de todos os caminhos.");
        System.out.println();
        System.out.printf("  %-28s %10s %9s %34s %22s %22s%n",
                "grafo", "vertices", "arestas", "pares com mais de um caminho",
                "desvio de Brandes", "desvio da definicao");

        for (var l : Medidas.asTresContas())
            System.out.printf(Locale.ROOT, "  %-28s %10d %9d %34d %22.2e %22.2e%n",
                    l.grafo(), l.vertices(), l.arestas(), l.paresComMaisDeUmCaminho(),
                    l.desvioDeBrandes(), l.desvioDaDefinicao());

        System.out.println();
    }

    private static void vazamento() {
        System.out.println();
        System.out.println("O NO SEM SAIDA QUE VAZA: a soma das notas deixa de ser um.");
        System.out.println();
        System.out.printf("  %-26s %10s %10s %22s %22s %12s %22s%n",
                "grafo", "vertices", "sem saida", "soma com tratamento", "soma sem tratamento",
                "vazou", "desvio do sistema");

        for (var l : Medidas.oVazamento())
            System.out.printf(Locale.ROOT, "  %-26s %10d %10d %22.10f %22.10f %12.4f %22.2e%n",
                    l.grafo(), l.vertices(), l.semSaida(), l.somaComTratamento(),
                    l.somaSemTratamento(), l.vazou(), l.desvioDoSistema());

        System.out.println();
    }

    private static void convergencia() {
        System.out.println();
        System.out.println("QUANTAS RODADAS a iteracao precisa para alcancar a solucao exata.");
        System.out.println();
        System.out.printf("  %10s %24s %20s%n", "rodadas", "desvio do sistema", "soma");

        for (var l : Medidas.aConvergencia())
            System.out.printf(Locale.ROOT, "  %10d %24.3e %20.10f%n", l.rodadas(), l.desvioDoSistema(), l.soma());

        System.out.println();
    }

    private static void haltere() {
        System.out.println();
        System.out.println("O HALTERE: a ponte tem o MENOR grau e a MAIOR intermediacao.");
        System.out.println();
        System.out.printf("  %10s %10s %8s %16s %24s %18s %26s%n",
                "por lado", "vertices", "ponte", "grau da ponte", "intermediacao da ponte",
                "posto por grau", "posto por intermediacao");

        for (var l : Medidas.oHaltere())
            System.out.printf(Locale.ROOT, "  %10d %10d %8d %16.4f %24.4f %18d %26d%n",
                    l.porLado(), l.vertices(), l.ponte(), l.grauDaPonte(), l.intermediacaoDaPonte(),
                    l.postoPorGrau(), l.postoPorIntermediacao());

        System.out.println();
    }

    private static void custo() {
        System.out.println();
        System.out.println("BRANDES CONTRA A DEFINICAO, em visitas, num grafo esparso.");
        System.out.println();
        System.out.printf("  %10s %10s %18s %20s %24s%n",
                "vertices", "arestas", "por Brandes", "pela definicao", "quantas vezes menos");

        for (var l : Medidas.oCusto())
            System.out.printf(Locale.ROOT, "  %10d %10d %18d %20d %24.1f%n",
                    l.vertices(), l.arestas(), l.porBrandes(), l.pelaDefinicao(), l.quantasVezesMenos());

        System.out.println();
    }

    private static void centro() {
        System.out.println();
        System.out.println("QUEM E O CENTRO de cada forma, pelas quatro medidas.");
        System.out.println();
        System.out.printf("  %-28s %10s %10s %18s %20s %14s %20s%n",
                "grafo", "vertices", "por grau", "por proximidade", "por intermediacao",
                "por PageRank", "as quatro concordam");

        for (var l : Medidas.oCentro())
            System.out.printf("  %-28s %10d %10d %18d %20d %14d %20s%n",
                    l.grafo(), l.vertices(), l.porGrau(), l.porProximidade(),
                    l.porIntermediacao(), l.porPageRank(), l.asQuatroConcordam() ? "sim" : "nao");

        System.out.println();
    }
}
