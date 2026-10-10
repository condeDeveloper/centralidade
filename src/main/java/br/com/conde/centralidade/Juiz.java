package br.com.conde.centralidade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * O JUIZ, que sao QUATRO juizes independentes.
 *
 * <p><b>1. A definicao de intermediacao.</b> Para cada par de vertices, listar TODOS
 * os caminhos minimos entre eles e contar em quantos cada terceiro vertice aparece no
 * meio. Custa o cubo do numero de vertices e so vai ate uns doze, e e justamente por
 * ser burra que ela julga: ela nao sabe o truque de Brandes.
 *
 * <p><b>3. O SISTEMA LINEAR do PageRank.</b> O PageRank e a solucao de um sistema de
 * equacoes lineares, e da para resolve-lo por ELIMINACAO em vez de iterar. A
 * eliminacao chega na resposta exata em n ao cubo operacoes e nao tem convergencia
 * nenhuma para discutir.
 *
 * <p>Esse segundo juiz e o mais forte do repositorio. A iteracao de potencia e um
 * metodo numerico que se aproxima da resposta; a eliminacao E a resposta. Quando os
 * dois concordam ate a decima casa, a iteracao convergiu de verdade e nao parou num
 * lugar parecido.
 *
 * <p><b>2. A ENUMERACAO LITERAL.</b> Listar todos os caminhos simples entre cada par
 * e contar na mao. Custa fatorial e so vai ate oito vertices, e e o juiz do proprio
 * juiz: foi ela que mostrou que a primeira versao da definicao estava errada.
 *
 * <p><b>4. As formas com resposta conhecida.</b> Na estrela, o centro tem
 * intermediacao um e as pontas tem zero. No ciclo e no completo, todos tem a mesma
 * coisa. No caminho, a intermediacao e simetrica. Sao numeros que dao para escrever
 * sem rodar nada.
 */
public final class Juiz {

    private Juiz() {
    }

    // ───────────────── 1 e 2. a intermediacao pela definicao e pela enumeracao ─────────────────

    /**
     * A INTERMEDIACAO pela DEFINICAO, em duas contas SEPARADAS.
     *
     * <p>Primeiro as distancias de todos para todos, por Floyd e Warshall. Depois,
     * para cada origem, QUANTOS caminhos minimos existem ate cada vertice, por uma
     * recorrencia em ordem de distancia crescente. Por fim, para cada terceiro
     * vertice, quantos desses caminhos passam por ele.
     *
     * <p>As duas primeiras contas sao separadas DE PROPOSITO, e essa separacao foi
     * uma correcao. A primeira versao contava os caminhos DENTRO do laco de Floyd e
     * Warshall, e isso conta duas vezes: um caminho que passa por dois vertices
     * intermediarios e somado na iteracao de cada um deles.
     *
     * <p>O erro nao aparecia em caminho, estrela, ciclo nem completo, porque nessas
     * formas ou ha um caminho minimo so entre cada par ou ha simetria total. Ele
     * aparecia nos grafos sorteados, e so a comparacao com Brandes o pegou.
     *
     * <p>Custa o cubo do numero de vertices e nao sabe o truque de Brandes, que e
     * justamente o que a torna juiz.
     */
    public static double[] intermediacaoPelaDefinicao(Grafo g, Contador contador) {
        int n = g.vertices();

        if (n > 60) throw new IllegalArgumentException("a definicao so vai ate 60 vertices, e vieram " + n);

        long[][] distancia = distanciasDeTodosParaTodos(g, contador);
        final long infinito = Long.MAX_VALUE / 4;

        // quantos caminhos minimos de i ate j, por ordem de distancia crescente
        double[][] caminhos = new double[n][n];

        for (int origem = 0; origem < n; origem++) {
            long[] daOrigem = distancia[origem];
            List<Integer> porDistancia = new ArrayList<>();

            for (int v = 0; v < n; v++) if (daOrigem[v] < infinito) porDistancia.add(v);

            porDistancia.sort((x, y) -> Long.compare(daOrigem[x], daOrigem[y]));

            caminhos[origem][origem] = 1;

            for (int v : porDistancia) {
                if (v == origem) continue;

                for (int u : g.chegandoEm(v)) {
                    contador.visitou();

                    if (distancia[origem][u] + 1 == distancia[origem][v])
                        caminhos[origem][v] += caminhos[origem][u];
                }
            }
        }

        double[] saida = new double[n];

        for (int v = 0; v < n; v++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    if (v == i || v == j || i == j) continue;

                    contador.visitou();

                    if (distancia[i][j] >= infinito || caminhos[i][j] == 0) continue;

                    if (distancia[i][v] + distancia[v][j] == distancia[i][j])
                        saida[v] += caminhos[i][v] * caminhos[v][j] / caminhos[i][j];
                }

        if (!g.dirigido()) for (int v = 0; v < n; v++) saida[v] /= 2;

        return Centralidades.normalizarIntermediacao(saida, n, g.dirigido());
    }

    /**
     * A INTERMEDIACAO pela ENUMERACAO LITERAL de todos os caminhos simples.
     *
     * <p>E a leitura mais burra possivel da definicao: para cada par de vertices,
     * LISTAR todos os caminhos simples entre eles, ficar com os mais curtos e contar em
     * quantos deles cada terceiro vertice aparece no meio. Nao tem matriz, nao tem
     * recorrencia, nao tem Floyd e nao tem Brandes. So listar e contar.
     *
     * <p>Custa fatorial e so vai ate oito vertices. E esse e o ponto: ela e burra
     * demais para errar do mesmo jeito que as outras duas erram.
     *
     * <p>Ela existe porque a versao anterior da {@link #intermediacaoPelaDefinicao}
     * estava ERRADA e eu nao tinha com que descobrir. Duas implementacoes que discordam
     * dizem que uma das duas esta errada e nao dizem qual. A terceira diz qual.
     *
     * <p>A recursao aqui e aceitavel porque a profundidade e no maximo oito, que e o
     * limite do metodo. Nas contas que rodam em grafo grande, a pilha e explicita.
     */
    public static double[] intermediacaoPorEnumeracao(Grafo g) {
        int n = g.vertices();

        if (n > 8) throw new IllegalArgumentException("a enumeracao so vai ate 8 vertices, e vieram " + n);

        double[] saida = new double[n];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                if (i == j) continue;

                List<List<Integer>> caminhos = new ArrayList<>();

                enumerar(g, i, j, new boolean[n], new ArrayList<>(List.of(i)), caminhos);

                if (caminhos.isEmpty()) continue;

                int menor = Integer.MAX_VALUE;

                for (List<Integer> caminho : caminhos) menor = Math.min(menor, caminho.size());

                List<List<Integer>> minimos = new ArrayList<>();

                for (List<Integer> caminho : caminhos) if (caminho.size() == menor) minimos.add(caminho);

                for (int v = 0; v < n; v++) {
                    if (v == i || v == j) continue;

                    int passam = 0;

                    // "no meio" e literal: nem a ponta de saida nem a de chegada contam
                    for (List<Integer> caminho : minimos)
                        if (caminho.subList(1, caminho.size() - 1).contains(v)) passam++;

                    saida[v] += (double) passam / minimos.size();
                }
            }

        if (!g.dirigido()) for (int v = 0; v < n; v++) saida[v] /= 2;

        return Centralidades.normalizarIntermediacao(saida, n, g.dirigido());
    }

    /**
     * Quantos PARES tem MAIS DE UM caminho minimo entre eles, pela enumeracao.
     *
     * <p>E a coluna que explica por que nenhuma forma desenhada a mao pegou o erro de
     * contagem: no caminho, na estrela e no haltere esse numero e ZERO. Onde ha um
     * caminho minimo so, contar certo e contar errado dao a mesma coisa.
     */
    public static int paresComMaisDeUmCaminhoMinimo(Grafo g) {
        int n = g.vertices();

        if (n > 8) throw new IllegalArgumentException("a enumeracao so vai ate 8 vertices, e vieram " + n);

        int pares = 0;

        for (int i = 0; i < n; i++)
            for (int j = g.dirigido() ? 0 : i + 1; j < n; j++) {
                if (i == j) continue;

                List<List<Integer>> caminhos = new ArrayList<>();

                enumerar(g, i, j, new boolean[n], new ArrayList<>(List.of(i)), caminhos);

                if (caminhos.isEmpty()) continue;

                int menor = Integer.MAX_VALUE;

                for (List<Integer> caminho : caminhos) menor = Math.min(menor, caminho.size());

                int minimos = 0;

                for (List<Integer> caminho : caminhos) if (caminho.size() == menor) minimos++;

                if (minimos > 1) pares++;
            }

        return pares;
    }

    private static void enumerar(Grafo g, int atual, int destino, boolean[] visitado,
                                 List<Integer> caminho, List<List<Integer>> achados) {

        if (atual == destino) {
            achados.add(List.copyOf(caminho));
            return;
        }

        visitado[atual] = true;

        for (int v : g.saindoDe(atual))
            if (!visitado[v]) {
                caminho.add(v);
                enumerar(g, v, destino, visitado, caminho, achados);
                caminho.remove(caminho.size() - 1);
            }

        visitado[atual] = false;
    }

    /** As distancias de todos para todos, por Floyd e Warshall. Serve de juiz da proximidade. */
    public static long[][] distanciasDeTodosParaTodos(Grafo g) {
        int n = g.vertices();
        long[][] distancia = new long[n][n];
        final long infinito = Long.MAX_VALUE / 4;

        for (long[] linha : distancia) Arrays.fill(linha, infinito);

        for (int v = 0; v < n; v++) distancia[v][v] = 0;

        for (int u = 0; u < n; u++)
            for (int v : g.saindoDe(u)) if (u != v) distancia[u][v] = 1;

        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (distancia[i][k] + distancia[k][j] < distancia[i][j])
                        distancia[i][j] = distancia[i][k] + distancia[k][j];

        return distancia;
    }

    /** O mesmo, contando as visitas. */
    public static long[][] distanciasDeTodosParaTodos(Grafo g, Contador contador) {
        contador.visitou((long) g.vertices() * g.vertices() * g.vertices());

        return distanciasDeTodosParaTodos(g);
    }

    // ───────────────────── 3. o sistema linear do PageRank ─────────────────────

    /**
     * O PAGERANK pela SOLUCAO EXATA do sistema linear, por eliminacao de Gauss.
     *
     * <p>O PageRank satisfaz, para cada vertice v:
     *
     * <pre>
     *   nota[v] = (1 - d) / n  +  d * soma das notas dos que apontam para v, divididas
     *             pelo grau de saida deles, mais a massa vazada repartida
     * </pre>
     *
     * <p>Isso e um sistema de n equacoes com n incognitas. A iteracao de potencia se
     * aproxima da solucao; a eliminacao E a solucao. Quando as duas concordam ate a
     * decima casa, a iteracao convergiu de verdade.
     *
     * <p>A eliminacao e com pivoteamento parcial, que troca linhas para o pivo ser o
     * maior em valor absoluto. Sem isso, um pivo quase zero faz o erro de
     * arredondamento explodir, e a conferencia viraria uma discussao sobre tolerancia.
     */
    public static double[] pageRankPeloSistema(Grafo g, double amortecimento, boolean tratarSemSaida) {
        int n = g.vertices();

        if (n == 0) return new double[0];
        if (n > 300) throw new IllegalArgumentException("o sistema so vai ate 300 vertices");

        List<Integer> becos = g.semSaida();
        double[][] a = new double[n][n + 1];

        for (int v = 0; v < n; v++) {
            a[v][v] = 1;

            for (int u : g.chegandoEm(v)) a[v][u] -= amortecimento / g.grauDeSaida(u);

            if (tratarSemSaida) for (int beco : becos) a[v][beco] -= amortecimento / n;

            a[v][n] = (1 - amortecimento) / n;
        }

        return resolver(a);
    }

    /** Eliminacao de Gauss com pivoteamento parcial. */
    public static double[] resolver(double[][] sistema) {
        int n = sistema.length;

        for (int coluna = 0; coluna < n; coluna++) {
            int pivo = coluna;

            for (int i = coluna + 1; i < n; i++)
                if (Math.abs(sistema[i][coluna]) > Math.abs(sistema[pivo][coluna])) pivo = i;

            double[] guarda = sistema[coluna];

            sistema[coluna] = sistema[pivo];
            sistema[pivo] = guarda;

            if (Math.abs(sistema[coluna][coluna]) < 1e-14)
                throw new IllegalStateException("o sistema nao tem solucao unica na coluna " + coluna);

            for (int i = coluna + 1; i < n; i++) {
                double fator = sistema[i][coluna] / sistema[coluna][coluna];

                for (int j = coluna; j <= n; j++) sistema[i][j] -= fator * sistema[coluna][j];
            }
        }

        double[] x = new double[n];

        for (int i = n - 1; i >= 0; i--) {
            double soma = sistema[i][n];

            for (int j = i + 1; j < n; j++) soma -= sistema[i][j] * x[j];

            x[i] = soma / sistema[i][i];
        }

        return x;
    }

    // ───────────────────── a comparacao ─────────────────────

    /** O maior desvio absoluto entre dois vetores de notas. */
    public static double maiorDesvio(double[] a, double[] b) {
        if (a.length != b.length) return Double.POSITIVE_INFINITY;

        double maior = 0;

        for (int i = 0; i < a.length; i++) maior = Math.max(maior, Math.abs(a[i] - b[i]));

        return maior;
    }

    /** Os dois vetores dao a MESMA ordem de importancia, mesmo com valores diferentes? */
    public static boolean mesmaOrdem(double[] a, double[] b, double tolerancia) {
        if (a.length != b.length) return false;

        List<Integer> porA = ordenados(a);
        List<Integer> porB = ordenados(b);

        for (int i = 0; i < porA.size(); i++)
            if (!porA.get(i).equals(porB.get(i))
                    && Math.abs(a[porA.get(i)] - a[porB.get(i)]) > tolerancia) return false;

        return true;
    }

    /** Os indices em ordem decrescente de valor, desempatando pelo indice. */
    public static List<Integer> ordenados(double[] valores) {
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < valores.length; i++) indices.add(i);

        indices.sort((x, y) -> {
            int porValor = Double.compare(valores[y], valores[x]);

            return porValor != 0 ? porValor : Integer.compare(x, y);
        });

        return indices;
    }
}
